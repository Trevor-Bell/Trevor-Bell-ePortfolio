//
// Benchmark.cpp
//
// Implementation of the AVLTree vs. BinarySearchTree timing comparison.
//

#include <iostream>
#include <sstream>
#include <chrono>
#include <algorithm>
#include <random>
#include <iomanip>
#include "Benchmark.h"
#include "AVLTree.h"
#include "BinarySearchTree.h"

using namespace std;
using namespace std::chrono;

vector<Course> generateSequentialCourses(int count) {
    vector<Course> courses;
    courses.reserve(count);
    for (int i = 0; i < count; i++) {
        Course c;
        // Zero-padded so string comparison order matches numeric order,
        // the way a real course catalog's numbering would.
        ostringstream numStream;
        numStream << "CSCI" << setw(5) << setfill('0') << i;
        c.courseNum = numStream.str();
        c.courseName = "Synthetic Course " + to_string(i);
        courses.push_back(c);
    }
    return courses;
}

vector<Course> generateShuffledCourses(int count) {
    vector<Course> courses = generateSequentialCourses(count);
    // Shuffle insertion order only
    unsigned seed = 42;  // fixed seed for reproducible benchmark runs
    shuffle(courses.begin(), courses.end(), default_random_engine(seed));
    return courses;
}

// Times how long it takes to insert an entire dataset into a fresh tree.
// Templated so the identical timing logic works for both tree types without duplicating it.
template <typename TreeType>
long long timeInsert(TreeType& tree, const vector<Course>& courses) {
    auto start = high_resolution_clock::now();
    for (const auto& course : courses) {
        tree.Insert(course);
    }
    auto end = high_resolution_clock::now();
    return duration_cast<microseconds>(end - start).count();
}

// Times a batch of PrintCourse lookups.
// Output is redirected to a discarded stringstream for the duration.
template <typename TreeType>
long long timeLookups(TreeType& tree, const vector<Course>& courses, int lookupCount) {
    ostringstream discard;
    streambuf* originalCoutBuffer = cout.rdbuf();
    cout.rdbuf(discard.rdbuf());

    auto start = high_resolution_clock::now();
    for (int i = 0; i < lookupCount; i++) {
        // (i * 37) % courses.size() spreads the lookups across the
        // dataset instead of always searching the same handful of
        // courses near the start of the vector.
        int index = (i * 37) % courses.size();
        tree.PrintCourse(courses[index].courseNum);
    }
    auto end = high_resolution_clock::now();

    cout.rdbuf(originalCoutBuffer);
    return duration_cast<microseconds>(end - start).count();
}

// Runs insert and lookup timing for both tree types against one dataset,
// and prints a formatted row of results.
void runOneComparison(const string& datasetLabel, const vector<Course>& courses, int lookupCount) {
    AVLTree avl;
    BinarySearchTree bst;

    long long avlInsertTime = timeInsert(avl, courses);
    long long bstInsertTime = timeInsert(bst, courses);

    long long avlLookupTime = timeLookups(avl, courses, lookupCount);
    long long bstLookupTime = timeLookups(bst, courses, lookupCount);

    cout << left << setw(28) << datasetLabel
        << right << setw(12) << avlInsertTime
        << setw(12) << bstInsertTime
        << setw(12) << avlLookupTime
        << setw(12) << bstLookupTime << endl;
}

void runBenchmark() {
    cout << "\n=== AVL Tree vs. Binary Search Tree Benchmark ===\n" << endl;
    cout << left << setw(28) << "Dataset"
        << right << setw(12) << "AVL Ins(us)"
        << setw(12) << "BST Ins(us)"
        << setw(12) << "AVL Lkp(us)"
        << setw(12) << "BST Lkp(us)" << endl;
    cout << string(76, '-') << endl;

    const int lookupCount = 1000;

    // Random-order insertion: a plain BST built from randomized input
    // tends to stay reasonably balanced by chance, so it can handle much larger
    // datasets here without recursing dangerously deep.
    vector<int> randomSizes = { 100, 1000, 5000, 10000 };
    for (int size : randomSizes) {
        auto shuffled = generateShuffledCourses(size);
        runOneComparison("Random order, n=" + to_string(size), shuffled, lookupCount);
    }

    cout << endl;

    // Sorted (worst-case) insertion: every insert goes the same
    // direction, so the plain BST degrades and its recursive addNode()
    // can stack-overflow at a surprisingly small n (confirmed during testing:
    // it crashed at n=1000 and n=2000 before these smaller sizes were chosen).
    vector<int> sortedSizes = { 50, 100, 200, 300 };
    for (int size : sortedSizes) {
        auto sequential = generateSequentialCourses(size);
        runOneComparison("Sorted (worst case), n=" + to_string(size), sequential, lookupCount);
    }

    cout << endl;

}
