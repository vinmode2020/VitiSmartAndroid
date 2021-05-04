package com.example.vinmod;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutUs extends AppCompatActivity {

    TextView posterLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        posterLink = findViewById(R.id.link);
        posterLink.setMovementMethod(LinkMovementMethod.getInstance());
        }

    // more about the project
    public void onClick(View v){
        // This link should be changed eventually to the project's website
        String url = "https://firebasestorage.googleapis.com/v0/b/vinmode-144a9.appspot.com/o/PSU-BD-CSSE-Class2021-Sec-001-Team-014-11.pdf?alt=media&token=f08808a7-7ffe-412b-9a01-1e9da27dac07";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

}





