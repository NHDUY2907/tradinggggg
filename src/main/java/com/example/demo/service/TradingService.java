package com.example.demo.service;

import com.example.demo.data.entity.StatisticalEntity;
import com.example.demo.enums.TradeState;
import com.example.demo.event.SignalEvent;
import com.example.demo.event.TradeSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingService {
  @Value("${trade.point.x.x}")
  private int tradeXx;

  @Value("${trade.point.x.y}")
  private int tradeXy;

  @Value("${trade.point.t.x}")
  private int tradeTx;

  @Value("${trade.point.t.y}")
  private int tradeTy;

  @Value("${trade.point.price.x}")
  private int priceX;

  @Value("${trade.point.price.y}")
  private int priceY;

  @Value("${trade.point.trade.x}")
  private int okX;

  @Value("${trade.point.trade.y}")
  private int okY;

  private static final Set<String> STRONG =
      Set.of(
          "X: 1-2-3-4 | T: 1-2-3-4",
          "X: 1-2-3 | T: 1-2-3-4",
          "X: 1-2-3-4 | T: 1-2-3",
          "X: 1-2-3-4 | T: 1-2-3-4-5",
          "X: 1-2-3-4-5 | T: 1-2-3-4",
          "X: 1-2-3-4 | T: 1-2-3-6",
          "X: 1-2-3-4 | T: 1-2-3-5");

  private static final Set<String> MEDIUM =
      Set.of(
          "X: 1-2-3-4-6 | T: 1-2-3",
          "X: 1-2-3-5 | T: 1-2-3-4",
          "X: 1-2-3 | T: 1-2-3-4-5",
          "X: 1-2-3-4-5 | T: 1-2-3",
          "X: 1-2-3-4 | T: 1-2-4-5");

  private static final Set<String> WEAK =
      Set.of(
          "X: 1-2-3-4-6 | T: 1-2-3-4",
          "X: 1-2-3-5 | T: 1-2-3-5",
          "X: 1-2-3 | T: 1-2-3-5",
          "X: 1-2-3 | T: 1-2-3-4-6",
          "X: 1-2-3-4-5 | T: 1-2-3-4-5");

  private final BotTradingService botTradingService;
  private final MouseService mouseService;

  private volatile boolean enabled = false;

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

  // =========================
  // SESSION STATE (QUAN TRỌNG NHẤT)
  // =========================
  private TradeSession session = null;

  private boolean canOpenSession(StatisticalEntity current) {
    return Objects.nonNull(current.getLechDuoi())
        && Objects.nonNull(current.getLechTren())
        && Objects.nonNull(current.getLength())
        && Objects.nonNull(current.getEqResult())
        && List.of(1, 2, -1, -2).contains(current.getEqResult())
        && current.getLechTren() <= 6
        && current.getLechDuoi() <= 12
        && (STRONG.contains(current.getLength())
            || MEDIUM.contains(current.getLength())
            || WEAK.contains(current.getLength()));
  }

  private boolean canContinueSession(StatisticalEntity current) {
    return current.getEqResult() != null;
  }

  @EventListener
  public void onSignalEvent(SignalEvent event) throws Exception {
    // =========================
    // GATE 1: ON/OFF SYSTEM
    // =========================
    if (!enabled) {
      botTradingService.sendMessageTrading("Signal ignored (OFF)");
//
//      mouseService.click(tradeXx, tradeXy, 1);
//      mouseService.click(priceX, priceY, session.getVol());
//      mouseService.click(okX, okY, 1);

      mouseService.click(tradeTx, tradeTy, 1);
      mouseService.click(priceX, priceY, 1);
      mouseService.click(okX, okY, 1);

      return;
    }

    StatisticalEntity current = event.getEntity();

    // Luồng mở phiên vào lệnh rồi return
    if (session == null) {

      // Kiểm tra điều kiện mở phiên
      if (!canOpenSession(current)) {
        return;
      }

      // Tạo session mới
      session = TradeSession.builder().openTrade(current).totalMoney(15).vol(1).num(1).build();
      botTradingService.sendMessageTrading(
          "🚀 ========================= "
              + "\n"
              + "🚀 BẮT ĐẦU PHIÊN GIAO DỊCH "
              + "\n"
              + "🚀 =========================");

      if (session.getOpenTrade().getEqResult() > 0) {
        botTradingService.sendMessageTrading(
            """
                📌 Lệnh thứ      : %s
                📊 Vol           : %s
                💰 CurrentMoney  : %s
                ➡️ Action        : X
                """
                .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
        // Xử lý click
        mouseService.click(tradeXx, tradeXy, 1);
        mouseService.click(priceX, priceY, session.getVol());
        mouseService.click(okX, okY, 1);

      } else {
        botTradingService.sendMessageTrading(
            """
          📌 Lệnh thứ      : %s
          📊 Vol           : %s
          💰 CurrentMoney  : %s
          ➡️ Action        : T
          """
                .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
        // Xử lý click
        mouseService.click(tradeTx, tradeTy, 1);
        mouseService.click(priceX, priceY, session.getVol());
        mouseService.click(okX, okY, 1);
      }

      return;
    }

    if (session.getOpenTrade() != null) {
      // Thực hiện tính toán và đưa ra kết quả kết thúc hay tiếp tục
      StatisticalEntity entityOld = session.getOpenTrade();

      int totalMoneyNew;

      if ((entityOld.getEqResult() > 0 && current.getResult() == 0)
          || (entityOld.getEqResult() < 0 && current.getResult() == 1)) {
        totalMoneyNew = session.getTotalMoney() + session.getVol();
      } else {
        totalMoneyNew = session.getTotalMoney() - session.getVol();
      }

      if (totalMoneyNew >= 15 || totalMoneyNew == 0) {
        botTradingService.sendMessageTrading(
            """
                🏁 =========================
                🏁 KẾT THÚC PHIÊN
                🏁 =========================

                📌 Số lệnh giao dịch : %s
                💰 Vốn cuối phiên    : %s
                📈 Kết quả           : %s
                """
                .formatted(
                    session.getNum(), totalMoneyNew, totalMoneyNew > 15 ? "WIN ✅" : "LOSS ❌"));
        session = null;
        return;
      }

      // Đoạn này đánh dấu tôi đã tính toán xong lệnh trước rồi
      session.setOpenTrade(null);

      session.setTotalMoney(totalMoneyNew);

      // Xác định vol tiếp theo
      if (totalMoneyNew > 13) {
        session.setVol(1);
      } else if (totalMoneyNew == 13) {
        session.setVol(3);
      } else if (totalMoneyNew > 6) {
        session.setVol(2);
      } else if (totalMoneyNew == 6) {
        session.setVol(6);
      }
    }

    if (canContinueSession(current)) {
      session.setNum(session.getNum() + 1);
      session.setOpenTrade(current);

      switch (session.getVol()) {
        case 1:
          if (session.getOpenTrade().getEqResult() > 0) {
            botTradingService.sendMessageTrading(
                """
                          📌 Lệnh thứ      : %s
                          📊 Vol           : %s
                          💰 CurrentMoney  : %s
                          ➡️ Action        : X
                          """
                    .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
            // Xử lý click
            mouseService.click(tradeXx, tradeXy, 1);
            mouseService.click(priceX, priceY, session.getVol());
            mouseService.click(okX, okY, 1);
          } else {
            botTradingService.sendMessageTrading(
                """
                    📌 Lệnh thứ      : %s
                    📊 Vol           : %s
                    💰 CurrentMoney  : %s
                    ➡️ Action        : T
                    """
                    .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
            // Xử lý click
            mouseService.click(tradeTx, tradeTy, 1);
            mouseService.click(priceX, priceY, session.getVol());
            mouseService.click(okX, okY, 1);
          }
          break;
        case 2:
          if (session.getOpenTrade().getEqResult() > 0) {
            botTradingService.sendMessageTrading(
                """
                          📌 Lệnh thứ      : %s
                          📊 Vol           : %s
                          💰 CurrentMoney  : %s
                          ➡️ Action        : X
                          """
                    .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
            // Xử lý click
            mouseService.click(tradeXx, tradeXy, 1);
            mouseService.click(priceX, priceY, session.getVol());
            mouseService.click(okX, okY, 1);
          } else {
            botTradingService.sendMessageTrading(
                """
                    📌 Lệnh thứ      : %s
                    📊 Vol           : %s
                    💰 CurrentMoney  : %s
                    ➡️ Action        : T
                    """
                    .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
            // Xử lý click
            mouseService.click(tradeTx, tradeTy, 1);
            mouseService.click(priceX, priceY, session.getVol());
            mouseService.click(okX, okY, 1);
          }
          break;

        case 3:
          if (session.getOpenTrade().getEqResult() > 0) {
            botTradingService.sendMessageTrading(
                """
                          📌 Lệnh thứ      : %s
                          📊 Vol           : %s
                          💰 CurrentMoney  : %s
                          ➡️ Action        : X
                          """
                    .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
            // Xử lý click
            mouseService.click(tradeXx, tradeXy, 1);
            mouseService.click(priceX, priceY, session.getVol());
            mouseService.click(okX, okY, 1);
          } else {
            botTradingService.sendMessageTrading(
                """
                    📌 Lệnh thứ      : %s
                    📊 Vol           : %s
                    💰 CurrentMoney  : %s
                    ➡️ Action        : T
                    """
                    .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
            // Xử lý click
            mouseService.click(tradeTx, tradeTy, 1);
            mouseService.click(priceX, priceY, session.getVol());
            mouseService.click(okX, okY, 1);
          }
          break;

        case 6:
          if (session.getOpenTrade().getEqResult() > 0) {
            botTradingService.sendMessageTrading(
                """
                          📌 Lệnh thứ      : %s
                          📊 Vol           : %s
                          💰 CurrentMoney  : %s
                          ➡️ Action        : X
                          """
                    .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
            // Xử lý click
            mouseService.click(tradeXx, tradeXy, 1);
            mouseService.click(priceX, priceY, session.getVol());
            mouseService.click(okX, okY, 1);
          } else {
            botTradingService.sendMessageTrading(
                """
                    📌 Lệnh thứ      : %s
                    📊 Vol           : %s
                    💰 CurrentMoney  : %s
                    ➡️ Action        : T
                    """
                    .formatted(session.getNum(), session.getVol(), session.getTotalMoney()));
            // Xử lý click
            mouseService.click(tradeTx, tradeTy, 1);
            mouseService.click(priceX, priceY, session.getVol());
            mouseService.click(okX, okY, 1);
          }
          break;
      }
    }
  }
}
