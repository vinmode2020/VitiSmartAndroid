package com.example.vinmod;

import android.app.DatePickerDialog;
import android.app.Dialog;
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
    private GoogleMap mMap;
    FirebaseFirestore fStore;
    Button confirmDateBtn;
    static Button startDate, endDate;
    TextView morePinInfo;

    LatLng startPoint;
    boolean startPointSet = false;

    SupportMapFragment mapFragment;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    FusedLocationProviderClient fusedLocationProviderClient;


    //Array's used for pins
    ArrayList<Double> latArray = new ArrayList<>();
    ArrayList<Double> lngArray = new ArrayList<>();
    ArrayList<String> dateArray = new ArrayList<>();
    ArrayList<String> timeArray = new ArrayList<>();
    ArrayList<Boolean> infestedArray = new ArrayList<>();
    ArrayList<String> imageLinkArray = new ArrayList<>();

    //Connecting to Firebase
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    List<DocumentSnapshot> userList;
    ArrayList<String> uidList = new ArrayList<String>();
    DatabaseReference dbRef = database.getReference("/Pins/" + user.getUid());

    String currentUid = "";
    boolean uidThreadComplete = false;
    boolean isAdmin = false;
    int startDateSortable, endDateSortable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        confirmDateBtn = findViewById(R.id.heatMapConfirmDateButton); //Linking confirmDateBtn to respective Button in heat map page
        startDate = findViewById(R.id.pick_startdate);
        endDate = findViewById(R.id.pick_enddate);

        morePinInfo = findViewById(R.id.morePinInfo);

        fStore = FirebaseFirestore.getInstance();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

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

        DocumentReference documentReference = fStore.collection("users").document(user.getUid());

        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot.get("AdminStatus") != null){
                    Toast.makeText(Map.this, "You're an admin!", Toast.LENGTH_SHORT).show();
                    isAdmin = true;
                }else {
                    Log.d("DOC_SNAPSHOT", "onEvent: Document does not exist");
                }
            }
        });

        checkPermission(); //Checking permissions

        confirmDateBtnListener(); //For Querying the The database

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialogFragment = new DatePickerFragment(true);
                dialogFragment.show(getSupportFragmentManager(), "datePicker");
            }
        });
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
                latArray.clear(); lngArray.clear(); dateArray.clear(); timeArray.clear(); infestedArray.clear(); imageLinkArray.clear(); //Clearing all the Arrays

                mMap.clear();
                String startDateText = startDate.getText().toString();
                String endDateText = endDate.getText().toString();

                if(startDateText.compareTo("Pick a date...") == 0 || endDateText.compareTo("Pick a date...") == 0){
                    Toast.makeText(Map.this, "Please select both a start and an end date.", Toast.LENGTH_SHORT).show();
                    return;
                }

                startDateSortable = Integer.parseInt(startDateText.substring(0, 4) + startDateText.substring(5, 7) + startDateText.substring(8));
                Log.d("DATES", Integer.toString(startDateSortable));
                endDateSortable = Integer.parseInt(endDateText.substring(0, 4) + endDateText.substring(5, 7) + endDateText.substring(8));
                Log.d("DATES", Integer.toString(endDateSortable));

                if(startDateSortable > endDateSortable){
                    Toast.makeText(Map.this, "Start date must be before end date", Toast.LENGTH_SHORT).show();
                }
                else{
                    if(!isAdmin){
                        Query query = dbRef;
                        query.addListenerForSingleValueEvent(valueEventListener);
                    }
                    else{
                        latArray.clear(); lngArray.clear(); dateArray.clear(); timeArray.clear(); infestedArray.clear(); imageLinkArray.clear(); //Clearing all the Arrays

                        Log.d("Ugh", uidList.toString());

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

    public void runAdminEventListener(Query query, String uid) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String date = snapshot.child("Date").getValue().toString();
                        int sortableDate = Integer.parseInt(date.substring(0, 4) + date.substring(5, 7) + date.substring(8));
                        if(sortableDate > startDateSortable && sortableDate < endDateSortable){
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

            }
        });
    }

    /**
     * ValueEventListener for our query statement being executed inside our ConfirmDateBtnListener()
     */
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

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener{
        boolean isStartDate;

        public DatePickerFragment(boolean x){
            if(x){
                isStartDate = true;
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            month += 1;
            if(isStartDate){
                startDate.setText(createStartDate(Integer.toString(day), Integer.toString(month), Integer.toString(year)));
            }
            else{
                endDate.setText(createStartDate(Integer.toString(day), Integer.toString(month), Integer.toString(year)));
            }
        }
    }

    /**
     * Checking permissions
     */
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            mapFragment.getMapAsync(this);
            Log.d("MAPCHECK", "In here...");
        }

    }

    /**
     * Requesting permissions
     * @param requestCode our code for the permission
     * @param permissions asking for specific permission
     * @param grantResults result of the permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
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

        HashMap<Integer, Task<Uri>> markerMap = new HashMap<Integer, Task<Uri>>();

        mMap = googleMap;
        googleMap.clear(); // Clearing the map allowing for a complete reset
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

        LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setFastestInterval(1000);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Log.d("MAPCHECK", "Updating location...");
                        Location location = locationResult.getLastLocation();
                        startPoint = new LatLng(location.getLatitude(), location.getLongitude());
                        if (!startPointSet){
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(startPoint));    //Setting the starting point to Erie, Pa
                            startPointSet = true;
                        }
                    }
                },Looper.myLooper());

    }

}