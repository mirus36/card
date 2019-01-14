import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Main {
    private static final Color WHITE_COLOR = new Color(255, 255, 255);
    private static final Color GREY_COLOR = new Color(120, 120, 120);
    private static final String ROOT = Main.class.getClassLoader().getResource("").getPath() + "Mask/";

    private static final String[] LITERS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "K", "Q", "A"};
    private static final Map<String, BufferedImage> SIGN_MASKS = new HashMap<>();
    static {
        for (String name : LITERS) {
            try {
                SIGN_MASKS.put(name, loadImage(ROOT + name + ".png"));
            } catch (IOException e) {
                System.exit(1);
            }
        }
    }

    private static final String[] RANKS = {"h", "s", "c", "d"};
    private static final Map<String, BufferedImage> RANKS_MASKS = new HashMap<>();
    static {
        for (String name : RANKS) {
            try {
                RANKS_MASKS.put(name, loadImage(ROOT + name + ".png"));
            } catch (IOException e) {
                System.exit(1);
            }
        }
    }

    static class Rectangle {
        int x;
        int y;
        int width;
        int height;
        int[][] rgbArray;

        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Неправильное колличество аргументов");
            return;
        }
        String imagesDirectory = args[0];
        List<String> imagesList = fileList(imagesDirectory);
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        for (String imagePath : imagesList) {
            queue.add(new CardEvaluator(imagePath));
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() + 1,
                Runtime.getRuntime().availableProcessors() + 1,
                0L,
                TimeUnit.MILLISECONDS,
                queue
        );
        executor.prestartAllCoreThreads();

        while (true) {
            Thread.sleep(500);
            if (queue.isEmpty()) {
                executor.shutdown();
                return;
            }
        }
    }

    static String evalRank(Rectangle rectangle) throws IOException {
        int maxX = 0, maxY = 0;

        for (int y = rectangle.height - 1; y > 0; y--) {
            boolean rowIsRear = true;
            for (int x = rectangle.width -1; x > 0; x--) {
                if (!isRearColor(rectangle.rgbArray[x][y])) {
                    rowIsRear = false;
                    break;
                }
            }
            if (!rowIsRear) {
                maxY = y;
                break;
            }
        }

        for (int x = rectangle.width - 1; x > 0; x--) {
            boolean colIsRear = true;
            for (int y = rectangle.height -1; y > 0; y--) {
                if (!isRearColor(rectangle.rgbArray[x][y])) {
                    colIsRear = false;
                    break;
                }
            }
            if (!colIsRear) {
                maxX = x;
                break;
            }
        }

        BufferedImage image = new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB);
        for (int y = maxY - 30; y < maxY; y++) {
            for (int x = maxX - 30; x < maxX; x++) {
                image.setRGB(x - maxX + 30, y - maxY + 30, rectangle.rgbArray[x][y]);
            }
        }

        String imgType = evalBestMatch(image, RANKS_MASKS);
        if (imgType == null) {
            imgType = evalFullMatch(image, RANKS_MASKS);
        }

        return imgType;
    }

    static String evalLetter(Rectangle rectangle) throws IOException {
        int minX = 0, minY = 0;

        for (int y = 0; y < rectangle.height; y++) {
            boolean rowIsRear = true;
            for (int x = 0; x < rectangle.width; x++) {
                if (!isRearColor(rectangle.rgbArray[x][y])) {
                    rowIsRear = false;
                    break;
                }
            }
            if (!rowIsRear) {
                minY = y;
                break;
            }
        }

        for (int x = 0; x < 25; x++) {
            boolean colIsRear = true;
            for (int y = 0; y < 25; y++) {
                if (!isRearColor(rectangle.rgbArray[x][y])) {
                    colIsRear = false;
                    break;
                }
            }
            if (!colIsRear) {
                minX = x;
                break;
            }
        }

        BufferedImage image = new BufferedImage(25, 25, BufferedImage.TYPE_INT_RGB);
        for (int y = minY; y < minY + 25; y++) {
            for (int x = minX; x < minX + 25; x++) {
                image.setRGB(x - minX, y - minY, rectangle.rgbArray[x][y]);
            }
        }

        String imgType = evalBestMatch(image, SIGN_MASKS);
        if (imgType == null) {
            imgType = evalFullMatch(image, SIGN_MASKS);
        }

        return imgType;
    }

    private static String evalBestMatch(BufferedImage image, Map<String, BufferedImage> masks) {
        String imgType = null;
        outerLoop:
        {
            for (Map.Entry<String, BufferedImage> entry : masks.entrySet()) {
                boolean maskIsOk = true;
                innerLoop:
                {
                    for (int y = 0; y <  entry.getValue().getHeight(); y++) {
                        for (int x = 0; x < entry.getValue().getWidth(); x++) {
                            if (!isRearColor(image.getRGB(x, y)) && isWhite(entry.getValue().getRGB(x, y))) {
                                maskIsOk = false;
                                break innerLoop;
                            }
                        }
                    }
                }
                if (maskIsOk) {
                    imgType = entry.getKey();
                    break outerLoop;
                }
            }
        }

        return imgType;
    }

    private static String evalFullMatch(BufferedImage image, Map<String, BufferedImage> masks) {
        int minError = 0;
        String type = null;
        for (Map.Entry<String, BufferedImage> entry : masks.entrySet()) {
            int error = 0;
            for (int y = 0; y < entry.getValue().getHeight(); y++) {
                for (int x = 0; x < entry.getValue().getWidth(); x++) {
                    if ((isRearColor(image.getRGB(x, y)) && !isWhite(entry.getValue().getRGB(x, y))) || (!(isRearColor(image.getRGB(x, y)) && isWhite(entry.getValue().getRGB(x, y))))) {
                        error++;
                    }
                }
            }
            if (type == null) {
                type = entry.getKey();
                minError = error;
            } else {
                if (error < minError) {
                    minError = error;
                    type = entry.getKey();
                }
            }
        }

        return type;
    }

    private static List<String> fileList(String directory) throws IOException {
        List<String> fileNames = new ArrayList<>();
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory));
        for (Path path : directoryStream) {
            fileNames.add(path.toString());
        }
        return fileNames;
    }

    static BufferedImage loadImage(String imagePath) throws IOException {
        return ImageIO.read(new File(imagePath));
    }

    static List<Rectangle> getCardRectangles(BufferedImage image) {
        int height = image.getHeight();
        int width = image.getWidth();
        List<Rectangle> rectangles = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isWhite(image.getRGB(x, y)) && !belongsRectangles(x, y, rectangles)) {
                    Rectangle rectangle = getRectangle(image, x, y, WHITE_COLOR.getRGB(), rectangles);
                    if (rectangle != null) {
                        rectangles.add(rectangle);
                    }
                }
                if (isGrey(image.getRGB(x, y)) && !belongsRectangles(x, y, rectangles)) {
                    Rectangle rectangle = getRectangle(image, x, y, GREY_COLOR.getRGB(), rectangles);
                    if (rectangle != null) {
                        rectangles.add(rectangle);
                    }
                }
            }
        }

        return rectangles;
    }

    private static Rectangle getRectangle(BufferedImage image, int startX, int startY, int color, List<Rectangle> otherRectangles) {
        Rectangle imageRectangle = new Rectangle(0, 0, image.getWidth(), image.getHeight());
        int x = startX, y = startY;

        while (belongsRectangle(x + 1, y, imageRectangle) && image.getRGB(x + 1, y) == color && !belongsRectangles(x + 1, y, otherRectangles)) {
            x += 1;
        }

        while (belongsRectangle(x, y + 1, imageRectangle) && image.getRGB(x, y +1) == color) {
            y += 1;
        }

        if (y - startY < 50 || x - startX < 50) {
            return null;
        }

        Rectangle result = new Rectangle(startX, startY, x - startX, y - startY);
        result.rgbArray = new int[result.width][result.height];
        for (int i = 0; i < result.width; i++) {
            for (int j = 0; j < result.height; j++) {
                result.rgbArray[i][j] = image.getRGB(i + result.x, j + result.y);
            }
        }

        return result;
    }

    private static boolean belongsRectangles(int x, int y, List<Rectangle> rectangles) {
        for (Rectangle rectangle : rectangles) {
            if (belongsRectangle(x, y, rectangle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean belongsRectangle(int x, int y, Rectangle rectangle) {
        return x >= rectangle.x && x <= rectangle.x + rectangle.width && y >= rectangle.y && y <= rectangle.y + rectangle.height;
    }

    private static boolean isWhite(int rgb) {
        return WHITE_COLOR.getRGB() == rgb;
    }

    private static boolean isGrey(int rgb) {
        return GREY_COLOR.getRGB() == rgb;
    }

    private static boolean isRearColor(int rgb) {
        return GREY_COLOR.getRGB() == rgb || WHITE_COLOR.getRGB() == rgb;
    }
}

class CardEvaluator implements Runnable{

    private String imagePath;

    CardEvaluator(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public void run() {
        try {
            StringBuilder out = new StringBuilder();
            out.append(imagePath).append(" - ");
            BufferedImage image = Main.loadImage(imagePath);

            List<Main.Rectangle> whiteBorderedRectangles = Main.getCardRectangles(image);
            for (Main.Rectangle rectangle : whiteBorderedRectangles) {
                String letter = Main.evalLetter(rectangle);
                String rank = Main.evalRank(rectangle);
                out.append(letter).append(rank);
            }
            System.out.println(out);
            out.setLength(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
