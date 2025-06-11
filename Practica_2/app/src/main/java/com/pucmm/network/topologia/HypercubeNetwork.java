package com.pucmm.network.topologia;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.pucmm.network.core.Message;
import com.pucmm.network.core.NetworkTopology;
import com.pucmm.network.core.Node;

/**
 * Topologia de hipercubo: nodos con IDs 0..2^d-1, conectados si difieren en un bit.
 */
public class HypercubeNetwork implements NetworkTopology {
    private List<Node> nodes;
    private ExecutorService nodeExecutor;
    private ExecutorService sendExecutor;
    // latencia por hop
    private final long hopLatencyMs = 100;

    /**
     * Configura la red de hipercubo con el numero de nodos dado.
     * Verifica que sea potencia de 2. Calcula dimension d tal que 2^d == numberOfNodes.
     * Crea nodos con ids 0..numberOfNodes-1 y arranca executors.
     *
     * @param numberOfNodes cantidad de nodos; debe ser potencia de 2
     * @throws IllegalArgumentException si numberOfNodes < 1 o no es potencia de 2
     */
    @Override
    public void configureNetwork(int numberOfNodes) {
        if (numberOfNodes < 1) {
            throw new IllegalArgumentException("HypercubeNetwork requiere al menos 1 nodo");
        }
        int d = 0;
        while ((1 << d) < numberOfNodes) {
            d++;
        }
        if ((1 << d) != numberOfNodes) {
            throw new IllegalArgumentException("HypercubeNetwork requiere numero de nodos potencia de 2");
        }
        nodes = new ArrayList<>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            nodes.add(new Node(i));
        }
        nodeExecutor = Executors.newFixedThreadPool(numberOfNodes);
        sendExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Envia un mensaje desde 'from' hacia 'to' en la red de hipercubo.
     * Rutea bit a bit: en cada hop corrige el bit de menor peso distinto.
     * Somete tarea en sendExecutor que imprime logs de ruteo y realiza
     * receiveMessage en cada hop.
     *
     * @param from indice origen
     * @param to indice destino
     * @param payload contenido del mensaje
     * @throws IllegalStateException si la topologia no esta configurada
     * @throws IllegalArgumentException si from o to fuera de rango
     */
    @Override
    public void sendMessage(int from, int to, String payload) {
        validateIndices(from, to);
        Message msg = new Message(from, to, payload);
        sendExecutor.submit(() -> {
            System.out.printf("[%s] HypercubeNetwork: nodo %d iniciando ruteo hacia %d%n",
                    Instant.now(), from, to);
            if (from == to) {
                nodes.get(to).receiveMessage(msg);
                System.out.printf("[%s] HypercubeNetwork: mensaje de %d a %d entregado directo%n",
                        Instant.now(), from, to);
                return;
            }
            int current = from;
            while (current != to) {
                int diff = current ^ to;
                int bit = Integer.lowestOneBit(diff);
                int next = current ^ bit;
                System.out.printf("[%s] HypercubeNetwork: nodo %d reenviando a %d%n",
                        Instant.now(), current, next);
                nodes.get(next).receiveMessage(msg);
                current = next;
                try {
                    Thread.sleep(hopLatencyMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.printf("[%s] HypercubeNetwork: ruteo de %d a %d completado%n", Instant.now(), from, to);
        });
    }

    /**
     * Inicia la simulacion: ejecuta cada nodo en su hilo para procesar inbox.
     *
     * @throws IllegalStateException si la topologia no esta configurada
     */
    @Override
    public void runNetwork() {
        for (Node node : nodes) {
            nodeExecutor.submit(node);
        }
    }

    /**
     * Detiene la simulacion:
     * - Llama stop() a cada nodo
     * - Cierra nodeExecutor y sendExecutor, esperando hasta 4s antes de forzar.
     */
    @Override
    public void shutdown() {
        for (Node node : nodes) {
            node.stop();
        }
        nodeExecutor.shutdown();
        sendExecutor.shutdown();
        try {
            if (!sendExecutor.awaitTermination(4, TimeUnit.SECONDS)) {
                sendExecutor.shutdownNow();
            }
            if (!nodeExecutor.awaitTermination(4, TimeUnit.SECONDS)) {
                nodeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sendExecutor.shutdownNow();
            nodeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Verifica que nodes no sea null y que from/to esten en rango valido.
     *
     * @param from indice origen
     * @param to indice destino
     * @throws IllegalStateException si nodes es null
     * @throws IllegalArgumentException si indices fuera de rango
     */
    private void validateIndices(int from, int to) {
        if (nodes == null) {
            throw new IllegalStateException("HypercubeNetwork no esta configurada.");
        }
        int size = nodes.size();
        if (from < 0 || from >= size || to < 0 || to >= size) {
            throw new IllegalArgumentException(
                    "Indices fuera de rango en HypercubeNetwork: from=" + from + " to=" + to);
        }
    }
}
