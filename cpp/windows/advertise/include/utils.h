#ifndef WINDOWS_BLE_UTILS_H
#define WINDOWS_BLE_UTILS_H

#include <plog/Log.h>
#include <string>

namespace utils {
void init_logger(); // New function to set up plog once
void show_stacktrace();
} // namespace utils

#ifndef WIN_LOG
#define WIN_LOG(msg) PLOG_DEBUG << L" [" << L"WIN_BLE_ADVERTISER" << L"] " << msg
#endif

#endif // WINDOWS_BLE_UTILS_H
