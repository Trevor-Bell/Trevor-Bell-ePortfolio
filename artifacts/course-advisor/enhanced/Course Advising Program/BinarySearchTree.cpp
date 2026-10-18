//
// BinarySearchTree.cpp
//
// Original, unmodified binary search tree implementation, unchanged from
// CS 300 except for being split into its own file.
//

#include <iostream>
#include "BinarySearchTree.h"

using namespace std;

/**
 * Default constructor
 */
BinarySearchTree::BinarySearchTree() {
    // root is equal to nullptr
    root = nullptr;
}

/**
 * Destructor
 */
BinarySearchTree::~BinarySearchTree() {
    // recurse from root deleting every node
    DeleteRecursive(root);
}

/**
 * Delete nodes recursively
 */
void BinarySearchTree::DeleteRecursive(Node* node) {
    // if node exits recurse from root deleting every node
    if (node) {
        DeleteRecursive(node->left);
        DeleteRecursive(node->right);
        delete node;
        node = nullptr;
    }
}

/**
 * Insert a course
 */
void BinarySearchTree::Insert(Course course) {
    // if root equal to null ptr
    if (root == nullptr) {
        // root is equal to new node bid
        root = new Node(course);
    }
    // else
    else {
        // add Node root and bid
        this->addNode(root, course);
    }
}

/**
 * Return number of course prerequites
 */
int BinarySearchTree::NumPrerequisites(Course course) {
    // for course prequisites if size > 0 increment number
    // return number of course prerequites
    int num = 0;
    for (unsigned int i = 0; i < course.prerequisites.size(); i++) {
        if (course.prerequisites.at(i).length() > 0)
            num++;
    }
    // return prerequite number
    return num;
}

/**
 * PrintCourseList
 */
void BinarySearchTree::PrintCourseList() {
    // printCourseList root
    this->printCourseList(root);
}

/**
 * PrintCourse
 */
void BinarySearchTree::PrintCourse(string courseNum) {
    // printCourse root
    this->printCourse(root, courseNum);
}

/**
 * Sort alphanumerically
 */
void BinarySearchTree::addNode(Node* node, Course course) {
    // if current courseNum is less than the current node's courseNum
    // add new Node to the left
    if (node->course.courseNum.compare(course.courseNum) > 0) {
        // if no left node
        if (node->left == nullptr) {
            // add new Node to the left
            node->left = new Node(course);
        }
        // else
        else {
            // addNode left
            this->addNode(node->left, course);
        }
    }
    // else
    else {
        // if current courseNum is equal or greater
        // than the current node's courseNum
        if (node->right == nullptr) {
            // add new Node to the right
            node->right = new Node(course);
        }
        // else
        else {
            // addNode right
            this->addNode(node->right, course);
        }
    }
}

/*
 * Prints the loaded courses
 */
void BinarySearchTree::printCourseList(Node* node) {
    // if no node
    if (node != nullptr) {
        // print course list from left
        printCourseList(node->left);
        // print course number and course name
        cout << node->course.courseNum << ", "
            << node->course.courseName << endl;
        // print course list to the right
        printCourseList(node->right);
    }
    // return
    return;
}

/*
 * Prints a course
 */
void BinarySearchTree::printCourse(Node* current, string courseNum) {
    // while current course number is not equal to nullptr
    while (current != nullptr) {
        // if courseNum matches the current courseNum
        if (current->course.courseNum.compare(courseNum) == 0) {
            // Print course and get num prereqs
            cout << endl << current->course.courseNum << ", "
                << current->course.courseName << endl;
            unsigned int size = NumPrerequisites(current->course);
            cout << "Prerequisites: ";
            // Print prerequites if any
            unsigned int i = 0;
            for (i = 0; i < size; i++) {
                cout << current->course.prerequisites.at(i);
                if (i != size - 1) {
                    cout << ", ";
                }
            }
            // If no prerequisites, return
            if (i == 0) {
                cout << endl;
                return;
            }
        }
        // else if courseNum is smaller than the current courseNum
        else if (courseNum.compare(current->course.courseNum) < 0) {
            // set current left
            current = current->left;
        }
        // else
        else {
            // set current right
            current = current->right;
        }
    }
    // when current course number is equal to nullptr
    cout << "No course by that number." << endl;
}
