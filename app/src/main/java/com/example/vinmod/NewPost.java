package com.example.vinmod;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class NewPost extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);


        String postBody = null, postSubject;
        Button publish_Btn;
        publish_Btn = findViewById(R.id.publish_Btn);


        // Click the Publish button
        publish_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewPost.this, Discussion.class);

                // get the text from the new post once its published
                // postBody.getText().toString();
                // String content = postBody.getText().toString(); //gets you the contents of edit text

                // upload the text here



            }
        });






    }
}