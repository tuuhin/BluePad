#ifndef WINDOWS_BLE_UTILS_H
#define WINDOWS_BLE_UTILS_H

#include <iostream>
#include <string>

namespace utils {
std::wstring get_current_thread_name_or_id();
uint64_t parse_mac_address(const std::string& mac_str);
void show_stacktrace();
}

#ifndef WIN_LOG
#define WIN_LOG(msg) std::wclog << L" TAG: [NATIVE-WINDOWS] " << msg << std::endl;
#endif

#endif // WINDOWS_BLE_UTILS_H