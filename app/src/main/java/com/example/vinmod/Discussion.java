package com.example.vinmod;

import android.content.Intent;
import android.graphics.Color;
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

    PostAdapter adapter;

    ArrayList<Post> dummyPostList = new ArrayList<Post>();

    int colorCounter = 0;

    int colors[] = {Color.argb(255, 86, 168, 179),
            Color.argb(255, 255, 139, 139),
            Color.argb(255, 235, 211, 201),
            Color.argb(255, 136, 99, 72),
            Color.argb(255, 250, 205, 82)};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion);

        Button newPost_Btn;
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
        dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));

        adapter = new PostAdapter(dummyPostList);

        newPost_Btn = findViewById(R.id.newPost_Btn);
        postList = findViewById(R.id.post_list);

        postList.setAdapter(adapter);

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
        public final TextView replyText;

        public Post post;

        public PostHolder(View itemView) {
            super(itemView);
            postTitle = itemView.findViewById(R.id.postName);
            postAuthor = itemView.findViewById(R.id.post_author);
            replyCount = itemView.findViewById(R.id.reply_count);
            replyText = itemView.findViewById(R.id.textView8);
        }

        public void bind(Post currentPost){
            post = currentPost;
            postTitle.setText(post.getTitle());
            postTitle.setBackgroundColor(colors[colorCounter]);

            postAuthor.setText("By " + post.getUserName());
            postAuthor.setBackgroundColor(colors[colorCounter]);

            replyCount.setText(Integer.toString(post.getReplyCount()));
            replyCount.setBackgroundColor(colors[colorCounter]);

            replyText.setBackgroundColor(colors[colorCounter]);

            colorCounter++;
            if (colorCounter == 5) colorCounter = 0;
        }
    }

    public class PostAdapter extends RecyclerView.Adapter<PostHolder> {

        private ArrayList<Post> postArrayList;

        public PostAdapter(ArrayList<Post> x){
            dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
            dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
            dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
            dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
            dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
            dummyPostList.add(new Post("hfjdskl", "hvreufidjsk", "hvurefi", 43));
            postArrayList = dummyPostList;
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