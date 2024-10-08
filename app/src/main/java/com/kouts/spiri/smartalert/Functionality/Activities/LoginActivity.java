package com.kouts.spiri.smartalert.Functionality.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Functionality.UserExtraInfo;
import com.kouts.spiri.smartalert.POJOs.User;
import com.kouts.spiri.smartalert.R;

public class LoginActivity extends AppCompatActivity {
    EditText loginEmail, loginPassword, registerEmail, registerPassword;
    ImageView languageImg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Helper.clearUser();

        ValidateUser(this);

        //initialize editText views
        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        registerEmail = findViewById(R.id.registerEmail);
        registerPassword = findViewById(R.id.registerPassword);


        //Fix viewFlipper animations
        ConstraintLayout loginCard = findViewById(R.id.loginView);
        ConstraintLayout registerCard = findViewById(R.id.RegisterView);
        ViewFlipper viewFlipper = findViewById(R.id.viewFlipper);

        findViewById(R.id.goToRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewFlipper.setInAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.flip_right_in));
                viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.flip_left_out));
                viewFlipper.showNext();
                loginCard.setVisibility(View.GONE);
                registerCard.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.goToLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewFlipper.setInAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.flip_right_in));
                viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.flip_left_out));
                viewFlipper.showNext();
                loginCard.setVisibility(View.VISIBLE);
                registerCard.setVisibility(View.GONE);
            }
        });
        // Set the saved locale before setting the content view
        String savedLanguage = Helper.getSavedLanguage(this);
        Helper.setLocale(this, savedLanguage);
        languageImg = findViewById(R.id.languageImg);

        if(savedLanguage.equals("en")) {
            languageImg.setImageResource(R.drawable.en);
        }
        else {
            languageImg.setImageResource(R.drawable.el);
        }

        //change language implementation
        languageImg.setOnClickListener(v -> {
            String currentLanguage = Helper.getSavedLanguage(LoginActivity.this);
            System.out.println(currentLanguage);
            if (currentLanguage.equals("en")) {
                Helper.setLocale(LoginActivity.this, "el");
            } else {
                Helper.setLocale(LoginActivity.this, "en");
            }
            // Restart activity to apply language change
            recreate();
        });

    }

    //Register User
    public void registerUser(View view) {

        try {
            FirebaseDB.getAuth().createUserWithEmailAndPassword(registerEmail.getText().toString(), registerPassword.getText().toString()).addOnCompleteListener(
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                Intent intent = new Intent(view.getContext(), UserExtraInfo.class);
                                intent.putExtra("email", registerEmail.getText().toString());
                                startActivity(intent);
                            }
                            else {
                                Helper.showMessage(view.getContext(),"Error",task.getException().getLocalizedMessage());
                            }
                        }
                    }
            );
        }catch (Exception e) {
            Log.d("error",e.toString());

        }
    }

    //Handle login
    public void loginUser(View view) {
        if(loginPassword.getText().length() == 0 || loginEmail.getText().length() == 0) {
            return;
        }
        FirebaseDB.getAuth().signInWithEmailAndPassword(loginEmail.getText().toString(), loginPassword.getText().toString()).addOnCompleteListener(
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Intent intent = new Intent(view.getContext(), MainActivity.class);
                            startActivity(intent);
                        }
                        else {
                            Helper.showMessage(view.getContext(), "Error",task.getException().getLocalizedMessage());
                        }
                    }
                }
        );
    }

    //check if logged in user exists in database
    private void ValidateUser(Context c) {
        String userId = FirebaseDB.getAuth().getUid();

        if (userId != null) {
            FirebaseDB.getUserInfo(FirebaseDB.getAuth().getUid(), new FirebaseDB.FirebaseUserListener() {
                @Override
                public void onUserRetrieved(User user) {
                    if (user != null) { //if user exists, move to MainActivity
                        Helper.setUser(user);
                        Helper.showToast(c,"Welcome back "+user.getName()+"!",Toast.LENGTH_SHORT);
                        Intent intent = new Intent(c, MainActivity.class);
                        startActivity(intent);
                    }
                    else {
                        String message = getString(R.string.user_not_found_in_the_database);
                        Helper.showToast(c, message, Toast.LENGTH_LONG);
                        Intent intent = new Intent(c, UserExtraInfo.class);
                        intent.putExtra("email", FirebaseDB.getAuth().getCurrentUser().getEmail());
                        startActivity(intent);
                        Log.d("USER NOT FOUND ERROR", "User not found in the database");


                    }
                }

                @Override
                public void onUserAdded() {

                }

                @Override
                public void onError(Exception e) {
                    Log.d("USER NOT FOUND ERROR", "User not found in the database");
                    String message = getString(R.string.user_not_found_in_the_database);
                    Helper.showMessage(c, "Error",message);
                }
            });
        }
    }
}