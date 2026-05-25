package com.example.demo.service;

import com.example.demo.data.entity.StatisticalEntity;
import com.example.demo.data.repository.StatisticalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatorService {
  private final StatisticalRepository statisticalRepository;
  private final TelegramService telegramService;

  public void getData() {
    int today = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

    List<StatisticalEntity> statisticalEntities =
        statisticalRepository.findTop10ByDateOrderByStatisticalIdDesc(today);
    if (statisticalEntities == null || statisticalEntities.isEmpty()) {
      return;
    }

    Collections.reverse(statisticalEntities);

    StringBuilder message = new StringBuilder();

    for (int i = 0; i < statisticalEntities.size(); i++) {

      StatisticalEntity entity = statisticalEntities.get(i);

      // Record mới nhất (cuối cùng)
      boolean isLast = (i == statisticalEntities.size() - 1);

      message.append(i + 1).append(". ").append(entity.getResult());

      if (isLast) {
        message.append(" | Length: ").append(entity.getLength());
      }

      message.append("\n");
    }
    telegramService.sendMessage(message.toString());
  }

  public Map<Integer, Double> calculatorMain(List<Integer> ytd) {
    Map<Integer, Double> result = new HashMap<>();
    ytd.forEach(
        date -> {
          // Tat ca
          List<StatisticalEntity> statisticalEntities = statisticalRepository.findAllByDate(date);

          // Danh sach chuẩn
          List<StatisticalEntity> totalResult =
              statisticalEntities.stream()
                  .filter(
                      item -> (Objects.nonNull(item.getWin()) || Objects.nonNull(item.getLose())))
                  .toList();

          for (int i = 0; i < totalResult.size(); i++) {

            // Số lan win
            int numberWin = 0;

            int totalMoney = 15;
            int num = 0;
            do {
              if (i + num > totalResult.size() - 1) {
                break;
              }

              if (Objects.equals(totalResult.get(i + num).getWin(), 1)) {
                if (totalMoney > 13) { // 14-15
                  totalMoney += 1;
                } else if (totalMoney == 13) { // 13
                  totalMoney += 3;
                } else if (totalMoney > 6) { // 12-11-10-9-8-7
                  totalMoney += 2;
                } else if (totalMoney == 6) { // 6
                  totalMoney += 6;
                }
              } else if (Objects.equals(totalResult.get(i + num).getLose(), 1)) {
                if (totalMoney > 13) { // 14-15
                  totalMoney -= 1;
                } else if (totalMoney == 13) { // 13
                  totalMoney -= 3;
                } else if (totalMoney > 6) { // 12-11-10-9-8-7
                  totalMoney -= 2;
                } else if (totalMoney == 6) { // 21
                  totalMoney -= 6;
                }
              }

              if (num > 0 && totalMoney == 15) {
                break;
              }

              if (totalMoney <= 0) {
                totalResult.get(i).setLoseLast(1);
                totalResult.get(i).setSoLanCuoc(-(num + 1));
                break;
              }
              if (totalMoney > 15) {
                totalMoney = 15;
                numberWin++;
              }
              if (numberWin == 1) {
                totalResult.get(i).setWinLast(1);
                totalResult.get(i).setSoLanCuoc(num + 1);
                break;
              }

              num++;
            } while (i < statisticalEntities.size() - 1);
          }

          statisticalRepository.saveAll(totalResult);

          long total =
              totalResult.stream()
                  .filter(
                      item ->
                          Objects.nonNull(item.getWinLast()) || Objects.nonNull(item.getLoseLast()))
                  .count();

          long totalWin =
              totalResult.stream().filter(item -> Objects.equals(item.getWinLast(), 1)).count();
          long totalLose =
              totalResult.stream().filter(item -> Objects.equals(item.getLoseLast(), 1)).count();

          System.out.println("Total: " + total);
          System.out.println("TotalWin: " + totalWin);
          System.out.println("totalLose: " + totalLose);

          result.put(date, ((double) totalWin / total) * 100);
        });
    return result.entrySet().stream()
        .sorted(Map.Entry.comparingByKey()) // Sắp xếp theo khóa
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue, // Xử lý xung đột (nếu có)
                LinkedHashMap::new // Giữ nguyên thứ tự đã sắp xếp
                ));
  }

  public void calculatorWinOrLose(List<Integer> ytd) {
    ytd.forEach(
        date -> {
          List<StatisticalEntity> statisticalEntities = statisticalRepository.findAllByDate(date);

          for (int i = 50; i < statisticalEntities.size() - 1; i++) {

            if (Objects.nonNull(statisticalEntities.get(i - 1).getEqResult())){
              int count = statisticalEntities.get(i - 1).getEqResult();

              if (count >= 1 && count < 6) {
                if (statisticalEntities.get(i).getResult() == 0) {
                  statisticalEntities.get(i).setWin(1);
                } else {
                  statisticalEntities.get(i).setLose(1);
                }
              }
              if (count <= -1 && count > -6) {
                if (statisticalEntities.get(i).getResult() == 1) {
                  statisticalEntities.get(i).setWin(1);
                } else {
                  statisticalEntities.get(i).setLose(1);
                }
              }
            }

          }
          statisticalRepository.saveAll(statisticalEntities);
        });
  }
}
