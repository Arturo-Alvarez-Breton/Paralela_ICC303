import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import src.SumWorker;

public class Main {
    public static void main(String[] args){
        int n = 1000000; // Número de registros
        String dataPath = "data/data.txt";

        // generar el archivo data.txt si no existe
        generarData(n, dataPath);

        // leer los datos del archivo
        int[] dataArray = leerData(dataPath);

        System.out.println("             Secuencial            \n___________________________________");
        sumaSecuencial(dataArray);

        System.out.println("-----------------------------------\n             Paralelo              ");      

        System.out.println("2 Hilos: --------------------------");
        sumaParalela(dataArray, 2);
        System.out.println("4 Hilos: --------------------------");
        sumaParalela(dataArray, 4);
        System.out.println("8 Hilos: --------------------------");
        sumaParalela(dataArray, 8);
        System.out.println("16 Hilos: -------------------------");
        sumaParalela(dataArray, 16);
        System.out.println("32 Hilos: -------------------------");
        sumaParalela(dataArray, 32);    
    }

    /*
     * 1) Genera un archivo con 1,000,000 de registros comprendido entre 1 y 10,000, el
     * cual deberá usar como base para los demás cálculos
     */
    public static void generarData(int n, String dataPath) {
        if (!Files.exists(Paths.get(dataPath))) {
            try(FileWriter writer = new FileWriter(dataPath)){
                Random random = new Random();
                for (int i = 0; i < n; i++) { writer.write((random.nextInt(10_000) + 1) + "\n"); }
            }
            catch (IOException e) {
                System.err.println("Error al generar el archivo: " + e.getMessage());
            }
        }
    }

    public static int[] leerData(String dataPath) {
        try {
            return Files.readAllLines(Paths.get(dataPath)).stream().mapToInt(Integer::parseInt).toArray();
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            return null;
        }
    }

    /*
     * 2) Escribe un programa secuencial que sume los elementos de un arreglo de un
     * millón de enteros.
     */
    public static void sumaSecuencial(int[] dataArray) {
        long suma = 0L;

        long inicio = System.nanoTime();
        for (int num : dataArray) {
            suma += num;
        }
        long fin = System.nanoTime();
        System.out.println("Total: " + suma);
        System.out.printf("Tiempo (s): %.6f%n", (fin - inicio) / 1_000_000_000.0);
    }

    /*
     * 3) Modifica tu programa para que use múltiples hilos o procesos para realizar la
     * suma en paralelo. Divide el arreglo en partes iguales para cada hilo/proceso.
     */
    public static void sumaParalela(int[] dataArray, int numThreads) {
        long suma = 0L;

        int chunkSize = dataArray.length / numThreads;
        List<SumWorker> workers = new ArrayList<>();

        long inicio = System.nanoTime();
        int startIdx = 0;
        for (int i = 0; i < numThreads; i++) {
            int extra = (i == numThreads - 1) ? dataArray.length - (i * chunkSize) : chunkSize;
            int endIdx = startIdx + extra;

            // Ahora le pasamos los índices directamente, sin copiar el arreglo
            SumWorker worker = new SumWorker(dataArray, startIdx, endIdx);
            workers.add(worker);
            worker.start();

            startIdx = endIdx;
        }

        for (SumWorker worker : workers) {
            try {
                worker.join();
                suma += worker.getSuma();
            } catch (InterruptedException e) {
                System.err.println("Error al esperar el hilo: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        long fin = System.nanoTime();
        System.out.println("Suma: " + suma);
        System.out.printf("Tiempo (s): %.6f%n", (fin - inicio) / 1_000_000_000.0);
    }

}