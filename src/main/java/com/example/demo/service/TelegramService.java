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

  @Value("${telegram.bot.token}")
  private String botToken;

  @Value("${telegram.chat.id}")
  private String chatId;

  private final RestTemplate restTemplate = new RestTemplate();

  // update cuối cùng đã xử lý
  private long updateId = 0;

  // =========================================================
  // SEND MESSAGE
  // =========================================================

  public void sendMessage(String message) {

    try {

      String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> body = new HashMap<>();
      body.put("chat_id", chatId);
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

    System.out.println("Telegram long polling started...");
  }

  // =========================================================
  // LONG POLLING
  // =========================================================

  private void longPolling() {

    while (true) {

      try {

        String url =
            "https://api.telegram.org/bot"
                + botToken
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

          // update_id mới nhất
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

          // chỉ cho phép chính bạn dùng bot
          if (!currentChatId.toString().equals(chatId)) {
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

        sendMessage("✅ start executed");

        break;

      case "/stop":
        callStop();

        sendMessage("✅ stop executed");

        break;

      case "/data":
        getData();

        sendMessage("✅ getData executed");

        break;

      case "/wol":
        callWOL();

        sendMessage("✅ callWOL executed");

        break;

      case "/main":
        callMain();

        sendMessage("✅ callMain executed");

        break;

      default:
        sendMessage("❌ Unknown command");
    }
  }

  // =========================================================
  // CALL LOCAL API
  // =========================================================

  private void callStart() {

    try {

      String api = "http://localhost:9191/job/start?x=2475&y=572&size=9";

      restTemplate.postForObject(api, null, String.class);

    } catch (Exception e) {

      sendMessage("❌ Call API start failed");

      e.printStackTrace();
    }
  }

  private void callStop() {

    try {

      String api = "http://localhost:9191/job/stop";

      restTemplate.postForObject(api, null, String.class);

    } catch (Exception e) {

      sendMessage("❌ Call API stop failed");

      e.printStackTrace();
    }
  }

  private void getData() {

    try {

      String api = "http://localhost:9191/calculator/get-data";

      restTemplate.postForObject(api, null, String.class);

    } catch (Exception e) {

      sendMessage("❌ Call API get data failed");

      e.printStackTrace();
    }
  }

  private void callWOL() {

    try {
      int today = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

      String api = "http://localhost:9191/calculator/win-or-lose?ytd=" + today;

      restTemplate.postForObject(api, null, String.class);

    } catch (Exception e) {

      sendMessage("❌ Call API get WOL failed");

      e.printStackTrace();
    }
  }

  private void callMain() {

    try {
      int today = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

      String api = "http://localhost:9191/calculator/main?ytd=" + today;

      restTemplate.postForObject(api, null, String.class);

    } catch (Exception e) {

      sendMessage("❌ Call API get Main failed");

      e.printStackTrace();
    }
  }
}
