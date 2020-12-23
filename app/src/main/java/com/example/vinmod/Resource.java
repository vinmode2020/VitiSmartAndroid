package com.example.vinmod;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

//import android.support.v4.view.ViewPager;
//import android.support.v7.app.AppCompatActivity;

public class Resource extends AppCompatActivity  {

    @Override
    protected void onCreate (Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_resource);

    // This is the old version below!
    // ImageView rimage1 = (ImageView) findViewById(R.id.rimage1);
    // int imageResourse = getResources().getIdentifier("@drawable/image1", null, this.getPackageName());
    // rimage1.setImageResource(imageResourse);

    // New Version
        ViewPager viewPager = findViewById(R.id.viewPager);
        ImageAdapter adapter = new ImageAdapter(this);
        viewPager.setAdapter(adapter);

    }
}



