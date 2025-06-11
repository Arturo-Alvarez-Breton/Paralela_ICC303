package com.pucmm.network.core;
/**
 * Interfaz base para representar una topologia de red
 * Define la configuracion, ejecucion, envio de mensajes y apagado de una red
 */
public interface NetworkTopology {
    /**
     * Configura la red con un n nodos
     * 
     * @param numberOfNodes numero de nodos en la topologia
     */
    void configureNetwork(int numberOfNodes);

    /**
     * Envia un mensaje desde un nodo de origen a un nodo de destino
     * 
     * @param from      indice del nodo origen
     * @param to        indice del nodo destino
     * @param message   contenido del mensaje
     */
    void sendMessage(int from, int to, String message);

    /**
     * Incia la simulacion de la red
     * Los nodos comienzan su actividad concurrente
     */
    void runNetwork();

    /**
     * Detien al simulacion y libera los recursos asociados
     */
    void shutdown();
    
}
