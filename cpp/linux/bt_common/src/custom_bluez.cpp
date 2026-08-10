#include <gio/gio.h>
#include <glib-2.0/glib/gtypes.h>
#include <glib.h>
#include <iostream>

#include "bt_bond_manager.h"
#include "custom_bluez.h"
#include "bt_common_utils.h"

static constexpr GDBusInterfaceVTable interface_vtable = {
    .method_call = handle_method_call,
};

static constexpr gchar agent_introspection_xml[] = "<node>"
                                                   "  <interface name='org.bluez.Agent1'>"
                                                   "    <method name='DisplayPasskey'>"
                                                   "      <arg type='o' name='device' direction='in' />"
                                                   "      <arg type='u' name='passkey' direction='in' />"
                                                   "      <arg type='q' name='entered' direction='in' />"
                                                   "    </method>"
                                                   "    <method name='RequestConfirmation'>"
                                                   "      <arg type='o' name='device' direction='in' />"
                                                   "      <arg type='u' name='passkey' direction='in' />"
                                                   "    </method>"
                                                   "    <method name='RequestAuthorization'>"
                                                   "      <arg type='o' name='device' direction='in' />"
                                                   "    </method>"
                                                   "    <method name='AuthorizeService'>"
                                                   "      <arg type='o' name='device' direction='in' />"
                                                   "      <arg type='s' name='uuid' direction='in' />"
                                                   "    </method>"
                                                   "    <method name='Cancel'>"
                                                   "    </method>"
                                                   "  </interface>"
                                                   "</node>";

static void handle_method_call(GDBusConnection*, const gchar*, const gchar*, const gchar*, const gchar* method_name,
                               GVariant* parameters, GDBusMethodInvocation* invocation, gpointer user_data) {

    const auto manager = static_cast<bluetooth_bond_manager*>(user_data);
    if (manager == nullptr) {
        LINUX_LOG("UNABLE TO SET THE BOND MANAGER EXITING");
        g_dbus_method_invocation_return_dbus_error(invocation, "org.bluez.Error.Failed",
                                                   "UNABLE TO RECEIVE THE CONTEXT");
        return;
    }

    if (g_strcmp0(method_name, "DisplayPasskey") == 0) {
        const char* device_path;
        guint32 passkey;
        guint16 entered;
        g_variant_get(parameters, "(&ouq)", &device_path, &passkey, &entered);
        manager->handle_display_passkey_event(device_path, passkey);
        g_dbus_method_invocation_return_value(invocation, nullptr);
        return;
    }

    if (g_strcmp0(method_name, "RequestConfirmation") == 0) {
        const char* device_path;
        guint32 passkey;
        g_variant_get(parameters, "(&ou)", &device_path, &passkey);
        if (manager->handle_request_confirmation_event(device_path, passkey, invocation)) {
            return;
        }
        g_dbus_method_invocation_return_dbus_error(invocation, "org.bluez.Error.Rejected", "No UI handler registered");
        return;
    }

    if (g_strcmp0(method_name, "RequestAuthorization") == 0 || g_strcmp0(method_name, "AuthorizeService") == 0) {
        g_dbus_method_invocation_return_value(invocation, nullptr);
        return;
    }

    if (g_strcmp0(method_name, "Cancel") == 0) {
        manager->handle_cancel_event();
        g_dbus_method_invocation_return_value(invocation, nullptr);
        return;
    }

    g_dbus_method_invocation_return_dbus_error(invocation, "org.bluez.Error.Rejected", "Un-implemented method");
}

guint register_custom_agent(GDBusConnection* conn, const char* path, gpointer user_data) {
    GError* error = nullptr;

    // create a reference for the dbus
    GDBusNodeInfo* info = g_dbus_node_info_new_for_xml(agent_introspection_xml, &error);

    if (info == nullptr) {
        std::cerr << "CANNOT PARSE INTROSPECTION XML FILE " << error->message << std::endl;
        g_error_free(error);
        return 0;
    }

    // create a registration id
    const guint register_id = g_dbus_connection_register_object(conn, path, info->interfaces[0], &interface_vtable,
                                                                user_data, nullptr, &error);

    // remove the reference
    g_dbus_node_info_unref(info);

    if (register_id < 1) {
        std::cerr << "FAIL TO REGISTER OBJECT" << error->message << std::endl;
        g_error_free(error);
        return 0;
    }

    // Include the ability to display yes or no
    GVariant* params = g_variant_new("(os)", path, "DisplayYesNo");
    GVariant* result =
        g_dbus_connection_call_sync(conn, "org.bluez", "/org/bluez", "org.bluez.AgentManager1", "RegisterAgent", params,
                                    nullptr, G_DBUS_CALL_FLAGS_NONE, -1, nullptr, &error);

    if (error) {
        std::cerr << "CANNOT REGISTER AGENT: " << error->message << std::endl;
        g_error_free(error);
        g_dbus_connection_unregister_object(conn, register_id);
        return 0;
    }
    if (result) g_variant_unref(result);

    // return register id
    return register_id;
}
