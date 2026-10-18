package com.example.trevor_bell_cs_360_final_project;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// SQLite access layer for the weight tracker.
// Schema v2: hashed passwords, enforced foreign key,
// composite index, and per-user goal weight / SMS opt-in stored
// in the Users table.
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database configuration
    private static final String DATABASE_NAME = "WeightTracker.db";
    private static final int DATABASE_VERSION = 2;

    // Users table
    private static final String TABLE_USERS = "Users";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password"; // stores the PBKDF2 hash string
    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_GOAL_WEIGHT = "goal_weight"; // NULL = no goal set
    private static final String COLUMN_SMS_OPT_IN = "sms_opt_in";   // 1 = opted in

    // Weight entries table
    private static final String TABLE_ENTRIES = "WeightEntries";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_NOTES = "notes";

    private static final String INDEX_ENTRIES_USER_DATE = "idx_entries_username_date";

    // Used only to migrate v1 values out of SharedPreferences
    private static final String LEGACY_PREFS = "UserPrefs";

    private static final String CREATE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USERNAME + " TEXT PRIMARY KEY NOT NULL, " +
                    COLUMN_PASSWORD + " TEXT NOT NULL, " +
                    COLUMN_PHONE + " TEXT, " +
                    COLUMN_GOAL_WEIGHT + " REAL, " +
                    COLUMN_SMS_OPT_IN + " INTEGER NOT NULL DEFAULT 1)";

    private static final String CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_ENTRIES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_DATE + " TEXT NOT NULL, " +
                    COLUMN_WEIGHT + " REAL NOT NULL, " +
                    COLUMN_NOTES + " TEXT NOT NULL DEFAULT '', " +
                    "FOREIGN KEY (" + COLUMN_USERNAME + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USERNAME + ") ON DELETE CASCADE)";

    private static final String CREATE_INDEX =
            "CREATE INDEX IF NOT EXISTS " + INDEX_ENTRIES_USER_DATE +
                    " ON " + TABLE_ENTRIES + "(" + COLUMN_USERNAME + ", " + COLUMN_DATE + ")";

    private final Context appContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    // SQLite does not enforce foreign keys unless this is switched on per connection
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS);
        db.execSQL(CREATE_ENTRIES);
        db.execSQL(CREATE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            migrateV1ToV2(db);
        }
    }

    // v1 -> v2 without losing data: adds columns, hashes the existing plaintext passwords,
    // moves goal weight / SMS opt-in out of SharedPreferences, and rebuilds WeightEntries.
    // Entries whose username has no matching user are dropped as orphans.
    private void migrateV1ToV2(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_GOAL_WEIGHT + " REAL");
        db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_SMS_OPT_IN +
                " INTEGER NOT NULL DEFAULT 1");

        SharedPreferences prefs = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        List<String[]> users = new ArrayList<>(); // {username, plaintext password}
        try (Cursor c = db.query(TABLE_USERS, new String[]{COLUMN_USERNAME, COLUMN_PASSWORD},
                null, null, null, null, null)) {
            while (c.moveToNext()) {
                users.add(new String[]{c.getString(0), c.getString(1)});
            }
        }

        for (String[] user : users) {
            String username = user[0];
            ContentValues values = new ContentValues();
            values.put(COLUMN_PASSWORD, PasswordHasher.hash(user[1]));

            String goalKey = "goalWeight_" + username;
            if (prefs.contains(goalKey)) {
                // Go through String so 150.1f becomes 150.1, not 150.10000305...
                values.put(COLUMN_GOAL_WEIGHT,
                        Double.parseDouble(Float.toString(prefs.getFloat(goalKey, 0f))));
                editor.remove(goalKey);
            }
            String smsKey = "smsOptIn_" + username;
            if (prefs.contains(smsKey)) {
                values.put(COLUMN_SMS_OPT_IN, prefs.getBoolean(smsKey, true) ? 1 : 0);
                editor.remove(smsKey);
            }
            db.update(TABLE_USERS, values, COLUMN_USERNAME + "=?", new String[]{username});
        }
        editor.apply();

        db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " RENAME TO WeightEntries_old");
        db.execSQL(CREATE_ENTRIES);
        db.execSQL("INSERT INTO " + TABLE_ENTRIES + " (id, username, date, weight, notes) " +
                "SELECT id, username, date, weight, COALESCE(notes, '') FROM WeightEntries_old " +
                "WHERE username IN (SELECT username FROM " + TABLE_USERS + ")");
        db.execSQL("DROP TABLE WeightEntries_old");
        db.execSQL(CREATE_INDEX);
    }

    // ---------------------------------------------------------------- Users

    // Registers a new user, storing only a salted hash of the password.
    // Returns false if the username is taken; the PRIMARY KEY constraint
    // enforces this atomically, so there is no check-then-insert race.
    public boolean registerUser(String username, String password, String phone) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, PasswordHasher.hash(password));
        values.put(COLUMN_PHONE, phone);
        return db.insert(TABLE_USERS, null, values) != -1;
    }

    // Checks login credentials by comparing against the stored salted hash
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_USERS, new String[]{COLUMN_PASSWORD},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null, "1")) {
            return c.moveToFirst() && PasswordHasher.verify(password, c.getString(0));
        }
    }

    // Returns the user's phone number for SMS alerts, or null if not found
    public String getPhoneNumber(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_USERS, new String[]{COLUMN_PHONE},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    // Saves the user's goal weight in pounds
    public boolean saveGoalWeight(String username, double goalWeight) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_GOAL_WEIGHT, goalWeight);
        return getWritableDatabase().update(TABLE_USERS, values,
                COLUMN_USERNAME + "=?", new String[]{username}) > 0;
    }

    // Returns the user's goal weight, or null if none has been set
    public Double getGoalWeight(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_USERS, new String[]{COLUMN_GOAL_WEIGHT},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null)) {
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getDouble(0);
            }
            return null;
        }
    }

    // Saves whether the user wants goal-reached SMS messages
    public boolean setSmsOptIn(String username, boolean optedIn) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_SMS_OPT_IN, optedIn ? 1 : 0);
        return getWritableDatabase().update(TABLE_USERS, values,
                COLUMN_USERNAME + "=?", new String[]{username}) > 0;
    }

    // Returns true if the user is opted in to SMS (default true)
    public boolean isSmsOptIn(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_USERS, new String[]{COLUMN_SMS_OPT_IN},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null)) {
            return !c.moveToFirst() || c.getInt(0) == 1;
        }
    }

    // -------------------------------------------------------------- Entries

    // Saves a weight entry. Returns false if the username doesn't exist.
    public boolean saveWeightEntry(String username, String date, double weight, String notes) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_NOTES, notes == null ? "" : notes);
        return getWritableDatabase().insert(TABLE_ENTRIES, null, values) != -1;
    }

    // Deletes a weight entry by its ID
    public boolean deleteEntryById(int id) {
        return getWritableDatabase().delete(TABLE_ENTRIES, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    // Updates the weight and notes of an existing entry
    public boolean updateEntry(int id, double newWeight, String newNotes) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_WEIGHT, newWeight);
        values.put(COLUMN_NOTES, newNotes == null ? "" : newNotes);
        return getWritableDatabase().update(TABLE_ENTRIES, values, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    // Returns all of a user's entries for a date, oldest first (uses the composite index)
    public List<WeightEntry> getEntryListForDate(String username, String date) {
        List<WeightEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_ENTRIES, null,
                COLUMN_USERNAME + "=? AND " + COLUMN_DATE + "=?",
                new String[]{username, date}, null, null, COLUMN_ID)) {
            while (c.moveToNext()) {
                list.add(new WeightEntry(
                        c.getInt(c.getColumnIndexOrThrow(COLUMN_ID)),
                        c.getDouble(c.getColumnIndexOrThrow(COLUMN_WEIGHT)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_NOTES))));
            }
        }
        return list;
    }

    // Returns a user's entries for a date as display text (TextView fallback)
    public String getEntriesForDate(String username, String date) {
        StringBuilder result = new StringBuilder();
        for (WeightEntry e : getEntryListForDate(username, date)) {
            result.append("Weight: ").append(e.getWeight()).append(" lbs");
            if (!e.getNotes().isEmpty()) {
                result.append("\nNotes: ").append(e.getNotes());
            }
            result.append("\n\n");
        }
        return result.length() > 0 ? result.toString().trim() : "(No entries yet)";
    }
}
