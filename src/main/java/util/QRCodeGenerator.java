package util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;

public class QRCodeGenerator {

    public static void generateQRCode(String url, String filePath) {
        int width = 300;
        int height = 300;
        String imageFormat = "png";

        // 设置二维码参数
        EncodeHintType hintType = EncodeHintType.ERROR_CORRECTION;
        ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
        LogUtil.logInfo("url:%s; filePath:%s", url, filePath);
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, width, height,
                    Collections.singletonMap(hintType, errorCorrectionLevel));

            // 将BitMatrix转换为图像文件
            Path path = Paths.get(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, imageFormat, path);

            System.out.println("QR code saved at: " + path.toAbsolutePath());
        } catch (WriterException | IOException e) {
            LogUtil.logError("generate qrcode error :%s", e.getMessage());
        }
    }

    public static class MatrixToImageWriter {
        private static final int BLACK = 0xFF000000;
        private static final int WHITE = 0xFFFFFFFF;

        private MatrixToImageWriter() {
        }

        public static void writeToPath(BitMatrix matrix, String format, Path path) throws IOException {
            BufferedImage image = toBufferedImage(matrix);
            ImageIO.write(image, format, path.toFile());
        }

        public static BufferedImage toBufferedImage(BitMatrix matrix) {
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
                }
            }
            return image;
        }

        public static String toBase64String(BitMatrix matrix, String format) throws IOException {
            BufferedImage image = toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, format, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }

}
