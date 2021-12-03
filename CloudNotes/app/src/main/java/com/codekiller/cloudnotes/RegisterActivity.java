package com.codekiller.cloudnotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageActivity;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {

    private final int PROFILE_IMAGE_REQUEST = 1;

    EditText userName, uerMailid, userPassword;
    Button loginBtn, cancenBtn;
    CircleImageView userImage;
    ProgressDialog progressDialog;

    FirebaseAuth auth;
    StorageReference storageReference;
    DatabaseReference databaseReference;

    Uri imageUri;
    String userId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userName = findViewById(R.id.user_name);
        uerMailid = findViewById(R.id.user_mailid);
        userPassword = findViewById(R.id.user_password);
        userImage = findViewById(R.id.user_image);

        loginBtn = findViewById(R.id.login_btn);
        cancenBtn = findViewById(R.id.cancel_btn);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("Logging in ..");
        progressDialog.setMessage("please wait");

        auth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("Users");
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");


        cancenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YoYo.with(Techniques.Bounce).duration(600).playOn(cancenBtn);
                userPassword.setText("");
                uerMailid.setText("");
                userName.setText("");
                userImage.setImageResource(R.drawable.user_icon);
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YoYo.with(Techniques.Bounce).duration(600).playOn(loginBtn);
                if (uerMailid.getText().toString().length() != 0 && userPassword.getText().toString().length() != 0) {
                    if (userPassword.getText().length() >= 6) {
                        login();
                    } else {
                        toast("password must be above 6 characters");
                    }
                } else {
                    toast("fill all the fields");
                    userPassword.setText("");
                }
            }
        });

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(RegisterActivity.this, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, 100);
                } else {
                    startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), PROFILE_IMAGE_REQUEST);
                }
            }
        });

        userPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userPassword.setInputType(InputType.TYPE_CLASS_TEXT);
            }
        });

    }

    private void login() {

        progressDialog.show();
        auth.createUserWithEmailAndPassword(uerMailid.getText().toString(), userPassword.getText().toString())
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        userId = authResult.getUser().getUid();
                        if (imageUri != null) {
                            // storage reference
                            final StorageReference reference = storageReference.child(System.currentTimeMillis() + "." + getExtension(imageUri));
                            reference.putFile(imageUri)
                                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            reference.getDownloadUrl() // storage reference for download url
                                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                        @Override
                                                        public void onSuccess(Uri uri) {
                                                            HashMap<String, String> map = new HashMap<>();
                                                            map.put("name", userName.getText().toString());
                                                            map.put("mailid", uerMailid.getText().toString());
                                                            map.put("imageUrl", uri.toString());
                                                            map.put("userId", userId);
                                                            // database reference
                                                            DatabaseReference reference = databaseReference.child(userId);
                                                            reference.setValue(map)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void aVoid) {
                                                                            toast("profile uploaded successfully");
                                                                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                                            finish();
                                                                        }
                                                                    })
                                                                    .addOnFailureListener(new OnFailureListener() {
                                                                        @Override
                                                                        public void onFailure(@NonNull Exception e) {
                                                                            toast("database error");
                                                                            progressDialog.dismiss();
                                                                        }
                                                                    }); // database reference
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            toast("url couldn't generate");
                                                            progressDialog.dismiss();
                                                        }
                                                    }); // storage reference for download url
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            toast(e.toString());
                                            progressDialog.dismiss();
                                        }
                                    }); // storage reference
                        } else {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("name", userName.getText().toString());
                            map.put("mailid", uerMailid.getText().toString());
                            map.put("imageUrl", "default");
                            map.put("userId", userId);
                            // database reference
                            DatabaseReference reference = databaseReference.child(userId);
                            reference.setValue(map)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            toast("profile uploaded successfully");
                                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            toast("database error");
                                            progressDialog.dismiss();
                                        }
                                    }); // database reference
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        toast("couldn't register !!!");
                    }
                });

    }

    private String getExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PROFILE_IMAGE_REQUEST && data != null) {
            /*CropImage.activity(data.getData())
                    .setMultiTouchEnabled(true)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(RegisterActivity.this);*/
            imageUri = processDP(data.getData());
            Glide.with(RegisterActivity.this).load(imageUri).into(userImage);
        }/*else if( requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE ){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if( resultCode == RESULT_OK ){
            }
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), PROFILE_IMAGE_REQUEST);
        }
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
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "IMG_" + System.currentTimeMillis() + "." + getExtension(data));
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        compressImage.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
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
}