package com.example.demo.service;

import com.example.demo.data.entity.StatisticalEntity;
import com.example.demo.data.repository.StatisticalRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.awt.*;
import java.util.Arrays;
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
  private final MouseService mouseService;
  private final StatisticalRepository statisticalRepository;

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

  // RestTemplate dùng cho gửi tin nhắn + gọi API localhost: timeout NGẮN.
  // Tránh việc Telegram/localhost chậm làm treo vòng capture (chạy đồng bộ trên thread scheduler).
  private final RestTemplate restTemplate;

  // RestTemplate RIÊNG cho long polling getUpdates: read timeout phải > timeout=60 của Telegram.
  private final RestTemplate pollingRestTemplate;

  private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();

  private final AtomicLong lastPollingSuccess = new AtomicLong(System.currentTimeMillis());

  // Luồng polling hiện hành; dùng để watchdog tự khởi động lại khi treo.
  private volatile Thread pollingThread;

  public TelegramService(
      TradingService tradingService,
      MouseService mouseService,
      StatisticalRepository statisticalRepository) {

    this.tradingService = tradingService;
    this.mouseService = mouseService;

    // RestTemplate gửi tin nhắn + gọi localhost: timeout NGẮN để không treo vòng capture.
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5_000);
    factory.setReadTimeout(10_000);
    this.restTemplate = new RestTemplate(factory);

    // RestTemplate RIÊNG cho long polling: read timeout phải lớn hơn timeout=60 của Telegram.
    SimpleClientHttpRequestFactory pollingFactory = new SimpleClientHttpRequestFactory();
    pollingFactory.setConnectTimeout(10_000);
    // Telegram timeout=60
    pollingFactory.setReadTimeout(70_000);
    this.pollingRestTemplate = new RestTemplate(pollingFactory);

    this.statisticalRepository = statisticalRepository;
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

    startPollingThread();

    Thread watchdogThread = new Thread(this::watchdog);

    watchdogThread.setName("telegram-watchdog");
    watchdogThread.setDaemon(true);
    watchdogThread.start();

    log.info("Telegram command bot started...");
  }

  // Tạo (hoặc tạo lại) luồng polling. Luồng cũ nếu còn sống sẽ bị yêu cầu dừng;
  // vòng longPolling chỉ chạy khi nó vẫn là pollingThread hiện hành nên luồng cũ tự thoát.
  private synchronized void startPollingThread() {

    Thread old = pollingThread;

    if (old != null && old.isAlive()) {
      old.interrupt();
    }

    Thread t = new Thread(this::longPolling);

    t.setName("telegram-polling");
    t.setDaemon(true);

    pollingThread = t;

    t.start();
  }

  private void watchdog() {

    boolean notified = false;

    while (true) {

      try {

        long diff = System.currentTimeMillis() - lastPollingSuccess.get();

        if (diff > 120_000) {

          if (!notified) {

            sendMessageAdmin("⚠️ Telegram polling bất thường, đang khởi động lại...");

            notified = true;
          }

          // Tự khởi động lại luồng polling khi phát hiện treo.
          startPollingThread();

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
  // SKIP PENDING UPDATES (tránh replay command cũ khi restart)
  // =========================================================

  private void skipPendingUpdates() {

    try {

      // offset=-1 trả về update mới nhất đang tồn đọng (nếu có).
      String url =
          "https://api.telegram.org/bot" + commandBotToken + "/getUpdates?timeout=0&offset=-1";

      Map response = pollingRestTemplate.getForObject(url, Map.class);

      if (response == null) {
        return;
      }

      List<Map> results = (List<Map>) response.get("result");

      if (results == null || results.isEmpty()) {
        return;
      }

      // Đặt updateId = update mới nhất => vòng polling sẽ gọi offset=updateId+1,
      // confirm và bỏ qua toàn bộ command tồn đọng.
      Map last = results.get(results.size() - 1);
      updateId = ((Number) last.get("update_id")).longValue();

      log.info("Đã bỏ qua command tồn đọng, bắt đầu từ offset {}", updateId + 1);

    } catch (Exception e) {

      log.error("Skip pending updates failed", e);
    }
  }

  // =========================================================
  // LONG POLLING
  // =========================================================

  private void longPolling() {

    // Bỏ qua toàn bộ command tồn đọng trước khi vào vòng polling
    // để tránh thực thi lại lệnh cũ sau khi service khởi động lại.
    skipPendingUpdates();

    // Chỉ chạy khi vẫn là luồng polling hiện hành; nếu watchdog đã tạo luồng mới
    // thì luồng cũ này sẽ tự thoát ở vòng lặp kế tiếp, tránh chạy song song (409).
    while (pollingThread == Thread.currentThread()) {

      try {

        String url =
            "https://api.telegram.org/bot"
                + commandBotToken
                + "/getUpdates?timeout=60&offset="
                + (updateId + 1);

        Map response = pollingRestTemplate.getForObject(url, Map.class);

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

  private void handleCommand(String message) throws Exception {
    String[] args = message.trim().split("\\s+");
    switch (args[0]) {
      case "/start":
        callStart();
        break;

      case "/stop":
        callStop();
        break;

      case "/data":
        getData();
        break;
      // ví dụ: /update 14789 0
      case "/update":
        int id = Integer.parseInt(args[1]);
        int value = Integer.parseInt(args[2]);
        callUpdate(id, value);
        break;

      // vi du: /insert 0 1 0 1 1 1
      case "/insert":
        List<Integer> values = Arrays.stream(args).skip(1).map(Integer::parseInt).toList();
        callInsert(values);
        break;

      case "/trade_on":
        tradingService.start();
        sendMessageAdmin("✅ Trading ON");
        break;

      case "/trade_off":
        tradingService.stop();
        sendMessageAdmin("🛑 Trading OFF");
        break;

      case "/get-location":
        getLocation();
        break;

      case "/reset-browser":
        mouseService.resetBrowser();
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

  private void callUpdate(Integer id, Integer value) {
    try {
      StatisticalEntity statisticalEntity = statisticalRepository.findByStatisticalId(id);
      statisticalEntity.setResult(value);
      statisticalRepository.save(statisticalEntity);
    } catch (Exception e) {
      sendMessageAdmin("❌ Call API update data failed");
    }
  }

  private void getLocation() {
    try {
      PointerInfo a = MouseInfo.getPointerInfo();
      Point p = a.getLocation();

      String msg = "X: " + p.x + " | Y: " + p.y;
      sendMessageAdmin(msg);

    } catch (Exception e) {
      sendMessageAdmin("❌ Call API INSERT data failed");
    }
  }

  private void callInsert(List<Integer> values) {
    int today = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

    try {
      List<StatisticalEntity> entities =
          values.stream()
              .map(
                  value -> {
                    StatisticalEntity entity = new StatisticalEntity();
                    entity.setResult(value);
                    entity.setDate(today);
                    return entity;
                  })
              .toList();

      statisticalRepository.saveAll(entities);
      sendMessageAdmin("✅ Call API INSERT data");
    } catch (Exception e) {
      sendMessageAdmin("❌ Call API INSERT data failed");
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
