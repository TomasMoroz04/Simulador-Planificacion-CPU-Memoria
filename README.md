# 🖥️ Simulador de Planificación de CPU y Paginación de Memoria

<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white" alt="Apache Maven">
  <img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white" alt="Hibernate">
  <img src="https://img.shields.io/badge/H2%20Database-004088?style=for-the-badge&logo=databricks&logoColor=white" alt="H2 Database">
  <img src="https://img.shields.io/badge/GUI-Java%20Swing-blue?style=for-the-badge" alt="Java Swing">
</p>

Simulador interactivo y multihilo de **Sistemas Operativos**, desarrollado en Java. El proyecto modela el funcionamiento interno del sistema operativo mediante la **planificación de procesos a corto plazo**, la **gestión de memoria virtual mediante paginación bajo demanda (MMU)** y la **sincronización de procesos concurrentes**.

Además, integra patrones de diseño de software, persistencia relacional mediante ORM, una interfaz gráfica de escritorio y generación de reportes estadísticos en PDF.

---

## 📑 Índice

- [🚀 Características principales](#-características-principales)
  - [Planificación de CPU](#1-planificación-de-cpu)
  - [Gestión de memoria y MMU](#2-gestión-de-memoria-y-mmu)
  - [Concurrencia y sincronización](#3-concurrencia-y-sincronización-en-tiempo-real)
  - [Persistencia y base de datos](#4-persistencia-orm-y-base-de-datos-embebida)
  - [Interfaz gráfica y reportes](#5-interfaz-gráfica-mvc-y-reportes-ejecutivos-pdf)
- [🛠️ Stack tecnológico](#️-stack-tecnológico-y-arquitectura)
- [📂 Estructura del proyecto](#-estructura-del-proyecto)
- [⚙️ Instalación y ejecución](#️-instalación-y-ejecución)
- [🗄️ Acceso a la base de datos H2](#️-acceso-a-la-base-de-datos-h2-en-vivo)
- [📊 Fórmulas y métricas](#-fórmulas-y-métricas-calculadas)

---

## 🚀 Características principales

### 1. Planificación de CPU

Implementación polimórfica de algoritmos clásicos de planificación mediante el **patrón Strategy**, utilizando la interfaz `PlanificadorStrategy`.

| Algoritmo | Descripción |
|---|---|
| **FCFS** (First-Come, First-Served) | Planificación no apropiativa que ejecuta los procesos según su orden de llegada. |
| **SJF** (Shortest Job First) | Planificación no apropiativa que selecciona el proceso con la ráfaga de CPU más corta. |
| **SRTF** (Shortest Remaining Time First) | Variante apropiativa de SJF que desaloja la CPU cuando ingresa un proceso con menor tiempo restante. |
| **Round Robin (RR)** | Planificación apropiativa que utiliza un *quantum* configurable y una cola circular. |

**Modo de comparación completa:** permite ejecutar secuencialmente los cuatro algoritmos sobre el mismo conjunto de procesos para comparar sus resultados y métricas de rendimiento.

### 2. Gestión de memoria y MMU

Implementación de un sistema de memoria virtual mediante **paginación bajo demanda**, con traducción de direcciones y administración de marcos físicos.

- **División lógica y física:** cálculo automático de las páginas necesarias para cada proceso, asignación de marcos físicos en RAM mediante `MarcoRAM` y medición de la fragmentación interna.
- **Unidad de Manejo de Memoria (MMU):** traducción de direcciones lógicas a direcciones físicas en hexadecimal durante cada *tick* de simulación.
- **Traducción de direcciones:** conversión de página lógica y desplazamiento en una dirección física a partir del marco de RAM asignado.
- **Fallos de página (*Page Faults*):** detección de páginas que no se encuentran cargadas en memoria principal.
- **Memoria virtual y *swapping*:** admisión de procesos incluso cuando la RAM está llena, utilizando memoria secundaria para almacenar páginas temporalmente.
- **Algoritmos de reemplazo de páginas:**
  - **FIFO** (*First-In, First-Out*): reemplaza la página que lleva más tiempo en memoria.
  - **LRU** (*Least Recently Used*): reemplaza la página que lleva más tiempo sin utilizarse.

### 3. Concurrencia y sincronización en tiempo real

El proyecto incorpora mecanismos de sincronización para simular la interacción entre procesos e hilos concurrentes.

#### 🔄 Problema del productor-consumidor

Implementación mediante:

- `HiloProductor`: genera e inyecta procesos en tiempo real, respetando sus tiempos de llegada.
- `BufferProcesos`: administra una cola compartida y sincronizada.
- Semáforos contadores: `vacios` y `llenos`, para controlar la disponibilidad de espacios y elementos.
- Semáforo binario `mutex`: protege el acceso concurrente al búfer.

#### 🖨️ Exclusión mutua y sección crítica

La clase `ImpresoraVirtual` representa un recurso compartido de entrada/salida (E/S). Su acceso está protegido mediante un **mutex**, implementado con `java.util.concurrent.Semaphore`, para ilustrar la exclusión mutua y la coordinación de procesos concurrentes.

### 4. Persistencia ORM y base de datos embebida

El simulador incorpora una capa de persistencia basada en **JPA, Hibernate y H2 Database**.

- **JPA + Hibernate:** mapeo objeto-relacional de la entidad `Proceso`, separando los atributos persistentes (`@Id`, `@Basic`) de los atributos transitorios del estado de simulación (`@Transient`).
- **Patrón DAO:** abstracción del acceso a datos mediante `IProcesoDao` y `ProcesoDaoImpl`.
- **Consultas JPQL:** recuperación y manipulación de los datos persistidos.
- **Base de datos H2:** almacenamiento relacional portable que no requiere instalar un servidor de base de datos externo.
- **Consola web H2:** servidor web integrado que se inicia automáticamente en el puerto `8082`.
- **Reconstrucción del estado:** recuperación de los atributos `@Transient` necesarios para la simulación mediante la implementación DAO.

### 5. Interfaz gráfica MVC y reportes ejecutivos PDF

#### 🖥️ Interfaz gráfica

La aplicación utiliza una arquitectura **Modelo-Vista-Controlador (MVC)** para separar las responsabilidades del sistema.

- **Modelo:** representa los procesos, sus estados, la memoria y las métricas.
- **Vista:** interfaz de escritorio desarrollada con Java Swing.
- **Controlador:** administra los eventos de la interfaz y coordina la ejecución de las simulaciones.
- **Ejecución concurrente:** permite realizar simulaciones en hilos de trabajo en segundo plano, evitando bloquear la interfaz gráfica.
- **Consola integrada:** captura y presenta la salida estándar (`System.out`) de manera segura mediante el *Event Dispatch Thread* (EDT).

#### 📄 Exportación de reportes PDF

Integración de **JFreeChart** e **iText 5** para generar reportes estadísticos opcionales en la carpeta `Reportes Simulacion/`.

Los reportes pueden incluir:

- Tablas detalladas con las métricas de cada proceso.
- Promedios generales de rendimiento.
- Diagramas de Gantt de la planificación.
- Gráficos de barras comparativos de tiempos de retorno y espera.

---

## 🛠️ Stack tecnológico y arquitectura

| Capa / Módulo | Tecnologías y patrones aplicados |
|---|---|
| **Lenguaje y construcción** | Java (JDK 17+) y Apache Maven |
| **Arquitectura general** | MVC (Modelo-Vista-Controlador) |
| **Planificación de CPU** | Patrón Strategy |
| **Concurrencia** | `Thread`, `Semaphore`, mutex y semáforos contadores |
| **Persistencia** | Patrón DAO, JPA (`javax.persistence`) e Hibernate Core 5.6 |
| **Base de datos** | H2 Database 2.2 |
| **Interfaz gráfica (GUI)** | Java Swing: `JFrame`, `BorderLayout`, `GridLayout` y `SwingUtilities` |
| **Reportes PDF** | iTextPDF 5.5 |
| **Gráficos estadísticos** | JFreeChart 1.5 |
| **Memoria virtual** | MMU, paginación bajo demanda, FIFO y LRU |

---

## 📂 Estructura del proyecto

```text
simulador/
├── Main.java                         # Punto de entrada: inicia H2, el DAO y la arquitectura MVC
│
├── concurrencia/
│   ├── BufferProcesos.java           # Cola compartida protegida mediante semáforos
│   ├── HiloProductor.java             # Hilo que inyecta procesos según su tiempo de llegada
│   └── ImpresoraVirtual.java          # Recurso de E/S protegido por mutex
│
├── controlador/
│   └── ControladorSimulador.java      # Gestiona eventos de la GUI y lanza simulaciones
│
├── dao/
│   ├── IProcesoDao.java               # Contrato de operaciones CRUD sobre procesos
│   └── ProcesoDaoImpl.java            # Implementación JPA/Hibernate y reconstrucción de estado
│
├── memoria/
│   └── GestorMemoria.java             # RAM, MMU, fallos de página y reemplazo FIFO/LRU
│
├── modelo/
│   ├── EntradaTablaPagina.java        # Página lógica, bit de presencia y marco asignado
│   ├── EstadoProceso.java             # Estados del ciclo de vida de un proceso
│   ├── MarcoRAM.java                  # Representación de un marco físico de RAM
│   └── Proceso.java                   # Entidad JPA y Bloque de Control de Proceso (PCB)
│
├── motor/
│   └── MotorSimulacion.java           # Reloj global, despacho de CPU, trazabilidad y Gantt
│
├── planificador/
│   ├── PlanificadorStrategy.java      # Interfaz común de los algoritmos de planificación
│   ├── PlanificadorFCFS.java          # First-Come, First-Served
│   ├── PlanificadorSJF.java           # Shortest Job First (no apropiativo)
│   ├── PlanificadorSRTF.java          # Shortest Remaining Time First (apropiativo)
│   └── PlanificadorRoundRobin.java    # Round Robin con quantum configurable
│
├── reportes/
│   └── GeneradorReporte.java          # Gráficos estadísticos y generación de PDF
│
└── vista/
    └── VentanaPrincipal.java          # Interfaz Swing con consola en tiempo real
```

---

## ⚙️ Instalación y ejecución

### ✅ Requisitos previos

Antes de ejecutar el proyecto, asegurate de tener instalados:

- **Java JDK 17 o superior.**
- **Apache Maven.**
- **Git**, para clonar el repositorio.

Podés verificar las instalaciones con los siguientes comandos:

```bash
java -version
mvn -version
git --version
```

### 1. Clonar el repositorio

Reemplazá `tu-usuario` por el nombre de usuario o la organización propietaria del repositorio.

```bash
git clone https://github.com/tu-usuario/simulador-planificacion-cpu-memoria.git

cd simulador-planificacion-cpu-memoria
```

### 2. Compilar el proyecto

Ejecutá Maven para limpiar, compilar y empaquetar el proyecto:

```bash
mvn clean install
```

### 3. Ejecutar la aplicación

Iniciá el simulador mediante Maven:

```bash
mvn exec:java -Dexec.mainClass="simulador.Main"
```

> **Nota:** el comando `mvn exec:java` requiere que el proyecto tenga configurado el plugin `exec-maven-plugin` en el archivo `pom.xml`.

También podés abrir el proyecto desde un IDE compatible con Java, como NetBeans, e iniciar la clase `simulador.Main`.

---

## 🗄️ Acceso a la base de datos H2 en vivo

Mientras el simulador está en ejecución, se inicia automáticamente un servidor web para inspeccionar la base de datos relacional.

### 1. Abrir la consola

Ingresá desde tu navegador a la siguiente dirección:

**[http://localhost:8082](http://localhost:8082)**

### 2. Configurar la conexión

Introducí los siguientes parámetros:

| Parámetro | Valor |
|---|---|
| **Driver Class** | `org.h2.Driver` |
| **JDBC URL** | `jdbc:h2:./simuladorDB` |
| **User Name** | `sa` |
| **Password** | Dejar en blanco |

### 3. Consultar los procesos

Una vez establecida la conexión, ejecutá la siguiente consulta SQL:

```sql
SELECT * FROM PROCESO;
```

Esto permite visualizar los registros de procesos almacenados en la base de datos.

---

## 📊 Fórmulas y métricas calculadas

Al finalizar cada simulación, el sistema calcula distintas métricas para evaluar el rendimiento de los algoritmos de planificación y la utilización de la memoria.

### ⏱️ Tiempo de retorno

Representa el tiempo total transcurrido desde la llegada de un proceso hasta su finalización.

<p align="center">
  <strong>T<sub>r</sub> = T<sub>f</sub> − T<sub>l</sub></strong>
</p>

**Donde:**

- <strong>T<sub>r</sub></strong>: tiempo de retorno.
- <strong>T<sub>f</sub></strong>: tiempo de finalización.
- <strong>T<sub>l</sub></strong>: tiempo de llegada.

### ⏳ Tiempo de espera

Representa el tiempo durante el cual un proceso permanece esperando para utilizar la CPU, considerando una única ráfaga de CPU y sin contabilizar tiempos adicionales de E/S.

<p align="center">
  <strong>T<sub>e</sub> = T<sub>r</sub> − T<sub>CPU</sub></strong>
</p>

**Donde:**

- <strong>T<sub>e</sub></strong>: tiempo de espera.
- <strong>T<sub>r</sub></strong>: tiempo de retorno.
- <strong>T<sub>CPU</sub></strong>: duración de la ráfaga de CPU.

### 🧠 Fragmentación interna

Representa el espacio de memoria asignado que no es utilizado por el proceso debido a la división de la memoria en páginas de tamaño fijo.

<p align="center">
  <strong>F<sub>i</sub> = (N<sub>p</sub> × T<sub>p</sub>) − T<sub>proceso</sub></strong>
</p>

**Donde:**

- <strong>F<sub>i</sub></strong>: fragmentación interna, en bytes.
- <strong>N<sub>p</sub></strong>: cantidad de páginas asignadas.
- <strong>T<sub>p</sub></strong>: tamaño de página, en bytes.
- <strong>T<sub>proceso</sub></strong>: tamaño del proceso, en bytes.

### 📈 Otras métricas

El simulador también registra:

- **Fallos de página (*Page Faults*):** cantidad de veces que se necesita acceder a una página que no está presente en RAM.
- **Reemplazos de páginas:** cantidad de reemplazos de marcos físicos mediante los algoritmos FIFO o LRU.
- **Diagramas de Gantt:** representación temporal de la ejecución de los procesos según el algoritmo seleccionado.
- **Tiempos de retorno y espera:** valores individuales y promedios que permiten comparar el rendimiento de los planificadores.

---

## 🎯 Objetivo del proyecto

El objetivo es integrar y visualizar conceptos fundamentales de **Sistemas Operativos**, permitiendo experimentar con algoritmos de planificación, memoria virtual, concurrencia, sincronización y persistencia de datos en una aplicación interactiva.

A través de la simulación y la comparación de resultados, el proyecto facilita la comprensión de cómo las decisiones de planificación y administración de recursos influyen en el rendimiento general de un sistema operativo.

---

<p align="center">
  <b>Desarrollado en Java ☕ | Sistemas Operativos | Planificación de CPU y Gestión de Memoria</b>
</p>
