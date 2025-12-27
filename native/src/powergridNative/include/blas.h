#pragma once

#include "f2c.h"

#if defined(__cplusplus)
extern "C" {
#endif

// These methods are defined by CBLAS but aren't available through the definitions header.
int dscal_(integer *n, doublereal *da, doublereal *dx, integer *incx);
doublereal dasum_(integer *n, doublereal *dx, integer *incx);

#if defined(__cplusplus)
}
#endif

