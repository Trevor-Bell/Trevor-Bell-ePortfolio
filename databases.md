# Enhancement Three: Databases

[Home](index.html) | [Code Review](code-review.html) | [Software Engineering](software-engineering.html) | [Algorithms](algorithms.html) | [Databases](databases.html)

## The artifact

An Android weight-tracking app built for **CS 360: Mobile Architecture and Programming**. Users register an account, log in, and record daily weight entries with optional notes. They can also set a goal weight and receive an SMS when they reach it. All data is stored in a local SQLite database through a `DatabaseHelper` class that extends `SQLiteOpenHelper`, with a `Users` table and a `WeightEntries` table.

## Original vs. enhanced

- [Original code: Weight Tracker](artifacts/weight-tracker/original/)
- [Enhanced code: Weight Tracker with salted hashing and enforced schema](artifacts/weight-tracker/enhanced/)

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
// Paste PasswordHasher: PBKDF2-HMAC-SHA256, random salt,
// stored format with iteration count, constant-time comparison.
```

```java
// Paste onConfigure() enabling foreign keys, and the WeightEntries
// CREATE TABLE statement showing the FOREIGN KEY and the composite index.
```

## Evidence

Database Inspector after upgrading over the original install, showing hashes where plain-text passwords had been:

![Database Inspector showing hashed passwords](assets/databases-inspector.png)

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
