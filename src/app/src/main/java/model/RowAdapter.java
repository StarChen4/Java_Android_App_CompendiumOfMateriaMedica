package model;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.compendiumofmateriamedica.MainActivity;
import com.example.compendiumofmateriamedica.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

public class RowAdapter extends RecyclerView.Adapter<RowAdapter.RowViewHolder> {
    private Context context;
    private ArrayList<Integer> data;
    private PostTreeManager postTreeManager;

    public RowAdapter(Context context, ArrayList<Integer> data) throws JSONException, IOException {
        this.context = context;
        this.data = data;
        postTreeManager = new PostTreeManager((RBTree<Post>) GeneratorFactory.tree(this.context, DataType.POST, R.raw.posts));
    }

    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_layout_item, parent, false);
        return new RowViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        ArrayList<RBTreeNode<Post>> nodes = postTreeManager.search(PostTreeManager.PostInfoType.POST_ID, String.valueOf(data.get(position)));
        Log.println(Log.ASSERT, "DEBUG", "[GridAdapter] onBindViewHolder: nodes size " + nodes.size());
        String postURL = nodes.get(0).getValue().getPhoto();
        MainActivity.loadImageFromURL(this.context, postURL, holder.postImage, "Photo");
        String postContent = nodes.get(0).getValue().getContent();

        holder.postContent.setText(postContent);

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class RowViewHolder extends RecyclerView.ViewHolder {
        public ImageView postImage;
        public TextView userName;
        public TextView postContent;


        public RowViewHolder(View itemView) {
            super(itemView);
            postImage = itemView.findViewById(R.id.rowLayoutPostImage);
            userName = itemView.findViewById(R.id.rowLayoutUserName);
            postContent = itemView.findViewById(R.id.rowLayoutPostContent);
        }
    }
}
