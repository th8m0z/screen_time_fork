import 'package:flutter/material.dart';

import '../main_page.dart';

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BLock Demo',
      home: MainPage(),
    );
  }
}
