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
std::string to_lower_uuid(const char* uuid);
bool is_secondary_channel_supported(GDBusConnection* dbus_conn, Adapter* adapter);
uint8_t get_max_adv_length(GDBusConnection* dbus_conn, Adapter* adapter);
} // namespace utils

#ifndef LINUX_LOG
#define LINUX_LOG(msg) PLOG_DEBUG << " [" << "LINUX_BLE_CENTRAL" << "] " << msg
#endif

#endif
