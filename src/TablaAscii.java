import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** Tabla sin ANSI: ajusta textos largos y mantiene constantes los anchos. */
public final class TablaAscii {
    private TablaAscii() { }

    public static void imprimir(PrintStream salida, List<String> encabezados,
            List<List<String>> filas, int... anchos) {
        if (anchos.length == 0 || encabezados.size() != anchos.length
                || java.util.Arrays.stream(anchos).anyMatch(ancho -> ancho < 1)
                || filas.stream().anyMatch(fila -> fila.size() != anchos.length)) {
            throw new IllegalArgumentException("Las columnas y sus anchos deben coincidir.");
        }
        StringBuilder borde = new StringBuilder("+");
        for (int ancho : anchos) { borde.append("-".repeat(ancho + 2)).append('+'); }
        salida.println(borde);
        imprimirFila(salida, encabezados, anchos);
        salida.println(borde);
        for (List<String> fila : filas) { imprimirFila(salida, fila, anchos); }
        salida.println(borde);
    }

    private static void imprimirFila(PrintStream salida, List<String> fila, int[] anchos) {
        List<List<String>> columnas = new ArrayList<>();
        int alto = 1;
        for (int i = 0; i < anchos.length; i++) {
            // No permitir saltos ni controles dentro de una celda.
            String texto = fila.get(i).replaceAll("[\\p{Cc}\\p{Cf}]", " ");
            int[] caracteres = texto.codePoints().toArray();
            List<String> lineas = new ArrayList<>();
            for (int inicio = 0; inicio < caracteres.length; inicio += anchos[i]) {
                lineas.add(new String(caracteres, inicio, Math.min(anchos[i], caracteres.length - inicio)));
            }
            if (lineas.isEmpty()) { lineas.add(""); }
            columnas.add(lineas);
            alto = Math.max(alto, lineas.size());
        }
        for (int linea = 0; linea < alto; linea++) {
            StringBuilder resultado = new StringBuilder("|");
            for (int columna = 0; columna < anchos.length; columna++) {
                String texto = linea < columnas.get(columna).size() ? columnas.get(columna).get(linea) : "";
                resultado.append(' ').append(texto)
                        .append(" ".repeat(anchos[columna] - texto.codePointCount(0, texto.length()) + 1)).append('|');
            }
            salida.println(resultado);
        }
    }
}
