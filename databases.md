# Enhancement Three: Databases

[Home](index.html) | [Code Review](code-review.html) | [Software Engineering](software-engineering.html) | [Algorithms](algorithms.html) | [Databases](databases.html)

## The artifact

An Android weight-tracking app built for **CS 360: Mobile Architecture and Programming**. Users register an account, log in, and record daily weight entries with optional notes. They can also set a goal weight and receive an SMS when they reach it. All data is stored in a local SQLite database through a `DatabaseHelper` class that extends `SQLiteOpenHelper`, with a `Users` table and a `WeightEntries` table.

## Original vs. enhanced

- [Original code: Weight Tracker](artifacts/weight-tracker/original/CS_360_Final_Project)
- [Enhanced code: Weight Tracker with salted hashing and enforced schema](artifacts/weight-tracker/enhanced/Weight_Tracker)

| | Original | Enhanced |
|---|---|---|
| Password storage | Plain text | Salted PBKDF2-HMAC-SHA256 via a new `PasswordHasher` class, per-user random salt, constant-time comparison, iteration count stored with the hash |
| Relationship between tables | `WeightEntries.username` linked to `Users` by convention only | Real `FOREIGN KEY` with `ON DELETE CASCADE`, enforcement enabled in `onConfigure()` |
| Indexing | None | Composite index on `(username, date)` |
| Goal weight and SMS opt-in | Goal weight in SharedPreferences | Both consolidated into the `Users` table |
| Schema upgrades | `onUpgrade()` dropped and recreated every table | Data-preserving migration |
| Registration | No password rules | Minimum password length |
| Testing | None for the data layer | JUnit tests for the hashing class |
| Documentation | Sparse method comments | Header comments on database methods |

## Key code

```java
// Salted password hashing using PBKDF2-HMAC-SHA256.
// Stored format: iterations$base64(salt)$base64(hash)
// Storing the iteration count lets it be raised later without breaking old hashes.
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    // Tune against current OWASP guidance and real device speed:
    // higher = harder to brute force, but slower to log in.
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    // Hashes a password with a fresh random salt
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS, KEY_BITS);
        return ITERATIONS + "$" + encode(salt) + "$" + encode(hash);
    }

    // Returns true if the password matches the stored hash string
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 3) return false;
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = derive(password, salt, iterations, expected.length * 8);
            // Constant-time comparison avoids leaking where the mismatch occurred
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false; // malformed stored value
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations, int bits) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
```

```java
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
```

## Narrative

### Why this artifact

I chose this artifact because it is the only project in my coursework where the database layer carries real responsibility for security and data integrity. It already did several things correctly. Every query uses Android's parameterized (`?`) placeholders, so it was not vulnerable to SQL injection. But my code review found real gaps between "works" and "well designed," and those gaps are what I set out to close.

The most important improvement is how passwords are stored. The original code saved and compared passwords in plain text, so anyone who extracted the database could read every user's password. I replaced that with salted hashing in a new `PasswordHasher` class using PBKDF2-HMAC-SHA256, a random per-user salt, and a constant-time comparison. The stored value includes the iteration count so it can be raised in the future without breaking existing accounts. I added JUnit tests for the hashing class to verify correct passwords, wrong passwords, and malformed stored values.

I also made the schema enforce its own rules. `WeightEntries.username` was only linked to `Users` by convention. It is now a real `FOREIGN KEY` with `ON DELETE CASCADE`, and foreign key enforcement is switched on in `onConfigure()`, since SQLite leaves it off by default. I added a composite index on `(username, date)` to match the query that screens actually run. I also consolidated the goal weight, which had been stored separately in SharedPreferences, into the `Users` table, so the user's data lives in one place. I moved the SMS opt-in setting there as well for the same reason, and I added a minimum password length to registration as part of the same security pass. Finally, I added header comments to the database methods. These changes demonstrate the database and security skills I developed during the program: schema design, constraints and indexing, schema versioning, secure credential storage, and writing code so that the data layer protects itself and does not rely on every caller behaving correctly.

### What I learned and the challenges I faced

The hardest part was not the hashing, which is only a small class. It was changing a live schema without destroying data. My original `onUpgrade()` dropped and recreated every table, which is fine in a course project but would wipe real users' data. SQLite also cannot add a foreign key to an existing table with `ALTER TABLE`, so I had to rebuild `WeightEntries` by renaming the old table, creating the new one with the constraint, copying the rows over, and dropping the old table. The migration also had to hash the passwords that were already stored in plain text and move each user's goal weight out of SharedPreferences.

I tested it by installing the original version, creating a user with entries, and then installing the new version over it. The user could still log in, the entries were intact, and the Android Studio Database Inspector showed hashes where plain-text passwords had been.

A smaller lesson came from the goal weight. SharedPreferences stored it as a float, so a value like 150.1 came out as 150.10000305 after conversion to a double. I fixed this by converting through a string.

Overall, this enhancement taught me that a database is a design with rules to enforce, not just a place to put data. I learned to make the database enforce its own constraints, to plan for schema evolution from the start, and to look at stored data the way an attacker would.

### Outcomes met

- [x] **Outcome 3:** Design a solution with sound practices while managing trade-offs. The clearest trade-off was the hash iteration count: a higher count makes brute-force attacks more expensive but slows login, and I chose a value that is secure and still fast on my test device.
- [x] **Outcome 4:** Use well-founded techniques and tools to deliver a working solution, shown by the data-preserving migration and the tested hashing class.
- [x] **Outcome 5:** Develop a security mindset that anticipates adversarial exploits. I treated the database as something an attacker might obtain and asked what they would find. Before, they would find every password; now they find salted hashes that are costly to attack.

[Download the narrative (.docx)](narratives/CS_499_Milestone_Four.docx)
