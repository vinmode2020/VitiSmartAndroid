package com.example.vinmod;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * AppCompatActivity class that handles the Infestation Map Activity.
 * It is linked to the activity_map.xml layout file.
 */
public class Map extends AppCompatActivity implements OnMapReadyCallback {

    static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 23;

    //Variables for layout elements
    GoogleMap mMap; //Stores Google Map instance
    Button confirmDateBtn;  //For applying date filter
    static Button startDate;    //For selecting start date for date filter
    static Button endDate;  //For selecting end date for date filter
    TextView morePinInfo;   //Displays pin data when pin is tapped on

    //Stores where map should navigate upon creation
    LatLng startPoint;
    boolean startPointSet = false;

    //Fragment that houses Google Map
    SupportMapFragment mapFragment;

    //Firebase References
    FirebaseFirestore fStore;   //Cloud Firestore reference
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();    //Data on currently logged in user
    FirebaseStorage storage = FirebaseStorage.getInstance();    //Cloud Storage instance
    StorageReference storageReference = storage.getReference(); //Cloud Storage reference
    FirebaseDatabase database = FirebaseDatabase.getInstance(); //Database instance
    DatabaseReference dbRef = database.getReference("/Pins/" + user.getUid());  //Database reference

    //Location Provider Client used to grab location data from GPS
    FusedLocationProviderClient fusedLocationProviderClient;

    //Arrays used for pin data
    ArrayList<Double> latArray = new ArrayList<>();
    ArrayList<Double> lngArray = new ArrayList<>();
    ArrayList<String> dateArray = new ArrayList<>();
    ArrayList<String> timeArray = new ArrayList<>();
    ArrayList<Boolean> infestedArray = new ArrayList<>();
    ArrayList<String> imageLinkArray = new ArrayList<>();

    //Used to display all user pins if current user is an admin
    ArrayList<String> uidList = new ArrayList<String>();
    List<DocumentSnapshot> userList;

    //Stores UID of currently logged in user
    String currentUid = "";

    //For preventing race condition between UI & background thread
    boolean uidThreadComplete = false;

    //Stores whether or not currently logged in user is an admin
    boolean isAdmin = false;

    //For converting String timestamps to sortable integer values
    int startDateSortable;
    int endDateSortable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //Initialize layout elements
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        confirmDateBtn = findViewById(R.id.heatMapConfirmDateButton); //Linking confirmDateBtn to respective Button in heat map page
        startDate = findViewById(R.id.pick_startdate);
        endDate = findViewById(R.id.pick_enddate);
        morePinInfo = findViewById(R.id.morePinInfo);

        //Get cloud Firestore instance
        fStore = FirebaseFirestore.getInstance();

        //Initialize Location Client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Get every user's UID. This is important for if the user is an admin and needs to see all
        //user pins
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnCompleteListener((new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            userList = task.getResult().getDocuments();
                            for(DocumentSnapshot ds: userList){
                                uidList.add(ds.getId());
                            }
                            uidThreadComplete = true;
                        }
                    }
                }));

        //Check if the currently logged in user is an admin
        DocumentReference documentReference = fStore.collection("users").document(user.getUid());
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                //If "AdminStatus" attribute exists, user is an administrator
                if(documentSnapshot.get("AdminStatus") != null){
                    AlertDialog.Builder crashWarning = new AlertDialog.Builder(Map.this);
                    crashWarning.setTitle("Admin Using Infestation Map");
                    crashWarning.setMessage("Administrators are able to view pins for images uploaded by all system "
                     + "users. This is a time-consuming process, and 15-20 seconds needs to be given to the app to "
                     + "process after \"Confirm Dates\" has been pressed. Otherwise, a crash may occur.");
                    crashWarning.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Close
                        }
                    });

                    isAdmin = true;

                    crashWarning.create().show();
                }else {
                    Log.d("DOC_SNAPSHOT", "onEvent: Document does not exist");
                }
            }
        });

        checkPermission(); //Checking permissions
        confirmDateBtnListener(); //For Querying the database

        //For selecting start date from calendar
        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialogFragment = new DatePickerFragment(true);
                dialogFragment.show(getSupportFragmentManager(), "datePicker");
            }
        });
        //For selecting end date from calendar
        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialogFragment = new DatePickerFragment(false);
                dialogFragment.show(getSupportFragmentManager(), "datePicker");
            }
        });
    }

    /**
     * This method allows the user to set a date for filtering in their heat map
     *
     * @param day startDaySpinner
     * @param month startMonthSpinner
     * @param year startYearSpinner
     * @return startingDate
     */
    private static String createStartDate(String day, String month, String year){
        if(month.length() == 1){
            month = "0" + month;
        }
        if(day.length() == 1){
            day = "0" + day;
        }
        return year + "-" + month + "-" + day;
    }

    /**
     * This method is the Event handler for our confirmDateBtn
     */
    private void confirmDateBtnListener() {
        confirmDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Wipe all data from previous query, preventing duplicate pins
                latArray.clear(); lngArray.clear(); dateArray.clear(); timeArray.clear(); infestedArray.clear(); imageLinkArray.clear(); //Clearing all the Arrays
                mMap.clear();

                //Get selected start and end dates as strings
                String startDateText = startDate.getText().toString();
                String endDateText = endDate.getText().toString();

                //Prevent pin generation if start date and/or and date not selected
                if(startDateText.compareTo("Pick a date...") == 0 || endDateText.compareTo("Pick a date...") == 0){
                    Toast.makeText(Map.this, "Please select both a start and an end date.", Toast.LENGTH_SHORT).show();
                    return;
                }

                //Generate sortable integers for date values (e.g. "5-4-2021" becomes 20210504)
                startDateSortable = Integer.parseInt(startDateText.substring(0, 4) + startDateText.substring(5, 7) + startDateText.substring(8));
                Log.d("DATES", Integer.toString(startDateSortable));
                endDateSortable = Integer.parseInt(endDateText.substring(0, 4) + endDateText.substring(5, 7) + endDateText.substring(8));
                Log.d("DATES", Integer.toString(endDateSortable));

                //If selected end date is before start date, throw error and prevent pin generation
                if(startDateSortable > endDateSortable){
                    Toast.makeText(Map.this, "Start date must be before end date", Toast.LENGTH_SHORT).show();
                }
                else{
                    //If the user is not an admin, generate pins for only their images
                    if(!isAdmin){
                        Query query = dbRef;
                        query.addListenerForSingleValueEvent(valueEventListener);
                    }
                    //If the user is an admin, generate pins for all user images
                    else{
                        latArray.clear(); lngArray.clear(); dateArray.clear(); timeArray.clear(); infestedArray.clear(); imageLinkArray.clear(); //Clearing all the Arrays
                        //Execute query for every single user
                        for(String x: uidList){
                            dbRef = database.getReference("/Pins/" + x);
                            currentUid = x;
                            Query query = dbRef;
                            runAdminEventListener(query, x);
                        }

                    }
                    Toast.makeText(Map.this, "Dates confirmed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //For generating all user pins, called on line 256
    public void runAdminEventListener(Query query, String uid) {
        //Query the database
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //For each user image
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        //Get date of image and convert it to sortable format
                        String date = snapshot.child("Date").getValue().toString();
                        int sortableDate = Integer.parseInt(date.substring(0, 4) + date.substring(5, 7) + date.substring(8));
                        //If image timestamp lies within filter interval, display it as a pin
                        if(sortableDate > startDateSortable && sortableDate < endDateSortable){
                            //The key for the image is its unique name generated when uploaded
                            imageLinkArray.add("pictures/" + uid + "/" + snapshot.getKey().replace("-", "."));
                            latArray.add(Double.parseDouble(snapshot.child("gpsLat").getValue().toString()));
                            lngArray.add(Double.parseDouble(snapshot.child("gpsLng").getValue().toString()));
                            dateArray.add(snapshot.child("Date").getValue().toString());
                            timeArray.add(snapshot.child("Time").getValue().toString());
                            infestedArray.add(Boolean.parseBoolean(snapshot.child("Status").getValue().toString()));
                        }
                    }
                    onMapReady(mMap); //Adding users pins to the map
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(startPoint));    //Setting the starting point to Erie, Pa
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //Do nothing
            }
        });
    }

    //Does the same as the adminEventListener, but only for images uploaded by the currently logged in user
    ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists()){
                for (DataSnapshot snapshot : dataSnapshot.getChildren() ){
                    String date = snapshot.child("Date").getValue().toString();
                    int sortableDate = Integer.parseInt(date.substring(0, 4) + date.substring(5, 7) + date.substring(8));
                    if(sortableDate > startDateSortable && sortableDate < endDateSortable){
                        imageLinkArray.add("pictures/" + user.getUid() + "/" + snapshot.getKey().replace("-", "."));
                        latArray.add(Double.parseDouble(snapshot.child("gpsLat").getValue().toString()));
                        lngArray.add(Double.parseDouble(snapshot.child("gpsLng").getValue().toString()));
                        dateArray.add(snapshot.child("Date").getValue().toString());
                        timeArray.add(snapshot.child("Time").getValue().toString());
                        infestedArray.add(Boolean.parseBoolean(snapshot.child("Status").getValue().toString()));
                    }
                }
                onMapReady(mMap); //Adding users pins to the map
                mMap.moveCamera(CameraUpdateFactory.newLatLng(startPoint));
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };

    /**
     * This class generates Calendars from which users can select start and end dates for filtering pins
     */
    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener{
        boolean isStartDate;

        public DatePickerFragment(boolean x){
            //If the boolean passed is true, a start date is being set. If not, end date is being set
            if(x){
                isStartDate = true;
            }
        }

        //When creating the calendar, link integer variables to calendar's year, month, and day values
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        //When the date has been set and confirmed, set the text of the corresponding date button to
        //the date selected
        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            month += 1; //This is needed because the month value returned by the calendar is one less than it should be for some reason
            //Update start date button
            if(isStartDate){
                startDate.setText(createStartDate(Integer.toString(day), Integer.toString(month), Integer.toString(year)));
            }
            //Update end date button
            else{
                endDate.setText(createStartDate(Integer.toString(day), Integer.toString(month), Integer.toString(year)));
            }
        }
    }

    //Request location permissions if they are not given
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            mapFragment.getMapAsync(this);
        }

    }

    //Executed after Location permission request is either approved or denied
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                //If permission was approved, generate the Google Map
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);
                }
                //If permission denied, hide the map and notify user
                else {
                    Log.d("MAPCHECK", "Permission denied :(");
                    mapFragment.getView().setVisibility(View.INVISIBLE);
                    confirmDateBtn.setClickable(false);
                    startDate.setClickable(false);
                    startDate.setText("");
                    endDate.setClickable(false);
                    endDate.setText("");
                    morePinInfo.setText("Enable location permissions to use the map.");
                }
                return;
            }
        }
    }

    /**
     * Creates our map instance
     * @param googleMap instance of our map API
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        //HashMap for storing links to images
        HashMap<Integer, Task<Uri>> markerMap = new HashMap<Integer, Task<Uri>>();

        mMap = googleMap;
        googleMap.clear(); // Clearing the map allowing for a complete reset

        //For each pin whose data was gathered in the confirmDateButtonListener
        for (int x = 0; x < dateArray.size() ; x++) {   //Loop for creating all of the map markers

            //Get this specific pin's data
            double lat = latArray.get(x);
            double lng = lngArray.get(x);
            String dateArrayHolder = dateArray.get(x);
            String timeArrayHolder = timeArray.get(x);
            String imageArrayHolder = imageLinkArray.get(x);
            boolean infestedArrayHolder = infestedArray.get(x);

            //Create the pin for the image
            Marker place;
            if (!infestedArrayHolder){  //If the cluster is not infested it will set the color of the marker to blue
                place = googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng))    //Adding the Date the picture was taken and the time.
                        .title("Recorded on "+ dateArrayHolder + " at "+timeArrayHolder).snippet(getString(R.string.mapLink)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }
            else{
                place = googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng))    //Adding the Date the picture was taken and the time.
                        .title("Recorded on "+ dateArrayHolder + " at "+timeArrayHolder).snippet(getString(R.string.mapLink)));
            }

            place.setTag(x);

            //Put the URL for this pin's respective image into the hashmap
            markerMap.put(x, storageReference.child(imageLinkArray.get(x)).getDownloadUrl());

            //Display the image the pin represents if the dialog message displaying the timestamp
            //is tapped on
            googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    Task<Uri> link = markerMap.get(marker.getTag());
                    Intent intent = new Intent(Map.this, MapImage.class);
                    Bundle extras = new Bundle();
                    extras.putString("IMAGE_URI", link.getResult().toString());
                    intent.putExtras(extras);
                    startActivity(intent);
                }
            });

        }

        //Grab new user location every 2 seconds
        LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setFastestInterval(1000);

        //Grab GPS location and set it to be the map's start point
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Log.d("MAPCHECK", "Updating location...");
                        Location location = locationResult.getLastLocation();
                        startPoint = new LatLng(location.getLatitude(), location.getLongitude());
                        //Only set the start point once for each time "Confirm dates" is clicked
                        if (!startPointSet){
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(startPoint));    //Setting the starting point to Erie, Pa
                            startPointSet = true;
                        }
                    }
                },Looper.myLooper());

    }

}