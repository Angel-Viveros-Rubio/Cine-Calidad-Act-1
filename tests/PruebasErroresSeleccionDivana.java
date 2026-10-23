import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Preparación para revisión; no representa pruebas realizadas o aprobadas por Divana. */
public class PruebasErroresSeleccionDivana {
    private static int verificaciones;

    public static void main(String[] args) {
        // Z9, duplicados, eliminación ausente y vacío ya están en PruebasSeleccionConsola.
        // Estos ejemplos amplían la cobertura con formatos y errores sobre una selección existente.
        probarIdentificadoresMalFormados();
        probarComandosConArgumentosDeMas();
        probarRecuperacionTrasVaciarLaSeleccion();

        // No llamar a los métodos pendiente... hasta completar sus comprobaciones.
        System.out.println("OK: " + verificaciones + " verificaciones de errores de selección.");
        System.out.println("PENDIENTES: 2 casos para completar y revisar; no ejecutados.");
    }

    private static void probarIdentificadoresMalFormados() {
        // Ningún identificador parecido a A1 debe aceptarse por coincidencia parcial.
        for (String invalido : List.of("A0", "A6", "D1", "A01", "A-1", "A1B", "A 1")) {
            String salida = ejecutar("A1\nAGREGAR " + invalido + "\nVER\nB2\nVER\nSALIR\n");
            // A 1 tiene dos argumentos: se rechaza por sintaxis, no por asiento inexistente.
            String mensaje = invalido.contains(" ") ? "Usa AGREGAR A1 o ELIMINAR A1." : "Asiento inexistente.";
            comprobar(salida.contains(mensaje), "Rechazo de " + invalido);
            comprobarTotales(salida, "70.00", "70.00", "140.00", "140.00");
            comprobar(salida.contains("Asientos seleccionados: A1, B2"),
                    "El error conserva A1 y permite agregar B2: " + invalido);
        }
    }

    private static void probarComandosConArgumentosDeMas() {
        // Un comando inválido no debe agregar ni eliminar solo una parte de sus argumentos.
        String salida = ejecutar("A1\nB2\nVER\nAGREGAR C3 A2\nVER\n"
                + "ELIMINAR A1 B2\nVER\nAGREGAR\nVER\nELIMINAR\nVER\n"
                + "ELIMINAR A1\nAGREGAR C3\nVER\nSALIR\n");
        comprobarTotales(salida, "70.00", "140.00", "140.00", "140.00", "140.00",
                "140.00", "140.00", "70.00", "140.00", "140.00");
        comprobar(contar(salida, "Usa AGREGAR A1 o ELIMINAR A1.") == 4,
                "Rechaza los cuatro comandos incompletos o con argumentos extra");
        comprobar(salida.contains("Asientos seleccionados: B2, C3"),
                "Tras los errores todavía se puede modificar la selección");
        comprobar(contar(salida, "Pago:") == 0, "Modificar la selección no inicia cobros");
    }

    private static void probarRecuperacionTrasVaciarLaSeleccion() {
        // Confirmar repetidamente después de quitar el último asiento no debe avanzar a cliente/pago.
        String salida = ejecutar("A1\nELIMINAR A1\nCONFIRMAR\nCONFIRMAR\n"
                + "ELIMINAR B2\nAGREGAR Z9\nVER\nB2\nVER\nSALIR\n");
        comprobarTotales(salida, "70.00", "0.00", "0.00", "70.00", "70.00");
        comprobar(contar(salida, "Selecciona al menos un asiento antes de continuar.") == 2,
                "Las dos confirmaciones vacías se rechazan");
        comprobar(salida.contains("Asientos seleccionados: B2"), "Permite empezar otra selección tras los errores");
        comprobar(contar(salida, "Nombre:") == 0, "Vacío no solicita cliente");
        comprobar(contar(salida, "Pago:") == 0, "Vacío no solicita pago");
        comprobar(contar(salida, "Reservación confirmada:") == 0, "Sin confirmaciones indebidas");
        comprobar(!salida.contains("$-"), "Los errores no producen importes negativos");
    }

    private static void pendienteComandoIncompletoNoConsumeLaSiguienteLinea() {
        // PENDIENTE para revisión y desarrollo personal: todavía no se ejecuta.
        // 1. Usar ejecutar("AGREGAR\nA1\nVER\nELIMINAR\nB2\nVER\nSALIR\n").
        // 2. Comprobar dos avisos de sintaxis: A1 y B2 deben leerse como comandos posteriores.
        // 3. Verificar totales 70, 70, 140, 140 y selección final A1, B2.
        // 4. Agregar las comprobaciones y después llamar al método desde main.
    }

    private static void pendienteErroresConLosQuinceAsientosSeleccionados() {
        // PENDIENTE para revisión y desarrollo personal: todavía no se ejecuta.
        // 1. Agregar A1 a C5 (puedes usar dos bucles, como PruebasSeleccionConsola).
        // 2. Intentar AGREGAR A1, AGREGAR Z9 y ELIMINAR D1; consultar VER tras cada error.
        // 3. Comprobar que siguen 15 seleccionados y $1050.00, sin importes mayores.
        // 4. Eliminar C5 y agregarlo otra vez: $980.00 y $1050.00; después activar en main.
    }

    private static String ejecutar(String entrada) {
        InputStream entradaOriginal = System.in;
        PrintStream salidaOriginal = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(entrada.getBytes(Charset.defaultCharset())));
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Main.main(new String[0]);
            return buffer.toString(StandardCharsets.UTF_8);
        } finally {
            // Restaurar la consola incluso si una prueba falla.
            System.setIn(entradaOriginal);
            System.setOut(salidaOriginal);
        }
    }

    private static void comprobarTotales(String salida, String... esperados) {
        List<String> actuales = salida.lines().filter(linea -> linea.startsWith("Precio de la selección: $"))
                .map(linea -> linea.substring("Precio de la selección: $".length()).replace(" MXN", "")).toList();
        comprobar(actuales.equals(List.of(esperados)), "Totales esperados " + List.of(esperados) + "; observados " + actuales);
    }

    private static long contar(String salida, String prefijo) {
        return salida.lines().filter(linea -> linea.startsWith(prefijo)).count();
    }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        verificaciones++;
    }
}
