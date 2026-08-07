#include <plog/Log.h>
#include <shobjidl_core.h>
#include <winrt/Windows.ApplicationModel.DataTransfer.h>
#include <winrt/Windows.Foundation.h>

#include "share_sheet.h"
#include "utils.h"

using namespace winrt;
using namespace Windows::ApplicationModel::DataTransfer;
using namespace Windows::Foundation;

share_sheet& share_sheet::instance() {
    static share_sheet s_instance;
    return s_instance;
}

void share_sheet::open_share_sheet(HWND hwnd, const std::string& title, const std::string& content) {

    try {
        init_apartment(apartment_type::single_threaded);
    } catch (const hresult_error& ex) {
        WIN_LOG(L"WinRT Exception CAUGHT RESULT:" << ex.code().value << L"MESSAGE: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }

    try {
        const auto interop = get_activation_factory<DataTransferManager, IDataTransferManagerInterop>();

        com_ptr<IInspectable> dtm_i_inspectable;
        check_hresult(interop->GetForWindow(hwnd, guid_of<DataTransferManager>(), put_abi(dtm_i_inspectable)));

        {
            const auto manager = dtm_i_inspectable.as<DataTransferManager>();
            std::lock_guard lock(m_mutex);
            if (m_shareToken.value != 0 && m_manager) {
                m_manager.DataRequested(m_shareToken);
                m_shareToken = {};
            }
            m_manager = manager;
        }

        const auto token =
            m_manager.DataRequested([title, content](DataTransferManager const&, const DataRequestedEventArgs& args) {
                const auto req  = args.Request();
                const auto data = req.Data();

                const auto titleWString   = to_hstring(title);
                const auto contentWString = to_hstring(content);

                data.Properties().Title(titleWString);
                if (!content.empty()) {
                    data.SetText(contentWString);
                    data.Properties().Description(contentWString);
                } else {
                    data.SetText(titleWString);
                }
                WIN_LOG("SHARE SHEET WITH TITLE " << title.c_str() << " CONTENT " << content.c_str());
            });

        {
            std::lock_guard lock(m_mutex);
            m_shareToken = token;
        }
        check_hresult(interop->ShowShareUIForWindow(hwnd));
    } catch (const hresult_error& ex) {
        WIN_LOG(L"WinRT Exception CAUGHT RESULT:" << ex.code().value << L"MESSAGE: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }
}

void share_sheet::open_share_sheet(HWND hwnd, const std::string& text) {

    try {
        init_apartment(apartment_type::single_threaded);
    } catch (const hresult_error& ex) {
        WIN_LOG(L"WinRT Exception CAUGHT RESULT:" << ex.code().value << L"MESSAGE: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }

    try {
        const auto interop = get_activation_factory<DataTransferManager, IDataTransferManagerInterop>();

        com_ptr<IInspectable> dtm_i_inspectable;
        check_hresult(interop->GetForWindow(hwnd, guid_of<DataTransferManager>(), put_abi(dtm_i_inspectable)));

        {
            const auto manager = dtm_i_inspectable.as<DataTransferManager>();
            std::lock_guard lock(m_mutex);
            if (m_shareToken.value != 0 && m_manager) {
                m_manager.DataRequested(m_shareToken);
                m_shareToken = {};
            }
            m_manager = manager;
        }

        const auto token =
            m_manager.DataRequested([text](DataTransferManager const&, const DataRequestedEventArgs& args) {
                const auto req  = args.Request();
                const auto data = req.Data();

                const auto textWString = to_hstring(text);
                data.Properties().Title(L"Shared Content Text");
                data.SetText(textWString);
            });

        {
            std::lock_guard lock(m_mutex);
            m_shareToken = token;
        }
        check_hresult(interop->ShowShareUIForWindow(hwnd));
    } catch (const hresult_error& ex) {
        WIN_LOG(L"WinRT Exception CAUGHT RESULT:" << ex.code().value << L"MESSAGE: " << ex.message().c_str());
    } catch (...) {
        utils::show_stacktrace();
    }
}

void share_sheet::remove_sheet() {
    std::lock_guard lock(m_mutex);
    if (m_shareToken.value != 0 && m_manager) {
        m_manager.DataRequested(m_shareToken);
        m_shareToken = {};
    }
    m_manager = nullptr;
}
