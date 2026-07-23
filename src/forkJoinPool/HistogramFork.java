package forkJoinPool;

import sequential.Filters;

import java.awt.*;


import java.util.concurrent.RecursiveTask;

public class HistogramFork extends RecursiveTask<int[]> {

    Color[][] image;
    int startRow;
    int endRow;
    int[] localHist = new int[256];
    private static final int THRESHOLD = 1_000_000;

    public HistogramFork(Color[][] image, int startRow, int endRow){
        this.image=image;
        this.startRow=startRow;
        this.endRow=endRow;
    }

    @Override
    protected int[] compute() {
        int totalPixels = 0;
        for (int i = startRow; i < endRow; i++) {
            totalPixels += image[i].length;
        }

        if (totalPixels <= THRESHOLD) {
            for (int i = startRow; i < endRow; i++) {
                for (int j = 0; j < image[i].length; j++) {
                    Color pixel = image[i][j];
                    int r = pixel.getRed();
                    int g = pixel.getGreen();
                    int b = pixel.getBlue();
                    int lum = Filters.computeLuminosity(r, g, b);
                    localHist[lum]++;
                }
            }
        } else {
            int mid = (endRow + startRow) / 2;
            HistogramFork histFork1 = new HistogramFork(image, startRow, mid);
            HistogramFork histFork2 = new HistogramFork(image, mid, endRow);

            histFork1.fork();                           //this goes to another thread
            int[] firstValue = histFork2.compute();     // this executes in this thread
            int[] secondValue = histFork1.join();       // assign the value when the other thread ends the execution

            for(int i = 0; i < firstValue.length; i++){
                firstValue[i] +=  secondValue[i];
            }
            localHist=firstValue;
        }
        return localHist;
    }
}
