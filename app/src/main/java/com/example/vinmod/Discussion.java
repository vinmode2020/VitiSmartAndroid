package com.example.vinmod;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Discussion extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button newPost_Btn;
        newPost_Btn = findViewById(R.id.newPost_Btn);


        // Click the Discussion board button
        newPost_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Discussion.this, NewPost.class);

            }
        });


    }
}