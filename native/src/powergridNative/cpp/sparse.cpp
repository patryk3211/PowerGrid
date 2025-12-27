#include "sparse.hpp"
#include "slu_util.h"
#include "util.hpp"

using namespace powergrid;

SparseMatrix::SparseMatrix()
    : m_GLU() {
    set_default_options(&m_opts);
    m_A.Store = 0;
    m_L.Store = 0;
    m_U.Store = 0;
    m_refactorize = true;
    StatInit(&m_stats);
}

SparseMatrix::~SparseMatrix() {
    freeMatrices();
    StatFree(&m_stats);
}

void SparseMatrix::resize(int size) {
    if(m_size == size)
        return;
    m_size = size;
    m_permC.resize(size);
    m_permR.resize(size);
    m_etree.resize(size);
    zero();
}

void SparseMatrix::zero() {
    m_opts.Fact = DOFACT;
    m_columns.clear();
    m_rowIndices.clear();
    m_elements.clear();
    freeMatrices();

    m_columns.resize(m_size + 1, 0);
    for(int i = 0; i < m_size; ++i) {
        m_permC[i] = i;
        m_permR[i] = i;
    }
}

double SparseMatrix::get(int row, int column) {
    PG_ASSERT(row < m_size && column < m_size, "Out of bounds matrix access ({}, {})", row, column);

    int start = m_columns[column];
    int end = m_columns[column + 1];
    
    for(int i = start; i < end; ++i) {
        if(m_rowIndices[i] == row) {
            return m_elements[i];
        }
    }
    return 0;
}

double& SparseMatrix::ref(int row, int column) {
    PG_ASSERT(row < m_size && column < m_size, "Out of bounds matrix access ({}, {})", row, column);

    int start = m_columns[column];
    int end = m_columns[column + 1];
    
    for(int i = start; i < end; ++i) {
        if(m_rowIndices[i] == row) {
            return m_elements[i];
        }
    }
    
    m_structureModified = true;
    // Not found in existing allocations
    // Append after the last column entry
    m_rowIndices.insert(m_rowIndices.begin() + end, row);
    auto iter = m_elements.insert(m_elements.begin() + end, 0);
    // Offset all columns after the modified index
    for(int i = column + 1; i < m_columns.size(); ++i) {
        ++m_columns[i];
    }
    return *iter;
}

void SparseMatrix::set(int row, int column, double value) {
    ref(row, column) = value;
    m_refactorize = true;
}

void SparseMatrix::add(int row, int column, double value) {
    ref(row, column) += value;
    m_refactorize = true;
}

void SparseMatrix::sortRows() {
    for(int c = 0; c < m_size; ++c) {
        int start = m_columns[c];
        int end = m_columns[c + 1];

        // Simple selection sort.
        for(int i = start; i < end; ++i) {
            int minIndex = i;
            int minValue = m_rowIndices[i];
            for(int j = i + 1; j < end; ++j) {
                if(m_rowIndices[j] < minValue) {
                    minIndex = j;
                    minValue = m_rowIndices[j];
                }
            }
            if(minIndex != i) {
                // Swap indices
                double bufD = m_elements[i];
                int bufR = m_rowIndices[i];
                m_elements[i] = m_elements[minIndex];
                m_rowIndices[i] = m_rowIndices[minIndex];
                m_elements[minIndex] = bufD;
                m_rowIndices[minIndex] = bufR;
            }
        }
    }
}

void SparseMatrix::formLogicalA() {
    freeMatrices();
    dCreate_CompCol_Matrix(&m_A, m_size, m_size, m_elements.size(), m_elements.data(), m_rowIndices.data(), m_columns.data(), SLU_NC, SLU_D, SLU_GE);
    m_aStore = (NCformat *) m_A.Store;
    m_structureModified = false;
    m_opts.Fact = DOFACT;
}

SuperMatrix *SparseMatrix::superMatrix() {
    if(m_structureModified)
        formLogicalA();
    return &m_A;
}

void SparseMatrix::freeMatrices() {
    m_structureModified = true;
    if(m_A.Store != 0) {
        // A matrix space is managed through std::vectors
        SUPERLU_FREE(m_A.Store);
        m_A.Store = 0;
        m_aStore = 0;
    }
    freeLU();
}

void SparseMatrix::freeLU() {
    m_opts.Fact = DOFACT;
    if(m_L.Store != 0) {
        Destroy_SuperNode_Matrix(&m_L);
        m_L.Store = 0;
    }
    if(m_U.Store != 0) {
        Destroy_CompCol_Matrix(&m_U);
        m_U.Store = 0;
    }
}

void SparseMatrix::factorize() {
    if(m_structureModified)
        formLogicalA();

    /*
     * Get column permutation vector perm_c[], according to permc_spec:
     *   permc_spec = NATURAL:  natural ordering 
     *   permc_spec = MMD_AT_PLUS_A: minimum degree on structure of A'+A
     *   permc_spec = MMD_ATA:  minimum degree on structure of A'*A
     *   permc_spec = COLAMD:   approximate minimum degree column ordering
     *   permc_spec = MY_PERMC: the ordering already supplied in perm_c[]
     */
    int permc_spec = m_opts.ColPerm;
    if(m_opts.Fact == DOFACT)
        get_perm_c(permc_spec, &m_A, m_permC.data());

    SuperMatrix AC;
    sp_preorder(&m_opts, &m_A, m_permC.data(), m_etree.data(), &AC);

    int panel_size = sp_ienv(1);
    int relax = sp_ienv(2);

    /* Compute the LU factorization of A. */
    int info;
    if(m_opts.Fact != SamePattern_SameRowPerm)
        freeLU();
    dgstrf(&m_opts, &AC, relax, panel_size, m_etree.data(), NULL, 0, m_permC.data(), m_permR.data(), &m_L, &m_U, &m_GLU, &m_stats, &info);

    Destroy_CompCol_Permuted(&AC);
    m_refactorize = false;
}

void SparseMatrix::solve(SuperMatrix *B) {
    if(m_refactorize)
        factorize();
    int info;
    dgstrs(NOTRANS, &m_L, &m_U, m_permC.data(), m_permR.data(), B, &m_stats, &info);
}

void SparseMatrix::samePattern(bool value) {
    m_opts.Fact = value ? SamePattern_SameRowPerm : DOFACT;
}

