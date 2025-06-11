package com.pucmm.network.topologia;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.pucmm.network.core.Message;
import com.pucmm.network.core.NetworkTopology;
import com.pucmm.network.core.Node;

/**
 * Topologia de arbol binario: nodos indicados 0..n-1, padre de i = (i-1)/2 si i>0.
 */
public class TreeNetwork implements NetworkTopology {
    private List<Node> nodes;
    private ExecutorService nodeExecutor;
    private ExecutorService sendExecutor;
    // latencia entre hops
    private final long hopLatencyMs = 100;

    /**
     * Configura la red de arbol con numberOfNodes nodos.
     * Requiere al menos 1 nodo.
     *
     * @param numberOfNodes cantidad de nodos en el arbol
     * @throws IllegalArgumentException si numberOfNodes < 1
     */
    @Override
    public void configureNetwork(int numberOfNodes) {
        if (numberOfNodes < 1) {
            throw new IllegalArgumentException("TreeNetwork requiere al menos 1 nodo");
        }
        nodes = new ArrayList<>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            nodes.add(new Node(i));
        }
        nodeExecutor = Executors.newFixedThreadPool(numberOfNodes);
        sendExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Envia un mensaje desde 'from' hacia 'to' en la topologia de arbol.
     * Calcula la ruta via LCA:
     * - Construye lista de ancestros de 'from' hasta raiz (excluyendo 'from').
     * - Construye lista de ancestros de 'to' hasta raiz.
     * - Encuentra el LCA mas cercano a 'from'.
     * - Crea ruta: sube desde 'from' hasta LCA, luego baja hasta 'to'.
     * - En cada hop se hace receiveMessage y se duerme hopLatencyMs.
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
            System.out.printf("[%s] TreeNetwork: nodo %d iniciando ruteo hacia %d%n",
                    Instant.now(), from, to);
            if (from == to) {
                nodes.get(to).receiveMessage(msg);
                System.out.printf("[%s] TreeNetwork: mensaje de %d a %d entregado directo%n",
                        Instant.now(), from, to);
                return;
            }
            // calculamos ruta via LCA
            List<Integer> pathUp = new ArrayList<>();
            int nodeUp = from;
            while (nodeUp > 0) {
                nodeUp = (nodeUp - 1) / 2;
                pathUp.add(nodeUp);
            }
            List<Integer> pathTo = new ArrayList<>();
            int nodeTo = to;
            while (nodeTo > 0) {
                nodeTo = (nodeTo - 1) / 2;
                pathTo.add(nodeTo);
            }
            // encontrar LCA: primer ancestro de pathUp que este en pathTo
            int lca = 0;
            for (int ancUp : pathUp) {
                if (pathTo.contains(ancUp)) {
                    lca = ancUp;
                    break;
                }
            }
            // ruta desde 'from' hasta lca
            List<Integer> route = new ArrayList<>();
            int current = from;
            while (current != lca) {
                current = (current - 1) / 2;
                route.add(current);
            }
            // luego bajamos de lca a 'to'
            Deque<Integer> stack = new ArrayDeque<>();
            int temp = to;
            while (temp != lca) {
                stack.push(temp);
                temp = (temp - 1) / 2;
            }
            while (!stack.isEmpty()) {
                route.add(stack.pop());
            }
            // enviamos por la ruta
            for (int hop : route) {
                System.out.printf("[%s] TreeNetwork: reenviando mensaje hacia nodo %d%n",
                        Instant.now(), hop);
                nodes.get(hop).receiveMessage(msg);
                try {
                    Thread.sleep(hopLatencyMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.printf("[%s] TreeNetwork: ruteo de %d a %d completado%n", Instant.now(), from, to);
        });
    }

     /**
     * Inicia la simulacion: ejecuta Node.run() de cada nodo.
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
     * - Cierra executors esperando hasta 4s antes de forzar
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
     * @param from indice de origen
     * @param to indice de destino
     * @throws IllegalStateException si topologia no configurada
     * @throws IllegalArgumentException si indices fuera de rango
     */
    private void validateIndices(int from, int to) {
        if (nodes == null) {
            throw new IllegalStateException("TreeNetwork no esta configurada.");
        }
        int size = nodes.size();
        if (from < 0 || from >= size || to < 0 || to >= size) {
            throw new IllegalArgumentException(
                    "Indices fuera de rango en TreeNetwork: from=" + from + " to=" + to);
        }
    }
}