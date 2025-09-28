package hn.sinap.conciliacion;


import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.data.ProgramCallDocument;
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
    private static final com.ibm.as400.access.AS400 AS400 = new AS400("localhost", "*CURRENT", "*CURRENT");
    private static final String PATH = "/QSYS.LIB/BANTRABOBJ.LIB/SRVP013I.SRVPGM";
    private static final String DIR = "/IPZip/";


    public static void main(String[] args) {

        // Recibimos el contenido de pa01 y fecha de cierre del archivo ICP001
        String pa01 = args[0];
        String fecha = args[1];
        String ruta = DIR + "zip-" + fecha + ".zip";

        System.out.println("\nSolicitando archivo ZIP para Conciliación del : " + fecha);
        if (pa01.isEmpty() || fecha.isEmpty()) {
            System.out.println("Atributos pa01 y fecha, no disponibles en IPC001. No se puede solicitar archivo ZIP ");
        } else {
            try {
                PaController controller = new PaController();
                File downloadedFile = controller.obtenerZip(9, "BANCO CUSCATLAN", pa01, ruta);
                System.out.println("Archivo descargado satisfactoriamente. " + downloadedFile.getAbsolutePath());
                System.out.println("Ahora enviamos por correo.. ");

                // # Preparamos llamada a procedimiento SendFileZip
                ProgramCallDocument pcml = new ProgramCallDocument("SRVP013I");
                pcml.setSystem(AS400);
                pcml.setPath("SENDFILEZIP", PATH);
                pcml.setValue("SENDFILEZIP.GFECHA", fecha.trim());
                pcml.setValue("SENDFILEZIP.GPA01", ruta.trim());
                // # we call procedure
                boolean success = pcml.callProgram("SENDFILEZIP");
                if (!success) {
                    AS400Message[] msgs = pcml.getMessageList("SENDFILEZIP");
                    for (AS400Message msg : msgs) {
                        System.out.println("error: " + msg.getID());
                        System.out.println("m s g: " + msg.getText());
                    }
                }else {
                    System.out.println("Correo con archivo ZIP  " +  "zip-" + fecha + ".zip" +
                            " enviado satisfactoriamente\n Fin de proceso." );
                }

            } catch (Exception e) {
                System.err.println("Error al descargar archivo: " + e.getMessage());
                e.printStackTrace();
            }
        }

    }


}


