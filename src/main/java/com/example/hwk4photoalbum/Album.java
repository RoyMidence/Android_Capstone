package com.example.hwk4photoalbum;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Album extends AppCompatActivity implements PhotoAdapter.itemClickInterface {
    final long ONE_MEGABYTE = 1024*1024*5;
    ActivityResultLauncher<Intent> otherActivityLauncher;

    List<Photo> photoList = new ArrayList<>();
    RecyclerView recyclerView;
    PhotoAdapter photoAdapter;

    private FirebaseStorage storage;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        setUpRecycler();


        otherActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == 0) {
                            Intent resultIntent = result.getData();
                            if (resultIntent != null) {
                                storeDataInArray();
                            }
                        }
                    }
                });
    }

    void setUpRecycler() {
        recyclerView = findViewById(R.id.recyclerView);
        photoAdapter = new PhotoAdapter(photoList,this);
        storeDataInArray();
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));
        recyclerView.setAdapter(photoAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // gets fresh values from database and resets recycler
    void storeDataInArray() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        db.collection("Hwk4PhotoAlbum")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            photoList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Create temporary Photo object to hold name
                                Photo temp = new Photo();
                                temp.setID(document.getId());
                                temp.setName(document.getString("Name"));
                                temp.setDate(document.getString("Date"));
                                temp.setCaption(document.getString("Caption"));
                                temp.setLocation(document.getString("Location"));
                                temp.setDetails(document.getString("Details"));

                                // Will need to get Photo associated with name
                                StorageReference imagesRef = storageRef.child("Hwk4PhotoAlbum/" + temp.getName() +".jpg");

                                // Get the actual Picture
                                imagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                    @Override
                                    public void onSuccess(byte[] bytes) {
                                        // Data for imagesRef is returns, use this as needed
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                                        temp.setPicture(bitmap);
                                        photoList.add(temp);
                                        photoAdapter.notifyDataSetChanged();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Handle any errors
                                    }
                                });
                            }
                            photoAdapter.setData(photoList);

                        } else {
                            Log.w("MYDEBUG", "Error getting documents.", task.getException());
                        }
                    }
                });// End of onComplete
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menuItemAddButton:
                // Go to add activity, get result
                intent = new Intent(Album.this, AddPhoto.class);
                otherActivityLauncher.launch(intent);
                return true;
            case R.id.menuItemSettings:
                // App Settings
                intent = new Intent(Album.this, UserPreferenceActivity.class);
                startActivity(intent);
                return true;
            case R.id.menuItemSortAZ:
                Collections.sort(photoList,Photo.photoComparatorAZ);
                photoAdapter.notifyDataSetChanged();
                return true;
            case R.id.menuItemSortNO:
                Collections.sort(photoList,Photo.photoDateComparatorNO);
                photoAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(Album.this,UpdatePhoto.class);
        intent.putExtra("Name",photoList.get(position).getName());
        intent.putExtra("Date", photoList.get(position).getDate());
        intent.putExtra("Location", photoList.get(position).getLocation());
        intent.putExtra("Caption", photoList.get(position).getCaption());
        intent.putExtra("Details", photoList.get(position).getDetails());
        intent.putExtra("ID", photoList.get(position).getID());
        otherActivityLauncher.launch(intent);
    }

}