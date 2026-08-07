#include "share_sheet_c.h"
#include "share_sheet.h"

extern "C" {
void open_share_sheet_text(HWND hwnd, const char* text) {
    if (!hwnd || !text) return;
    share_sheet::instance().open_share_sheet(hwnd, std::string(text));
}

void open_share_sheet_title_and_desc(HWND hwnd, const char* title, const char* content) {
    if (!hwnd || !title) return;
    share_sheet::instance().open_share_sheet(hwnd, std::string(title), std::string(content));
}
}
