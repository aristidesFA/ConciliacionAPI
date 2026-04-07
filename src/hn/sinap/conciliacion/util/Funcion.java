package hn.sinap.conciliacion.util;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Funcion {

    public Funcion() {
    }


    /**
     * La clase File, representa un archivo. Y a partir de esa clase, lo
     * <p>
     * pasamos como constructor a la clase FileWriter, el cual manejamos su
     * <p>
     * excepción por si el archivo no existe, no se puede escribir en él,
     * <p>
     * etc.
     *
     * @param nombre de archivo
     */
    public static void crearArchivo(String nombre) {
        /*
            nombre = path, ruta completa
            Crea una nueva instancia de archivo al convertir
            la cadena de nombre de ruta dada en un nombre de ruta abstracto.
         */
        File archivo = new File(nombre);

        /* Clase que se encarga de crear el archivo */
        try {
            /*
                Si queremos que el contenido, sea remplazado cada vez que invocamos,
                entonces dejamos así.

                Pero si queremos conservar el contenido previo. Entonces
                agregamos otro argumento (boolean true)
                FileWriter(archivo, true)
             */
            FileWriter escritor = new FileWriter(archivo, true);

            /*
                Escribiendo en el archivo una vez creado.
                Puede ser con write(), pero no encadena.
                Usaremos append()
             */
            escritor.append(LocalDateTime.now().toString()).append("\n")
                    .append("Hola que tal amigos.\n")
                    .append("Todo bien? yo acá escribiendo un archivo...\n")
                    .append("Hasta luego!\n");

            escritor.close();
            System.out.println("File " + nombre + ", creado con éxito...crearArchivo con FileWriter y append");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * FileWriter
     * <p>
     * Probando writer(); en vez de append();
     *
     * @param nombre nombre de archivo
     */
    public static void createArchivo(String nombre) {

        File archivo = new File(nombre);
        /*
            NOTA: para este método, usamos () después del try
            y ahí colocamos la creación de la Instancia.
            Y podemos eliminar él escritor.close(), porque el try
            lo cierra automáticamente.
         */
        try (FileWriter escritor = new FileWriter(archivo, true)) {

            escritor.append(LocalDateTime.now().toString()).append("\n");
            escritor.write("Hola que tal amigos.\n");
            escritor.write("Todo bien? yo acá escribiendo un archivo...\n");
            escritor.write("Hasta luego!\n");
            //escritor.close();
            System.out.println("File " + nombre + ", creado con éxito...createArchivo");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * createArchivoBuffered();
     */
    public static void createArchivoBuffered(String nombre) {

        File archivo = new File(nombre);

        try {
            /* FileWriter crea el archivo y activa bandera */
            FileWriter escritor = new FileWriter(archivo, true);

            BufferedWriter buffer = new BufferedWriter(escritor);

            buffer.append(LocalDateTime.now().toString()).append("\n")
                    .append("Hola que tal amigos\n")
                    .append("Todo bien? yo acá escribiendo un archivo...\n")
                    .append("Hasta luego!\n");
            buffer.close();
            System.out.println("File " + nombre + ", creado con éxito...con BufferedWriter");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * createArchivoPrintWrite();
     *
     * @param directorio carpeta
     * @param nombre     nombre file
     * @param contenido  contenido a escribir
     */
    public static void createArchivoPrintWrite(String directorio, String nombre, String contenido) {

        File dir = new File(directorio);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File archivo = new File(directorio + getSeparador() + nombre);

        try (PrintWriter print = new PrintWriter(new FileWriter(archivo, true))) {

//            print.println();
//            print.println(LocalDateTime.now());

            print.println(contenido);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * leerArchivoBufferedReader();
     *
     * @param nombre ruta con file a leer
     * @return String
     */
    public static String leerArchivoBufferedReader(String nombre) {
        /* Creamos un StringBuilder para guardar el contenido del archivo */
        StringBuilder sb = new StringBuilder();
        /* Creamos nuestra instancia/representación de Archivo */
        File archivo = new File(nombre);
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {


            String linea;
            while ((linea = reader.readLine()) != null) {
                sb.append(linea).append("\n");
            }
            // reader.close();  cerrar recurso acá no es necesario

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();

    }

    /**
     * leerArchivoScanner();
     *
     * @param nombre ruta con file a leer
     * @return String
     */
    public static String leerArchivoScanner(String nombre) {
        /* Creamos un StringBuilder para guardar el contenido del archivo */
        StringBuilder sb = new StringBuilder();
        /* Creamos nuestra instancia/representación de Archivo */
        File archivo = new File(nombre);
        try {

            Scanner s = new Scanner(archivo);
            s.useDelimiter("\n");
            /* Si no ponemos este, muestra palabra por palabra */
            while (s.hasNext()) {
                sb.append(s.next()).append("\n");
            }
            s.close(); // acá si es necesario el cerrar recurso

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();

    }

    public static String getHome() {
        return System.getProperty("user.home", "/home");
    }

    /**
     * getSeparador() propiedad file.separator de files
     *
     * @return valor de la propiedad, y si no lo encuentra retornará por defecto
     */
    public static String getSeparador() {
        return System.getProperty("file.separator", "/");
    }

}
