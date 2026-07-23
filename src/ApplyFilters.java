import computableFutures.FiltersCompletableFuture;
import forkJoinPool.FiltersForkJoinPool;
import sequential.Filters;
import threadPools.FiltersMultiThreadWithThreadPools;
import withoutThreadPools.FiltersMultiThreadNoPools;

import java.util.Arrays;

public class ApplyFilters {
    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.out.println("Usage: java Main <filePath> <mode>");
            System.out.println("Modes: sequential, nopool, threadpool, forkjoin");
            System.out.println("Number of Threads");
            return;
        }

        String filePath = args[0];
        String mode = args[1];
        int threads = Integer.parseInt(args[2]);

        int runs = 10;
        int warmup = 3;

        long[] times = new long[runs];
        long[] mem = new long[runs];

        for (int i = 0; i < runs; i++) {
            long[] r;
            switch (mode) {
                case "sequential" -> {
                    Filters f = new Filters(filePath);
                    r = f.HistogramFilter("output.jpg", 128);
                }
                case "nopool" -> {
                    FiltersMultiThreadNoPools f = new FiltersMultiThreadNoPools(filePath, threads);
                    r = f.HistogramFilter("output-multi.jpg", 128);
                }
                case "threadpool" -> {
                    FiltersMultiThreadWithThreadPools f = new FiltersMultiThreadWithThreadPools(filePath, threads);
                    r = f.HistogramFilter("output-multi-thread-pools.jpg", 128);
                }
                case "forkjoin" -> {
                    FiltersForkJoinPool f = new FiltersForkJoinPool(filePath);
                    r = f.HistogramFilter("output-fork-join.jpg", 128);
                }
                case "computableFutures" -> {
                    FiltersCompletableFuture f = new FiltersCompletableFuture(filePath);
                    r = f.HistogramFilter("output-computableFutures.jpg", 128);
                }
                default -> {
                    System.out.println("Unknown mode: " + mode);
                    return;
                }
            }
            times[i] = r[0];
            mem[i] = r[1];
            Thread.sleep(200);
        }

        int valid = runs - warmup;
        System.out.printf("%s | Time: %dms | Mem: %.2f MB | Threads: %d%n",
                mode,
                Arrays.stream(times, warmup, runs).sum() / valid,
                Arrays.stream(mem, warmup, runs).sum() / valid / (1024.0 * 1024.0),
                threads);
    }
}
