# Enhancement Two: Algorithms and Data Structures

[Home](index.html) | [Code Review](code-review.html) | [Software Engineering](software-engineering.html) | [Algorithms](algorithms.html) | [Databases](databases.html)

## The artifact

A course advising program originally built for **CS 300: Data Structures and Algorithms: Analysis and Design**. The original `ProjectTwo.cpp` is a C++ console application that loads course data from a CSV file (course number, name, and prerequisites) and lets a user print the full catalog in alphanumeric order or look up a single course and its prerequisites. The original stored this data in a plain, unbalanced binary search tree.

## Original vs. enhanced

- [Original code: ProjectTwo.cpp](https://github.com/Trevor-Bell/Trevor-Bell-ePortfolio/tree/main/artifacts/course-advisor/original)
- [Enhanced code: AVL tree, CourseLoader, Benchmark](https://github.com/Trevor-Bell/Trevor-Bell-ePortfolio/tree/main/artifacts/course-advisor/enhanced/Course_Advising_Program)

| | Original | Enhanced |
|---|---|---|
| Data structure | Unbalanced binary search tree | `AVLTree` with height tracking and all four rotation cases |
| Worst-case height | O(n) on sorted input | O(log n) regardless of insertion order |
| CSV loading | Missing return on file-open failure, magic number, no bounds check | `CourseLoader.h` with all three defects fixed |
| Dependencies | `CSVparser.hpp` (constructed but never used) | Removed |
| Evidence | None | `Benchmark.cpp` timing both trees on synthetic datasets |

## Key code

```cpp
/**
 * Recursive insert with rebalancing. Follows the same
 * alphanumeric-ordering logic as the original BST's addNode, 
 * but returns the root of the subtree it was called on,
 * so the caller can correctly re-link it.
 */
AVLNode* AVLTree::insertNode(AVLNode* node, Course course) {
    // Standard BST insert: find the correct leaf position
    if (node == nullptr) {
        return new AVLNode(course);
    }

    if (course.courseNum.compare(node->course.courseNum) < 0) {
        node->left = insertNode(node->left, course);
    }
    else if (course.courseNum.compare(node->course.courseNum) > 0) {
        node->right = insertNode(node->right, course);
    }
    else {
        // Duplicate course number
        return node;
    }

    // Update this node's height now that a child may have changed
    node->height = 1 + max(getHeight(node->left), getHeight(node->right));

    // Check the balance factor and rotate if this node has become
    // unbalanced as a result of the insert
    int balance = getBalance(node);

    // Left-left case
    if (balance > 1 && course.courseNum.compare(node->left->course.courseNum) < 0) {
        return rotateRight(node);
    }

    // Right-right case
    if (balance < -1 && course.courseNum.compare(node->right->course.courseNum) > 0) {
        return rotateLeft(node);
    }

    // Left-right case
    if (balance > 1 && course.courseNum.compare(node->left->course.courseNum) > 0) {
        node->left = rotateLeft(node->left);
        return rotateRight(node);
    }

    // Right-left case
    if (balance < -1 && course.courseNum.compare(node->right->course.courseNum) < 0) {
        node->right = rotateRight(node->right);
        return rotateLeft(node);
    }

    // Already balanced
    return node;
}
```

## Evidence

Insert time on already-sorted (worst-case) input, original BST vs. AVL tree:

| Courses (n) | Original BST | AVL tree |
|---|---|---|
| 50 | 4537 | 1523 |
| 100 | 19253 | 3546 |
| 200 | 72628 | 7663 |
| 300 | 157811 | 12213 |
| 1,000+ | Stack overflow (crash) | Completed without issue |

Across sizes 50 to 300, the BST's insert time grew roughly 25 times slower, while the AVL tree's grew only about 8.5 times slower.

## Narrative

### Why this artifact

I selected this artifact for the Algorithms and Data Structures category because a code review of the original code identified a specific, well-defined algorithmic weakness. A plain binary search tree provides no guarantee about its own shape. If course data happens to load in an order close to sorted, which is a realistic possibility for a maintained course catalog, the tree degenerates toward a linked list, and every operation's cost grows from the expected O(log n) toward O(n). The review also found three smaller defects: `loadCourses()` had no return statement on its file-open failure path (undefined behavior), an unexplained magic number governed the CSV field-splitting logic, and CSV rows were indexed into without a bounds check, risking a crash on malformed input.

To address these, I built a new `AVLTree` class implementing the same public interface as the original `BinarySearchTree` (`Insert`, `PrintCourseList`, `PrintCourse`, `NumPrerequisites`), but with height tracking and the four standard AVL rotation cases (left-left, right-right, left-right, right-left) added to its insert logic, guaranteeing O(log n) worst-case height regardless of insertion order. I fixed all three code-review defects in a new `CourseLoader.h`. While implementing, I also discovered and removed an unused dependency: the original's `CSVparser.hpp` was constructed but never used for parsing. Every field was, and still is, split manually with `string::find`/`substr`/`erase`. Since the library's source is no longer available to me and it was never used, I removed the dependency entirely rather than reconstruct it.

I also built a benchmark (`Benchmark.cpp`) that times both trees against synthetic datasets, including a deliberately worst-case, already-sorted dataset, which produced the actual evidence for this enhancement's value. Across dataset sizes from 50 to 300, insert time for the original BST grew roughly 25 times slower while the AVL tree's grew only about 8.5 times slower over the same size range, which is consistent with the BST's total insert cost degrading toward O(n²) on sorted input versus the AVL tree's O(n log n). At moderately larger sizes (1,000 to 2,000 already-sorted courses), the original BST's recursive `addNode()` function stack-overflowed and crashed entirely, while the AVL tree handled the same input without issue.

### What I learned and the challenges I faced

The most significant thing I learned was the practical difference between a code review that reads code and one that actually runs it. The Milestone One code review caught three real defects through careful reading, but it could not have predicted the infinite-loop bug I found in `printCourse()`. The original logic only returned from its search loop when a matched course had zero prerequisites, meaning any course with one or more prerequisites would print correctly but then fall back into the search loop and match, and reprint, forever. This was present, unnoticed, in the original artifact the entire time. It only surfaced once I had real test data and used the program's third menu option against a course with prerequisites. Finding and fixing it reinforced that static review and dynamic testing catch genuinely different classes of defect, and that a "working" artifact I had submitted for a grade in a previous course still contained a defect nobody, including me, had triggered.

A related lesson came from the benchmark itself. My first attempt at a large, worst-case dataset (n = 10,000) didn't produce slow numbers; it crashed the program outright with a stack overflow, in a way that turned out to depend on both the original's recursive design and its `Insert(Course course)` signature passing the whole `Course` struct by value, compounding stack usage at every recursive level. Narrowing the dataset size down to find where the crash occurred, rather than treating it as a failure to work around, turned an inconvenient bug into compelling evidence for the enhancement.

Finally, removing the `CSVparser` dependency taught me to read code for what it actually does, not what its includes and object declarations imply it does. A constructed-but-unused object is easy to overlook in a first read, but it is a real dependency risk when that object's source becomes unavailable, as it did here.

### Outcomes met

- [x] **Outcome 3:** Design and evaluate computing solutions while managing trade-offs. Rather than simply asserting that an AVL tree is theoretically better, I measured the actual complexity difference and let the numbers, including the BST's outright crash, make the case.
- [x] **Outcome 4:** Use well-founded and innovative techniques, skills, and tools. This is shown by a correct implementation of all four AVL rotation cases and a reusable, templated `loadCourses()` function that lets both tree types be benchmarked against identical, correctly parsed data.

The benchmark's crash finding was unplanned and only strengthened the Outcome 3 evidence.

[Download the narrative (.docx)](narratives/CS_499_Milestone_Three.docx)
