package com.example.vinmod;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * This class handles the Discussion Forum Home Page Activity.
 * It is linked to the activity_discussion.xml layout file.
*/
public class Discussion extends AppCompatActivity {

    //Layout elements declaration
    Button newPostBtn;  //Button for creating a new post
    Button searchButton;    //Button for searching the discussion forum
    RecyclerView postList;  //RecyclerView that displays the list of discussion posts
    Spinner sortOptions;    //Spinner that displays the post sorting options when tapped on

    //Firebase reference variables
    FirebaseDatabase database = FirebaseDatabase.getInstance(); //Realtime database instance
    DatabaseReference dbRef = database.getReference("/posts");  //Realtime database reference at location of discussion posts
    FirebaseUser user;  //Information for logged in user
    FirebaseFirestore fStore;   //Cloud Firestore instance
    DocumentReference documentReference;    //Cloud Firestore document reference

    Toast errorToast;   //Used to display error messages

    //Boolean values used for conditional logic throughout class
    Boolean isDescending = true;
    Boolean isModerator = false;

    //Boolean values used to control race conditions
    Boolean emailThreadComplete = false;

    //Adapter for postList
    PostAdapter adapter;

    //Dynamic arraylist stores discussion posts pulled from database
    ArrayList<Post> discussionPosts = new ArrayList<Post>();

    //Array of colors used to give post headers their varying background colors
    int colorCounter = 0;
    int[] colors = {Color.argb(255, 86, 180, 233),
            Color.argb(255, 230, 159, 0),
            Color.argb(255, 0, 158, 115),
            Color.argb(255, 240, 228, 66),
            Color.argb(255, 204, 121, 167)};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion);

        //Initialize layout elements
        newPostBtn = findViewById(R.id.newPost_Btn);
        searchButton = findViewById(R.id.search_btn);
        postList = findViewById(R.id.post_list);

        //Add spacing between post headers in RecyclerView
        int spacing = getResources().getDimensionPixelSize(R.dimen.nav_header_vertical_spacing);
        postList.addItemDecoration(new SpacesItemDecoration(spacing));

        createSpinner();

        user = FirebaseAuth.getInstance().getCurrentUser();
        fStore = FirebaseFirestore.getInstance();
        documentReference = fStore.collection("users").document(user.getUid());

        //Check if the currently logged in user is a moderator
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot.get("moderator") != null){
                    isModerator = true;
                }else {
                    Log.d("DOC_SNAPSHOT", "onEvent: Document does not exist");
                }
            }
        });

        // Listener for "new post" button
        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Navigate to view for creating a new post
                Intent intent = new Intent(Discussion.this, NewPost.class);
                startActivityForResult(intent, 1);
            }
        });

        //Listener for "Search" button
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //EditText for inputting search key
                final EditText search = new EditText(v.getContext());

                //Dialog used to prompt input of search key
                final AlertDialog.Builder searchDialog = new AlertDialog.Builder(v.getContext());
                searchDialog.setTitle("New Search");
                searchDialog.setMessage("Enter Search Key...");
                searchDialog.setView(search);

                //User confirms search
                searchDialog.setPositiveButton("Search", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<Post> filteredPosts = new ArrayList<>();
                        if (search.getText().length() > 0){
                            //Check each discussion post to see if its title or body contains the search key
                            //(Not case-sensitive)
                            for (Post x:discussionPosts
                                 ) {
                                if(x.getTitle().toLowerCase().contains(search.getText().toString().toLowerCase()) || x.getText().toLowerCase().contains(search.getText().toString().toLowerCase())){
                                    filteredPosts.add(x);
                                    Log.d("SEARCH", x.toString());
                                }
                            }

                            //Reset postList to only show posts that contain search key
                            adapter = new PostAdapter(filteredPosts);
                            postList.setAdapter(adapter);

                            Log.d("SEARCH", filteredPosts.toString());
                        }
                    }
                });
                //User cancels search
                searchDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing
                    }
                });

                searchDialog.create().show();
            }
        });
    }

    //ValueListener for collecting post data from the database
    ValueEventListener queryValueListener = new ValueEventListener() {

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
            Iterator<DataSnapshot> iterator = snapshotIterator.iterator();

            //For each post in the discussion forum
            while (iterator.hasNext()) {
                DataSnapshot next = (DataSnapshot) iterator.next();
                //Add post object to post list with info from database
                discussionPosts.add(new Post(next.getKey().toString(),
                        next.child("date").getValue().toString(),
                        next.child("title").getValue().toString(),
                        next.child("text").getValue().toString(),
                        next.child("author").getValue().toString(),
                        Integer.parseInt(next.child("replyCount").getValue().toString()),
                        (next.child("authorId").exists()) ? next.child("authorId").getValue().toString() : "N/A"));
            }

            //If user opted to sort posts by newest first
            if(isDescending){
                Collections.reverse(discussionPosts);
            }

            //Set post list to display newly fetched posts
            adapter = new PostAdapter(discussionPosts);
            postList.setAdapter(adapter);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            //Do nothing
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        discussionPosts = new ArrayList<Post>();
        postList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            finish();
            startActivity(getIntent());
        }
    }

    //Executed when a moderator opts to ban user
    public void onBanUserClick(View v, String userName, String userUID){
        //Dialog for prompting moderator to confirm ban and input reason for ban
        AlertDialog.Builder banDialog = new AlertDialog.Builder(v.getContext());

        banDialog.setTitle("Ban " + userName);

        //For ban reason inputting
        EditText reasonText = new EditText(Discussion.this);
        banDialog.setMessage("Supply a reason for removing the post: ");
        banDialog.setView(reasonText);

        //If moderator confirms the ban
        banDialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Ban does not execute if not reason provided
                if(reasonText.getText().length() > 0){
                    Map<String, Object> docData = new HashMap<>();
                    docData.put("banned", "1");
                    docData.put("bannedReason", reasonText.getText().toString());

                    //Give user banned attribute in respective Cloud Firestore Document
                    fStore.collection("users").document(userUID).set(docData, SetOptions.merge());

                    Toast.makeText(Discussion.this, "User has been banned.", Toast.LENGTH_SHORT).show();

                    //Refresh
                    finish();
                    startActivity(getIntent());
                }
                else{
                    errorToast = Toast.makeText(Discussion.this, "User not banned; you must supply a reason for the ban.", Toast.LENGTH_SHORT);
                    View view = errorToast.getView();
                    view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    errorToast.show();
                }
            }
        });
        banDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Do nothing
            }
        });

        banDialog.create().show();
    }

    //Executed when moderator opts to unban previously banned user
    public void onUnbanUserClick(View v, String userName, String userID){
        //Dialog used to prompt confirmation of unbanning
        AlertDialog.Builder unbanDialog = new AlertDialog.Builder(v.getContext());

        unbanDialog.setTitle("Ban " + userName);
        unbanDialog.setMessage("Are you sure you want to unban " + userName + "?");

        //If moderator confirms unbanning
        unbanDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String, Object> docData = new HashMap<>();
                docData.put("banned", FieldValue.delete());
                docData.put("bannedReason", FieldValue.delete());

                //Remove banned attribute for respective user in Cloud Firestore
                documentReference = fStore.collection("users").document(userID);
                documentReference.update(docData);

                Toast.makeText(Discussion.this, "User has been unbanned.", Toast.LENGTH_SHORT).show();

                finish();
                startActivity(getIntent());
            }
        });
        unbanDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Do nothing
            }
        });

        unbanDialog.create().show();
    }

    /**
     * ViewHolder class for postList RecyclerView
     *
     * This class uses the list_item_post.xml layout file to generate post headers for each post
     * pulled from the database.
     */
    public class PostHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public final TextView postTitle;
        public final TextView postAuthor;
        public final TextView replyCount;
        public final TextView replyText;
        public final TextView deleteBtn;
        public final ImageView banIcon;
        public final ConstraintLayout postListItem;

        //Post to be displayed in next header
        public Post post;

        //Constructor
        public PostHolder(View itemView) {
            super(itemView);
            postTitle = itemView.findViewById(R.id.postName);
            postAuthor = itemView.findViewById(R.id.post_author);
            replyCount = itemView.findViewById(R.id.reply_count);
            replyText = itemView.findViewById(R.id.textView8);
            deleteBtn = itemView.findViewById(R.id.delete_post);
            banIcon = itemView.findViewById(R.id.ban_icon);
            postListItem = itemView.findViewById(R.id.postListItem);

            //Set layout elements to execute logic in click listener when clicked
            postTitle.setOnClickListener(this);
            postAuthor.setOnClickListener(this);
            replyCount.setOnClickListener(this);
            replyText.setOnClickListener(this);
        }

        //Changes and establishes layout elements in current instance of list_item_post to include
        //data specific to the current post being considered
        public void bind(Post currentPost){
            post = currentPost;
            documentReference = fStore.collection("users").document(post.getAuthorId());

            //Set text of TextViews to respective post data and give it a background color
            postTitle.setText(post.getTitle());
            postTitle.setBackgroundColor(colors[colorCounter]);
            postAuthor.setText("By " + post.getUserName());
            postAuthor.setBackgroundColor(colors[colorCounter]);
            replyCount.setText(Integer.toString(post.getReplyCount()));
            replyCount.setBackgroundColor(colors[colorCounter]);
            replyText.setBackgroundColor(colors[colorCounter]);
            postListItem.setBackgroundTintList(ColorStateList.valueOf(colors[colorCounter]));

            //By default, cannot see ban option
            banIcon.setVisibility(View.INVISIBLE);

            //Iterate to next color in color list
            colorCounter++;
            if (colorCounter == 5) colorCounter = 0;

            //Check if current post author has any special user statuses
            documentReference.addSnapshotListener(Discussion.this, new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                    //If the post author is banned
                    if(documentSnapshot.get("banned") != null){
                        //Give proper color coding and special character indicator
                        postAuthor.append(" ×");
                        Spannable spannable = new SpannableString(postAuthor.getText().toString());
                        spannable.setSpan(new ForegroundColorSpan(Color.DKGRAY), 3, postAuthor.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        StyleSpan bold = new StyleSpan(Typeface.BOLD);
                        spannable.setSpan(bold, 3, postAuthor.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        postAuthor.setText(spannable, TextView.BufferType.SPANNABLE);

                        //If currently logged in user is a moderator, give them option to unban the user
                        if(isModerator){
                            postAuthor.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    onUnbanUserClick(v, post.getUserName(), post.getAuthorId());
                                }
                            });
                        }
                    }
                    //If the post author is an administrator
                    else if(documentSnapshot.get("AdminStatus") != null){
                        //Give proper color coding and special character indicator
                        postAuthor.append(" ♔");
                        Spannable spannable = new SpannableString(postAuthor.getText().toString());
                        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#8b0000")), 3, postAuthor.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        StyleSpan bold = new StyleSpan(Typeface.BOLD);
                        spannable.setSpan(bold, 3, postAuthor.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        postAuthor.setText(spannable, TextView.BufferType.SPANNABLE);
                    }
                    //If the post author is a moderator
                    else if(documentSnapshot.get("moderator") != null){
                        //Give proper color coding and special character indicator
                        postAuthor.append(" ★");
                        Spannable spannable = new SpannableString(postAuthor.getText().toString());
                        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#006400")), 3, postAuthor.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        StyleSpan bold = new StyleSpan(Typeface.BOLD);
                        spannable.setSpan(bold, 3, postAuthor.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        postAuthor.setText(spannable, TextView.BufferType.SPANNABLE);
                    }
                    //If the post author is an ordinary user
                    else {
                        //If the currently logged in user is a moderator, give them the option to ban the post author
                        if(isModerator){
                            banIcon.setVisibility(View.VISIBLE);
                            postAuthor.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    onBanUserClick(v, post.getUserName(), post.getAuthorId());
                                }
                            });
                            banIcon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    onBanUserClick(v, post.getUserName(), post.getAuthorId());
                                }
                            });
                        }
                    }
                }
            });

            //If the currently logged in user is the author of the post, give them the ability to delete it
            //If the currently logged in user is a moderator, give them the ability to remove posts not authored by the admin account
            if(user.getUid().compareTo(post.getAuthorId()) == 0 || (isModerator && post.getUserName().compareTo("vinmode2020") != 0)) {
                deleteBtn.setText("  ×  ");
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Dialog for prompting post deletion/removal confirmation
                        final AlertDialog.Builder removePostDialog = new AlertDialog.Builder(v.getContext());
                        removePostDialog.setTitle("Delete Post");

                        //If removing a post you authored
                        if(user.getUid().compareTo(post.getAuthorId()) == 0){
                            removePostDialog.setMessage("Are you sure you want to delete this post?");

                            //Deletes post from database
                            removePostDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dbRef.child(post.getId()).removeValue();
                                    Toast.makeText(Discussion.this, "Post successfully deleted!", Toast.LENGTH_SHORT).show();
                                    finish();
                                    startActivity(getIntent());
                                }
                            });
                            removePostDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Do nothing
                                }
                            });
                        }
                        //If moderator is removing a post they did not author
                        else{
                            //Used to input reason for removal
                            EditText reasonText = new EditText(Discussion.this);
                            removePostDialog.setMessage("Supply a reason for removing the post: ");
                            removePostDialog.setView(reasonText);

                            //Remove the post from the database only if a reason is supplied
                            removePostDialog.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(reasonText.getText().length() > 0){
                                        if(isOnline()){
                                            documentReference = fStore.collection("users").document(post.getAuthorId());
                                            //Fetches the post author's e-mail address
                                            documentReference.addSnapshotListener(Discussion.this, new EventListener<DocumentSnapshot>() {
                                                @Override
                                                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                                    String userEmail = documentSnapshot.get("email").toString();
                                                    //Send e-mail notification of post removal to post author
                                                    EmailThread emailThread = new EmailThread(userEmail, post.getTitle(), reasonText.getText().toString());
                                                    emailThread.start();
                                                    //Wait for e-mail to send
                                                    while(!emailThreadComplete){
                                                        Log.d("AUTH", "Stuck in here...");
                                                    }
                                                    //Delete post from database
                                                    dbRef.child(post.getId()).removeValue();

                                                    Toast.makeText(Discussion.this, "Post removed.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        else{
                                            Toast.makeText(Discussion.this, "Delete Message failed to send, make sure you have a reliable internet connection.", Toast.LENGTH_SHORT).show();
                                        }
                                        //Refresh
                                        finish();
                                        startActivity(getIntent());
                                    }
                                    else{
                                        errorToast = Toast.makeText(Discussion.this, "Post not removed; you must supply a reason for the removal.", Toast.LENGTH_SHORT);
                                        View view = errorToast.getView();
                                        view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                                        errorToast.show();
                                    }
                                }
                            });
                            removePostDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Do nothing
                                }
                            });
                        }
                        removePostDialog.create().show();
                    }
                });
            }
            //If neither, make delete button invisible
            else{
                deleteBtn.setText("");
            }
        }

        //If post header is clicked on, navigate user to activity_view_post instance that contains this specific post's data
        @Override
        public void onClick(View view){
            Intent intent = new Intent(Discussion.this, ViewPost.class);
            Bundle bundle = new Bundle();
            bundle.putString("POST_ID", post.getId());
            bundle.putString("POST_DATE", post.getDate());
            bundle.putString("POST_AUTHOR", post.getUserName());
            bundle.putString("POST_TITLE", post.getTitle());
            bundle.putString("POST_CONTENT", post.getText());
            bundle.putString("REPLY_COUNT", String.valueOf(post.getReplyCount()));
            if(post.getAuthorId() != ""){
                bundle.putString("AUTHOR_ID", post.getAuthorId());
            }
            //Pass current post data to ViewPost class instance
            intent.putExtras(bundle);
            startActivityForResult(intent, 1);
        }
    }

    //Background thread for sending post removal notification
    private class EmailThread extends Thread{
        private String recipient;
        private String postName;
        private String reason;

        //Execute sending of e-mail
        public void run() {
            //Send e-mail from VitiSmartSender@gmail.com to post author e-mail address
            GMailSender gMailSender = new GMailSender("VitiSmartSender@gmail.com", "wnxnznupbcoccggu", recipient, postName, reason);
            gMailSender.sendMail();
            //Notify main thread that thread execution is complete
            emailThreadComplete = true;
        }

        public EmailThread(String w, String x, String y){
            this.recipient = w;
            this.postName = x;
            this.reason = y;
        }
    }

    /**
     * Adapter class for postList RecyclerView.
     *
     * This class applies the list_item_post instances created in PostHolder to the RecyclerView
     * layout element in activity_discussion.xml.
     */
    public class PostAdapter extends RecyclerView.Adapter<PostHolder> {
        private ArrayList<Post> postArrayList;

        public PostAdapter(ArrayList<Post> x){
            postArrayList = x;
        }

        @NonNull
        @Override
        public PostHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_post, parent, false);
            return new PostHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostHolder holder, int position) {
            Post nextPost = postArrayList.get(position);
            holder.bind(nextPost);
        }

        @Override
        public int getItemCount() {
            return postArrayList.size();
        }
    }

    //Check if device has a stable internet connection
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    /**
     * Class uses GMail SMTP server to send post removal notification.
     */
    public class GMailSender extends javax.mail.Authenticator{
        private Session session;
        private String user;
        private String pword;
        private String recipient;
        private String postName;
        private String reason;

        public GMailSender(String user, String password, String recEmail, String postName, String reason){
            this.user = user;
            this.pword = password;
            this.recipient = recEmail;
            this.postName = postName;
            this.reason = reason;
        }

        //Authenicate VitiSmartSender@gmail.com credentials (uses an app password)
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, pword);
        }

        public synchronized void sendMail(){
            try{
                //Generate porperties object that stores protocol type, authentication, server address, and port numbers
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

                //Generate e-mail content
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(user)); //From VitiSmartSender@gmail.com
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipient));  //To post author e-mail address
                message.setSubject("VitiSmart: Post Removal Notice ");
                message.setText("Hi " + recipient.substring(0, recipient.indexOf('@')) + ",\n\n" + "Your post entitled \"" + postName + "\" has been removed from the forum for the following reason:\n\n" +
                        reason + "\n\nIf you think this was done in error or if you have any questions, please reach out to vinmode2020@gmail.com with your concern or use the \"Contact Us\" tool within the app." +
                        "\n\nThanks,\nThe VitiSmart Team");
                Transport.send(message);
            } catch(MessagingException me){
                me.printStackTrace();
            }
        }
    }

    //Creates the spinner for port sorting options, called in onCreate()
    public void createSpinner(){
        sortOptions = findViewById(R.id.sort_options);

        //Give the spinner the three options defined by the filter_options array resource (oldest first, newest first, most popular)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.filter_options, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortOptions.setAdapter(adapter);

        //Once a spinner item is selected, execute sorting logic based on which option was selected
        sortOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //startMonthSpinner listener

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                discussionPosts = new ArrayList<>();

                //If newest first is selected
                if(position == 0){
                    isDescending = true;
                    Query query = dbRef.orderByChild("dateSortable");
                    query.addListenerForSingleValueEvent(queryValueListener);
                }
                //If oldest first is selected
                else if(position == 1){
                    isDescending = false;
                    Query query = dbRef.orderByChild("dateSortable");
                    query.addListenerForSingleValueEvent(queryValueListener);
                }
                //If most popular is selected
                else if(position == 2){
                    isDescending = true;
                    Query query = dbRef.orderByChild("replyCount");
                    query.addListenerForSingleValueEvent(queryValueListener);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
            }
        });
    }

    //Adds a decoration to each post header that provides spacing between adjacent headers
    private class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;

            if (parent.getChildLayoutPosition(view) == 0) {
                outRect.top = space;
            } else {
                outRect.top = 0;
            }
        }
    }

}

