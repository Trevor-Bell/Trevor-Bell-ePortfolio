# Trevor Bell | Computer Science ePortfolio

[Home](index.html) | [Code Review](code-review.html) | [Software Engineering](software-engineering.html) | [Algorithms](algorithms.html) | [Databases](databases.html)

## Professional Self-Assessment

I’m a developer who treats security and reliability as design requirements, not finishing touches. My strengths are in systems-minded engineering with a security mindset: understanding how a program behaves underneath, anticipating how it can fail or be attacked, and building it so it protects itself.

### My journey and goals
My path through the program began with Python scripting in IT 140 and Java in IT 145, where the goal was making programs work. It ended in courses like Secure Coding and Software Reverse Engineering, where the goal was understanding why code breaks, and how someone could make it break on purpose. The most significant growth came in the 300- and 400-level courses, where assignments stopped dictating the solution and asked me to justify my choices. That is the habit this ePortfolio is built to show.

### Collaborating in a team environment
Collaboration in my program happened mostly through written peer discussion, where I explained my process and responded to other students’ ideas. Explaining my approach in writing forced me to state my assumptions and justify trade-offs. Responding to others’ ideas taught me to engage with the reasoning behind a design instead of just approving or objecting to it. I carry those habits into a team setting: state decisions with their reasons, give feedback that is specific enough to act on, and treat disagreement as a way to find the better design. The code review in this portfolio uses the same approach. It was written to be understood by peers rather than by me alone.

### Communicating with stakeholders
Much of my coursework asked me to write for readers who were not in the room when I made a decision. In CS 405, I wrote a secure-development policy for a fictional company, Green Pace, and presented it in a recorded briefing, which meant explaining coding standards and threat levels to an audience that needed the reasoning, not just the rules. In CS 465, I wrote a software design document for a travel company’s web application that opened with an executive summary of the architecture and constraints, aimed at people deciding whether to approve the design. I’ve learned to lead with what the reader needs to decide, then support it with detail, and to adapt the same information for a developer, a manager, or a client.

### Data structures and algorithms
In CS 370, I implemented the learning loop for a deep Q-learning agent that navigates a maze to reach a treasure. The task was to complete the training function: choosing actions with an epsilon-greedy strategy, storing experience in a replay memory, and training a neural network on batches of that memory. In my run, the agent reached a 100% win rate at epoch 371, and I used the completion check to confirm it could solve the maze from the start rather than trusting the win rate alone. The assignment itself warns that a 100% win rate can happen by chance, and I followed that instead of stopping early. I also wrote about the exploration-versus-exploitation trade-off, in which the agent explores heavily at first and shifts toward what it has learned as it gains experience. The same principle runs through all my algorithm work: define what correct means, measure against it, and understand the trade-off behind each choice.

### Software engineering and databases
In CS 320, I built contact, task, and appointment services in Java with validation enforced in the constructors and JUnit tests covering each class, so bad data was rejected at the point it entered the system. In CS 340, I separated a Python CRUD module from a Dash dashboard so the database logic could be reused and tested apart from the interface, backed by MongoDB. In CS 465, I designed and built a full-stack MEAN application with a server-rendered customer site and an Angular admin interface sharing one database. In CS 405, I wrote fifteen Google Test unit tests, including negative tests that check invalid operations fail correctly. The common thread is that I design so each piece has one job and can be tested on its own.

### Security
Security is the center of my work. In CS 410, I analyzed a C++ program and documented six vulnerabilities: a buffer overflow from an unbounded `cin` into a fixed array, hardcoded credentials, no login lockout, missing input validation, missing authorization, and unencapsulated global data. For each, I wrote a specific fix. In CS 405, I implemented exception handling, XOR encryption with file I/O, and static analysis, and I wrote a case study of the Capital One breach, where a misconfigured firewall let an attacker use server-side request forgery to obtain cloud credentials. I also wrote journals on defense in depth and on why security cannot be left to the end of development. What connects these is a habit: I ask what an attacker would find, and what the system does when something goes wrong. In CS 410, I recommended salted hashing in place of a hardcoded password. In this portfolio, I implemented it in a real application.

## How the artifacts fit together

This portfolio contains three artifacts from earlier courses, each enhanced in a different area of computer science. They share one pattern: each began as a working project and became more reliable, more efficient, or more secure through deliberate engineering rather than a rewrite.

The Raspberry Pi thermostat shows software design and engineering. I broke a single-file script into focused modules with dependency injection, added defensive handling for hardware failures, and built a test suite that runs without the hardware. The course advising program shows algorithms and data structures. I replaced an unbalanced binary search tree with an AVL tree and measured both, including the sorted-input case where the original crashed. The Weight Tracker app shows databases and security. I replaced plain-text passwords with salted hashes, made the schema enforce its own rules, and migrated existing data without losing it.

Together they show how I work: I review before I change, I measure instead of assuming, and I treat reliability and security as design requirements. The code review shows each artifact before any changes, and the three enhancement pages show the results with the narratives behind them.

| Artifact | Category | Origin | Enhancement | Outcomes |
|---|---|---|---|---|
| [Raspberry Pi thermostat](software-engineering.html) | Software Design and Engineering | CS 350 | Refactored a single-file script into tested, single-responsibility modules with defensive hardware handling | 3, 4 |
| [Course advising program](algorithms.html) | Algorithms and Data Structures | CS 300 | Replaced an unbalanced binary search tree with an AVL tree and benchmarked both | 3, 4 |
| [Weight Tracker app](databases.html) | Databases | CS 360 | Added salted password hashing, an enforced schema, and a data-preserving migration | 3, 4, 5 |

The [code review](code-review.html) shows each artifact before any changes were made. Outcomes 1 and 2 (collaboration and communication) are supported by the code review video, the artifact narratives, and this self-assessment.

## Explore the portfolio

- [Code Review](code-review.html): video walkthrough of the original artifacts and the plan for each enhancement
- [Enhancement One: Software Design and Engineering](software-engineering.html)
- [Enhancement Two: Algorithms and Data Structures](algorithms.html)
- [Enhancement Three: Databases](databases.html)

## Contact

- GitHub: [Trevor-Bell](https://github.com/Trevor-Bell)
- LinkedIn: [trevorjoebell](https://www.linkedin.com/in/trevorjoebell/)
- Email: [trevorjoebell@gmail.com](mailto:trevorjoebell@gmail.com)
