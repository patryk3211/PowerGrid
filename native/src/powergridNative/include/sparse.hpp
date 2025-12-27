#pragma once

#include <slu_ddefs.h>
#include <vector>

namespace powergrid {
    class SparseMatrix {
        superlu_options_t m_opts;
        SuperLUStat_t m_stats;
        int m_size;

        std::vector<int> m_columns;
        std::vector<int> m_rowIndices;
        std::vector<double> m_elements;

        bool m_structureModified, m_refactorize;
        SuperMatrix m_A;
        NCformat *m_aStore;

        SuperMatrix m_L;
        SuperMatrix m_U;

        GlobalLU_t m_GLU;

        std::vector<int> m_permC;
        std::vector<int> m_permR;
        std::vector<int> m_etree;

      public:
        SparseMatrix();
        ~SparseMatrix();

        void resize(int size);
        void zero();

        double get(int row, int column);
        void set(int row, int column, double value);
        void add(int row, int column, double value);

        void formLogicalA();
        SuperMatrix *superMatrix();
        void factorize();
        void sortRows();

        void samePattern(bool value);

        void solve(SuperMatrix *B);

      private:
        double& ref(int row, int column);

        void freeMatrices();
        void freeLU();
    };
}

