#include "include/screen_time/screen_time_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "screen_time_plugin.h"

void ScreenTimePluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  screen_time::ScreenTimePlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
