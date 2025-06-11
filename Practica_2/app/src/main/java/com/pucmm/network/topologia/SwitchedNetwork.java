package com.pucmm.network.topologia;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.pucmm.network.core.Message;
import com.pucmm.network.core.NetworkTopology;
import com.pucmm.network.core.Node;

/**
 * Topologia con switch central: los nodos envian mensajes al switch,
 * el switch los procesa y reenvia al destino final.
 * Se ejecuta un hilo dedicado para el switch y un pool para nodos.
 */
public class SwitchedNetwork implements NetworkTopology {
    private List<Node> nodes;
    private ExecutorService nodeExecutor;
    private ExecutorService switchExecutor;
    private BlockingQueue<Message> switchQueue;
    private volatile boolean switchRunning;

    /**
     * Configura la red con numberOfNodes nodos.
     * Crea nodos ids 0..numberOfNodes-1, inicializa nodeExecutor y switchExecutor,
     * inicia la tarea del switch que lee de switchQueue y entrega a destino.
     *
     * @param numberOfNodes cantidad de nodos en la red
     * @throws IllegalArgumentException si numberOfNodes < 1
     */
    @Override
    public void configureNetwork(int numberOfNodes) {
        if (numberOfNodes < 1) {
            throw new IllegalArgumentException("SwitchedNetwork requiere al menos 1 nodo");
        }
        nodes = new ArrayList<>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            nodes.add(new Node(i));
        }
        nodeExecutor = Executors.newFixedThreadPool(numberOfNodes);
        switchExecutor = Executors.newSingleThreadExecutor();
        switchQueue = new LinkedBlockingQueue<>();
        switchRunning = true;
        // iniciar proceso del switch
        switchExecutor.submit(() -> {
            while (switchRunning || !switchQueue.isEmpty()) {
                try {
                    Message msg = switchQueue.poll(500, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        int to = msg.getToId();
                        // Validar indices antes de entregar
                        if (to < 0 || to >= nodes.size()) {
                            System.err.printf("SwitchedNetwork: destino fuera de rango: %d%n", to);
                        } else {
                            System.out.printf("[%s] SwitchedNetwork: switch reenviando a %d%n",
                                    Instant.now(), to);
                            nodes.get(to).receiveMessage(msg);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * Envia un mensaje desde 'from' hacia 'to' a traves del switch.
     * Valida indices y pone el mensaje en la cola switchQueue.
     *
     * @param from indice de origen
     * @param to indice de destino
     * @param payload contenido del mensaje
     * @throws IllegalStateException si topologia no configurada
     * @throws IllegalArgumentException si from o to fuera de rango
     */
    @Override
    public void sendMessage(int from, int to, String payload) {
        if (nodes == null) {
            throw new IllegalStateException("SwitchedNetwork no esta configurada.");
        }
        int size = nodes.size();
        if (from < 0 || from >= size || to < 0 || to >= size) {
            throw new IllegalArgumentException(
                    "Indices fuera de rango en SwitchedNetwork: from=" + from + " to=" + to);
        }
        Message msg = new Message(from, to, payload);
        System.out.printf("[%s] SwitchedNetwork: nodo %d enviando al switch mensaje hacia %d%n",
                Instant.now(), from, to);
        switchQueue.offer(msg);
    }

    /**
     * Inicia la simulacion: ejecuta Node.run() de cada nodo.
     *
     * @throws IllegalStateException si topologia no configurada
     */
    @Override
    public void runNetwork() {
        for (Node node : nodes) {
            nodeExecutor.submit(node);
        }
    }

    /**
     * Detiene la simulacion:
     * - Marca stop() a cada nodo
     * - Señala switchRunning=false para terminar el bucle del switch
     * - Cierra nodeExecutor y switchExecutor, esperando hasta 3s antes de forzar
     */
    @Override
    public void shutdown() {
        // detener nodos
        for (Node node : nodes) {
            node.stop();
        }
        // detener switch
        switchRunning = false;
        nodeExecutor.shutdown();
        switchExecutor.shutdown();
        try {
            if (!switchExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                switchExecutor.shutdownNow();
            }
            if (!nodeExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                nodeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            switchExecutor.shutdownNow();
            nodeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}