package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MouseService {


  @Value("${trade.point.reset.x1}")
  private int resetX1;

  @Value("${trade.point.reset.y1}")
  private int resetY1;

  @Value("${trade.point.reset.x2}")
  private int resetX2;

  @Value("${trade.point.reset.y2}")
  private int resetY2;

  @Value("${trade.point.reset.x3}")
  private int resetX3;

  @Value("${trade.point.reset.y3}")
  private int resetY3;

  @Value("${trade.point.reset.x4}")
  private int resetX4;

  @Value("${trade.point.reset.y4}")
  private int resetY4;

  @Value("${trade.point.reset.x5}")
  private int resetX5;

  @Value("${trade.point.reset.y5}")
  private int resetY5;

  @Value("${trade.point.reset.x6}")
  private int resetX6;

  @Value("${trade.point.reset.y6}")
  private int resetY6;

  @Value("${trade.point.reset.x7}")
  private int resetX7;

  @Value("${trade.point.reset.y7}")
  private int resetY7;

  @Value("${trade.point.reset.x8}")
  private int resetX8;

  @Value("${trade.point.reset.y8}")
  private int resetY8;

  @Value("${trade.point.reset.x9}")
  private int resetX9;

  @Value("${trade.point.reset.y9}")
  private int resetY9;

  private Robot robot;

  private Robot getRobot() throws AWTException {
    if (robot == null) {
      robot = new Robot();
      robot.setAutoDelay(50);
    }
    return robot;
  }

  private void randomSleep(int minMs, int maxMs) {
    try {
      Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void click(int x, int y, int clickCount) throws Exception {

    // trước thao tác
    randomSleep(0, 1000);

    Robot robot = getRobot();

    robot.mouseMove(x, y);

    for (int i = 0; i < clickCount; i++) {

      robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

      // giữa các lần click
      if (i < clickCount - 1) {
        randomSleep(0, 1000);
      }
    }
  }

  public void clickRandom(int x, int y, int minOffset, int maxOffset) throws Exception {

    randomSleep(0, 1000);

    Robot robot = getRobot();

    robot.mouseMove(x, y);

    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    randomSleep(minOffset, maxOffset);

  }

  public void resetBrowser() throws Exception {
    // Đóng
    clickRandom(resetX1, resetY1, 1000, 2000);
    // Mở
    clickRandom(resetX2, resetY2, 23000, 25000);
    // Đóng wc
    clickRandom(resetX3, resetY3, 10000, 11000);
    // Đóng live
    clickRandom(resetX4, resetY4, 1000, 2000);
    // GBai
    clickRandom(resetX5, resetY5, 1000, 2000);
    // BJ
    clickRandom(resetX6, resetY6, 5000, 6000);
    // Mini
    clickRandom(resetX7, resetY7, 1000, 2000);
    // TX
    clickRandom(resetX8, resetY8, 2000, 3000);
    // CHAT
    clickRandom(resetX9, resetY9, 1000, 2000);
  }
}
