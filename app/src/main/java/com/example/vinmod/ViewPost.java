package com.example.vinmod;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class ViewPost extends AppCompatActivity {

    TextView postTitle;
    TextView postHeader;
    TextView postContent;

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
                extras.getString("POST_AUTHOR"), 0
        );

        postTitle = findViewById(R.id.post_title_details);
        postHeader = findViewById(R.id.post_header);
        postContent = findViewById(R.id.post_text);

        postTitle.setText(post.getTitle());
        postHeader.setText("On " + post.getDate().substring(0, 10) + ", " + post.getUserName() + " wrote:\n");
        postContent.setText(post.getText());
    }
}