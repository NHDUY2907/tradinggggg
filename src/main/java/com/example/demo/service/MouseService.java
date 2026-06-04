package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MouseService {
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
}
