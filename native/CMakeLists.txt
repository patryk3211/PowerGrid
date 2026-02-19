cmake_minimum_required(VERSION 3.14)

project(PowerGridNative VERSION 1.0.0 LANGUAGES CXX)

option(PG_TRACE "Enable tracing in native backend code" false)
option(PG_MULTITARGET "Enable multi-target OpenBLAS build" false)
option(PG_NATIVE_VERSION "Version suffix" "0")

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()

set(SOURCES
    src/powergridNative/cpp/dense.cpp
    src/powergridNative/cpp/operations.cpp
    src/powergridNative/cpp/sparse.cpp
    src/powergridNative/cpp/solver.cpp
    src/powergridNative/cpp/mna.cpp
    src/powergridNative/cpp/exception_handler.cpp
)

set(CMAKE_C_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_DEBUG "-g")

set(CMAKE_C_FLAGS_RELEASE "-O3 -mavx2")
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -mavx2")

add_library(powergridNative SHARED ${SOURCES})
set_target_properties(powergridNative PROPERTIES OUTPUT_NAME "powergridNative${PG_NATIVE_VERSION}")

target_include_directories(powergridNative PUBLIC src/powergridNative/include ${JVM_INCLUDE})

if(${CMAKE_BUILD_TYPE} STREQUAL "Debug")
  target_compile_definitions(powergridNative PRIVATE DEBUG)
endif()

if(PG_TRACE)
  target_compile_definitions(powergridNative PRIVATE TRACE)
endif()

set_target_properties(powergridNative PROPERTIES CXX_STANDARD 20)

set(BUILD_SHARED_LIBS OFF)
set(BUILD_STATIC_LIBS ON)
set(BUILD_WITHOUT_LAPACK ON)
set(BUILD_TESTING OFF)
set(BUILD_SINGLE true)
set(BUILD_DOUBLE true)
set(BUILD_COMPLEX false)
set(BUILD_COMPLEX16 false)
set(BUILD_MATGEN false)
set(USE_THREAD false)

if(PG_MULTITARGET)
  set(TARGET "NEHALEM")
  set(DYNAMIC_ARCH true)
  set(DYNAMIC_LIST "BULLDOZER;ZEN;SKYLAKEX")
else()
  unset(TARGET)
  set(DYNAMIC_ARCH false)
endif()
set(NOFORTRAN true)

add_subdirectory(OpenBLAS)

export(TARGETS openblas_static FILE "staticblas.cmake")

set(TPL_BLAS_LIBRARIES "openblas")
set(enable_internal_blaslib OFF)
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

target_link_libraries(powergridNative PUBLIC superlu openblas_static)
