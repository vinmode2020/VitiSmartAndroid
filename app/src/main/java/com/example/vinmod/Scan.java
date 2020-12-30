package com.example.vinmod;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class Scan extends AppCompatActivity {
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;
    public static final int STORAGE_PER_CODE = 103;
    public int currentCode;
    String galleryFileName;



    ImageView selectedImage;
    Button cameraBtn,galleryBtn;
    String currentPhotoPath;
    StorageReference storageReference;
    Button infBtn ,notSureBtn;
    Button notInfBtn;
    Camera mCamera;
    CameraPreview mPreview;
    TextView stepCounter;
    TextView stepDescription;
    File pictureFile;
    Uri contentUri;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
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

        askCameraPermissions();
        askStoragePermissions();

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
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCode = GALLERY_REQUEST_CODE;
                Intent gallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
                cameraBtn.setVisibility(View.INVISIBLE);
                galleryBtn.setVisibility(View.INVISIBLE);
                preview.setVisibility(View.INVISIBLE);
                // Now make the three buttons visible
                selectedImage.setVisibility(View.VISIBLE);
                infBtn.setVisibility(View.VISIBLE);
                notInfBtn.setVisibility(View.VISIBLE);
                notSureBtn.setVisibility(View.VISIBLE);

            }
        });

        infBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentCode == CAMERA_REQUEST_CODE){
                    contentUri = Uri.fromFile(pictureFile);
                    uploadImageToFirebase(pictureFile.getName(), contentUri);
                }
                else if (currentCode == GALLERY_REQUEST_CODE){
                    uploadImageToFirebase(galleryFileName, contentUri);
                }
                onBackPressed();
            }
        });

        notInfBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentCode == CAMERA_REQUEST_CODE){
                    contentUri = Uri.fromFile(pictureFile);
                    uploadImageToFirebase(pictureFile.getName(), contentUri);
                }
                else if (currentCode == GALLERY_REQUEST_CODE){
                    uploadImageToFirebase(galleryFileName, contentUri);
                }

                onBackPressed();
            }
        });

        // This will allow the notSureBtn to connect the Scan page to the Resource page.
        notSureBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent( Scan.this , Resource.class);
                startActivity(intent);
            }


        });

    }
// if no permission request it to use the camera
    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);}
//        }else {
//            dispatchTakePictureIntent();
//     Toast.makeText(Scan.this, "TEST ELSE Camera permission.", Toast.LENGTH_SHORT).show();
//        }

    }





        // Ask for storage permission
    /** This is not working as of now */
    private void askStoragePermissions() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
           ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.MANAGE_EXTERNAL_STORAGE}, STORAGE_PER_CODE );}
//        }else {
//            dispatchTakePictureIntent();
//     Toast.makeText(Scan.this, "TEST ELSE Camera permission.", Toast.LENGTH_SHORT).show();
//        }

    }



    // If the user granted the permission open the camera,
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CAMERA_PERM_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                recreate();
               // dispatchTakePictureIntent();
            }else {
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
                Log.d("tag", "ABsolute Url of Image is " + Uri.fromFile(pictureFile));

                // more info https://developer.android.com/training/camera/photobasics#java
                // Post the picture in the phone gallery.
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(pictureFile);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
             //   uploadImageToFirebase(f.getName(), contentUri);

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
             //   uploadImageToFirebase(imageFileName, contentUri);


            }

        }


    }

    private void uploadImageToFirebase(String name, Uri contentUri) {
        final StorageReference image = storageReference.child("pictures/" + name);
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSuccess: Uploaded Image URl is " + uri.toString());
                    }
                });

                Toast.makeText(Scan.this, "Image Is Uploaded.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Scan.this, "Upload Failled.", Toast.LENGTH_SHORT).show();
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

