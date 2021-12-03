package com.codekiller.cloudnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText editText;
    Button btn;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        editText = findViewById(R.id.edit_text);
        btn = findViewById(R.id.change_btn);
        toolbar =  findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YoYo.with(Techniques.Bounce).duration(600).playOn(btn);
                if( editText.getText().toString().trim().length() == 0 ){
                    YoYo.with(Techniques.Shake).duration(800).playOn(editText);
                    toast("fill your mail id");
                }else{
                    FirebaseAuth.getInstance().sendPasswordResetEmail(editText.getText().toString().trim())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    toast("check your mail");
                                    YoYo.with(Techniques.FadeOutUp).duration(800).playOn(editText);
                                    YoYo.with(Techniques.FadeOutDown).duration(800).playOn(btn);
                                    finish();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    toast("couldn't change \n something went wrong");
                                }
                            });
                }
            }
        });

    }
    public void toast(String s){
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}