#pragma once


#if defined(__cplusplus)
extern "C" {
#endif

// These methods are defined by CBLAS but aren't available through the definitions header.
double dasum_(int *n, double *dx, int *incx);
int idamax_(int *n, double *dx, int *incx);
void daxpy_(int *n, double *da, double *dx, int *incx, double *dy, int *incy);
void dscal_(int *n, double *da, double *dx, int *incx);

#if defined(__cplusplus)
}
#endif

