import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'home_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> with SingleTickerProviderStateMixin {
  final GoogleSignIn _googleSignIn = GoogleSignIn(
    scopes: [
      'https://www.googleapis.com/auth/gmail.readonly',
      'https://www.googleapis.com/auth/calendar.readonly',
    ],
  );

  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    );
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeIn),
    );
    _animationController.forward();
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  Future<void> _handleGoogleSignIn() async {
    setState(() => _isLoading = true);
    try {
      final account = await _googleSignIn.signIn();
      if (account != null) {
        final auth = await account.authentication;
        final accessToken = auth.accessToken ?? 'mock_access_token_dev';
        
        if (mounted) {
          Navigator.pushReplacement(
            context,
            MaterialPageRoute(
              builder: (context) => HomeScreen(
                userId: account.id,
                userEmail: account.email,
                accessToken: accessToken,
                displayName: account.displayName ?? 'User',
              ),
            ),
          );
        }
      }
    } catch (error) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Sign-in failed: $error')),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _handleMockBypass() {
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (context) => const HomeScreen(
          userId: 'mock_user_123',
          userEmail: 'sandbox@nod.ai',
          accessToken: 'mock_access_token_abc',
          displayName: 'Sandbox User',
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F172A), // Premium Dark Slate
      body: Stack(
        children: [
          // Background Gradient Orbs
          Positioned(
            top: -100,
            right: -50,
            child: Container(
              width: 300,
              height: 300,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: const Color(0x1F6366F1), // Indigo
                boxShadow: [
                  BoxShadow(
                    color: const Color(0x1F6366F1),
                    blurRadius: 100,
                    spreadRadius: 50,
                  )
                ],
              ),
            ),
          ),
          Positioned(
            bottom: -150,
            left: -100,
            child: Container(
              width: 400,
              height: 400,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: const Color(0x1F3B82F6), // Blue
                boxShadow: [
                  BoxShadow(
                    color: const Color(0x1F3B82F6),
                    blurRadius: 120,
                    spreadRadius: 60,
                  )
                ],
              ),
            ),
          ),
          
          // Foreground Login Content
          SafeArea(
            child: Center(
              child: FadeTransition(
                opacity: _fadeAnimation,
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 24.0),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      // Logo / Icon
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: const Color(0xFF1E293B),
                          border: Border.all(color: const Color(0x336366F1), width: 1.5),
                        ),
                        child: const Icon(
                          Icons.radar_rounded,
                          size: 64,
                          color: Color(0xFF818CF8),
                        ),
                      ),
                      const SizedBox(height: 32),
                      
                      // Heading
                      const Text(
                        'NOD',
                        style: TextStyle(
                          fontSize: 40,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                          letterSpacing: 8.0,
                        ),
                      ),
                      const SizedBox(height: 12),
                      const Text(
                        'Proactive Digital Chief of Staff',
                        style: TextStyle(
                          fontSize: 16,
                          color: Color(0xFF94A3B8),
                        ),
                      ),
                      const SizedBox(height: 64),

                      if (_isLoading)
                        const CircularProgressIndicator(color: Color(0xFF818CF8))
                      else ...[
                        // Google Sign-In Button
                        ElevatedButton.icon(
                          onPressed: _handleGoogleSignIn,
                          icon: const Icon(Icons.login_rounded, color: Colors.white),
                          label: const Text(
                            'Sign In with Google',
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                          ),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF6366F1),
                            minimumSize: const Size(double.infinity, 56),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                            elevation: 0,
                          ),
                        ),
                        const SizedBox(height: 16),
                        
                        // Mock Developer Bypass
                        TextButton(
                          onPressed: _handleMockBypass,
                          child: const Text(
                            'Bypass with Sandbox Account',
                            style: TextStyle(
                              color: Color(0xFF818CF8),
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
