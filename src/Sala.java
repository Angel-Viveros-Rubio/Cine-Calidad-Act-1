import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Sala {
    private final List<Asiento> asientos;

    public Sala(BigDecimal precioPorAsiento) {
        List<Asiento> lugares = new ArrayList<>();
        for (char fila = 'A'; fila <= 'C'; fila++) {
            for (int numero = 1; numero <= 5; numero++) {
                lugares.add(new Asiento("" + fila + numero, precioPorAsiento));
            }
        }
        asientos = List.copyOf(lugares);
    }

    public List<Asiento> getAsientos() {
        return asientos;
    }
}
