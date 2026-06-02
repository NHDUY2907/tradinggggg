package com.example.demo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelegramService {

  @Value("${telegram.notify.bot.token}")
  private String notifyBotToken;

  @Value("${telegram.notify.chat.id}")
  private String notifyChatId;

  @Value("${telegram.command.bot.token}")
  private String commandBotToken;

  @Value("${telegram.command.chat.id}")
  private String commandChatId;

  private final RestTemplate restTemplate = new RestTemplate();

  // update cuối cùng đã xử lý
  private long updateId = 0;

  // =========================================================
  // SEND MESSAGE
  // =========================================================

  public void sendMessage(String message) {

    try {

      String url = "https://api.telegram.org/bot" + notifyBotToken + "/sendMessage";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> body = new HashMap<>();
      body.put("chat_id", notifyChatId);
      body.put("parse_mode", "HTML");
      body.put("text", message);

      HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

      restTemplate.postForObject(url, request, String.class);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // =========================================================
  // SEND MESSAGE ADMIN
  // =========================================================

  public void sendMessageAdmin(String message) {

    try {

      String url = "https://api.telegram.org/bot" + commandBotToken + "/sendMessage";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> body = new HashMap<>();
      body.put("chat_id", commandChatId);
      body.put("parse_mode", "HTML");
      body.put("text", message);

      HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

      restTemplate.postForObject(url, request, String.class);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // =========================================================
  // START TELEGRAM LONG POLLING
  // =========================================================

  @PostConstruct
  public void startTelegramPolling() {

    Thread pollingThread = new Thread(this::longPolling);

    pollingThread.setDaemon(true);

    pollingThread.start();

    System.out.println("Telegram command bot started...");
  }

  // =========================================================
  // LONG POLLING
  // =========================================================

  private void longPolling() {

    while (true) {

      try {

        String url =
            "https://api.telegram.org/bot"
                + commandBotToken
                + "/getUpdates?timeout=60&offset="
                + (updateId + 1);

        Map response = restTemplate.getForObject(url, Map.class);

        if (response == null) {
          continue;
        }

        List<Map> results = (List<Map>) response.get("result");

        if (results == null || results.isEmpty()) {
          continue;
        }

        for (Map result : results) {

          updateId = ((Number) result.get("update_id")).longValue();

          Map message = (Map) result.get("message");

          if (message == null) {
            continue;
          }

          String text = (String) message.get("text");

          if (text == null || text.isBlank()) {
            continue;
          }

          Long currentChatId = ((Number) ((Map) message.get("chat")).get("id")).longValue();

          if (!currentChatId.toString().equals(commandChatId)) {
            continue;
          }

          System.out.println("COMMAND: " + text);

          handleCommand(text.trim().toLowerCase());
        }

      } catch (Exception e) {

        e.printStackTrace();

        try {
          Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }

  // =========================================================
  // HANDLE COMMAND
  // =========================================================

  private void handleCommand(String command) {

    switch (command) {
      case "/start":
        callStart();
        break;

      case "/stop":
        callStop();
        break;

      case "/data":
        getData();
        break;

      case "/wol":
        callWOL();
        sendMessageAdmin("✅ callWOL executed");
        break;

      case "/main":
        callMain();
        sendMessageAdmin("✅ callMain executed");
        break;

      default:
        sendMessageAdmin("❌ Unknown command");
    }
  }

  // =========================================================
  // LOCAL API
  // =========================================================

  private void callStart() {

    try {
      restTemplate.postForObject(
          "http://localhost:9191/job/start?x=2475&y=572&size=9", null, String.class);
    } catch (Exception e) {
      sendMessageAdmin("❌ Call API start failed");
    }
  }

  private void callStop() {

    try {
      restTemplate.postForObject("http://localhost:9191/job/stop", null, String.class);
    } catch (Exception e) {
      sendMessageAdmin("❌ Call API stop failed");
    }
  }

  private void getData() {

    try {
      restTemplate.postForObject("http://localhost:9191/calculator/get-data", null, String.class);
    } catch (Exception e) {
      sendMessageAdmin("❌ Call API get data failed");
    }
  }

  private void callWOL() {

    try {

      int today = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

      restTemplate.postForObject(
          "http://localhost:9191/calculator/win-or-lose?ytd=" + today, null, String.class);

    } catch (Exception e) {
      sendMessageAdmin("❌ Call API WOL failed");
    }
  }

  private void callMain() {

    try {

      int today = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

      restTemplate.postForObject(
          "http://localhost:9191/calculator/main?ytd=" + today, null, String.class);

    } catch (Exception e) {
      sendMessageAdmin("❌ Call API Main failed");
    }
  }
}
