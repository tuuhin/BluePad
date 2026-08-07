#ifndef WINDOWS_SHARE_SHEET_H
#define WINDOWS_SHARE_SHEET_H

#include <mutex>
#include <shobjidl_core.h>
#include <string>
#include <winrt/Windows.ApplicationModel.DataTransfer.h>

#include "utils.h"

using winrt::Windows::ApplicationModel::DataTransfer::DataTransferManager;
class share_sheet {
public:
    // Singleton access
    static share_sheet& instance();

    // Prevent copy and move operations
    share_sheet(const share_sheet&)            = delete;
    share_sheet& operator=(const share_sheet&) = delete;
    share_sheet(share_sheet&&)                 = delete;
    share_sheet& operator=(share_sheet&&)      = delete;

    void open_share_sheet(HWND hwnd, const std::string& title, const std::string& content);
    void open_share_sheet(HWND hwnd, const std::string& text);
    void remove_sheet();

private:
    share_sheet() { utils::init_logger(); }
    ~share_sheet() { remove_sheet(); }

    winrt::event_token m_shareToken{};
    std::mutex m_mutex;
    DataTransferManager m_manager = nullptr;
};
#endif