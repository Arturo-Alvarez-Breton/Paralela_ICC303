package com.pucmm.network.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Clase que representa un nodo dentro de la red.
 * Cada nodo ejecuta su propio hilo para procesar su bandeja de mensajes (inbox).
 */
public class Node implements Runnable {
    private final int id;
    private final BlockingQueue<Message> inbox;
    private volatile boolean running = true;

    /**
     * Constructor de un nodo con identificador unico.
     *
     * @param id identificador unico del nodo
     */
    public Node(int id) {
        this.id = id;
        this.inbox = new LinkedBlockingDeque<>();
    }

    // Getter
    public int getId() {
        return id;
    }

    /**
     * Recibe un mensaje y lo pone en la cola para procesar.
     *
     * @param msg mensaje a procesar
     */
    public void receiveMessage(Message msg) {
        inbox.offer(msg);
    }

    /**
     * Marca el nodo para que deje de ejecutarse. Procesara lo que quede en la cola y luego saldra.
     */
    public void stop() {
        running = false;
    }

    /**
     * Simula la actividad del nodo: solo procesa mensajes cuyo destino es este nodo.
     * Si msg.getToId()!=id, se descarta sin log.
     */
    @Override
    public void run() {
        while (running || !inbox.isEmpty()) {
            try {
                Message msg = inbox.poll(500, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    if (msg.getToId() == this.id) {
                        // Procesamos solo si es destino final
                        // Mostrar timestamp original y id origen/destino
                        System.out.printf("[%s] Nodo %d procesando mensaje %d->%d: %s%n",
                                msg.getTimestamp(), id, msg.getFromId(), msg.getToId(), msg.getPayload());
                    }
                    // si no era para este nodo, ignoramos
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.printf("Nodo %d detenido.%n", id);
    }
}