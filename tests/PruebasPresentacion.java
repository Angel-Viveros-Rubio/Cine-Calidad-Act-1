import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class PruebasPresentacion {
    private static int verificaciones;

    public static void main(String[] args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream pantalla = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        PresentacionConsola presentacion = new PresentacionConsola(pantalla);
        presentacion.mostrarMenu();
        String menu = texto(buffer);
        List<String> comandos = menu.lines().filter(l -> l.startsWith("| ") && !l.contains("PALABRA CLAVE"))
                .map(l -> l.split("\\|", -1)[1].strip()).toList();
        comprobar(comandos.equals(List.of("DISPONIBILIDAD", "NUEVO", "CONFIRMAR", "COMPROBANTES", "REEMITIR",
                "AGREGAR", "ELIMINAR", "VER", "SALIR")), "Exactamente nueve comandos en orden");
        comprobar(menu.contains("PALABRA CLAVE") && menu.contains("FUNCIONALIDAD"), "Dos encabezados");
        comprobar(menu.lines().filter(l -> l.startsWith("|") || l.startsWith("+")).count() == 13,
                "Encabezado, nueve filas y tres bordes");
        verificarTabla(menu, List.of(0, 17, 79));
        comprobar(!menu.contains("\u001b"), "Sin ANSI obligatorio");

        buffer.reset();
        TablaAscii.imprimir(pantalla, List.of("CLAVE", "TEXTO"),
                List.of(List.of("A", "Texto largo con acentos: selección\ny prueba"), List.of("", "corto")), 5, 12);
        String tabla = texto(buffer);
        verificarTabla(tabla, List.of(0, 8, 23));
        comprobar(tabla.contains("selección"), "Acentos conservados al ajustar texto");
        comprobar(tabla.lines().allMatch(l -> l.startsWith("+") || l.startsWith("|")), "Sin saltos que rompan las celdas");
        rechaza(() -> TablaAscii.imprimir(pantalla, List.of("A"), List.of(List.of("A")), 0), "Ancho inválido");
        rechaza(() -> TablaAscii.imprimir(pantalla, List.of("A"), List.of(List.of("A", "B")), 4), "Columnas inválidas");

        Sala sala = new Sala(new BigDecimal("70.00"));
        var administrador = AdministradorBloqueos.paraSala(sala);
        var asientos = sala.getAsientos();
        var cliente = new Cliente("Ana", "555");
        var bloqueo = administrador.iniciar(cliente, List.of(asientos.get(1)));
        var pago = administrador.iniciar(cliente, List.of(asientos.get(2)));
        administrador.pagarTerminal(pago, true);
        buffer.reset();
        presentacion.mostrarSala(sala, administrador, List.of(asientos.get(0), asientos.get(1), asientos.get(2)));
        String mapa = texto(buffer);
        for (String marca : List.of("A1[S]", "A2[B]", "A3[P]", "A4[L]", "C5[L]", "SELECCIONADO (temporal)",
                "EN COBRO / BLOQUEADO", "OCUPADO / PAGADO")) {
            comprobar(mapa.contains(marca), "Mapa: " + marca);
        }
        verificarTabla(mapa, List.of(0, 7, 17, 27, 37, 47, 57));
        comprobar(administrador.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.DISPONIBLE,
                "Seleccionado es solo visual");
        comprobar(administrador.consultarEstado("A2") == AdministradorBloqueos.EstadoAsiento.EN_COBRO, "No libera bloqueo");
        comprobar(administrador.consultarEstado("A3") == AdministradorBloqueos.EstadoAsiento.OCUPADO_PAGADO, "No cambia pago");
        administrador.cancelar(bloqueo);
        buffer.reset();
        presentacion.mostrarSala(sala, administrador, List.of());
        comprobar(texto(buffer).contains("A1[L]") && texto(buffer).contains("A2[L]"), "Sin marcas temporales obsoletas");

        String disponibilidad = ejecutar(sala, "AGREGAR A1\nDISPONIBILIDAD\nSALIR\n");
        String ver = ejecutar(sala, "AGREGAR A1\nVER\nSALIR\n");
        String consultaMapa = ultimaConsulta(disponibilidad);
        String consultaCliente = ultimaConsulta(ver);
        comprobar(consultaMapa.contains("A1[S]"), "DISPONIBILIDAD identifica selección");
        comprobar(!consultaMapa.contains("RESUMEN DEL CLIENTE") && !consultaMapa.contains("Precio de la selección:"),
                "DISPONIBILIDAD prioriza mapa sin resumen de importe");
        comprobar(consultaCliente.contains("A1[S]") && consultaCliente.contains("RESUMEN DEL CLIENTE"), "VER incluye mapa y resumen");
        comprobar(consultaCliente.contains("Cantidad de asientos: 1") && consultaCliente.contains("$70.00 MXN"), "VER cantidad e importe");
        comprobar(!consultaMapa.equals(consultaCliente), "Consultas diferentes");
        comprobar(administrador.getConfirmadas().size() == 1, "Consultas no crean compras");

        String ayuda = ejecutar(sala, "CONFIRMAR\nZ9\nAGREGAR A3\nREEMITIR\nerror\nSALIR\n");
        for (String mensaje : List.of("Para comenzar: AGREGAR A1", "Selecciona al menos un asiento", "Formato: AGREGAR A1",
                "No está disponible.", "Folio inválido. Copia el UUID completo")) {
            comprobar(ayuda.contains(mensaje), "Ayuda: " + mensaje);
        }
        comprobar(!ayuda.contains("Reservación confirmada:"), "Errores no confirman");
        System.out.println("OK: " + verificaciones + " verificaciones de presentación CineFlow.");
    }

    private static String ultimaConsulta(String salida) {
        int inicio = salida.lastIndexOf("MAPA DE LA SALA");
        return salida.substring(inicio);
    }
    private static String ejecutar(Sala sala, String entrada) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Main.ejecutar(sala, new Scanner(entrada));
            return texto(buffer);
        } finally { System.setOut(original); }
    }
    private static void verificarTabla(String salida, List<Integer> separadores) {
        for (String linea : salida.lines().filter(l -> l.startsWith("|") || l.startsWith("+")).toList()) {
            comprobar(linea.length() == separadores.get(separadores.size() - 1) + 1, "Ancho constante: " + linea);
            char borde = linea.charAt(0);
            for (int indice : separadores) { comprobar(linea.charAt(indice) == borde, "Separador alineado " + indice); }
        }
    }
    private static String texto(ByteArrayOutputStream buffer) { return buffer.toString(StandardCharsets.UTF_8); }
    private static void rechaza(Runnable accion, String caso) {
        try { accion.run(); }
        catch (IllegalArgumentException e) { verificaciones++; return; }
        throw new AssertionError(caso);
    }
    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        verificaciones++;
    }
}
