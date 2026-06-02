package com.example.demo.service;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotTradingService {

  @Value("${telegram.trading.bot.token}")
  private String tradingBotToken;

  @Value("${telegram.trading.chat.id}")
  private String tradingChatId;

  private final RestTemplate restTemplate = new RestTemplate();

  // =========================================================
  // SEND MESSAGE
  // =========================================================

  public void sendMessageTrading(String message) {

    try {

      String url = "https://api.telegram.org/bot" + tradingBotToken + "/sendMessage";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> body = new HashMap<>();
      body.put("chat_id", tradingChatId);
      body.put("parse_mode", "HTML");
      body.put("text", message);

      HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

      restTemplate.postForObject(url, request, String.class);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
