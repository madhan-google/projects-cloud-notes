package com.codekiller.cloudnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotesActivity extends AppCompatActivity {

    String userId;
    HashSet<String> tempSet;
    HashSet<String> set;
    public static ArrayList<String> arrayList;
    public static final String TAG = "Notes Activity";

    ListView listView;
    CircleImageView userImage;
    TextView userName;
    ProgressBar progressBar;

    BottomNavigationView bottomNavigationView;
    Toolbar toolbar;

    StorageReference storageReference;
    DatabaseReference databaseReference, databaseReferenceForNotes;

    SharedPreferences sharedPreferences;
    ListViewAdapter listViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        listView = findViewById(R.id.list_view);
        userImage = findViewById(R.id.user_image);
        userName = findViewById(R.id.user_name);
        progressBar = findViewById(R.id.progress_bar);


        bottomNavigationView = findViewById(R.id.bottom_navigation);
        toolbar = findViewById(R.id.toolbar);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReferenceForNotes = FirebaseDatabase.getInstance().getReference("Notes");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        sharedPreferences = getApplicationContext().getSharedPreferences("com.codekiller.cloudnotes", Context.MODE_PRIVATE);


        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        checkPermissions();
        initToolBar();
        initDatasFromFirebase();

        tempSet = new HashSet<>();
        set = new HashSet<>();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivity(new Intent(NotesActivity.this, MainActivity.class)
                        .putExtra("noteId", position));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                new AlertDialog.Builder(NotesActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("DELETE")
                        .setMessage("are you sure")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                arrayList.remove(position);
                                listViewAdapter.notifyDataSetChanged();
                                sharedPreferences.edit().putStringSet(userId, new HashSet<String>(arrayList)).apply();
                                //listView.setAdapter(new ListViewAdapter(NotesActivity.this,arrayList));
                                /*if( MainActivity.isNetworkAvailable() ){
                                    DatabaseReference reference = databaseReferenceForNotes.child(userId);
                                    HashMap<String,Object> map = new HashMap<>();
                                    map.put("notes",arrayList);
                                    reference.updateChildren(map)
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    toast("couldn't update");
                                                }
                                            });
                                    reference.setValue(map)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //toast("text uploaded successfully");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    toast("texts couldn't upload");
                                                }
                                            });
                                }*/
                                toast("deleted");
                            }
                        })
                        .setNegativeButton("NO", null)
                        .show();

                return true;
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.saved_notes);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.add_notes:
                        startActivity(new Intent(NotesActivity.this, MainActivity.class));
                        //overridePendingTransition(1, 1);
                        finish();
                        return true;
                    case R.id.settings:
                        startActivity(new Intent(NotesActivity.this, SettingsActivity.class));
                        //overridePendingTransition(1, 1);
                        finish();
                        return true;
                }

                return true;
            }
        });

    }

    private void initDatasFromFirebase() {
        tempSet = new HashSet<>();
        set = new HashSet<>();

        DatabaseReference reference = databaseReferenceForNotes.child(userId);
        tempSet = (HashSet<String>) sharedPreferences.getStringSet(userId, null);
        if (tempSet != null) {
            arrayList = new ArrayList<>(tempSet);
            listViewAdapter = new ListViewAdapter(NotesActivity.this, arrayList);
            listView.setAdapter(listViewAdapter);

            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    HashMap<String, ArrayList<String>> map = (HashMap<String, ArrayList<String>>) dataSnapshot.getValue();
                    if (map != null) {
                        set.addAll(map.get("notes"));
                    }
                    for (String s : set) {
                        if (!arrayList.contains(s)) {
                            arrayList.add(s);
                        }
                    }
                    //arrayList.addAll(tempSet);
                    listViewAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        } else {
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    HashMap<String, ArrayList<String>> map = (HashMap<String, ArrayList<String>>) dataSnapshot.getValue();
                    if (map != null) {
                        set.addAll(map.get("notes"));
                    }
                    arrayList = new ArrayList<>(set);
                    listViewAdapter = new ListViewAdapter(NotesActivity.this, arrayList);
                    listView.setAdapter(listViewAdapter);
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        }


    }

    private void initToolBar() {

        DatabaseReference reference = databaseReference.child(userId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                HashMap<String, String> map = (HashMap<String, String>) dataSnapshot.getValue();
                userName.setText(map.get("name"));
                if (map.get("imageUrl").equals("default")) {
                    Picasso.with(NotesActivity.this).load(R.drawable.user_icon).into(userImage);
                } else {
                    Picasso.with(NotesActivity.this).load(map.get("imageUrl")).into(userImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Picasso.with(NotesActivity.this).load(R.drawable.user_icon).into(userImage);
                userName.setText("User Name");
            }
        });

    }

    private void checkPermissions() {

        if (ContextCompat.checkSelfPermission(NotesActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) +
                ContextCompat.checkSelfPermission(NotesActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                ContextCompat.checkSelfPermission(NotesActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(NotesActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(NotesActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(NotesActivity.this, Manifest.permission.INTERNET)) {
                new AlertDialog.Builder(NotesActivity.this)
                        .setTitle("Permission Needed")
                        .setMessage("permissions partially granted")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(NotesActivity.this, new String[]{
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.INTERNET
                                }, 100);
                            }
                        })
                        .setNegativeButton("CANCEL", null)
                        .show();
            } else {
                ActivityCompat.requestPermissions(NotesActivity.this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET
                }, 100);
            }
        }

    }


    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }*/

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (arrayList != null) {
            sharedPreferences.edit().putStringSet(userId, new HashSet<String>(arrayList)).apply();
            DatabaseReference reference = databaseReferenceForNotes.child(userId);
            HashMap<String, Object> map = new HashMap<>();
            map.put("notes", arrayList);
            reference.updateChildren(map)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast("couldn't update");
                        }
                    });
        }
    }
}