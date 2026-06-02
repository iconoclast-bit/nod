import 'package:flutter/material.dart';
import 'screens/login_screen.dart';

void main() {
  runApp(const NodApp());
}

class NodApp extends StatelessWidget {
  const NodApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Nod',
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.dark,
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFF0F172A),
        fontFamily: 'Inter',
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF6366F1), // Indigo accent
          secondary: Color(0xFF3B82F6), // Blue accent
          surface: Color(0xFF1E293B), // Slate card surface
          background: Color(0xFF0F172A), // Slate background
        ),
      ),
      home: const LoginScreen(),
    );
  }
}
