package com.example.vinmod;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Resource extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource);
        ImageView rimage1 = (ImageView) findViewById(R.id.rimage1);
        int imageResourse = getResources().getIdentifier("@drawable/image1", null, this.getPackageName());
        rimage1.setImageResource(imageResourse);


    }
}