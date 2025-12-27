cmake_minimum_required(VERSION 3.14)

project(PowerGridNative VERSION 1.0.0 LANGUAGES CXX)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()

set(SOURCES
    src/powergridNative/cpp/dense.cpp
    src/powergridNative/cpp/operations.cpp
    src/powergridNative/cpp/sparse.cpp
    src/powergridNative/cpp/solver.cpp
    src/powergridNative/cpp/mna.cpp
)
set(CMAKE_CXX_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -mavx2")

add_library(powergridNative SHARED ${SOURCES})

target_include_directories(powergridNative PUBLIC src/powergridNative/include ${JVM_INCLUDE})
target_include_directories(powergridNative PRIVATE superlu/CBLAS)

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
