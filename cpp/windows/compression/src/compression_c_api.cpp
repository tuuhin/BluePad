#include <vector>

#include "compression_c_api.h"
#include "compression_manager.h"

extern "C" {
compression_result compression_ztd(const compression_op mode, const uint8_t* input, const size_t input_size,
                                   uint8_t* output, const size_t output_capacity, size_t* output_size) {
    if (!input || !output || !output_size) return invalid_data;

    const std::vector inputVec(input, input + input_size);

    std::vector<uint8_t> result;
    if (mode == compression)
        result = compression_manager::compress_bytes_with_ztd(inputVec);
    else
        result = compression_manager::de_compress_bytes_with_ztd(inputVec);

    if (result.empty()) return empty_output;

    if (result.size() > output_capacity) {
        *output_size = result.size();
        return output_size_too_low;
    }

    std::memcpy(output, result.data(), result.size());
    *output_size = result.size();

    return success;
}

compression_result compression_lz4(const compression_op mode, const uint8_t* input, const size_t input_size,
                                   uint8_t* output, const size_t output_capacity, size_t* output_size) {
    if (!input || !output || !output_size) return invalid_data;

    const std::vector inputVec(input, input + input_size);

    std::vector<uint8_t> result;
    if (mode == compression)
        result = compression_manager::compress_bytes_with_lz4(inputVec);
    else
        result = compression_manager::de_compress_bytes_with_lz4(inputVec);

    if (result.empty()) return empty_output;

    if (result.size() > output_capacity) {
        *output_size = result.size();
        return output_size_too_low;
    }

    std::memcpy(output, result.data(), result.size());
    *output_size = result.size();

    return success;
}

compression_result compression_deflate_algo(const compression_op mode, const uint8_t* input, const size_t input_size,
                                            uint8_t* output, const size_t output_capacity, size_t* output_size) {
    if (!input || !output || !output_size) return invalid_data;

    const std::vector inputVec(input, input + input_size);

    std::vector<uint8_t> result;
    if (mode == compression)
        result = compression_manager::compress_bytes_with_deflate(inputVec);
    else
        result = compression_manager::de_compress_bytes_with_deflate(inputVec);

    if (result.empty()) return empty_output;

    if (result.size() > output_capacity) {
        *output_size = result.size();
        return output_size_too_low;
    }

    std::memcpy(output, result.data(), result.size());
    *output_size = result.size();

    return success;
}

compression_result compression_gzip(const compression_op mode, const uint8_t* input, const size_t input_size,
                                    uint8_t* output, const size_t output_capacity, size_t* output_size) {
    if (!input || !output || !output_size) return invalid_data;

    const std::vector inputVec(input, input + input_size);

    std::vector<uint8_t> result;
    if (mode == compression)
        result = compression_manager::compress_bytes_with_gzip(inputVec);
    else
        result = compression_manager::de_compress_bytes_with_gzip(inputVec);

    if (result.empty()) return empty_output;

    if (result.size() > output_capacity) {
        *output_size = result.size();
        return output_size_too_low;
    }

    std::memcpy(output, result.data(), result.size());
    *output_size = result.size();

    return success;
}
}
