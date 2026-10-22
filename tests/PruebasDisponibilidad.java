import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class PruebasDisponibilidad {
    private static int verificaciones;
    private static final Cliente CLIENTE = new Cliente("Otra taquilla", "555");

    public static void main(String[] args) {
        Sala sala = nuevaSala();
        AdministradorBloqueos admin = AdministradorBloqueos.paraSala(sala);
        String salida = ejecutar(sala,
                "A1\nA2\nCONFIRMAR\nCONTINUAR\nAna\n555\n2\n1\n"
                + "NUEVO\nA1\nA2\nVER\nA3\nCONFIRMAR\nCONTINUAR\nLuis\n666\n1\n70\nDISPONIBILIDAD\nSALIR\n", null);
        cantidad(salida, "Reservación confirmada:", 2);
        cantidad(salida, "Comprobante:", 2);
        contiene(salida, "Nueva reservación. Selección vacía.");
        contiene(salida, "A1[P]");
        contiene(salida, "A2[P]");
        contiene(salida, "A3[P]");
        contiene(salida, "El asiento A1 está protegido por otro cobro.");
        contiene(salida, "El asiento A2 está protegido por otro cobro.");
        verificar(admin.getConfirmadas().size() == 2, "Mismo administrador conserva ambas compras");
        verificar(admin.getDisponibles().size() == 12, "Tres lugares ocupados");
        for (String id : List.of("A1", "A2", "A3")) {
            verificar(admin.consultarEstado(id) == AdministradorBloqueos.EstadoAsiento.OCUPADO_PAGADO, "Pagado " + id);
        }
        verificar(admin.getConfirmadas().stream().anyMatch(r -> r.getCliente().getNombre().equals("Ana")
                && r.getTotal().equals(new BigDecimal("140.00"))), "Cliente uno total correcto");
        verificar(admin.getConfirmadas().stream().anyMatch(r -> r.getCliente().getNombre().equals("Luis")
                && r.getTotal().equals(new BigDecimal("70.00"))), "Cliente dos sin arrastrar selección");

        Sala cancelacion = nuevaSala();
        salida = ejecutar(cancelacion, "B1\nCONFIRMAR\nCONTINUAR\nAna\n555\n0\nNUEVO\nB1\nCONFIRMAR\nCONTINUAR\nLuis\n666\n2\n1\nSALIR\n", null);
        contiene(salida, "B1[L]");
        cantidad(salida, "Reservación confirmada:", 1);
        verificar(AdministradorBloqueos.paraSala(cancelacion).getConfirmadas().get(0).getCliente().getNombre().equals("Luis"),
                "Cancelación no registra compra de Ana");

        Sala bloqueada = nuevaSala();
        var otroAdmin = AdministradorBloqueos.paraSala(bloqueada);
        Reservacion otroCobro = otroAdmin.iniciar(CLIENTE, bloqueada.getAsientos().subList(0, 2));
        salida = ejecutar(bloqueada, "A3\nA1\nAGREGAR A2\nVER\nSALIR\n", null);
        contiene(salida, "A1[B]");
        contiene(salida, "A2[B]");
        contiene(salida, "Asientos seleccionados: A3");
        verificar(salida.lines().filter(l -> l.equals("Precio de la selección: $70.00 MXN")).count() == 2,
                "Rechazos mantienen el total");
        verificar(!salida.contains("Precio de la selección: $140.00 MXN"), "Sin suma de bloqueados");
        otroAdmin.cancelar(otroCobro);
        salida = ejecutar(bloqueada, "A1\nVER\nSALIR\n", null);
        contiene(salida, "A1[L]");
        contiene(salida, "Asientos seleccionados: A1");

        // Otro cobro toma el asiento después de seleccionarlo, antes del resumen.
        Sala cambia = nuevaSala();
        var adminCambia = AdministradorBloqueos.paraSala(cambia);
        AtomicReference<Reservacion> bloqueo = new AtomicReference<>();
        salida = ejecutar(cambia, "A1\nCONFIRMAR\nSALIR\n", texto -> {
            if (texto.equals("Asientos seleccionados: A1") && bloqueo.get() == null) {
                bloqueo.set(adminCambia.iniciar(CLIENTE, cambia.getAsientos().subList(0, 1)));
            }
        });
        contiene(salida, "Estado: EN_COBRO");
        cantidad(salida, "Nombre:", 0);
        cantidad(salida, "Pago:", 0);
        verificar(adminCambia.getConfirmadas().isEmpty(), "Sin compra si deja de estar disponible");
        adminCambia.cancelar(bloqueo.get());

        // Carrera posterior al resumen: iniciar() sigue siendo la validación atómica final.
        Sala carrera = nuevaSala();
        var adminCarrera = AdministradorBloqueos.paraSala(carrera);
        AtomicReference<Reservacion> enCurso = new AtomicReference<>();
        salida = ejecutar(carrera, "A1\nCONFIRMAR\nCONTINUAR\nAna\n555\nSALIR\n", texto -> {
            if (texto.equals("Nombre:") && enCurso.get() == null) {
                enCurso.set(adminCarrera.iniciar(CLIENTE, carrera.getAsientos().subList(0, 1)));
            }
        });
        cantidad(salida, "Pago:", 0);
        cantidad(salida, "Reservación confirmada:", 0);
        contiene(salida, "El asiento A1 está protegido por otro cobro.");
        verificar(adminCarrera.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.EN_COBRO,
                "Rechazo no libera bloqueo ajeno");
        adminCarrera.cancelar(enCurso.get());
        System.out.println("OK: " + verificaciones + " verificaciones de disponibilidad.");
    }

    private static Sala nuevaSala() { return new Sala(new BigDecimal("70.00")); }
    private static String ejecutar(Sala sala, String entrada, java.util.function.Consumer<String> alImprimir) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream pantalla = new PrintStream(buffer, true, StandardCharsets.UTF_8) {
            @Override public void println(String texto) {
                super.println(texto);
                if (alImprimir != null) { alImprimir.accept(texto); }
            }
        };
        try {
            System.setOut(pantalla);
            Main.ejecutar(sala, new Scanner(entrada));
            return buffer.toString(StandardCharsets.UTF_8);
        } finally { System.setOut(original); }
    }
    private static void contiene(String salida, String texto) { verificar(salida.contains(texto), texto); }
    private static void cantidad(String salida, String prefijo, long esperado) {
        verificar(salida.lines().filter(l -> l.startsWith(prefijo)).count() == esperado, "Cantidad " + prefijo);
    }
    private static void verificar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        verificaciones++;
    }
}
