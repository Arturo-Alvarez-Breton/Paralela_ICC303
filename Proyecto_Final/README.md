## **Este repo contendra el proyecto final de la materia Programacion Paralela y Concurrente ICC303. De momento el proyecto se encuentra en etatpa de desrrollo y se presenta un avance del mismo proyecto.**

### **Descripcion General:**
El proyecto consiste en desarrollar un sistema para la gestion y control de semaforos y trafico en dos escenarios distintos utilizando tecnicas de programacion paralela y concurrente en `Java`. Se debe implementar una aplicacion con interfaz grafica de `JavaFX` que permita simular el comportamiento del trafico en un cruce de calles con 4 intersecciones y en una autopista de dos direcciones con multiples semaforos. La plicacion debe asegurar que no ocurran colosiones y que los vehiculos de emergencia siempre tengan prioridad.

### **Escenarios del Proyecto**
- `Escenario 1:` Cruce de calles con 4 intersecciones
  - Cada interseccion tiene un "pare"
  - Los vehiculos deben cruzar en el orden que llegan a la interseccion
  - Los vehiculos de emergencia tienen prioridad sobre todos los demas vehiculos. Se debe considerar que para que un vehiculo de emergencia avance todos los que estan delante deben avanzar primero
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
