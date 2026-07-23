package withoutThreadPools;

import sequential.Filters;

import java.awt.*;

public class HistogramThread extends Thread{
    Color[][] image;
    int startRow;
    int endRow;

    int[] localHist = new int[256];

    public HistogramThread(Color[][] image, int startRow, int endRow){
        this.image=image;
        this.startRow=startRow;
        this.endRow=endRow;
    }

    @Override
    public void run(){
        for(int i = startRow; i < endRow; i ++){
            for(int j = 0; j < image[i].length; j++){
                Color pixel = image[i][j];
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                int lum = Filters.computeLuminosity(r, g, b);
                localHist[lum]++;
            }
        }
    }

    public int[] getLocalHist() {
        return localHist;
    }
}
