import java.math.BigDecimal;
import java.util.List;

public class PruebasPrecio {
    private static int verificaciones;

    public static void main(String[] args) {
        CalculadoraPrecio calculadora = new CalculadoraPrecio();
        Sala sala = new Sala(new BigDecimal("70.00"));
        comprobar(sala.getAsientos().size() == 15, "Sala de 15 asientos");
        comprobar(sala.getAsientos().stream().map(Asiento::getIdentificador).distinct().count() == 15,
                "Identificadores únicos");
        for (Asiento asiento : sala.getAsientos()) {
            importe("70.00", asiento.getPrecio(), "Precio individual " + asiento.getIdentificador());
        }
        importe("0.00", calculadora.calcularTotal(List.of()), "Selección vacía");
        importe("70.00", calculadora.calcularTotal(List.of(sala.getAsientos().get(0))), "Un asiento");
        importe("210.00", calculadora.calcularTotal(sala.getAsientos().subList(0, 3)), "Tres asientos, incluido el último");
        importe("1050.00", calculadora.calcularTotal(sala.getAsientos()), "Sala completa");
        Sala otraTarifa = new Sala(new BigDecimal("85.50"));
        importe("171.00", calculadora.calcularTotal(otraTarifa.getAsientos().subList(0, 2)), "Precio configurable");
        importe("0.30", calculadora.calcularTotal(List.of(
                new Asiento("X1", new BigDecimal("0.10")),
                new Asiento("X2", new BigDecimal("0.20")))), "Suma decimal exacta");
        rechaza(() -> new Asiento("A1", new BigDecimal("-1")), "Precio negativo");
        rechaza(() -> new Asiento("A1", new BigDecimal("1.001")), "Fracciones de centavo");
        rechaza(() -> new Asiento(" ", BigDecimal.ONE), "Identificador vacío");
        rechaza(() -> new Asiento("A1", null), "Precio nulo");
        rechaza(() -> calculadora.calcularTotal(null), "Selección nula");
        rechaza(() -> calculadora.calcularTotal(java.util.Arrays.asList((Asiento) null)), "Asiento nulo");
        System.out.println("OK: " + verificaciones + " verificaciones.");
    }

    private static void importe(String esperado, BigDecimal resultado, String caso) {
        comprobar(new BigDecimal(esperado).equals(resultado), caso + ": " + resultado);
    }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) {
            throw new AssertionError(caso);
        }
        verificaciones++;
    }

    private static void rechaza(Runnable accion, String caso) {
        try {
            accion.run();
        } catch (IllegalArgumentException e) {
            verificaciones++;
            return;
        }
        throw new AssertionError("No se rechazó: " + caso);
    }
}
