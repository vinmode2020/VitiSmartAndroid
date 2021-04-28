package com.example.vinmod;

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

/**
 * AppCompatActivity class that handles the Discussion Forum New Post Activity.
 * It is linked to the activity_new_post.xml layout file.
 */
public class NewPost extends AppCompatActivity {

    //Firebase reference variables
    private DatabaseReference databaseReference;    //Database reference
    private FirebaseUser user;  //Info on currently logged in user

    //Variables for layout elements
    Button publishBtn;  //Button for publishing post
    Button cancelButton;    //Button for canceling publishing
    EditText postTitle; //ExitText for inputting title of post
    EditText postContent;   //EditText for inputting body of post

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        //Initialize layout variables
        publishBtn = findViewById(R.id.publish_Btn);
        cancelButton = findViewById(R.id.cancel_btn);
        postTitle = findViewById(R.id.edit_title);
        postContent = findViewById(R.id.edit_body);


        // Click the Publish button
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Publish post only if both title and body are supplied
                if(!postTitle.getText().toString().isEmpty() && !postContent.getText().toString().isEmpty()){
                    //Get displayable and sortable timestamp
                    SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy 'at' hh:mm:ss");
                    SimpleDateFormat dateSortable = new SimpleDateFormat("yyyyMMddHHmmss");
                    String formatDate = date.format(new Date());
                    String formatDateSortable = dateSortable.format(new Date());

                    //Get info on currently logged in user
                    user = FirebaseAuth.getInstance().getCurrentUser();
                    //Get user e-mail without "@--.com"
                    String screenName = user.getEmail().substring(0, user.getEmail().indexOf('@'));

                    //Create new post data object for the post to be added to database
                    Post newPost = new Post(
                            UUID.randomUUID().toString().replace("-", ""),
                            formatDate, postTitle.getText().toString(),
                            postContent.getText().toString().trim(),
                            screenName, 0, user.getUid()
                    );

                    //Get database reference
                    databaseReference = FirebaseDatabase.getInstance().getReference();

                    //Add post to database at path "posts"
                    databaseReference.child("posts").child(newPost.getId());
                    databaseReference.child("posts").child(newPost.getId()).child("authorId").setValue(newPost.getAuthorId());
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

        //If cancelled, return to forum home
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V){
                onBackPressed();
            }
        });




    }
}