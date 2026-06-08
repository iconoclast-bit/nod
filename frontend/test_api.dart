import 'dart:convert';
import 'package:http/http.dart' as http;

void main() async {
  final response = await http.get(Uri.parse('http://localhost:8080/api/users/110686676835052170081/nod-cards'));
  print('Status code: ${response.statusCode}');
  
  if (response.statusCode == 200) {
    final List<dynamic> data = json.decode(response.body);
    print('First item raw keys: ${data.first.keys}');
    print('First item raw: ${data.first}');
    
    // Simulate fromJson
    final jsonItem = data.first;
    final choreType = jsonItem['chore_type'] ?? jsonItem['choreType'] ?? '';
    final summaryText = jsonItem['summary_text'] ?? jsonItem['summaryText'] ?? '';
    
    print('Parsed choreType: "$choreType"');
    print('Parsed summaryText: "$summaryText"');
  }
}
