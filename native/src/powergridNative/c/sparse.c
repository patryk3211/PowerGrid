#include "sparse.h"
#include "util.h"

#include <stdlib.h>

void sparsematrix_init(sparsematrix_t *matrix) {
    PG_TRACE("[SparseMatrix::SparseMatrix] entering");
    memset(matrix, 0, sizeof(sparsematrix_t));
    set_default_options(&matrix->m_opts);
    // m_opts.ColPerm = NATURAL;

    matrix->m_A.Store = &matrix->m_Astore;
    matrix->m_A.Stype = SLU_NC;
    matrix->m_A.Dtype = SLU_D;
    matrix->m_A.Mtype = SLU_GE;

    matrix->m_flags = SPARSE_MATRIX_STRUCTURE_MODIFIED | SPARSE_MATRIX_REFACTORIZE;

    StatInit(&matrix->m_stats);
    PG_TRACE("[SparseMatrix::SparseMatrix] returning");
}

void sparsematrix_free_arrays(sparsematrix_t *matrix) {
    free(matrix->m_columns);
    free(matrix->m_rowIndices);
    free(matrix->m_elements);
    free(matrix->m_permC);
    free(matrix->m_permR);
    free(matrix->m_etree);
    matrix->m_columns = 0;
    matrix->m_rowIndices = 0;
    matrix->m_elements = 0;
    matrix->m_permC = 0;
    matrix->m_permR = 0;
    matrix->m_etree = 0;
}

void sparsematrix_destroy(sparsematrix_t *matrix) {
    sparsematrix_free_logical(matrix);
    sparsematrix_free_arrays(matrix);
    StatFree(&matrix->m_stats);
}

void sparsematrix_free_logical(sparsematrix_t *matrix) {
    PG_TRACE("[SparseMatrix::freeMatrices] entering");
    matrix->m_opts.Fact = DOFACT;
    if(matrix->m_AC.Store != 0) {
        Destroy_CompCol_Permuted(&matrix->m_AC);
        matrix->m_AC.Store = 0;
    }
    if(matrix->m_L.Store != 0) {
        Destroy_SuperNode_Matrix(&matrix->m_L);
        matrix->m_L.Store = 0;
    }
    if(matrix->m_U.Store != 0) {
        Destroy_CompCol_Matrix(&matrix->m_U);
        matrix->m_U.Store = 0;
    }
    PG_TRACE("[SparseMatrix::freeMatrices] returning");
}

void sparsematrix_resize(sparsematrix_t *matrix, int size) {
    if(matrix->m_size == size)
        return;
    PG_TRACE("[SparseMatrix::resize] Reallocating sparse matrix to size %d", size);
    matrix->m_size = size;
    sparsematrix_free_logical(matrix);
    sparsematrix_free_arrays(matrix);
    matrix->m_flags |= SPARSE_MATRIX_STRUCTURE_MODIFIED;
    if(size == 0)
        return;
    matrix->m_permC = ALLOC(int, size);
    matrix->m_permR = ALLOC(int, size);
    matrix->m_etree = ALLOC(int, size);
    matrix->m_columns = ALLOC(int, size + 1);
    memset(matrix->m_permC, 0, sizeof(int) * size);
    memset(matrix->m_permR, 0, sizeof(int) * size);
    memset(matrix->m_etree, 0, sizeof(int) * size);
    memset(matrix->m_columns, 0, sizeof(int) * (size + 1));
    matrix->m_A.ncol = matrix->m_A.nrow = size;
    matrix->m_Astore.colptr = matrix->m_columns;
    sparsematrix_zero(matrix);
}

void sparsematrix_zero(sparsematrix_t *matrix) {
    PG_TRACE("[SparseMatrix::zero] Zeroing sparse matrix of size %d", matrix->m_size);
    matrix->m_opts.Fact = DOFACT;
    matrix->m_flags |= SPARSE_MATRIX_REFACTORIZE | SPARSE_MATRIX_STRUCTURE_MODIFIED;
    sparsematrix_free_logical(matrix);
    free(matrix->m_rowIndices);
    free(matrix->m_elements);
    matrix->m_rowIndices = 0;
    matrix->m_elements = 0;
    matrix->m_elementCount = 0;
    matrix->m_elementsSize = 0;
    memset(matrix->m_columns, 0, sizeof(int) * (matrix->m_size + 1));
    for(int i = 0; i < matrix->m_size; ++i) {
        matrix->m_permC[i] = i;
        matrix->m_permR[i] = i;
    }
}

double sparsematrix_get(sparsematrix_t *matrix, int row, int column) {
    PG_ASSERT(row < matrix->m_size && column < matrix->m_size, "Out of bounds matrix access (%d, %d)", row, column);
    if(matrix->m_rowIndices == 0)
        return 0;
    
    int start = matrix->m_columns[column];
    int end = matrix->m_columns[column + 1];
    
    // TODO: Since row indices should always be sorted we could use binary search here.
    for(int i = start; i < end; ++i) {
        if(matrix->m_rowIndices[i] == row) {
            return matrix->m_elements[i];
        }
        if(matrix->m_rowIndices[i] > row)
            break;
    }
    return 0;
}

double *sparsematrix_ptr(sparsematrix_t *matrix, int row, int column) {
    PG_ASSERT(row < matrix->m_size && column < matrix->m_size, "Out of bounds matrix access (%d, %d)", row, column);

    int insertPos = 0;
    if(matrix->m_rowIndices != 0) {
        int start = matrix->m_columns[column];
        int end = matrix->m_columns[column + 1];
        
        insertPos = start;
        for(int i = start; i < end; ++i) {
            int index = matrix->m_rowIndices[i];
            if(index == row) {
                return matrix->m_elements + i;
            }
            // If the first row index is larger,
            //  loop will break with insertPos = start
            // If all row indices are smaller,
            //  loop will end with insertPos = i + 1 (end)
            // If start = end, insertPos = start
            if(index > row) {
                insertPos = i;
                break;
            }
            insertPos = i + 1;
        }
    }
    
    // Not found in existing allocations
    matrix->m_flags |= SPARSE_MATRIX_STRUCTURE_MODIFIED;
    if(matrix->m_elementCount >= matrix->m_elementsSize) {
        // Needs to be reallocated to allow for another element.
        // Expand allocation by an additional block.
        matrix->m_elementsSize += SPARSE_MATRIX_ELEMENT_BLOCK_SIZE;
        matrix->m_rowIndices = realloc(matrix->m_rowIndices, sizeof(int) * matrix->m_elementsSize);
        matrix->m_elements = realloc(matrix->m_elements, sizeof(double) * matrix->m_elementsSize);
    }
    // Append in position which preserves ordering.
    for(int i = matrix->m_elementCount; i > insertPos; --i) {
        matrix->m_rowIndices[i] = matrix->m_rowIndices[i - 1];
        matrix->m_elements[i] = matrix->m_elements[i - 1];
    }
    matrix->m_rowIndices[insertPos] = row;
    matrix->m_elements[insertPos] = 0;
    // Offset all columns after the modified index
    for(int i = column + 1; i < matrix->m_size + 1; ++i) {
        ++matrix->m_columns[i];
    }
    ++matrix->m_elementCount;
    return matrix->m_elements + insertPos;
}

void sparsematrix_set(sparsematrix_t *matrix, int row, int column, double value) {
    *sparsematrix_ptr(matrix, row, column) = value;
    matrix->m_flags |= SPARSE_MATRIX_REFACTORIZE;
}

void sparsematrix_add(sparsematrix_t *matrix, int row, int column, double value) {
    *sparsematrix_ptr(matrix, row, column) += value;
    matrix->m_flags |= SPARSE_MATRIX_REFACTORIZE;
}

void sparsematrix_form_logical_a(sparsematrix_t *matrix) {
    PG_TRACE("[SparseMatrix::formLogicalA] entering");
    sparsematrix_free_logical(matrix);
    matrix->m_Astore.nnz = matrix->m_elementCount;
    matrix->m_Astore.nzval = matrix->m_elements;
    matrix->m_Astore.rowind = matrix->m_rowIndices;
    matrix->m_flags &= ~SPARSE_MATRIX_STRUCTURE_MODIFIED;
    matrix->m_flags |= SPARSE_MATRIX_REFACTORIZE;
    matrix->m_opts.Fact = DOFACT;
    PG_TRACE("[SparseMatrix::formLogicalA] returning");
}

SuperMatrix *sparsematrix_supermatrix(sparsematrix_t *matrix) {
    if(matrix->m_flags & SPARSE_MATRIX_STRUCTURE_MODIFIED)
        sparsematrix_form_logical_a(matrix);
    return &matrix->m_A;
}

void sparsematrix_same_pattern(sparsematrix_t *matrix, int value) {
    matrix->m_opts.Fact = value ? SamePattern_SameRowPerm : DOFACT;
}

void sparsematrix_factorize(sparsematrix_t *matrix) {
    PG_TRACE("[SparseMatrix::factorize] entering");
    if(matrix->m_flags & SPARSE_MATRIX_STRUCTURE_MODIFIED)
        sparsematrix_form_logical_a(matrix);
    if(matrix->m_opts.Fact != SamePattern_SameRowPerm)
        sparsematrix_free_logical(matrix);

    /*
     * Get column permutation vector perm_c[], according to permc_spec:
     *   permc_spec = NATURAL:  natural ordering 
     *   permc_spec = MMD_AT_PLUS_A: minimum degree on structure of A'+A
     *   permc_spec = MMD_ATA:  minimum degree on structure of A'*A
     *   permc_spec = COLAMD:   approximate minimum degree column ordering
     *   permc_spec = MY_PERMC: the ordering already supplied in perm_c[]
     */
    int permc_spec = matrix->m_opts.ColPerm;
    if(matrix->m_opts.Fact == DOFACT) {
        // When SamePattern_SameRowPerm is used we can reuse the already permuted AC matrix
        get_perm_c(permc_spec, &matrix->m_A, matrix->m_permC);
        sp_preorder(&matrix->m_opts, &matrix->m_A, matrix->m_permC, matrix->m_etree, &matrix->m_AC);
        PG_TRACE("[SparseMatrix::factorize] preordered");
    }

    int panel_size = sp_ienv(1);
    int relax = sp_ienv(2);

    /* Compute the LU factorization of A. */
    int info;
    dgstrf(&matrix->m_opts, &matrix->m_AC, relax, panel_size, matrix->m_etree, NULL, 0, matrix->m_permC, matrix->m_permR, &matrix->m_L, &matrix->m_U, &matrix->m_GLU, &matrix->m_stats, &info);
    matrix->m_flags &= ~SPARSE_MATRIX_REFACTORIZE;
    PG_TRACE("[SparseMatrix::factorize] returning");
}

void sparsematrix_solve(sparsematrix_t *matrix, SuperMatrix *B) {
    PG_TRACE("[SparseMatrix::solve] entering");
    if(matrix->m_flags & SPARSE_MATRIX_REFACTORIZE)
        sparsematrix_factorize(matrix);
    int info;
    dgstrs(NOTRANS, &matrix->m_L, &matrix->m_U, matrix->m_permC, matrix->m_permR, B, &matrix->m_stats, &info);
    PG_TRACE("[SparseMatrix::solve] returning");
}

/*
std::string SparseMatrix::printMatrix() {
    std::string output = "";
    for(int r = 0; r < m_size; ++r) {
        std::string line = "";
        for(int c = 0; c < m_size; ++c) {
            line += std::format("{:+010.3e} ", get(r, c));
        }
        output += line;
        output += "\n";
    }
    std::cerr << output;
    return output;
}
*/
