package com.example.trevor_bell_cs_360_final_project;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database configuration
    private static final String DATABASE_NAME = "WeightTracker.db";
    private static final int DATABASE_VERSION = 1;

    // User table and columns
    private static final String TABLE_USERS = "Users";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    // Weight entry table and columns
    private static final String TABLE_ENTRIES = "WeightEntries";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_NOTES = "notes";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users table with phone number
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USERNAME + " TEXT PRIMARY KEY, " +
                COLUMN_PASSWORD + " TEXT, " +
                "phone TEXT)";
        db.execSQL(createUsersTable);

        // Create WeightEntries table
        String createEntriesTable = "CREATE TABLE " + TABLE_ENTRIES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_WEIGHT + " REAL, " +
                COLUMN_NOTES + " TEXT)";
        db.execSQL(createEntriesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop existing tables and recreate them
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRIES);
        onCreate(db);
    }

    // Register a new user if username is not taken
    public boolean registerUser(String username, String password, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if username already exists
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_USERNAME + "=?",
                new String[]{username}, null, null, null);

        if (cursor.getCount() > 0) {
            cursor.close();
            return false; // Username already exists
        }

        cursor.close();

        // Insert new user into Users table
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        values.put("phone", phone);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Validate login credentials
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);

        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        return isValid;
    }

    // Save a new weight entry for a user
    public boolean saveWeightEntry(String username, String date, double weight, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_NOTES, notes);

        long result = db.insert(TABLE_ENTRIES, null, values);
        return result != -1;
    }

    // Delete a weight entry by its ID
    public boolean deleteEntryById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete("WeightEntries", "id=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // Update an existing weight entry
    public boolean updateEntry(int id, double newWeight, String newNotes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("weight", newWeight);
        values.put("notes", newNotes);

        int rows = db.update("WeightEntries", values, "id=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // Get all entries for a specific user and date (used for ListView)
    public List<WeightEntry> getEntryListForDate(String username, String date) {
        List<WeightEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query("WeightEntries", null,
                "username=? AND date=?", new String[]{username, date},
                null, null, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
            String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
            list.add(new WeightEntry(id, weight, notes));
        }

        cursor.close();
        return list;
    }

    // Get entries for a specific date as a formatted string (used for TextView fallback)
    public String getEntriesForDate(String username, String date) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_ENTRIES, null,
                COLUMN_USERNAME + "=? AND " + COLUMN_DATE + "=?",
                new String[]{username, date}, null, null, null);

        StringBuilder result = new StringBuilder();
        while (cursor.moveToNext()) {
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT));
            String notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES));
            result.append("Weight: ").append(weight).append(" lbs");
            if (!notes.isEmpty()) {
                result.append("\nNotes: ").append(notes);
            }
            result.append("\n\n");
        }

        cursor.close();
        return result.length() > 0 ? result.toString().trim() : "(No entries yet)";
    }

    // Save goal weight using SharedPreferences
    public void saveGoalWeight(String username, double goalWeight, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        prefs.edit().putFloat("goalWeight_" + username, (float) goalWeight).apply();
    }

    // Retrieve goal weight from SharedPreferences
    public double getGoalWeight(String username, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        return prefs.getFloat("goalWeight_" + username, Float.MAX_VALUE);
    }

    // Get user's phone number for SMS alerts
    public String getPhoneNumber(String username) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS, new String[]{"phone"},
                COLUMN_USERNAME + "=?", new String[]{username},
                null, null, null);

        String phone = null;
        if (cursor.moveToFirst()) {
            phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        }

        cursor.close();
        return phone;
    }
}