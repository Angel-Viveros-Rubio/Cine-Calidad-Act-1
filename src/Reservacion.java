import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class Reservacion {
    public enum Estado { EN_COBRO, CONFIRMADA, CANCELADA }
    private final UUID id = UUID.randomUUID();
    private final Cliente cliente;
    private final List<Asiento> asientos;
    private final BigDecimal total;
    private volatile Estado estado = Estado.EN_COBRO;

    Reservacion(Cliente cliente, List<Asiento> asientos) {
        this.cliente = cliente;
        this.asientos = List.copyOf(asientos);
        total = new CalculadoraPrecio().calcularTotal(asientos);
    }

    public UUID getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public List<Asiento> getAsientos() { return asientos; }
    public BigDecimal getTotal() { return total; }
    public Estado getEstado() { return estado; }
    void cambiarEstado(Estado estado) { this.estado = estado; }
}
