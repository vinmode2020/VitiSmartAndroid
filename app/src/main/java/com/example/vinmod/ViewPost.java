package com.example.vinmod;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

public class ViewPost extends AppCompatActivity implements Serializable {

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
    ImageView editButton;

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
                extras.getString("AUTHOR_ID")
        );

        postTitle = findViewById(R.id.post_title_details);
        postHeader = findViewById(R.id.post_header);
        postContent = findViewById(R.id.post_text);
        replyHeader = findViewById(R.id.replies_header);
        addReplyButton = findViewById(R.id.add_reply_button);
        replyList = findViewById(R.id.reply_list);
        editButton = findViewById(R.id.edit_button);

        int spacing = getResources().getDimensionPixelSize(R.dimen.nav_header_vertical_spacing);
        replyList.addItemDecoration(new SpacesItemDecoration(spacing));

        postTitle.setText(post.getTitle());
        postHeader.setText("On " + post.getDate() + ", " + post.getUserName() + " wrote:\n");
        postContent.setText(post.getText());

        if(post.getReplyCount() != 1){
            replyHeader.setText(post.getReplyCount() + " Replies");
        }
        else{
            replyHeader.setText(post.getReplyCount() + " Reply");
        }

        user = FirebaseAuth.getInstance().getCurrentUser();

        if(post.getAuthorId().compareTo(user.getUid()) != 0){
            editButton.setVisibility(View.INVISIBLE);
        }

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder editDialog = new AlertDialog.Builder(v.getContext());
                EditText editTitle = new EditText(v.getContext());
                EditText editBody = new EditText(v.getContext());
                TextView editTitleText = new TextView(v.getContext());
                editTitleText.setText("\nTitle:");
                TextView editBodyText = new TextView(v.getContext());
                editBodyText.setText("\nBody:");

                LinearLayout ll = new LinearLayout(v.getContext());
                ll.setOrientation(LinearLayout.VERTICAL);
                ll.addView(editTitleText);
                ll.addView(editTitle);
                ll.addView(editBodyText);
                ll.addView(editBody);

                editTitle.setText(post.getTitle());
                editBody.setText(post.getText());

                editDialog.setTitle("Edit Post");
                editDialog.setView(ll);

                editDialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseReference = FirebaseDatabase.getInstance().getReference();
                        String newTitle = editTitle.getText().toString();
                        String newBody = editBody.getText().toString();
                        if(newTitle.length() > 0 && newBody.length() > 0){
                            databaseReference.child("posts").child(post.getId()).child("title").setValue(newTitle);
                            databaseReference.child("posts").child(post.getId()).child("text").setValue(newBody);
                            Toast.makeText(ViewPost.this, "Edit complete.", Toast.LENGTH_SHORT).show();
                            postTitle.setText(newTitle);
                            postContent.setText(newBody);
                        }
                        else if(newBody.length() == 0){
                            editBody.setError("Body cannot be blank");
                        }
                        else{
                            editTitle.setError("Title cannot be blank");
                        }
                    }
                });

                editDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // close
                    }
                });

                editDialog.create().show();
            }
        });

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
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("replyCount").setValue(0);

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
                    if(next.child("replyCount").exists()){
                        postReplies.add(new Reply(next.getKey().toString(), next.child("date").getValue().toString(), next.child("user").getValue().toString(), next.child("message").getValue().toString(), next.child("replyCount").getValue().toString()));
                    }
                    else{
                        postReplies.add(new Reply(next.getKey().toString(), next.child("date").getValue().toString(), next.child("user").getValue().toString(), next.child("message").getValue().toString()));
                    }
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

    public class ReplyHolder extends RecyclerView.ViewHolder implements View.OnClickListener, Serializable {
        public final TextView replyHeader;
        public final TextView replyContent;
        public final ConstraintLayout constraintLayout;
        public final ImageView replyToReplyButton;

        public Reply reply;

        public ReplyHolder(View itemView) {
            super(itemView);
            replyHeader = itemView.findViewById(R.id.reply_header);
            replyContent = itemView.findViewById(R.id.reply_text);
            constraintLayout = itemView.findViewById(R.id.reply_view);
            replyToReplyButton = itemView.findViewById(R.id.replytoreply_button);

            constraintLayout.setOnClickListener(this);
        }

        public void bind(Reply currentReply){
            reply = currentReply;

            replyHeader.setText("On " + reply.getDate() + ", " + reply.getUser() + " wrote:");
            replyContent.setText(reply.getMessage());

            replyHeader.setBackgroundColor(colors[colorCounter]);
            replyContent.setBackgroundColor(colors[colorCounter]);
            constraintLayout.setBackgroundTintList(ColorStateList.valueOf(colors[colorCounter]));

            colorCounter++;
            if (colorCounter == 5) colorCounter = 0;

            if (Integer.parseInt(reply.getReplyCount()) > 0){
                Log.d("REPLY", "In here...");
                ConstraintSet set = new ConstraintSet();
                TextView replyCountText = new TextView(ViewPost.this);
                replyCountText.setId(View.generateViewId());
                constraintLayout.addView(replyCountText, 0);

                set.clone(constraintLayout);
                set.connect(replyCountText.getId(), ConstraintSet.TOP, replyContent.getId(), ConstraintSet.BOTTOM, 8);
                set.applyTo(constraintLayout);

                replyCountText.setText("Tap to view " + reply.getReplyCount() + " replies");
                replyCountText.setTypeface(null, Typeface.ITALIC);
            }

            replyToReplyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText replyText = new EditText(v.getContext());

                    final AlertDialog.Builder addReplyDialog = new AlertDialog.Builder(v.getContext());
                    addReplyDialog.setTitle("New Reply");
                    addReplyDialog.setMessage("Reply to " + reply.getUser() + ":");
                    addReplyDialog.setView(replyText);

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

                            if(replyText.getText().toString().isEmpty()){
                                Toast.makeText(ViewPost.this, "Please write a reply before submitting.", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                databaseReference.child("posts").child(post.getId()).child("replies").child(reply.getId()).child("replies").child(replyId);
                                databaseReference.child("posts").child(post.getId()).child("replies").child(reply.getId()).child("replies").child(replyId).child("user").setValue(screenName);
                                databaseReference.child("posts").child(post.getId()).child("replies").child(reply.getId()).child("replies").child(replyId).child("date").setValue(formatDate);
                                databaseReference.child("posts").child(post.getId()).child("replies").child(reply.getId()).child("replies").child(replyId).child("dateSortable").setValue(formatDateSortable);
                                databaseReference.child("posts").child(post.getId()).child("replies").child(reply.getId()).child("replies").child(replyId).child("message").setValue(replyText.getText().toString().trim());
                                databaseReference.child("posts").child(post.getId()).child("replies").child(reply.getId()).child("replyCount").setValue(Integer.parseInt(reply.getReplyCount()) + 1);

                                Toast.makeText(ViewPost.this, "Reply Successfully Added!", Toast.LENGTH_SHORT).show();
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
                }});
        }

        @Override
        public void onClick(View v) {
            if(Integer.parseInt(reply.getReplyCount()) > 0){
                DatabaseReference newReference = databaseReference.child(reply.getId()).child("replies");
                Log.d("CLICK", newReference.toString());
                Intent intent = new Intent(ViewPost.this, RepliesToReply.class);
                Bundle bundle = new Bundle();
                bundle.putString("POST_ID", reply.getId());
                bundle.putString("POST_DATE", reply.getDate());
                bundle.putString("POST_AUTHOR", reply.getUser());
                bundle.putString("POST_CONTENT", reply.getMessage());
                bundle.putString("REPLY_COUNT", String.valueOf(reply.getReplyCount()));
                bundle.putString("REFERENCE", newReference.toString());
                intent.putExtras(bundle);
                startActivityForResult(intent, 1);
            }
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

    private class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;

            if (parent.getChildLayoutPosition(view) == 0) {
                outRect.top = space;
            } else {
                outRect.top = 0;
            }
        }
    }
}