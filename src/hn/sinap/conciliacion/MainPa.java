package hn.sinap.conciliacion;


import hn.sinap.conciliacion.controller.PaController;

import java.io.File;

/**
 * GetZip.jar en el AS400.
 * <p>
 * Esta clase es llamada por el programa de servicio
 * <p>
 * BANTRABOBJ/SRVP013I para realizar un GET a la IP
 * <p>
 * solicitando obtener el archivo ZIP de Conciliación Final
 * <p>
 * una vez que el Banco declaro y posteo su aprobación para una
 * <p>
 * Conciliación. Entonces para completar el proceso el Banco
 * <p>
 * tiene que descargar para el área de operaciones
 * <p>
 * un zip que contiene 3 documentos. 2 para el Banco y uno
 * <p>
 * encriptado para que ellos envién a Banco Central.
 * <p>
 * Entonces este zip, se guardará en la ruta /IPZip y su
 * <p>
 * formato será docs-{fecha de cierre. Formato: aaaa-mm-dd}
 * <p>
 * Para tales efectos esta clase principal recibirá 2
 * <p>
 * argumentos:
 * <p>
 * el contenido del campo "pa01"
 * <p>
 * y la fecha del cierre que corresponde.
 */


public class MainPa {
    private static final String DIR = "/IPZip/";


    public static void main(String[] args) {

        // Recibimos el contenido de pa01 y fecha de cierre del archivo ICP001
        String pa01 = args[0];
        String fecha = args[1];
        String ruta = DIR + "zip-" + fecha + ".zip";

        System.out.println("\npa01: " + pa01);
        System.out.println("ruta: " + ruta);

        try {
            PaController controller = new PaController();
            File downloadedFile = controller.obtenerZip(9, "BANCO CUSCATLAN", pa01, ruta);
            System.out.println("Archivo descargado correctamente en: " + downloadedFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error al descargar archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }


}


