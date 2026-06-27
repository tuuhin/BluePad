#include "utils.h"
#include <iostream>
#include <windows.h>
#include <winrt/base.h>

#ifndef NDEBUG
#include "cpptrace/basic.hpp"
#include "cpptrace/from_current.hpp"
#endif

using namespace std;

wstring utils::get_current_thread_name_or_id() {
    PWSTR thread_name_raw = nullptr;

    const auto result = GetThreadDescription(GetCurrentThread(), &thread_name_raw);

    if (SUCCEEDED(result) && thread_name_raw != nullptr && wcslen(thread_name_raw) > 0) {
        const std::wstring name(thread_name_raw);
        LocalFree(thread_name_raw);
        return std::wstring(L"THREAD_NAME: ") + name;
    }

    // ensure the memory is cleaned
    if (thread_name_raw) LocalFree(thread_name_raw);
    return L"THREAD_ID:" + std::to_wstring(GetCurrentThreadId());
}

uint64_t utils::parse_mac_address(const std::string& mac_str) {
    unsigned int bytes[6];
    const int scanned = sscanf_s(mac_str.c_str(), "%x:%x:%x:%x:%x:%x", &bytes[0], &bytes[1], &bytes[2], &bytes[3],
                                 &bytes[4], &bytes[5]);
    if (scanned != 6) throw std::invalid_argument("Invalid MAC address format");
    uint64_t result = 0;
    for (const unsigned int byte : bytes) {
        result = (result << 8) | (byte & 0xFF);
    }
    return result;
}

void utils::show_stacktrace() {
#ifndef NDEBUG
    WIN_LOG(L"UNEXPECTED ERROR OCCURRED");
    cpptrace::generate_trace().print();
#else
    WIN_LOG(L"UNKNOWN CRITICAL EXCEPTION OCCURRED");
#endif
}