package com.example.vinmod;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class Discussion extends AppCompatActivity {

    Button newPost_Btn;
    RecyclerView postList;
    Spinner sortOptions;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = database.getReference("/posts");
    FirebaseUser user;

    Boolean isDescending = true;

    PostAdapter adapter;

    ArrayList<Post> discussionPosts = new ArrayList<Post>();

    int colorCounter = 0;

    int colors[] = {Color.argb(255, 86, 180, 233),
            Color.argb(255, 230, 159, 0),
            Color.argb(255, 0, 158, 115),
            Color.argb(255, 240, 228, 66),
            Color.argb(255, 204, 121, 167)};


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion);

        Button newPost_Btn;

        newPost_Btn = findViewById(R.id.newPost_Btn);
        postList = findViewById(R.id.post_list);

        createSpinner();

        user = FirebaseAuth.getInstance().getCurrentUser();

        // Listener for "new post" button
        newPost_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Discussion.this, NewPost.class);
                startActivity(intent);
            }
        });


    }

    ValueEventListener queryValueListener = new ValueEventListener() {

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
            Iterator<DataSnapshot> iterator = snapshotIterator.iterator();

            while (iterator.hasNext()) {
                DataSnapshot next = (DataSnapshot) iterator.next();
                discussionPosts.add(new Post(next.getKey().toString(),
                        next.child("date").getValue().toString(),
                        next.child("title").getValue().toString(),
                        next.child("text").getValue().toString(),
                        next.child("author").getValue().toString(),
                        Integer.parseInt(next.child("replyCount").getValue().toString()),
                        (next.child("authorId").exists()) ? next.child("authorId").getValue().toString() : "N/A"));
            }

            if(isDescending){
                Collections.reverse(discussionPosts);
            }

            adapter = new PostAdapter(discussionPosts);

            postList.setAdapter(adapter);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        discussionPosts = new ArrayList<Post>();

        //Query query = dbRef.orderByChild("dateSortable");
        //query.addListenerForSingleValueEvent(queryValueListener);

        postList.setLayoutManager(new LinearLayoutManager(this));
    }

    public class PostHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public final TextView postTitle;
        public final TextView postAuthor;
        public final TextView replyCount;
        public final TextView replyText;
        public final TextView deleteBtn;

        public Post post;

        public PostHolder(View itemView) {
            super(itemView);
            postTitle = itemView.findViewById(R.id.postName);
            postAuthor = itemView.findViewById(R.id.post_author);
            replyCount = itemView.findViewById(R.id.reply_count);
            replyText = itemView.findViewById(R.id.textView8);
            deleteBtn = itemView.findViewById(R.id.delete_post);

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

            if(user.getUid().compareTo(post.getAuthorId()) == 0) {
                deleteBtn.setText(" X ");
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final AlertDialog.Builder removePostDialog = new AlertDialog.Builder(v.getContext());
                        removePostDialog.setTitle("Delete Post");
                        removePostDialog.setMessage("Are you sure you want to delete this post?");

                        removePostDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbRef.child(post.getId()).removeValue();
                                Toast.makeText(Discussion.this, "Post successfully deleted!", Toast.LENGTH_SHORT).show();
                                finish();
                                startActivity(getIntent());
                            }
                        });

                        removePostDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // close
                            }
                        });

                        removePostDialog.create().show();
                    }
                });
            }
            else{
                deleteBtn.setText("");
            }
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
            bundle.putString("REPLY_COUNT", String.valueOf(post.getReplyCount()));
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

    public void createSpinner(){
        sortOptions = findViewById(R.id.sort_options);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.filter_options, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sortOptions.setAdapter(adapter);

        sortOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //startMonthSpinner listener

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                discussionPosts = new ArrayList<>();

                if(position == 0){
                    isDescending = true;
                    Query query = dbRef.orderByChild("dateSortable");
                    query.addListenerForSingleValueEvent(queryValueListener);
                }
                else if(position == 1){
                    isDescending = false;
                    Query query = dbRef.orderByChild("dateSortable");
                    query.addListenerForSingleValueEvent(queryValueListener);
                }
                else if(position == 2){
                    isDescending = true;
                    Query query = dbRef.orderByChild("replyCount");
                    query.addListenerForSingleValueEvent(queryValueListener);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

}