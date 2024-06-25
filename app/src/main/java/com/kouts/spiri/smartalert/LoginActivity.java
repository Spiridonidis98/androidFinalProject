package com.kouts.spiri.smartalert;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
}