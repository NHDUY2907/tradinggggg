package com.example.demo.service;

import com.example.demo.data.entity.StatisticalEntity;
import com.example.demo.data.repository.StatisticalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScreenCaptureService {

  private final TelegramService telegramService;
  private final StatisticalRepository statisticalRepository;

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

      public static void main(String[] args) throws Exception {
        while (true) {
          PointerInfo a = MouseInfo.getPointerInfo();
          Point p = a.getLocation();

          System.out.println("X: " + p.x + " | Y: " + p.y);

          Thread.sleep(5000); // 0.5s in 1 lần
        }
      }

  private Robot robot; // lazy init
  private static final String SAVE_DIR = "C:\\Users\\T9 Plus\\Desktop\\Capture\\";

  // Lazy init Robot
  private Robot getRobot() throws AWTException {
    if (robot == null) {
      // bỏ check headless vì chạy trên desktop active
      robot = new Robot();
    }
    return robot;
  }

  private GraphicsDevice getScreenDevice(int x, int y) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] devices = ge.getScreenDevices();

    for (GraphicsDevice device : devices) {
      Rectangle bounds = device.getDefaultConfiguration().getBounds();
      if (bounds.contains(x, y)) {
        return device;
      }
    }

    return ge.getDefaultScreenDevice(); // fallback
  }

  private BufferedImage cropCircle(BufferedImage src) {
    int diameter = Math.min(src.getWidth(), src.getHeight());

    BufferedImage mask = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = mask.createGraphics();

    // bật anti-alias để mịn
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // vẽ hình tròn
    g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, diameter, diameter));

    // vẽ ảnh vào vùng clip
    g2.drawImage(src, 0, 0, diameter, diameter, null);

    g2.dispose();

    return mask;
  }

  // Thực hiện chụp ảnh và so sánh
  public int captureCircleCenter(int centerX, int centerY, int radius) throws Exception {

    int diameter = radius * 2;

    int x = centerX - radius;
    int y = centerY - radius;

    GraphicsDevice device = getScreenDevice(x, y);
    Robot r = new Robot(device);

    Rectangle rect = new Rectangle(x, y, diameter, diameter);
    BufferedImage image = r.createScreenCapture(rect);

    BufferedImage circleImage = cropCircle(image);

    return detectBlackOrWhite(circleImage);
  }

  public void testImage(int centerX, int centerY, int radius) throws Exception {

    int diameter = radius * 2;

    int x = centerX - radius;
    int y = centerY - radius;

    GraphicsDevice device = getScreenDevice(x, y);
    Robot r = new Robot(device);

    Rectangle rect = new Rectangle(x, y, diameter, diameter);

    BufferedImage image = r.createScreenCapture(rect);

    BufferedImage circleImage = cropCircle(image);

    // 🔥 lưu ảnh sau crop
    File file2 = new File("C:\\Users\\T9 Plus\\Desktop\\Capture\\debug_circle.png");
    ImageIO.write(circleImage, "png", file2);
  }

  private BufferedImage loadImage(String path) throws Exception {
    return ImageIO.read(new File(path));
  }

  private double compareImages(BufferedImage img1, BufferedImage img2) {
    int width = Math.min(img1.getWidth(), img2.getWidth());
    int height = Math.min(img1.getHeight(), img2.getHeight());

    long diff = 0;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int rgb1 = img1.getRGB(x, y);
        int rgb2 = img2.getRGB(x, y);

        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = rgb1 & 0xff;

        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >> 8) & 0xff;
        int b2 = rgb2 & 0xff;

        diff += Math.abs(r1 - r2);
        diff += Math.abs(g1 - g2);
        diff += Math.abs(b1 - b2);
      }
    }

    double maxDiff = 3L * 255 * width * height;
    return diff / maxDiff; // càng nhỏ càng giống
  }

  public int detectBlackOrWhite(BufferedImage captured) throws Exception {

    BufferedImage den = loadImage("C:\\Users\\T9 Plus\\Desktop\\Capture\\den.png");
    BufferedImage trang = loadImage("C:\\Users\\T9 Plus\\Desktop\\Capture\\trang.png");

    double diffDen = compareImages(captured, den);
    double diffTrang = compareImages(captured, trang);

    int result = (diffDen < diffTrang) ? 1 : 0;

    invalidCoordinates(diffDen, diffTrang);

    int today = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

    StatisticalEntity statisticalEntity = insertEntity(result, today);

    // Tinh lech tren duoi
    setData(today, statisticalEntity);

    if (Objects.nonNull(statisticalEntity.getLechTren())
        && Objects.nonNull(statisticalEntity.getLechDuoi())
        && statisticalEntity.getLechTren() <= 6
        && statisticalEntity.getLechDuoi() <= 12
        && Objects.nonNull(statisticalEntity.getEqResult())) {
      String nextAction;
      if (statisticalEntity.getEqResult() > 0) {
        nextAction = "X";
      } else {
        nextAction = "T";
      }

      if (Objects.nonNull(statisticalEntity.getLength())
          && STRONG.contains(statisticalEntity.getLength())) {
        telegramService.sendMessage(
            "📊 <b>STRONG</b> | 🔔 "
                + statisticalEntity.getEqResult()
                + "\n\n"
                + "📌 <b>Tín hiệu:</b> "
                + statisticalEntity.getLength()
                + "\n"
                + "📌 <b>Tín hiệu:</b> "
                + "LT: "
                + statisticalEntity.getLechTren()
                + " | LD: "
                + statisticalEntity.getLechDuoi()
                + "\n"
                + "👉 <b>Lệnh tiếp theo: </b> "
                + nextAction);
      }
      if (Objects.nonNull(statisticalEntity.getLength())
          && MEDIUM.contains(statisticalEntity.getLength())) {
        telegramService.sendMessage(
            "📊 <b>MEDIUM</b> | 🔔 "
                + statisticalEntity.getEqResult()
                + "\n\n"
                + "📌 <b>Tín hiệu:</b> "
                + statisticalEntity.getLength()
                + "\n"
                + "📌 <b>Tín hiệu:</b> "
                + "LT: "
                + statisticalEntity.getLechTren()
                + " | LD: "
                + statisticalEntity.getLechDuoi()
                + "\n"
                + "👉 <b>Lệnh tiếp theo: </b> "
                + nextAction);
      }

      if (Objects.nonNull(statisticalEntity.getLength())
          && WEAK.contains(statisticalEntity.getLength())) {
        telegramService.sendMessage(
            "📊 <b>WEAK</b> | 🔔 "
                + statisticalEntity.getEqResult()
                + "\n\n"
                + "📌 <b>Tín hiệu:</b> "
                + statisticalEntity.getLength()
                + "\n"
                + "📌 <b>Tín hiệu:</b> "
                + "LT: "
                + statisticalEntity.getLechTren()
                + " | LD: "
                + statisticalEntity.getLechDuoi()
                + "\n"
                + "👉 <b>Lệnh tiếp theo: </b> "
                + nextAction);
      }
    }

    return result;
  }

  private void setData(int today, StatisticalEntity statisticalEntity) {
    List<StatisticalEntity> subListLD =
        statisticalRepository.findTop100ByDateOrderByStatisticalIdDesc(today);

    if (subListLD.size() < 100) {
      return;
    }
    int lechDuoi = 0;
    int t = (int) subListLD.stream().filter(item -> item.getResult() == 1).count();
    int x = (int) subListLD.stream().filter(item -> item.getResult() == 0).count();
    if (t > x) {
      lechDuoi = t - x;
    } else if (t < x) {
      lechDuoi = x - t;
    }
    statisticalEntity.setLechDuoi(lechDuoi);

    // Tinh lech tren
    List<StatisticalEntity> subList = new ArrayList<>();
    List<StatisticalEntity> subListV1 =
        statisticalRepository.findTop100ByDateOrderByStatisticalIdDesc(today).stream()
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
    int t1 = (int) subList.stream().filter(item -> item.getResult() == 1).count();
    int x1 = (int) subList.stream().filter(item -> item.getResult() == 0).count();
    if (t1 > x1) {
      lechTren = t1 - x1;
    } else if (t1 < x1) {
      lechTren = x1 - t1;
    }
    statisticalEntity.setLechTren(lechTren);

    statisticalRepository.save(statisticalEntity);
  }

  private Integer countEqResult(List<StatisticalEntity> statisticalEntities) {
    int currentResult = statisticalEntities.get(0).getResult();
    int countEqResult = 1;
    // Đếm số phần tử liên tiếp phía trước có cùng result
    for (int j = 1; j < statisticalEntities.size(); j++) {
      if (statisticalEntities.get(j).getResult() == currentResult) {
        countEqResult++;
      } else {
        break; // Ngừng đếm khi gặp phần tử khác result
      }
    }
    return countEqResult;
  }

  private StatisticalEntity insertEntity(int result, int today) {

    StatisticalEntity statisticalEntity = new StatisticalEntity();
    statisticalEntity.setDate(today);
    statisticalEntity.setResult(result);
    statisticalRepository.save(statisticalEntity);

    List<StatisticalEntity> list =
        statisticalRepository.findTop50ByDateOrderByStatisticalIdDesc(today);

    if (list.size() == 50) {
      // Ket qua hien tai
      int currentResult = list.get(0).getResult();
      // Số phần tử liên tiếp phía trước có cùng result
      int totalEqResult = countEqResult(list);

      int count = 0;
      // Lấy 100 bản ghi trươc đó
      for (int j = 49; j >= totalEqResult; j--) {

        boolean isSequence = true;

        // Kiểm tra xem có x giá trị liên tiếp là y không
        for (int k = 0; k < totalEqResult; k++) {
          if (list.get(j - k).getResult() != currentResult) {
            isSequence = false;
            break;
          }
        }

        // Nếu tìm thấy chuỗi liên tiếp, kiểm tra giá trị tiếp theo
        if (isSequence) {
          int nextValue =
              list.get(j - totalEqResult).getResult(); // Giá trị thứ x+1 sau chuỗi liên tiếp
          if (nextValue == 0) {
            count -= 1; // Nếu là 0, trừ z đi 1
          } else if (nextValue == 1) {
            count += 1; // Nếu là 1, cộng z thêm 1
          }
        }
      }

      if (count >= 1 && count < 6) {
        statisticalEntity.setEqResult(count);
      }
      if (count <= -1 && count > -6) {
        statisticalEntity.setEqResult(count);
      }

      // đảo lại để đúng thứ tự thời gian
      Collections.reverse(list);
      // lấy result
      List<Integer> input = list.stream().map(StatisticalEntity::getResult).toList();

      Map<Integer, Set<Integer>> resultMap = new HashMap<>();

      int current = input.get(0);
      int count1 = 1;

      for (int i = 1; i < input.size(); i++) {
        if (input.get(i).equals(current)) {
          count1++;
        } else {
          resultMap.computeIfAbsent(current, k -> new TreeSet<>()).add(count1);
          current = input.get(i);
          count1 = 1;
        }
      }

      // đoạn cuối
      resultMap.computeIfAbsent(current, k -> new TreeSet<>()).add(count1);

      // build string
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<Integer, Set<Integer>> entry : resultMap.entrySet()) {
        String prefix = entry.getKey() == 0 ? "X: " : "T: ";
        String values =
            entry.getValue().stream().map(String::valueOf).collect(Collectors.joining("-"));

        if (!sb.isEmpty()) {
          sb.append(" | ");
        }
        sb.append(prefix).append(values);
      }

      // set vào record vừa insert
      statisticalEntity.setLength(sb.toString());
    }
    return statisticalRepository.save(statisticalEntity);
  }

  private void invalidCoordinates(double diffDen, double diffTrang) {
    double thresholdInvalid = 0.15;

    boolean invalidPosition = diffDen > thresholdInvalid && diffTrang > thresholdInvalid;

    if (invalidPosition) {
      String message =
          String.format(
              "⚠️ INVALID CAPTURE\n\n"
                  + "📊 diffDen: %.2f | diffTrang: %.2f\n"
                  + "⚠️ Khả năng cao sai tọa độ hoặc lệch vùng capture",
              diffDen, diffTrang);
      telegramService.sendMessage(message);
    }
  }
}
