package forkJoinPool;

import sequential.Filters;

import java.awt.*;
import java.util.concurrent.RecursiveAction;

public class ImageWriterFork extends RecursiveAction {

    Color[][] localImage;
    int startRow;
    int endRow;
    int cdfMin;
    int total_pixels;
    int[] cumulative;
    private static final int THRESHOLD = 1_000_000;

    public ImageWriterFork(Color[][] image, int startRow, int endRow, int total_pixels, int[] cumulative, int cdfMin){
        this.localImage=image;
        this.startRow=startRow;
        this.endRow=endRow;
        this.total_pixels=total_pixels;
        this.cumulative = cumulative;
        this.cdfMin=cdfMin;
    }

    @Override
    protected void compute() {

        int totalPixels = 0;
        for (int i = startRow; i < endRow; i++) {
            totalPixels += localImage[i].length;
        }

        if(totalPixels < THRESHOLD){
            for (int i = startRow; i < endRow; i++) {
                for (int j = 0; j < localImage[i].length; j++) {
                    // fetches values of each pixel
                    Color pixel = localImage[i][j];
                    int r = pixel.getRed();
                    int g = pixel.getGreen();
                    int b = pixel.getBlue();
                    int lum = Filters.computeLuminosity(r, g, b);
                    //int newLum = 255*(cumulative[lum]/total_pixels);

                    double cdf = (double) cumulative[lum] / (double) (total_pixels - cdfMin);
                    int newLum = (int) Math.round(255.0 * cdf);

                    if (newLum < 0) newLum = 0;
                    if (newLum > 255) newLum = 255;
                    localImage[i][j] = new Color(newLum, newLum, newLum);
                }
            }
        }else {
            int mid = (endRow + startRow) / 2;
            ImageWriterFork writer1 = new ImageWriterFork(localImage, startRow, mid, total_pixels, cumulative, cdfMin);
            ImageWriterFork writer2 = new ImageWriterFork(localImage, mid, endRow, total_pixels, cumulative, cdfMin);

            writer1.fork();
            writer2.compute();
            writer1.join();
        }
    }
}
