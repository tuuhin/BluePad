#include <binc/adapter.h>
#include <cstdint>
#include <gio/gio.h>
#include <glib-object.h>
#include <iomanip>
#include <plog/Appenders/ColorConsoleAppender.h>
#include <plog/Formatters/TxtFormatter.h>
#include <plog/Init.h>

#include "bt_utils.h"

#include <algorithm>
#include <charconv>

#ifndef NDEBUG
#include "cpptrace/basic.hpp"
#include "cpptrace/from_current.hpp"
#endif

namespace plog {
class bt_common_formatter {
public:
    static util::nstring header() { return {}; }
    static util::nstring footer() { return {}; }

    static util::nstring format(const Record& record) {
        tm t{};
        util::localtime_s(&t, &record.getTime().time);

        util::nostringstream stream;

        stream << PLOG_NSTR("[") << std::setfill(PLOG_NSTR('0')) << std::setw(2) << t.tm_hour << PLOG_NSTR(":")
               << std::setw(2) << t.tm_min << PLOG_NSTR(":") << std::setw(2) << t.tm_sec << PLOG_NSTR("] ");

        stream << "[" << get_current_thread_name_or_id() << "] ";
        stream << severityToString(record.getSeverity());
        stream << record.getMessage() << "\n";
        return stream.str();
    }

private:
    static std::string get_current_thread_name_or_id() {
        char buffer[16]   = {0};
        const auto result = pthread_getname_np(pthread_self(), buffer, sizeof(buffer));

        if (result == 0 && buffer[0] != '\0') {
            const std::string narrow_name(buffer);
            return narrow_name;
        }

        const auto tid = syscall(SYS_gettid);
        return "THREAD_ID: " + std::to_string(tid);
    }
};
} // namespace plog

void utils::init_logger() {
    static plog::ConsoleAppender<plog::bt_common_formatter> appender((plog::streamStdOut));
    plog::init(plog::debug, &appender);
}

uint64_t utils::parse_mac_address(const std::string& mac_str) {
    if (mac_str.length() != 17) throw std::invalid_argument("Invalid MAC address length");

    uint64_t result = 0;
    const std::string_view sv(mac_str);

    for (size_t i = 0; i < 6; ++i) {
        std::string_view byte_sv = sv.substr(i * 3, 2);

        uint8_t byte_val = 0;

        if (auto [ptr, ec] = std::from_chars(byte_sv.data(), byte_sv.data() + byte_sv.size(), byte_val, 16);
            ec != std::errc{} || ptr != byte_sv.data() + byte_sv.size()) {
            throw std::invalid_argument("Invalid hex characters in MAC address");
        }

        if (i < 5 && mac_str[i * 3 + 2] != ':') throw std::invalid_argument("Invalid MAC address delimiter format");
        result = (result << 8) | byte_val;
    }
    return result;
}

std::string utils::to_lower_uuid(const char* uuid) {
    if (!uuid) return "";
    std::string str(uuid);
    std::ranges::transform(str, str.begin(), [](const unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return str;
}

bool utils::is_secondary_channel_supported(GDBusConnection* dbus_conn, Adapter* adapter) {
    if (!dbus_conn || !adapter) return false;

    const char* adapter_path = binc_adapter_get_path(adapter);

    GError* error = nullptr;
    GVariant* result =
        g_dbus_connection_call_sync(dbus_conn, "org.bluez", adapter_path, "org.freedesktop.DBus.Properties", "Get",
                                    g_variant_new("(ss)", "org.bluez.LEAdvertisingManager1", "SupportedCapabilities"),
                                    G_VARIANT_TYPE("(v)"), G_DBUS_CALL_FLAGS_NONE, -1, nullptr, &error);

    if (error) {
        g_error_free(error);
        return false;
    }

    if (result == nullptr) return false;

    bool supports_secondary = false;
    GVariant* inner_val = nullptr;
    g_variant_get(result, "(v)", &inner_val);

    if (inner_val == nullptr) {
        g_variant_unref(result);
        return false;
    }

    GVariantIter iter;
    const char* key;
    GVariant* val;
    g_variant_iter_init(&iter, inner_val);
    while (g_variant_iter_loop(&iter, "{sv}", &key, &val)) {
        if (std::string(key) == "SecondaryChannels") {
            supports_secondary = true;
            break;
        }
    }
    g_variant_unref(inner_val);
    g_variant_unref(result);

    return supports_secondary;
}

uint8_t utils::get_max_adv_length(GDBusConnection* dbus_conn, Adapter* adapter) {
    if (!dbus_conn || !adapter) return 31;

    const char* adapter_path = binc_adapter_get_path(adapter);
    GError* error            = nullptr;

    GVariant* result =
        g_dbus_connection_call_sync(dbus_conn, "org.bluez", adapter_path, "org.freedesktop.DBus.Properties", "Get",
                                    g_variant_new("(ss)", "org.bluez.LEAdvertisingManager1", "SupportedCapabilities"),
                                    G_VARIANT_TYPE("(v)"), G_DBUS_CALL_FLAGS_NONE, -1, nullptr, &error);

    if (error) {
        g_error_free(error);
        return 31;
    }

    if (result == nullptr) return 31;

    uint8_t max_len       = 31;
    GVariant* variant_val = nullptr;
    g_variant_get(result, "(v)", &variant_val);

    if (variant_val == nullptr) {
        g_variant_unref(result);
        return 31;
    }

    GVariantIter iter;
    const char* key = nullptr;
    GVariant* value = nullptr;

    g_variant_iter_init(&iter, variant_val);
    while (g_variant_iter_loop(&iter, "{sv}", &key, &value)) {
        if (g_strcmp0(key, "MaxAdvLen") == 0) {
            if (g_variant_is_of_type(value, G_VARIANT_TYPE_BYTE)) {
                max_len = g_variant_get_byte(value);
            }
            break;
        }
    }
    g_variant_unref(variant_val);
    g_variant_unref(result);
    return max_len;
}

void utils::show_stacktrace() {
#ifndef NDEBUG
    LINUX_LOG("UNEXPECTED ERROR OCCURRED");
    cpptrace::generate_trace().print();
#else
    WIN_LOG("UNKNOWN CRITICAL EXCEPTION OCCURRED");
#endif
}
