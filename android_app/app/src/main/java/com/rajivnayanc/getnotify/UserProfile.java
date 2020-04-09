package com.rajivnayanc.getnotify;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

public class UserProfile extends AppCompatActivity implements View.OnClickListener {
    private static final int CHOOSE_IMAGE = 101;
    ImageView imageView;
    EditText displayName;
    Button saveButton,logoutButton,copyDeviceToken;
    ProgressBar progressBar, progressBar2;
    Uri uriProfilePicture;
    String profilePictureDownloadURL;
    SwipeRefreshLayout swipeRefreshLayout;
    FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        mAuth = FirebaseAuth.getInstance();
        imageView = findViewById(R.id.profile_picture);
        displayName = findViewById(R.id.display_name);
        saveButton = findViewById(R.id.button_save);
        logoutButton = findViewById(R.id.button_logout);
        progressBar = findViewById(R.id.progressbar);
        progressBar2 = findViewById(R.id.progressbar2);
        swipeRefreshLayout = findViewById(R.id.swiperefresh);
        copyDeviceToken = findViewById(R.id.copy_device_token);
        copyDeviceToken.setOnClickListener(this);
        imageView.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        logoutButton.setOnClickListener(this);

        loadUserInformation();
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadUserInformation();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void loadUserInformation() {
        FirebaseUser user = mAuth.getCurrentUser();
        user.reload();
        String displayPictureUrl;
        if(user!=null){
            if(user.getPhotoUrl()!=null){
                displayPictureUrl = user.getPhotoUrl().toString();
                imageView.setBackground(null);
                Glide.with(this)
                        .load(displayPictureUrl)
                        .placeholder(R.drawable.camera_icon)
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .into(imageView);
            }

            if(user.getDisplayName()!=null){
                displayName.setText(user.getDisplayName());
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.profile_picture:
                showImageChooser();
                break;

            case R.id.button_save:
                saveUserInfo();
                break;

            case R.id.button_logout:
                UserLogout();
                break;
            case R.id.copy_device_token:
                CopyTokenToClipBoard();
                break;
        }
    }

    private void CopyTokenToClipBoard() {
        String token = FireBaseMessagingService.getToken(this);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("token", token);
        clipboard.setPrimaryClip(clip);
    }

    private void UserLogout() {
        mAuth.signOut();
        finish();
        startActivity(new Intent(this,MainActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mAuth.getCurrentUser()==null){
            finish();
            startActivity(new Intent(this,MainActivity.class));
        }
    }

    private void saveUserInfo() {
        String UserDisplayName = displayName.getText().toString();
        if(UserDisplayName.isEmpty()){
            displayName.setError("Name is Required");
            displayName.requestFocus();
            return;
        }
        progressBar2.setVisibility(View.VISIBLE);
        FirebaseUser user = mAuth.getCurrentUser();
        if(user!=null){
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName(UserDisplayName)
                    .setPhotoUri(Uri.parse(profilePictureDownloadURL))
                    .build();
            user.updateProfile(profile)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            progressBar2.setVisibility(View.GONE);
                            if(task.isSuccessful()){
                                Toast.makeText(getApplicationContext(),"Profile Updated Successfully",Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(getApplicationContext(),task.getException().getMessage(),Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CHOOSE_IMAGE && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            uriProfilePicture = data.getData();
            try {

                ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),uriProfilePicture);
                Bitmap bitmap = ImageDecoder.decodeBitmap(source);
                imageView.setImageBitmap(bitmap);
                imageView.setBackground(null);
                uploadImageToFirebaseStorage();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private void uploadImageToFirebaseStorage() {
        StorageReference profileImageReference = FirebaseStorage.getInstance().getReference("ProfilePics/"+System.currentTimeMillis()+".jpg");
        if(uriProfilePicture!=null){
            progressBar.setVisibility(View.VISIBLE);
            profileImageReference.putFile(uriProfilePicture)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressBar.setVisibility(View.GONE);
                            if(taskSnapshot.getMetadata()!=null){
                                if(taskSnapshot.getMetadata().getReference()!=null){
                                    Task<Uri> result = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                                    result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            profilePictureDownloadURL=uri.toString();
                                        }
                                    });
                                }
                            }

                            Toast.makeText(getApplicationContext(),"Image Uploaded Successfully",Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG);
                        }
                    });
        }

    }

    private void showImageChooser(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Profile Picture"),CHOOSE_IMAGE);
    }
}
