# Image Filters Performance Benchmark (Java)

## Overview

This project implements and evaluates multiple parallelization strategies in Java applied to image processing, specifically a histogram-based filter. The primary objective is to assess the impact of different concurrency models on:

- Execution time
- Memory consumption
- Scalability with respect to the number of threads

---

## Project Structure

```
src/
├── ApplyFilters.java                  # Entry point / benchmark runner
├── sequential/
│   └── Filters.java                  # Sequential implementation (baseline)
├── withoutThreadPools/
│   ├── FiltersMultiThreadNoPools.java
│   ├── HistogramThread.java
│   ├── HistogramThreadContention.java
│   └── ImageWriterThread.java
├── threadPools/
│   ├── FiltersMultiThreadWithThreadPools.java
│   ├── HistogramTask.java
│   └── ImageWriterTask.java
├── forkJoinPool/
│   ├── FiltersForkJoinPool.java
│   ├── HistogramFork.java
│   └── ImageWriterFork.java
├── computableFutures/
│   └── FiltersCompletableFuture.java
└── utils/
    └── Utils.java
```

---

## Compilation and Execution

### Compilation

Compile the project with:

```bash
javac -d out src/**/*.java src/*.java
```

If there are changes to the source code, perform a clean compilation:

```bash
rm -rf out && javac -d out src/**/*.java src/*.java
```

If no changes were made, the existing compiled classes can be reused without removing the output directory.

---

### Execution

```bash
java -Xmx12g -cp out ApplyFilters <filePath> <mode> <threads>
```

### Example

```bash
java -Xmx12g -cp out ApplyFilters src3.jpg nopool 2
```

If there are changes to the source code, perform a clean compilation and execute:
```bash
rm -rf out && javac -d out src/**/*.java src/*.java && java -Xmx12g -cp out ApplyFilters src.jpg computableFutures 0
```
For the `forkjoin` and `computableFutures` modes, the number of threads parameter should be set to `0`, as thread management is handled internally by the framework.

---

## Available Modes

| Mode                | Description |
|---------------------|------------|
| `sequential`        | Sequential execution (baseline) |
| `nopool`            | Manual thread management (no pooling) |
| `threadpool`        | Thread pool-based execution (ExecutorService) |
| `forkjoin`          | Fork/Join framework (divide-and-conquer model) |
| `computableFutures` | Asynchronous execution using CompletableFuture |

---

## Benchmark Methodology

The benchmarking process follows a controlled execution model:

1. The algorithm is executed **10 times**
2. The first **3 runs are discarded** as warm-up iterations  
   (to mitigate JVM Just-In-Time compilation effects)
3. The following metrics are collected:
    - Execution time (milliseconds)
    - Memory usage (converted to megabytes)
4. The final result is computed as the average of the valid runs

### Output Format

```
threadpool | Time: 45ms | Mem: 60.12 MB | Threads: 4
```

---

## Implementation Characteristics

### Sequential
- Serves as the baseline reference
- No concurrency overhead

---

### No Pool (`nopool`)
- Explicit thread creation and management
- Higher overhead due to:
    - frequent thread instantiation
    - lack of reuse mechanisms

---

### Thread Pool (`threadpool`)
- Reuses a fixed set of threads
- Provides better resource management
- Generally more efficient than manual threading in practical scenarios

---

### Fork/Join (`forkjoin`)
- Based on a divide-and-conquer paradigm
- Employs work-stealing for load balancing
- Well-suited for recursive or highly decomposable tasks

---

### CompletableFuture (`computableFutures`)
- Supports asynchronous and non-blocking execution
- Enables composition of dependent tasks
- May introduce additional overhead depending on task granularity and chaining

---

## Notes

- `System.gc()` is invoked between runs to reduce memory-related interference, though it does not guarantee full garbage collection
- `Thread.sleep(200)` introduces a delay to stabilize execution between iterations
- Performance results may vary depending on:
    - CPU architecture
    - number of available cores
    - system load and background processes

---

## Objective

The goal of this project is to provide a systematic comparison of different parallelization strategies in Java, identifying:

- Performance improvements over the sequential baseline
- Scalability behavior under increased parallelism
- Overhead introduced by concurrency mechanisms
- Potential bottlenecks limiting performance gains

---
