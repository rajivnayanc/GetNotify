package com.rajivnayanc.getnotify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class SignUp extends AppCompatActivity implements View.OnClickListener {

    EditText editTextEmail,editTextPassword;
    ProgressBar progressBar;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        editTextEmail = findViewById(R.id.email);
        editTextPassword=findViewById(R.id.password);
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_up_button).setOnClickListener(this);
        progressBar = findViewById(R.id.progressbar);
        mAuth = FirebaseAuth.getInstance();
    }
    private void registerUser(){
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        if(email.isEmpty()){
            editTextEmail.setError("Email is Required");
            editTextEmail.requestFocus();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            editTextEmail.setError("Enter a valid Email");
            editTextEmail.requestFocus();
            return;
        }
        if(password.isEmpty()){
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }
        if(password.length()<6){
            editTextPassword.setError("Minimum Length of password is 6.");
            editTextPassword.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressBar.setVisibility(View.GONE);
                if(task.isSuccessful()){
                    FirebaseUser user = mAuth.getCurrentUser();
                    if(user!=null)
                        user.sendEmailVerification();

                    Toast.makeText(getApplicationContext(),"User Created Successfully!",Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(),"Verify your email address",Toast.LENGTH_LONG).show();
                    finish();
                    startActivity(new Intent(SignUp.this,MainActivity.class));
                }else{
                    if(task.getException() instanceof FirebaseAuthUserCollisionException) {
                        finish();
                        Toast.makeText(getApplicationContext(), "You're already registered", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(SignUp.this, MainActivity.class));
                    }

                    else
                        Toast.makeText(getApplicationContext(),task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sign_in_button:
                finish();
                startActivity(new Intent(this,MainActivity.class));
                break;

            case R.id.sign_up_button:
                registerUser();
                break;
        }
    }
}
