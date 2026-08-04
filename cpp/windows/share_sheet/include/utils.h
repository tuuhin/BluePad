#ifndef WINDOWS_UTILS_H
#define WINDOWS_UTILS_H

#include <cstdint>
#include <plog/Log.h>
#include <string>

namespace utils {
void init_logger();
uint64_t parse_mac_address(const std::string& mac_str);
void show_stacktrace();
} // namespace utils

#ifndef WIN_LOG
#define WIN_LOG(msg) PLOG_DEBUG << L" [" << L"WIN_UTILITY" << L"] " << msg
#endif
#endif // WINDOWS_UTILS_H
