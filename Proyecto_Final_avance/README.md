# **Proyecto Final ICC303 Programacion Paralela y Concurrente**

Este repo contendra el proyecto final de la materia Programacion Paralela y Concurrente ICC303. 
De momento el proyecto se encuentra en etatpa de desrrollo y se presenta un avance del mismo proyecto.

## **Descripcion General:**
Este proyecto desarrolla un sistema de gestió y control de tráfico en un cruce de cuatro calles con señales de
"Pare", usando técnicas de programación paralela y concurrente en Java, con interfaz gráfica JavaFX. 
Se deben evitar colisiones y garantizar que los vehiculos de emergencia tengan prioridad.

## **Actores y entidades**
- **Vehicle (NORMAL / EMERGENCY):** Vehículo que llega al cruce con una dirección determinada (STRAIGHT, RIGHT, LEFT, U-TURN)
- **Intersection:** Punto de cruce con una cola concurrente de vehiculos segun las reglas
- **TrafficController:** Componente concurrente que despacha vehiculos segun las reglas
- **TraficLight:** Semafora para futura extencion (No es parte de esta entrega / avance)

## **Casos de Uso Clave**
1) **LLegaada de un Vehiculo** 
   - se crea un objeto Vehicle con atributos
     - `type`: NORMAL o EMERGENCY
     - `direction`: RIGHT, LEFT, STRAIGHT, U-TURN
     - `arrivalTime`: timestamp del momento en el que fue colocado
   - se invoca `Intersection.addVehicle(vehicle)` entonces se coloca en la `PriorityblockingQueue`


2) **Despacho de Vehiculo**
   - Ejecutado periodicamente por `TrafficController.manageIntersections()`
   - Se obtiene el proximo vehiculo `(poll())`segun comparador:
     1) EMERGENCY > NORMAL
     2) Si son del mismo tipo entonces menor `arrivalTime` (FIFO)
   - Antes de avanzar se confirma que no hay conflicto con otros vehiculos en la interseccion


3) **Confiracion de Cruce Libre**
   -  Se mantiene un mapa de carriles ocupados `Map<DirectionEnum, Boolean>`
   - Definir matriz de conflictos entre direcciones: un vehiculo solo avanza si:
     - Su carril de entrada y salida estan libres
     - No existe otro vehiculo en cola con mayor prioridad de giro que generaria colision
   - Al despachar, se marcan los carriles como ocupados, se liberan al completar la animacion.

## **Regals de Prioridad de Giro**

Lista los posibles movimientos simultaneos de los demas carros

| Direccion del vehiculo con prioirdad | Carro Opuesto           | Carro a la Derecha    | Carro a la Izquierda |
|--------------------------------------|-------------------------|-----------------------|----------------------|
| STRAIGHT                             | STRAIGHT, RIGHT         | X                     | RIGHT, U-TURN        |
| RIGHT                                | STRAIGHT, RIGHT, U-TURN | STRAIGHT, RIGHT, LEFT | RIGHT, U-TURN        |
| LEFT                                 | X                       | RIGHT, U-TURN         | RIGHT                |
| U-TURN                               | RIGHT, U-TURN           | STRAIGHT, RIGHT       | X                    |
Los vehiculos de emergencia siempre sobrepasan esta tabla



### **Escenarios del Proyecto**
- `Escenario 1:` Cruce de calles con 4 intersecciones
  - Cada interseccion tiene un "pare"
  - Los vehiculos deben cruzar en el orden que llegan a la interseccion
  - Los vehiculos de emergencia tienen prioridad sobre todos los demas vehiculos. Se debe considerar 
que para que un vehiculo de emergencia avance todos los que estan delante deben avanzar primero
  - Evitar colisiones entre vehiculos
  - Interfaz grafica con `JavaFX`

### **Componentes para entregar**
- Codigo fuente del proyecto el cual se encuentra en este repositorio publico de `GitHub`
- Documentacion detallada que incluya:
  - Descripcion del diseño del sistema
  - Explicacion de los algoritmos de control
  - Instrucciones para ejecutar la aplicacion
  - Capturas de pantalla de la interfaz de usuario
  - Resultados de pruebas y evaluacion de sistema

### **Evaluacion del Proyecto**
Esta se basara en:
- Correctitud y funcionalidad: Implementacion correcta de la logica de trafico y control de semaforos
- Uso de Tecnicas Concurrentes: Aplicacion efectiva de programacion paralela y concurrente
- Interfaz de Usuario: Usabilidad y claridad de la interfaz grafica
- prevencion de Colisiones: Correcta implementacion de la logica para evitar colisiones
- Documentacion: Claridad y detalle de la documentacion proporcionada

### **Sujerencias y Mejoras**
- Extension del Proyecto: Considerar agregar mas intersecciones y semaforos, o extender la autopista para simular un area de trafico mas grande
- Optimizacion del Rendimiento: Utilizar perfiles de rendimiento para identificar y optimizar cuellos de botella
- Funciones adicionales: Agregar funcionalidades como la simulacion de acccidentes y desvios, o la inclusion de transporte publico en la simulacion.
