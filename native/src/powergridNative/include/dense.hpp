#pragma once

#include "types.hpp"

namespace powergrid {
    class MatrixDense {
    public:
        pg_size_t m_size;
        pg_number_t *m_data;

    public:
        MatrixDense(pg_size_t size);
        ~MatrixDense();

        void factorize(pg_number_t *result, pg_size_t *pivots);
    };
}
