package simulador.modelo;

public class EntradaTablaPagina {
    private final int numeroPagina;
    private int numeroMarco; // -1 si está en Swap (Disco secundario)
    private boolean presenteEnRAM;
    private int ultimoAccesoTick; // Para el algoritmo LRU

    public EntradaTablaPagina(int numeroPagina) {
        this.numeroPagina = numeroPagina;
        this.numeroMarco = -1;
        this.ultimoAccesoTick = 0;
        this.presenteEnRAM = false;
    }

    public void asignarMarco(int numeroMarco, int tickActual) {
        this.numeroMarco = numeroMarco;
        this.presenteEnRAM = true;
        this.ultimoAccesoTick = tickActual;
    }

    public void desalojarMarco() {
        this.numeroMarco = -1;
        this.presenteEnRAM = false;
    }

    public void registrarAcceso(int tickActual) {
        this.ultimoAccesoTick = tickActual;
    }

    public int getNumeroPagina() { return numeroPagina; }
    public int getNumeroMarco() { return numeroMarco; }
    public boolean isPresenteEnRAM() { return presenteEnRAM; }
    public int getUltimoAccesoTick() { return ultimoAccesoTick; }
}