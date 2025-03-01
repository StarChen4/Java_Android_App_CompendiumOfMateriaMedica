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
 * @author Haochen Gong
 * @uid u7776634
 * @description: PostTree management method class
 **/
public class PostTreeManager implements TreeManager<Post> {
    private final RBTree<Post> postRBTree;

    public enum PostInfoType {
        POST_ID, UID, PLANT_ID, TIME, CONTENT;
    }

    // singleton design pattern
    private static PostTreeManager instance;

    private PostTreeManager(RBTree<Post> postRBTree) {
        if (instance != null) {
            throw new IllegalStateException("Instance already created");
        }
        this.postRBTree = postRBTree;
    }

    public static synchronized PostTreeManager getInstance(RBTree<Post> postRBTree) {
        if (instance == null) {
            instance = new PostTreeManager(postRBTree);
        }
        return instance;
    }

    // getter
    public synchronized static PostTreeManager getInstance() {
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
    public void delete(int postId) {
        this.postRBTree.delete(postId);
    }

    // External search interface, call this method to start a search
    public <T> ArrayList<Post> search(PostInfoType infoType, T info) {

        ArrayList<Post> posts = new ArrayList<>();

        if (infoType == PostInfoType.POST_ID) {
            try {
                RBTreeNode<Post> post = this.postRBTree.search(Integer.parseInt((String) info));
                if (post != null) {
                    posts.add(post.getValue());
                    //Log.d("PostTreeManager", "Found post id " + post.getValue().getPost_id() + " in postTree.");
                } else{
                    //Log.d("PostTreeManager", "Post id is not in postTree.");
                }
            } catch (NumberFormatException e) {
                return posts;
            }
        } else {
            search(this.postRBTree.root, infoType, info, posts);
        }

        return posts;
    }

    // The actual recursive search method
    private <T> void search(RBTreeNode<Post> node, PostInfoType infoType, T info, ArrayList<Post> posts) {
        // If the current node is null, it means that the child of the leaf node has been reached, and it is returned directly
        if (node == null) {
            return;
        }
        try {
            // If the value of the current node is equal to the searched value, add to the result list
            switch (infoType) {
                case UID:
                    if (node.getValue().getUser_id() == Integer.parseInt((String) info)) {
                        posts.add(node.getValue());
                    }
                    break;
                case PLANT_ID:
                    if (node.getValue().getPlant_id() == Integer.parseInt((String) info)) {
                        posts.add(node.getValue());
                    }
                    break;
                // Timestamp handling needs to be encapsulated in advance (here it's simply a matter of determining whether the timestamps stored in the post object are identical)
                case TIME:
                    if (node.getValue().getTimestamp().contains((CharSequence) info)) {
                        posts.add(node.getValue());
                    }
                    break;
                // Find out if the content contains a certain character
                case CONTENT:
                    List<Token> content = node.getValue().getContent(); // 转换成小写字母
                    for (Token token : content) {
                        if (token.getToken().toLowerCase().contains((CharSequence) info)) {
                            posts.add(node.getValue());
                            break;
                        }
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            return;
        }

        search(node.getLeft(), infoType, info, posts);
        search(node.getRight(), infoType, info, posts);
    }

    // get newest posts based on given number and timestamp
    public ArrayList<Post> getNewestPosts(int numberOfPosts, String lastLoadedPostTimestamp) {

        ArrayList<Post> beforePosts = new ArrayList<>();

        // get all posts before this timestamp
        getBeforePosts(postRBTree.root, lastLoadedPostTimestamp, beforePosts);
        // descending order
        beforePosts.sort(new Comparator<Post>() {
            @Override
            public int compare(Post p1, Post p2) {
                return LocalDateTime.parse(p2.getTimestamp()).compareTo(LocalDateTime.parse(p1.getTimestamp()));
            }
        });

        // return all when not adequate, partial when enough.
        if (beforePosts.size() <= numberOfPosts) {
            return beforePosts;
        } else {
            return new ArrayList<>(beforePosts.subList(0, numberOfPosts));
        }
    }

    // find all posts before timestamp
    private void getBeforePosts(RBTreeNode<Post> node, String timeStamp, ArrayList<Post> posts) {
        if (node != null) {
            if (LocalDateTime.parse(node.getValue().getTimestamp()).isBefore(LocalDateTime.parse(timeStamp))) {
                posts.add(node.getValue());  // add
            }
            getBeforePosts(node.getLeft(), timeStamp, posts);  // iterate left child
            getBeforePosts(node.getRight(), timeStamp, posts);  // iterate right child
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
        ArrayList<Post> searchResult = search(PostTreeManager.PostInfoType.POST_ID, String.valueOf(postId));
        // Check user validation, uid is unique
        if (!searchResult.isEmpty()) {
            return searchResult.get(0);
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
        ArrayList<Post> user_post_data = search(PostTreeManager.PostInfoType.UID, String.valueOf(uid));
        //if(user_post_data != null)
            //Log.d("PostTreeManager", "This user has " + user_post_data.size() + " posts in me.");
//        Log.w("PostTree root",""+instance.postRBTree);
        List<Post> user_post_data_list = new ArrayList<>();
        if (!user_post_data.isEmpty()) {
            for (Post post : user_post_data) {
                user_post_data_list.add(post);
            }
            return user_post_data_list;
        } else {
            //Log.w("PostTreeManager", "Get posts by user id" + uid + " failed, there is no posts of this user");
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

    public PostInfoType getTypeByString(String type) throws Exception {
        switch (type.toUpperCase()) {
            case "POST_ID":
                return PostInfoType.POST_ID;
            case "UID":
                return PostInfoType.UID;
            case "PLANT_ID":
                return PostInfoType.PLANT_ID;
            case "TIME":
                return PostInfoType.TIME;
            case "CONTENT":
                return PostInfoType.CONTENT;
            default:
                throw new Exception("Invalid Post Type");
        }
    }
}


