import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PruebasIntegracionTaquilla {
    private static int verificaciones;

    public static void main(String[] args) throws Exception {
        String salida = ejecutar("A1\nB2\nCONFIRMAR\nCONTINUAR\nAna\n555\n1\n200\nVER\nSALIR\n");
        contiene(salida, "- A1");
        contiene(salida, "- B2");
        contiene(salida, "Monto total: $140.00 MXN");
        contiene(salida, "Cambio: $60.00 MXN");
        cantidad(salida, "Reservación confirmada:", 1);
        contiene(salida, "Selección vacía.");
        verificar(salida.indexOf("Monto total:") < salida.indexOf("Nombre:"), "Resumen antes del cliente");
        verificar(salida.indexOf("Teléfono:") < salida.indexOf("Pago:"), "Cliente antes del pago");
        String ruta = salida.lines().filter(l -> l.startsWith("Comprobante: ")).findFirst().orElseThrow()
                .substring("Comprobante: ".length());
        String comprobante = Files.readString(Path.of(ruta), StandardCharsets.UTF_8);
        contiene(comprobante, "Cliente: Ana");
        contiene(comprobante, "Asientos: A1, B2");
        contiene(comprobante, "Importe total: $140.00 MXN");
        contiene(comprobante, "Método de pago: EFECTIVO");

        salida = ejecutar("A1\nA2\nCONFIRMAR\nVOLVER\nELIMINAR A1\nB2\nCONFIRMAR\nCONTINUAR\nAna\n555\n2\n1\nSALIR\n");
        contiene(salida, "Asientos seleccionados: A2, B2");
        cantidad(salida, "Monto total: $140.00 MXN", 3);
        cantidad(salida, "Nombre:", 1);
        cantidad(salida, "Reservación confirmada:", 1);

        salida = ejecutar("CONFIRMAR\nVER\nSALIR\n");
        contiene(salida, "Selecciona al menos un asiento antes de continuar.");
        cantidad(salida, "Nombre:", 0);
        cantidad(salida, "Pago:", 0);

        salida = ejecutar("A1\nCONFIRMAR\nCONTINUAR\nAna\n555\n0\nCONFIRMAR\nCONTINUAR\nLuis\n666\n2\n1\nSALIR\n");
        contiene(salida, "No se confirmó una compra.");
        cantidad(salida, "Reservación confirmada:", 1);
        cantidad(salida, "Comprobante:", 1);
        verificar(!salida.contains("está protegido por otro cobro"), "Cancelar libera para reintento");

        salida = ejecutar("C5\nCONFIRMAR\nCONTINUAR\n\nAna\n\n555\n1\n50\n2\n2\n2\n1\nSALIR\n");
        contiene(salida, "El campo Nombre es obligatorio.");
        contiene(salida, "El campo Teléfono es obligatorio.");
        contiene(salida, "Efectivo insuficiente.");
        contiene(salida, "Pago rechazado.");
        cantidad(salida, "Reservación confirmada:", 1);
        cantidad(salida, "Comprobante:", 1);
        contiene(salida, "Monto total: $70.00 MXN");

        salida = ejecutar("A1\nCONFIRMAR\nCONTINUAR\nAna\n555\n2\n1\nA1\nCONFIRMAR\nCONTINUAR\nLuis\n666\nSALIR\n");
        cantidad(salida, "Reservación confirmada:", 1);
        cantidad(salida, "Comprobante:", 1);
        cantidad(salida, "Pago:", 1);
        contiene(salida, "El asiento A1 está protegido por otro cobro.");

        salida = ejecutar("A1\nCONFIRMAR\nx\nVOLVER\nVER\nSALIR\n");
        contiene(salida, "Opción inválida. Usa CONTINUAR o VOLVER.");
        contiene(salida, "Asientos seleccionados: A1");
        cantidad(salida, "Nombre:", 0);
        cantidad(ejecutar("A1\nCONFIRMAR\n"), "Pago:", 0);
        cantidad(ejecutar("A1\nCONFIRMAR\nCONTINUAR\n"), "Pago:", 0);
        cantidad(ejecutar("A1\nCONFIRMAR\nCONTINUAR\nAna\n555\n2\n"), "Reservación confirmada:", 0);
        System.out.println("OK: " + verificaciones + " verificaciones de integración con taquilla.");
    }

    private static String ejecutar(String entrada) {
        InputStream originalEntrada = System.in;
        PrintStream originalSalida = System.out;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(entrada.getBytes(Charset.defaultCharset())));
            System.setOut(new PrintStream(salida, true, StandardCharsets.UTF_8));
            Main.main(new String[0]);
            return salida.toString(StandardCharsets.UTF_8);
        } finally {
            System.setIn(originalEntrada);
            System.setOut(originalSalida);
        }
    }
    private static void contiene(String texto, String esperado) {
        verificar(texto.contains(esperado), "Falta: " + esperado);
    }
    private static void cantidad(String texto, String prefijo, long esperada) {
        verificar(texto.lines().filter(l -> l.startsWith(prefijo)).count() == esperada, "Cantidad de " + prefijo);
    }
    private static void verificar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        verificaciones++;
    }
}
