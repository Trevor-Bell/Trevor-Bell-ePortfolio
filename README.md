# Trevor Bell | CS 499 Capstone ePortfolio

**Live site:** [Trevor Bell Computer Science ePortfolio](https://trevor-bell.github.io/Trevor-Bell-ePortfolio/)

This repository holds my Computer Science capstone ePortfolio for Southern New Hampshire University. It contains the original and enhanced versions of three artifacts from my coursework, the narratives explaining each enhancement, and my code review. The published site is the best place to start. It opens with my professional self-assessment and links to everything below.

## Artifacts

| Category | Artifact | Language | Origin | Enhancement |
|---|---|---|---|---|
| Software Design and Engineering | Raspberry Pi thermostat | Python | CS 350 | Refactored a single script into tested, single-responsibility modules with defensive hardware handling |
| Algorithms and Data Structures | Course advising program | C++ | CS 300 | Replaced an unbalanced binary search tree with an AVL tree and benchmarked both |
| Databases | Weight Tracker app | Java (Android) | CS 360 | Added salted password hashing, an enforced schema, and a data-preserving migration |

## Repository layout

```
artifacts/
  thermostat/         original/  enhanced/
  course-advisor/     original/  enhanced/
  weight-tracker/     original/  enhanced/
narratives/           Milestone narratives (Word documents)
assets/               Originally for assets used on the site...empty
index.md              Home page and professional self-assessment
code-review.md        Code review video and script
software-engineering.md, algorithms.md, databases.md
```

## Running the code

- **Thermostat:** The full application needs a Raspberry Pi with an AHT20 sensor, a 16x2 LCD, three buttons, and two LEDs. The unit tests use mocked hardware and run on any machine with Python and pytest.
- **Course advisor:** Compile the C++ source in `artifacts/course-advisor/enhanced/`. `Benchmark.cpp` compares the original and AVL trees.
- **Weight Tracker:** Open `artifacts/weight-tracker/enhanced/` in Android Studio and run it on an emulator or device.

## Contact

[LinkedIn](https://www.linkedin.com/in/trevorjoebell/) | [Email](mailto:trevorjoebell@gmail.com)
