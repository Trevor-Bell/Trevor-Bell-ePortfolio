//
// CourseLoader.h
//
// Loads course data from a CSV file into a tree. 
// Pulled out of main.cpp into its own file.
//
// The original program also depended on a third-party CSVparser.hpp
// library, but on closer inspection, that dependency was never actually
// used to do any parsing - a csv::Parser object was constructed and then
// never referenced again; every field was split manually with
// string::find/substr/erase, exactly as it still is here.
//
// Templated on the tree type so the exact same, already-fixed loading
// logic can be reused for both AVLTree and BinarySearchTree - this is
// what lets Benchmark.cpp load identical data into both trees without
// duplicating (and potentially desynchronizing) this logic.
//

#ifndef COURSELOADER_H
#define COURSELOADER_H

#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <exception>
#include "Course.h"

using namespace std;

// Named replacement for the original's unexplained "comma < 100" check.
// A row's comma is expected within the first CSV_FIELD_LENGTH_LIMIT characters.
const unsigned int CSV_FIELD_LENGTH_LIMIT = 100;

// Minimum fields a valid course row must have: course number and course
// name. Anything with fewer fields is skipped rather than crashing
// on an out-of-bounds access.
const unsigned int MIN_REQUIRED_FIELDS = 2;

template <typename TreeType>
bool loadCourses(string csvPath, TreeType* tree) {
    cout << "Loading CSV file " << csvPath << endl;

    try {
        ifstream courseFile(csvPath);

        // Explicitly return false when the file can't be opened.
        if (!courseFile.is_open()) {
            return false;
        }

        while (!courseFile.eof()) {
            // courseInfo accumulates the comma-separated fields of one
            // line: [0] = course number, [1] = course name, [2..] =
            // zero or more prerequisites.
            vector<string> courseInfo;
            string courseData;
            getline(courseFile, courseData);

            // Skip blank lines rather than attempting to parse them into a course.
            if (courseData.length() == 0) {
                continue;
            }

            // Manually split courseData on commas.
            while (courseData.length() > 0) {
                unsigned int comma = courseData.find(',');
                // Named constant instead of a bare "100".
                if (comma < CSV_FIELD_LENGTH_LIMIT) {
                    courseInfo.push_back(courseData.substr(0, comma));
                    courseData.erase(0, comma + 1);
                }
                else {
                    courseInfo.push_back(courseData.substr(0, courseData.length()));
                    courseData = "";
                }
            }

            // Bounds check before indexing courseInfo[0]/[1].
            if (courseInfo.size() < MIN_REQUIRED_FIELDS) {
                cout << "Warning: skipping malformed row (expected at least "
                    << MIN_REQUIRED_FIELDS << " fields, found "
                    << courseInfo.size() << ")" << endl;
                continue;
            }

            // Build the Course from the parsed fields. 
            // Any fields past index 1 are prerequisites.
            Course course;
            course.courseNum = courseInfo[0];
            course.courseName = courseInfo[1];
            for (unsigned int i = 2; i < courseInfo.size(); i++) {
                course.prerequisites.push_back(courseInfo[i]);
            }
            tree->Insert(course);
        }
        courseFile.close();
        return true;
    }
    catch (const exception& e) {
        // Broadened from the original's catch (csv::Error&) to catch
        // (const exception&), since the CSVparser-specific exception
        // type no longer exists (see the removal note above).
        cerr << e.what() << endl;
        return false;
    }
}

#endif // COURSELOADER_H
