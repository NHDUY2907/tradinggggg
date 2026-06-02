package com.example.demo.service;

import com.example.demo.data.entity.StatisticalEntity;
import com.example.demo.enums.TradeState;
import com.example.demo.event.SignalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingService {
  private final BotTradingService botTradingService;

  private volatile boolean enabled = false;
  private volatile TradeState state = TradeState.READY;
  private final int FIXED_STAKE = 15;

  // ON
  public void start() {
    enabled = true;
    log.info("TRADING ENABLED");
  }

  // OFF
  public void stop() {
    enabled = false;
    log.info("TRADING DISABLED");
  }

  private StatisticalEntity pendingEntity = null;

  @EventListener
  public void onSignalEvent(SignalEvent event) {
    // =========================
    // GATE 1: ON/OFF SYSTEM
    // =========================
    if (!enabled) {
      botTradingService.sendMessageTrading("Signal ignored (OFF)");
      return;
    }

    // =========================
    // GATE 2: STATE CHECK
    // =========================
    if (state == TradeState.PROCESSING) {
      botTradingService.sendMessageTrading("Signal ignored (PROCESSING)");
      return;
    }

    try {

      // =========================
      // LOCK STATE
      // =========================
      state = TradeState.PROCESSING;

      StatisticalEntity current = event.getEntity();

      int numberWin = 0;

      int totalMoney = 15;
      int num = 0;

      do {
        if (current.getEqResult() > 0) {
          // thực hiện vào lệnh
          // dựa vào totalMoney để thực hiện ào lệnh
          if (totalMoney > 13) { // 14-15
            // xử lý vào 1
          } else if (totalMoney == 13) { // 13
            // xử lý vào 3
          } else if (totalMoney > 6) { // 12-11-10-9-8-7
            // xử lý vào 2
          } else if (totalMoney == 6) { // 6
            // xử lý vào 6
          }
        } else {
          if (totalMoney > 13) { // 14-15
            // xử lý vào 1
          } else if (totalMoney == 13) { // 13
            // xử lý vào 3
          } else if (totalMoney > 6) { // 12-11-10-9-8-7
            // xử lý vào 2
          } else if (totalMoney == 6) { // 6
            // xử lý vào 6
          }
        }

        // Đoạn này chờ khi job scan có kết quả mới, mang ra để check ví dụ
        StatisticalEntity currentNew = event.getEntity();
        if ((currentNew.getResult() == 0 && current.getEqResult() > 0)
            || (currentNew.getResult() == 1 && current.getEqResult() < 0)) {
          // ghi nhớ vừa nãy vào 1,2,3 hay 6 để + - tương ứng
        }

        // sau đó set currentNew = current để chờ lệnh tiếp theo

        // Điều kiện chỗ while này thay vì true, thì set lại. biết khi totalMoney > 15 || totalMoney
        // = 15 || totalMoney <=0 thì thoát vòng lặp
      } while (true);

    } catch (Exception e) {

      botTradingService.sendMessageTrading("ERROR: " + e.getMessage());

    } finally {

      // =========================
      // UNLOCK STATE
      // =========================
      state = TradeState.READY;
    }
  }
}
