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
 * Topologia de malla: cada nodo se comunica directo con cualquier otro.
 */
public class MeshNetwork implements NetworkTopology {
    private List<Node> nodes;
    private ExecutorService nodeExecutor;
    private ExecutorService sendExecutor;

    /**
     * Configura la topologia de malla con un numero de nodos dado.
     * Crea nodos ids 0..numberOfNodes-1 y prepara los executors.
     *
     * @param numberOfNodes cantidad de nodos a crear
     * @throws IllegalArgumentException si numberOfNodes < 1
     */
    @Override
    public void configureNetwork(int numberOfNodes) {
        if (numberOfNodes < 1) {
            throw new IllegalArgumentException("MeshNetwork requiere al menos 1 nodo");
        }
        nodes = new ArrayList<>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            nodes.add(new Node(i));
        }
        nodeExecutor = Executors.newFixedThreadPool(numberOfNodes);
        sendExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Envia un mensaje directo desde 'from' a 'to'.
     * Somete una tarea en sendExecutor que imprime log de envio y
     * agrega el mensaje al inbox del nodo destino.
     *
     * @param from indice de origen
     * @param to indice de destino
     * @param payload contenido del mensaje
     * @throws IllegalStateException si la topologia no esta configurada
     * @throws IllegalArgumentException si from o to estan fuera de rango
     */
    @Override
    public void sendMessage(int from, int to, String payload) {
        validateIndices(from, to);
        Message msg = new Message(from, to, payload);
        sendExecutor.submit(() -> {
            System.out.printf("[%s] MeshNetwork: nodo %d enviando directo a %d%n",
                    Instant.now(), from, to);
            nodes.get(to).receiveMessage(msg);
        });
    }
    
    /**
     * Inicia la simulacion: ejecuta cada nodo en su propio hilo para procesar inbox.
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
     * - Marca cada nodo con stop()
     * - Cierra executors esperando hasta 2 segundos antes de shutdownNow()
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
     * Verifica que la topologia este lista y que from/to esten en rango.
     *
     * @param from indice de origen
     * @param to indice de destino
     * @throws IllegalStateException si nodes es null
     * @throws IllegalArgumentException si indices fuera de rango
     */
    private void validateIndices(int from, int to) {
        if (nodes == null) {
            throw new IllegalStateException("MeshNetwork no esta configurada.");
        }
        int size = nodes.size();
        if (from < 0 || from >= size || to < 0 || to >= size) {
            throw new IllegalArgumentException(
                    "Indices fuera de rango en MeshNetwork: from=" + from + " to=" + to);
        }
    }
}