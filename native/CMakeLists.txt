cmake_minimum_required(VERSION 3.14)

project(PowerGridNative VERSION 1.0.0 LANGUAGES CXX)

set(SOURCES
    src/powergridNative/cpp/main.cpp
    src/powergridNative/cpp/dense.cpp
    src/powergridNative/cpp/operations.cpp
    src/powergridNative/cpp/sparse.cpp
)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g")

add_library(powergridNative SHARED ${SOURCES})

target_include_directories(powergridNative PUBLIC src/powergridNative/include ${JVM_INCLUDE})

set_target_properties(powergridNative PROPERTIES CXX_STANDARD 20)

set(enable_internal_blaslib ON)
set(enable_single OFF)
set(enable_double ON)
set(enable_complex OFF)
set(enable_complex16 OFF)
set(enable_matlabmex OFF)
set(enable_doc OFF)
set(enable_examples OFF)
set(enable_fortran OFF)
set(enable_tests OFF)
add_subdirectory(superlu)

target_link_libraries(powergridNative PUBLIC superlu)
