package com.kouts.spiri.smartalert.Functionality;

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

import com.kouts.spiri.smartalert.Database.FirebaseDB;
import com.kouts.spiri.smartalert.Assistance.Helper;
import com.kouts.spiri.smartalert.Functionality.Activities.MainActivity;
import com.kouts.spiri.smartalert.POJOs.User;
import com.kouts.spiri.smartalert.R;

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
            String message = getString(R.string.required_data_is_missing);
            Helper.showMessage(view.getContext(), "Warning", message);
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
                Helper.showToast(view.getContext(),"Welcome "+newUser.getName()+"!",Toast.LENGTH_SHORT);

                Intent intent = new Intent(view.getContext(), MainActivity.class);
                startActivity(intent);
            }

            @Override
            public void onError(Exception e) {
                String message = getString(R.string.unknown_error_occurred_user_could_not_be_submitted);
                Helper.showMessage(view.getContext(), "Error", message);
            }
        });
    }
}