package com.pucmm.network.topologia;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pucmm.network.core.NetworkTopology;
import com.pucmm.network.core.Node;

/**
 * Implementacion de la topologia de bus.
 * Todos los nodos comparten un medio de transmision comun.
 */
public class BusNetwork implements NetworkTopology{
    private List<Node> nodes;
    private ExecutorService executor;

    /**
     * Configura la red en topología de bus con un conjunto de nodos.
     * 
     * @param numberOfNodes cantidad de nodos a instanciar
     */
    @Override
    public void configureNetwork(int numberOfNodes){
        nodes = new ArrayList<>();
        
        for(int i = 0; i < numberOfNodes; i++){
            nodes.add(new Node(i));
        }

        executor = Executors.newFixedThreadPool(numberOfNodes);
    }

    /**
     * Envia un mensaje desde un nodo a otro en la topologia de bus.
     * El mensaje se procesa de forma asincrona.
     * 
     * @param from indice del nodo origen
     * @param to indice del nodo destino
     * @param message contenido a transmitir
     */
    @Override
    public void sendMessage(int from, int to, String message){
        // ...
        executor.submit(() -> nodes.get(to).receiveMessage(
            String.format("[Desde %d]: %s", from, message)
        ));
    }

    /**
     * Inicia la simulacion de la red lanzando la ejecucion de cada nodo
     */
    @Override
    public void runNetwork() {
         for (Node node : nodes) {
            executor.submit(node);
        }
    }

    /**
     * Apaga el executor y detiene todas las tareas en ejecucion.
     */
    @Override
    public void shutdown(){
        executor.shutdown();
    }
    
}
