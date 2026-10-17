import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.ZonedDateTime;
import java.time.ZoneId;

public final class Reservacion {
    public enum Estado { EN_COBRO, CONFIRMADA, CANCELADA }
    public enum MetodoPago { EFECTIVO, TERMINAL }
    private record Pago(MetodoPago metodo, ZonedDateTime fecha) { }
    private final UUID id = UUID.randomUUID();
    private final Cliente cliente;
    private final List<Asiento> asientos;
    private final BigDecimal total;
    private volatile Estado estado = Estado.EN_COBRO;
    private volatile Pago pago;

    Reservacion(Cliente cliente, List<Asiento> asientos) {
        this.cliente = cliente;
        this.asientos = List.copyOf(asientos);
        total = new CalculadoraPrecio().calcularTotal(asientos);
    }

    public UUID getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public List<Asiento> getAsientos() { return asientos; }
    public BigDecimal getTotal() { return total; }
    public Estado getEstado() { return pago != null ? Estado.CONFIRMADA : estado; }
    public MetodoPago getMetodoPago() { Pago actual = pago; return actual == null ? null : actual.metodo(); }
    public ZonedDateTime getFechaConfirmacion() { Pago actual = pago; return actual == null ? null : actual.fecha(); }

    synchronized void confirmarPago(MetodoPago metodo) {
        if (getEstado() != Estado.EN_COBRO) {
            throw new IllegalStateException("La operación ya terminó.");
        }
        // Publicación única: fecha, método y confirmación aparecen juntos.
        pago = new Pago(java.util.Objects.requireNonNull(metodo),
                ZonedDateTime.now(ZoneId.of("America/Mexico_City")));
    }

    synchronized void cancelar() {
        if (getEstado() == Estado.EN_COBRO) { estado = Estado.CANCELADA; }
    }
}
