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
 * Implementacion de la topologia de bus.
 * Todos los nodos comparten un medio comun. Hacemos broadcast,
 * pero Node solo procesa si es destino.
 */
public class BusNetwork implements NetworkTopology {
    private List<Node> nodes;
    private ExecutorService nodeExecutor;
    private ExecutorService sendExecutor;

    /**
     * Configura la red en topologia de bus con un numero de nodos dado.
     * Crea los nodos con ids 0..numNodes-1 y prepara los executors:
     * - nodeExecutor: pool fijo de tamaño numNodes para ejecutar los nodos.
     * - sendExecutor: pool cached para envios concurrentes.
     *
     * @param numNodes numero de nodos a instanciar en la topologia
     * @throws IllegalArgumentException si numNodes < 1
     */
    @Override
    public void configureNetwork(int numNodes) {
        if (numNodes < 1) {
            throw new IllegalArgumentException("BusNetwork requiere al menos 1 nodo");
        }
        nodes = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            nodes.add(new Node(i));
        }
        // Executor dedicado para run de nodos
        nodeExecutor = Executors.newFixedThreadPool(numNodes);
        // Executor para tareas de envio, tamanho variable
        sendExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Envia un mensaje desde el nodo 'from' hacia 'to' haciendo broadcast.
     * Se crea un Message con origen, destino y payload, y se somete una tarea
     * en sendExecutor para cada nodo de la lista. Node.run() filtrara segun destino.
     *
     * @param from indice del nodo origen
     * @param to indice del nodo destino
     * @param payload contenido del mensaje
     * @throws IllegalStateException si la topologia no esta configurada
     * @throws IllegalArgumentException si from o to estan fuera de rango
     */
    @Override
    public void sendMessage(int from, int to, String payload) {
        validateIndices(from, to);
        Message msg = new Message(from, to, payload);
        // Broadcast: enviamos a todos, pero Node.run filtrara segun destino
        for (Node node : nodes) {
            sendExecutor.submit(() -> {
                // Log opcion: podriamos indicar que enviamos el broadcast
                System.out.printf("[%s] BusNetwork: nodo %d enviando broadcast mensaje %d->%d%n",
                        Instant.now(), from, msg.getFromId(), msg.getToId());
                node.receiveMessage(msg);
            });
        }
    }

    /**
     * Inicia la simulacion de la red: somete cada nodo al executor dedicado.
     * Cada Node.run() correra en paralelo leyendo su inbox.
     *
     * @throws IllegalStateException si la topologia no esta configurada
     */
    @Override
    public void runNetwork() {
        // Iniciamos cada nodo en su propio hilo
        for (Node node : nodes) {
            nodeExecutor.submit(node);
        }
    }

    /**
     * Detiene la simulacion:
     * - Marca cada nodo para que deje de ejecutar (node.stop()).
     * - Cierra nodeExecutor y sendExecutor, esperando hasta 2 segundos
     *   para drenar tareas pendientes; si no termina, fuerza shutdownNow().
     */
    @Override
    public void shutdown() {
        // Paramos nodos
        for (Node node : nodes) {
            node.stop();
        }
        // Shutdown de executors
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
     * Verifica que la topologia este configurada y que los indices from y to
     * esten dentro de rango [0, size-1].
     *
     * @param from indice de origen
     * @param to indice de destino
     * @throws IllegalStateException si nodes es null (no configurada)
     * @throws IllegalArgumentException si from o to estan fuera de rango
     */
    private void validateIndices(int from, int to) {
        if (nodes == null) {
            throw new IllegalStateException("BusNetwork no esta configurada.");
        }
        int size = nodes.size();
        if (from < 0 || from >= size || to < 0 || to >= size) {
            throw new IllegalArgumentException(
                    "Indices fuera de rango en BusNetwork: from=" + from + " to=" + to);
        }
    }
}