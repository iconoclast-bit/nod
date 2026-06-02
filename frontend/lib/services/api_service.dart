import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiService {
  final String baseUrl;

  ApiService({this.baseUrl = 'http://localhost:8080/api'});

  /// Fetch all pending NodCards for the given user.
  Future<List<NodCard>> fetchPendingCards(String userId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/users/$userId/nod-cards'),
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = json.decode(response.body);
      return data.map((json) => NodCard.fromJson(json)).toList();
    } else if (response.statusCode == 404) {
      // User or cards not found, treat as empty list for safety
      return [];
    } else {
      throw Exception('Failed to load pending nod cards: ${response.body}');
    }
  }

  /// Update status of a specific NodCard (e.g. approve/reject/complete).
  Future<bool> updateCardStatus(String userId, String cardId, String status) async {
    final response = await http.patch(
      Uri.parse('$baseUrl/users/$userId/nod-cards/$cardId'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode({'status': status}),
    );

    return response.statusCode == 200;
  }
}

class NodCard {
  final String id;
  final String userId;
  final String choreType;
  final String summaryText;
  final String status;
  final Map<String, dynamic> actionPayload;

  NodCard({
    required this.id,
    required this.userId,
    required this.choreType,
    required this.summaryText,
    required this.status,
    required this.actionPayload,
  });

  factory NodCard.fromJson(Map<String, dynamic> json) {
    Map<String, dynamic> payload = {};
    if (json['action_payload'] != null) {
      if (json['action_payload'] is String) {
        payload = jsonDecode(json['action_payload']);
      } else {
        payload = Map<String, dynamic>.from(json['action_payload']);
      }
    }
    return NodCard(
      id: json['id'] ?? '',
      userId: json['user_id'] ?? '',
      choreType: json['chore_type'] ?? '',
      summaryText: json['summary_text'] ?? '',
      status: json['status'] ?? 'pending',
      actionPayload: payload,
    );
  }
}
