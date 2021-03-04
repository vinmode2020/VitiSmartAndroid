package com.example.vinmod;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

public class ViewPost extends AppCompatActivity {

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference;
    private FirebaseUser user;

    RecyclerView replyList;
    ReplyAdapter adapter;

    private ArrayList<Reply> postReplies;

    int colorCounter = 0;

    int colors[] = {Color.argb(255, 86, 180, 233),
            Color.argb(255, 230, 159, 0),
            Color.argb(255, 0, 158, 115),
            Color.argb(255, 240, 228, 66),
            Color.argb(255, 204, 121, 167)};

    TextView postTitle;
    TextView postHeader;
    TextView postContent;
    TextView replyHeader;

    Button addReplyButton;

    Post post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_post);

        Bundle extras = getIntent().getExtras();

        post = new Post(
                extras.getString("POST_ID"),
                extras.getString("POST_DATE"),
                extras.getString("POST_TITLE"),
                extras.getString("POST_CONTENT"),
                extras.getString("POST_AUTHOR"),
                Integer.parseInt(extras.getString("REPLY_COUNT")),
                ""
        );

        postTitle = findViewById(R.id.post_title_details);
        postHeader = findViewById(R.id.post_header);
        postContent = findViewById(R.id.post_text);
        replyHeader = findViewById(R.id.replies_header);
        addReplyButton = findViewById(R.id.add_reply_button);
        replyList = findViewById(R.id.reply_list);

        postTitle.setText(post.getTitle());
        postHeader.setText("On " + post.getDate() + ", " + post.getUserName() + " wrote:\n");
        postContent.setText(post.getText());

        if(post.getReplyCount() != 1){
            replyHeader.setText(post.getReplyCount() + " Replies");
        }
        else{
            replyHeader.setText(post.getReplyCount() + " Reply");
        }


        addReplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final EditText reply = new EditText(v.getContext());

                final AlertDialog.Builder addReplyDialog = new AlertDialog.Builder(v.getContext());
                addReplyDialog.setTitle("New Reply");
                addReplyDialog.setMessage("Write a reply...");
                addReplyDialog.setView(reply);

                addReplyDialog.setPositiveButton("Post", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy 'at' HH:mm:ss");
                        SimpleDateFormat dateSortable = new SimpleDateFormat("yyyyMMddHHmmss");

                        String formatDate = date.format(new Date());
                        String formatDateSortable = dateSortable.format(new Date());

                        user = FirebaseAuth.getInstance().getCurrentUser();

                        String screenName = user.getEmail().substring(0, user.getEmail().indexOf('@'));
                        String replyId = UUID.randomUUID().toString().replace("-", "");

                        databaseReference = FirebaseDatabase.getInstance().getReference();

                        if(reply.getText().toString().isEmpty()){
                            Toast.makeText(ViewPost.this, "Please write a reply before submitting.", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId);
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("user").setValue(screenName);
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("date").setValue(formatDate);
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("dateSortable").setValue(formatDateSortable);
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("message").setValue(reply.getText().toString().trim());
                            databaseReference.child("posts").child(post.getId()).child("replyCount").setValue(post.getReplyCount() + 1);

                            Toast.makeText(ViewPost.this, "Reply Successfully Added!", Toast.LENGTH_SHORT).show();
                            getIntent().putExtra("REPLY_COUNT", String.valueOf(post.getReplyCount() + 1));
                            finish();
                            startActivity(getIntent());
                        }

                    }
                });

                addReplyDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // close
                    }
                });

                addReplyDialog.create().show();

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        postReplies = new ArrayList<Reply>();

        databaseReference = database.getReference("/posts/" + post.getId() + "/replies");

        ValueEventListener queryValueListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                Iterator<DataSnapshot> iterator = snapshotIterator.iterator();

                while (iterator.hasNext()) {
                    DataSnapshot next = (DataSnapshot) iterator.next();
                    postReplies.add(new Reply(next.getKey().toString(), next.child("date").getValue().toString(), next.child("user").getValue().toString(), next.child("message").getValue().toString()));
                }

                Collections.reverse(postReplies);

                adapter = new ViewPost.ReplyAdapter(postReplies);

                replyList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };


        Query query = databaseReference.orderByChild("dateSortable");
        query.addListenerForSingleValueEvent(queryValueListener);



        replyList.setLayoutManager(new LinearLayoutManager(this));
    }

    public class ReplyHolder extends RecyclerView.ViewHolder{
        public final TextView replyHeader;
        public final TextView replyContent;

        public Reply reply;

        public ReplyHolder(View itemView) {
            super(itemView);
            replyHeader = itemView.findViewById(R.id.reply_header);
            replyContent = itemView.findViewById(R.id.reply_text);
        }

        public void bind(Reply currentReply){
            reply = currentReply;

            replyHeader.setText("On " + reply.getDate() + ", " + reply.getUser() + " wrote:");
            replyContent.setText(reply.getMessage());

            replyHeader.setBackgroundColor(colors[colorCounter]);
            replyContent.setBackgroundColor(colors[colorCounter]);

            colorCounter++;
            if (colorCounter == 5) colorCounter = 0;
        }

    }

    public class ReplyAdapter extends RecyclerView.Adapter<ViewPost.ReplyHolder> {

        private ArrayList<Reply> replyArrayList;

        public ReplyAdapter(ArrayList<Reply> x){

            replyArrayList = postReplies;
        }

        @NonNull
        @Override
        public ViewPost.ReplyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_reply, parent, false);

            return new ViewPost.ReplyHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewPost.ReplyHolder holder, int position) {
            Reply nextReply = replyArrayList.get(position);
            holder.bind(nextReply);
        }

        @Override
        public int getItemCount() {
            return replyArrayList.size();
        }
    }
}