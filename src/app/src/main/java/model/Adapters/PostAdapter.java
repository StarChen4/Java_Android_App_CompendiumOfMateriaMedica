package model.Adapters;

import static androidx.core.content.ContextCompat.startActivity;


import static model.UtilsApp.formatTimestamp;
import static model.UtilsApp.loadImageFromURL;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.compendiumofmateriamedica.MainActivity;
import com.example.compendiumofmateriamedica.R;
import com.example.compendiumofmateriamedica.ui.profile.ProfileFragment;
import com.example.compendiumofmateriamedica.ui.profile.ProfilePage;
import com.example.compendiumofmateriamedica.ui.profile.ProfileViewModel;
import com.example.compendiumofmateriamedica.ui.social.PhotoDialogFragment;

import java.util.List;
import java.util.stream.Collectors;

import model.Datastructure.Post;
import model.Datastructure.PostTreeManager;
import model.Datastructure.User;
import model.Datastructure.UserTreeManager;
import model.Parser.Token;
import model.UtilsApp;

/**
 * @author: Xing Chen
 * @datetime: 2024/5/2
 * @description: A post adapter for showing posts
 * the posts will be shown in separate view holders
 * each post is arranged using post_item.xml
 */

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private final String TAG = "PostAdapter";
    private Context context;
    // a list of posts
    private List<Post> postsList;
    private FragmentManager fragmentManager;
    private boolean showLikeButton;
    private boolean isLiked;
    private User currentUser;
    // an inner class to hold and reuse the view
    public static class PostViewHolder extends RecyclerView.ViewHolder{

        public TextView username;
        public ImageView userAvatar;
        public TextView content;
        public ImageView photo;
        public ImageButton buttonLike;
        public TextView timestamp;
        public ImageView userLevel;

        public PostViewHolder(View itemView){
            super(itemView);
            username = itemView.findViewById(R.id.post_user_name);
            userAvatar = itemView.findViewById(R.id.post_user_avatar);
            content = itemView.findViewById(R.id.post_content);
            photo = itemView.findViewById(R.id.post_photo);
            buttonLike = itemView.findViewById(R.id.button_post_like);
            timestamp = itemView.findViewById(R.id.post_timestamp);
            userLevel = itemView.findViewById(R.id.post_user_level);
        }
    }

    public PostAdapter(Context context, List<Post> postsList, FragmentManager fragmentManager, Boolean showLikeButton, User currentUser){
        this.context = context;
        this.postsList = postsList;
        this.fragmentManager = fragmentManager;
        this.showLikeButton = showLikeButton;
        this.currentUser = currentUser;
        Log.d(TAG, "Initialize postAdapter successful, show_like_button=" + showLikeButton + ", current user=" + currentUser.getUser_id());
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_item, parent, false);
        return new PostViewHolder(v);
    }

    /**
     * attach the date onto view holder
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *        item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position){
        UserTreeManager userTreeManager=UserTreeManager.getInstance();
        // get current post
        Post post = postsList.get(position);
        // get uid of this post user
        int uid = post.getUser_id();
        // get the user using this uid
        User postUser = userTreeManager.findUserById(uid);
        // get the user name,the user's avatar and the post photo
        if (postUser != null){
            String postUserUsername = postUser.getUsername();
            String postUserAvatarURL = postUser.getAvatar_url();
            String postPhotoURL = post.getPhoto_url();
            List<Token> postContent = post.getContent();
            String postTimestamp = post.getTimestamp();


            // 设置头像点击事件
            holder.userAvatar.setOnClickListener(v -> {
                PhotoDialogFragment avatarDialogFragment = PhotoDialogFragment.newInstance(postUserAvatarURL);
                avatarDialogFragment.show(fragmentManager, "avatar_dialog");
            });
            // 设置用户名点击事件，点击后会进入个人主页
            holder.username.setOnClickListener(v -> {
                if(postUser.getUser_id() != currentUser.getUser_id()) {
                    // 跳转被点击用户的个人主页
                    Intent intent = new Intent(context, ProfilePage.class);
                    intent.putExtra("AppUser", currentUser);
                    intent.putExtra("ProfileUser", postUser);
                    context.startActivity(intent);
                }
            });
            // 设置照片点击事件
            holder.photo.setOnClickListener(v -> {
                PhotoDialogFragment photoDialogFragment = PhotoDialogFragment.newInstance(postPhotoURL);
                photoDialogFragment.show(fragmentManager, "photo_dialog");
            });

            // 设置点赞按钮装态
            if(showLikeButton){
                // 加载点赞状态
                isLiked = post.isLikedByUser(currentUser.getUser_id());
                // 设置点赞图标
                if(!isLiked){
                    holder.buttonLike.setImageResource(R.drawable.button_post_like);
                } else {
                    holder.buttonLike.setImageResource(R.drawable.button_post_unlike);
                }
                // 设置点赞按钮事件
                holder.buttonLike.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (!isLiked) {
                            // 此次为点赞操作
                            isLiked = !isLiked;
                            // UI逻辑
                            holder.buttonLike.setImageResource(R.drawable.button_post_unlike);  // 点赞后的图标
                            //后台逻辑
                            post.likedByUser(currentUser.getUser_id());


                        } else {
                            // 此次为取消点赞操作
                            isLiked = !isLiked;
                            // UI逻辑
                            holder.buttonLike.setImageResource(R.drawable.button_post_like);  // 默认图标
                            //后台逻辑
//                            post.likedByUser(currentUser.getUser_id());
                        }
                    }
                });
            }


            PostTreeManager postTreeManager=PostTreeManager.getInstance();
            // set the content of the viewHolder
            // load avatar image from url
            loadImageFromURL(this.context, postUserAvatarURL, holder.userAvatar, "Avatar");
            holder.username.setText(postUserUsername);
            holder.content.setText(postContent.stream().map(Token::getToken).collect(Collectors.joining(" ")));
            // 处理时间戳
            holder.timestamp.setText(formatTimestamp(postTimestamp));
            // user level
            ProfileFragment.setUserLevelImage(holder.userLevel, postTreeManager.getUserPlantDiscovered(uid).size());
            // load photo from post
            loadImageFromURL(this.context, postPhotoURL, holder.photo, "Photo");
        } else {
            String postPhotoURL = post.getPhoto_url();

            // 如果用户不存在，不做任何操作
            holder.userAvatar.setOnClickListener(v -> {
                Toast.makeText(context, "No user info available", Toast.LENGTH_SHORT).show();
            });

            holder.username.setText("Unknown User");
            holder.content.setText(post.getContent().stream().map(Token::getToken).collect(Collectors.joining(" ")));
            holder.userAvatar.setImageResource(R.drawable.unknown_user);
            loadImageFromURL(this.context, postPhotoURL, holder.photo, "Photo");
        }

    }

    public int getItemCount(){
        return postsList.size();
    }
    // a method to update the posts and change the display
    public void setPosts(List<Post> posts){
        this.postsList = posts;
        notifyDataSetChanged();
    }
}
