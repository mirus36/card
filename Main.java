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

public class Main {

    private static final Color WHITE_COLOR = new Color(255, 255, 255);
    private static final String ROOT = Main.class.getClassLoader().getResource("").getPath() + "Mask/";

    private static final String[] LITERS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "K", "Q", "A"};
    private static final Map<String, BufferedImage> LITER_MASKS = new HashMap<>();
    static {
        for (String name : LITERS) {
            try {
                LITER_MASKS.put(name, loadImage(ROOT + name + ".png"));
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


    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Неправильное колличество аргументов");
            return;
        }
        String imagesDirectory = args[0];
        List<String> imagesList = fileList(imagesDirectory);
        StringBuffer out = new StringBuffer();
        for (String imagePath : imagesList) {
            out.append(imagePath).append(" - ");
            BufferedImage image = loadImage(imagePath);
            List<Rectangle> whiteBorderedRectangles = getWhiteBorderedRectangles(image);
            for (Rectangle rectangle : whiteBorderedRectangles) {
                String letter = evalLetter(rectangle);
                String rank = evalRank(rectangle);
                out.append(letter).append(rank);
            }
            System.out.println(out);
            out.setLength(0);
        }
    }


    private static String evalRank(Rectangle rectangle) throws IOException {
        int maxX = 0, maxY = 0;

        for (int y = rectangle.height - 1; y > 0; y--) {
            boolean rowIsWhite = true;
            for (int x = rectangle.width -1; x > 0; x--) {
                if (rectangle.rgbArray[x][y] != WHITE_COLOR.getRGB()) {
                    rowIsWhite = false;
                    break;
                }
            }
            if (!rowIsWhite) {
                maxY = y;
                break;
            }
        }

        for (int x = rectangle.width - 1; x > 0; x--) {
            boolean colIsWhite = true;
            for (int y = rectangle.height -1; y > 0; y--) {
                if (rectangle.rgbArray[x][y] != WHITE_COLOR.getRGB()) {
                    colIsWhite = false;
                    break;
                }
            }
            if (!colIsWhite) {
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

        String imgType = null;
        outerLoop:
        {
            for (Map.Entry<String, BufferedImage> entry : RANKS_MASKS.entrySet()) {
                boolean maskIsOk = true;
                innerLoop:
                {
                    for (int y = 0; y < 30; y++) {
                        for (int x = 0; x < 30; x++) {
                            if (image.getRGB(x, y) != WHITE_COLOR.getRGB() && entry.getValue().getRGB(x, y) == WHITE_COLOR.getRGB()) {
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


        if (imgType == null) {
            int minError = 0;
            String type = null;
            for (Map.Entry<String, BufferedImage> entry : RANKS_MASKS.entrySet()) {
                int error = 0;
                for (int y = 0; y < 30; y++) {
                    for (int x = 0; x < 30; x++) {
                        if ((image.getRGB(x, y) == WHITE_COLOR.getRGB() && entry.getValue().getRGB(x, y) != WHITE_COLOR.getRGB()) ||
                                (image.getRGB(x, y) != WHITE_COLOR.getRGB() && entry.getValue().getRGB(x, y) == WHITE_COLOR.getRGB())) {
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

            imgType = type;
        }

        return imgType;
    }

    private static String evalLetter(Rectangle rectangle) throws IOException {
        int minX = 0, minY = 0;

        for (int y = 0; y < rectangle.height; y++) {
            boolean rowIsWhite = true;
            for (int x = 0; x < rectangle.width; x++) {
                if (rectangle.rgbArray[x][y] != WHITE_COLOR.getRGB()) {
                    rowIsWhite = false;
                    break;
                }
            }
            if (!rowIsWhite) {
                minY = y;
                break;
            }
        }

        for (int x = 0; x < 25; x++) {
            boolean colIsWhite = true;
            for (int y = 0; y < 25; y++) {
                if (rectangle.rgbArray[x][y] != WHITE_COLOR.getRGB()) {
                    colIsWhite = false;
                    break;
                }
            }
            if (!colIsWhite) {
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

        String imgType = null;
        outerLoop:
        {
            for (Map.Entry<String, BufferedImage> entry : LITER_MASKS.entrySet()) {
                boolean maskIsOk = true;
                innerLoop:
                {
                    for (int y = 0; y < 25; y++) {
                        for (int x = 0; x < 25; x++) {
                            if (image.getRGB(x, y) != WHITE_COLOR.getRGB() && entry.getValue().getRGB(x, y) == WHITE_COLOR.getRGB()) {
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


        if (imgType == null) {
            int minError = 0;
            String type = null;
            for (Map.Entry<String, BufferedImage> entry : LITER_MASKS.entrySet()) {
                int error = 0;
                for (int y = 0; y < 25; y++) {
                    for (int x = 0; x < 25; x++) {
                        if ((image.getRGB(x, y) == WHITE_COLOR.getRGB() && entry.getValue().getRGB(x, y) != WHITE_COLOR.getRGB()) ||
                                (image.getRGB(x, y) != WHITE_COLOR.getRGB() && entry.getValue().getRGB(x, y) == WHITE_COLOR.getRGB())) {
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

            imgType = type;
        }

        return imgType;
    }


    private static List<String> fileList(String directory) throws IOException {
        List<String> fileNames = new ArrayList<>();
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory));
        for (Path path : directoryStream) {
            fileNames.add(path.toString());
        }
        return fileNames;
    }

    private static BufferedImage loadImage(String imagePath) throws IOException {
        return ImageIO.read(new File(imagePath));
    }

    private static List<Rectangle> getWhiteBorderedRectangles(BufferedImage image) {
        int height = image.getHeight();
        int width = image.getWidth();
        List<Rectangle> rectangles = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelRGB = image.getRGB(x, y);
                if (WHITE_COLOR.getRGB() == pixelRGB && !belongsRectangles(x, y, rectangles)) {
                    Rectangle rectangle = getRectangle(image, x, y, rectangles);
                    if (rectangle != null) {
                        rectangles.add(rectangle);
                    }
                }
            }
        }

        return rectangles;
    }

    private static class Rectangle {
        int x;
        int y;
        int width;
        int height;
        int[][] rgbArray;
    }

    private static Rectangle getRectangle(BufferedImage image, int startX, int startY, List<Rectangle> otherRectangles) {

        int height = image.getHeight(), width = image.getWidth();

        Rectangle imageRectangle = new Rectangle();
        imageRectangle.x = 0;
        imageRectangle.y = 0;
        imageRectangle.height = height;
        imageRectangle.width = width;

        int x = startX, y = startY;

        while (belongsRectangle(x + 1, y, imageRectangle) && isWhite(image, x + 1, y) && !belongsRectangles(x + 1, y, otherRectangles)) {
            x += 1;
        }

        while (belongsRectangle(x, y + 1, imageRectangle) && isWhite(image, x, y + 1)) {
            y += 1;
        }

        if (y - startY < 50 || x - startX < 50) {
            return null;
        }

        Rectangle result = new Rectangle();
        result.x = startX;
        result.y = startY;
        result.height = y - startY;
        result.width = x - startX;
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

    private static boolean isWhite(BufferedImage image, int x, int y) {
        return WHITE_COLOR.getRGB() == image.getRGB(x, y);
    }

    private static int findMaxIndex(int[] arr) {
        int max = arr[0];
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }


}
