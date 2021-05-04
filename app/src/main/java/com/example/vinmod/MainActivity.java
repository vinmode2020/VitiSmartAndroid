package com.example.vinmod;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * AppCompatActivity class that handles the Construction of the Home Page.
 * It is linked to the activity_main.xml layout file.
 */
public class MainActivity extends AppCompatActivity {

    //Variables for layout elements
    TextView fullName;  //Displays name of current user
    TextView verifyMsg; //Message notifying user if e-mail has not been verified
    Button resendCode;  //Button for resending verification e-mail
    Button contBtn; //For navigating to Contact Us page
    Button aboutUsBtn;  //For navigating to About Us page
    Button rPage;   //For navigating to Resource page
    Button scan;    //For navigating to Cluster Capture menu
    Button map; //For navigating to Infestation Map
    Button dBoard;  //For navigating to the Discussion Board
    Button resetPassLocal;  //For resetting the current user's password
    Button logout;  //For logging out

    //Firebase reference variables
    FirebaseAuth fAuth; //Reference to Firebase authentication
    FirebaseFirestore fStore;   //Reference to Cloud FireStore
    FirebaseUser user;  //To store info on currently logged in user
    StorageReference storageReference;  //Reference to Firebase Cloud Storage

    //Stores whether or not currently logged in user is banned from the Discussion Forum
    boolean bannedFromDF = false;
    String reasonForBan;

    //Stores currently logged in user UID
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize layout elements
        fullName = findViewById(R.id.profileName);
        resetPassLocal = findViewById(R.id.resetPasswordLocal);
        rPage = findViewById(R.id.button_rpage);
        scan = findViewById(R.id.button_scan);
        logout = findViewById(R.id.button_logout);
        map = findViewById(R.id.button_map);
        dBoard = findViewById(R.id.button_dboard);
        contBtn = findViewById(R.id.contBtn);
        aboutUsBtn = findViewById(R.id.aboutUs_Btn);
        resendCode = findViewById(R.id.resendCode);
        verifyMsg = findViewById(R.id.verifyMsg);

        //Initialize Firebase references
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        //Get current user data
        userId = fAuth.getCurrentUser().getUid();
        user = fAuth.getCurrentUser();

        //If the user does not have a verified e-mail, notify them that this is the case
        //and present option to resend verification message
        if(!user.isEmailVerified()){
            verifyMsg.setVisibility(View.VISIBLE);
            resendCode.setVisibility(View.VISIBLE);

            resendCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {

                    user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(v.getContext(), "Verification Email Has been Sent.", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("tag", "onFailure: Email not sent " + e.getMessage());
                        }
                    });
                }
            });
        }

        //Query Cloud FireStore to check if currently logged in user is banned from Discussion Forum
        DocumentReference documentReference = fStore.collection("users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot != null){
                    fullName.setText(documentSnapshot.getString("fName"));
                    //If the user has a "banned" attribute, they are banned
                    if(documentSnapshot.get("banned") != null){
                        bannedFromDF = true;
                        reasonForBan = documentSnapshot.get("bannedReason").toString();
                    }
                }else {
                    Log.d("DOC_SNAPSHOT", "onEvent: Document does not exist");
                }
            }
        });

        //Click listener for "Reset Password" button
        resetPassLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //EditText for inputting new password
                final EditText resetPassword = new EditText(v.getContext());

                //Dialog for prompting user to input new password
                final AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(v.getContext());
                passwordResetDialog.setTitle("Reset Password ?");
                passwordResetDialog.setMessage("Enter New Password > 6 Characters long.");
                passwordResetDialog.setView(resetPassword);

                passwordResetDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Extract the email and send reset link
                        String newPassword = resetPassword.getText().toString();
                        //Update the currently logged in user's password
                        user.updatePassword(newPassword).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(MainActivity.this, "Password Reset Successfully.", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "Password Reset Failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                passwordResetDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing
                    }
                });
                //Display dialog
                passwordResetDialog.create().show();
            }
        });

        //Click the Resource page button
        rPage.setOnClickListener(new View.OnClickListener(){
        @Override
        public void onClick (View v){
        Intent intent = new Intent(MainActivity.this, Resource.class);
        startActivity(intent);
        }
        });

        //Click the "Upload Cluster" button
        scan.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v){
                Intent intent = new Intent(MainActivity.this, Scan.class);
                startActivity(intent);
            }
        });

        //Click the Logout button
        logout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v){
                //Sign the user out and return to Login page
                Intent intent = new Intent(MainActivity.this, Login.class);
                FirebaseAuth.getInstance().signOut();
                  startActivity(intent);
            }
        });

        //Click the scan button
        map.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v){
                Intent intent = new Intent(MainActivity.this, Map.class);
               // FirebaseAuth.getInstance().signOut();
                startActivity(intent);
            }
        });

        //Click the Discussion Board button
        dBoard.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v){
                //If the user is not banned, navigate to discussion forum
                if(!bannedFromDF){
                    Intent intent = new Intent(MainActivity.this, Discussion.class);
                    //FirebaseAuth.getInstance().signOut();
                    startActivity(intent);
                }
                //If the user is banned, display notification of ban and prevent forum access
                else{
                    AlertDialog.Builder banNotice = new AlertDialog.Builder(MainActivity.this);
                    banNotice.setTitle("DF BAN NOTICE");
                    banNotice.setMessage("You have been banned from the app discussion forum for the following reason:\n\n" + reasonForBan + "\n\nThis means you may no longer view or post in the VitiSmart discussion forum." +
                            " If you believe this was done in error, please let us know at vinmode2020@gmail.com or use the \"Contact Us\" option on the home page.");
                    banNotice.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //close
                        }
                    });
                    banNotice.create().show();
                }
            }
        });

        //Click the Contact Us button
        contBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v){
                if(!user.isEmailVerified()){
                    Toast.makeText(MainActivity.this, "ERROR: Must verify e-mail before using \"Contact Us\".", Toast.LENGTH_SHORT).show();
                }
                else{
                    Intent intent = new Intent(MainActivity.this, ContactUs.class);
                    startActivity(intent);
                }
            }
        });

        //Click the About Us button
        aboutUsBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v){
                Intent intent = new Intent(MainActivity.this, AboutUs.class);
                startActivity(intent);
            }
        });

    }

    //Pressing the back button here logs out the user in some cases, so this dialog checks to make sure the
    //user is aware of that and prompts them to confirm that is what they want to do
    @Override
    public void onBackPressed() {
        final AlertDialog.Builder addReplyDialog = new AlertDialog.Builder(this);
        addReplyDialog.setMessage("Really logout?");

        addReplyDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.super.onBackPressed();
            }
        });

        addReplyDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        addReplyDialog.create().show();
    }
}