package com.pucmm.network;

import com.pucmm.network.manager.NetworkManager;
import com.pucmm.network.topologia.BusNetwork;
import com.pucmm.network.topologia.RingNetwork;
import com.pucmm.network.topologia.StarNetwork;
import com.pucmm.network.topologia.MeshNetwork;
import com.pucmm.network.topologia.TreeNetwork;
import com.pucmm.network.topologia.FullyConnectedNetwork;
import com.pucmm.network.topologia.HypercubeNetwork;
import com.pucmm.network.topologia.SwitchedNetwork;

public class Main {
    public static void main(String[] args) {
        NetworkManager manager = new NetworkManager();

        // Ajusta tiempos si quieres probar distinto comportamiento
        final long PREPARE_WAIT = 100;   
        final long SHORT_WAIT = 500;
        final long MEDIUM_WAIT = 2000;  
        final long LONG_WAIT = 3000;    

        // 1. BusNetwork
        System.out.println("=== Testing BusNetwork ===");
        manager.configureNetwork(new BusNetwork(), 5);
        manager.runNetwork();
        sleepMs(PREPARE_WAIT);
        manager.sendMessage(0, 1, "Bus 0->1");
        manager.sendMessage(2, 4, "Bus 2->4");
        manager.sendMessage(3, 0, "Bus 3->0");
        sleepMs(MEDIUM_WAIT);
        manager.shutdown();
        sleepMs(SHORT_WAIT);

        // 2. RingNetwork
        System.out.println("\n=== Testing RingNetwork ===");
        manager.configureNetwork(new RingNetwork(), 5);
        manager.runNetwork();
        sleepMs(PREPARE_WAIT);
        manager.sendMessage(0, 3, "Ring 0->3");
        manager.sendMessage(4, 1, "Ring 4->1");
        sleepMs(MEDIUM_WAIT);
        manager.shutdown();
        sleepMs(SHORT_WAIT);

        // 3. MeshNetwork
        System.out.println("\n=== Testing MeshNetwork ===");
        manager.configureNetwork(new MeshNetwork(), 4);
        manager.runNetwork();
        sleepMs(PREPARE_WAIT);
        manager.sendMessage(0, 3, "Mesh 0->3");
        manager.sendMessage(3, 1, "Mesh 3->1");
        sleepMs(MEDIUM_WAIT);
        manager.shutdown();
        sleepMs(SHORT_WAIT);

        // 4. StarNetwork
        System.out.println("\n=== Testing StarNetwork ===");
        manager.configureNetwork(new StarNetwork(), 5);
        manager.runNetwork();
        sleepMs(PREPARE_WAIT);
        manager.sendMessage(0, 2, "Star 0->2");
        manager.sendMessage(3, 0, "Star 3->0");
        manager.sendMessage(4, 1, "Star 4->1");
        manager.sendMessage(2, 3, "Star 2->3");
        sleepMs(MEDIUM_WAIT);
        manager.shutdown();
        sleepMs(SHORT_WAIT);

        // 5. FullyConnectedNetwork
        System.out.println("\n=== Testing FullyConnectedNetwork ===");
        manager.configureNetwork(new FullyConnectedNetwork(), 4);
        manager.runNetwork();
        sleepMs(PREPARE_WAIT);
        manager.sendMessage(1, 3, "Fully 1->3");
        manager.sendMessage(2, 0, "Fully 2->0");
        sleepMs(MEDIUM_WAIT);
        manager.shutdown();
        sleepMs(SHORT_WAIT);

        // 6. HypercubeNetwork (8 nodos)
        System.out.println("\n=== Testing HypercubeNetwork ===");
        manager.configureNetwork(new HypercubeNetwork(), 8);
        manager.runNetwork();
        sleepMs(PREPARE_WAIT);
        manager.sendMessage(0, 7, "Hypercube 0->7");
        manager.sendMessage(3, 5, "Hypercube 3->5");
        sleepMs(LONG_WAIT);
        manager.shutdown();
        sleepMs(SHORT_WAIT);

        // 7. TreeNetwork
        System.out.println("\n=== Testing TreeNetwork ===");
        manager.configureNetwork(new TreeNetwork(), 7);
        manager.runNetwork();
        sleepMs(PREPARE_WAIT);
        manager.sendMessage(5, 2, "Tree 5->2");
        manager.sendMessage(6, 4, "Tree 6->4");
        manager.sendMessage(3, 3, "Tree 3->3");
        sleepMs(LONG_WAIT);
        manager.shutdown();
        sleepMs(SHORT_WAIT);

        // 8. SwitchedNetwork
        System.out.println("\n=== Testing SwitchedNetwork ===");
        manager.configureNetwork(new SwitchedNetwork(), 5);
        manager.runNetwork();
        sleepMs(PREPARE_WAIT);
        manager.sendMessage(2, 4, "Switch 2->4");
        manager.sendMessage(0, 3, "Switch 0->3");
        manager.sendMessage(4, 1, "Switch 4->1");
        sleepMs(MEDIUM_WAIT);
        manager.shutdown();
        sleepMs(SHORT_WAIT);

        System.out.println("=== All tests completed ===");
    }

    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}