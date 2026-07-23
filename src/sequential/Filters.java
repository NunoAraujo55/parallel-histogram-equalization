package sequential;

import utils.Utils;

import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author Jorge Coelho
 * @contact jmn@isep.ipp.pt
 * @version 1.0
 */
public class Filters {

    String file;
    protected Color[][] image;
    protected int[] cumulative = new int[256];
    protected int cdfMin = 0;

    // Constructor with filename for source image
    public Filters(String filename) {
        this.file = filename;
        image = Utils.loadImage(filename);
    }


    public long[] HistogramFilter(String outputFile, int value) throws IOException, ExecutionException, InterruptedException {
        Color[][] tmp = Utils.copyImage(image);
        int[] hist;
        int total_pixels = tmp.length * tmp[0].length;
        Runtime rt = Runtime.getRuntime();

        System.out.println("\nTotal pixels: " + total_pixels);

        // === MEDIÇÃO DE TEMPO E MEMÓRIA ===
        rt.gc();
        long memBefore = rt.totalMemory() - rt.freeMemory();
        long start = System.nanoTime();

        hist = luminosityResultMatrix(tmp);

        cumulativeHistogram(hist);

        Color[][] newTmp = createNewImage(tmp, total_pixels);
        long end = System.nanoTime();
        Utils.writeImage(newTmp, outputFile);
        long memAfterHist = rt.totalMemory() - rt.freeMemory();

        // === RESULTADOS ===
        double memHistMB = (memAfterHist - memBefore) / (1024.0 * 1024.0);
        double timeMs = (end-start)/1e6;

        System.out.printf("Total time and mem usage (Sequential):                  %.2fms | %.2f MB%n", timeMs, memHistMB);

        return new long[]{
                (end - start) / 1_000_000,
                (memAfterHist - memBefore)
        };
    }

    public static int computeLuminosity(int r, int g, int b) {
        return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }

    public int[] luminosityResultMatrix(Color[][] tmp){

        int[] hist = new int[256];
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {

                // fetches values of each pixel
                Color pixel = tmp[i][j];
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                int lum = computeLuminosity(r, g, b);
                hist[lum]++;
            }
        }
        return hist;
    }

    public void cumulativeHistogram(int[] hist){
        cumulative[0] = hist[0];
        for (int i = 1; i < 256; i++) {
            cumulative[i] = cumulative[i - 1] + hist[i];
        }
        for (int i = 0; i < 256; i++) {
            if (cumulative[i] != 0) {
                cdfMin = cumulative[i];
                break;
            }
        }
    }

    public Color[][] createNewImage(Color[][] tmp, int total_pixels){
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {

                // fetches values of each pixel
                Color pixel = tmp[i][j];
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                int lum = computeLuminosity(r, g, b);
                //int newLum = 255*(cumulative[lum]/total_pixels);

                double cdf = (double) cumulative[lum] / (double) (total_pixels - cdfMin);
                int newLum = (int) Math.round(255.0 * cdf);

                if (newLum < 0) newLum = 0;
                if (newLum > 255) newLum = 255;
                tmp[i][j] = new Color(newLum, newLum, newLum);
            }
        }
        return tmp;
    }
}
