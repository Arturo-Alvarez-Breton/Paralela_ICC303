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
 * Topologia en estrella: existe un nodo central (indice 0) que conecta con todas las hojas.
 * Los mensajes entre hojas pasan por el central. Se simula latencia en reenvio de hoja->central->hoja.
 */
public class StarNetwork implements NetworkTopology {
    private List<Node> nodes;
    private ExecutorService nodeExecutor;
    private ExecutorService sendExecutor;
    private final int centralIndex = 0;
    // latencia entre envio de hoja->central->hoja
    private final long forwardLatencyMs = 50;

     /**
     * Configura la red en estrella con numNodes nodos.
     * Requiere al menos 2 nodos (1 central + 1 hoja).
     *
     * @param numNodes cantidad de nodos; debe ser >= 2
     * @throws IllegalArgumentException si numNodes < 2
     */
    @Override
    public void configureNetwork(int numNodes) {
        if (numNodes < 2) {
            throw new IllegalArgumentException("StarNetwork requiere al menos 2 nodos");
        }
        nodes = new ArrayList<>(numNodes);
        for (int i = 0; i < numNodes; i++) {
            nodes.add(new Node(i));
        }
        nodeExecutor = Executors.newFixedThreadPool(numNodes);
        sendExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Envia un mensaje desde 'from' hacia 'to' en la topologia en estrella.
     * Casos:
     * - from == to: entrega directa.
     * - from == central: entrega directa a hoja.
     * - to == central: entrega directa hoja->central.
     * - hoja->hoja: reenviar hoja->central, duerme forwardLatencyMs, luego central->destino.
     * Imprime logs de cada paso.
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
            System.out.printf("[%s] StarNetwork: nodo %d enviando a %d%n",
                    Instant.now(), from, to);
            if (from == to) {
                nodes.get(to).receiveMessage(msg);
                System.out.printf("[%s] StarNetwork: mensaje de %d a %d entregado directo%n",
                        Instant.now(), from, to);
                return;
            }
            if (from == centralIndex) {
                // central a hoja
                System.out.printf("[%s] StarNetwork: reenviando directo central->%d%n",
                        Instant.now(), to);
                nodes.get(to).receiveMessage(msg);
            } else if (to == centralIndex) {
                // hoja a central
                System.out.printf("[%s] StarNetwork: reenvio hoja->central%n", Instant.now());
                nodes.get(centralIndex).receiveMessage(msg);
            } else {
                // hoja->central, luego central->destino
                System.out.printf("[%s] StarNetwork: reenvio hoja->central%n", Instant.now());
                nodes.get(centralIndex).receiveMessage(msg);
                try {
                    Thread.sleep(forwardLatencyMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                System.out.printf("[%s] StarNetwork: reenviando central->%d%n", Instant.now(), to);
                nodes.get(to).receiveMessage(msg);
            }
            System.out.printf("[%s] StarNetwork: ruteo de %d a %d completado%n", Instant.now(), from, to);
        });
    }

    /**
     * Inicia la simulacion: ejecuta Node.run() de cada nodo en paralelo.
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
     * - Cierra executors con espera hasta 3s antes de forzar.
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
     * Verifica que nodes no sea null y que from/to esten dentro de rango.
     *
     * @param from indice de origen
     * @param to indice de destino
     * @throws IllegalStateException si topologia no configurada
     * @throws IllegalArgumentException si indices fuera de rango
     */
    private void validateIndices(int from, int to) {
        if (nodes == null) {
            throw new IllegalStateException("StarNetwork no esta configurada.");
        }
        int size = nodes.size();
        if (from < 0 || from >= size || to < 0 || to >= size) {
            throw new IllegalArgumentException(
                    "Indices fuera de rango en StarNetwork: from=" + from + " to=" + to);
        }
    }
}