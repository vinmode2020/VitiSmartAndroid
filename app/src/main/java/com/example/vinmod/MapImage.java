package com.example.vinmod;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * AppCompatActivity class that handles the Displaying of images in the Infestation Map.
 * It is linked to the map_imageview.xml layout file.
 */
public class MapImage extends AppCompatActivity {

    //Layout Elements
    ImageView imageView;    //For displaying the image
    Button backButton;  //For returning to the Infestation Map

    //Will hold image fetched from Firebase
    Drawable d = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_imageview);

        //Initialize layout elements
        imageView = findViewById(R.id.map_image);
        backButton = findViewById(R.id.back_button);

        //Listener for "Back" button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //Execute the downloading of the image from Firebase on a background thread,
        //since network activities cannot be executed on main thread
        new DownloadTask().execute();

        //Wait for download thread to finish
        while (d == null){
            Log.d("MAPIMAGE", "waiting for background thread to finish");
        }

        Log.d("MAPIMAGE", "UI thread has resumed");

        //Set the ImageView to display the fetched image
        imageView.setImageDrawable(d);
    }

    /**
     * Background thread that fetches the requested image from Firebase
     * cloud storage
     */
    private class DownloadTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            //Get the image URI from the Infestation Map instance that called this MapImage instance
            Bundle extras = getIntent().getExtras();
            Uri uriData = Uri.parse(extras.getString("IMAGE_URI"));
            //Try to grab the image from Firebase
            try {
                //Get image InputStream and attempt to create a Drawable copy of it
                InputStream is = (InputStream) new URL(uriData.toString()).getContent();
                d = Drawable.createFromStream(is, "MAP_IMAGE");
                Log.d("MAPIMAGE", "background thread has finished");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

}
