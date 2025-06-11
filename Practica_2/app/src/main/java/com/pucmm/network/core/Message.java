package com.pucmm.network.core;

import java.time.Instant;

/**
 * Clase inmutable que representa un mensaje con origen, destino, contenido y timestamp.
 */
public class Message {
    private final int fromId;
    private final int toId;
    private final String payload;
    private final Instant timestamp;

    /**
     * Construye un mensaje con origen, destino y contenido.
     * 
     * @param fromId    identificador del nodo emisor
     * @param toId      identificador del nodo receptor
     * @param payload   contenido del mensaje
     */
    public Message(int fromId, int toId, String payload) {
        this.fromId = fromId;
        this.toId = toId;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    // Getters
    public int getFromId() { return fromId; }
    public int getToId() { return toId; }
    public String getPayload() { return payload; }
    public Instant getTimestamp() { return timestamp; }
}