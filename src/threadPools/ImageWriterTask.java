package threadPools;

import sequential.Filters;

import java.awt.*;
import java.util.concurrent.Callable;

public class ImageWriterTask implements Callable<Color[][]> {

    Color[][] localImage;
    int startRow;
    int endRow;
    int cdfMin;
    int total_pixels;
    int[] cumulative;

    public ImageWriterTask(Color[][] image, int startRow, int endRow, int total_pixels, int[] cumulative, int cdfMin){
        this.localImage=image;
        this.startRow=startRow;
        this.endRow=endRow;
        this.total_pixels=total_pixels;
        this.cumulative = cumulative;
        this.cdfMin=cdfMin;
    }

    @Override
    public Color[][] call() throws Exception {
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
        return localImage;
    }
}
