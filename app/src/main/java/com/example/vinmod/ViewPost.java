package com.example.vinmod;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
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

/**
 * AppCompatActivity class that handles the Discussion Forum View Post Activity.
 * It is linked to the activity_view_post.xml layout file.
 */
public class ViewPost extends AppCompatActivity implements Serializable {

    //Firebase reference variables
    private FirebaseDatabase database = FirebaseDatabase.getInstance(); //Realtime database instance
    private DatabaseReference databaseReference; //Realtime database reference
    private FirebaseUser user; //Information for logged in user

    //Layout elements declaration
    RecyclerView replyList; //RecyclerView that displays the list of discussion post replies
    ReplyAdapter adapter; //Adapter for replyList
    TextView postTitle; //Post title text
    TextView postHeader;    //Post header information (timestamp and author)
    TextView postContent;   //Post body text
    TextView replyHeader;   //Header for replies section ("x replies", where x = no. of replies)
    Button addReplyButton;  //Button for posting a reply
    ImageView editButton;   //Pencil icon used for editing posts

    //Dynamic arraylist stores discussion post replies pulled from database
    private ArrayList<Reply> postReplies;

    //Array of colors used to give post reply headers their varying background colors
    int colorCounter = 0;
    int[] colors = {Color.argb(255, 86, 180, 233),
            Color.argb(255, 230, 159, 0),
            Color.argb(255, 0, 158, 115),
            Color.argb(255, 240, 228, 66),
            Color.argb(255, 204, 121, 167)};

    //Discussion post whose data is being displayed
    Post post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_post);

        //Get data from Discussion class instance that called ViewPost
        Bundle extras = getIntent().getExtras();

        //Store data for post tapped on in forum home into new Post object
        post = new Post(
                extras.getString("POST_ID"),
                extras.getString("POST_DATE"),
                extras.getString("POST_TITLE"),
                extras.getString("POST_CONTENT"),
                extras.getString("POST_AUTHOR"),
                Integer.parseInt(extras.getString("REPLY_COUNT")),
                extras.getString("AUTHOR_ID")
        );

        //Initialize layout elements from activity_view_post.xml
        postTitle = findViewById(R.id.post_title_details);
        postHeader = findViewById(R.id.post_header);
        postContent = findViewById(R.id.post_text);
        replyHeader = findViewById(R.id.replies_header);
        addReplyButton = findViewById(R.id.add_reply_button);
        replyList = findViewById(R.id.reply_list);
        editButton = findViewById(R.id.edit_button);

        //Add spacing between reply headers in RecyclerView
        int spacing = getResources().getDimensionPixelSize(R.dimen.nav_header_vertical_spacing);
        replyList.addItemDecoration(new SpacesItemDecoration(spacing));

        //Set TextViews to display appropriate strings
        postTitle.setText(post.getTitle());
        postHeader.setText("On " + post.getDate() + ", " + post.getUserName() + " wrote:\n");
        postContent.setText(post.getText());

        if(post.getReplyCount() != 1){
            replyHeader.setText(post.getReplyCount() + " Replies");
        }
        else{
            replyHeader.setText(post.getReplyCount() + " Reply");
        }

        //Get info on currently logged in user
        user = FirebaseAuth.getInstance().getCurrentUser();

        //If the logged in user is the post author, give them the ability to edit the post
        if(post.getAuthorId().compareTo(user.getUid()) != 0){
            editButton.setVisibility(View.INVISIBLE);
        }

        //If the edit (i.e. "pencil") icon is tapped on
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Dialog for prompting edit input
                final AlertDialog.Builder editDialog = new AlertDialog.Builder(v.getContext());
                EditText editTitle = new EditText(v.getContext());
                EditText editBody = new EditText(v.getContext());
                TextView editTitleText = new TextView(v.getContext());
                editTitleText.setText("\nTitle:");
                TextView editBodyText = new TextView(v.getContext());
                editBodyText.setText("\nBody:");

                //Create complex layout for dialog, needs to show fields for post title and body
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

                //If confirmed, update post in database with new title and body
                editDialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseReference = FirebaseDatabase.getInstance().getReference();
                        String newTitle = editTitle.getText().toString();
                        String newBody = editBody.getText().toString();
                        //Only makes changes if title and body are both populated
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

        //If user opts to add a reply
        addReplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //EditText for inputting reply content
                final EditText reply = new EditText(v.getContext());

                //Dialog for prompting input of reply
                final AlertDialog.Builder addReplyDialog = new AlertDialog.Builder(v.getContext());
                addReplyDialog.setTitle("New Reply");
                addReplyDialog.setMessage("Write a reply...");
                addReplyDialog.setView(reply);

                //If selected, posts reply to database
                addReplyDialog.setPositiveButton("Post", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Get time stamp
                        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy 'at' HH:mm:ss");
                        SimpleDateFormat dateSortable = new SimpleDateFormat("yyyyMMddHHmmss");
                        String formatDate = date.format(new Date());
                        String formatDateSortable = dateSortable.format(new Date());

                        //Get logged in user data and store username without "@--.com"
                        user = FirebaseAuth.getInstance().getCurrentUser();
                        String screenName = user.getEmail().substring(0, user.getEmail().indexOf('@'));

                        String replyId = UUID.randomUUID().toString().replace("-", "");

                        databaseReference = FirebaseDatabase.getInstance().getReference();

                        //Do not post reply if reply is blank
                        if(reply.getText().toString().isEmpty()){
                            Toast.makeText(ViewPost.this, "Please write a reply before submitting.", Toast.LENGTH_SHORT).show();
                        }
                        //Post reply in proper database path ("/posts/postID/replies/replyID")
                        else{
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId);
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("user").setValue(screenName);
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("date").setValue(formatDate);
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("dateSortable").setValue(formatDateSortable);
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("message").setValue(reply.getText().toString().trim());
                            databaseReference.child("posts").child(post.getId()).child("replyCount").setValue(post.getReplyCount() + 1);
                            databaseReference.child("posts").child(post.getId()).child("replies").child(replyId).child("replyCount").setValue(0);

                            Toast.makeText(ViewPost.this, "Reply Successfully Added!", Toast.LENGTH_SHORT).show();

                            //Update reply count in ViewPost UI
                            getIntent().putExtra("REPLY_COUNT", String.valueOf(post.getReplyCount() + 1));

                            //Refresh
                            finish();
                            startActivity(getIntent());
                        }

                    }
                });

                addReplyDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing
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

        //Initialize database reference at path "posts/postID/replies/"
        databaseReference = database.getReference("/posts/" + post.getId() + "/replies");

        //Gather post replies from database and put them in postReplies ArrayList
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

                //Order by newest first
                Collections.reverse(postReplies);

                //Set RecyclerView to display replies in ArrayList
                adapter = new ViewPost.ReplyAdapter(postReplies);
                replyList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        //Execute database query
        Query query = databaseReference.orderByChild("dateSortable");
        query.addListenerForSingleValueEvent(queryValueListener);

        replyList.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * ViewHolder class for replyList RecyclerView
     *
     * This class uses the list_item_reply.xml layout file to generate post headers for each reply
     * pulled from the database.
     */
    public class ReplyHolder extends RecyclerView.ViewHolder implements View.OnClickListener, Serializable {
        public final TextView replyHeader;
        public final TextView replyContent;
        public final ConstraintLayout constraintLayout;
        public final ImageView replyToReplyButton;  //Arrow icon that allows users to reply to replies

        //Reply to be displayed in next instance of list_item_reply.xml
        public Reply reply;

        //Constructor
        public ReplyHolder(View itemView) {
            super(itemView);
            replyHeader = itemView.findViewById(R.id.reply_header);
            replyContent = itemView.findViewById(R.id.reply_text);
            constraintLayout = itemView.findViewById(R.id.reply_view);
            replyToReplyButton = itemView.findViewById(R.id.replytoreply_button);

            //Set header to execute logic in click listener when clicked
            constraintLayout.setOnClickListener(this);
        }

        //Changes and establishes layout elements in current instance of list_item_reply to include
        //data specific to the current reply being considered
        public void bind(Reply currentReply){
            reply = currentReply;

            //Set text of TextViews to respective reply data and give it a background color
            replyHeader.setText("On " + reply.getDate() + ", " + reply.getUser() + " wrote:");
            replyContent.setText(reply.getMessage());
            replyHeader.setBackgroundColor(colors[colorCounter]);
            replyContent.setBackgroundColor(colors[colorCounter]);
            constraintLayout.setBackgroundTintList(ColorStateList.valueOf(colors[colorCounter]));

            //Iterate to next color in color list
            colorCounter++;
            if (colorCounter == 5) colorCounter = 0;

            //If the reply has its own replies, include element of reply header that discloses number of replies
            if (Integer.parseInt(reply.getReplyCount()) > 0){
                Log.d("REPLY", "In here...");

                //Create new constraintLayout element for the TextView to be added
                ConstraintSet set = new ConstraintSet();
                TextView replyCountText = new TextView(ViewPost.this);
                replyCountText.setId(View.generateViewId());
                constraintLayout.addView(replyCountText, 0);

                //Append TextView to bottom of reply header
                set.clone(constraintLayout);
                set.connect(replyCountText.getId(), ConstraintSet.TOP, replyContent.getId(), ConstraintSet.BOTTOM, 8);
                set.applyTo(constraintLayout);

                replyCountText.setText("Tap to view " + reply.getReplyCount() + " replies");
                replyCountText.setTypeface(null, Typeface.ITALIC);
            }

            //When reply to reply icon is selected
            replyToReplyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //EditText for inputting reply content
                    final EditText replyText = new EditText(v.getContext());

                    //Dialog for prompting reply-to-reply input
                    final AlertDialog.Builder addReplyDialog = new AlertDialog.Builder(v.getContext());
                    addReplyDialog.setTitle("New Reply");
                    addReplyDialog.setMessage("Reply to " + reply.getUser() + ":");
                    addReplyDialog.setView(replyText);

                    //Post new reply using identical logic to post reply
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
                            //Use database path "/posts/postID/replies/replyID/replies/replyToReplyID"
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

        //If reply header is clicked on, execute this listener
        @Override
        public void onClick(View v) {
            //If reply has reply, spin up RepliesToReply instance and pass necessary data into it
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
                //Pass current database path to RepliesToReply instance
                bundle.putString("REFERENCE", newReference.toString());
                intent.putExtras(bundle);
                startActivityForResult(intent, 1);
            }
        }
    }

    /**
     * Adapter class for replyList RecyclerView.
     *
     * This class applies the list_item_post instances created in ReplyHolder to the RecyclerView
     * layout element in activity_view_post.xml.
     */
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

    //Adds a decoration to each reply header that provides spacing between adjacent headers
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