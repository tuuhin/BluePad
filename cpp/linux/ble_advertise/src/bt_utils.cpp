#include <binc/adapter.h>
#include <cstdint>
#include <gio/gio.h>
#include <glib-object.h>
#include <iomanip>
#include <plog/Appenders/ColorConsoleAppender.h>
#include <plog/Formatters/TxtFormatter.h>
#include <plog/Init.h>

#include "bt_utils.h"

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

void utils::show_stacktrace() {
#ifndef NDEBUG
    LINUX_LOG("UNEXPECTED ERROR OCCURRED");
    cpptrace::generate_trace().print();
#else
    WIN_LOG("UNKNOWN CRITICAL EXCEPTION OCCURRED");
#endif
}
