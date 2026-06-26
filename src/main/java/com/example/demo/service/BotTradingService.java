package com.example.demo.service;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class BotTradingService {

  @Value("${telegram.trading.bot.token}")
  private String tradingBotToken;

  @Value("${telegram.trading.chat.id}")
  private String tradingChatId;

  private final RestTemplate restTemplate;

  // Inject lazy để tránh phụ thuộc vòng:
  // BotTradingService -> TelegramService -> TradingService -> BotTradingService
  private final TelegramService telegramService;

  public BotTradingService(@Lazy TelegramService telegramService) {
    this.telegramService = telegramService;

    // Timeout để 1 cú gửi Telegram bị treo không làm đứng cả luồng job/trading.
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5_000);
    factory.setReadTimeout(10_000);
    this.restTemplate = new RestTemplate(factory);
  }

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
      // TODO(tạm thời): khi hệ thống đã chạy ổn định thì có thể bỏ phần báo timeout này.
      if (isTimeout(e)) {
        log.error("Trading Telegram send TIMEOUT", e);
        notifyAdminSafe(
            "⏱️ <b>TIMEOUT khi gửi Telegram (bot trading)</b>\n\n"
                + "Đã bỏ qua message này để không treo luồng trading.\n"
                + "Vào kiểm tra mạng/Telegram ngay.\n"
                + "Chi tiết: "
                + e.getMessage());
      } else {
        log.error("Trading Telegram send failed", e);
      }
    }
  }

  // Lần theo chuỗi nguyên nhân để xác định có phải lỗi timeout không.
  private boolean isTimeout(Throwable e) {
    Throwable cur = e;
    while (cur != null) {
      if (cur instanceof SocketTimeoutException) {
        return true;
      }
      cur = cur.getCause();
    }
    return false;
  }

  // Báo admin qua bot command (token khác) nên thường vẫn gửi được dù bot trading lỗi.
  private void notifyAdminSafe(String message) {
    try {
      telegramService.sendMessageAdmin(message);
    } catch (Exception ex) {
      log.error("Notify admin (timeout) failed", ex);
    }
  }
}
