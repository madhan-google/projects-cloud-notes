package com.codekiller.cloudnotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import javax.xml.parsers.SAXParser;

import de.hdodenhof.circleimageview.CircleImageView;

//https://indexbound.blogspot.com/2021/01/cloud-notes-about-help.html
public class SettingsActivity extends AppCompatActivity {

    String userId;
    private final int IMAGE_REQUEST_CODE = 1;
    public static final String TAG = "Settings Activity";

    CircleImageView userImage;
    TextView userNameView, forgotPasswordView;
    EditText userName;
    ImageView editBtn, saveBtn, clearBtn;
    ProgressBar progressBar;
    ImageView logoutbtn;

    Uri imageUri;
    SharedPreferences sharedPreferences;

    StorageReference storageReference;
    DatabaseReference databaseReference;
    HashMap<String, String> map;

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userImage = findViewById(R.id.user_image);
        userNameView = findViewById(R.id.user_name_view);
        userName = findViewById(R.id.user_name);
        forgotPasswordView = findViewById(R.id.password_reset_view);
        logoutbtn = findViewById(R.id.logout_btn);

        editBtn = findViewById(R.id.edit_btn);
        saveBtn = findViewById(R.id.save_btn);
        clearBtn = findViewById(R.id.clear_btn);

        progressBar = findViewById(R.id.progress_bar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        sharedPreferences = getApplicationContext().getSharedPreferences("com.codekiller.cloudnotes", Context.MODE_PRIVATE);
        storageReference = FirebaseStorage.getInstance().getReference("Users");
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        map = new HashMap<>();

        DatabaseReference reference = databaseReference.child(userId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                map = (HashMap<String, String>) dataSnapshot.getValue();
                userNameView.setText(map.get("name"));
                if (map.get("imageUrl").equals("default")) {
                    Glide.with(SettingsActivity.this).load(R.drawable.user_icon).into(userImage);
                } else {
                    Glide.with(SettingsActivity.this).load(map.get("imageUrl")).into(userImage);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        bottomNavigationView.setSelectedItemId(R.id.settings);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.add_notes:
                        startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                        //overridePendingTransition(1, 1);
                        finish();
                        return true;
                    case R.id.saved_notes:
                        startActivity(new Intent(SettingsActivity.this, NotesActivity.class));
                        //overridePendingTransition(1, 1);
                        finish();
                        return true;
                }

                return true;
            }
        });

        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userName.setVisibility(View.VISIBLE);
                userName.setText(userNameView.getText().toString());
                userNameView.setVisibility(View.GONE);
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userName.setVisibility(View.GONE);
                userNameView.setVisibility(View.VISIBLE);
            }
        });

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //onRequestPermissionsResult(100,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},null);
                if(ContextCompat.checkSelfPermission(SettingsActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(SettingsActivity.this,new String[]{
                         Manifest.permission.READ_EXTERNAL_STORAGE
                    },100);
                }else{
                    startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), IMAGE_REQUEST_CODE);
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userName.getText().toString().trim().length() != 0) {
                    DatabaseReference reference1 = databaseReference.child(userId);
                    map.put("name", userName.getText().toString().trim());
                    reference1.setValue(map);
                    toast("Name changed");
                    userName.setVisibility(View.GONE);
                    userNameView.setVisibility(View.VISIBLE);
                } else {
                    toast("enter name");
                }
            }
        });

        logoutbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YoYo.with(Techniques.Bounce).duration(600).playOn(logoutbtn);
                FirebaseAuth.getInstance().signOut();
                //sharedPreferences.getStringSet(userId,null).clear();
                startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                finish();
            }
        });

        forgotPasswordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YoYo.with(Techniques.Bounce).duration(600).playOn(forgotPasswordView);
                startActivity(new Intent(SettingsActivity.this, ForgotPasswordActivity.class));
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if( grantResults[0] == PackageManager.PERMISSION_GRANTED ){
            startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), IMAGE_REQUEST_CODE);
        }else{
            ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{permissions[0]},100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            /*CropImage.activity(data.getData())
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setMultiTouchEnabled(true)
                    .start(SettingsActivity.this);*/
            imageUri = processDP(data.getData());
            Log.d(TAG, "onActivityResult: image uri " + imageUri.toString());
            //Log.d(TAG, "onActivityResult: network avalibility true ");
            progressBar.setVisibility(View.VISIBLE);
            //Picasso.with(SettingsActivity.this).load(imageUri).into(userImage);
            //userImage.setVisibility(View.GONE);
            if( !map.get("imageUrl").equals("default") ) {
                StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(map.get("imageUrl"));
                imageRef.delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                toast("profile picture deleted");
                            }
                        });
            }
            final StorageReference reference = storageReference.child(System.currentTimeMillis() + "." + getExtension(imageUri));
            reference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            reference.getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            map.put("imageUrl", uri.toString());
                                            DatabaseReference reference = databaseReference.child(userId);
                                            reference.setValue(map)
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            toast("profile couldn't upload");
                                                            progressBar.setVisibility(View.GONE);
                                                            //.setVisibility(View.VISIBLE);
                                                        }
                                                    })
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            toast("profile upload successfully");
                                                            //userImage.setVisibility(View.VISIBLE);
                                                            progressBar.setVisibility(View.GONE);
                                                        }
                                                    });
                                        }
                                    });
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //imageUploadProgress.setText(taskSnapshot.getBytesTransferred()+" / "+taskSnapshot.getTotalByteCount()+" Bytes");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
        }/*else if( requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE ){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if( resultCode == RESULT_OK ){
            }
        }*/
    }

    private Uri processDP(Uri data) {
        Bitmap bitmap = BitmapFactory.decodeFile(getFullFilePath(data));
        double ratioSquare;
        int bitmapHeight, bitmapWidth;
        bitmapHeight = bitmap.getHeight();
        bitmapWidth = bitmap.getWidth();
        ratioSquare = (bitmapHeight * bitmapWidth) / 360000;
        if (ratioSquare <= 1)
            return data;
        double ratio = Math.sqrt(ratioSquare);
        int requireHeight = (int) Math.round(bitmapHeight / ratio);
        int requireWidth = (int) Math.round(bitmapWidth / ratio);
        Bitmap compressImage = Bitmap.createScaledBitmap(bitmap, requireWidth, requireHeight, true);

        FileOutputStream fileOutputStream = null;
        File f = Environment.getExternalStorageDirectory();
        File dir = new File(f.getAbsolutePath() + "/Cloud Notes/User Profile Images");
        if( !dir.exists() ){
            dir.mkdirs();
        }
        File file = new File(dir, "IMG_" + System.currentTimeMillis() + "." +  getExtension(data));
        if( !file.exists() ){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        compressImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
        try {
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(file);

    }

    private String getFullFilePath(Uri data) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(data, filePathColumn, null, null, null);
        int columnId = cursor.getColumnIndex(filePathColumn[0]);
        cursor.moveToFirst();
        return cursor.getString(columnId);
    }

    public String getExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}