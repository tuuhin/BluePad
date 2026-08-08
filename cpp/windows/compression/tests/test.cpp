#include "compression_c_api.h"
#include <chrono>
#include <cstring>
#include <gtest/gtest.h>
#include <string>
#include <vector>

// Sample test data to compress/decompress
const std::string TEST_STRING = "The quick brown fox jumps over the lazy dog. "
                                "The quick brown fox jumps over the lazy dog. "
                                "The quick brown fox jumps over the lazy dog.";

// Helper function to test idempotency (Compress -> Decompress -> Verify matching output)
typedef enum compression_result (*CompressionFunc)(enum compression_op, const uint8_t*, size_t, uint8_t*, size_t,
                                                   size_t*);

static void TestAlgorithmIdempotency(CompressionFunc func) {
    const auto* input       = reinterpret_cast<const uint8_t*>(TEST_STRING.data());
    const size_t input_size = TEST_STRING.size();

    const size_t compress_capacity = input_size * 2 + 64;
    std::vector<uint8_t> compressed_buf(compress_capacity);
    size_t compressed_size = 0;

    // prepare buffer compress the fields
    enum compression_result comp_res =
        func(compression, input, input_size, compressed_buf.data(), compress_capacity, &compressed_size);

    ASSERT_EQ(comp_res, success) << "Compression failed";
    ASSERT_GT(compressed_size, 0u) << "Compressed size must be greater than zero";

    const size_t decompress_capacity = input_size;
    std::vector<uint8_t> decompressed_buf(decompress_capacity);
    size_t decompressed_size = 0;

    // prepare the buffer de compress the fields
    const enum compression_result decomp_res = func(de_compression, compressed_buf.data(), compressed_size,
                                                    decompressed_buf.data(), decompress_capacity, &decompressed_size);

    ASSERT_EQ(decomp_res, success) << "Decompression failed";
    ASSERT_EQ(decompressed_size, input_size) << "Decompressed size does not match original input size";

    // check if all is good
    const std::string decompressed_string(reinterpret_cast<const char*>(decompressed_buf.data()), decompressed_size);
    EXPECT_EQ(TEST_STRING, decompressed_string) << "Decompressed output does not match original input";
}

TEST(COMPRESSION_TEST, TEST_LZ4_COMPRESSION) {
    constexpr uint8_t input[]   = "Hello LZ4 Compression";
    constexpr size_t input_size = sizeof(input);
    std::vector<uint8_t> output(256);
    size_t output_size = 0;

    enum compression_result res =
        compression_lz4(compression, input, input_size, output.data(), output.size(), &output_size);

    EXPECT_EQ(res, success);
    EXPECT_GT(output_size, 0u);
}

TEST(COMPRESSION_TEST, TEST_ZTD_COMPRESSION) {
    constexpr uint8_t input[]   = "Hello ZTD Compression";
    constexpr size_t input_size = sizeof(input);
    std::vector<uint8_t> output(256);
    size_t output_size = 0;

    const enum compression_result res =
        compression_ztd(compression, input, input_size, output.data(), output.size(), &output_size);

    EXPECT_EQ(res, success);
    EXPECT_GT(output_size, 0u);
}

TEST(COMPRESSION_TEST, TEST_DEFLATE_COMPRESSION) {
    constexpr uint8_t input[]   = "Hello Deflate Compression";
    constexpr size_t input_size = sizeof(input);
    std::vector<uint8_t> output(256);
    size_t output_size = 0;

    const enum compression_result res =
        compression_deflate_algo(compression, input, input_size, output.data(), output.size(), &output_size);

    EXPECT_EQ(res, success);
    EXPECT_GT(output_size, 0u);
}

TEST(COMPRESSION_TEST, TEST_GZIP_COMPRESSION) {
    constexpr uint8_t input[]   = "Hello Gzip Compression";
    constexpr size_t input_size = sizeof(input);
    std::vector<uint8_t> output(256);
    size_t output_size = 0;

    const enum compression_result res =
        compression_gzip(compression, input, input_size, output.data(), output.size(), &output_size);

    EXPECT_EQ(res, success);
    EXPECT_GT(output_size, 0u);
}

TEST(COMPRESSION_IDEMPOTENCY, TEST_LZ4_IDEMPOTENCY) { TestAlgorithmIdempotency(compression_lz4); }

TEST(COMPRESSION_IDEMPOTENCY, TEST_ZTD_IDEMPOTENCY) { TestAlgorithmIdempotency(compression_ztd); }

TEST(COMPRESSION_IDEMPOTENCY, TEST_DEFLATE_IDEMPOTENCY) { TestAlgorithmIdempotency(compression_deflate_algo); }

TEST(COMPRESSION_IDEMPOTENCY, TEST_GZIP_IDEMPOTENCY) { TestAlgorithmIdempotency(compression_gzip); }

TEST(COMPRESSION_ERROR_HANDLING, TEST_SMALL_OUTPUT_BUFFER) {
    constexpr uint8_t input[]   = "This buffer is too long for a tiny output capacity";
    constexpr size_t input_size = sizeof(input);
    uint8_t output[2];
    size_t output_size = 0;

    const enum compression_result res =
        compression_lz4(compression, input, input_size, output, sizeof(output), &output_size);

    EXPECT_EQ(res, output_size_too_low);
}
