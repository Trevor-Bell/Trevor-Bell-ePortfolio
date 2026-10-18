//
// main.cpp
//
// Application entry point: the course planner menu. 
// Uses AVLTree as the primary data structure.
//

#include <iostream>
#include "AVLTree.h"
#include "CourseLoader.h"
#include "Benchmark.h"

using namespace std;

/**
 * Program entry point. Accepts up to two optional command-line
 * arguments (a CSV path, and a course number to look up). 
 * Runs the menu loop until the user selects option 9 to exit.
 */
int main(int argc, char* argv[]) {

    // Process command line arguments.
    // argc == 2 means a CSV path was supplied; 
    // argc == 3 means both a CSV path and a course number were supplied, 
    // letting the program be pre-loaded from the command line 
    // rather than typed at the menu.
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

    // avl is allocated on first successful "Load Data Structure" (case 1)
    // rather than here, so the program can start up without immediately
    // needing course data.
    AVLTree* avl = nullptr;

    // filesLoaded lives outside the menu loop and is declared
    // once, so it correctly reflects whether a load has ever actually
    // succeeded, rather than resetting to true on every loop iteration
    // the way the original's in-loop declaration did.
    bool filesLoaded = false;

    cout << "\nWelcome to the course planner.\n" << endl;

    // choice/userChoice drive the menu loop. userChoice is derived from
    // choice's first character so a non-numeric or multi-character entry
    // safely falls through to the default case instead of crashing.
    string choice = "0";
    int userChoice = choice[0] - '0';

    while (userChoice != 9) {
        cout << " 1. Load Data Structure." << endl;
        cout << " 2. Print Course List." << endl;
        cout << " 3. Print Course." << endl;
        cout << " 4. Run AVL vs. BST Benchmark." << endl;
        cout << " 9. Exit" << endl;
        cout << "\nWhat would you like to do? ";
        cin >> choice;

        if (choice.length() == 1)
            userChoice = choice[0] - '0';
        else
            userChoice = 0;

        switch (userChoice) {

        case 1:
            // Construct the tree once and reuse it across multiple
            // loads, so choosing "Load" more than once (e.g. after a
            // typo in the file path) doesn't leak the previous tree.
            if (avl == nullptr) {
                avl = new AVLTree();
            }
            if (csvPath.length() == 0) {
                cout << "Enter name of file that contains the course data: ";
                cin >> csvPath;
            }
            // loadCourses<AVLTree> is the templated loader from
            // CourseLoader.h - the same function Benchmark.cpp reuses
            // for BinarySearchTree, so both trees are always loaded
            // through identical, already-fixed parsing logic.
            filesLoaded = loadCourses<AVLTree>(csvPath, avl);
            if (filesLoaded) {
                cout << "Course data loaded.\n" << endl;
            }
            else {
                cout << "No file by that name.\n" << endl;
            }
            // Clear csvPath after the attempt (success or failure) so a
            // second "Load" selection re-prompts for a path instead of
            // silently reusing the last one.
            csvPath = "";
            break;
        case 2:
            if (avl != nullptr && filesLoaded) {
                cout << "Here is a sample schedule:\n" << endl;
                avl->PrintCourseList();
                cout << endl;
            }
            else
                cout << "Load Data Structure first.\n" << endl;
            break;
        case 3:
            // filesLoaded is checked here too, which the original did
            // not do - the original only checked "bst != nullptr",
            // meaning a tree object could exist (from a failed load
            // attempt) while still being empty, and case 3 would let
            // the user "search" it anyway.
            if (avl != nullptr && filesLoaded) {
                if (courseId.length() == 0) {
                    cout << "What course do you want to know about? ";
                    cin >> courseId;
                    // Renamed from the original's "userChoice" loop
                    // variable to "ch"
                    for (auto& ch : courseId) ch = toupper(ch);
                }
                avl->PrintCourse(courseId);
                cout << endl;
            }
            else
                cout << "Load Data Structure first.\n" << endl;
            courseId = "";
            break;
        case 4:
            // Runs entirely on its own synthetic data (see
            // Benchmark.cpp) - doesn't require, and has no effect on,
            // whatever CSV data is currently loaded into avl. Safe to
            // run before, after, or between other menu selections.
            runBenchmark();
            break;
        default:
            if (userChoice != 9)
                cout << choice << " is not a valid option.\n" << endl;
            break;
        }
    }

    // avl may be nullptr if the user exits without ever loading data
    delete avl;
    cout << "\nThank you for using the course planner!" << endl;
    return 0;
}
