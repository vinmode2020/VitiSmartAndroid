package com.example.vinmod;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.util.ArrayList;
import java.util.HashMap;


//https://console.cloud.google.com/apis/credentials?authuser=1&project=vinmod&supportedpurview=project

/**
 * This Class creates the Map fragment for use in our Application
 *
 * @author David Simmons, Mohammed Ibrahim
 */
public class Map extends AppCompatActivity implements OnMapReadyCallback {


    static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 23;
    private GoogleMap mMap;
    Button confirmDateBtn;

    //Spinners that are used to set parameters
    private String startMonthSpinner;
    private String startYearSpinner;
    private String startDaySpinner;
    private String endMonthSpinner;
    private String endYearSpinner;
    private String endDaySpinner;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    //Array's used for pins
    ArrayList<Double> latArray = new ArrayList<>();
    ArrayList<Double> lngArray = new ArrayList<>();
    ArrayList<String> dateArray = new ArrayList<>();
    ArrayList<String> timeArray = new ArrayList<>();
    ArrayList<Boolean> infestedArray = new ArrayList<>();
    ArrayList<String> imageLinkArray = new ArrayList<>();

    //Connecting to Firebase
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = database.getReference("/Pins/" + user.getUid());



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        confirmDateBtn = findViewById(R.id.heatMapConfirmDateButton); //Linking confirmDateBtn to respective Button in heat map page

        checkPermission(); //Checking permissions
        createSpinners(); //Creating the spinners in our activity
        confirmDateBtnListener(); //For Querying the The database
        
    }


    /**
     * This method allows the user to set a start date for filtering in their heat map
     *
     * @param day startDaySpinner
     * @param month startMonthSpinner
     * @param year startYearSpinner
     * @return startingDate
     */
    private String createStartDate(String day, String month, String year){
        String startingDate = year + "-" + month + "-" + day;
        System.out.println(startingDate);
        return startingDate;
    }

    /**
     * This method allows the user to set an end date for filtering their heat map
     *
     * @param day endDaySpinner
     * @param month endMonthSpinner
     * @param year endYearSpinner
     * @return endDate
     */
    private String createEndDate(String day, String month, String year){
        String endDate = year + "-" + month + "-" + day;
        System.out.println(endDate);
        return endDate;
    }

   
    /**
     * This method is the Event handler for our confirmDateBtn
     */
    private void confirmDateBtnListener() {
        confirmDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String startMonth, endMonth;

                switch(startMonthSpinner){
                    case "Jan":
                        startMonth = "1";
                    case "Feb":
                        startMonth = "2";
                    case "Mar":
                        startMonth = "3";
                    case "Apr":
                        startMonth = "4";
                    case "May":
                        startMonth = "5";
                    case "Jun":
                        startMonth = "6";
                    case "Jul":
                        startMonth = "7";
                    case "Aug":
                        startMonth = "8";
                    case "Sep":
                        startMonth = "9";
                    case "Oct":
                        startMonth = "10";
                    case "Nov":
                        startMonth = "11";
                    default:
                        startMonth = "12";

                }

                switch(endMonthSpinner){
                    case "Jan":
                        endMonth = "1";
                    case "Feb":
                        endMonth = "2";
                    case "Mar":
                        endMonth = "3";
                    case "Apr":
                        endMonth = "4";
                    case "May":
                        endMonth = "5";
                    case "Jun":
                        endMonth = "6";
                    case "Jul":
                        endMonth = "7";
                    case "Aug":
                        endMonth = "8";
                    case "Sep":
                        endMonth = "9";
                    case "Oct":
                        endMonth = "10";
                    case "Nov":
                        endMonth = "11";
                    default:
                        endMonth = "12";
                }

                double startDateSortable = Integer.parseInt(startYearSpinner + startMonth + startDaySpinner);
                double endDateSortable = Integer.parseInt(endYearSpinner + endMonth + endDaySpinner);

                if(startDateSortable > endDateSortable){
                    Toast.makeText(Map.this, "Start date must be before end date", Toast.LENGTH_SHORT).show();
                }
                else{
                    Query query = dbRef
                            .orderByChild("Date")   //Sorting the pins by the date they were uploaded
                            .startAt(createStartDate(startDaySpinner, startMonthSpinner, startYearSpinner)) //Filtering the query to start at specified date
                            .endAt(createEndDate(endDaySpinner, endMonthSpinner, endYearSpinner));  //Filtering the query to end at the specified date


                    query.addListenerForSingleValueEvent(valueEventListener);
                }


            }
        });

    }


    /**
     * ValueEventListener for our query statement being executed inside our ConfirmDateBtnListener()
     */
    ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            latArray.clear(); lngArray.clear(); dateArray.clear(); timeArray.clear(); infestedArray.clear(); imageLinkArray.clear(); //Clearing all the Arrays

            if (dataSnapshot.exists()){
                for (DataSnapshot snapshot : dataSnapshot.getChildren() ){
                    imageLinkArray.add("pictures/" + user.getUid() + "/" + snapshot.getKey().replace("-", "."));
                    latArray.add(Double.parseDouble(snapshot.child("gpsLat").getValue().toString()));
                    lngArray.add(Double.parseDouble(snapshot.child("gpsLng").getValue().toString()));
                    dateArray.add(snapshot.child("Date").getValue().toString());
                    timeArray.add(snapshot.child("Time").getValue().toString());
                    infestedArray.add(Boolean.parseBoolean(snapshot.child("Status").getValue().toString()));
                }
                onMapReady(mMap); //Adding users pins to the map
                LatLng firstPin = new LatLng(latArray.get(0), lngArray.get(0)); //Setting a new LatLng Variable to update camera to first pin
                mMap.moveCamera(CameraUpdateFactory.newLatLng(firstPin)); //Updating the Camera position to first pin the user Query

            }
            //-----Delete when finished-----//
            for (int x = 0; x <infestedArray.size();x++){
                System.out.println("Image Link: " + imageLinkArray.get(x) + "\nLatitude: " + latArray.get(x) + "\nLongitude: " + lngArray.get(x) +
                        "\nDate: " + dateArray.get(x) + "\nTime: " + timeArray.get(x) + "\nInfested: " + infestedArray.get(x));
            }
            //-----Delete when finished-----//
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };

    /**
     * This method creates all of the spinners that will be used for our activity_map.xml
     */
    private void createSpinners(){

        //Dropdown Menu
        Spinner spinner1 = (Spinner) findViewById(R.id.startMonthSpinner);
        Spinner spinner2 = (Spinner) findViewById(R.id.startYearSpinner);
        Spinner spinner3 = (Spinner) findViewById(R.id.endMonthSpinner);
        Spinner spinner4 = (Spinner) findViewById(R.id.endYearSpinner);
        Spinner spinner5 = (Spinner) findViewById(R.id.startDaySpinner);
        Spinner spinner6 = (Spinner) findViewById(R.id.endDaySpinner);

        //ArrayAdapter using the string array and default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.months, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.years, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this, R.array.months, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> adapter4 = ArrayAdapter.createFromResource(this, R.array.years, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> adapter5 = ArrayAdapter.createFromResource(this, R.array.days, android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> adapter6 = ArrayAdapter.createFromResource(this, R.array.days, android.R.layout.simple_spinner_dropdown_item);

        //Specifying the layout to use when the list of choice appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter5.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter6.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //Applying adapter to the spinner
        spinner1.setAdapter(adapter);
        spinner2.setAdapter(adapter2);
        spinner3.setAdapter(adapter3);
        spinner4.setAdapter(adapter4);
        spinner5.setAdapter(adapter5);
        spinner6.setAdapter(adapter6);

        //Setting up the onItemSelectedListeners
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //startMonthSpinner listener

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                startMonthSpinner = spinner1.getSelectedItem().toString(); // Setting the value of the start month
                createStartDate(startDaySpinner, startMonthSpinner, startYearSpinner);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //StartYearSpinner listener

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {

                startYearSpinner = spinner2.getSelectedItem().toString(); // Setting the value of the start year
                createStartDate(startDaySpinner, startMonthSpinner, startYearSpinner);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });

        spinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //endMonthSpinner listener

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {

                endMonthSpinner = spinner3.getSelectedItem().toString(); // Setting the value of the end month
                createEndDate(endDaySpinner, endMonthSpinner, endYearSpinner);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });

        spinner4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //endYearSpinner listener

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {

                endYearSpinner = spinner4.getSelectedItem().toString(); // Setting the value of the end year
                createEndDate(endDaySpinner, endMonthSpinner, endYearSpinner);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });

        spinner5.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //startDaySpinner listener

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                startDaySpinner = spinner5.getSelectedItem().toString();
                createStartDate(startDaySpinner, startMonthSpinner, startYearSpinner); // Setting the value of the start day
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });

        spinner6.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { //endDaySpinner listener

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                endDaySpinner = spinner6.getSelectedItem().toString(); // Setting the value of the end day
                createEndDate(endDaySpinner, endMonthSpinner, endYearSpinner);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });



    }

    /**
     * Checking permissions
     */
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

    }

    /**
     * Requesting permissions
     * @param requestCode our code for the permission
     * @param permissions asking for specific permission
     * @param grantResults result of the permmissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
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

        HashMap<Integer, Task<Uri>> markerMap = new HashMap<Integer, Task<Uri>>();

        mMap = googleMap;
        googleMap.clear(); // Clearing the map allowing for a complete reset
        LatLng eriePa = new LatLng(42.1292 , -80.0851); //Coordinates for Erie, Pa
        for (int x = 0; x < dateArray.size() ; x++) {   //Loop for creating all of the map markers

            double lat = latArray.get(x);
            double lng = lngArray.get(x);
            String dateArrayHolder = dateArray.get(x);
            String timeArrayHolder = timeArray.get(x);
            String imageArrayHolder = imageLinkArray.get(x);
            boolean infestedArrayHolder = infestedArray.get(x);

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

            markerMap.put(x, storageReference.child(imageLinkArray.get(x)).getDownloadUrl());

            int finalX = x;
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

            //googleMap.addMarker(place); //Adding the marker To the map

        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(eriePa));    //Setting the starting point to Erie, Pa


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then
            //How to overlay two images in Android to set an ImageView?www.tutorialspoint.com › how-to-overlay-two-images-i...
            //Sep 11, 2019 — This example demonstrates how do I overlay two images In Android to set an ImageView.Step 1 − Create a new project in Android Studio, ...overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
    }

}