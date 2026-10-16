package simulador.concurrencia;

import simulador.modelo.Proceso;
import java.util.List;

public class HiloProductor extends Thread {
    private final BufferProcesos buffer;
    private final List<Proceso> procesosCargados;

    public HiloProductor(BufferProcesos buffer, List<Proceso> procesos) {
        this.buffer = buffer;
        this.procesosCargados = procesos;
    }

    @Override
    public void run() {
        int tickActualSimulado = 0;
        try {
            for (Proceso p : procesosCargados) {
                // Calcula cuántos "Ticks" de tiempo real (500ms) debe dormir antes de lanzar el proceso
                int tiempoEspera = p.getTiempoLlegada() - tickActualSimulado;
                if (tiempoEspera > 0) {
                    Thread.sleep(tiempoEspera * 500L); 
                    tickActualSimulado += tiempoEspera;
                }
                
                buffer.producir(p);
                System.out.println("\n[HILO PRODUCTOR] -> Inyecto el proceso " + p.getPid() + " al buffer del sistema.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}