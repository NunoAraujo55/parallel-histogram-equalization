package withoutThreadPools;

import sequential.Filters;
import utils.Utils;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class FiltersMultiThreadNoPools extends Filters {
    int numThreads;

    // Constructor with filename for source image
    public FiltersMultiThreadNoPools(String filename, int numThreads) {
        super(filename);
        this.numThreads = numThreads;
    }

    public long[] HistogramFilter(String outputFile, int value) throws IOException {
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

        System.out.printf("Total time and mem usage (NoThreadPools):                  %.2fms | %.2f MB%n", timeMs, memHistMB);

        return new long[]{
                (end - start) / 1_000_000,
                (memAfterHist - memBefore)
        };
    }

    public int[] luminosityResultMatrixParallelLocal(Color[][] tmp, int numThreads){
        int[] finalHist = new int[256];
        int partsPerThread = tmp.length / numThreads;
        List<HistogramThread> histList = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            int startRow = t * partsPerThread;
            int endRow = (t == numThreads - 1) ? tmp.length : (t + 1) * partsPerThread;

            histList.add(new HistogramThread(tmp, startRow, endRow));
        }

        histList.forEach(HistogramThread::start);

        for (HistogramThread histogramThread : histList) {
            try {
                histogramThread.join();
            } catch (InterruptedException e) {
                System.out.println("Thread  Interrupted: " + e);
            }
        }

        for (HistogramThread histogramThread: histList){
            int[] localHist = histogramThread.getLocalHist();
            for(int i = 0; i < 256; i++){
                finalHist[i] += localHist[i];
            }
        }
        return finalHist;
    }

    public Color[][] createNewImageParallel(Color[][] tmp, int total_pixels, int numThreads){
        int partsPerThread = tmp.length / numThreads;
        List<ImageWriterThread> imageWTList = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            int startRow = t * partsPerThread;
            int endRow = (t == numThreads - 1) ? tmp.length : (t + 1) * partsPerThread;

            imageWTList.add(new ImageWriterThread(tmp, startRow, endRow, total_pixels, this.cumulative, this.cdfMin));
        }

        imageWTList.forEach(ImageWriterThread::start);

        for (ImageWriterThread imageWriterThread : imageWTList) {
            try {
                imageWriterThread.join();
            } catch (InterruptedException e) {
                System.out.println("Thread  Interrupted: " + e);
            }
        }
        return tmp;
    }

    public int[] luminosityResultMatrixParallel(Color[][] tmp, int numThreads){
        AtomicIntegerArray sharedHist = new AtomicIntegerArray(256);
        int partsPerThread = tmp.length / numThreads;
        List<HistogramThreadContention> histList = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            int startRow = t * partsPerThread;
            int endRow = (t == numThreads - 1) ? tmp.length : (t + 1) * partsPerThread;

            histList.add(new HistogramThreadContention(tmp, startRow, endRow, sharedHist));
        }

        histList.forEach(HistogramThreadContention::start);

        for (HistogramThreadContention histogramThread : histList) {
            try {
                histogramThread.join();
            } catch (InterruptedException e) {
                System.out.println("Thread  Interrupted: " + e);
            }
        }

        int[] finalHist = new int[256];
        for (int i = 0; i < 256; i++) {
            finalHist[i] = sharedHist.get(i);
        }
        return finalHist;
    }
}


