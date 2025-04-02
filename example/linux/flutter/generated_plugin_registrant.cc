//
//  Generated file. Do not edit.
//

// clang-format off

#include "generated_plugin_registrant.h"

#include <screen_time/screen_time_plugin.h>

void fl_register_plugins(FlPluginRegistry* registry) {
  g_autoptr(FlPluginRegistrar) screen_time_registrar =
      fl_plugin_registry_get_registrar_for_plugin(registry, "ScreenTimePlugin");
  screen_time_plugin_register_with_registrar(screen_time_registrar);
}
