#include "bt_bond_manager.h"
#include <gio/gio.h>
#include <glib-2.0/glib/gtypes.h>
#include <glib.h>

#include <iostream>


// Simplified org.bluez.Agent1 interface implementation
static constexpr gchar agent_introspection_xml[] = "<node>"
                                               "  <interface name='org.bluez.Agent1'>"
                                               "    <method name='RequestPinCode'>"
                                               "      <arg type='o' name='device' direction='in' />"
                                               "      <arg type='s' name='pincode' direction='out' />"
                                               "    </method>"
                                               "    <method name='DisplayPinCode'>"
                                               "      <arg type='o' name='device' direction='in' />"
                                               "      <arg type='s' name='pincode' direction='in' />"
                                               "    </method>"
                                               "    <method name='RequestPasskey'>"
                                               "      <arg type='o' name='device' direction='in' />"
                                               "      <arg type='u' name='passkey' direction='out' />"
                                               "    </method>"
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
                                               "    <method name='Cancel'>"
                                               "    </method>"
                                               "  </interface>"
                                               "</node>";

static void handle_method_call(GDBusConnection*, const gchar*, const gchar*, const gchar*, const gchar* method_name,
                               GVariant* parameters, GDBusMethodInvocation* invocation,  gpointer user_data) {

    const auto manager = static_cast<bluetooth_bond_manager*>(user_data);
    if (manager == nullptr) {
        g_dbus_method_invocation_return_dbus_error(invocation, "org.bluez.Error.Failed", "UNABLE TO RECEIVE THE CONTEXT");
        return;
    }

    // --- CASE 1: Display Passkey ---
    if (g_strcmp0(method_name, "DisplayPasskey") == 0) {
        const char* device_path;
        guint32 passkey;
        guint16 entered;
        g_variant_get(parameters, "(ouq)", &device_path, &passkey, &entered);

        manager->handle_display_passkey_event(device_path, passkey);

        g_dbus_method_invocation_return_value(invocation, nullptr);
        return;
    }

    // --- CASE 2: Request Confirmation ---
    if (g_strcmp0(method_name, "RequestConfirmation") == 0) {
        const char* device_path;
        guint32 passkey;
        g_variant_get(parameters, "(ou)", &device_path, &passkey);

        // Let the manager manage the lifecycle of this async confirmation
        if (manager->handle_request_confirmation_event(device_path, passkey, invocation)) {
            return;
        }

        g_dbus_method_invocation_return_dbus_error(invocation, "org.bluez.Error.Rejected", "No UI handler registered");
        return;
    }

    g_dbus_method_invocation_return_dbus_error(invocation, "org.bluez.Error.Rejected", "Not implemented");
}

static constexpr GDBusInterfaceVTable interface_vtable = {
    .method_call = handle_method_call,
};

guint register_custom_agent(GDBusConnection* conn, const char* path, gpointer user_data) {
    GError* error       = nullptr;
    GDBusNodeInfo* info = g_dbus_node_info_new_for_xml(agent_introspection_xml, &error);

    if (info == nullptr) {
        std::cerr << "CANNOT PARSE INTROSPECTION XML FILE " << error->message << std::endl;
        g_error_free(error);
        return 0;
    }

    const guint registration_id = g_dbus_connection_register_object(conn, path, info->interfaces[0], &interface_vtable,
                                                                    user_data, nullptr, &error);

    g_dbus_node_info_unref(info);

    if (registration_id == 0) {
        std::cerr << "FAIL TO REGISTER OBJECT" << error->message << std::endl;
        g_error_free(error);
        return 0;
    }

    // Call RegisterAgent method on org.bluez.AgentManager1
    GVariant* params = g_variant_new("(os)", path, "KeyboardDisplay");
    GVariant* result =
        g_dbus_connection_call_sync(conn, "org.bluez", "/org/bluez", "org.bluez.AgentManager1", "RegisterAgent", params,
                                    nullptr, G_DBUS_CALL_FLAGS_NONE, -1, nullptr, &error);

    if (error) {
        std::cerr << "Failed to register agent: " << error->message << std::endl;
        g_error_free(error);
        g_dbus_connection_unregister_object(conn, registration_id);
        return 0;
    }
    if (result) g_variant_unref(result);

    return registration_id;
}
