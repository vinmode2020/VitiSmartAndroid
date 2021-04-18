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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

public class RepliesToReply extends AppCompatActivity {

    Reply rootReply;
    RecyclerView replyList;
    Button backButton;

    private FirebaseUser user;
    private DatabaseReference databaseReference;

    private ArrayList<Reply> postReplies;
    private ReplyAdapter adapter;

    int colorCounter = 0;

    int colors[] = {Color.argb(255, 86, 180, 233),
            Color.argb(255, 230, 159, 0),
            Color.argb(255, 0, 158, 115),
            Color.argb(255, 240, 228, 66),
            Color.argb(255, 204, 121, 167)};



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replies_to_reply);

        replyList = findViewById(R.id.reply_list2);
        backButton = findViewById(R.id.back_button3);

        int spacing = getResources().getDimensionPixelSize(R.dimen.nav_header_vertical_spacing);
        replyList.addItemDecoration(new SpacesItemDecoration(spacing));


        Bundle extras = getIntent().getExtras();

        rootReply = new Reply(
                extras.getString("POST_ID"),
                extras.getString("POST_DATE"),
                extras.getString("POST_AUTHOR"),
                extras.getString("POST_CONTENT"),
                extras.getString("REPLY_COUNT")
        );

        databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl(extras.getString("REFERENCE"));

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        postReplies = new ArrayList<Reply>();

        ValueEventListener queryValueListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                Iterator<DataSnapshot> iterator = snapshotIterator.iterator();

                Log.d("CLICK", dataSnapshot.toString());

                while (iterator.hasNext()) {
                    Log.d("CLICK", "Hello");
                    DataSnapshot next = (DataSnapshot) iterator.next();
                    if(next.child("replyCount").exists()){
                        postReplies.add(new Reply(next.getKey().toString(), next.child("date").getValue().toString(), next.child("user").getValue().toString(), next.child("message").getValue().toString(), next.child("replyCount").getValue().toString()));
                    }
                    else{
                        postReplies.add(new Reply(next.getKey().toString(), next.child("date").getValue().toString(), next.child("user").getValue().toString(), next.child("message").getValue().toString()));
                    }
                }

                Collections.reverse(postReplies);

                adapter = new RepliesToReply.ReplyAdapter(postReplies);

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

    public class ReplyHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
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
                TextView replyCountText = new TextView(RepliesToReply.this);
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

                            if(replyText.getText().toString().isEmpty()){
                                Toast.makeText(RepliesToReply.this, "Please write a reply before submitting.", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                databaseReference.child(reply.getId()).child("replies").child(replyId);
                                databaseReference.child(reply.getId()).child("replies").child(replyId).child("user").setValue(screenName);
                                databaseReference.child(reply.getId()).child("replies").child(replyId).child("date").setValue(formatDate);
                                databaseReference.child(reply.getId()).child("replies").child(replyId).child("dateSortable").setValue(formatDateSortable);
                                databaseReference.child(reply.getId()).child("replies").child(replyId).child("message").setValue(replyText.getText().toString().trim());
                                databaseReference.child(reply.getId()).child("replyCount").setValue(Integer.parseInt(reply.getReplyCount()) + 1);

                                Toast.makeText(RepliesToReply.this, "Reply Successfully Added!", Toast.LENGTH_SHORT).show();
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
                Intent intent = new Intent(RepliesToReply.this, RepliesToReply.class);
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



    public class ReplyAdapter extends RecyclerView.Adapter<RepliesToReply.ReplyHolder> {

        private ArrayList<Reply> replyArrayList;

        public ReplyAdapter(ArrayList<Reply> x){

            replyArrayList = postReplies;
        }

        @NonNull
        @Override
        public RepliesToReply.ReplyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_reply, parent, false);

            return new RepliesToReply.ReplyHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RepliesToReply.ReplyHolder holder, int position) {
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
