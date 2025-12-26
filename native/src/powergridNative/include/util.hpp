#pragma once

#include <iostream>
#include <format>

#define PG_ASSERT(x, args...) if(!(x)) std::cerr << std::format(args) << std::endl

