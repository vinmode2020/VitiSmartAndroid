package com.example.vinmod;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

//import android.support.v4.view.ViewPager;


public class Resource extends AppCompatActivity {

    //private final float MIN_SCALE = 0.70f;
    //private final float MIN_ALPHA = 0.50f;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource);

        /* int pagewidth = view.getWidth();
        // int pagewidth = view.getHeight();

        if (position <-1){
            view.setAlpha(0f);
        }

        else if (position <= 1) {

        }
*/

        ImageView rimage1 = (ImageView) findViewById(R.id.rimage1);
        int imageResourse = getResources().getIdentifier("@drawable/image1", null, this.getPackageName());
        rimage1.setImageResource(imageResourse);


    }
}