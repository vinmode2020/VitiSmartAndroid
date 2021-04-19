package com.example.vinmod;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class ContactUs extends AppCompatActivity {
    EditText etSubject, etMessage;
    Button btSend;
    Button btMod;
    ProgressBar progressBar;

    boolean authFailed = false;
    boolean emailThreadComplete = false;
    boolean feedbackMessage = true;

    private FirebaseUser fbaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        fbaseUser = FirebaseAuth.getInstance().getCurrentUser();

        etSubject = findViewById(R.id.et_subject);
        etMessage = findViewById(R.id.et_Message);
        btSend = findViewById(R.id.bt_send);
        btMod = findViewById(R.id.bt_moderator);
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
                        btSend.setEnabled(false);
                        while(!emailThreadComplete){
                            Log.d("AUTH", "Stuck in here...");
                        }
                        if(authFailed){
                            btSend.setEnabled(true);
                            Toast.makeText(ContactUs.this, "SMTP Authentication Failed. navigating to email app...", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
                            intent.setData(Uri.parse("mailto:vinmode2020@gmail.com"));
                            intent.putExtra(Intent.EXTRA_SUBJECT, etSubjectText);
                            intent.putExtra(Intent.EXTRA_TEXT, etMessageText);
                            try {
                                // startActivity(intent);
                                startActivity(Intent.createChooser(intent, "Send email using..."));
                            } catch (android.content.ActivityNotFoundException ex) {
                                Toast.makeText(ContactUs.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else{
                            Toast.makeText(ContactUs.this, "Message Sent!", Toast.LENGTH_SHORT).show();
                            onBackPressed();
                        }
                    }
                    else{
                        Toast.makeText(ContactUs.this, "Message failed to send, make sure you have a reliable internet connection.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btMod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder modDialog = new AlertDialog.Builder(v.getContext());

                modDialog.setTitle("New Moderator Request");
                modDialog.setMessage("Users with moderator status are tasked with overseeing the app's discussion forum, using their discretion to remove posts and ban users who use the forum inappropriately. Apply to become a moderator by tapping \"Send Moderator Request\" below.");

                modDialog.setPositiveButton("Send Moderator Request", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(isOnline()){
                            feedbackMessage = false;
                            EmailThread emailThread = new EmailThread();
                            emailThread.start();
                            btSend.setEnabled(false);
                            while(!emailThreadComplete){
                                Log.d("AUTH", "Stuck in here...");
                            }
                            if(authFailed){
                                btSend.setEnabled(true);
                                Toast.makeText(ContactUs.this, getApplicationContext().getFilesDir().toString(), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
                                intent.setData(Uri.parse("mailto:vinmode2020@gmail.com"));
                                intent.putExtra(Intent.EXTRA_SUBJECT, "Moderator Request");
                                intent.putExtra(Intent.EXTRA_TEXT, "This user wants to be moderator.");
                                try {
                                    startActivity(Intent.createChooser(intent, "Send email using..."));
                                } catch (android.content.ActivityNotFoundException ex) {
                                    Toast.makeText(ContactUs.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else{
                                Toast.makeText(ContactUs.this, "Request Sent!", Toast.LENGTH_SHORT).show();
                                onBackPressed();
                            }
                        }
                        else{
                            Toast.makeText(ContactUs.this, "Message failed to send, make sure you have a reliable internet connection.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                modDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //close
                    }
                });

                modDialog.create().show();
            }
        });
    }

    private class EmailThread extends Thread{
        public void run() {
            GMailSender gMailSender = new GMailSender("VitiSmartSender@gmail.com", "wnxnznupbcoccggu", etSubject.getText().toString(), etMessage.getText().toString());
            gMailSender.sendMail();
            emailThreadComplete = true;
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

    public class GMailSender extends javax.mail.Authenticator{
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
                if(feedbackMessage){
                    message.setSubject("Feedback from " + fbaseUser.getEmail().substring(0, fbaseUser.getEmail().indexOf('@')));
                    message.setText("A message has been sent from VitiSmart by " + fbaseUser.getEmail().substring(0, fbaseUser.getEmail().indexOf('@')) + " regarding the following:\n\n" +
                            "Subject: " + subject + "\n\n" + "Message:\n" + text + "\n\nReply at: " + fbaseUser.getEmail());
                }
                else{
                    MimeBodyPart bodyPart1 = new MimeBodyPart();
                    bodyPart1.setText("This user has made a request to be granted moderator status. Below are some key user details:\n" +
                            "   • Email address: " + fbaseUser.getEmail() + "\n" +
                            "   • UID: " + fbaseUser.getUid() + "\n\n" +
                            "Currently, automatic granting of moderator status is not supported and must be done manually in Firebase. In order to complete this, login to firebase and navigate to the \"users\" collection in Firestore, locate the document named after this user's UID, create a new attribute \"moderator\" and give it a value of 1.\n\n" +
                            "Open this link for a visual instruction: https://firebasestorage.googleapis.com/v0/b/vinmode-144a9.appspot.com/o/howtoscreenshot.PNG?alt=media&token=1615e5dd-7e52-4056-8dd5-92017e1a86bf");
                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(bodyPart1);
                    message.setSubject("Moderator request from " + fbaseUser.getEmail().substring(0, fbaseUser.getEmail().indexOf('@')));
                    message.setContent(multipart);
                }

                Transport.send(message);
            } catch(MessagingException me){
                me.printStackTrace();
                authFailed = true;
            }
        }
    }
}