package simulador.modelo;

public class MarcoRAM {
    private final int idMarco;
    private boolean libre;
    private String pidPropietario;
    private int numeroPagina;
    private int tickCarga;      // Para reemplazo FIFO
    private int ultimoUsoTick;  // Para reemplazo LRU

    public MarcoRAM(int idMarco) {
        this.idMarco = idMarco;
        this.libre = true;
        this.pidPropietario = null;
        this.numeroPagina = -1;
        this.tickCarga = -1;
        this.ultimoUsoTick = -1;
    }

    public void ocupar(String pid, int pagina, int tickActual) {
        this.libre = false;
        this.pidPropietario = pid;
        this.numeroPagina = pagina;
        this.tickCarga = tickActual;
        this.ultimoUsoTick = tickActual;
    }

    public void liberar() {
        this.libre = true;
        this.pidPropietario = null;
        this.numeroPagina = -1;
        this.tickCarga = -1;
        this.ultimoUsoTick = -1;
    }

    public void actualizarUso(int tickActual) {
        this.ultimoUsoTick = tickActual;
    }

    public int getIdMarco() { return idMarco; }
    public boolean isLibre() { return libre; }
    public String getPidPropietario() { return pidPropietario; }
    public int getNumeroPagina() { return numeroPagina; }
    public int getTickCarga() { return tickCarga; }
    public int getUltimoUsoTick() { return ultimoUsoTick; }

    @Override
    public String toString() {
        if (libre) {
            return String.format("[M%02d: LIBRE ]", idMarco);
        }
        return String.format("[M%02d: %s-P%d]", idMarco, pidPropietario, numeroPagina);
    }
}