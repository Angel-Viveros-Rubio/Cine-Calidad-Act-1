import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PruebasSeleccionConsola {
    private static int verificaciones;

    public static void main(String[] args) {
        String salida = ejecutar("A1\nAGREGAR B2\nC5\nVER\nSALIR\n");
        contiene(salida, "Precio por asiento: $70.00 MXN");
        contiene(salida, "C5[L]");
        totales(salida, "70.00", "140.00", "210.00", "210.00");
        contiene(salida, "Asientos seleccionados: A1, B2, C5");

        salida = ejecutar("A1\nA2\nELIMINAR A1\nVER\nELIMINAR A2\nVER\nSALIR\n");
        totales(salida, "70.00", "140.00", "70.00", "70.00", "0.00", "0.00");
        contiene(salida, "Asientos seleccionados: A2");
        contiene(salida, "Selección vacía.");

        salida = ejecutar("A1\na1\nAGREGAR A1\nVER\nELIMINAR A1\nA1\nVER\nSALIR\n");
        totales(salida, "70.00", "70.00", "0.00", "70.00", "70.00");
        comprobar(salida.lines().filter(l -> l.equals("Ese asiento ya está seleccionado.")).count() == 2,
                "Duplicados rechazados sin alterar total");

        salida = ejecutar("ELIMINAR A1\nA1\nELIMINAR B2\nELIMINAR Z9\nVER\nELIMINAR A1\nELIMINAR A1\nVER\nSALIR\n");
        totales(salida, "70.00", "70.00", "0.00", "0.00");
        comprobar(salida.lines().filter(l -> l.equals("Ese asiento no está seleccionado.")).count() == 3,
                "Rechazar eliminación ausente y repetida");
        contiene(salida, "Asiento inexistente. Usa A1 a C5.");
        comprobar(!salida.contains("$-"), "Sin importes negativos");

        salida = ejecutar("\nZ9\nELIMINAR\nAGREGAR\nELIMINAR A1 A2\nVER\nSALIR\n");
        totales(salida, "0.00");
        contiene(salida, "Usa AGREGAR A1 o ELIMINAR A1.");
        salida = ejecutar("  agregar   a1  \n eliminar   a1 \nVER\nSALIR\n");
        totales(salida, "70.00", "0.00", "0.00");

        totales(ejecutar("VER\nSALIR\n"), "0.00");
        contiene(ejecutar(""), "Selección vacía. Precio de la selección: $0.00 MXN");
        totales(ejecutar("A1\n"), "70.00");
        totales(ejecutar("SALIR\nA1\n"));

        StringBuilder salaCompleta = new StringBuilder();
        for (char fila = 'A'; fila <= 'C'; fila++) {
            for (int numero = 1; numero <= 5; numero++) { salaCompleta.append(fila).append(numero).append('\n'); }
        }
        String llena = ejecutar(salaCompleta + "VER\nSALIR\n");
        contiene(llena, "Precio de la selección: $1050.00 MXN");
        for (char fila = 'A'; fila <= 'C'; fila++) {
            for (int numero = 1; numero <= 5; numero++) { salaCompleta.append("ELIMINAR ").append(fila).append(numero).append('\n'); }
        }
        salida = ejecutar(salaCompleta + "VER\nSALIR\n");
        comprobar(salida.strip().endsWith("Comando:"), "Termina sin error");
        List<String> valores = valores(salida);
        comprobar(valores.get(valores.size() - 1).equals("0.00"), "Vaciar sala completa termina en cero");
        comprobar(!salida.contains("$-"), "Sala completa sin negativos");
        System.out.println("OK: " + verificaciones + " verificaciones de selección por consola.");
    }

    private static String ejecutar(String texto) {
        InputStream entradaOriginal = System.in;
        PrintStream salidaOriginal = System.out;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(texto.getBytes(Charset.defaultCharset())));
            System.setOut(new PrintStream(salida, true, StandardCharsets.UTF_8));
            Main.main(new String[0]);
            return salida.toString(StandardCharsets.UTF_8);
        } finally {
            System.setIn(entradaOriginal);
            System.setOut(salidaOriginal);
        }
    }

    private static List<String> valores(String salida) {
        return salida.lines().filter(l -> l.startsWith("Precio de la selección: $"))
                .map(l -> l.substring("Precio de la selección: $".length()).replace(" MXN", "")).toList();
    }

    private static void totales(String salida, String... esperados) {
        comprobar(valores(salida).equals(List.of(esperados)), "Secuencia exacta de totales: " + valores(salida));
    }
    private static void contiene(String salida, String esperado) { comprobar(salida.contains(esperado), esperado); }
    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        verificaciones++;
    }
}
