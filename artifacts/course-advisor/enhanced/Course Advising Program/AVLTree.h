//
// AVLTree.h
//
// Declares a self-balancing AVL tree, matching the public interface of the
// original BinarySearchTree class (Insert, PrintCourseList, PrintCourse,
// NumPrerequisites) so the rest of the program can use either
// interchangeably.
//

#ifndef AVLTREE_H
#define AVLTREE_H

#include "Course.h"

// Internal structure for an AVL tree node. Adds a height field, which a
// plain BST node doesn't need, since height is what the balancing logic
// uses to detect and correct an imbalance after each insert.
struct AVLNode {
    Course course;
    AVLNode* left;
    AVLNode* right;
    int height;

    AVLNode() {
        left = nullptr;
        right = nullptr;
        height = 1;
    }

    AVLNode(Course aCourse) : AVLNode() {
        this->course = aCourse;
    }
};

class AVLTree {
private:
    AVLNode* root;

    // Internal recursive helpers
    AVLNode* insertNode(AVLNode* node, Course course);
    void printCourseList(AVLNode* node);
    void printCourse(AVLNode* node, string courseNum);
    void deleteRecursive(AVLNode* node);

    // AVL-specific helpers
    int getHeight(AVLNode* node);
    int getBalance(AVLNode* node);
    AVLNode* rotateRight(AVLNode* node);
    AVLNode* rotateLeft(AVLNode* node);

public:
    AVLTree();
    virtual ~AVLTree();
    void Insert(Course course);
    int NumPrerequisites(Course course);
    void PrintCourseList();
    void PrintCourse(string courseNum);
};

#endif // AVLTREE_H
