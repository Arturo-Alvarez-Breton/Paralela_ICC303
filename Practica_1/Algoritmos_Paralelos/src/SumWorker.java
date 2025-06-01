/*
 * SumWorker.java
 * 
 * 
 */

package src;

public class SumWorker extends Thread {
    private final int[] data;
    private final int startIdx;
    private final int endIdx;
    private long suma;

    public SumWorker(int[] data, int startIdx, int endIdx) {
        this.data = data;
        this.startIdx = startIdx;
        this.endIdx = endIdx;
        this.suma = 0;
    }

    @Override
    public void run() {
        long localSum = 0;
        for (int i = startIdx; i < endIdx; i++) {
            localSum += data[i];
        }
        suma = localSum;
    }

    public long getSuma() {
        return suma;
    }
}