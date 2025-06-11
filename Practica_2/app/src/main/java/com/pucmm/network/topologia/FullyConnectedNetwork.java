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
 * Topologia totalmente conectada: cada par de nodos comunica directo.
 */
public class FullyConnectedNetwork implements NetworkTopology {
    private List<Node> nodes;
    private ExecutorService nodeExecutor;
    private ExecutorService sendExecutor;

     /**
     * Envia un mensaje desde 'from' a 'to' directamente.
     * Somete tarea al sendExecutor que registra log y entrega al inbox del destino.
     *
     * @param from indice de origen
     * @param to indice de destino
     * @param payload contenido del mensaje
     * @throws IllegalStateException si la topologia no esta configurada
     * @throws IllegalArgumentException si from o to fuera de rango
     */
    @Override
    public void configureNetwork(int numberOfNodes) {
        if (numberOfNodes < 1) {
            throw new IllegalArgumentException("FullyConnectedNetwork requiere al menos 1 nodo");
        }
        nodes = new ArrayList<>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            nodes.add(new Node(i));
        }
        nodeExecutor = Executors.newFixedThreadPool(numberOfNodes);
        sendExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void sendMessage(int from, int to, String payload) {
        validateIndices(from, to);
        Message msg = new Message(from, to, payload);
        sendExecutor.submit(() -> {
            System.out.printf("[%s] FullyConnectedNetwork: nodo %d enviando a %d%n",
                    Instant.now(), from, to);
            nodes.get(to).receiveMessage(msg);
        });
    }

     /**
     * Inicia la simulacion de la red: arranca cada nodo en su hilo para procesar inbox.
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
     * - Cierra executors y espera hasta 2s antes de forzar shutdownNow()
     */
    @Override
    public void shutdown() {
        for (Node node : nodes) {
            node.stop();
        }
        nodeExecutor.shutdown();
        sendExecutor.shutdown();
        try {
            if (!sendExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                sendExecutor.shutdownNow();
            }
            if (!nodeExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                nodeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sendExecutor.shutdownNow();
            nodeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Verifica que la red este configurada y que from/to sean indices validos.
     *
     * @param from indice origen
     * @param to indice destino
     * @throws IllegalStateException si nodes es null
     * @throws IllegalArgumentException si indices fuera de rango
     */
    private void validateIndices(int from, int to) {
        if (nodes == null) {
            throw new IllegalStateException("FullyConnectedNetwork no esta configurada.");
        }
        int size = nodes.size();
        if (from < 0 || from >= size || to < 0 || to >= size) {
            throw new IllegalArgumentException(
                    "Indices fuera de rango en FullyConnectedNetwork: from=" + from + " to=" + to);
        }
    }
}