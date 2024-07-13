package com.kouts.spiri.smartalert.Functionality;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.database.DataSnapshot;
import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.R;

public class LoginActivity extends AppCompatActivity {
    EditText loginEmail, loginPassword, registerEmail, registerPassword;

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
    }

    //Register User
    public void registerUser(View view) {
    //    if(registerPassword.getText().length() == 0 || registerEmail.getText().length() == 0) return;  //I don't think that's needed

        try {
            FirebaseDB.getAuth().createUserWithEmailAndPassword(registerEmail.getText().toString(), registerPassword.getText().toString()).addOnCompleteListener(
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                Helper.showToast(view.getContext(), "User created successfully", Toast.LENGTH_LONG);
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

    public void loginUser(View view) {
        if(loginPassword.getText().length() == 0 || loginEmail.getText().length() == 0) return;

        try {
            FirebaseDB.getAuth().signInWithEmailAndPassword(loginEmail.getText().toString(), loginPassword.getText().toString()).addOnCompleteListener(
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                Helper.showToast(view.getContext(), "User logged in successfully", Toast.LENGTH_LONG);
                                Intent intent = new Intent(view.getContext(), MainActivity.class);
                                startActivity(intent);


                            }
                            else {
                                Helper.showMessage(view.getContext(), "Error",task.getException().getLocalizedMessage());
                            }
                        }
                    }
            );
        }catch (Exception e) {
            Log.d("rerwer",e.toString());

        }
    }
    private void ValidateUser(Context c) {
        String userId = FirebaseDB.getAuth().getUid();
        if (userId != null) {
            FirebaseDB.getUserReference().child(userId).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChildren()) {
                        Intent intent = new Intent(c, MainActivity.class);
                        startActivity(intent);
                    }
                }
            });
        }
    }
}