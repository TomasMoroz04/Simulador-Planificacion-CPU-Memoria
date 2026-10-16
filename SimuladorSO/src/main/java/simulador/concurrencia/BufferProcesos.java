package simulador.concurrencia;

import simulador.modelo.Proceso;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class BufferProcesos {
    private final Queue<Proceso> cola = new LinkedList<>();
    
    // Semáforo Binario (Mutex) para garantizar exclusión mutua en la cola
    private final Semaphore mutex = new Semaphore(1);
    
    // Semáforos Enteros para sincronización
    private final Semaphore vacios; 
    private final Semaphore llenos = new Semaphore(0);

    public BufferProcesos(int capacidadMaxima) {
        this.vacios = new Semaphore(capacidadMaxima);
    }

    public void producir(Proceso p) throws InterruptedException {
        vacios.acquire();  // Operación Wait(S) o Down: Bloquea si el buffer está lleno
        mutex.acquire();   // Entra a la Sección Crítica
        
        cola.add(p);
        
        mutex.release();   // Operación Signal(S) o Up: Sale de la Sección Crítica
        llenos.release();  // Avisa al consumidor que hay un nuevo elemento disponible
    }

    public Proceso consumir() {
        // Se usa tryAcquire() para que el reloj de la CPU (Consumidor) no se detenga si no hay procesos listos
        if (llenos.tryAcquire()) {
            try {
                mutex.acquire();
                Proceso p = cola.poll();
                mutex.release();
                
                vacios.release(); // Libera un espacio para el productor
                return p;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return null; // Retorna null si no hay procesos nuevos, permitiendo a la CPU seguir en IDLE
    }
}