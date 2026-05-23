package com.example.demo.service;

import com.example.demo.data.entity.StatisticalEntity;
import com.example.demo.data.repository.StatisticalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatorService {
  private final StatisticalRepository statisticalRepository;

  public Map<Integer, Double> calculatorRealV3(List<Integer> ytd, int totalNum) {
    Map<Integer, Double> result = new HashMap<>();
    ytd.forEach(
        date -> {
          // Tat ca
          List<StatisticalEntity> statisticalEntities = statisticalRepository.findAllByDate(date);

          // ID ban ghi thu 100
          int id = statisticalEntities.get(100).getStatisticalId();

          // Danh sach chuẩn
          List<StatisticalEntity> totalResult =
              statisticalEntities.stream()
                  .filter(
                      item -> (Objects.nonNull(item.getWin()) || Objects.nonNull(item.getLose())))
                  .toList();

          for (int i = 0; i < totalResult.size(); i++) {
            int idV1 = totalResult.get(i).getStatisticalId();

            // Tinh lech duoi
            if (idV1 >= id) {
              List<StatisticalEntity> subList =
                  statisticalEntities.stream()
                      .filter(
                          item ->
                              item.getStatisticalId() < idV1
                                  && item.getStatisticalId() >= idV1 - 100)
                      .toList();
              int lechDuoi = 0;
              int t = (int) subList.stream().filter(item -> item.getResult() == 1).count();
              int x = (int) subList.stream().filter(item -> item.getResult() == 0).count();
              if (t > x) {
                lechDuoi = t - x;
              } else if (t < x) {
                lechDuoi = x - t;
              }
              totalResult.get(i).setLechDuoi(lechDuoi);
            }
            // Tinh lech tren
            if (idV1 >= id) {
              List<StatisticalEntity> subList = new ArrayList<>();
              List<StatisticalEntity> subListV1 =
                  statisticalEntities.stream()
                      .filter(
                          item ->
                              item.getStatisticalId() < idV1
                                  && item.getStatisticalId() >= idV1 - 100)
                      .sorted(Comparator.comparing(StatisticalEntity::getStatisticalId).reversed())
                      .toList();
              int count = 0;
              StatisticalEntity prev = subListV1.get(0);
              List<StatisticalEntity> tempList = new ArrayList<>();
              tempList.add(prev);

              for (int j = 1; j < subListV1.size(); j++) {
                if (Objects.equals(subListV1.get(j).getResult(), prev.getResult())) {
                  tempList.add(subListV1.get(j));
                  if (tempList.size() > 5 && tempList.size() <= 10) {
                    count++;
                  } else if (tempList.size() > 10 && tempList.size() <= 15) {
                    count++;
                  } else if (tempList.size() > 15 && tempList.size() <= 20) {
                    count++;
                  } else if (tempList.size() > 20 && tempList.size() <= 25) {
                    count++;
                  }
                } else {
                  count++;
                  if (count == 20) {
                    break;
                  }
                  subList.addAll(tempList);
                  tempList.clear();
                  tempList.add(subListV1.get(j));
                  prev = subListV1.get(j);
                }
              }

              subList.addAll(tempList);

              int lechTren = 0;
              int t = (int) subList.stream().filter(item -> item.getResult() == 1).count();
              int x = (int) subList.stream().filter(item -> item.getResult() == 0).count();
              if (t > x) {
                lechTren = t - x;
              } else if (t < x) {
                lechTren = x - t;
              }
              totalResult.get(i).setLechTren(lechTren);
            }

            // Số lan win
            int numberWin = 0;

            int totalMoney = 30;
            int num = 0;
            do {
              if (i + num > totalResult.size() - 1) {
                break;
              }

              if (Objects.equals(totalResult.get(i + num).getWin(), 1)) {
                if (totalMoney > 28) { // 30-29
                  totalMoney += 1;
                } else if (totalMoney == 28) { // 28
                  totalMoney += 3;
                } else if (totalMoney > 21) { // 27-26-25-24-23-22
                  totalMoney += 2;
                } else if (totalMoney == 21) { // 21
                  totalMoney += 6;
                } else // 11
                if (totalMoney > 9) { // 20-19-18-17-16-15-14-13-12-11-10
                  totalMoney += 3;
                } else totalMoney += totalMoney;
              } else if (Objects.equals(totalResult.get(i + num).getLose(), 1)) {
                if (totalMoney > 28) { // 30-29
                  totalMoney -= 1;
                } else if (totalMoney == 28) { // 28
                  totalMoney -= 3;
                } else if (totalMoney > 21) { // 27-26-25-24-23-22
                  totalMoney -= 2;
                } else if (totalMoney == 21) { // 21
                  totalMoney -= 6;
                } else // 11
                if (totalMoney > 9) { // 20-19-18-17-16-15-14-13-12-11-10
                  totalMoney -= 3;
                } else totalMoney -= totalMoney;
              }

              if (totalMoney <= 0) {
                totalResult.get(i).setLoseLast(1);
                totalResult.get(i).setSoLanCuoc(-(num + 1));
                break;
              }
              if (totalMoney > 30) {
                totalMoney = 30;
                numberWin++;
              }
              if (numberWin == totalNum) {
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

  public Map<Integer, Double> calculatorRealV4(List<Integer> ytd) {
    Map<Integer, Double> result = new HashMap<>();
    ytd.forEach(
        date -> {
          // Tat ca
          List<StatisticalEntity> statisticalEntities = statisticalRepository.findAllByDate(date);

          // ID ban ghi thu 100
          int id = statisticalEntities.get(100).getStatisticalId();

          // Danh sach chuẩn
          List<StatisticalEntity> totalResult =
              statisticalEntities.stream()
                  .filter(
                      item -> (Objects.nonNull(item.getWin()) || Objects.nonNull(item.getLose())))
                  .toList();

          for (int i = 0; i < totalResult.size(); i++) {
            int idV1 = totalResult.get(i).getStatisticalId();

            // Tinh lech duoi
            if (idV1 >= id) {
              List<StatisticalEntity> subList =
                  statisticalEntities.stream()
                      .filter(
                          item ->
                              item.getStatisticalId() < idV1
                                  && item.getStatisticalId() >= idV1 - 100)
                      .toList();
              int lechDuoi = 0;
              int tai = (int) subList.stream().filter(item -> item.getResult() == 1).count();
              int xiu = (int) subList.stream().filter(item -> item.getResult() == 0).count();
              if (tai > xiu) {
                lechDuoi = tai - xiu;
              } else if (tai < xiu) {
                lechDuoi = xiu - tai;
              }
              totalResult.get(i).setLechDuoi(lechDuoi);
            }
            // Tinh lech tren
            if (idV1 >= id) {
              List<StatisticalEntity> subList = new ArrayList<>();
              List<StatisticalEntity> subListV1 =
                  statisticalEntities.stream()
                      .filter(
                          item ->
                              item.getStatisticalId() < idV1
                                  && item.getStatisticalId() >= idV1 - 100)
                      .sorted(Comparator.comparing(StatisticalEntity::getStatisticalId).reversed())
                      .toList();
              int count = 0;
              StatisticalEntity prev = subListV1.get(0);
              List<StatisticalEntity> tempList = new ArrayList<>();
              tempList.add(prev);

              for (int j = 1; j < subListV1.size(); j++) {
                if (Objects.equals(subListV1.get(j).getResult(), prev.getResult())) {
                  tempList.add(subListV1.get(j));
                  if (tempList.size() > 5 && tempList.size() <= 10) {
                    count++;
                  } else if (tempList.size() > 10 && tempList.size() <= 15) {
                    count++;
                  } else if (tempList.size() > 15 && tempList.size() <= 20) {
                    count++;
                  } else if (tempList.size() > 20 && tempList.size() <= 25) {
                    count++;
                  }
                } else {
                  count++;
                  if (count == 20) {
                    break;
                  }
                  subList.addAll(tempList);
                  tempList.clear();
                  tempList.add(subListV1.get(j));
                  prev = subListV1.get(j);
                }
              }

              subList.addAll(tempList);

              int lechTren = 0;
              int tai = (int) subList.stream().filter(item -> item.getResult() == 1).count();
              int xiu = (int) subList.stream().filter(item -> item.getResult() == 0).count();
              if (tai > xiu) {
                lechTren = tai - xiu;
              } else if (tai < xiu) {
                lechTren = xiu - tai;
              }
              totalResult.get(i).setLechTren(lechTren);
            }

            // Số lan win
            int numberWin = 0;

            int totalMoney = 35;
            int num = 0;
            do {
              if (i + num > totalResult.size() - 1) {
                break;
              }

              if (Objects.equals(totalResult.get(i + num).getWin(), 1)) {
                if (totalMoney > 33) { // 35-34
                  totalMoney += 1;
                } else if (totalMoney == 33) { // 33
                  totalMoney += 3;
                } else if (totalMoney > 26) { // 32-31-30-29-28-27
                  totalMoney += 2;
                } else if (totalMoney == 26) { // 26
                  totalMoney += 6;
                } else // 11
                if (totalMoney > 12) { // 20-19-18-17-16-15-14-13-12-11-10
                  totalMoney += 4;
                } else totalMoney += totalMoney;
              } else if (Objects.equals(totalResult.get(i + num).getLose(), 1)) {
                if (totalMoney > 33) { // 30-29
                  totalMoney -= 1;
                } else if (totalMoney == 33) { // 28
                  totalMoney -= 3;
                } else if (totalMoney > 26) { // 27-26-25-24-23-22
                  totalMoney -= 2;
                } else if (totalMoney == 26) { // 21
                  totalMoney -= 6;
                } else // 11
                if (totalMoney > 12) { // 20-19-18-17-16-15-14-13-12-11-10
                  totalMoney -= 4;
                } else totalMoney -= totalMoney;
              }

              if (totalMoney <= 0) {
                totalResult.get(i).setLoseLast(1);
                totalResult.get(i).setSoLanCuoc(-(num + 1));
                break;
              }
              if (totalMoney > 35) {
                totalMoney = 35;
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

  public Map<Integer, Double> calculatorRealV5(List<Integer> ytd) {
    Map<Integer, Double> result = new HashMap<>();
    ytd.forEach(
        date -> {
          // Tat ca
          List<StatisticalEntity> statisticalEntities = statisticalRepository.findAllByDate(date);

          // ID ban ghi thu 100
//          int id = statisticalEntities.get(100).getStatisticalId();

          // Danh sach chuẩn
          List<StatisticalEntity> totalResult =
              statisticalEntities.stream()
                  .filter(
                      item -> (Objects.nonNull(item.getWin()) || Objects.nonNull(item.getLose())))
                  .toList();

          for (int i = 0; i < totalResult.size(); i++) {
//            int idV1 = totalResult.get(i).getStatisticalId();

            // trenDuoi(idV1, id, statisticalEntities, totalResult, i);

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

  private static void trenDuoi(
      int idV1,
      int id,
      List<StatisticalEntity> statisticalEntities,
      List<StatisticalEntity> totalResult,
      int i) {
    // Tinh lech duoi
    if (idV1 >= id) {
      List<StatisticalEntity> subList =
          statisticalEntities.stream()
              .filter(
                  item -> item.getStatisticalId() < idV1 && item.getStatisticalId() >= idV1 - 100)
              .toList();
      int lechDuoi = 0;
      int t = (int) subList.stream().filter(item -> item.getResult() == 1).count();
      int x = (int) subList.stream().filter(item -> item.getResult() == 0).count();
      if (t > x) {
        lechDuoi = t - x;
      } else if (t < x) {
        lechDuoi = x - t;
      }
      totalResult.get(i).setLechDuoi(lechDuoi);
    }
    // Tinh lech tren
    if (idV1 >= id) {
      List<StatisticalEntity> subList = new ArrayList<>();
      List<StatisticalEntity> subListV1 =
          statisticalEntities.stream()
              .filter(
                  item -> item.getStatisticalId() < idV1 && item.getStatisticalId() >= idV1 - 100)
              .sorted(Comparator.comparing(StatisticalEntity::getStatisticalId).reversed())
              .toList();
      int count = 0;
      StatisticalEntity prev = subListV1.get(0);
      List<StatisticalEntity> tempList = new ArrayList<>();
      tempList.add(prev);

      for (int j = 1; j < subListV1.size(); j++) {
        if (Objects.equals(subListV1.get(j).getResult(), prev.getResult())) {
          tempList.add(subListV1.get(j));
          if (tempList.size() > 5 && tempList.size() <= 10) {
            count++;
          } else if (tempList.size() > 10 && tempList.size() <= 15) {
            count++;
          } else if (tempList.size() > 15 && tempList.size() <= 20) {
            count++;
          } else if (tempList.size() > 20 && tempList.size() <= 25) {
            count++;
          }
        } else {
          count++;
          if (count == 20) {
            break;
          }
          subList.addAll(tempList);
          tempList.clear();
          tempList.add(subListV1.get(j));
          prev = subListV1.get(j);
        }
      }

      subList.addAll(tempList);

      int lechTren = 0;
      int t = (int) subList.stream().filter(item -> item.getResult() == 1).count();
      int x = (int) subList.stream().filter(item -> item.getResult() == 0).count();
      if (t > x) {
        lechTren = t - x;
      } else if (t < x) {
        lechTren = x - t;
      }
      totalResult.get(i).setLechTren(lechTren);
    }
  }

  public void calculatorLength(List<Integer> ytd) {
    ytd.forEach(
        date -> {
          List<StatisticalEntity> statisticalEntities = statisticalRepository.findAllByDate(date);

          int windowSize = 51; // cửa sổ 51 phần tử

          for (int end = windowSize; end <= statisticalEntities.size(); end++) {
            int start = end - windowSize; // bắt đầu của cửa sổ
            List<StatisticalEntity> window = statisticalEntities.subList(start, end);

            // Lấy list result để tính toán
            List<Integer> input = window.stream().map(StatisticalEntity::getResult).toList();

            Map<Integer, Set<Integer>> result = new HashMap<>();

            int current = input.get(0);
            int count = 1;

            for (int i = 1; i < input.size(); i++) {
              if (input.get(i).equals(current)) {
                count++;
              } else {
                result.computeIfAbsent(current, k -> new TreeSet<>()).add(count);
                current = input.get(i);
                count = 1;
              }
            }
            // lưu đoạn cuối cùng
            result.computeIfAbsent(current, k -> new TreeSet<>()).add(count);

            // Build chuỗi length
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, Set<Integer>> entry : result.entrySet()) {
              String prefix = entry.getKey() == 0 ? "X: " : "T: ";
              String values =
                  entry.getValue().stream().map(String::valueOf).collect(Collectors.joining("-"));

              if (sb.length() > 0) {
                sb.append(" | ");
              }
              sb.append(prefix).append(values);
            }

            // 🟢 cập nhật vào entity kết thúc window (bản ghi thứ end)

            StatisticalEntity targetEntity = statisticalEntities.get(end - 1);
            targetEntity.setLength(sb.toString());
            statisticalRepository.save(targetEntity);
          }
        });
  }

  // win/lose
  public void calculatorWinOrLose(List<Integer> ytd) {

    statisticalRepository.updateNull();

    ytd.forEach(
        date -> {
          List<StatisticalEntity> statisticalEntities = statisticalRepository.findAllByDate(date);

          for (int i = 50; i < statisticalEntities.size() - 1; i++) {
            // Ket qua hien tai
            int currentResult = statisticalEntities.get(i - 1).getResult();
            // Số phần tử liên tiếp phía trước có cùng result
            int totalEqResult = countEqResult(statisticalEntities, i);

            int count = 0;
            // Lấy 100 bản ghi trươc đó
            for (int j = i - 50; j < i - totalEqResult; j++) {

              boolean isSequence = true;

              // Kiểm tra xem có x giá trị liên tiếp là y không
              for (int k = 0; k < totalEqResult; k++) {
                if (statisticalEntities.get(j + k).getResult() != currentResult) {
                  isSequence = false;
                  break;
                }
              }

              // Nếu tìm thấy chuỗi liên tiếp, kiểm tra giá trị tiếp theo
              if (isSequence) {
                int nextValue =
                    statisticalEntities
                        .get(j + totalEqResult)
                        .getResult(); // Giá trị thứ x+1 sau chuỗi liên tiếp
                if (nextValue == 0) {
                  count -= 1; // Nếu là 0, trừ z đi 1
                } else if (nextValue == 1) {
                  count += 1; // Nếu là 1, cộng z thêm 1
                }
              }
            }

            if (count >= 1 && count < 6) {
              statisticalEntities.get(i).setEqResult(count);
              if (statisticalEntities.get(i).getResult() == 0) {
                statisticalEntities.get(i).setWin(1);
              } else {
                statisticalEntities.get(i).setLose(1);
              }
            }
            if (count <= -1 && count > -6) {
              statisticalEntities.get(i).setEqResult(count);
              if (statisticalEntities.get(i).getResult() == 1) {
                statisticalEntities.get(i).setWin(1);
              } else {
                statisticalEntities.get(i).setLose(1);
              }
            }
          }
          statisticalRepository.saveAll(statisticalEntities);
        });
  }

  private Integer countEqResult(List<StatisticalEntity> statisticalEntities, int i) {
    int currentResult = statisticalEntities.get(i - 1).getResult();
    int countEqResult = 1;
    // Đếm số phần tử liên tiếp phía trước có cùng result
    for (int j = i - 2; j >= 0; j--) {
      if (statisticalEntities.get(j).getResult() == currentResult) {
        countEqResult++;
      } else {
        break; // Ngừng đếm khi gặp phần tử khác result
      }
    }
    return countEqResult;
  }
}
