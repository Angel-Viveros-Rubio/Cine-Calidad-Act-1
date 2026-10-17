import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class PruebasCalculadora {
    private static int verificaciones;

    public static void main(String[] args) {
        CalculadoraPrecio calculadora = new CalculadoraPrecio();
        Asiento a = new Asiento("A1", new BigDecimal("70.00"));
        Asiento b = new Asiento("A2", new BigDecimal("85.50"));
        importe("0.00", calculadora.getTotalAcumulado(), "Inicio vacío");
        importe("70.00", calculadora.agregarAsiento(a), "Agregar primero");
        importe("155.50", calculadora.agregarAsiento(b), "Agregar segundo");
        rechaza(() -> calculadora.agregarAsiento(a), "No duplicar referencia");
        rechaza(() -> calculadora.agregarAsiento(new Asiento("A1", BigDecimal.ONE)), "No duplicar identificador");
        importe("155.50", calculadora.getTotalAcumulado(), "Duplicados no alteran selección");
        importe("85.50", calculadora.removerAsiento(new Asiento("A1", new BigDecimal("999.00"))),
                "Quitar por identificador sin restar precio ajeno");
        rechaza(() -> calculadora.removerAsiento(a), "No quitar ausente");
        importe("85.50", calculadora.getTotalAcumulado(), "Ausente no altera total");
        importe("0.00", calculadora.removerAsiento(b), "Quitar último");
        rechaza(() -> calculadora.removerAsiento(b), "No producir total negativo");

        importe("155.50", calculadora.calcularTotal(List.of(a, b)), "Cargar selección completa");
        importe("155.50", calculadora.getTotalAcumulado(), "Consulta coherente tras calcular");
        importe("155.50", calculadora.calcularTotal(List.of(a, b)), "Recalcular sin duplicar");
        importe("70.00", calculadora.removerAsiento(b), "Eliminar después de calcular");
        importe("155.50", calculadora.agregarAsiento(b), "Agregar después de calcular");
        rechaza(() -> calculadora.calcularTotal(Arrays.asList(a, null)), "Validación completa antes de reemplazar");
        importe("155.50", calculadora.getTotalAcumulado(), "Error conserva selección anterior");
        rechaza(() -> calculadora.calcularTotal(List.of(a, a)), "Colección con duplicados");
        rechaza(() -> calculadora.calcularTotal(List.of(a, new Asiento("A1", BigDecimal.TEN))), "Duplicados con distinto precio");
        rechaza(() -> calculadora.calcularTotal(null), "Colección nula");
        rechaza(() -> calculadora.agregarAsiento(null), "Agregar nulo");
        rechaza(() -> calculadora.removerAsiento(null), "Quitar nulo");
        importe("155.50", calculadora.getTotalAcumulado(), "Errores no alteran total");
        importe("85.50", calculadora.calcularTotal(List.of(b)), "Reemplazar selección sin arrastrar asientos");
        importe("0.00", calculadora.calcularTotal(List.of()), "Vaciar selección");
        importe("0.00", calculadora.getTotalAcumulado(), "Consulta vacía");
        importe("0.10", calculadora.agregarAsiento(new Asiento("X1", new BigDecimal("0.10"))), "Primer decimal");
        importe("0.30", calculadora.agregarAsiento(new Asiento("X2", new BigDecimal("0.20"))), "Suma decimal exacta");
        importe("0.30", calculadora.getTotalAcumulado(), "Consulta decimal exacta");
        importe("0.00", new CalculadoraPrecio().getTotalAcumulado(), "Instancias independientes");
        System.out.println("OK: " + verificaciones + " verificaciones de calculadora.");
    }

    private static void importe(String esperado, BigDecimal actual, String caso) {
        if (!new BigDecimal(esperado).equals(actual)) { throw new AssertionError(caso + ": " + actual); }
        verificaciones++;
    }

    private static void rechaza(Runnable accion, String caso) {
        try { accion.run(); }
        catch (IllegalArgumentException e) { verificaciones++; return; }
        throw new AssertionError(caso);
    }
}
