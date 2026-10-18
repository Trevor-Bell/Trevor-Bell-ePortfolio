//
// Benchmark.h
//
// Times AVLTree vs. BinarySearchTree insert and lookup performance on
// synthetic datasets of increasing size, including a deliberately
// worst-case, already-sorted dataset. This is what actually demonstrates
// the point of the AVL enhancement: a plain BST degrades toward O(n) on
// sorted input, while a self-balancing AVL tree stays at O(log n)
// regardless of insertion order.
//

#ifndef BENCHMARK_H
#define BENCHMARK_H

#include <vector>
#include <string>
#include "Course.h"

// Builds a dataset of course numbers in already-sorted order (CSCI0000, CSCI0001, CSCI0002, ...).
// This is the worst case for a plain BST: each insert goes to the same side every
// time, producing a tree that's really just a linked list, O(n) deep.
std::vector<Course> generateSequentialCourses(int count);

// Builds the same dataset as above, but in randomized insertion order.
// This is a more typical, average-case scenario, where a plain BST
// usually stays reasonably balanced by chance rather than by guarantee.
std::vector<Course> generateShuffledCourses(int count);

// Runs the full comparison (both dataset types, insert and lookup timing, both tree types)
// and prints a results table to the console.
void runBenchmark();

#endif // BENCHMARK_H
