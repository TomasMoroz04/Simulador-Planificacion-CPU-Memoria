package simulador.concurrencia;

import simulador.modelo.Proceso;
import java.util.concurrent.Semaphore;

public class ImpresoraVirtual {
    // Semáforo Binario (Mutex) inicializado en 1
    private final Semaphore mutex = new Semaphore(1);

    public void usarImpresora(Proceso p, int tickActual) {
        if (mutex.tryAcquire()) {
            System.out.printf("   [SECCION CRITICA] %s accedio a la Impresora Virtual (Mutex cerrado) en el tick %d%n", p.getPid(), tickActual);
            
            // Simula el tiempo que el proceso ocupa el recurso antes de liberarlo
            mutex.release(); 
        } else {
            System.out.printf("   [ESPERA ACTIVA] %s intento imprimir, pero el recurso esta bloqueado.%n", p.getPid());
        }
    }
}