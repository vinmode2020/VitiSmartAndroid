package com.example.vinmod;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class NewPost extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private FirebaseUser user;

    Button publishBtn;
    Button cancelButton;

    EditText postTitle;
    EditText postContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        publishBtn = findViewById(R.id.publish_Btn);
        cancelButton = findViewById(R.id.cancel_btn);
        postTitle = findViewById(R.id.edit_title);
        postContent = findViewById(R.id.edit_body);


        // Click the Publish button
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!postTitle.getText().toString().isEmpty() && !postContent.getText().toString().isEmpty()){
                    SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy 'at' hh:mm:ss");
                    SimpleDateFormat dateSortable = new SimpleDateFormat("yyyyMMddHHmmss");

                    String formatDate = date.format(new Date());
                    String formatDateSortable = dateSortable.format(new Date());

                    user = FirebaseAuth.getInstance().getCurrentUser();

                    String screenName = user.getEmail().substring(0, user.getEmail().indexOf('@'));

                    Post newPost = new Post(
                            UUID.randomUUID().toString().replace("-", ""),
                            formatDate, postTitle.getText().toString(),
                            postContent.getText().toString().trim(),
                            screenName, 0
                    );

                    databaseReference = FirebaseDatabase.getInstance().getReference();

                    databaseReference.child("posts").child(newPost.getId());
                    databaseReference.child("posts").child(newPost.getId()).child("author").setValue(newPost.getUserName());
                    databaseReference.child("posts").child(newPost.getId()).child("date").setValue(newPost.getDate());
                    databaseReference.child("posts").child(newPost.getId()).child("dateSortable").setValue(formatDateSortable);
                    databaseReference.child("posts").child(newPost.getId()).child("replyCount").setValue(newPost.getReplyCount());
                    databaseReference.child("posts").child(newPost.getId()).child("text").setValue(newPost.getText());
                    databaseReference.child("posts").child(newPost.getId()).child("title").setValue(newPost.getTitle());

                    Toast.makeText(NewPost.this, "Post Successfully Published!", Toast.LENGTH_SHORT).show();

                    onBackPressed();
                }
                else if (postTitle.getText().toString().isEmpty()){
                    Toast.makeText(NewPost.this, "Please include a title.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(NewPost.this, "Please include a post body.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V){
                onBackPressed();
            }
        });




    }
}