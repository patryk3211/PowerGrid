#include "sparse.hpp"
#include "util.hpp"
#include <format>

using namespace powergrid;

SparseMatrix::SparseMatrix()
    : m_GLU{}, m_opts{} {
    PG_TRACE("[SparseMatrix::SparseMatrix] entering");
    set_default_options(&m_opts);
    // // m_opts.ColPerm = NATURAL;

    m_A.Store = &m_Astore;
    m_A.Stype = SLU_NC;
    m_A.Dtype = SLU_D;
    m_A.Mtype = SLU_GE;

    m_L.Store = 0;
    m_U.Store = 0;
    m_AC.Store = 0;
    m_structureModified = true;
    m_refactorize = true;
    m_size = 0;

    StatInit(&m_stats);
    PG_TRACE("[SparseMatrix::SparseMatrix] returning");
}

SparseMatrix::~SparseMatrix() {
    PG_TRACE("[SparseMatrix::~SparseMatrix] entering");
    freeMatrices();
    StatFree(&m_stats);
    PG_TRACE("[SparseMatrix::~SparseMatrix] returning");
}

void SparseMatrix::resize(int size) {
    if(m_size == size)
        return;
    PG_TRACE("[SparseMatrix::resize] Reallocating sparse matrix to size {}", size);
    m_size = size;
    m_permC.resize(size);
    m_permR.resize(size);
    m_etree.resize(size);
    m_columns.resize(size + 1);
    m_A.ncol = m_A.nrow = size;
    m_Astore.colptr = m_columns.data();
    zero();
}

void SparseMatrix::zero() {
    PG_TRACE("[SparseMatrix::zero] Zeroing sparse matrix of size {}", m_size);
    m_opts.Fact = DOFACT;
    freeMatrices();
    m_rowIndices.clear();
    m_elements.clear();

    std::fill(m_columns.begin(), m_columns.end(), 0);
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
    PG_TRACE("[SparseMatrix::sortRows] entering");
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
    PG_TRACE("[SparseMatrix::sortRows] returning");
}

void SparseMatrix::formLogicalA() {
    PG_TRACE("[SparseMatrix::formLogicalA] entering");
    freeMatrices();
    m_Astore.nnz = m_elements.size();
    m_Astore.nzval = m_elements.data();
    m_Astore.rowind = m_rowIndices.data();
    m_structureModified = false;
    m_opts.Fact = DOFACT;
    PG_TRACE("[SparseMatrix::formLogicalA] returning");
}

SuperMatrix *SparseMatrix::superMatrix() {
    if(m_structureModified)
        formLogicalA();
    return &m_A;
}

void SparseMatrix::freeMatrices() {
    PG_TRACE("[SparseMatrix::freeMatrices] entering");
    m_structureModified = true;
    freeLU();
    PG_TRACE("[SparseMatrix::freeMatrices] returning");
}

void SparseMatrix::freeLU() {
    PG_TRACE("[SparseMatrix::freeLU] entering");
    m_opts.Fact = DOFACT;
    if(m_AC.Store != 0) {
        Destroy_CompCol_Permuted(&m_AC);
        m_AC.Store = 0;
    }
    if(m_L.Store != 0) {
        Destroy_SuperNode_Matrix(&m_L);
        m_L.Store = 0;
    }
    if(m_U.Store != 0) {
        Destroy_CompCol_Matrix(&m_U);
        m_U.Store = 0;
    }
    PG_TRACE("[SparseMatrix::freeLU] returning");
}

void SparseMatrix::factorize() {
    PG_TRACE("[SparseMatrix::factorize] entering");
    if(m_structureModified)
        formLogicalA();
    if(m_opts.Fact != SamePattern_SameRowPerm)
        freeLU();

    /*
     * Get column permutation vector perm_c[], according to permc_spec:
     *   permc_spec = NATURAL:  natural ordering 
     *   permc_spec = MMD_AT_PLUS_A: minimum degree on structure of A'+A
     *   permc_spec = MMD_ATA:  minimum degree on structure of A'*A
     *   permc_spec = COLAMD:   approximate minimum degree column ordering
     *   permc_spec = MY_PERMC: the ordering already supplied in perm_c[]
     */
    int permc_spec = m_opts.ColPerm;
    if(m_opts.Fact == DOFACT) {
        // When SamePattern_SameRowPerm is used we can reuse the already permuted AC matrix
        get_perm_c(permc_spec, &m_A, m_permC.data());
        sp_preorder(&m_opts, &m_A, m_permC.data(), m_etree.data(), &m_AC);
        PG_TRACE("[SparseMatrix::factorize] preordered");
    }

    int panel_size = sp_ienv(1);
    int relax = sp_ienv(2);

    /* Compute the LU factorization of A. */
    int info;
    dgstrf(&m_opts, &m_AC, relax, panel_size, m_etree.data(), NULL, 0, m_permC.data(), m_permR.data(), &m_L, &m_U, &m_GLU, &m_stats, &info);
    m_refactorize = false;
    PG_TRACE("[SparseMatrix::factorize] returning");
}

void SparseMatrix::solve(SuperMatrix *B) {
    PG_TRACE("[SparseMatrix::solve] entering");
    if(m_refactorize)
        factorize();
    int info;
    dgstrs(NOTRANS, &m_L, &m_U, m_permC.data(), m_permR.data(), B, &m_stats, &info);
    PG_TRACE("[SparseMatrix::solve] returning");
}

void SparseMatrix::samePattern(bool value) {
    m_opts.Fact = value ? SamePattern_SameRowPerm : DOFACT;
}

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

