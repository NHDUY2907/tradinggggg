package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelegramService {

  private final String BOT_TOKEN = "8445579764:AAG63KH8G4t6WAvLf06fgZxNmcMa3-J-tBM";
  private final String CHAT_ID = "7013142081";

  public void sendMessage(String message) {

    String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body = new HashMap<>();
    body.put("chat_id", CHAT_ID);
    body.put("parse_mode", "HTML");
    body.put("text", message);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    restTemplate.postForObject(url, request, String.class);
  }
}
