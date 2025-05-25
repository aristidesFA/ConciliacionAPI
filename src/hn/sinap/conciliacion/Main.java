package hn.sinap.conciliacion;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.data.PcmlException;
import com.ibm.as400.data.ProgramCallDocument;
import hn.sinap.conciliacion.controller.ConciliacionController;
import hn.sinap.conciliacion.model.ConciliacionResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
//        System.out.println("Ingresamos al MAIN:");
        String path = "/QSYS.LIB/BANTRABOBJ.LIB/SRVP013I.SRVPGM";

        try {
            ConciliacionController controller = new ConciliacionController();
            // Usar los valores reales de tu institución
            ConciliacionResponse response = controller.obtenerDatosConciliacion(9, "BANCO CUSCATLAN");


            AS400 as400 = new AS400("localhost","*CURRENT", "*CURRENT");
            System.out.println("#1 ProgramCallDocument - setSystem");

            ProgramCallDocument pcml = new ProgramCallDocument( "SRVP013I");
            pcml.setSystem(as400);

            System.out.println("#2 pcml.setPath();");
            pcml.setPath("RESPONSECONCILIACION", path);

            if (response != null && response.getMensaje() != null) {
                System.out.println("#3 Dentro de if response ok");

                // Acá cargamos el PCML
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.MENSAJE", response.getMensaje());
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA_HORA", response.getFecha_hora());

                System.out.println("#4 pcml.setValue(MENSAJE - HORA )");


                if (response.getDatos() != null) {
                    System.out.println("#5 Entramos a objeto DATOS y SET  a recuperar (ID-BANCO....)");

                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ID", response.getDatos().getId());
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.BANCO", response.getDatos().getBanco());
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA", response.getDatos().getFecha());
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ESTADO", response.getDatos().getEstado());
                } else {
                    System.out.println("#6 SET objeto DATOS default");

                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ID", "");
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.BANCO", 0);
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA", "");
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ESTADO", "");
                }

            } else {
                System.out.println("#7 La respuesta del servidor está vacía o es inválida");

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedDateTime = now.format(formatter);

                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.MENSAJE", "La respuesta del servidor está vacía o es inválida");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA_HORA", formattedDateTime);
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ID", "");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.BANCO", 0);
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA", "");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ESTADO", "");

                System.out.println("#8 Terminamos SET DE JAVA");

            }

            // Después de haber validado la respuesta del servidor y cargado el PCML controlado DO
            // 2. Llamar a procedimiento "GETSCHOOL"
            System.out.println("#9 ANTES DE LLAMAR A  pcml.callProgram(RESPONSECONCILIACION)");

            boolean success = pcml.callProgram("RESPONSECONCILIACION");

            System.out.println("#10 DESPUES DE LLAMAR A  pcml.callProgram(RESPONSECONCILIACION)");


            if (!success) {
                System.out.println("#11 SUCCESS ERROR");

                AS400Message[] msgs = pcml.getMessageList("RESPONSECONCILIACION");
                for (AS400Message msg : msgs) {
                    System.out.println("error: " + msg.getID());
                    System.out.println("m s g: " + msg.getText());
                }
            }

        } catch (PcmlException e) {
            System.out.println("#12 PcmlException");
            e.printStackTrace();
            System.err.println("Error crítico:");
            e.printStackTrace();

            // Diagnóstico adicional
            System.err.println("Ejecuta en AS400:");

        }

//        catch (IOException e) {
//            throw new RuntimeException(e);
//        }


    }
}

/*
public class Main {
    public static void main(String[] args) {
//        System.out.println("Ingresamos al MAIN:");
        String path = "/QSYS.LIB/BANTRABOBJ.LIB/SRVP013I.SRVPGM";

        try {
            ConciliacionController controller = new ConciliacionController();
            // Usar los valores reales de tu institución
            ConciliacionResponse response = controller.obtenerDatosConciliacion(9, "BANCO CUSCATLAN");

            // 1. Obtener el PCML como recurso del JAR
            InputStream is = Main.class.getResourceAsStream("/SRVP013I.pcml");
            if (is == null) {
                throw new RuntimeException("Archivo PCML no encontrado en el JAR");
            }
            System.out.println("# Paso InputStream");


            // 2. Crear archivo temporal en IFS
            File tempFile = new File("/tmp/SRVP013I.pcml");
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("# Creo en /tmp");

            // 3. Cargar PCML desde archivo físico (constructor con String)
            ProgramCallDocument pcml = new ProgramCallDocument(tempFile.getAbsolutePath());

//            ProgramCallDocument pcml = new ProgramCallDocument( "SRVP013I");
            pcml.setPath("RESPONSECONCILIACION", path);
            System.out.println("#1 ProgramCallDocument");
            System.out.println("#2 pcml.setPath();");
            System.out.println("Ruta del PCML: " + Main.class.getResource("/SRVP013I.pcml"));


            if (response != null && response.getMensaje() != null) {
                System.out.println("#3 Dentro de if response ok");

                // Acá cargamos el PCML
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.MENSAJE", response.getMensaje());
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA_HORA", response.getFecha_hora());

                System.out.println("#4 pcml.setValue(MENSAJE - HORA )");


                if (response.getDatos() != null) {
                    System.out.println("#5 Entramos a objeto DATOS y SET  a recuperar (ID-BANCO....)");

                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ID", response.getDatos().getId());
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.BANCO", response.getDatos().getBanco());
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA", response.getDatos().getFecha());
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ESTADO", response.getDatos().getEstado());
                } else {
                    System.out.println("#6 SET objeto DATOS default");

                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ID", "");
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.BANCO", 0);
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA", "");
                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ESTADO", "");
                }

            } else {
                System.out.println("#7 La respuesta del servidor está vacía o es inválida");

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedDateTime = now.format(formatter);

                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.MENSAJE", "La respuesta del servidor está vacía o es inválida");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA_HORA", formattedDateTime);
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ID", "");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.BANCO", 0);
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA", "");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ESTADO", "");
                System.out.println("#8 Terminamos SET DE JAVA");

            }

            // Después de haber validado la respuesta del servidor y cargado el PCML controlado DO
            // 2. Llamar a procedimiento "GETSCHOOL"
            System.out.println("#9 ANTES DE LLAMAR A  pcml.callProgram(RESPONSECONCILIACION)");

            boolean success = pcml.callProgram("RESPONSECONCILIACION");

            System.out.println("#10 DESPUES DE LLAMAR A  pcml.callProgram(RESPONSECONCILIACION)");


            if (!success) {
                System.out.println("#11 SUCCESS ERROR");

                AS400Message[] msgs = pcml.getMessageList("RESPONSECONCILIACION");
                for (AS400Message msg : msgs) {
                    System.out.println("error: " + msg.getID());
                    System.out.println("m s g: " + msg.getText());
                }
            }

        } catch (PcmlException e) {
            System.out.println("#12 PcmlException");
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
*/





//                System.out.println("Respuesta del servidor:");
//                System.out.println("Mensaje:" + response.getMensaje());
//                System.out.println("Fecha: " + response.getFecha_hora());


//                    System.out.println("datos.id: " + response.getDatos().getId());
//                    System.out.println("datos.banco: " + response.getDatos().getBanco());
//                    System.out.println("datos.fecha: " + response.getDatos().getFecha());
//                    System.out.println("datos.estado: " + response.getDatos().getEstado());

//                    for (ConciliacionResponse. Transaccion trx : response.getDatos().getTransacciones()) {
//imprimimos lista de transacciones objeto pivote
//                        System.out.println("transacciones.datos.id: " + trx.getId());
//                        System.out.println("transacciones.datos.operacion: " + trx.getOperacion());
//                        System.out.println("transacciones.datos.comprobante: " + trx.getComprobante());
//                        System.out.println("transacciones.datos.placa: " + trx.getPlaca());
//                        System.out.println("transacciones.datos.alcaldia: " + trx.getAlcaldia());
//                        System.out.println("transacciones.datos.siglo21: " + trx.getSiglo21());
//                        System.out.println("transacciones.datos.valor_placa: " + trx.getValor_placa());
//                        System.out.println("transacciones.datos.estado: " + trx.getEstado());

//                    }