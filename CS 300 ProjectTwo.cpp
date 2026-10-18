//============================================================================
// Name        : ProjectTwo.cpp
// Author      : Trevor Bell
// Version     : 1.0
// Description : Project Two in C++
//============================================================================

#include <iostream>
#include <fstream>

#include "CSVparser.hpp"

using namespace std;

//============================================================================
// Global definitions visible to all methods and classes
//============================================================================

// define a structure to hold course information
struct Course {
    string courseNum;
    string courseName;
    vector<string> prerequisites;
    Course() {}
};

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

//============================================================================
// Binary Search Tree class definition
//============================================================================

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

//============================================================================
// Static methods used for testing
//============================================================================

/**
 * Load a CSV file containing courses into a container
 *
 * @param csvPath the path to the CSV file to load
 * @return a container holding all the bids read
 */
bool loadCourses(string csvPath, BinarySearchTree* bst) {
    cout << "Loading CSV file " << csvPath << endl;

    // initialize the CSV Parser using the given path
    csv::Parser file = csv::Parser(csvPath);

    try {
        // loop to read rows of a CSV file
        ifstream courseFile(csvPath);
        if (courseFile.is_open()) {
            while (!courseFile.eof()) {
                // Create a vector data structure 
                // add to the collection of courses
                vector<string> courseInfo;
                string courseData;
                getline(courseFile, courseData);
                while (courseData.length() > 0) {
                    unsigned int comma = courseData.find(',');
                    if (comma < 100) {
                            courseInfo.push_back(courseData.substr(0, comma));
                        courseData.erase(0, comma + 1);
                    }
                    else {
                        courseInfo.push_back(courseData.substr(0, courseData.length()));
                        courseData = "";
                    }
                }
                Course course;
                course.courseNum = courseInfo[0];
                course.courseName = courseInfo[1];
                for (unsigned int i = 2; i < courseInfo.size(); i++) {
                    course.prerequisites.push_back(courseInfo[i]);
                }
                bst->Insert(course);
            }
            courseFile.close();
            return true;
        }
    }
    catch (csv::Error& e) {
        std::cerr << e.what() << std::endl;
    }
}



/**
 * The one and only main() method
 */
int main(int argc, char* argv[]) {

    // Process command line arguments
    string csvPath, courseId;
    switch (argc) {
    case 2:
        csvPath = argv[1];
        break;
    case 3:
        csvPath = argv[1];
        courseId = argv[2];
        break;
    default:
        csvPath = "";
        break;
    }

    // Define a binary search tree to hold all courses
    BinarySearchTree* bst = nullptr;

    // Welcome user
    cout << "\nWelcome to the course planner.\n" << endl;

    // Menu
    string choice = "0";
    int userChoice = choice[0] - '0';

    while (userChoice != 9) {
        cout << " 1. Load Data Structure." << endl;
        cout << " 2. Print Course List." << endl;
        cout << " 3. Print Course." << endl;
        cout << " 9. Exit" << endl;
        cout << "\nWhat would you like to do? ";
        cin >> choice;

        if (choice.length() == 1)
            userChoice = choice[0] - '0';
        else
            userChoice = 0;

        bool filesLoaded = 1;

        switch (userChoice) {

        case 1:
            if (bst == nullptr) {
                bst = new BinarySearchTree();
            }
            if (csvPath.length() == 0) {
                cout << "Enter name of file that contains the course data: ";
                cin >> csvPath;
            }
            filesLoaded = loadCourses(csvPath, bst);
            if (filesLoaded) {
                cout << "Course data loaded.\n" << endl;
            }
            else {
                cout << "No file by that name.\n" << endl;
            }
            csvPath = "";
            break;
        case 2:
            if (bst != nullptr && filesLoaded) {
                cout << "Here is a sample schedule:\n" << endl;
                bst->PrintCourseList();
                cout << endl;
            }
            else
                cout << "Load Data Structure first.\n" << endl;
            break;
        case 3:
            if (bst != nullptr) {
                if (courseId.length() == 0) {
                    cout << "What course do you want to know about? ";
                    cin >> courseId;
                    for (auto& userChoice : courseId) userChoice =
                        toupper(userChoice);
                }
                bst->PrintCourse(courseId);
                cout << endl;
            }
            else
                cout << "Load Data Structure first.\n" << endl;
            courseId = "";
            break;
        default:
            if (userChoice != 9)
                cout << choice << " is not a valid option.\n" << endl;
            break;
        }
    }
    cout << "\nThank you for using the course planner!" << endl;
    return 0;
}
