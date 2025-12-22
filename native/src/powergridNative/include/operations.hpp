#pragma once

#include "types.hpp"

namespace powergrid {
    pg_size_t findMaximumCoefficient(pg_number_t *data, pg_size_t stride, pg_size_t count);
    void swapRows(pg_number_t *row1, pg_number_t *row2, pg_size_t length);
    void subtractRow(pg_number_t *dest, pg_number_t *source, pg_number_t alpha, pg_size_t length);

    void factorizeDense(pg_number_t *matrix, pg_size_t rowsCols, pg_number_t *lu, pg_size_t *pvt);
}

