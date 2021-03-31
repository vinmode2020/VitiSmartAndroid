package com.example.vinmod;

import android.Manifest;

import androidx.exifinterface.media.ExifInterface;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    String currentPhotoPath;
    StorageReference storageReference;
    Button infBtn, notSureBtn;
    Button notInfBtn;
    ProgressBar progressBar;
    Camera mCamera;
    CameraPreview mPreview;
    TextView stepCounter;
    TextView stepDescription;
    File pictureFile;
    Uri contentUri;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    FusedLocationProviderClient fusedLocationProviderClient;

    String imageDate;
    String imageTime;
    String imageLat;
    String imageLon;
    String imageStatus;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d("PICTURE", "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("PICTURE", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("PICTURE", "Error accessing file: " + e.getMessage());
            }

            selectedImage.setImageURI(Uri.fromFile(pictureFile));
            selectedImage.setRotation(90);
        }
    };


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

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        askCameraPermissions();
        askStoragePermissions();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mCamera = getCameraInstance();

        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCode = CAMERA_REQUEST_CODE;
                cameraBtn.setVisibility(View.INVISIBLE);
                galleryBtn.setVisibility(View.INVISIBLE);
                preview.setVisibility(View.INVISIBLE);
                infBtn.setVisibility(View.VISIBLE);
                notInfBtn.setVisibility(View.VISIBLE);
                notSureBtn.setVisibility(View.VISIBLE);
                selectedImage.setVisibility(View.VISIBLE);
                stepCounter.setText("Step 2");
                stepDescription.setText("Review the image and confirm its status.");
                mCamera.takePicture(null, null, mPicture);
                askLocationPermissions();
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCode = GALLERY_REQUEST_CODE;
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
                cameraBtn.setVisibility(View.INVISIBLE);
                galleryBtn.setVisibility(View.INVISIBLE);
                preview.setVisibility(View.INVISIBLE);
                // Now make the three buttons visible
                selectedImage.setVisibility(View.VISIBLE);
                infBtn.setVisibility(View.VISIBLE);
                notInfBtn.setVisibility(View.VISIBLE);
                notSureBtn.setVisibility(View.VISIBLE);
                askMediaLocationPermissions();

            }
        });

        infBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infBtn.setEnabled(false);
                notInfBtn.setEnabled(false);
                notSureBtn.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                if (currentCode == CAMERA_REQUEST_CODE) {
                    contentUri = Uri.fromFile(pictureFile);
                    imageStatus = "true";
                    uploadImageToFirebase(pictureFile.getName(), contentUri, true);
                } else if (currentCode == GALLERY_REQUEST_CODE) {
                    imageStatus = "true";
                    uploadImageToFirebase(galleryFileName, contentUri, false);
                }

                //onBackPressed();
            }
        });

        notInfBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infBtn.setEnabled(false);
                notInfBtn.setEnabled(false);
                notSureBtn.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                if (currentCode == CAMERA_REQUEST_CODE) {
                    contentUri = Uri.fromFile(pictureFile);
                    imageStatus = "false";
                    uploadImageToFirebase(pictureFile.getName(), contentUri, true);
                } else if (currentCode == GALLERY_REQUEST_CODE) {
                    imageStatus = "false";
                    uploadImageToFirebase(galleryFileName, contentUri, false);
                }

                //onBackPressed();
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
//        }else {
//            dispatchTakePictureIntent();
//     Toast.makeText(Scan.this, "TEST ELSE Camera permission.", Toast.LENGTH_SHORT).show();
//        }

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

    /** This is not working as of now */
    private void askStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PER_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PER_CODE);
        }
//        }else {
//            dispatchTakePictureIntent();
//     Toast.makeText(Scan.this, "TEST ELSE Camera permission.", Toast.LENGTH_SHORT).show();
//        }

    }


    // If the user granted the permission open the camera,
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                // If the user deny the request to use the camera
                Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Added a super call here to resolve the error
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                pictureFile = new File(currentPhotoPath);
                selectedImage.setImageURI(Uri.fromFile(pictureFile));
                Log.d("tag", "Absolute Uri of Image is " + Uri.fromFile(pictureFile));

                // more info https://developer.android.com/training/camera/photobasics#java
                // Post the picture in the phone gallery.
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(pictureFile);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);

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
            LocationRequest locationRequest = new LocationRequest()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(2000)
                    .setFastestInterval(1000);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            Location location = locationResult.getLastLocation();
                            imageLat = Double.toString(location.getLatitude());
                            imageLon = Double.toString(location.getLongitude());
                        }
                    },
                    Looper.myLooper());
        }
        else if (!isCapture){

            InputStream stream;
            String picturePath;

//            if(android.os.Build.VERSION.SDK_INT >= 29){
//                //this.grantUriPermission(getPackageName() , contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                this.grantUriPermission(getPackageName() , contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                contentUri = MediaStore.setRequireOriginal(contentUri);
//                try{
//                    stream = getContentResolver().openInputStream(contentUri);
//                    if(stream != null){
//                        try {
//                            ExifInterface exifInterface = new ExifInterface(stream);
//                            float[] latLong = new float[2];
//                            exifInterface.getLatLong(latLong);
//                            imageLat = Float.toString(latLong[0]);
//                            imageLon = Float.toString(latLong[1]);
//
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            //Toast.makeText(Scan.this, picturePath, Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }catch(FileNotFoundException fnfe){
//                    Log.d("INPUTSTREAM_ERROR", "File was not found.");
//                    fnfe.printStackTrace();
//                }
//            }
            if (1 == 1){
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
            }

        }



        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if(imageLat == null || imageLon == null){
                    Toast.makeText(Scan.this, "Picture not uploaded, error fetching GPS coordinates.", Toast.LENGTH_SHORT).show();
                    image.delete();
                    onBackPressed();
                    return;
                }
                else if (imageLat.compareTo("0.0") == 0 && imageLon.compareTo("0.0") == 0){
                    Toast.makeText(Scan.this, "Picture not uploaded, image has no GPS coordinates.", Toast.LENGTH_SHORT).show();
                    image.delete();
                    onBackPressed();
                    return;
                }

                final boolean[] uploadFinished = {false};

                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSuccess: Uploaded Image URl is " + uri.toString());
                        uploadFinished[0] = true;
                    }
                });

//                while(!uploadFinished[0]){
//                    Log.d("tag", "waiting for upload...");
//                }

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
                Toast.makeText(Scan.this, "Upload Failed.", Toast.LENGTH_SHORT).show();
            }
        });

    }



    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//    (If we don't want the picture to appear in the local gallery)    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // New folder called pictures
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
         Toast.makeText(Scan.this, "TEST dispatchTakePictureIntent ", Toast.LENGTH_SHORT).show();

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
            //  Toast.makeText(Scan.this, "TEST IF null Camera", Toast.LENGTH_SHORT).show();

                photoFile = createImageFile();
            } catch (IOException ex) {
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                // testing here......
                Toast.makeText(Scan.this, "Testing Camera 17", Toast.LENGTH_SHORT).show();
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    // Method for safely retrieving camera UI
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    // Create a File for saving an image or video
    private static File getOutputMediaFile(int type){

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ClusterScan");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

}

