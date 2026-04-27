#pragma once

#include <slu_ddefs.h>

#define SPARSE_MATRIX_STRUCTURE_MODIFIED 1
#define SPARSE_MATRIX_REFACTORIZE 2

#define SPARSE_MATRIX_ELEMENT_BLOCK_SIZE 1024

#if defined(__cplusplus)
extern "C" {
#endif

typedef struct sparsematrix {
    superlu_options_t m_opts;
    SuperLUStat_t m_stats;
    int m_size;
    
    int *m_columns;
    int *m_rowIndices;
    double *m_elements;
    
    int m_elementCount;
    int m_elementsSize;
    
    char m_flags;
    
    SuperMatrix m_A;
    NCformat m_Astore;
    
    SuperMatrix m_AC;
    SuperMatrix m_L;
    SuperMatrix m_U;

    GlobalLU_t m_GLU;
    
    int *m_permC;
    int *m_permR;
    int *m_etree;
} sparsematrix_t;

void sparsematrix_init(sparsematrix_t *matrix);
void sparsematrix_destroy(sparsematrix_t *matrix);

void sparsematrix_free_logical(sparsematrix_t *matrix);

void sparsematrix_resize(sparsematrix_t *matrix, int size);
void sparsematrix_zero(sparsematrix_t *matrix);

double sparsematrix_get(sparsematrix_t *matrix, int row, int column);
double *sparsematrix_ptr(sparsematrix_t *matrix, int row, int column);
void sparsematrix_set(sparsematrix_t *matrix, int row, int column, double value);
void sparsematrix_add(sparsematrix_t *matrix, int row, int column, double value);
SuperMatrix *sparsematrix_supermatrix(sparsematrix_t *matrix);

void sparsematrix_same_pattern(sparsematrix_t *matrix, int value);

void sparsematrix_factorize(sparsematrix_t *matrix);
void sparsematrix_solve(sparsematrix_t *matrix, SuperMatrix *B);

#if defined(__cplusplus)
}

#endif
