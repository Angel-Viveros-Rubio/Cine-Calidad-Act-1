# HU-02: checkpoints 4 a 8

Esta entrega agrega pago simulado, protección durante el cobro, estado ocupado/pagado
y comprobantes de texto. No modifica Main, Sala, Asiento ni CalculadoraPrecio.

## Compilar y probar

Desde la raíz, con JDK 21:

```powershell
javac --release 21 -encoding UTF-8 -d out src/*.java tests/*.java
java -cp out PruebasPrecio
java -cp out PruebasTaquilla
java -cp out PruebasCobro
java -cp out PruebasComprobante
java -cp out Taquilla
```

En IntelliJ también se puede ejecutar el método main de Taquilla o de cada prueba.
La demostración solicita nombre y teléfono y usa A1 y A2 ($140.00 MXN).

- Efectivo: elegir 1 e introducir 200; confirma y muestra cambio de $60.00.
- Insuficiente: elegir 1 e introducir 100; no confirma y permite reintentar.
- Terminal: elegir 2 y después 1 para aprobar o 2 para rechazar.
- Cancelar: elegir 0 en cualquiera de las preguntas de pago.
- Entradas incorrectas: probar letras, números negativos, más de dos decimales,
  o resultados de terminal distintos de 0, 1 y 2; permite corregirlos.
- Fin de entrada: cancela el cobro pendiente y libera sus asientos.

PruebasCobro reproduce dos taquillas con la misma sala: mientras la primera cobra,
la segunda intenta los mismos lugares y es rechazada, incluso después de un pago
insuficiente. También arranca dos hilos simultáneamente y verifica que solo uno
obtenga un mismo asiento. Comprueba liberación tras cancelación, error e interrupción.

## Propiedad y recuperación

Todas las taquillas que atienden la misma sala deben compartir la misma instancia
de Sala. AdministradorBloqueos.paraSala devuelve el mismo administrador para ella.
El bloqueo de un conjunto se valida completo antes de aplicarlo, bajo sincronización.
Solo se aceptan los asientos originales del catálogo; no copias ni duplicados.
Una reservación ajena o falsificada no puede liberar los bloqueos de otra.

Taquilla utiliza finally para cancelar y liberar únicamente operaciones pendientes.
Pago y confirmación se efectúan juntos bajo el monitor del administrador. Si falla
la salida de pantalla después del pago, getConfirmadas() permite recuperar la
reservación por su ID, cliente, asientos y total, sin cobrar otra vez. Los nuevos
intentos de pago sobre una operación terminada se rechazan.

Los pagos confirmados mantienen protección y sus asientos pasan a OCUPADO_PAGADO.
El administrador deriva ese estado de la misma confirmación de la reservación:
no mantiene una copia de estado que pueda quedar desactualizada. Las consultas
consultarEstado(identificador) y getDisponibles() están sincronizadas con los cobros.
No hay caducidad automática ni liberación de pagos exitosos: hacerlo permitiría vender
otra vez esos lugares. Los rechazos mantienen EN_COBRO hasta reintentar o cancelar;
cancelar una operación pendiente devuelve sus asientos a DISPONIBLE.
Quien use iniciar directamente debe cancelar su operación pendiente en un finally;
el flujo de Taquilla ya lo hace.

## Comprobantes y pruebas manuales

Tras aprobar un pago, Taquilla genera `comprobantes/comprobante-<folio>.txt` en UTF-8.
El archivo contiene folio, nombre, teléfono, asientos, total, método y fecha de
confirmación con zona America/Mexico_City. La fecha se registra una vez al pagar.
La demostración muestra A1 y A2 como OCUPADO_PAGADO y 13 disponibles tras el pago;
si se cancela antes del pago muestra DISPONIBLE y 15 disponibles.
Abrir el archivo en un editor de texto permite revisarlo e imprimirlo manualmente.

GeneradorComprobante rechaza reservaciones pendientes o canceladas. Escribe primero
un temporal en el directorio destino y lo renombra sin reemplazar un archivo existente.
Las llamadas concurrentes del mismo proceso se serializan. Un reintento con el mismo
folio devuelve el archivo existente si coincide exactamente; si difiere, avisa del
conflicto sin sobrescribirlo. Un error de archivo no cancela el pago ni la ocupación.
No se garantiza recuperación de archivos frente a corte de energía o cierre forzado.

Para reintentar desde código, conservar la Sala y consultar
`AdministradorBloqueos.paraSala(sala).getConfirmadas()`, localizar la reservación por
folio y llamar `taquilla.generarComprobante(reservacion, directorio)` de nuevo.
La reemisión no llama a ningún método de pago. El directorio puede ser, por ejemplo,
`Path.of("comprobantes")`; se rechazan src y tests como destinos en la raíz del proyecto.

PruebasComprobante realiza una demostración reproducible de error de escritura usando
un archivo como directorio destino; después verifica pago y ocupación, reintenta en
una carpeta válida y comprueba el archivo. También prueba ambos métodos, contenido,
fecha estable, generación simultánea, doble cobro y rechazo de sobreescrituras.
Los artefactos de pruebas se guardan en out/ y los recibos de la demostración en
comprobantes/; ambos están excluidos de Git. Ninguno se escribe en src/ ni tests/.

## Límites

Datos y bloqueos existen solo en memoria y dentro del mismo proceso Java. Reiniciar
el programa descarta todo; dos programas separados no comparten disponibilidad.
La terminal y el efectivo son simulados, sin integración bancaria ni manejo real de dinero.
No se valida formato regional del teléfono, solo que sea obligatorio.
Scanner sobre una consola puede esperar hasta recibir una línea o fin de entrada;
una interrupción de hilo se reconoce al recuperar el control, no desbloquea por sí
sola una lectura de consola del sistema. No se usa expiración de bloqueos durante
una lectura porque podría liberar asientos mientras continúa un cobro.
Main y la selección de HU-01 aún no consultan este administrador; la protección se
garantiza al iniciar un cobro mediante Taquilla y el administrador compartido.
