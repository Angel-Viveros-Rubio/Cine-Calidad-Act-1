import java.math.BigDecimal;
import java.util.Collection;

public class CalculadoraPrecio {
    public BigDecimal calcularTotal(Collection<Asiento> asientos) {
        if (asientos == null) {
            throw new IllegalArgumentException("La selección no puede ser nula.");
        }
        BigDecimal total = new BigDecimal("0.00");
        for (Asiento asiento : asientos) {
            if (asiento == null) {
                throw new IllegalArgumentException("La selección contiene un asiento nulo.");
            }
            total = total.add(asiento.getPrecio());
        }
        return total;
    }
}
