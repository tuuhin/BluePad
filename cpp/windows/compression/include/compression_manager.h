#ifndef WINDOWS_COMPRESSION_H
#define WINDOWS_COMPRESSION_H

#include <cstdint>
#include <vector>

using namespace std;

class compression_manager {
public:
    static vector<uint8_t> compress_bytes_with_ztd(const vector<uint8_t>& incoming);
    static vector<uint8_t> de_compress_bytes_with_ztd(const vector<uint8_t>& incoming);
    static vector<uint8_t> compress_bytes_with_lz4(const vector<uint8_t>& incoming);
    static vector<uint8_t> de_compress_bytes_with_lz4(const vector<uint8_t>& incoming);
    static vector<uint8_t> compress_bytes_with_deflate(vector<uint8_t> incoming);
    static vector<uint8_t> de_compress_bytes_with_deflate(vector<uint8_t> incoming);
    static vector<uint8_t> compress_bytes_with_gzip(vector<uint8_t> incoming);
    static vector<uint8_t> de_compress_bytes_with_gzip(vector<uint8_t> incoming);
};

#endif
