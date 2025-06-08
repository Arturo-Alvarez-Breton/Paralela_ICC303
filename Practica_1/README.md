# Paralelización de Algoritmos y Análisis de Rendimiento
En este proyecto se implementan algoritmos secuenciales y paralelos para sumar un millón de números enteros generados aleatoriamente. El objetivo principal es comparar el rendimiento entre diferentes enfoques de procesamiento y analizar cómo la paralelización afecta el tiempo de ejecución.

## Requisitos el Sistema
- JDK: 21 o superior
- OS: Windows, Linux o MacOS

## Estructura del Código
- src/Main.java: Clase principal con la lógica de generación de datos y medición
- src/SumWorker.java: Implementación de hilos para suma paralela
- data/data.txt: Archivo generado con números aleatorios (1,000,000 registros)

## Ejecucion
- Corra el archivo Main.java
- Si desea cambiar actualzar los datos del archivo data.txt solamente debe borrarlo y se generara uno nuevo.
- Tambien puede usar uno existente cambiando el valor del String dataPath.

## Ejemplo de salida
![image](https://github.com/user-attachments/assets/0a7f5ede-eace-4c25-adbd-a489a7a3e557)

## Métricas calculadas:
- Speedup: Tiempo secuencial / Tiempo paralelo
- Eficiencia: Speedup / Número de hilos

## Resultados
Total = 4,999,565,950

| Número de Hilos | Tiempo Secuencial (s) | Tiempo Paralelo (s)   | Speedup | Eficiencia |
|-----------------|------------------------|----------------------|---------|------------|
| 1 (Secuencial)  | 0.003360               | -                    | 1.0000  | 1.0000     |
| 2               | 0.003360               | 0.006302             | 0.5332  | 0.2666     |
| 4               | 0.003360               | 0.005115             | 0.6568  | 0.1642     |
| 8               | 0.003360               | 0.001368             | 2.4561  | 0.3070     |
| 16              | 0.003360               | 0.001456             | 2.3077  | 0.1442     |
| 32              | 0.003360               | 0.002316             | 1.4512  | 0.0454     |


Parte 1 de la practica:
![PXL_20250601_013243521 RAW-01 COVER](https://github.com/user-attachments/assets/6b8cf3c0-f40a-44cb-8d35-4107eec74980)
