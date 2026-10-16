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

    // Metodo para agregar un asiento individual y actualizar el total acumulado
    public BigDecimal agregarAsiento(Asiento asiento) {
        if (asiento == null) {
            throw new IllegalArgumentException("El asiento a agregar no puede ser nulo.");
        }
        this.totalAcumulado = this.totalAcumulado.add(asiento.getPrecio());
        return this.totalAcumulado;
    }

    // Metodo para remover un asiento individual y restar su precio
    public BigDecimal removerAsiento(Asiento asiento) {
        if (asiento == null) {
            throw new IllegalArgumentException("El asiento a remover no puede ser nulo.");
        }
        this.totalAcumulado = this.totalAcumulado.subtract(asiento.getPrecio());
        return this.totalAcumulado;
    }

    // Getter para obtener el total acumulado actual
    public BigDecimal getTotalAcumulado() {
        return totalAcumulado;
    }
}