package com.example.hwk4photoalbum;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddPhoto extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    EditText editTextPhotoName, editTextAddCaption, editTextAddLocation, editTextAddDetails;
    TextView textViewProgress, textViewDate;
    Button buttonAddPhoto, buttonSavePhoto;
    ImageView imageViewNewPhoto;
    boolean workAround; // Using this to see if they actually get a picture

    private FirebaseStorage storage;
    private FirebaseFirestore db;

    // Date stuff
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormat;
    private String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_photo);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);

        storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        textViewProgress = findViewById(R.id.textViewProgress);
        textViewDate = findViewById(R.id.textViewDateTaken);
        editTextPhotoName = findViewById(R.id.editTextPhotoName);
        editTextAddCaption = findViewById(R.id.editTextAddCaption);
        editTextAddLocation = findViewById(R.id.editTextAddLocation);
        editTextAddDetails = findViewById(R.id.editTextAddDetails);
        buttonAddPhoto = findViewById(R.id.buttonAddPhoto);
        buttonSavePhoto = findViewById(R.id.buttonSavePhoto);
        imageViewNewPhoto = findViewById(R.id.imageViewNewPhoto);

        // get date
        calendar = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat(sp.getString("date_format_preference", "MMMM dd, yyyy"));
        date = simpleDateFormat.format(calendar.getTime());
        textViewDate.setText(date);

        workAround = false;

        ActivityResultLauncher<Intent> otherActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent resultIntent = result.getData();
                            if (resultIntent != null) {
                                Bitmap bitmap = (Bitmap) resultIntent.getExtras().get("data");
                                imageViewNewPhoto.setImageBitmap(bitmap);
                                buttonAddPhoto.setVisibility(View.GONE);
                                imageViewNewPhoto.setVisibility(View.VISIBLE);
                                workAround = true;
                            }
                        }
                    }
                });

        buttonAddPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                otherActivityLauncher.launch(intent);
            }
        });

        buttonSavePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create Document for Photo
                if (editTextPhotoName.getText().toString().trim().equals("") ||
                        editTextAddCaption.getText().toString().trim().equals("") ||
                        editTextAddLocation.getText().toString().trim().equals("") ||
                        editTextAddDetails.getText().toString().trim().equals("")) {
                    Toast.makeText(getApplicationContext(),"Complete all fields", Toast.LENGTH_SHORT).show();
                } else {
                    db = FirebaseFirestore.getInstance();

                    Map<String, Object> details = new HashMap<>();
                    details.put("Name", editTextPhotoName.getText().toString().trim());
                    details.put("Caption", editTextAddCaption.getText().toString().trim());
                    details.put("Location", editTextAddLocation.getText().toString().trim());
                    details.put("Details", editTextAddDetails.getText().toString().trim());

                    // gotta enter correctly formated date
                    simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                    details.put("Date", simpleDateFormat.format(calendar.getTime()));

                    Task<DocumentReference> addedDocRef =
                            db.collection("Hwk4PhotoAlbum")
                                    .add(details)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            // Code for successfully adding goes here…
                                            Toast.makeText(getApplicationContext(), "Document Created", Toast.LENGTH_SHORT).show();
                                        }
                                    }); // Hopefully this worked

                    // Upload Photo into Storage
                    // Create Photo Reference
                    StorageReference uploadRef = storageRef.child("Hwk4PhotoAlbum/" + editTextPhotoName.getText().toString().trim() + ".jpg");

                    // Turn Photo into Bytes
                    imageViewNewPhoto.setDrawingCacheEnabled(true);
                    imageViewNewPhoto.buildDrawingCache();
                    Bitmap bitmap = ((BitmapDrawable) imageViewNewPhoto.getDrawable()).getBitmap();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] data = baos.toByteArray();

                    // Upload Photo
                    UploadTask uploadTask = uploadRef.putBytes(data);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Photo Upload Failed
                            Toast.makeText(getApplicationContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Upload Finished and Successful
                            Toast.makeText(getApplicationContext(), "Upload Successful", Toast.LENGTH_SHORT).show();
                            Intent resultIntent = new Intent();
                            setResult(0, resultIntent);
                            finish();
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            // Tell User upload Progress
                            imageViewNewPhoto.setVisibility(View.GONE);
                            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                            textViewProgress.setVisibility(View.VISIBLE);
                            textViewProgress.setText("Upload is " + progress + "% done");
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Code that should run when a preference is updated
        simpleDateFormat = new SimpleDateFormat(sharedPreferences.getString(key, "MMMM dd, yyyy"));
        date = simpleDateFormat.format(calendar.getTime());
        textViewDate.setText(date);

    }
}