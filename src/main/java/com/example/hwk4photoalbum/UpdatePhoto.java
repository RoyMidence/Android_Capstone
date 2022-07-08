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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UpdatePhoto extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
    final long ONE_MEGABYTE = 1024*1024*5;

    ImageView imageViewUpdatePhoto;
    TextView textViewDateTaken;
    Button buttonUpdate, buttonDelete;
    EditText editTextUpdateName, editTextCaption, editTextLocation, editTextDetails;
    FloatingActionButton floatingActionButtonUpdate;

    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private String name;
    // Date stuff
    private SimpleDateFormat simpleDateFormat;
    private String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_photo);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        name = getIntent().getStringExtra("Name");

        imageViewUpdatePhoto = findViewById(R.id.imageViewUpdatePhoto);
        editTextCaption = findViewById(R.id.editTextCaption);
        editTextCaption.setText(getIntent().getStringExtra("Caption"));
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextLocation.setText(getIntent().getStringExtra("Location"));
        editTextDetails = findViewById(R.id.editTextDetails);
        editTextDetails.setText(getIntent().getStringExtra("Details"));

        textViewDateTaken = findViewById(R.id.textViewDateTaken);
        // get date
        // Its in MMMM dd, yyyy by default
        // sp.getString("date_format_preference", "MMMM dd, yyyy")
        date = getIntent().getStringExtra("Date");
        simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        try {
            Date d1 = simpleDateFormat.parse(date);
            simpleDateFormat = new SimpleDateFormat(sp.getString("date_format_preference", "MMMM dd, yyyy"));
            date = simpleDateFormat.format(d1);
            textViewDateTaken.setText(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Will need to get Photo associated with name
        StorageReference imagesRef = storageRef.child("Hwk4PhotoAlbum/" + name +".jpg");

        // Get the actual Picture
        imagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for imagesRef is returns, use this as needed
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                imageViewUpdatePhoto.setImageBitmap(bitmap);
            }
        });

        editTextUpdateName = findViewById(R.id.editTextUpdateName);
        editTextUpdateName.setText(name);

        buttonUpdate = findViewById(R.id.buttonUpdate);
        buttonDelete = findViewById(R.id.buttonDelete);
        floatingActionButtonUpdate = findViewById(R.id.floatingActionButtonUpdate);

        ActivityResultLauncher<Intent> otherActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent resultIntent = result.getData();
                            if (resultIntent != null) {
                                Bitmap bitmap = (Bitmap) resultIntent.getExtras().get("data");
                                imageViewUpdatePhoto.setImageBitmap(bitmap);
                            }
                        }
                    }
                });

        floatingActionButtonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                otherActivityLauncher.launch(intent);
            }
        });

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Delete Photo
                deletePhoto();

                // Now Delete associated document
                deleteDocument();
            }
        });

        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 if (editTextUpdateName.getText().toString().trim().equals("") ||
                         editTextCaption.getText().toString().trim().equals("") ||
                         editTextLocation.getText().toString().trim().equals("") ||
                         editTextDetails.getText().toString().trim().equals("")) {
                     Toast.makeText(getApplicationContext(),"Fill Out all fields", Toast.LENGTH_SHORT).show();
                     return;
                 }

                // Gather data in map
                Map<String, Object> details = new HashMap<>();
                details.put("Name", editTextUpdateName.getText().toString().trim());
                details.put("Caption", editTextCaption.getText().toString().trim());
                details.put("Location", editTextLocation.getText().toString().trim());
                details.put("Details", editTextDetails.getText().toString().trim());

                // Update Document First
                String id = getIntent().getStringExtra("ID");
                DocumentReference updateRef = db.collection("Hwk4PhotoAlbum").document(id);
                updateRef
                        .update(details)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(getApplicationContext(), "Document Updated", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), "Failed to Update", Toast.LENGTH_SHORT).show();
                            }
                        });

                // Time to Update Photo

                // I don't know how to check if Photo Changed so gonna delete old one and put new one
                // Name might have changed anyways too
                deletePhoto();

                // Now we go through the process of adding the Photo
                updatePhoto();
            }
        });

    }

    private void deletePhoto() {
        // Deleting with original name since might have changed edit Text
        StorageReference deleteRef = storageRef.child("Hwk4PhotoAlbum/" + name + ".jpg");

        deleteRef.delete().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Don't care for success, only failure, and it failed
                Toast.makeText(getApplicationContext(), "Failure to delete Image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteDocument() {
        // using document ID to determine what to delete
        String id = getIntent().getStringExtra("ID");

        db.collection("Hwk4PhotoAlbum").document(id)
                .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                // Worked, its gone, activity over
                Toast.makeText(getApplicationContext(), "Document Deleted", Toast.LENGTH_SHORT).show();

                // Finish and update Album
                Intent resultIntent = new Intent();
                setResult(0, resultIntent);
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Failed
                Toast.makeText(getApplicationContext(), "Failure to delete Document", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePhoto() {
        // Create Photo Reference
        // Using new name in editText
        StorageReference uploadRef = storageRef.child("Hwk4PhotoAlbum/" + editTextUpdateName.getText().toString().trim() + ".jpg");

        // Turn Photo into Bytes
        imageViewUpdatePhoto.setDrawingCacheEnabled(true);
        imageViewUpdatePhoto.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) imageViewUpdatePhoto.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        // Too lazy to set up Progress text
        Toast.makeText(getApplicationContext(), "Updating in Progress", Toast.LENGTH_SHORT).show();

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
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Code that should run when a preference is updated
        simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        try {
            Date d1 = simpleDateFormat.parse(date);
            simpleDateFormat = new SimpleDateFormat(sharedPreferences.getString(key, "MMMM dd, yyyy"));
            date = simpleDateFormat.format(d1);
            textViewDateTaken.setText(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}