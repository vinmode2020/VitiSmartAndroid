package com.example.vinmod;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;

public class Discussion extends AppCompatActivity {

    Button newPost_Btn;
    RecyclerView postList;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = database.getReference("/posts");

    PostAdapter adapter;

    ArrayList<Post> discussionPosts = new ArrayList<Post>();

    int colorCounter = 0;

    int colors[] = {Color.argb(255, 86, 168, 179),
            Color.argb(255, 255, 139, 139),
            Color.argb(255, 235, 211, 201),
            Color.argb(255, 136, 99, 72),
            Color.argb(255, 250, 205, 82)};


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion);

        Button newPost_Btn;

        newPost_Btn = findViewById(R.id.newPost_Btn);
        postList = findViewById(R.id.post_list);

        // Listener for "new post" button
        newPost_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Discussion.this, NewPost.class);
                startActivity(intent);
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

        discussionPosts = new ArrayList<Post>();

        ValueEventListener queryValueListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                Iterator<DataSnapshot> iterator = snapshotIterator.iterator();

                while (iterator.hasNext()) {
                    DataSnapshot next = (DataSnapshot) iterator.next();
                    discussionPosts.add(new Post(next.getKey().toString(), next.child("date").getValue().toString(), next.child("title").getValue().toString(), next.child("text").getValue().toString(), next.child("author").getValue().toString(), Integer.parseInt(next.child("replyCount").getValue().toString())));
                }

                adapter = new PostAdapter(discussionPosts);

                postList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        Query query = dbRef.orderByKey();
        query.addListenerForSingleValueEvent(queryValueListener);



        postList.setLayoutManager(new LinearLayoutManager(this));
    }

    public class PostHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
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

            postTitle.setOnClickListener(this);
            postAuthor.setOnClickListener(this);
            replyCount.setOnClickListener(this);
            replyText.setOnClickListener(this);
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

        @Override
        public void onClick(View view){
            Intent intent = new Intent(Discussion.this, ViewPost.class);
            Bundle bundle = new Bundle();
            bundle.putString("POST_ID", post.getId());
            bundle.putString("POST_DATE", post.getDate());
            bundle.putString("POST_AUTHOR", post.getUserName());
            bundle.putString("POST_TITLE", post.getTitle());
            bundle.putString("POST_CONTENT", post.getText());
            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    public class PostAdapter extends RecyclerView.Adapter<PostHolder> {

        private ArrayList<Post> postArrayList;

        public PostAdapter(ArrayList<Post> x){

            postArrayList = discussionPosts;
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