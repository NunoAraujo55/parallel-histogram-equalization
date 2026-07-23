package threadPools;

import sequential.Filters;
import utils.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class FiltersMultiThreadWithThreadPools extends Filters {
    int numThreads;
    public ExecutorService executorService;

    // Constructor with filename for source image
    public FiltersMultiThreadWithThreadPools(String filename, int numThreads) {
        super(filename);
        this.numThreads= numThreads;
        executorService = Executors.newFixedThreadPool(numThreads);
    }

    public long[] HistogramFilter(String outputFile, int value) throws ExecutionException, InterruptedException {
        Color[][] tmp = Utils.copyImage(image);
        int[] hist;
        int total_pixels = tmp.length * tmp[0].length;
        Runtime rt = Runtime.getRuntime();

        System.out.println("\nTotal pixels: " + total_pixels);

        // === MEDIÇÃO DE TEMPO E MEMÓRIA ===
        rt.gc();
        long memBefore = rt.totalMemory() - rt.freeMemory();
        long start = System.nanoTime();

        hist = luminosityResultMatrixParallel(tmp, numThreads);

        cumulativeHistogram(hist);

        Color[][] newTmp = createNewImageParallel(tmp, total_pixels, numThreads);

        long end = System.nanoTime();

        Utils.writeImage(newTmp, outputFile);

        long memAfterHist = rt.totalMemory() - rt.freeMemory();

        // === RESULTADOS ===
        double memHistMB = (memAfterHist - memBefore) / (1024.0 * 1024.0);
        double timeMs = (end-start)/1e6;

        System.out.printf("Total time and mem usage (WithThreadPools):                  %.2fms | %.2f MB%n", timeMs, memHistMB);

        return new long[]{
                (end - start) / 1_000_000,
                (memAfterHist - memBefore)
        };
    }

    public int[] luminosityResultMatrixParallel(Color[][] tmp, int numThreads) throws InterruptedException, ExecutionException {
        int[] finalHist = new int[256];
        int partsPerThread = tmp.length / numThreads;
        List<Future<int[]>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            int startRow = t * partsPerThread;
            int endRow = (t == numThreads - 1) ? tmp.length : (t + 1) * partsPerThread;

            futures.add(executorService.submit(new HistogramTask(tmp, startRow, endRow)));
        }

        for (Future<int[]> f : futures) {
            int[] localHist = f.get();
            for(int i = 0; i < 256; i++){
                finalHist[i] += localHist[i];
            }
        }

        return finalHist;
    }

    public Color[][] createNewImageParallel(Color[][] tmp, int total_pixels, int numThreads) throws InterruptedException {
        int partsPerThread = tmp.length / numThreads;
        List<Future<Color[][]>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            int startRow = t * partsPerThread;
            int endRow = (t == numThreads - 1) ? tmp.length : (t + 1) * partsPerThread;

            futures.add(executorService.submit(new ImageWriterTask(tmp, startRow, endRow, total_pixels, this.cumulative, this.cdfMin)));
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        return tmp;
    }
}


