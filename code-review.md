# Code Review (Milestone One)

[Home](index.html) | [Code Review](code-review.html) | [Software Engineering](software-engineering.html) | [Algorithms](algorithms.html) | [Databases](databases.html)

## Video

[Watch the code review on YouTube](https://youtu.be/H-ZJf6Opbsw)

This informal code review was recorded before any enhancements were made. It walks through the existing functionality of each artifact, analyzes it using the CS 499 code review checklist, and explains the planned enhancements and the course outcomes they support.

| Artifact | Category | Origin |
|---|---|---|
| [Thermostat](software-engineering.html) | Software Design and Engineering | CS 350 |
| [Course advisor](algorithms.html) | Algorithms and Data Structures | CS 300 |
| [Weight Tracker](databases.html) | Databases | CS 360 |

## Script

The full script is below, with the on-screen cues removed. Expand each section to read it.

<details>
<summary><strong>Intro</strong></summary>

Hello, my name is Trevor Bell, and this is my Milestone One code review for CS 499. This review will walk through three existing artifacts from earlier in my Computer Science coursework that I will be enhancing for my ePortfolio, one for each of the course's three required categories: Software Design and Engineering, Algorithms and Data Structures, and Databases.

The first artifact is a Raspberry Pi–based embedded thermostat, written in Python, which represents my software design and engineering enhancement. The second artifact is a C++ console application that loads and searches a course catalog using a binary search tree, which represents my algorithms and data structures enhancement. The third artifact is an Android app for tracking daily weight entries, backed by a local SQLite database, which represents my databases enhancement.

For each artifact, I'll show the code, walk through specific findings using the CS 499 code review checklist, and explain the enhancement I'm planning and which course outcomes it supports.


</details>

<details>
<summary><strong>Artifact One: Software Design and Engineering - CS 350</strong></summary>

### Functionality

Let's start with software design and engineering. This artifact is a thermostat I built for CS-350, running on a Raspberry Pi. It's implemented in Thermostat.py.

The thermostat has three states: off, heat, and cool, cycled by a button press. A second and third button raise and lower the temperature setpoint. Depending on the state, a red or blue LED either holds solid or pulses, based on whether the current temperature is above or below the setpoint. A 16x2 LCD display alternates between showing the current date/time and the current temperature or thermostat status. Every 30 seconds, the thermostat also writes a status line: state, current temperature, and setpoint, out over the serial port to a separate temperature server.

### Code Review

Now let's walk through the code review checklist.

### Structure

Looking at the file, everything lives in one script: GPIO and sensor setup, the ManagedDisplay class, the TemperatureMachine state machine, button wiring, and the main loop are all in a single file. TemperatureMachine itself reaches directly into module-level globals: redLight, blueLight, ser, thSensor, screen, rather than receiving them as dependencies. This directly relates to the checklist's question about whether any modules are excessively complex and should be split into multiple routines.

There's also duplicated code: I have an earlier file, TemperatureSensorIntegration.py, from Module Six, and its ManagedDisplay class is copy-pasted identically into Thermostat.py. This is another checklist concern and could be condensed into one shared component.

And there are hardcoded values throughout instead of named constants: GPIO pin numbers like PWMLED(18) and Button(24), the serial settings port='/dev/ttyS0' and baudrate=115200, and the default setPoint = 72, all magic numbers with no configuration file backing them.

### Documentation

The header comments and most inline comments are clear, they explain what each class and method does, and there's a version history block at the top.

But it's not perfect: in the earlier TemperatureSensorIntegration.py file, the docstring above on_enter_Fahrenheit() says "Action performed when the state machine transitions into the Celsius state", which is a leftover copy-paste from the Celsius handler above it. An easy fix but a mistake.

### Variables

Most variable names are reasonably clear: redLight, blueLight, setPoint. The one that's a little opaque is the pair counter and altCounter inside manageMyDisplay(), two separate counters that both drive timing logic, one for alternating the display line and one for triggering the 30-second serial update. It works, but I don’t think the names alone make it obvious that's what they're doing.

### Arithmetic Operations

There's not much arithmetic in this file beyond the Fahrenheit conversion, there's no float equality comparison and no division by a variable that could hit zero. One small thing worth noting: updateLights() runs the current temperature through floor() before comparing it to the setpoint, so a reading like 71.9 degrees gets treated as 71, which is minor, but it shifts exactly when the state changes.

### Loops and Branches

The loops themselves are fine, termination conditions are clear, and counters are initialized right before their loop, not left over from a previous run. One small note: updateLights() has an if/elif for the heat and cool states but no explicit else, it relies on the separate on_enter_off() method to turn the lights off rather than having one unified default branch that covers all three states in one place.

### Defensive Programming

This is where the most significant issues are. getFahrenheit() reads thSensor.temperature directly, and that call has no error handling anywhere, not in getFahrenheit(), and not in manageMyDisplay(), which calls it inside a background thread. If the I2C sensor read fails for any reason, that exception is unhandled, and because it's happening inside a Thread, it just silently kills that thread; the display freezes, but the rest of the program keeps running with no error surfaced anywhere.

The same is true of ser.write(), if the serial connection drops, that call has no try/except either. And serial.Serial(...) itself runs at module load time, outside of any error handling, so if the serial device isn't available the moment the script starts, the whole program crashes before it even reaches the main loop.

On the positive side, cleanupDisplay() does properly deinitialize all six GPIO lines when the program exits, so resource cleanup on shutdown is handled well.

### Enhancements

My plan is to refactor this into separate, single-responsibility classes, SensorInterface, DisplayManager, ThermostatStateMachine, and SerialReporter, each passed into each other as dependencies instead of relying on module-level globals. This directly addresses the structural and duplication issues.

I'll wrap the sensor read and serial write calls in proper error handling with sensible fallback behavior, add a startup check around the serial connection instead of letting it crash at import time, and add bounds checking on setPoint so it can't be pushed to an unrealistic value. I'll also move the hardcoded pin numbers and serial settings into a configuration file.

Finally, since I own the hardware, I can test against the physical Pi, but I'll also add unit tests using mocked hardware interfaces, so the state machine and business logic can be verified without needing the device connected every time.

I believe this enhancement supports Course Outcome 3: designing and evaluating a computing solution while managing design trade-offs, and Outcome 4: applying well-founded tools and techniques to deliver a working solution.


</details>

<details>
<summary><strong>Artifact Two: Algorithms and Data Structures - CS 300</strong></summary>

### Functionality

Next is algorithms and data structures. This artifact is a C++ console application from CS-300. It reads a CSV file of course data: course number, course name, and a list of prerequisites, into a binary search tree keyed on course number. From a simple menu, the user can load the data structure, print the full course list in alphanumeric order using an in-order traversal, or look up a single course by number and see its prerequisites.

### Code Review

### Structure

There's an issue here in loadCourses(). It opens the CSV file and checks if (courseFile.is_open()), but if that check fails, there's no else branch and no return statement. The function falls off the end without returning a value, which is undefined behavior for a function declared to return bool. It happens to work in practice because the file is almost always present, but it's not correct.

There's also an unexplained magic number: while splitting each CSV line on commas, the code checks if (comma < 100) with no comment and no named constant explaining why 100 is the threshold.

And there's a logic bug in main(): bool filesLoaded = 1; is declared inside the menu's while loop, so it gets reset to true on every single iteration. The check in case 2, if (bst != nullptr && filesLoaded), looks like it's verifying a successful load happened, but because the variable resets every loop, that check doesn't reflect whether loading ever succeeded.

On the positive side, the recursive BST logic itself: addNode, printCourseList, printCourse, is clean and correctly implements standard binary search tree insert and in-order traversal behavior.

### Documentation

Comments are present throughout and generally match what the code does; each method has a short header comment, and there are inline comments walking through the BST insert and traversal logic.

### Variables

Most variable names are clear: courseNum, courseName, prerequisites. One naming issue: in main(), this line: for (auto& userChoice : courseId) userChoice = toupper(userChoice);, reuses the name userChoice for a char& loop variable, even though userChoice is already declared as an int in the outer scope. It compiles fine and technically shadows correctly, but reusing that name for something completely unrelated makes the code harder to follow.

### Arithmetic Operations

This category doesn't really apply here; there's no floating-point comparison and no division anywhere in this file, so there's no risk of these specific issues.

### Loops and Branches

The recursive traversal functions are correct, and their base cases are clear. The one thing worth noting isn't a loop, it's the missing return path in loadCourses() already mentioned under Structure, it’s the same defect, but it fits here since it's really about a branch that falls through without producing a defined result.

### Defensive Programming

The CSV-parsing code doesn't validate its input before using it: course.courseNum = courseInfo[0]; and course.courseName = courseInfo[1]; both index directly into the courseInfo vector with no check that it has at least two elements. A malformed row, such as a blank line or a row missing a comma, would cause an out-of-bounds access.

On the positive side, the file-open check I mentioned earlier does exist, courseFile.is_open(), so the program is checking for the file's existence before attempting to read it, even though the failure path itself isn't handled correctly.

### Enhancements

My plan is to convert this from a plain binary search tree into a self-balancing AVL tree, so lookups and inserts keep their O(log n) guarantee even in the worst case. For example, if course data happens to load in already-sorted order, which would degrade a plain BST toward O(n). I'll add empirical timing benchmarks comparing the original and AVL versions across progressively larger datasets to back that up with real numbers.

As part of the same pass, I'll fix the two defects I found: adding the missing return path in loadCourses(), replacing the magic number with a named constant, and correcting the filesLoaded logic so it actually reflects the CSV load result rather than resetting every loop iteration. I'll also add bounds checking before indexing into courseInfo.

I believe this enhancement supports Course Outcome 3: designing and evaluating a computing solution using algorithmic principles while managing trade-offs, and Outcome 4: applying well-founded, innovative tools and techniques to computing practice.


</details>

<details>
<summary><strong>Artifact Three: Databases - CS 360</strong></summary>

### Functionality

Last is databases. This artifact is an Android app from CS-360, written in Java, that lets a user register an account, log in, and track daily weight entries: date, weight, an optional note, an optional goal weight, and a phone number for SMS reminders. All of this is backed by a local SQLite database, managed through a DatabaseHelper class that extends SQLiteOpenHelper, with a Users table and a WeightEntries table.

### Code Review

### Structure

The WeightEntries table stores username as a plain text column that's meant to tie each entry back to its owner in the Users table, but there's no actual FOREIGN KEY constraint declared, the relationship is only implied by convention, not enforced by the database itself. There's also no index on the columns that lookup queries actually filter on, username and date.

Also, onUpgrade() currently just drops and recreates both tables on any version bump, which means a real schema change would wipe all existing user data rather than migrating it. That's fine for a course project with no real users, but it's a limitation.

### Documentation

Method-level comments are present but fairly sparse compared to my other two artifacts, most methods in DatabaseHelper don't have a header comment explaining their purpose, so this is an area I'd call out as needing improvement rather than something that's already clear.

### Variables

Column and variable names are clear and consistent, username, password, weight, date, no issues here.

### Arithmetic Operations

This category doesn't really apply to this artifact; weight values are stored and displayed as entered, with no floating-point equality comparisons or division happening in the database layer.

### Loops and Branches

Also, largely not applicable; the database methods are straightforward single-query operations, not loop-driven logic.

### Defensive Programming

This is where the most important finding is. In registerUser() and validateUser(), the password is stored and compared as plain text, there's no hashing and no salting anywhere in this file. If this database were ever extracted, every user's password would be immediately readable.

On the positive side, I want to be accurate here rather than overstate the problem: the queries themselves already use Android's parameterized db.query() API with ? placeholders throughout, so this code is not vulnerable to SQL injection as written, so that part is already done correctly and doesn't need to change.

### Enhancements

My primary enhancement is adding salted password hashing, hashing the password with a per-user random salt at registration, and comparing hashes rather than raw text at login. I'll also enable SQLite foreign key enforcement and add a real FOREIGN KEY constraint linking WeightEntries.username back to Users and add a composite index on username and date to speed up the lookups that already filter on both. Where useful, I'll also consolidate the goal weight, currently stored separately in SharedPreferences, into the database, so all user data lives in one consistent place.

I believe this enhancement supports Course Outcome 3: designing a solution using appropriate practices and standards while managing trade-offs, and Outcome 5: developing a security mindset that anticipates adversarial exploits and mitigates design flaws.


</details>

<details>
<summary><strong>Outro</strong></summary>

To wrap up: across all three artifacts, the pattern is the same, take something that already works and make it more maintainable, more efficient, or more secure, rather than starting over. The thermostat gets a modular refactor and proper error handling around its hardware calls. The course advising program gets a self-balancing data structure along with some bugs I found while reviewing it. And the weight tracking app gets proper password hashing and a more enforceable database schema.

Together, these three enhancements are designed to demonstrate all five course outcomes, from communicating technical decisions clearly, to reasoning about algorithmic trade-offs, and to applying a security mindset. Thanks for watching.

</details>

[Download the script (.docx)](narratives/CS_499_Milestone_One_Script.docx)
