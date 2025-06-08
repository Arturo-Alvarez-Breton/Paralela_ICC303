package com.pucmm.network.core;

/**
 * Clase que representa un nodo dentro de la red
 * Cada nodo puede ejecutar una tarea simulada y procesar mensajes
 */
public class Node implements Runnable {
    private final int id;

    /**
     * Constructor de un nodo con identificador.
     * 
     * @param id identificador unico del nodo
     */
    public Node(int id) {
        this.id = id;
    }

    /**
     * Obtiene el identificador del nodo.
     * 
     * @return id del nodo
     */
    public int getId() {
        return id;
    }

    /**
     * Simula la actividad del nodo (p.ej. procesamiento de mensajes).
     */
    @Override
    public void run() {
        try {
            // Simulacion de trabajo aleatorio
            Thread.sleep((long) (Math.random() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Procesa la recepción de un mensaje.
     * 
     * @param message contenido recibido
     */
    public void receiveMessage(String message) {
        System.out.printf("Nodo %d recibió mensaje: %s%n", id, message);
    }
}