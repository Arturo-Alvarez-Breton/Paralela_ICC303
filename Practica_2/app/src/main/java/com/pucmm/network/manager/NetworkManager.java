package com.pucmm.network.manager;

import com.pucmm.network.core.NetworkTopology;

/**
 * Gestor generico para las topologias de red.
 * Orquesta configuracion, ejecucion, envio de mensajes y detencion.
 */
public class NetworkManager {
    private NetworkTopology topology;
    private int nodeCount;

    /**
     * Configura en base a la topologia y numero de nodos.
     * 
     * @param topology implementacion de NetworkTopology
     * @param numberOfNodes cantidad de nodos a crear
     */
    public void configureNetwork(NetworkTopology topology, int numberOfNodes) {
        this.topology = topology;
        this.nodeCount = numberOfNodes;
        topology.configureNetwork(numberOfNodes);
    }

    /**
     * Inicia la simulacion.
     */
    public void runNetwork() {
        if (topology == null) {
            throw new IllegalStateException("NetworkTopology no esta configurada.");
        }
        topology.runNetwork();
    }

    /**
     * Envia un mensaje entre dos nodos.
     */
    public void sendMessage(int from, int to, String message) {
        if (topology == null) {
            throw new IllegalStateException("NetworkTopology no esta configurada.");
        }
        topology.sendMessage(from, to, message);
    }

    /**
     * Detiene la simulacion y libera recursos.
     */
    public void shutdown() {
        if (topology == null) {
            throw new IllegalStateException("NetworkTopology no esta configurada.");
        }
        topology.shutdown();
    }
}