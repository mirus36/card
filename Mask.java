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
import java.util.List;

public class Mask {

    private static final Color WHITE_COLOR = new Color(255, 255, 255);
    private static final Color BLACK_COLOR = new Color(0, 0, 0);
    private static String DIRS[] = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "K", "Q", "A"};
    private static String RANKS_DIRS[] = {"h", "s", "c", "d"};


    public static void main(String[] args) throws IOException {
        String root = args[0];
        for (String dir : DIRS) {
            build(25, root, dir);
        }

        for (String dir : RANKS_DIRS) {
            build(30, root, dir);
        }
    }

    private static void build (int size, String root, String dir) throws IOException {
        int[][] matrix = new int[size][size];


        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                matrix[x][y] = WHITE_COLOR.getRGB();
            }
        }

        List<String> files = fileList(root + dir);
        for (String file : files) {
            BufferedImage image = loadImage(file);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (image.getRGB(x, y) != WHITE_COLOR.getRGB()) {
                        matrix[x][y] = BLACK_COLOR.getRGB();
                    }
                }
            }

        }

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (matrix[x][y] != 0) {
                    image.setRGB(x, y, matrix[x][y]);
                }
            }
        }

        File file = new File("/home/mirus36/Pictures/Result/" + dir + ".png");
        ImageIO.write(image, "png", file);

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

}
