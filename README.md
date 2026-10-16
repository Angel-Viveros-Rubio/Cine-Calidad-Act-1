# Cine-Calidad-Act-1

Proyecto universitario de Metodologías Ágiles desarrollado con Scrum por un equipo de cuatro integrantes.

## Objetivo y estado actual

El objetivo general es construir un sistema de consola para administrar reservaciones de cine. Esta entrega prepara únicamente la estructura inicial para desarrollar historias de usuario en ramas independientes.

HU-01 tiene implementados únicamente los checkpoints 1, 2 y 3: mostrar el precio individual, calcular una selección individual y sumar múltiples asientos. Los demás checkpoints y HU-02 a HU-05 están pendientes. No hay eliminación de asientos, confirmación de reservaciones ni cobros.

## Tecnologías

- Java 21 (JDK 21).
- IntelliJ IDEA.
- Git y GitHub.
- Aplicación de consola sin frameworks, bases de datos ni dependencias externas.

## Estructura

```text
Cine-Calidad-Act-1/
├── src/
│   ├── Main.java
│   ├── Sala.java
│   ├── Asiento.java
│   ├── Reservacion.java
│   ├── CalculadoraPrecio.java
│   └── Taquilla.java
├── .gitignore
└── README.md
```

Main muestra los precios y permite seleccionar asientos escribiendo su identificador; `SALIR` termina la ejecución. Sala proporciona 3 filas de 5 asientos en memoria. Asiento contiene identificador y precio; CalculadoraPrecio suma los importes con BigDecimal. Reservacion y Taquilla siguen vacías.

El precio se configura en `PRECIO_POR_ASIENTO` de Main y comienza en $70.00 MXN. Sala también admite otro precio mediante su constructor. Se rechazan precios negativos o con fracciones de centavo; la consola rechaza asientos inexistentes y repetidos.

## Ejecutar desde IntelliJ IDEA

1. Instalar un JDK 21 y abrir la carpeta del repositorio con **File > Open**.
2. En **File > Project Structure > Project**, seleccionar **Project SDK: 21** y **Language level: 21**, sin funciones preview.
3. En **Modules > Sources**, marcar `src` como **Sources Root** si no se reconoce automáticamente. Verificar que el módulo use el SDK del proyecto.
4. Abrir `src/Main.java` y ejecutar `Main.main()` con el botón de ejecución junto al método.

La configuración de IntelliJ se realiza localmente y está excluida de Git.

## Compilar y ejecutar desde la terminal

Desde la raíz del proyecto, con el JDK 21 en PATH:

```powershell
java -version
javac -version
javac --release 21 -encoding UTF-8 -d out src/*.java
java -cp out Main
```

La salida comienza con:

```text
Sistema de Reservaciones de Cine - Sprint 1
```

Después se muestran los 15 asientos con su precio. Por ejemplo, seleccionar A1 y B2 muestra $70.00 y $140.00 MXN respectivamente. Este bloque solo permite seleccionar y salir.

## Pruebas de HU-01 (checkpoints 1 a 3)

Las pruebas sin dependencias están en `tests/PruebasPrecio.java`:

```powershell
javac --release 21 -encoding UTF-8 -d out src/*.java tests/*.java
java -cp out PruebasPrecio
```

Comprueban 29 condiciones: sala e identificadores, precios individuales, selección vacía, uno y varios asientos, inclusión del último asiento, sala completa, tarifa configurable, decimales exactos y argumentos inválidos. Para verificar la consola, probar entradas vacías, Z9, a1, A1 repetido, B2, C5 y SALIR: solo las tres selecciones válidas deben producir $70.00, $140.00 y $210.00 MXN.

`out/` contiene los compilados y está excluida de Git. `--release 21` establece explícitamente Java 21 como versión de compilación. No se requiere Maven ni Gradle.

## Colaboración mediante ramas

Clonar el repositorio:

```powershell
git clone https://github.com/Angel-Viveros-Rubio/Cine-Calidad-Act-1.git
cd Cine-Calidad-Act-1
```

La estructura inicial se prepara en `estructura-inicial`, creada desde `main`. Los cambios locales de esta entrega solo estarán disponibles para los demás cuando el equipo los revise, confirme y publique. Un clon recibe únicamente los commits publicados en GitHub.

Después de que el equipo publique la rama, puede revisarse con:

```powershell
git fetch origin
git switch --track origin/estructura-inicial
```

Cuando el equipo integre la estructura revisada en `main`, cada integrante debe actualizar su copia y crear una rama para su historia. Ejemplo:

```powershell
git switch main
git pull --ff-only origin main
git switch -c hu-01-precio-total
```

Usar una rama por historia, realizar commits pequeños, compilar antes de compartir y abrir una Pull Request para revisión del equipo. Coordinar cambios en archivos compartidos, especialmente Main.java, y evitar desarrollar directamente sobre main.

Los commits, push, merges y Pull Requests quedan a cargo del equipo; esta preparación no los realiza automáticamente. `.gitignore` excluye configuraciones personales de IntelliJ, compilados y temporales.
