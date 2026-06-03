package com.example.demo.service;

import com.example.demo.data.entity.StatisticalEntity;
import com.example.demo.enums.TradeState;
import com.example.demo.event.SignalEvent;
import com.example.demo.event.TradeSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingService {

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
  public void onSignalEvent(SignalEvent event) {
    // =========================
    // GATE 1: ON/OFF SYSTEM
    // =========================
    if (!enabled) {
      botTradingService.sendMessageTrading("Signal ignored (OFF)");
      return;
    }

    StatisticalEntity current = event.getEntity();

    if (session == null) {

      // Kiểm tra điều kiện mở phiên
      if (!canOpenSession(current)) {
        return;
      }

      // Tạo session mới
      session = TradeSession.builder().openTrade(current).totalMoney(15).vol(1).num(1).build();
      botTradingService.sendMessageTrading("Bắt đầu mở phiên");

      if (session.getOpenTrade().getEqResult() > 0) {
        botTradingService.sendMessageTrading("1. Lenh bat dau: X");
        // Xử lý click
      } else {
        botTradingService.sendMessageTrading("1. Lenh bat dau: T");
        // Xử lý click
      }

      return;
    }

    if (session.getOpenTrade() == null) {
      if (!canContinueSession(current)) {
        return;
      }
      session.setOpenTrade(current);
      botTradingService.sendMessageTrading("MO LENH TIEP THEO #" + session.getNum());
      return;
    }

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
      session = null;
      botTradingService.sendMessageTrading("KET THUC PHIEN\n" + "Ket qua: " + totalMoneyNew);
      return;
    }

    // Quan trọng:
    // Lệnh này đã có kết quả rồi
    session.setOpenTrade(null);

    session.setTotalMoney(totalMoneyNew);

    if (totalMoneyNew > 13) {
      session.setVol(1);
    } else if (totalMoneyNew == 13) {
      session.setVol(3);
    } else if (totalMoneyNew > 6) {
      session.setVol(2);
    } else if (totalMoneyNew == 6) {
      session.setVol(6);
    }

    if (canContinueSession(current)) {
      session.setNum(session.getNum() + 1);
      session.setOpenTrade(current);

      switch (session.getVol()) {
        case 1:
          if (session.getOpenTrade().getEqResult() > 0) {
            botTradingService.sendMessageTrading(session.getNum() + ". Lenh bat dau: X");
            // Xử lý click
          } else {
            botTradingService.sendMessageTrading(session.getNum() + ". Lenh bat dau: T");
            // Xử lý click
          }
          break;
        case 2:
          if (session.getOpenTrade().getEqResult() > 0) {
            botTradingService.sendMessageTrading(session.getNum() + ". Lenh bat dau: X");
            // Xử lý click
          } else {
            botTradingService.sendMessageTrading(session.getNum() + ". Lenh bat dau: T");
            // Xử lý click
          }
          break;

        case 3:
          if (session.getOpenTrade().getEqResult() > 0) {
            botTradingService.sendMessageTrading(session.getNum() + ". Lenh bat dau: X");
            // Xử lý click
          } else {
            botTradingService.sendMessageTrading(session.getNum() + ". Lenh bat dau: T");
            // Xử lý click
          }
          break;

        case 6:
          if (session.getOpenTrade().getEqResult() > 0) {
            botTradingService.sendMessageTrading(session.getNum() + ". Lenh bat dau: X");
            // Xử lý click
          } else {
            botTradingService.sendMessageTrading(session.getNum() + ". Lenh bat dau: T");
            // Xử lý click
          }
          break;
      }
    }
  }
}
