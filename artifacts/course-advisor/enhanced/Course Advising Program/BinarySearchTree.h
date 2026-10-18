//
// BinarySearchTree.h
//
// This is the original, unmodified binary search tree implementation from
// CS 300, moved into its own file so it can serve as an untouched
// baseline for benchmarking against the new AVLTree. No logic here has
// been changed from the original artifact.
//

#ifndef BINARYSEARCHTREE_H
#define BINARYSEARCHTREE_H

#include "Course.h"

// Internal structure for tree node
struct Node {
    Course course;
    Node* left;
    Node* right;

    // default constructor
    Node() {
        left = nullptr;
        right = nullptr;
    }

    // initialize with a course
    Node(Course aCourse) : Node() {
        this->course = aCourse;
    }
};

/**
 * Define a class containing data members and methods to
 * implement a binary search tree
 */
class BinarySearchTree {

private:
    Node* root;

    void addNode(Node* node, Course course);
    void printCourseList(Node* node);
    void printCourse(Node* node, string courseNum);

public:
    BinarySearchTree();
    virtual ~BinarySearchTree();
    void DeleteRecursive(Node* node);
    void Insert(Course course);
    int NumPrerequisites(Course course);
    void PrintCourseList();
    void PrintCourse(string courseNum);
};

#endif // BINARYSEARCHTREE_H
