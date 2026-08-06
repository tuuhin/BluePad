#ifndef WINDOWS_COMPRESSION_ENUMS_H
#define WINDOWS_COMPRESSION_ENUMS_H

enum compression_result {
    success             = 0,
    invalid_data        = 1,
    empty_output        = 2,
    output_size_too_low = 3,
};

enum compression_op {
    compression    = 0,
    de_compression = 1,
};

#endif
