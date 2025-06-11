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
 * Topologia de anillo: cada nodo conectado al siguiente modulo N, mensajes via hops unidireccionales.
 */
public class RingNetwork implements NetworkTopology {
    private List<Node> nodes;
    private ExecutorService nodeExecutor;
    private ExecutorService sendExecutor;
    // latencia entre hops en ms, parametrizable
    private final long hopLatencyMs = 100;

    /**
     * Configura la topologia de anillo con numNodes nodos.
     * Valida numNodes>=1 y crea nodos con ids 0..numNodes-1.
     *
     * @param numNodes cantidad de nodos en el anillo
     * @throws IllegalArgumentException si numNodes < 1
     */
    @Override
    public void configureNetwork(int numNodes) {
        if (numNodes < 1) {
            throw new IllegalArgumentException("RingNetwork requiere al menos 1 nodo");
        }
        nodes = new ArrayList<>(numNodes);
        for (int i = 0; i < numNodes; i++) {
            nodes.add(new Node(i));
        }
        nodeExecutor = Executors.newFixedThreadPool(numNodes);
        sendExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Envía un mensaje desde 'from' a 'to' por el anillo en direccion unidireccional.
     * Somete tarea en sendExecutor que recorre hops: current->(current+1)%size...
     * Imprime logs de reenveio y llegada a destino.
     *
     * @param from indice de origen
     * @param to indice de destino
     * @param payload contenido del mensaje
     * @throws IllegalStateException si la topologia no esta configurada
     * @throws IllegalArgumentException si from o to fuera de rango
     */
    @Override
    public void sendMessage(int from, int to, String payload) {
        validateIndices(from, to);
        Message msg = new Message(from, to, payload);
        sendExecutor.submit(() -> {
            System.out.printf("[%s] RingNetwork: nodo %d empezando ruteo hacia %d%n",
                    Instant.now(), from, to);
            if (from == to) {
                // directo
                nodes.get(to).receiveMessage(msg);
                System.out.printf("[%s] RingNetwork: mensaje de %d a %d entregado directo%n",
                        Instant.now(), from, to);
                return;
            }
            int current = from;
            int size = nodes.size();
            while (true) {
                int next = (current + 1) % size;
                // log reenviar hop
                System.out.printf("[%s] RingNetwork: nodo %d reenviando a %d%n",
                        Instant.now(), current, next);
                nodes.get(next).receiveMessage(msg);
                if (next == to) {
                    System.out.printf("[%s] RingNetwork: mensaje de %d a %d llegado a destino%n",
                            Instant.now(), from, to);
                    break;
                }
                current = next;
                try {
                    Thread.sleep(hopLatencyMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
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
     * - Llama stop() a cada nodo
     * - Shutdown de executors con espera hasta 3s antes de forzar
     */
    @Override
    public void shutdown() {
        for (Node node : nodes) {
            node.stop();
        }
        nodeExecutor.shutdown();
        sendExecutor.shutdown();
        try {
            if (!sendExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                sendExecutor.shutdownNow();
            }
            if (!nodeExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
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
     * @param from indice de origen
     * @param to indice de destino
     * @throws IllegalStateException si topologia no configurada
     * @throws IllegalArgumentException si indices fuera de rango
     */
    private void validateIndices(int from, int to) {
        if (nodes == null) {
            throw new IllegalStateException("RingNetwork no esta configurada.");
        }
        int size = nodes.size();
        if (from < 0 || from >= size || to < 0 || to >= size) {
            throw new IllegalArgumentException(
                    "Indices fuera de rango en RingNetwork: from=" + from + " to=" + to);
        }
    }
}