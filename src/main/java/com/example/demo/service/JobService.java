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

                  telegramService.sendMessage(
                      "❌ <b>JOB CAPTURE BỊ LỖI</b>" + "\n\n" + "Chi tiết:" + "\n" + e.getMessage());
                }
              },
              Instant.now(),
              Duration.ofSeconds(70));

      log.info("Capture job started");

      telegramService.sendMessage(
          "🟢 <b>ĐÃ KHỞI ĐỘNG JOB CAPTURE</b>"
              + "\n\n"
              + "📍 <b>X:</b> "
              + x
              + "\n"
              + "📍 <b>Y:</b> "
              + y
              + "\n"
              + "📐 <b>SIZE:</b> "
              + size);

    } catch (Exception e) {

      log.error("Start job failed", e);

      telegramService.sendMessage(
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

        telegramService.sendMessage("🛑 <b>ĐÃ DỪNG JOB CAPTURE</b>");
      }

    } catch (Exception e) {

      log.error("Stop job failed", e);

      telegramService.sendMessage(
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

  @Scheduled(cron = "0 30 5 * * *")
  public void autoStartJobMorning() {

    try {

      log.info("Checking capture job at 5:30 AM");

      if (!isRunning()) {

        telegramService.sendMessage(
            "⚠️ <b>JOB CHƯA CHẠY</b>" + "\n\n" + "Hệ thống đang tự khởi động lại...");

        startJob(2545, 574, 9);

      } else {

        telegramService.sendMessage("✅ <b>JOB VẪN ĐANG HOẠT ĐỘNG BÌNH THƯỜNG</b>");
      }

    } catch (Exception e) {

      log.error("Auto start job failed", e);

      telegramService.sendMessage(
          "❌ <b>TỰ KHỞI ĐỘNG JOB THẤT BẠI</b>" + "\n\n" + "Chi tiết:" + "\n" + e.getMessage());
    }
  }
}
