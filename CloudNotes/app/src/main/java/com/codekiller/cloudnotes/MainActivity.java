package com.codekiller.cloudnotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.codekiller.cloudnotes.NotesActivity.arrayList;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    //public static ArrayList<String> arrayList;
    int noteId;
    private final int IMAGE_REQUEST = 1;
    public static String TAG = "Main Activity";

    CircleImageView userImage;
    TextView userName;
    Toolbar toolbar;

    SharedPreferences sharedPreferences;
    DatabaseReference databaseReference, databaseReferenceForNotes;
    String userId;

    ImageView saveBtn, clearBtn, readBtn, imageBtn, micBtn;
    EditText editText;
    BottomNavigationView bottomNavigationView;

    TextToSpeech textToSpeech;
    Intent speechRecognizerIntent;
    SpeechRecognizer speechRecognizer;
    ProgressDialog progressDialog;

    public static ConnectivityManager connectivityManager;
    public static NetworkInfo networkInfo;

    AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReferenceForNotes = FirebaseDatabase.getInstance().getReference("Notes");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        sharedPreferences = getSharedPreferences("com.codekiller.cloudnotes", Context.MODE_PRIVATE);

        userImage = findViewById(R.id.user_image);
        userName = findViewById(R.id.user_name);

        saveBtn = findViewById(R.id.save_btn);
        clearBtn = findViewById(R.id.clear_btn);
        readBtn = findViewById(R.id.read_btn);
        imageBtn = findViewById(R.id.image_btn);
        micBtn = findViewById(R.id.mic_btn);
        adView = findViewById(R.id.ad_view);

        editText = findViewById(R.id.edit_text);
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("please wait");

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        //arrayList = new ArrayList<>();

        textToSpeech = new TextToSpeech(MainActivity.this, this);

        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connectivityManager.getActiveNetworkInfo();

        //checkPermissions();
        initUserInfo();
        //initDatasFromFireBase();
        initSpeechRecognizer();

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        noteId = getIntent().getIntExtra("noteId", -1);
        if (noteId != -1) {
            editText.setText(arrayList.get(noteId));
        }

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setText("");
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText().toString().trim().length() != 0) {
                    if (noteId == -1) {
                        arrayList.add(editText.getText().toString().trim());
                    } else {
                        arrayList.set(noteId, editText.getText().toString().trim());
                    }
                    sharedPreferences.edit().putStringSet(userId, new HashSet<String>(arrayList)).apply();
                    //if( isNetworkAvailable() ){
                    DatabaseReference reference = databaseReferenceForNotes.child(userId);
                    HashMap<String, ArrayList<String>> map = new HashMap<>();
                    map.put("notes", arrayList);
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
                    //}
                    /*HashMap<String,ArrayList<String>> map = new HashMap<>();
                    map.put("notes",arrayList);
                    DatabaseReference reference = databaseReferenceForNotes.child(userId);
                    reference.setValue(map);*/
                    toast("text saved");
                } else {
                    toast("nothing to save");
                }
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.add_notes);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.saved_notes:
                        startActivity(new Intent(MainActivity.this, NotesActivity.class));
                        //overridePendingTransition(1,1);
                        finish();
                        return true;
                    case R.id.settings:
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        //overridePendingTransition(1,1);
                        finish();
                        return true;
                }

                return true;
            }
        });

        readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText().toString().trim().length() != 0) {
                    speak(editText.getText().toString());
                } else {
                    toast("nothing to read");
                }
            }
        });

        micBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        speechRecognizer.stopListening();
                        return true;
                    case MotionEvent.ACTION_DOWN:
                        speechRecognizer.startListening(speechRecognizerIntent);
                        toast("Listening ...");
                        return true;

                }

                return true;
            }
        });

        imageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), IMAGE_REQUEST);
            }
        });

    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {

            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                editText.setText(list.get(0));
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });

    }

    /*@SuppressLint("LongLogTag")
    private void initDatasFromFireBase() {

        DatabaseReference reference = databaseReferenceForNotes.child(userId);
        final HashSet<String> set = (HashSet<String>) sharedPreferences.getStringSet(userId,null);
        tempSet = new HashSet<>();
        if( set == null ){
            arrayList.add("no notes");
        }else{
            arrayList = new ArrayList<>(set);
        }
        Log.v("ARRAYLIST FROM SHAREDPREFERENCE",arrayList.toString());
        if( isNetworkAvailable() ){
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    HashMap<String,ArrayList<String>> map = (HashMap<String, ArrayList<String>>) dataSnapshot.getValue();
                    if( map != null ){
                        tempSet = new HashSet<>(map.get("notes"));
                        Log.v("TEMP SET",tempSet.toString());
                        set.addAll(tempSet);
                        arrayList = new ArrayList<>(set);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        Log.v("ARRAY LIST AFTER ADDED ALL", arrayList.toString());

    }*/

    private void initUserInfo() {
        databaseReference.child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        HashMap<String, String> map = (HashMap<String, String>) dataSnapshot.getValue();
                        userName.setText(map.get("name"));
                        if (map.get("imageUrl").equals("default")) {
                            Picasso.with(MainActivity.this).load(R.drawable.user_icon).into(userImage);
                        } else {
                            Picasso.with(MainActivity.this).load(map.get("imageUrl")).into(userImage);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        //toast("profile init error");
                    }
                });
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch ( item.getItemId() ){
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this,LoginActivity.class));
                finish();
                return true;
        }
        return true;
    }*/


    @Override
    protected void onPause() {
        super.onPause();
        textToSpeech.shutdown();
        //sharedPreferences.edit().putStringSet(userId,new HashSet<String>(arrayList)).apply();

        /*if( isNetworkAvailable() ){
            DatabaseReference reference = databaseReferenceForNotes.child(userId);
            HashMap<String,ArrayList<String>> map = new HashMap<>();
            map.put("notes",new ArrayList<String>(sharedPreferences.getStringSet("notes",null)));
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

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            progressDialog.show();
            convertImageToText(data.getData());
        }
    }

    private void convertImageToText(Uri uri) {
        try {
            FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(MainActivity.this, uri);
            FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
            textRecognizer.processImage(image)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                        @Override
                        public void onSuccess(FirebaseVisionText firebaseVisionText) {
                            if (firebaseVisionText.getText().toString().length() == 0) {
                                toast("select text image\nthis is non-text image");
                            } else {
                                editText.setText(firebaseVisionText.getText().toString());
                            }
                            progressDialog.dismiss();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast("couldn't recognize ..");
                            progressDialog.dismiss();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static boolean isNetworkAvailable() {
        return networkInfo != null && networkInfo.isConnected() && networkInfo.isAvailable();
    }

    public void speak(String s) {
        textToSpeech.speak(s, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onInit(int status) {

    }
}