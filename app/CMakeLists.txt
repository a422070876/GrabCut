# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library(
             native-lib

             SHARED

             src/main/cpp/native-lib.cpp )


find_library(
              log-lib

              log )

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

set(distribution_DIR ../../../../libs)
include_directories(libs/include)

add_library(OpenCV_java3 SHARED IMPORTED )
set_target_properties(  OpenCV_java3
                        PROPERTIES IMPORTED_LOCATION
                        ${distribution_DIR}/${ANDROID_ABI}/libopencv_java3.so
                        )

target_link_libraries( # Specifies the target library.
                       native-lib

                      OpenCV_java3
                       ${log-lib} )