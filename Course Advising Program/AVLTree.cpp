//
// AVLTree.cpp
//
// Implements the self-balancing AVL tree declared in AVLTree.h.
//

#include <iostream>
#include <algorithm>
#include "AVLTree.h"

using namespace std;

/**
 * Default constructor
 */
AVLTree::AVLTree() {
    root = nullptr;
}

/**
 * Destructor
 */
AVLTree::~AVLTree() {
    deleteRecursive(root);
}

/**
 * Delete nodes recursively
 */
void AVLTree::deleteRecursive(AVLNode* node) {
    if (node) {
        deleteRecursive(node->left);
        deleteRecursive(node->right);
        delete node;
        node = nullptr;
    }
}

/**
 * Returns a node's stored height, or 0 for a null node.
 */
int AVLTree::getHeight(AVLNode* node) {
    if (node == nullptr) {
        return 0;
    }
    return node->height;
}

/**
 * Returns a node's balance factor: the height of its left
 * subtree minus the height of its right subtree.
 */
int AVLTree::getBalance(AVLNode* node) {
    if (node == nullptr) {
        return 0;
    }
    return getHeight(node->left) - getHeight(node->right);
}

/**
 * AVL right rotation, used to correct a left-heavy imbalance.
 * Returns the new root of the rotated subtree.
 */
AVLNode* AVLTree::rotateRight(AVLNode* node) {
    AVLNode* newRoot = node->left;
    AVLNode* transferSubtree = newRoot->right;

    // Perform the rotation
    newRoot->right = node;
    node->left = transferSubtree;

    // Heights must be updated bottom-up
    node->height = 1 + max(getHeight(node->left), getHeight(node->right));
    newRoot->height = 1 + max(getHeight(newRoot->left), getHeight(newRoot->right));

    return newRoot;
}

/**
 * AVL left rotation, used to correct a right-heavy imbalance.
 * Mirror image of rotateRight above.
 */
AVLNode* AVLTree::rotateLeft(AVLNode* node) {
    AVLNode* newRoot = node->right;
    AVLNode* transferSubtree = newRoot->left;

    // Perform the rotation
    newRoot->left = node;
    node->right = transferSubtree;

    // Same bottom-up height update as rotateRight
    node->height = 1 + max(getHeight(node->left), getHeight(node->right));
    newRoot->height = 1 + max(getHeight(newRoot->left), getHeight(newRoot->right));

    return newRoot;
}

/**
 * Public entry point, mirrors the original BST's
 * Insert() but reassigns root.
 */
void AVLTree::Insert(Course course) {
    root = insertNode(root, course);
}

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

/**
 * Return number of course prerequites
 */
int AVLTree::NumPrerequisites(Course course) {
    int num = 0;
    for (unsigned int i = 0; i < course.prerequisites.size(); i++) {
        if (course.prerequisites.at(i).length() > 0)
            num++;
    }
    return num;
}

/**
 * Public entry point, unchanged in structure from the
 * original.
 */
void AVLTree::PrintCourseList() {
    this->printCourseList(root);
}

/**
 * Public entry point, unchanged in structure from the
 * original.
 */
void AVLTree::PrintCourse(string courseNum) {
    this->printCourse(root, courseNum);
}

/**
 * Identical in-order traversal logic to the
 * original BST's printCourseList.
 */
void AVLTree::printCourseList(AVLNode* node) {
    if (node != nullptr) {
        printCourseList(node->left);
        cout << node->course.courseNum << ", "
            << node->course.courseName << endl;
        printCourseList(node->right);
    }
    return;
}

/**
 * Identical search logic to the original BST's printCourse.
 */
void AVLTree::printCourse(AVLNode* current, string courseNum) {
    while (current != nullptr) {
        if (current->course.courseNum.compare(courseNum) == 0) {
            cout << endl << current->course.courseNum << ", "
                << current->course.courseName << endl;
            unsigned int size = NumPrerequisites(current->course);
            cout << "Prerequisites: ";
            unsigned int i = 0;
            for (i = 0; i < size; i++) {
                cout << current->course.prerequisites.at(i);
                if (i != size - 1) {
                    cout << ", ";
                }
            }
            cout << endl;
            return;  // always return once the match is found and printed
        }
        else if (courseNum.compare(current->course.courseNum) < 0) {
            current = current->left;
        }
        else {
            current = current->right;
        }
    }
    cout << "No course by that number." << endl;
}
