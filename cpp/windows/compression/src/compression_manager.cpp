#include "compression_manager.h"

#include "deflate.h"
#include <lz4.h>
#include <stdexcept>
#include <zconf.h>
#include <zlib.h>
#include <zstd.h>

using namespace std;

vector<uint8_t> compression_manager::compress_bytes_with_ztd(const vector<uint8_t>& incoming) {

    if (incoming.empty()) return {};

    const auto maxCompressSize = ZSTD_compressBound(incoming.size());
    std::vector<uint8_t> compressed(maxCompressSize);
    constexpr auto level = 3;

    const auto size = ZSTD_compress(compressed.data(), compressed.size(), incoming.data(), incoming.size(), level);
    if (ZSTD_isError(size)) return {};

    compressed.resize(size);
    return compressed;
}

vector<uint8_t> compression_manager::de_compress_bytes_with_ztd(const vector<uint8_t>& incoming) {
    if (incoming.empty()) return {};

    const auto size = ZSTD_getFrameContentSize(incoming.data(), incoming.size());

    if (size == ZSTD_CONTENTSIZE_ERROR || size == ZSTD_CONTENTSIZE_UNKNOWN) return {};

    std::vector<uint8_t> decompressed(size);

    const auto final_size = ZSTD_decompress(decompressed.data(), decompressed.size(), incoming.data(), incoming.size());

    if (ZSTD_isError(final_size)) return {};
    decompressed.resize(final_size);
    return decompressed;
}

vector<uint8_t> compression_manager::compress_bytes_with_lz4(const vector<uint8_t>& incoming) {
    const int srcSize    = static_cast<int>(incoming.size());
    const int maxDstSize = LZ4_compressBound(srcSize);

    std::vector<uint8_t> compressed(sizeof(uint32_t) + maxDstSize);

    const auto originalSize = static_cast<uint32_t>(srcSize);
    std::memcpy(compressed.data(), &originalSize, sizeof(originalSize));

    const int compressedSize =
        LZ4_compress_default(reinterpret_cast<const char*>(incoming.data()),
                             reinterpret_cast<char*>(compressed.data() + sizeof(uint32_t)), srcSize, maxDstSize);

    if (compressedSize <= 0) return {};
    compressed.resize(sizeof(uint32_t) + compressedSize);
    return compressed;
}
vector<uint8_t> compression_manager::de_compress_bytes_with_lz4(const vector<uint8_t>& incoming) {
    if (incoming.size() < sizeof(uint32_t)) return {};

    uint32_t originalSize;
    std::memcpy(&originalSize, incoming.data(), sizeof(originalSize));

    std::vector<uint8_t> decompressed(originalSize);

    const int result = LZ4_decompress_safe(
        reinterpret_cast<const char*>(incoming.data() + sizeof(uint32_t)), reinterpret_cast<char*>(decompressed.data()),
        static_cast<int>(incoming.size() - sizeof(uint32_t)), static_cast<int>(originalSize));

    if (result < 0) return {};

    decompressed.resize(result);
    return decompressed;
}

vector<uint8_t> compression_manager::compress_bytes_with_deflate(vector<uint8_t> incoming) {
    if (incoming.empty()) return {};

    z_stream stream{};
    if (deflateInit2(&stream, Z_DEFAULT_COMPRESSION, Z_DEFLATED, -MAX_WBITS, 8, Z_DEFAULT_STRATEGY) != Z_OK) {
        return {};
    }

    const auto bound = compressBound(static_cast<uLong>(incoming.size()));
    std::vector<uint8_t> output(sizeof(uint32_t) + bound);

    const auto originalSize = static_cast<uint32_t>(incoming.size());
    std::memcpy(output.data(), &originalSize, sizeof(originalSize));

    stream.next_in  = incoming.data();
    stream.avail_in = static_cast<uInt>(incoming.size());

    stream.next_out  = output.data() + sizeof(uint32_t);
    stream.avail_out = static_cast<uInt>(bound);

    const int ret = deflate(&stream, Z_FINISH);
    deflateEnd(&stream);

    if (ret != Z_STREAM_END) return {};

    output.resize(sizeof(uint32_t) + stream.total_out);
    return output;
}

vector<uint8_t> compression_manager::de_compress_bytes_with_deflate(vector<uint8_t> incoming) {
    if (incoming.size() < sizeof(uint32_t)) return {};

    uint32_t originalSize;
    std::memcpy(&originalSize, incoming.data(), sizeof(originalSize));

    std::vector<uint8_t> output(originalSize);

    z_stream stream{};
    if (inflateInit2(&stream, -MAX_WBITS) != Z_OK) return {};

    stream.next_in  = incoming.data() + sizeof(uint32_t);
    stream.avail_in = static_cast<uInt>(incoming.size() - sizeof(uint32_t));

    stream.next_out  = output.data();
    stream.avail_out = originalSize;

    const int ret = inflate(&stream, Z_FINISH);
    inflateEnd(&stream);

    if (ret != Z_STREAM_END) return {};

    output.resize(stream.total_out);
    return output;
}
std::vector<uint8_t> compression_manager::compress_bytes_with_gzip(vector<uint8_t> incoming) {
    if (incoming.empty()) return {};

    z_stream stream{};
    if (deflateInit2(&stream, Z_DEFAULT_COMPRESSION, Z_DEFLATED, MAX_WBITS + 16, 8, Z_DEFAULT_STRATEGY) != Z_OK) {
        return {};
    }

    // deflateBound gives the exact upper bound AFTER stream initialization (includes gzip headers)
    const uLong bound = deflateBound(&stream, static_cast<uLong>(incoming.size()));
    std::vector<uint8_t> output(sizeof(uint32_t) + bound);

    const auto originalSize = static_cast<uint32_t>(incoming.size());
    std::memcpy(output.data(), &originalSize, sizeof(originalSize));

    stream.next_in  = incoming.data();
    stream.avail_in = static_cast<uInt>(incoming.size());

    stream.next_out  = output.data() + sizeof(uint32_t);
    stream.avail_out = static_cast<uInt>(bound);

    const int ret = deflate(&stream, Z_FINISH);
    deflateEnd(&stream);

    if (ret != Z_STREAM_END) return {};

    output.resize(sizeof(uint32_t) + stream.total_out);
    return output;
}
vector<uint8_t> compression_manager::de_compress_bytes_with_gzip(vector<uint8_t> incoming) {
    if (incoming.size() < sizeof(uint32_t)) return {};

    uint32_t originalSize;
    std::memcpy(&originalSize, incoming.data(), sizeof(originalSize));

    std::vector<uint8_t> output(originalSize);
    z_stream stream{};

    if (inflateInit2(&stream, MAX_WBITS + 16) != Z_OK) return {};

    stream.next_in  = incoming.data() + sizeof(uint32_t);
    stream.avail_in = static_cast<uInt>(incoming.size() - sizeof(uint32_t));

    stream.next_out  = output.data();
    stream.avail_out = originalSize;

    const int ret = inflate(&stream, Z_FINISH);
    inflateEnd(&stream);

    if (ret != Z_STREAM_END) return {};

    output.resize(stream.total_out);
    return output;
}