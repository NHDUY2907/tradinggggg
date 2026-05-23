package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

  private final ThreadPoolTaskScheduler scheduler;
  private final ScreenCaptureService screenCaptureService;

  private ScheduledFuture<?> future;

  public void startJob(int x, int y, int size) {

    // stop job cũ nếu có
    if (future != null && !future.isCancelled()) {
      future.cancel(true);
    }

    future =
        scheduler.scheduleAtFixedRate(
            () -> {
              try {
                screenCaptureService.captureCircleCenterV1(x, y, size);
              } catch (Exception e) {
                log.error("Error while running capture job", e);
              }
            },
            Instant.now(),
            Duration.ofSeconds(70));
  }

  public void stopJob() {
    if (future != null) {
      future.cancel(true);
    }
  }
}
