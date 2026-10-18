package com.example.trevor_bell_cs_360_final_project;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    // UI elements
    CalendarView calendarView;
    TextView selectedDateTextView;
    ListView entriesListView;
    EditText weightEditText, notesEditText, goalWeightEditText;
    CheckBox smsOptInCheckBox;
    Button saveEntryButton, logoutButton;

    // App state
    String selectedDate;
    String loggedInUsername;
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Request SMS permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, 1);
        }

        // Get logged-in username from intent
        loggedInUsername = getIntent().getStringExtra("username");

        // Connect UI elements to layout
        calendarView = findViewById(R.id.calendarView);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
        entriesListView = findViewById(R.id.entriesListView);
        weightEditText = findViewById(R.id.weightEditText);
        notesEditText = findViewById(R.id.notesEditText);
        goalWeightEditText = findViewById(R.id.goalWeightEditText);
        smsOptInCheckBox = findViewById(R.id.smsOptInCheckBox);
        saveEntryButton = findViewById(R.id.saveEntryButton);
        logoutButton = findViewById(R.id.logoutButton);
        databaseHelper = new DatabaseHelper(this);

        // Set default date to today and load entries
        selectedDate = getTodayDate();
        selectedDateTextView.setText("Selected Date: " + formatDateForDisplay(selectedDate));
        loadEntriesForDate(selectedDate);

        // Load saved SMS opt-in preference
        boolean smsOptIn = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getBoolean("smsOptIn_" + loggedInUsername, true);
        smsOptInCheckBox.setChecked(smsOptIn);

        // Save opt-in preference when checkbox is toggled
        smsOptInCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("smsOptIn_" + loggedInUsername, isChecked)
                    .apply();
        });

        // Update selected date when user taps a new date
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            selectedDateTextView.setText("Selected Date: " + formatDateForDisplay(selectedDate));
            loadEntriesForDate(selectedDate);
        });

        // Handle Save Entry button click
        saveEntryButton.setOnClickListener(v -> {
            String weightText = weightEditText.getText().toString().trim();
            String notes = notesEditText.getText().toString().trim();
            String goalText = goalWeightEditText.getText().toString().trim();

            // Validate weight input
            if (weightText.isEmpty()) {
                Toast.makeText(this, "Please enter your weight", Toast.LENGTH_SHORT).show();
                return;
            }

            double weight = Double.parseDouble(weightText);
            double goalWeight = goalText.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(goalText);

            // Save weight entry to database
            boolean success = databaseHelper.saveWeightEntry(loggedInUsername, selectedDate, weight, notes);
            if (success) {
                Toast.makeText(this, "Entry saved for " + selectedDate, Toast.LENGTH_SHORT).show();
                loadEntriesForDate(selectedDate);

                // Clear input fields after saving
                weightEditText.setText("");
                notesEditText.setText("");
                goalWeightEditText.setText("");

                // Refresh entry list
                loadEntriesForDate(selectedDate);

                // Check if user entered a new goal weight
                String goalInput = goalWeightEditText.getText().toString();
                double savedGoal;

                if (!goalInput.isEmpty()) {
                    goalWeight = Double.parseDouble(goalInput);
                    databaseHelper.saveGoalWeight(loggedInUsername, goalWeight, this);
                    savedGoal = goalWeight;
                } else {
                    savedGoal = databaseHelper.getGoalWeight(loggedInUsername, this);
                }

                boolean smsEnabled = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        .getBoolean("smsOptIn_" + loggedInUsername, true);

                if (weight <= savedGoal && smsEnabled) {
                    String phoneNumber = databaseHelper.getPhoneNumber(loggedInUsername);
                    String message = "Congrats " + loggedInUsername + "! You hit your goal weight of " + savedGoal + " lbs.";

                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                        Toast.makeText(this, "Goal met! SMS sent.", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Goal met, but SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(this, "Failed to save entry.", Toast.LENGTH_SHORT).show();
            }
        });

        logoutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(DashboardActivity.this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Clear saved login state
                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                .edit()
                                .remove("loggedInUsername")
                                .apply();

                        // Navigate back to login screen and clear back stack
                        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // Load entries for the selected date and display in ListView
    private void loadEntriesForDate(String date) {
        List<WeightEntry> entries = databaseHelper.getEntryListForDate(loggedInUsername, date);

        // Create adapter with delete and edit support
        EntryAdapter adapter = new EntryAdapter(this, entries, new EntryAdapter.OnEntryActionListener() {
            @Override
            public void onDeleteClick(int entryId) {
                // Show confirmation dialog before deleting
                new AlertDialog.Builder(DashboardActivity.this)
                        .setTitle("Delete Entry")
                        .setMessage("Are you sure you want to delete this entry?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            boolean deleted = databaseHelper.deleteEntryById(entryId);
                            if (deleted) {
                                Toast.makeText(DashboardActivity.this, "Entry deleted.", Toast.LENGTH_SHORT).show();
                                loadEntriesForDate(selectedDate); // Refresh list
                            } else {
                                Toast.makeText(DashboardActivity.this, "Failed to delete entry.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onEditClick(WeightEntry entry) {
                // Show edit dialog for selected entry
                showEditDialog(entry);
            }
        });
        entriesListView.setAdapter(adapter);
    }

    // Show dialog to edit weight and notes for an entry
    private void showEditDialog(WeightEntry entry) {
        View dialogView = getLayoutInflater().inflate(R.layout.edit_entry, null);
        EditText editWeight = dialogView.findViewById(R.id.editWeightEditText);
        EditText editNotes = dialogView.findViewById(R.id.editNotesEditText);

        // Pre-fill fields with existing data
        editWeight.setText(String.valueOf(entry.getWeight()));
        editNotes.setText(entry.getNotes());

        // Build and show dialog
        new AlertDialog.Builder(this)
                .setTitle("Edit Entry")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    double newWeight = Double.parseDouble(editWeight.getText().toString().trim());
                    String newNotes = editNotes.getText().toString().trim();

                    boolean updated = databaseHelper.updateEntry(entry.getId(), newWeight, newNotes);
                    if (updated) {
                        Toast.makeText(this, "Entry updated", Toast.LENGTH_SHORT).show();
                        loadEntriesForDate(selectedDate); // Refresh list
                    } else {
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Utility method to get today's date in yyyy-MM-dd format
    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    // Convert stored date to display format
    private String formatDateForDisplay(String storedDate) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
            Date date = dbFormat.parse(storedDate);
            return displayFormat.format(date);
        } catch (Exception e) {
            return storedDate; // fallback
        }
    }
}