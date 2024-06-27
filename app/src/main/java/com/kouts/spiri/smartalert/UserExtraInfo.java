package com.kouts.spiri.smartalert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UserExtraInfo extends AppCompatActivity {

    EditText name, lastname;

    String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_extra_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        name = findViewById(R.id.name);
        lastname = findViewById(R.id.lastname);

        Intent intent = getIntent();
        email = intent.getStringExtra("email");
    }

    public void registerUser(View view) {
        if(lastname.getText().length() == 0 || name.getText().length() == 0) {
            Helper.showMessage(view, "Warning", "Required Data are missing");
            return;
        }

        User newUser = new User(FirebaseDB.getAuth().getUid(), email, name.getText().toString(), lastname.getText().toString(), 1);

        FirebaseDB.addUser(newUser, new FirebaseDB.FirebaseUserListener() {
            @Override
            public void onUserRetrieved(User temp) {
                return;
            }

            @Override
            public void onUserAdded() {
                Helper.showToast(view, "User created successfully", Toast.LENGTH_LONG);
                Intent intent = new Intent(view.getContext(), MainActivity.class);
                startActivity(intent);
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }
}