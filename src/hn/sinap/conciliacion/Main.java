package hn.sinap.conciliacion;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.data.PcmlException;
import com.ibm.as400.data.ProgramCallDocument;
import hn.sinap.conciliacion.controller.ConciliacionController;
import hn.sinap.conciliacion.model.ConciliacionResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Main {
    private static final AS400 AS400 = new AS400("localhost", "*CURRENT", "*CURRENT");
    private static final String PATH = "/QSYS.LIB/BANTRABOBJ.LIB/SRVP013I.SRVPGM";
    private static final int MAX = 20;


    public static void main(String[] args) {


        try {
            String fechaConciliacion = "?fecha=" + args[0].trim();
//            System.out.println("Fecha : " + fechaConciliacion);

            // #1 we do GET on Server and we catch response
            ConciliacionController controller = new ConciliacionController();
            ConciliacionResponse response = controller.obtenerDatosConciliacion(9, "BANCO CUSCATLAN", fechaConciliacion);


            // #2 we create object PCML  and set system and path
            ProgramCallDocument pcml = new ProgramCallDocument("SRVP013I");
            pcml.setSystem(AS400);
            pcml.setPath("RESPONSECONCILIACION", PATH);
            pcml.setValue("RESPONSECONCILIACION.WFECHA", args[0].trim());


            // #3 we validate response, we set parameters for object PCML, and call procedure
            setCallPcml(pcml, response);

        } catch (PcmlException e) {
            System.out.println("error critico. Algo fallo en las instrucciones del try principal");
            e.printStackTrace();
        }


    }

    public static void setCallPcml(ProgramCallDocument pcml, ConciliacionResponse response) throws PcmlException {
        // #3 We validate that response is not null
        if (response != null && response.getMensaje() != null) {
//            System.out.println("-1- response != null");
            // Acá cargamos el PCML
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.MENSAJE", response.getMensaje());
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA_HORA", response.getFecha_hora());


            if (response.getDatos() != null) {
//                System.out.println("-2- response.datos tiene datos");

                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ID", response.getDatos().getId());
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.BANCO", response.getDatos().getBanco());
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA", response.getDatos().getFecha());
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ESTADO", response.getDatos().getEstado());

                if (response.getDatos().getTransacciones() != null) {
//                    System.out.println("-3- response.datos.transacciones tiene datos");

                    List<ConciliacionResponse.Transaccion> trxs = response.getDatos().getTransacciones();
                    int noRegLista = trxs.size();
//                    System.out.println("Elementos<Transacciones> : " + noRegLista);


                    if (noRegLista <= MAX) { // noRegLista <= MAX entonces solamente es (1) vuelta
                        setArrayDefault(pcml);
                        int[] indx = new int[1];

                        for (int polygon = 0; polygon < noRegLista; polygon++) {
                            indx[0] = polygon;

                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ID", indx, trxs.get(polygon).getId());
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.OPERACION", indx, trxs.get(polygon).getOperacion());
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.COMPROBANTE", indx, trxs.get(polygon).getComprobante());
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.PLACA", indx, trxs.get(polygon).getPlaca());
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.TUAV", indx, trxs.get(polygon).getTuav());
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ALCALDIA", indx, trxs.get(polygon).getAlcaldia());
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.SIGLO21", indx, trxs.get(polygon).getSiglo21());
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.VALOR_PLACA", indx, trxs.get(polygon).getValor_placa());
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.REPOSICION", indx, trxs.get(polygon).getReposicion());
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ESTADO", indx, trxs.get(polygon).getEstado());
                        }
                        pcml.setValue("RESPONSECONCILIACION.WRESPONSE.NBR", noRegLista);
                        pcml.setValue("RESPONSECONCILIACION.WFLAG", 1);

                    } else { // noRegLista > MAX entonces ya mínimo son (2) vueltas
                        float x = (float) noRegLista / MAX;
                        int parteEntera = (int) Math.floor(x); // Parte entera
                        float parteDecimal = x - parteEntera;  // Parte decimal

                        // #1 Llenamos PCML de tamaño de MAX las vueltas enteras.
                        int yy = 0;
                        for (int i = 0; i < parteEntera; i++) {
                            int[] indx = new int[1];
                            for (int polygon = 0; polygon < MAX; polygon++) {
                                indx[0] = polygon;
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ID", indx, trxs.get(yy).getId());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.OPERACION", indx, trxs.get(yy).getOperacion());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.COMPROBANTE", indx, trxs.get(yy).getComprobante());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.PLACA", indx, trxs.get(yy).getPlaca());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.TUAV", indx, trxs.get(yy).getTuav());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ALCALDIA", indx, trxs.get(yy).getAlcaldia());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.SIGLO21", indx, trxs.get(yy).getSiglo21());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.VALOR_PLACA", indx, trxs.get(yy).getValor_placa());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.REPOSICION", indx, trxs.get(yy).getReposicion());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ESTADO", indx, trxs.get(yy).getEstado());
                                yy++;
                            }
                            // LLamar a PCML
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.NBR", MAX);
                            pcml.setValue("RESPONSECONCILIACION.WFLAG", i + 1);
                            // #4 we call procedure
                            boolean success = pcml.callProgram("RESPONSECONCILIACION");
                            if (!success) {
                                AS400Message[] msgs = pcml.getMessageList("RESPONSECONCILIACION");
                                for (AS400Message msg : msgs) {
                                    System.out.println("error: " + msg.getID());
                                    System.out.println("m s g: " + msg.getText());
                                }
                            }
                        }// end-for-1

                        // #2 Sí parte_decimal > 0 entonces rellenamos lo último
                        if (parteDecimal > 0) {
                            setArrayDefault(pcml); // Set array Trxs
                            int ult_registros = noRegLista - (MAX * (parteEntera));
                            int[] indx = new int[1];
                            for (int polygon = 0; polygon < ult_registros; polygon++) {
                                indx[0] = polygon;
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ID", indx, trxs.get(yy).getId());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.OPERACION", indx, trxs.get(yy).getOperacion());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.COMPROBANTE", indx, trxs.get(yy).getComprobante());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.PLACA", indx, trxs.get(yy).getPlaca());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.TUAV", indx, trxs.get(yy).getTuav());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ALCALDIA", indx, trxs.get(yy).getAlcaldia());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.SIGLO21", indx, trxs.get(yy).getSiglo21());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.VALOR_PLACA", indx, trxs.get(yy).getValor_placa());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.REPOSICION", indx, trxs.get(yy).getReposicion());
                                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ESTADO", indx, trxs.get(yy).getEstado());
                                yy++;
                            }
                            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.NBR", ult_registros);
                            pcml.setValue("RESPONSECONCILIACION.WFLAG", 2);


                        } else {
                            return; // Salimos del método porque llenamos mínimo dos vueltas
                        }
                    } // Fin noRegLista > MAX entonces ya mínimo son (2) vueltas

                } else {
//                    System.out.println("error -4- response.Datos.trans = null ==> set Default (id-banco..");

                    pcml.setValue("RESPONSECONCILIACION.WRESPONSE.NBR", 0);
                    setArrayDefault(pcml); // Set array Trxs
                    pcml.setValue("RESPONSECONCILIACION.WFLAG", 1);
                }


            } else {
//                System.out.println("error -5- response.Datos = null ==> set Default (id-banco..");

                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ID", "");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.BANCO", 0);
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA", "");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ESTADO", "");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.NBR", 0);
                setArrayDefault(pcml); // Set array Trxs
                pcml.setValue("RESPONSECONCILIACION.WFLAG", 1);

            }

        } else {
//            System.out.println("error -6- response = null entonces mensaje de java personalizado");
            setDefault0(pcml);
        }

        // #4 we call procedure
        boolean success = pcml.callProgram("RESPONSECONCILIACION");
        if (!success) {
            AS400Message[] msgs = pcml.getMessageList("RESPONSECONCILIACION");
            for (AS400Message msg : msgs) {
                System.out.println("error: " + msg.getID());
                System.out.println("m s g: " + msg.getText());
            }
        }

    }

    public static void setDefault0(ProgramCallDocument pcml) throws PcmlException {
        // Response is null then we set parameters default and flag=0 and nbr=0 that say not array
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        pcml.setValue("RESPONSECONCILIACION.WRESPONSE.MENSAJE", "La respuesta del servidor está vacía o es inválida");
        pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA_HORA", formattedDateTime);
        pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ID", "");
        pcml.setValue("RESPONSECONCILIACION.WRESPONSE.BANCO", 0);
        pcml.setValue("RESPONSECONCILIACION.WRESPONSE.FECHA", "");
        pcml.setValue("RESPONSECONCILIACION.WRESPONSE.ESTADO", "");
        pcml.setValue("RESPONSECONCILIACION.WRESPONSE.NBR", 0);
        setArrayDefault(pcml); // Set array Trxs
        pcml.setValue("RESPONSECONCILIACION.WFLAG", 0);


    }

    public static void setArrayDefault(ProgramCallDocument pcml) throws PcmlException {
        int[] indices = new int[1];

        for (int polygon = 0; polygon < MAX; polygon++) {
            indices[0] = polygon;

            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ID", indices, "");
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.OPERACION", indices, 0);
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.COMPROBANTE", indices, 0);
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.PLACA", indices, "");
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.TUAV", indices, 0);
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ALCALDIA", indices, 0);
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.SIGLO21", indices, 0);
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.VALOR_PLACA", indices, 0);
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.REPOSICION", indices, 0);
            pcml.setValue("RESPONSECONCILIACION.WRESPONSE.TRXS.ESTADO", indices, "");

        }

    }


}


