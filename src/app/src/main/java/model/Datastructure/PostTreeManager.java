package model.Datastructure;

import android.util.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Parser.Token;

/**
 * @author: Haochen Gong
 * @description: post树的管理方法类
 **/
public class PostTreeManager implements TreeManager<Post> {
    private final RBTree<Post> postRBTree;

    public enum PostInfoType {
        POST_ID, UID, PLANT_ID, TIME, CONTENT;
    }

    // singleton design pattern
    private static PostTreeManager instance;

    private PostTreeManager(RBTree<Post> postRBTree) {
        this.postRBTree = postRBTree;
    }

    public static synchronized PostTreeManager getInstance(RBTree<Post> postRBTree) {
        if (instance == null) {
            instance = new PostTreeManager(postRBTree);
        }
        return instance;
    }

    // getter
    public static PostTreeManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Instance not created. Call getInstance(RBTree<Post>) first.");
        }
        return instance;
    }

    @Override
    public void insert(int postId, Post post) {
        this.postRBTree.insert(postId, post);
    }

    @Override
    public void delete(int plantId) {
        this.postRBTree.delete(plantId);
    }

    // 对外的搜索接口，调用这个方法来开始搜索
    public <T> ArrayList<RBTreeNode<Post>> search(PostInfoType infoType, T info) {

        ArrayList<RBTreeNode<Post>> posts = new ArrayList<>();

        if (infoType == PostInfoType.POST_ID) {
            RBTreeNode<Post> post = this.postRBTree.search(Integer.parseInt((String) info));
            if (post != null) {
                posts.add(post);
            }
        } else {
            search(this.postRBTree.root, infoType, info, posts);
        }

        return posts;
    }

    // 实际的递归搜索方法
    private <T> void search(RBTreeNode<Post> node, PostInfoType infoType, T info, ArrayList<RBTreeNode<Post>> posts) {
        // 如果当前节点是null，说明已经到达了叶子节点的子节点，直接返回
        if (node == null) {
            return;
        }
        // 如果当前节点的值与搜索的值相等，加入结果列表
        switch (infoType) {
            case UID:
                if (node.getValue().getUser_id() == Integer.parseInt((String) info)) {
                    posts.add(node);
                }
                break;
            case PLANT_ID:
                if (node.getValue().getPlant_id() == Integer.parseInt((String) info)) {
                    posts.add(node);
                }
                break;
            // 需要提前封装timestamp的处理（这里只是简单的判断了post对象储存的时间戳是否完全一致）
            case TIME:
                if (node.getValue().getTimestamp().contains((CharSequence) info)) {
                    posts.add(node);
                }
                break;
            // 查找内容里是否含有某字符
            case CONTENT:
                List<Token> content = node.getValue().getContent(); // 转换成小写字母
                for (Token token : content) {
                    if (token.getToken().toLowerCase().contains((CharSequence) info)) {
                        posts.add(node);
                        break;
                    }
                }
                break;
        }

        // 继续在左子树中递归搜索
        search(node.getLeft(), infoType, info, posts);
        // 继续在右子树中递归搜索
        search(node.getRight(), infoType, info, posts);
    }

    // 根据时间戳获取指定数量post
    public ArrayList<Post> getNewestPosts(int numberOfPosts, String lastLoadedPostTimestamp) {

        ArrayList<Post> beforePosts = new ArrayList<>();

        // 获取指定时间戳之前的帖子
        getBeforePosts(postRBTree.root, lastLoadedPostTimestamp, beforePosts);  // 获取所有时间在指定时间之前的post
        // 按时间戳降序排列
        beforePosts.sort(new Comparator<Post>() {
            @Override
            public int compare(Post p1, Post p2) {
                return LocalDateTime.parse(p2.getTimestamp()).compareTo(LocalDateTime.parse(p1.getTimestamp()));
            }
        });

        // 不足需要的数量时,全部返回,足够时返回指定数量
        if (beforePosts.size() <= numberOfPosts) {
            return beforePosts;
        } else {
            return new ArrayList<>(beforePosts.subList(0, numberOfPosts));
        }
    }

    // 找到所有发布时间在输入时间之前的post
    private void getBeforePosts(RBTreeNode<Post> node, String timeStamp, ArrayList<Post> posts) {
        if (node != null) {
            if (LocalDateTime.parse(node.getValue().getTimestamp()).isBefore(LocalDateTime.parse(timeStamp))) {
                posts.add(node.getValue());  // 当前节点时间在输入时间之前时，添加到结果列表
            }
            getBeforePosts(node.getLeft(), timeStamp, posts);  // 访问左子树
            getBeforePosts(node.getRight(), timeStamp, posts);  // 访问右子树
        }
    }

    public int getTreeSize() {
        return postRBTree.size();
    }

    public Post getPostByPostId(int postId) {
        // Check the initialization of PostTreeManager
        if (checkManagerInitial()==false){
            return null;
        }
        // Search post with given postId
        ArrayList<RBTreeNode<Post>> searchResult = instance.search(PostTreeManager.PostInfoType.POST_ID, String.valueOf(postId));
        // Check user validation, uid is unique
        if (!searchResult.isEmpty()) {
            return searchResult.get(0).getValue();
        }
        return null;
    }

    // get all posts by user with uid
    public List<Post> getPostsByUserId(int uid) {
        // Check the initialization of PostTreeManager
        if (checkManagerInitial()==false){
            return null;
        }
        // Search post with given uID
        ArrayList<RBTreeNode<Post>> user_post_data = instance.search(PostTreeManager.PostInfoType.UID, String.valueOf(uid));
//        Log.w("PostTree root",""+instance.postRBTree);
        List<Post> user_post_data_list = new ArrayList<>();
        if (!user_post_data.isEmpty()) {
            for (RBTreeNode<Post> node : user_post_data) {
                user_post_data_list.add(node.getValue());
            }
            return user_post_data_list;
        } else {
            Log.w("MainActivity", "Get posts by user id" + uid + " failed, there is no posts of this user");
        }
        return user_post_data_list;
    }


    public Set<Integer> getUserPlantDiscovered(int uid){
        // Check the initialization of PostTreeManager
        if (checkManagerInitial()==false){
            return null;
        }
        List<Post> posts = getPostsByUserId(uid);
        Set<Integer> plantsDiscovered = new HashSet<>();
        for (Post post : posts){
            int plantId = post.getPlant_id();
            plantsDiscovered.add(plantId);
        }
        return plantsDiscovered;
    }

    // get the newest one post of given uid
    public Post getUserNewestPost(int uid){
        // Check the initialization of PostTreeManager
        if (checkManagerInitial()==false){
            return null;
        }
        List<Post> allUserPosts = getPostsByUserId(uid);
        if (allUserPosts.isEmpty()) {
            return null;
        }
        Post newestPost = allUserPosts.get(0);
        for (Post post : allUserPosts) {
            if (post.getTimestamp().compareTo(newestPost.getTimestamp()) > 0) {
                newestPost = post;
            }
        }
        return newestPost;
    }

    public boolean checkManagerInitial(){
        // Check the initialization of PostTreeManager
        if (instance == null) {
            Log.w("PostTreeManager", "PostTreeManager has not been initialized");
            return false;
        }
        return true;
    }
}


