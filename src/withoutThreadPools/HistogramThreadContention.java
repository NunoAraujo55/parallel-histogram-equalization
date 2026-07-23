package withoutThreadPools;

import sequential.Filters;

import java.awt.*;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class HistogramThreadContention extends Thread{

    Color[][] image;
    int startRow;
    int endRow;
    AtomicIntegerArray sharedHist;

    public HistogramThreadContention(Color[][] image, int startRow, int endRow, AtomicIntegerArray sharedHist) {
        this.image = image;
        this.startRow = startRow;
        this.endRow = endRow;
        this.sharedHist = sharedHist;
    }

    @Override
    public void run() {
        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < image[i].length; j++) {
                Color pixel = image[i][j];
                int lum = Filters.computeLuminosity(pixel.getRed(), pixel.getGreen(), pixel.getBlue());
                sharedHist.incrementAndGet(lum);
            }
        }
    }
}
