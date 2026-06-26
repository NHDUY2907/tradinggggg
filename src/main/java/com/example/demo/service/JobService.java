package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${trade.point.start.x}")
  private int startX;

  @Value("${trade.point.start.y}")
  private int startY;

  private final ThreadPoolTaskScheduler scheduler;
  private final ScanService scanService;
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

                  scanService.captureCircleCenter(x, y, size);

                } catch (Throwable e) {
                  // Bắt cả Throwable (gồm Error như OutOfMemoryError/AWTError):
                  // nếu để lọt ra ngoài, scheduleAtFixedRate sẽ âm thầm hủy job vĩnh viễn.
                  log.error("Error while running capture job", e);

                  try {
                    telegramService.sendMessageAdmin(
                        "❌ <b>JOB CAPTURE BỊ LỖI</b>"
                            + "\n\n"
                            + "Loại lỗi: "
                            + e.getClass().getSimpleName()
                            + "\n"
                            + "Chi tiết:"
                            + "\n"
                            + e.getMessage());
                  } catch (Throwable notifyError) {
                    log.error("Notify admin (job capture error) failed", notifyError);
                  }
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
}
