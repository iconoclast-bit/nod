import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:google_sign_in_web/web_only.dart' as web;
import 'home_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> with SingleTickerProviderStateMixin {
  final GoogleSignIn _googleSignIn = GoogleSignIn(
    clientId: '81792018615-u7ca79t0qukt8qeg3gjfmj0ks1eao389.apps.googleusercontent.com',
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
    
    // Listen for authentication events from the web GSI popup
    _googleSignIn.onCurrentUserChanged.listen((GoogleSignInAccount? account) {
      if (account != null) {
        _handleSignInSuccess(account);
      }
    });

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

  Future<void> _handleSignInSuccess(GoogleSignInAccount account) async {
    setState(() => _isLoading = true);
    try {
      // In the new GSI web flow, identity and OAuth are separated.
      // We must explicitly request the Gmail scopes to get an access token.
      final bool isAuthorized = await _googleSignIn.canAccessScopes(_googleSignIn.scopes);
      if (!isAuthorized) {
        final granted = await _googleSignIn.requestScopes(_googleSignIn.scopes);
        if (!granted) {
          throw Exception("Permissions denied. Cannot fetch chores.");
        }
      }

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
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Sign-in failed: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _handleMockBypass() {
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (context) => const HomeScreen(
          userId: 'test_user_1',
          userEmail: 'sriayush1999@gmail.com',
          accessToken: 'mock_access_token_abc',
          displayName: 'Ayush (Sandbox)',
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
                        // Native Google Sign-In Button (replaces deprecated signIn)
                        SizedBox(
                          width: 250,
                          height: 50,
                          child: web.renderButton(),
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
