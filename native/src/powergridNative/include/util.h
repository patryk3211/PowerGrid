#pragma once

#include <stdio.h>

#define ALLOC(type, size) ((type *) malloc((size) * sizeof(type)))

#if DEBUG
#define PG_ASSERT_(x, fmt) if(!(x)) fprintf(stderr, "!ASSERTION FAILED! " fmt "\n")
#define PG_ASSERT(x, fmt, args...) if(!(x)) fprintf(stderr, "!ASSERTION FAILED! " fmt "\n", args)
#else
#define PG_ASSERT_(x, fmt)
#define PG_ASSERT(x, args...)
#endif

#if TRACE
#define STR_(s) #s
#define STR(s) STR_(s)
#define PG_TRACE(args...) fprintf(stderr, __BASE_FILE__ ":" STR(__LINE__) ": " args); fprintf(stderr, "\n")
#else
#define PG_TRACE(args...)
#endif

