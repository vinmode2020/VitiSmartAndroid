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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class MapImage extends AppCompatActivity {

    ImageView imageView;
    Button backButton;

    Drawable d = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_imageview);

        imageView = findViewById(R.id.map_image);
        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        new MyTask().execute();

        while (d == null){
            Log.d("MAPIMAGE", "waiting for background thread to finish");
        }

        Log.d("MAPIMAGE", "UI thread has resumed");
        imageView.setImageDrawable(d);
    }

    private class MyTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Bundle extras = getIntent().getExtras();
            Uri uriData = Uri.parse(extras.getString("IMAGE_URI"));
            try {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


    }
}
