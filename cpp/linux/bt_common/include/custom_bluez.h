#ifndef LINUX_BT_CUSTOM_BLUZ
#define LINUX_BT_CUSTOM_BLUZ

#include <gio/gio.h>
#include <glib-2.0/glib/gtypes.h>
#include <glib.h>

guint register_custom_agent(GDBusConnection* conn, const char* path, gpointer user_data);

static void handle_method_call(GDBusConnection*, const gchar*, const gchar*, const gchar*, const gchar* method_name,
                               GVariant* parameters, GDBusMethodInvocation* invocation, gpointer user_data);

#endif
