//
// Course.h
//
// Defines the Course struct used throughout the application to represent
// a single course record: its number, name, and list of prerequisites.
// Placed into its own header so both BinarySearchTree.h and AVLTree.h 
// can share the same definition without duplicating it.
//

#ifndef COURSE_H
#define COURSE_H

#include <string>
#include <vector>

using namespace std;

struct Course {
    string courseNum;
    string courseName;
    vector<string> prerequisites;
    Course() {}
};

#endif // COURSE_H
