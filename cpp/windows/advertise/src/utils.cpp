#include <iomanip>
#include <sstream>
#include <windows.h>
#include <winrt/base.h>

#include <plog/Appenders/ColorConsoleAppender.h>
#include <plog/Formatters/TxtFormatter.h>
#include <plog/Init.h>

#include "utils.h"

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
        tm t;
        util::localtime_s(&t, &record.getTime().time);

        util::nostringstream stream;

        stream << L"[" << std::setfill(L'0') << std::setw(2) << t.tm_hour << L":" << std::setw(2) << t.tm_min << L":"
               << std::setw(2) << t.tm_sec << L"] ";

        stream << L"[" << get_current_thread_name_or_id() << L"] ";
        stream << severityToString(record.getSeverity());
        stream << record.getMessage() << L"\n";
        return stream.str();
    }

private:
    static std::wstring get_current_thread_name_or_id() {
        PWSTR thread_name_raw = nullptr;
        const auto result     = GetThreadDescription(GetCurrentThread(), &thread_name_raw);

        if (SUCCEEDED(result) && thread_name_raw != nullptr && wcslen(thread_name_raw) > 0) {
            const std::wstring name(thread_name_raw);
            LocalFree(thread_name_raw);
            return name;
        }

        if (thread_name_raw) LocalFree(thread_name_raw);
        return L"THREAD_ID: " + std::to_wstring(GetCurrentThreadId());
    }
};
} // namespace plog

void utils::init_logger() {
    static plog::ConsoleAppender<plog::bt_common_formatter> appender((plog::streamStdOut));
    plog::init(plog::debug, &appender);
}

void utils::show_stacktrace() {
#ifndef NDEBUG
    WIN_LOG(L"UNEXPECTED ERROR OCCURRED");
    cpptrace::generate_trace().print();
#else
    WIN_LOG(L"UNKNOWN CRITICAL EXCEPTION OCCURRED");
#endif
}
