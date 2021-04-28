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
import android.graphics.ColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class Scan extends AppCompatActivity {
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;
    public static final int STORAGE_PER_CODE = 103;
    public static final int LOCATION_PER_CODE = 106;
    public static final int MEDIA_LOCATION_CODE = 107;
    public int currentCode;
    String galleryFileName;

    DatabaseReference databaseReference;
    LocationManager locationManager;
    ImageView selectedImage;
    Button cameraBtn, galleryBtn;
    StorageReference storageReference;
    Button infBtn, notSureBtn;
    Button notInfBtn;
    ProgressBar progressBar;
    TextView stepCounter;
    TextView stepDescription;
    TextView beforeImage;
    Uri contentUri;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    FusedLocationProviderClient fusedLocationProviderClient;

    String imageDate;
    String imageTime;
    String imageLat;
    String imageLon;
    String imageStatus;

    Toast errorToast;

    boolean uploadImageInListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        stepCounter = findViewById(R.id.stepCounter);
        stepDescription = findViewById(R.id.stepDesc);
        selectedImage = findViewById(R.id.displayImageView);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        storageReference = FirebaseStorage.getInstance().getReference();
        infBtn = findViewById(R.id.infBtn);
        notInfBtn = findViewById(R.id.notInfBtn);
        notSureBtn = findViewById(R.id.notSureBtn);
        progressBar = findViewById(R.id.contact_progressbar);
        beforeImage = findViewById(R.id.before_image);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        askStoragePermissions();
        askCameraPermissions();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askLocationPermissions();
                currentCode = CAMERA_REQUEST_CODE;
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Images.Media.TITLE, "New Picture");
                contentValues.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                contentUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
                startActivityForResult(captureIntent, CAMERA_REQUEST_CODE);
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askLocationPermissions();
                currentCode = GALLERY_REQUEST_CODE;
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });

        infBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infBtn.setEnabled(false);
                notInfBtn.setEnabled(false);
                notSureBtn.setEnabled(false);
                if (ContextCompat.checkSelfPermission(Scan.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    errorToast = Toast.makeText(Scan.this, "Picture not uploaded; GPS permissions not granted!", Toast.LENGTH_SHORT);
                    View view = errorToast.getView();
                    view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    errorToast.show();
                    onBackPressed();
                }
                progressBar.setVisibility(View.VISIBLE);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                if (currentCode == CAMERA_REQUEST_CODE) {
                    imageStatus = "true";
                    uploadImageToFirebase("IMG_"+ timeStamp + ".jpg", contentUri, true);
                } else if (currentCode == GALLERY_REQUEST_CODE) {
                    imageStatus = "true";
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

        notInfBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infBtn.setEnabled(false);
                notInfBtn.setEnabled(false);
                notSureBtn.setEnabled(false);
                if (ContextCompat.checkSelfPermission(Scan.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    errorToast = Toast.makeText(Scan.this, "Picture not uploaded; GPS permissions denied!", Toast.LENGTH_SHORT);
                    View view = errorToast.getView();
                    view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    errorToast.show();
                    onBackPressed();
                }
                progressBar.setVisibility(View.VISIBLE);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                Log.d("RESULT", Integer.toString(currentCode));
                if (currentCode == CAMERA_REQUEST_CODE) {
                    imageStatus = "false";
                    uploadImageToFirebase("IMG_"+ timeStamp + ".jpg", contentUri, true);
                } else if (currentCode == GALLERY_REQUEST_CODE) {
                    imageStatus = "false";
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

    // if no permission request it to use the camera
    private void askCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }
    }

    private void askLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PER_CODE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PER_CODE);
            return;
        }
    }

    private void askMediaLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, MEDIA_LOCATION_CODE);
        }
    }


    // Ask for storage permission
    private void askStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PER_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PER_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("PERMISSIONCHECK", "Why are we already here");
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                // If the user deny the request to use the camera
                cameraBtn.setClickable(false);
                cameraBtn.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                beforeImage.setText("Camera permissions are required to use the \"capture\" option.");
            }
        }
        if (requestCode == STORAGE_PER_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                cameraBtn.setClickable(false);
                cameraBtn.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                galleryBtn.setClickable(false);
                galleryBtn.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                beforeImage.setText("Storage permissions required!");
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Added a super call here to resolve the error
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("RESULT", Integer.toString(resultCode));
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            try{
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), contentUri);
                selectedImage.setImageBitmap(imageBitmap);
            }catch(Exception ie){
                ie.printStackTrace();
            }
        }

        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "." + getFileExt(contentUri);
                Log.d("tag", "onActivityResult: Gallery Image Uri:  " + imageFileName);
                // Display the image on the app
                selectedImage.setImageURI(contentUri);
                galleryFileName = imageFileName;
            }

        }

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

    private void uploadImageToFirebase(String name, Uri contentUri, boolean isCapture) {
        final StorageReference image = storageReference.child("pictures/" + user.getUid() + "/" + name);

        if (isCapture) {
            uploadImageInListener = true;
            LocationRequest locationRequest = new LocationRequest()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(2000)
                    .setFastestInterval(1000);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
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
        else if (!isCapture){
            final AlertDialog.Builder uploadDialog = new AlertDialog.Builder(this);
            uploadDialog.setTitle("What would you like to use to geolocate the image?");
            uploadDialog.setItems(R.array.geo_options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which == 0){
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
                        String picturePath;
                        picturePath = getPath( getApplicationContext(), contentUri );
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
                        putFile(imageLat, imageLon, contentUri, image, name);
                    }
                }
            });

            uploadDialog.create().show();
        }
    }

    public void putFile(String imageLat, String imageLon, Uri contentUri, StorageReference image, String name){
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if(imageLat == null || imageLon == null){
                    errorToast = Toast.makeText(Scan.this, "Picture not uploaded, error fetching GPS coordinates.", Toast.LENGTH_SHORT);
                    View view = errorToast.getView();
                    view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    errorToast.show();
                    image.delete();
                    onBackPressed();
                }
                else if (imageLat.compareTo("0.0") == 0 && imageLon.compareTo("0.0") == 0){
                    errorToast = Toast.makeText(Scan.this, "Picture not uploaded, image has no GPS coordinates.", Toast.LENGTH_SHORT);
                    View view = errorToast.getView();
                    view.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    errorToast.show();
                    image.delete();
                    onBackPressed();
                }

                final boolean[] uploadFinished = {false};

                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSuccess: Uploaded Image URl is " + uri.toString());
                        uploadFinished[0] = true;
                    }
                });
                databaseReference = FirebaseDatabase.getInstance().getReference();

                String databaseName = name.replace('.', '-');

                String storageTimeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
                imageDate = storageTimeStamp.substring(0, 10);
                imageTime = storageTimeStamp.substring(11, 19);

                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("Date").setValue(imageDate);
                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("Time").setValue(imageTime);
                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("gpsLng").setValue(imageLon);
                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("gpsLat").setValue(imageLat);
                databaseReference.child("Pins").child(user.getUid()).child(databaseName).child("Status").setValue(imageStatus);

                Toast.makeText(Scan.this, "Upload Successful!", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Scan.this, "Upload Failed, no picture was taken!", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        });
    }
    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }
}

