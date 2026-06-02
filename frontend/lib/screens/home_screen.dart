import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_card_swiper/flutter_card_swiper.dart';
import '../services/api_service.dart';

class HomeScreen extends StatefulWidget {
  final String userId;
  final String userEmail;
  final String accessToken;
  final String displayName;

  const HomeScreen({
    super.key,
    required this.userId,
    required this.userEmail,
    required this.accessToken,
    required this.displayName,
  });

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final ApiService _apiService = ApiService();
  final CardSwiperController _swiperController = CardSwiperController();
  
  List<NodCard> _cards = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadPendingCards();
  }

  @override
  void dispose() {
    _swiperController.dispose();
    super.dispose();
  }

  Future<void> _loadPendingCards() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final fetched = await _apiService.fetchPendingCards(widget.userId);
      setState(() {
        _cards = fetched;
        _isLoading = false;
      });
    } catch (e) {
      // For testing, fallback to mock data if backend not running
      setState(() {
        _cards = [
          NodCard(
            id: 'mock_card_1',
            userId: widget.userId,
            choreType: 'cancel_subscription',
            summaryText: 'Cancel Spotify Premium Subscription',
            status: 'pending',
            actionPayload: {'service': 'Spotify'},
          ),
          NodCard(
            id: 'mock_card_2',
            userId: widget.userId,
            choreType: 'flight_checkin',
            summaryText: 'Check-in to Flight UA284',
            status: 'pending',
            actionPayload: {'airline': 'United'},
          ),
          NodCard(
            id: 'mock_card_3',
            userId: widget.userId,
            choreType: 'return_item',
            summaryText: 'Return Amazon Product by June 10',
            status: 'pending',
            actionPayload: {'merchant': 'Amazon'},
          ),
        ];
        _isLoading = false;
      });
    }
  }

  Future<bool> _onSwipe(int previousIndex, int? currentIndex, CardSwiperDirection direction) async {
    final swipedCard = _cards[previousIndex];
    final String status = direction == CardSwiperDirection.right ? 'approved' : 'rejected';
    
    // Immediate micro haptic feedback
    HapticFeedback.lightImpact();
    print('Swiped ${swipedCard.id} to status: $status');

    try {
      await _apiService.updateCardStatus(widget.userId, swipedCard.id, status);
    } catch (e) {
      print('Failed to notify backend: $e');
    }

    // If final card cleared, trigger heavy haptic reward
    if (currentIndex == null || currentIndex >= _cards.length) {
      Future.delayed(const Duration(milliseconds: 300), () {
        HapticFeedback.vibrate();
      });
    }

    return true;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F172A), // Premium Dark Slate
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Text(
          'NOD CHIEF',
          style: TextStyle(
            color: Colors.white.withOpacity(0.9),
            fontWeight: FontWeight.bold,
            letterSpacing: 2.0,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Color(0xFF818CF8)),
            onPressed: _loadPendingCards,
          ),
        ],
      ),
      body: SafeArea(
        child: _isLoading
            ? const Center(child: CircularProgressIndicator(color: Color(0xFF818CF8)))
            : _cards.isEmpty
                ? _buildEmptyState()
                : _buildCardSwiper(),
      ),
    );
  }

  Widget _buildCardSwiper() {
    return Column(
      children: [
        // Greeting & Count
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Hello, ${widget.displayName}',
                    style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 14),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    'Action Queue',
                    style: TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                decoration: BoxDecoration(
                  color: const Color(0x336366F1),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: const Color(0x666366F1)),
                ),
                child: Text(
                  '${_cards.length} Pending',
                  style: const TextStyle(color: Color(0xFFC7D2FE), fontWeight: FontWeight.bold),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),
        
        // Swiper Container
        Expanded(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: CardSwiper(
              controller: _swiperController,
              cardsCount: _cards.length,
              onSwipe: _onSwipe,
              numberOfCardsDisplayed: _cards.length > 2 ? 3 : _cards.length,
              backCardOffset: const Offset(0, -30),
              padding: const EdgeInsets.all(12.0),
              cardBuilder: (context, index, percentX, percentY) {
                final card = _cards[index];
                return _buildSwipeCard(card);
              },
            ),
          ),
        ),
        const SizedBox(height: 32),
        
        // Control Hint Buttons
        Padding(
          padding: const EdgeInsets.only(bottom: 40.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Reject Button (Left)
              IconButton(
                onPressed: () => _swiperController.swipe(CardSwiperDirection.left),
                icon: const Icon(Icons.close_rounded, size: 36, color: Color(0xFFF43F5E)),
                style: IconButton.styleFrom(
                  backgroundColor: const Color(0xFF1E293B),
                  padding: const EdgeInsets.all(16),
                  side: const BorderSide(color: Color(0x33F43F5E)),
                ),
              ),
              const SizedBox(width: 48),
              // Approve Button (Right)
              IconButton(
                onPressed: () => _swiperController.swipe(CardSwiperDirection.right),
                icon: const Icon(Icons.check_rounded, size: 36, color: Color(0xFF10B981)),
                style: IconButton.styleFrom(
                  backgroundColor: const Color(0xFF1E293B),
                  padding: const EdgeInsets.all(16),
                  side: const BorderSide(color: Color(0x3310B981)),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSwipeCard(NodCard card) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B), // Card background
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: const Color(0x1AFFFFFF), width: 1.5),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 15,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      padding: const EdgeInsets.all(28.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header / Icon representation
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0x1F6366F1),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(
                  _getChoreIcon(card.choreType),
                  color: const Color(0xFF818CF8),
                  size: 28,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  _formatChoreTitle(card.choreType),
                  style: const TextStyle(
                    color: Color(0xFF94A3B8),
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                    letterSpacing: 1.0,
                  ),
                ),
              ),
            ],
          ),
          const Spacer(),
          
          // Summary Description text
          Text(
            card.summaryText,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 26,
              fontWeight: FontWeight.bold,
              height: 1.3,
            ),
          ),
          const SizedBox(height: 12),
          
          // Action target url meta
          if (card.actionPayload['url'] != null)
            Text(
              'Target: ${card.actionPayload['url']}',
              style: const TextStyle(
                color: Color(0xFF64748B),
                fontSize: 13,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          const Spacer(),
          
          // Bottom Status Label indicator
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Swipe Right to Execute',
                style: TextStyle(color: Color(0xFF475569), fontSize: 13),
              ),
              Icon(
                Icons.arrow_forward_rounded,
                color: Colors.white.withOpacity(0.2),
                size: 20,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: Color(0x1F10B981), // Emerald green
              ),
              child: const Icon(
                Icons.done_all_rounded,
                size: 64,
                color: Color(0xFF34D399),
              ),
            ),
            const SizedBox(height: 28),
            const Text(
              'Zero Nods Pending',
              style: TextStyle(
                color: Colors.white,
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 10),
            const Text(
              'Your proactive chief of staff has completed all outstanding automation tasks.',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Color(0xFF64748B),
                fontSize: 15,
                height: 1.4,
              ),
            ),
            const SizedBox(height: 36),
            ElevatedButton(
              onPressed: _loadPendingCards,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF1E293B),
                side: const BorderSide(color: Color(0x33FFFFFF)),
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: const Text('Check for Chores', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }

  IconData _getChoreIcon(String type) {
    switch (type.toLowerCase()) {
      case 'cancel_subscription':
        return Icons.unsubscribe_rounded;
      case 'flight_checkin':
        return Icons.flight_takeoff_rounded;
      case 'return_item':
        return Icons.keyboard_return_rounded;
      default:
        return Icons.assignment_rounded;
    }
  }

  String _formatChoreTitle(String type) {
    return type.toUpperCase().replaceAll('_', ' ');
  }
}
