#pragma once

#include <iostream>
#include <format>

#if DEBUG
#define PG_ASSERT(x, args...) if(!(x)) std::cerr << "!ASSERTION FAILED!" << std::format(args) << std::endl
#else
#define PG_ASSERT(x, args...)
#endif

#if TRACE
#define PG_TRACE(args...) std::cout << this << "%" << std::format(args) << std::endl
#else
#define PG_TRACE(args...)
#endif

