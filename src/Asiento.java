import java.math.BigDecimal;
import java.math.RoundingMode;

public class Asiento {
    private final String identificador;
    private final BigDecimal precio;

    public Asiento(String identificador, BigDecimal precio) {
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("El asiento debe tener un identificador.");
        }
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException("El precio debe ser cero o mayor.");
        }
        try {
            this.precio = precio.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("El precio debe expresarse en centavos exactos.");
        }
        this.identificador = identificador.strip();
    }

    public String getIdentificador() {
        return identificador;
    }

    public BigDecimal getPrecio() {
        return precio;
    }
}
