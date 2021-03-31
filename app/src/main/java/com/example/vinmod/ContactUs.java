package com.example.vinmod;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class ContactUs extends AppCompatActivity {
    EditText etSubject, etMessage;
    Button btSend;
    ProgressBar progressBar;

    private FirebaseUser fbaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        fbaseUser = FirebaseAuth.getInstance().getCurrentUser();

        etSubject = findViewById(R.id.et_subject);
        etMessage = findViewById(R.id.et_Message);
        btSend = findViewById(R.id.bt_send);
        progressBar = findViewById(R.id.contact_progressbar);

        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                String etSubjectText = etSubject.getText().toString();
                String etMessageText = etMessage.getText().toString();
                if(etSubjectText.isEmpty()){
                    etSubject.setError("Field cannot be blank");
                }
                if(etMessageText.isEmpty()){
                    etMessage.setError("Field cannot be blank");
                }
                if(!etSubjectText.isEmpty() && !etMessageText.isEmpty()){
                    if(isOnline()){
                        EmailThread emailThread = new EmailThread();
                        emailThread.start();
                        Toast.makeText(ContactUs.this, "Message Sent!", Toast.LENGTH_SHORT).show();
                        emailThread.interrupt();
                        onBackPressed();
                    }
                    else{
                        Toast.makeText(ContactUs.this, "Message failed to send, make sure you have a reliable internet connection.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private class EmailThread extends Thread{
        public void run() {
            GMailSender gMailSender = new GMailSender("vinmode2020@gmail.com", "pennstate2020", etSubject.getText().toString(), etMessage.getText().toString());
            gMailSender.sendMail();
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    private class GMailSender extends javax.mail.Authenticator{
        private Session session;
        private String user;
        private String pword;
        private String subject;
        private String text;

        public GMailSender(String user, String password, String sub, String mes){
            this.user = user;
            this.pword = password;
            this.subject = sub;
            this.text = mes;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, pword);
        }

        public synchronized void sendMail(){
            try{
                Properties properties = new Properties();
                properties.setProperty("mail.transport.protocol", "smtp");
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");
                properties.put("mail.smtp.host", "smtp.gmail.com");
                properties.put("mail.smtp.socketFactory.class",
                        "javax.net.ssl.SSLSocketFactory");
                properties.put("mail.smtp.port", "465");
                properties.put("mail.smtp.socketFactory.port", "465");

                session = Session.getDefaultInstance(properties, this);
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fbaseUser.getEmail()));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse("vinmode2020@gmail.com"));
                message.setSubject("Feedback from " + fbaseUser.getEmail().substring(0, fbaseUser.getEmail().indexOf('@')));
                message.setText("A message has been sent from VitiSmart by " + fbaseUser.getEmail().substring(0, fbaseUser.getEmail().indexOf('@')) + " regarding the following:\n\n" +
                        "Subject: " + subject + "\n\n" + "Message:\n" + text + "\n\nReply at: " + fbaseUser.getEmail());

                Transport.send(message);
            } catch(MessagingException me){
                me.printStackTrace();
            }
        }
    }
}