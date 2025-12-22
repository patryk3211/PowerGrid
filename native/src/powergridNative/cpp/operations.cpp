#include "operations.hpp"

#include <cstring>
#include <cmath>

using namespace powergrid;

pg_size_t powergrid::findMaximumCoefficient(pg_number_t *data, pg_size_t stride, pg_size_t count) {
    pg_size_t index = 0;
    pg_number_t max = *data;
    for(pg_size_t i = 1; i < count; ++i) {
        data += stride;
        if(std::abs(*data) > max) {
            max = std::abs(*data);
            index = i;
        }
    }
    return index;
}

void powergrid::swapRows(pg_number_t *row1, pg_number_t *row2, pg_size_t length) {
    for(pg_size_t i = 0; i < length; ++i) {
        pg_number_t buf = row1[i];
        row1[i] = row2[i];
        row2[i] = buf;
    }
}

void powergrid::subtractRow(pg_number_t *dest, pg_number_t *source, pg_number_t alpha, pg_size_t length) {
    for(pg_size_t i = 0; i < length; ++i) {
        dest[i] -= source[i] * alpha;
    }
}

void powergrid::factorizeDense(pg_number_t *m_data, pg_size_t m_size, pg_number_t *result, pg_size_t *pivots) {
    // Initialize pivots
    for(pg_size_t i = 0; i < m_size; ++i) {
        pivots[i] = i;
    }

    memcpy(result, m_data, m_size * m_size * sizeof(pg_number_t));
    for(pg_size_t i = 0; i < m_size; ++i) {
        pg_size_t remainder = m_size - i;

        // Find next pivot
        pg_number_t *corner = result + i + i * m_size;
        pg_size_t max_in_col = findMaximumCoefficient(corner, m_size, remainder) + i;

        if(max_in_col != i) {
            // Swap row into current location (i)
            swapRows(result + i * m_size, result + max_in_col * m_size, m_size);
            pg_size_t p0 = pivots[i];
            pivots[i] = pivots[max_in_col];
            pivots[max_in_col] = p0;
        }

        // Perform Gaussian elimination
        for(pg_size_t j = i + 1; j < m_size; ++j) {
            pg_number_t *dest = result + i + j * m_size;
            pg_number_t alpha = *dest / *corner;
            subtractRow(dest, corner, alpha, remainder);

            // *dest is now zero, we store alpha here.
            *dest = alpha;
        }
    }
}
