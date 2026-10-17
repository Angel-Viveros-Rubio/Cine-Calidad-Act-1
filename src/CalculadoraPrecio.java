import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class CalculadoraPrecio {
    private final Map<String, Asiento> seleccion = new LinkedHashMap<>();

    // Reemplaza la selección completa solo después de validar todos sus elementos.
    public synchronized BigDecimal calcularTotal(Collection<Asiento> asientos) {
        if (asientos == null) {
            throw new IllegalArgumentException("La selección no puede ser nula.");
        }
        Map<String, Asiento> nuevaSeleccion = new LinkedHashMap<>();
        for (Asiento asiento : asientos) {
            validarAsiento(asiento);
            if (nuevaSeleccion.putIfAbsent(asiento.getIdentificador(), asiento) != null) {
                throw new IllegalArgumentException("La selección contiene un asiento repetido.");
            }
        }
        seleccion.clear();
        seleccion.putAll(nuevaSeleccion);
        return getTotalAcumulado();
    }

    // Metodo para agregar un asiento individual y actualizar el total acumulado
    public synchronized BigDecimal agregarAsiento(Asiento asiento) {
        validarAsiento(asiento);
        if (seleccion.putIfAbsent(asiento.getIdentificador(), asiento) != null) {
            throw new IllegalArgumentException("El asiento ya está seleccionado.");
        }
        return getTotalAcumulado();
    }

    // Metodo para remover un asiento individual y restar su precio
    public synchronized BigDecimal removerAsiento(Asiento asiento) {
        validarAsiento(asiento);
        if (seleccion.remove(asiento.getIdentificador()) == null) {
            throw new IllegalArgumentException("El asiento no está seleccionado.");
        }
        return getTotalAcumulado();
    }

    // Getter para obtener el total acumulado actual
    public synchronized BigDecimal getTotalAcumulado() {
        BigDecimal total = new BigDecimal("0.00");
        for (Asiento asiento : seleccion.values()) {
            total = total.add(asiento.getPrecio());
        }
        return total;
    }

    private void validarAsiento(Asiento asiento) {
        if (asiento == null) {
            throw new IllegalArgumentException("El asiento no puede ser nulo.");
        }
        if (asiento.getPrecio() == null || asiento.getPrecio().signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser nulo ni negativo.");
        }
    }
}
