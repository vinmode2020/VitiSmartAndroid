package com.example.vinmod;

import android.Manifest;

import androidx.appcompat.app.AlertDialog;
import androidx.exifinterface.media.ExifInterface;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * AppCompatActivity class that handles the Cluster Capture Activity.
 * It is linked to the activity_view_post.xml layout file.
 */
public class Scan extends AppCompatActivity {

    //Permission request codes; used in checking for and requesting of app permissions
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;
    public static final int STORAGE_PER_CODE = 103;
    public static final int LOCATION_PER_CODE = 106;
    public static final int MEDIA_LOCATION_CODE = 107;
    public int currentCode; //Stores CAMERA_REQUEST_CODE if taking picture, GALLERY_REQUEST_CODE if uploading
    String galleryFileName;

    //Firebase contect references
    DatabaseReference databaseReference;    //Reference to Realtime Database
    StorageReference storageReference;  //Reference to Firebase Cloud Storage
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();    //Info on currently logged in user

    //Used to request LatLng coordinates from device GPS
    LocationManager locationManager;
    FusedLocationProviderClient fusedLocationProviderClient;

    //Layout elements
    ImageView selectedImage;    //Displays image preview after it is uploaded/taken
    Button cameraBtn;   //Used to take picture in-app
    Button galleryBtn;  //Used to upload image from photo gallery
    Button infBtn;  //Used to mark an image as infested
    Button notSureBtn;  //Used to mark an image as not infested
    Button notInfBtn;   //Used to navigate to resource page
    ProgressBar progressBar;    //Displays while app is uploading image
    TextView stepCounter;   //Displays what step of the process the user is currently on ("Step 1" or "Step 2")
    TextView stepDescription;   //Displays what needs to be done in the current step
    TextView beforeImage;   //Gray box that appears before an image is uploaded/taken

    //Stores path to image to be uploaded
    Uri contentUri;

    //Variables for globally storing image metadata
    String imageDate;
    String imageTime;
    String imageLat;
    String imageLon;
    String imageStatus;

    //Displays error messages
    Toast errorToast;

    //Controls whether or not to upload an image during an iteration of the location listener
    boolean uploadImageInListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //Get Reference to Firebase Cloud Storage
        storageReference = FirebaseStorage.getInstance().getReference();

        //Initialize layout elements
        stepCounter = findViewById(R.id.stepCounter);
        stepDescription = findViewById(R.id.stepDesc);
        selectedImage = findViewById(R.id.displayImageView);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        infBtn = findViewById(R.id.infBtn);
        notInfBtn = findViewById(R.id.notInfBtn);
        notSureBtn = findViewById(R.id.notSureBtn);
        progressBar = findViewById(R.id.contact_progressbar);
        beforeImage = findViewById(R.id.before_image);

        //Initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //Ask location and camera permissions
        askStoragePermissions();
        askCameraPermissions();

        //Initialize location provider client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Click Listener for "Capture" Button
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ask to access GPS location
                askLocationPermissions();
                currentCode = CAMERA_REQUEST_CODE;

                //ContentValues object provides title and description metadata for the image to be taken
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Images.Media.TITLE, "New Cluster");
                contentValues.put(MediaStore.Images.Media.DESCRIPTION, "From device's Camera");

                //Initialize contentURI to contain path to device media storage
                contentUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                //Create and launch intent to open device camera
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
                startActivityForResult(captureIntent, CAMERA_REQUEST_CODE);
            }
        });

        //Click Listener for "Gallery" Button
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ask to access GPS location
                askLocationPermissions();
                currentCode = GALLERY_REQUEST_CODE;

                //Create and launch intent to select image from device photo gallery
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });

        //Click Listener for "Infested" Button
        infBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Disable all other buttons while upload executes
                infBtn.setEnabled(false);
                notInfBtn.setEnabled(false);
                notSureBtn.setEnabled(false);

                //If location permissions have not been granted, notify user and prevent upload
                if (ContextCompat.checkSelfPermission(Scan.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    errorToast = Toast.makeText(Scan.this, "Picture not uploaded; GPS permissions not granted!", Toast.LENGTH_SHORT);
                    View view = errorToast.getView();
                    view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    errorToast.show();
                    onBackPressed();
                }

                //Show progress bar
                progressBar.setVisibility(View.VISIBLE);

                //Fetch current time stamp
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

                //If image was taken in-app
                if (currentCode == CAMERA_REQUEST_CODE) {
                    imageStatus = "true";
                    uploadImageToFirebase("IMG_"+ timeStamp + ".jpg", contentUri, true);
                }
                //If image was obtained from photo gallery
                else if (currentCode == GALLERY_REQUEST_CODE) {
                    imageStatus = "true";
                    //If contentURI is null, that means that the user navigated away from the photo gallery
                    //without selecting an image. Prevent upload in this case.
                    if(contentUri != null){
                        uploadImageToFirebase(galleryFileName, contentUri, false);
                    }
                    else{
                        errorToast = Toast.makeText(Scan.this, "ERROR: No image selected.", Toast.LENGTH_SHORT);
                        View view = errorToast.getView();
                        view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                        errorToast.show();
                        onBackPressed();
                    }
                }

            }
        });

        //Click listener for "Not Infested" Button
        notInfBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Disable all buttons while upload executes
                infBtn.setEnabled(false);
                notInfBtn.setEnabled(false);
                notSureBtn.setEnabled(false);

                //If location permissions have not been granted, notify user and prevent upload
                if (ContextCompat.checkSelfPermission(Scan.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    errorToast = Toast.makeText(Scan.this, "Picture not uploaded; GPS permissions denied!", Toast.LENGTH_SHORT);
                    View view = errorToast.getView();
                    view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    errorToast.show();
                    onBackPressed();
                }

                //Show progress bar
                progressBar.setVisibility(View.VISIBLE);

                //Fetch current time stamp
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

                //If image was taken in-app
                if (currentCode == CAMERA_REQUEST_CODE) {
                    imageStatus = "false";
                    // Naming and uploading the image
                    uploadImageToFirebase("IMG_"+ timeStamp + ".jpg", contentUri, true);
                }
                //If image was obtained from photo gallery
                else if (currentCode == GALLERY_REQUEST_CODE) {
                    imageStatus = "false";
                    //If contentURI is null, that means that the user navigated away from the photo gallery
                    //without selecting an image. Prevent upload in this case.
                    if(contentUri != null){
                        uploadImageToFirebase(galleryFileName, contentUri, false);
                    }
                    else{
                        errorToast = Toast.makeText(Scan.this, "ERROR: No image selected.", Toast.LENGTH_SHORT);
                        View view = errorToast.getView();
                        view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                        errorToast.show();
                        onBackPressed();
                    }
                }

            }
        });

        // This will allow the notSureBtn to connect the Scan page to the Resource page.
        notSureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Scan.this, Resource.class);
                startActivity(intent);
            }
        });

    }

    //Requests permission to access device camera
    private void askCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }
    }

    //Requests permission to use device GPS
    private void askLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PER_CODE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PER_CODE);
            return;
        }
    }

    //For Android API 29+, requests permission to access photo gallery storage directory.
    //More info can be found here: https://developer.android.com/training/data-storage/shared/media
    private void askMediaLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, MEDIA_LOCATION_CODE);
        }
    }


    //For Android API 28 and below, request permission to read and write to external device storage
    private void askStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PER_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PER_CODE);
        }
    }

    //Method is executed following a permission request prompt
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //If camera permissions were just asked for
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                //If the user deny the request to use the camera, prevent use of "Capture" option
                cameraBtn.setClickable(false);
                cameraBtn.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                beforeImage.setText("Camera permissions are required to use the \"capture\" option.");
            }
        }
        //If storage permissions were just asked for
        if (requestCode == STORAGE_PER_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                //If the user denied the request to access device storage, prevent all further action
                cameraBtn.setClickable(false);
                cameraBtn.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                galleryBtn.setClickable(false);
                galleryBtn.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                beforeImage.setText("Storage permissions required!");
            }
        }
    }

    //Method is executed after either taking a picture or uploading one from the gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //If an image was successfully taken in-app
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            try{
                //Create a bitmap copy of the image taken and display it in the app using the ImageView
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), contentUri);
                selectedImage.setImageBitmap(imageBitmap);
            }catch(Exception ie){
                ie.printStackTrace();
            }
        }

        //If an image was successfully uploaded from the gallery
        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                //Get the file's path and generate a unique name for the image using current time stamp
                contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "." + getFileExt(contentUri);
                Log.d("tag", "onActivityResult: Gallery Image Uri:  " + imageFileName);
                // Display the image on the app
                selectedImage.setImageURI(contentUri);
                galleryFileName = imageFileName;
            }

        }

        //If an image was successfully taken or uploaded, display "Step 2" options
        if(contentUri != null && resultCode == RESULT_OK){
            // make imageview and decision buttons visible
            cameraBtn.setVisibility(View.INVISIBLE);
            galleryBtn.setVisibility(View.INVISIBLE);
            beforeImage.setVisibility(View.INVISIBLE);
            selectedImage.setVisibility(View.VISIBLE);
            stepCounter.setText("Step 2");
            stepDescription.setText("Review the image and confirm its status.");
            infBtn.setVisibility(View.VISIBLE);
            notInfBtn.setVisibility(View.VISIBLE);
            notSureBtn.setVisibility(View.VISIBLE);
            askMediaLocationPermissions();
        }
        else{
            beforeImage.setText("No image selected!");
        }


    }

    //This method returns a working path to an image to be uploaded to Firebase
    //Obtained from https://stackoverflow.com/questions/17546101/get-real-path-for-uri-android
    public static String getPath(Context context, Uri uri) {
        String result = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(proj[0]);
                result = cursor.getString(column_index);
            }
            cursor.close();
        }
        if (result == null) {
            result = "Not found";
        }
        return result;
    }

    //Executes once a user has selected either "Infested" or "Not Infested"
    private void uploadImageToFirebase(String name, Uri contentUri, boolean isCapture) {
        final StorageReference image = storageReference.child("pictures/" + user.getUid() + "/" + name);

        //If the image was taken in-app
        if (isCapture) {
            uploadImageInListener = true;

            //LocationRequest fetches new location coordinates from the GPS every 2 seconds
            LocationRequest locationRequest = new LocationRequest()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(2000)
                    .setFastestInterval(1000);

            //Do not allow further action if fine location permission was not granted. Fine location
            //is specifically needed to get the most precise GPS coordinates.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            //Listener for updating location coordinates
            //PutFile method call is included in here to ensure GPS coordinates are fetched before
            //attempting to upload the image.
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            Location location = locationResult.getLastLocation();
                            imageLat = Double.toString(location.getLatitude());
                            imageLon = Double.toString(location.getLongitude());
                            //Only upload the image on the first iteration of the LocationListener
                            if(uploadImageInListener){
                                putFile(imageLat, imageLon, contentUri, image, name);
                            }
                            uploadImageInListener = false;
                        }
                    },
                    Looper.myLooper());
        }
        //If the image was obtained from the photo gallery
        else if (!isCapture){
            //Dialog for prompting the user to either use the GPS coordinates to geotag the image
            //or attempt to fetch Exif location metadata
            final AlertDialog.Builder uploadDialog = new AlertDialog.Builder(this);
            uploadDialog.setTitle("What would you like to use to geolocate the image?");
            uploadDialog.setItems(R.array.geo_options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //If user chose to use GPS coordinates
                    if(which == 0){
                        //Fetch location and upload in the same way as lines 433-463
                        uploadImageInListener = true;
                        LocationRequest locationRequest = new LocationRequest()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setInterval(2000)
                                .setFastestInterval(1000);

                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        Location location = locationResult.getLastLocation();
                                        imageLat = Double.toString(location.getLatitude());
                                        imageLon = Double.toString(location.getLongitude());
                                        if(uploadImageInListener){
                                            putFile(imageLat, imageLon, contentUri, image, name);
                                        }
                                        uploadImageInListener = false;
                                    }
                                },
                                Looper.myLooper());
                    }
                    else{
                        //Get the real image path and store it in contentUri
                        String picturePath;
                        picturePath = getPath( getApplicationContext(), contentUri );
                        //Attempt to fetch location coordinates from Exif Metadata
                        try {
                            ExifInterface exifInterface = new ExifInterface(picturePath);
                            float[] latLong = new float[2];
                            exifInterface.getLatLong(latLong);
                            imageLat = Float.toString(latLong[0]);
                            imageLon = Float.toString(latLong[1]);

                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(Scan.this, picturePath, Toast.LENGTH_SHORT).show();
                        }
                        //Upload the image
                        putFile(imageLat, imageLon, contentUri, image, name);
                    }
                }
            });
            //Display the dialog
            uploadDialog.create().show();
        }
    }

    //Method for executing the uploading of an image to Firebase
    public void putFile(String imageLat, String imageLon, Uri contentUri, StorageReference image, String name){
        //Attempt to upload the image to Firebase cloud storage
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            //Execute this method if the upload was successful
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //If for whatever reason location data was not properly gathered, delete the image and notify the user
                if(imageLat == null || imageLon == null){
                    errorToast = Toast.makeText(Scan.this, "Picture not uploaded, error fetching GPS coordinates.", Toast.LENGTH_SHORT);
                    View view = errorToast.getView();
                    view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    errorToast.show();
                    image.delete();
                    onBackPressed();
                }
                //LatLng coordinates are (0,0) if fetching from Exif metadata failed. In this case,
                //delete image and notify user
                else if (imageLat.compareTo("0.0") == 0 && imageLon.compareTo("0.0") == 0){
                    errorToast = Toast.makeText(Scan.this, "Picture not uploaded, image has no GPS coordinates.", Toast.LENGTH_SHORT);
                    View view = errorToast.getView();
                    view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    errorToast.show();
                    image.delete();
                    onBackPressed();
                }

                final boolean[] uploadFinished = {false};

                //Log image url to console
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSuccess: Uploaded Image URl is " + uri.toString());
                        uploadFinished[0] = true;
                    }
                });

                //Initialize database reference
                databaseReference = FirebaseDatabase.getInstance().getReference();

                String databaseName = name.replace('.', '-');

                String storageTimeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
                imageDate = storageTimeStamp.substring(0, 10);
                imageTime = storageTimeStamp.substring(11, 19);

                //Add Pin object to database under the current user's UID
                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("Date").setValue(imageDate);
                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("Time").setValue(imageTime);
                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("gpsLng").setValue(imageLon);
                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("gpsLat").setValue(imageLat);
                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("Status").setValue(imageStatus);

                //Notify user of successful upload
                Toast.makeText(Scan.this, "Upload Successful!", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        }).addOnFailureListener(new OnFailureListener() {
            //Execute if image failed to upload; usually because of network connectivity issues
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Scan.this, "Upload Failed, check your network connection.", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        });
    }

    //Returns the file type extension of the image (e.g. ".jpeg")
    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }
}

