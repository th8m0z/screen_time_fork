// In order to *not* need this ignore, consider extracting the "web" version
// of your plugin as a separate package, instead of inlining it in the same
// package as the core of your plugin.
// ignore: avoid_web_libraries_in_flutter

import 'package:flutter_web_plugins/flutter_web_plugins.dart';
// import 'package:web/web.dart' as web;

import 'screen_time_platform_interface.dart';

/// A web implementation of the ScreenTimePlatform of the ScreenTime plugin.
class ScreenTimeWeb extends ScreenTimePlatform {
  /// Constructs a ScreenTimeWeb
  ScreenTimeWeb();

  static void registerWith(Registrar registrar) {
    ScreenTimePlatform.instance = ScreenTimeWeb();
  }

  // /// Returns a [String] containing the version of the platform.
  // @override
  // Future<String?> getPlatformVersion() async {
  //   final version = web.window.navigator.userAgent;
  //   return version;
  // }
}
