#include "dense.hpp"
#include "operations.hpp"

#include <iostream>
#include <cstring>

using namespace powergrid;

MatrixDense::MatrixDense(pg_size_t size) {
    m_size = size;
    m_data = new pg_number_t[size * size];
}

MatrixDense::~MatrixDense() {
    delete[] m_data;
}

void MatrixDense::factorize(pg_number_t *result, pg_size_t *pivots) {
    factorizeDense(m_data, m_size, result, pivots);
}
