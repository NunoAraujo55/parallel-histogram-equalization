package forkJoinPool;

import sequential.Filters;
import utils.Utils;

import java.util.concurrent.ExecutionException;

import java.awt.Color;
import java.util.concurrent.*;


public class FiltersForkJoinPool extends Filters {
    public ForkJoinPool forkJoinPool;

    // Constructor with filename for source image
    public FiltersForkJoinPool(String filename) {
        super(filename);
        forkJoinPool = new ForkJoinPool();
    }

    public long[] HistogramFilter(String outputFile, int value) throws ExecutionException, InterruptedException {
        Color[][] tmp = Utils.copyImage(image);
        int[] hist;
        int total_pixels = tmp.length * tmp[0].length;
        Runtime rt = Runtime.getRuntime();

        System.out.println("\nTotal pixels: " + total_pixels);

        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("\nTotal threads: " + (numThreads - 1));
        // === MEDIÇÃO DE TEMPO E MEMÓRIA ===

        long memBefore = rt.totalMemory() - rt.freeMemory();
        long start = System.nanoTime();

        hist = luminosityResultMatrixParallel(tmp);

        cumulativeHistogram(hist);

        createNewImageParallel(tmp, total_pixels);

        long end = System.nanoTime();

        Utils.writeImage(tmp, outputFile);

        long memAfterHist = rt.totalMemory() - rt.freeMemory();

        // === RESULTADOS ===
        double memHistMB = (memAfterHist - memBefore) / (1024.0 * 1024.0);
        double timeMs = (end-start)/1e6;

        System.out.printf("Total time and mem usage (ForkJointPool):                  %.2fms | %.2f MB%n", timeMs, memHistMB);

        return new long[]{
                (end - start) / 1_000_000,
                (memAfterHist - memBefore)
        };
    }

    public int[] luminosityResultMatrixParallel(Color[][] tmp) {
        HistogramFork task = new HistogramFork(tmp, 0, tmp.length);
        return forkJoinPool.invoke(task);
    }

    public void createNewImageParallel(Color[][] tmp, int total_pixels) {
        ImageWriterFork task = new ImageWriterFork(tmp, 0, tmp.length, total_pixels, cumulative, cdfMin);
        forkJoinPool.invoke(task);
    }
}



