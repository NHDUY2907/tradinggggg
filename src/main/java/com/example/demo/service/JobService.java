package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
  private final TelegramService telegramService;

  private ScheduledFuture<?> future;

  // =========================================================
  // START JOB
  // =========================================================

  public synchronized void startJob(int x, int y, int size) {

    try {

      // stop job cũ nếu có
      stopJob();

      future =
          scheduler.scheduleAtFixedRate(
              () -> {
                try {

                  screenCaptureService.captureCircleCenter(x, y, size);

                } catch (Exception e) {

                  log.error("Error while running capture job", e);

                  telegramService.sendMessageAdmin(
                      "❌ <b>JOB CAPTURE BỊ LỖI</b>" + "\n\n" + "Chi tiết:" + "\n" + e.getMessage());
                }
              },
              Instant.now(),
              Duration.ofSeconds(70));

      log.info("Capture job started");

      telegramService.sendMessageAdmin("🟢 <b>ĐÃ KHỞI ĐỘNG JOB CAPTURE</b>");

    } catch (Exception e) {

      log.error("Start job failed", e);

      telegramService.sendMessageAdmin(
          "❌ <b>KHỞI ĐỘNG JOB THẤT BẠI</b>" + "\n\n" + "Chi tiết:" + "\n" + e.getMessage());
    }
  }

  // =========================================================
  // STOP JOB
  // =========================================================

  public synchronized void stopJob() {

    try {

      if (future != null && !future.isCancelled() && !future.isDone()) {

        future.cancel(true);

        log.info("Capture job stopped");

        telegramService.sendMessageAdmin("🛑 <b>ĐÃ DỪNG JOB CAPTURE</b>");
      }

    } catch (Exception e) {

      log.error("Stop job failed", e);

      telegramService.sendMessageAdmin(
          "❌ <b>DỪNG JOB THẤT BẠI</b>" + "\n\n" + "Chi tiết:" + "\n" + e.getMessage());
    }
  }

  // =========================================================
  // CHECK RUNNING
  // =========================================================

  public boolean isRunning() {

    return future != null && !future.isCancelled() && !future.isDone();
  }

  // =========================================================
  // AUTO CHECK EVERY DAY 5:30 AM
  // =========================================================

  @Scheduled(cron = "30 29 5 * * *")
  public void autoStartJobMorning() {

    try {

      log.info("Checking capture job at 5:30 AM");

      if (!isRunning()) {

        telegramService.sendMessageAdmin(
            "⚠️ <b>JOB CHƯA CHẠY</b>" + "\n\n" + "Hệ thống đang tự khởi động lại...");

        startJob(2475, 572, 9);

      } else {

        telegramService.sendMessageAdmin("✅ <b>JOB VẪN ĐANG HOẠT ĐỘNG BÌNH THƯỜNG</b>");
      }

    } catch (Exception e) {

      log.error("Auto start job failed", e);

      telegramService.sendMessageAdmin(
          "❌ <b>TỰ KHỞI ĐỘNG JOB THẤT BẠI</b>" + "\n\n" + "Chi tiết:" + "\n" + e.getMessage());
    }
  }
}
