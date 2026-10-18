package com.example.trevor_bell_cs_360_final_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // UI elements for login screen
    EditText usernameEditText, passwordEditText;
    Button loginButton, registerButton;

    // Database helper for user validation
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is already logged in
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String savedUsername = preferences.getString("loggedInUsername", null);

        if (savedUsername != null) {
            // Auto-login: skip login screen and go to dashboard
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("username", savedUsername);
            startActivity(intent);
            finish(); // prevent back navigation to login

            return;
        }

        // Connect UI elements to code
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        databaseHelper = new DatabaseHelper(this);

        // Handle login button click
        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Validate input fields
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check credentials against database
            boolean isValid = databaseHelper.validateUser(username, password);
            if (isValid) {
                // Save login state
                preferences.edit().putString("loggedInUsername", username).apply();

                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

                // Navigate to dashboard
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
                finish(); // prevent back navigation to login
            } else {
                Toast.makeText(this, "Invalid username or password. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigate to registration screen
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear input fields when returning to log in screen
        usernameEditText.setText("");
        passwordEditText.setText("");
    }
}