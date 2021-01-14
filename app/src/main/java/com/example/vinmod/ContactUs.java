package com.example.vinmod;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ContactUs extends AppCompatActivity {

    EditText etSubject, etMessage;
    Button btSend;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        etSubject = findViewById(R.id.et_subject);
        etMessage = findViewById(R.id.et_Message);
        btSend = findViewById(R.id.bt_send);


        btSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick (View v){
               // Intent intent = new Intent(Intent.ACTION_VIEW
                 //       , Uri.parse("mailto: vinmode2020@gmail.com" ));

                    Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
                    intent.setData(Uri.parse("mailto:vinmode2020@gmail.com"));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "this is my Subject of email");
                    intent.putExtra(Intent.EXTRA_TEXT, etMessage.getText().toString());


                    try {
                       // startActivity(intent);
                        startActivity(Intent.createChooser(intent, "Send email using..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(ContactUs.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }

            }
        });
    }
}