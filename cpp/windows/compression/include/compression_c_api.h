#ifndef COMPRESSION_C_API_H
#define COMPRESSION_C_API_H

#include "compression_enums.h"
#include <stdint.h>

#ifdef _MSC_VER
#ifdef ALLOW_COMPRESSION_MODULE_EXPORTS
#define COMPRESSION_API __declspec(dllexport)
#else
#define COMPRESSION_API __declspec(dllimport)
#endif
#else
#define COMPRESSION_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

// Global utility functions
COMPRESSION_API enum compression_result compression_ztd(enum compression_op mode, const uint8_t* input,
                                                        size_t input_size, uint8_t* output, size_t output_capacity,
                                                        size_t* output_size);
COMPRESSION_API enum compression_result compression_lz4(enum compression_op mode, const uint8_t* input,
                                                        size_t input_size, uint8_t* output, size_t output_capacity,
                                                        size_t* output_size);
COMPRESSION_API enum compression_result compression_deflate_algo(enum compression_op mode, const uint8_t* input,
                                                                 size_t input_size, uint8_t* output,
                                                                 size_t output_capacity, size_t* output_size);
COMPRESSION_API enum compression_result compression_gzip(enum compression_op mode, const uint8_t* input,
                                                         size_t input_size, uint8_t* output, size_t output_capacity,
                                                         size_t* output_size);
#ifdef __cplusplus
}
#endif

#endif
