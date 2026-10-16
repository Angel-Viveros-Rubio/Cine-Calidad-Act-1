# Cine-Calidad-Act-1

Proyecto universitario de Metodologías Ágiles desarrollado con Scrum por un equipo de cuatro integrantes.

## Objetivo y estado actual

El objetivo general es construir un sistema de consola para administrar reservaciones de cine. Esta entrega prepara únicamente la estructura inicial para desarrollar historias de usuario en ramas independientes.

Las clases están vacías, excepto Main, que imprime el mensaje inicial. HU-01, HU-02, HU-03, HU-04 y HU-05 están pendientes. No se han implementado menús, cálculos, validaciones, cobros ni gestión de asientos o reservaciones.

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

Main es el punto de entrada. Sala, Asiento, Reservacion, CalculadoraPrecio y Taquilla son clases vacías para el desarrollo futuro, sin atributos ni métodos.

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

Salida esperada:

```text
Sistema de Reservaciones de Cine - Sprint 1
```

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
