#ifndef LINUX_BT_UTILS_H
#define LINUX_BT_UTILS_H

#include <binc/adapter.h>
#include <cstdint>
#include <glib-object.h>
#include <plog/Log.h>
#include <string>

namespace utils {
void init_logger();
uint64_t parse_mac_address(const std::string& mac_str);
void show_stacktrace();
gboolean check_adapter_role_supported(const Adapter* adapter, const char* role_search);
} // namespace utils

#ifndef LINUX_LOG
#define LINUX_LOG(msg) PLOG_DEBUG << " [" << "LINUX_BT_COMMON" << "] " << msg
#endif

#endif
