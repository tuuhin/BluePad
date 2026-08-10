#ifndef WIN_SHARE_SHEET_C_H
#define WIN_SHARE_SHEET_C_H

#include <windows.h>

#ifdef SHARE_SHEET_EXPORTS
#define SHARE_SHEET_API __declspec(dllexport)
#else
#define SHARE_SHEET_API __declspec(dllimport)
#endif

#ifdef __cplusplus
extern "C" {
#endif

SHARE_SHEET_API void open_share_sheet_title_and_desc(HWND hw, const char* title, const char* content);
SHARE_SHEET_API void open_share_sheet_text(HWND hw, const char* text);

#ifdef __cplusplus
}
#endif

#endif // WIN_SHARE_SHEET_C_H
