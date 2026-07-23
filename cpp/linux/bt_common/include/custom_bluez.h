#ifndef LINUX_BT_CUSTOM_BLUZ
#define LINUX_BT_CUSTOM_BLUZ

#include <binc/adapter.h>
#include <gio/gio.h>
#include <glib-2.0/glib/gtypes.h>
#include <glib.h>

guint register_custom_agent(GDBusConnection* conn, const char* path);

#endif
