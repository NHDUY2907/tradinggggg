package com.example.demo.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TelegramService {
  private final TradingService tradingService;

  @Value("${telegram.notify.bot.token}")
  private String notifyBotToken;

  @Value("${telegram.notify.chat.id}")
  private String notifyChatId;

  @Value("${telegram.command.bot.token}")
  private String commandBotToken;

  @Value("${telegram.command.chat.id}")
  private String commandChatId;

  @Value("${trade.point.start.x}")
  private int startX;

  @Value("${trade.point.start.y}")
  private int startY;

  private final RestTemplate restTemplate;

  private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();

  private final AtomicLong lastPollingSuccess = new AtomicLong(System.currentTimeMillis());

  public TelegramService(TradingService tradingService) {

    this.tradingService = tradingService;

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

    factory.setConnectTimeout(10_000);

    // Telegram timeout=60
    factory.setReadTimeout(70_000);

    this.restTemplate = new RestTemplate(factory);
  }

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

    pollingThread.setName("telegram-polling");
    pollingThread.setDaemon(true);
    pollingThread.start();

    Thread watchdogThread = new Thread(this::watchdog);

    watchdogThread.setName("telegram-watchdog");
    watchdogThread.setDaemon(true);
    watchdogThread.start();

    log.info("Telegram command bot started...");
  }

  private void watchdog() {

    boolean notified = false;

    while (true) {

      try {

        long diff = System.currentTimeMillis() - lastPollingSuccess.get();

        if (diff > 120_000) {

          if (!notified) {

            sendMessageAdmin("⚠️ Telegram polling bất thường");

            notified = true;
          }

        } else {

          if (notified) {

            sendMessageAdmin("✅ Telegram polling đã phục hồi");
          }

          notified = false;
        }

        Thread.sleep(60_000);

      } catch (Exception e) {

        log.error("Watchdog error", e);
      }
    }
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

        lastPollingSuccess.set(System.currentTimeMillis());

        if (response == null) {
          continue;
        }

        List<Map> results = (List<Map>) response.get("result");

        if (results == null || results.isEmpty()) {
          continue;
        }

        for (Map result : results) {

          long currentUpdateId = ((Number) result.get("update_id")).longValue();

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

          log.info("COMMAND: {}", text);

          commandExecutor.submit(
              () -> {
                try {

                  handleCommand(text.trim().toLowerCase());

                } catch (Exception e) {

                  log.error("Handle command failed", e);

                  sendMessageAdmin("❌ Command failed");
                }
              });

          updateId = currentUpdateId;
        }

      } catch (Throwable e) {

        log.error("Polling error", e);

        try {

          Thread.sleep(3000);

        } catch (InterruptedException ex) {

          Thread.currentThread().interrupt();
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

      case "/trade_on":
        tradingService.start();
        sendMessageAdmin("✅ Trading ON");
        break;

      case "/trade_off":
        tradingService.stop();
        sendMessageAdmin("🛑 Trading OFF");
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
          "http://localhost:9191/job/start?x=" + startX + "&y=" + startY + "&size=9",
          null,
          String.class);
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
