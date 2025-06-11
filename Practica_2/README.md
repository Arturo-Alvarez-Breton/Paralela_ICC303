# Simulador de Topologias de Red (Arturo Alvarez - 10149008)
Este proyecto en Java permite simular distintas topologias de red con concurrencia. Cada topologia implementa una interfaz comun y usa nodos que procesan mensajes en paralelo.

## Estructura de paquetes
├── Main.java
├── manager
│ └── NetworkManager.java
├── core
│ ├── NetworkTopology.java
│ ├── Node.java
│ └── Message.java
└── topologia
├── BusNetwork.java
├── MeshNetwork.java
├── FullyConnectedNetwork.java
├── RingNetwork.java
├── StarNetwork.java
├── HypercubeNetwork.java
├── TreeNetwork.java
└── SwitchedNetwork.java

- **core**: clases base:
  - `NetworkTopology`: interfaz con metodos `configureNetwork`, `sendMessage`, `runNetwork`, `shutdown`.
  - `Node`: cada nodo con su cola de mensajes, procesa solo si es destino
  - `Message`: inmutable con fromId, toId, payload y timestamp

- **manager**:
  - `NetworkManager`: orquesta la topologia: configura, inicia, envia mensajes y detiene la red

- **topologia**: cada implementacion de red:
  - `BusNetwork`: broadcast a todos, cada nodo filtra internamente
  - `MeshNetwork` / `FullyConnectedNetwork`: envio directo par a par
  - `RingNetwork`: ruteo unidireccional en anillo
  - `StarNetwork`: nodo central conecta hojas; hoja-hoja via central
  - `HypercubeNetwork`: ids 0..2^d-1, ruteo bit a bit
  - `TreeNetwork`: arbol binario implicito, ruteo via LCA
  - `SwitchedNetwork`: switch central con cola y reenvio

- **Main.java**: ejemplo de uso que prueba cada topologia en secuencia, con esperas para procesar mensajes

## Requisitos
- JDK 11 o superior
- IDE opcional (Eclipse, IntelliJ, VSCode) o Maven/Gradle
- Git para version de control

## Uso rapido
1. Clonar o copiar el proyecto.
2. Importar en IDE o compilar con Maven/Gradle o javac:
   ```bash
   javac -d bin src/.../*.java
   java -cp bin com.pucmm.network.Main
En `Main.java` ajustar sleeps si cambia la alntencia

