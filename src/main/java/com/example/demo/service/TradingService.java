package com.example.demo.service;

import com.example.demo.event.SignalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingService {
  private final TelegramService telegramService;

  @EventListener
  public void onSignalEvent(SignalEvent event) {

    // TEST ONLY - chưa có logic trading
    String message =
        "📡 <b>TRADING EVENT RECEIVED</b>\n\n"
            + "🆔 Signal ID: "
            + event.getEntity().getStatisticalId()
            + "\n"
            + "📊 Type: "
            + event.getType()
            + "\n"
            + "👉 Next action: "
            + event.getNextAction();

    telegramService.sendMessage(message);
  }
}
