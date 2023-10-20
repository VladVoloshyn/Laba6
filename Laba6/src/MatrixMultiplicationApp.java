import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Random;

public class MatrixMultiplicationApp {
    public static void main(String[] args) {

        int matrixSize = 1000;
        int[][] matrixA = new int[matrixSize][matrixSize];
        int[][] matrixB = new int[matrixSize][matrixSize];
        Random random = new Random();

        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                matrixA[i][j] = random.nextInt(100);
                matrixB[i][j] = random.nextInt(100);
            }
        }

        int[][] resultMatrix;

        int[] threadCounts = {1, 2, 4};

        for (int threads : threadCounts) {
            System.out.println("Number of threads: " + threads);

            long startTime = System.nanoTime();
            resultMatrix = sequentialMatrixMultiplication(matrixA, matrixB);
            long endTime = System.nanoTime();
            double sequentialTime = (endTime - startTime) / 1_000_000_000.0;
            System.out.println("Algorithm 1 (sequential) time: " + sequentialTime + " seconds");

            startTime = System.nanoTime();
            resultMatrix = foxMatrixMultiplication(matrixA, matrixB, threads);
            endTime = System.nanoTime();
            double foxTime = (endTime - startTime) / 1_000_000_000.0;
            System.out.println("Algorithm 2 (Fox's method) time: " + foxTime + " seconds");

            startTime = System.nanoTime();
            resultMatrix = cannonMatrixMultiplication(matrixA, matrixB, threads);
            endTime = System.nanoTime();
            double cannonTime = (endTime - startTime) / 1_000_000_000.0;
            System.out.println("Algorithm 3 (Cannon's method) time: " + cannonTime + " seconds");
        }
    }

    private static int[][] sequentialMatrixMultiplication(int[][] matrixA, int[][] matrixB) {
        int rowsA = matrixA.length;
        int colsA = matrixA[0].length;
        int colsB = matrixB[0].length;

        int[][] resultMatrix = new int[rowsA][colsB];

        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                int sum = 0;
                for (int k = 0; k < colsA; k++) {
                    sum += matrixA[i][k] * matrixB[k][j];
                }
                resultMatrix[i][j] = sum;
            }
        }

        return resultMatrix;
    }

    private static int[][] foxMatrixMultiplication(int[][] matrixA, int[][] matrixB, int threads) {
        int n = matrixA.length;
        int blockSize = n / threads;


        int[][][] subMatrixA = new int[threads][blockSize][blockSize];
        int[][][] subMatrixB = new int[threads][blockSize][blockSize];


        ExecutorService executor = Executors.newFixedThreadPool(threads);


        int[][] resultMatrix = new int[n][n];


        Future<?>[] futures = new Future[threads];

        for (int i = 0; i < threads; i++) {
            final int threadIndex = i;
            futures[i] = executor.submit(() -> {
                for (int k = 0; k < n; k++) {
                    for (int b = 0; b < blockSize; b++) {
                        for (int j = 0; j < blockSize; j++) {
                            resultMatrix[threadIndex * blockSize + b][k] += subMatrixA[threadIndex][b][k] * subMatrixB[threadIndex][j][k];
                        }
                    }
                }
            });
        }


        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
            }
        }


        executor.shutdown();

        return resultMatrix;
    }


    private static int[][] cannonMatrixMultiplication(int[][] matrixA, int[][] matrixB, int threads) {
        int n = matrixA.length;
        int blockSize = n / threads;


        int[][][] subMatrixA = new int[threads][blockSize][blockSize];
        int[][][] subMatrixB = new int[threads][blockSize][blockSize];




        ExecutorService executor = Executors.newFixedThreadPool(threads);


        int[][] resultMatrix = new int[n][n];


        Future<?>[] futures = new Future[threads];

        for (int i = 0; i < threads; i++) {
            final int threadIndex = i;
            futures[i] = executor.submit(() -> {

                cannonMultiplicationForBlock(subMatrixA[threadIndex], subMatrixB[threadIndex], resultMatrix, threadIndex, blockSize);
            });
        }


        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        executor.shutdown();

        return resultMatrix;
    }

    private static void cannonMultiplicationForBlock(int[][] subMatrixA, int[][] subMatrixB, int[][] resultMatrix, int blockIndex, int blockSize) {
        int n = subMatrixA.length;
        int[][] C = new int[blockSize][blockSize];

        for (int k = 0; k < blockSize; k++) {

            int[][] shiftedA = shiftMatrix(subMatrixA, 0, k, blockSize);
            int[][] shiftedB = shiftMatrix(subMatrixB, k, 0, blockSize);


            for (int i = 0; i < blockSize; i++) {
                for (int j = 0; j < blockSize; j++) {
                    C[i][j] += shiftedA[i][j] * shiftedB[i][j];
                }
            }
        }


        for (int i = 0; i < blockSize; i++) {
            for (int j = 0; j < blockSize; j++) {
                resultMatrix[blockIndex * blockSize + i][j] = C[i][j];
            }
        }
    }

    private static int[][] shiftMatrix(int[][] matrix, int shiftRows, int shiftCols, int blockSize) {
        int n = matrix.length;
        int[][] shiftedMatrix = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int newRow = (i + shiftRows) % n;
                int newCol = (j + shiftCols) % n;
                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n) {
                    shiftedMatrix[newRow][newCol] = matrix[i][j];
                }
            }
        }

        return extractBlock(shiftedMatrix, 0, 0, blockSize);
    }


    private static int[][] extractBlock(int[][] matrix, int startRow, int startCol, int blockSize) {
        int[][] block = new int[blockSize][blockSize];
        for (int i = 0; i < blockSize; i++) {
            for (int j = 0; j < blockSize; j++) {
                block[i][j] = matrix[startRow + i][startCol + j];
            }
        }
        return block;
    }
}
