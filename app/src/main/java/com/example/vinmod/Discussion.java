package com.example.vinmod;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Discussion extends AppCompatActivity {

    Button newPost_Btn;
    RecyclerView postList;

    ArrayList<Post> dummyPostList = new ArrayList<Post>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button newPost_Btn;
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));

        newPost_Btn = findViewById(R.id.newPost_Btn);
        postList = findViewById(R.id.post_list);

        postList.setLayoutManager(new LinearLayoutManager(this));

        // Click the Discussion board button

        // Listener for "new post" button
        newPost_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Discussion.this, NewPost.class);

            }
        });


    }

    public class PostHolder extends RecyclerView.ViewHolder{
        public final TextView postTitle;
        public final TextView postAuthor;
        public final TextView replyCount;

        public Post post;

        public PostHolder(View itemView) {
            super(itemView);
            postTitle = findViewById(R.id.post_title);
            postAuthor = findViewById(R.id.post_author);
            replyCount = findViewById(R.id.reply_count);
        }

        public void bind(Post currentPost){
            post = currentPost;
            postTitle.setText(post.getTitle());
            postAuthor.setText(post.getUserName());
            replyCount.setText(post.getReplyCount());
        }
    }

    public class PostAdapter extends RecyclerView.Adapter<PostHolder> {

        private ArrayList<Post> postArrayList;

        public PostAdapter(ArrayList<Post> x){
            postArrayList = x;
        }

        @NonNull
        @Override
        public PostHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_post, parent, false);

            return new PostHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostHolder holder, int position) {
            Post nextPost = postArrayList.get(position);
            holder.bind(nextPost);
        }

        @Override
        public int getItemCount() {
            return postArrayList.size();
        }
    }

}