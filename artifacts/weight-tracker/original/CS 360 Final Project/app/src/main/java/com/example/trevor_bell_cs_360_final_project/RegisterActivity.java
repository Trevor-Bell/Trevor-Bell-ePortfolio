package com.example.trevor_bell_cs_360_final_project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    // UI elements for registration screen
    EditText newUsernameEditText, newPasswordEditText, phoneEditText;
    Button confirmRegisterButton;

    // Database helper for storing user info
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Link UI elements to layout
        newUsernameEditText = findViewById(R.id.newUsernameEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        confirmRegisterButton = findViewById(R.id.confirmRegisterButton);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Handle registration button click
        confirmRegisterButton.setOnClickListener(v -> {
            // Get input values from fields
            String username = newUsernameEditText.getText().toString().trim();
            String password = newPasswordEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();

            // Validate input fields
            if (username.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Attempt to register user in database
            boolean success = databaseHelper.registerUser(username, password, phone);
            if (success) {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                finish(); // Return to login screen
            } else {
                Toast.makeText(this, "Username already exists. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}