package computableFutures;

import sequential.Filters;
import utils.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FiltersCompletableFuture extends Filters {

    public FiltersCompletableFuture(String filename) {
        super(filename);
    }

    public long[] HistogramFilter(String outputFile, int value) {
        Color[][] tmp = Utils.copyImage(image);
        int[] hist;
        int total_pixels = tmp.length * tmp[0].length;
        Runtime rt = Runtime.getRuntime();

        System.out.println("\nTotal pixels: " + total_pixels);
        int numTasks = Runtime.getRuntime().availableProcessors();

        System.out.println(numTasks);

        // === MEDIÇÃO DE TEMPO E MEMÓRIA ===
        rt.gc();
        long memBefore = rt.totalMemory() - rt.freeMemory();
        long start = System.nanoTime();

        hist = luminosityResultMatrixParallel(tmp);

        cumulativeHistogram(hist);

        createNewImageParallel(tmp, total_pixels, numTasks);

        long end = System.nanoTime();

        Utils.writeImage(tmp, outputFile);

        long memAfterHist = rt.totalMemory() - rt.freeMemory();

        // === RESULTADOS ===
        double memHistMB = (memAfterHist - memBefore) / (1024.0 * 1024.0);
        double timeMs = (end-start)/1e6;

        System.out.printf("Total time and mem usage (ComputableFutures):                  %.2fms | %.2f MB%n", timeMs, memHistMB);

        return new long[]{
                (end - start) / 1_000_000,
                (memAfterHist - memBefore)
        };
    }


    public int[] luminosityResultMatrixParallel(Color[][] tmp) {
        int numTasks = Runtime.getRuntime().availableProcessors();
        int partsPerThread = tmp.length / numTasks;

        List<CompletableFuture<int[]>> futures = new ArrayList<>();

        for (int t = 0; t < numTasks; t++) {
            int startRow = t * partsPerThread;
            int endRow = (t == numTasks - 1) ? tmp.length : (t + 1) * partsPerThread;

            futures.add(CompletableFuture.supplyAsync(() -> {
                int[] localHist = new int[256];
                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < tmp[i].length; j++) {
                        Color pixel = tmp[i][j];
                        int lum = computeLuminosity(pixel.getRed(), pixel.getGreen(), pixel.getBlue());
                        localHist[lum]++;
                    }
                }
                return localHist;
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        int[] finalHist = new int[256];
        for (CompletableFuture<int[]> f : futures) {
            int[] local = f.join();
            for (int i = 0; i < 256; i++) finalHist[i] += local[i];
        }
        return finalHist;
    }

    public void createNewImageParallel(Color[][] tmp, int total_pixels, int numTasks){

        int partsPerThread = tmp.length / numTasks;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for(int t = 0; t < numTasks; t++ ){
            int startRow = t * partsPerThread;
            int endRow = (t == numTasks - 1) ? tmp.length : (t + 1) * partsPerThread;

            futures.add(CompletableFuture.runAsync(() -> {

                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < tmp[i].length; j++) {
                        // fetches values of each pixel
                        Color pixel = tmp[i][j];
                        int r = pixel.getRed();
                        int g = pixel.getGreen();
                        int b = pixel.getBlue();
                        int lum = Filters.computeLuminosity(r, g, b);
                        //int newLum = 255*(cumulative[lum]/total_pixels);

                        double cdf = (double) cumulative[lum] / (double) (total_pixels - cdfMin);
                        int newLum = (int) Math.round(255.0 * cdf);

                        if (newLum < 0) newLum = 0;
                        if (newLum > 255) newLum = 255;
                        tmp[i][j] = new Color(newLum, newLum, newLum);
                    }
                }
            }
            ));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
