#ifndef FLUTTER_PLUGIN_SCREEN_TIME_PLUGIN_H_
#define FLUTTER_PLUGIN_SCREEN_TIME_PLUGIN_H_

#include <flutter/method_channel.h>
#include <flutter/plugin_registrar_windows.h>

#include <memory>

namespace screen_time {

class ScreenTimePlugin : public flutter::Plugin {
 public:
  static void RegisterWithRegistrar(flutter::PluginRegistrarWindows *registrar);

  ScreenTimePlugin();

  virtual ~ScreenTimePlugin();

  // Disallow copy and assign.
  ScreenTimePlugin(const ScreenTimePlugin&) = delete;
  ScreenTimePlugin& operator=(const ScreenTimePlugin&) = delete;

  // Called when a method is called on this plugin's channel from Dart.
  void HandleMethodCall(
      const flutter::MethodCall<flutter::EncodableValue> &method_call,
      std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result);
};

}  // namespace screen_time

#endif  // FLUTTER_PLUGIN_SCREEN_TIME_PLUGIN_H_
