package hn.sinap.conciliacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.data.PcmlException;
import com.ibm.as400.data.ProgramCallDocument;
import hn.sinap.conciliacion.controller.PostTransaccionesController;
import hn.sinap.conciliacion.model.ConciliacionResponse;
import hn.sinap.conciliacion.model.PostTransaccion;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PostConciliacionIP.jar en el AS400.
 * <p>
 * El propósito general de esta clase cuando es llamada por
 * <p>
 * el procedimiento del SRVP013I, es el de disparar proceso
 * <p>
 * para Declarar a la IP su declaración de Cierre.
 * <p>
 * Ósea que posteamos POST a la IP las transacciones del IPC002
 * <p>
 * ya revisadas por operaciones del Banco.
 * <p>
 * Iniciamos llamando el procedimiento GetInfo(); para recuperar
 * <p>
 * el, id de conciliación del IPC001 y no de registros o transacciones del IPC002.
 * <p>
 * Con estos datos validados, llamamos a procedimiento PostIpTransacciones();
 * <p>
 * Del SERVP013I para barrer el IPC y generar la lista que se ocupa para hacer
 * <p>
 * el POST a la IP.
 * <p>
 * Él response recibido no es más que el mismo JSON de GetConciliaciónIP.jar entonces
 * <p>
 * llamamos a al mismo procedimiento PostIpTransacciones(); para llenar los archivos
 * <p>
 * IPC001 e IPC002. Ya con el PA01 con contenido, para proceder a llamar a través
 * <p>
 * de otra clase el GET para obtener el file ZIP de conciliación. *
 * <p>
 */
public class MainPost {
    private static final AS400 AS400 = new AS400("localhost", "*CURRENT", "*CURRENT");
    private static final String PATH = "/QSYS.LIB/BANTRABOBJ.LIB/SRVP013I.SRVPGM";
    private static final int MAX = 20;


    public static void main(String[] args) {

        try {

            //----------- RECUPERAR DATOS DE IPC001 Y IPC002 -------\\
            String id;
            int trxs;
            List<PostTransaccion> listaPost;
            String jsonRequest;


            // #1 Iniciar código para llamar procedimiento que retorne el # de registros que están en IPC002 y
            //    que tome de IPC001 él, id- de-conciliación.
            System.out.println("Iniciando Declaración de Cierre a la IP..");

            ProgramCallDocument pcmlInfo = new ProgramCallDocument("SRVP013I");

            pcmlInfo.setSystem(AS400);
            pcmlInfo.setPath("GETINFOPOST", PATH);

            boolean success = pcmlInfo.callProgram("GETINFOPOST");
            if (success) {
                id = pcmlInfo.getStringValue("GETINFOPOST.WID");
                trxs = pcmlInfo.getIntValue("GETINFOPOST.WREG");
                System.out.println("Id Conciliación : " + id);

                if (!id.equals("nada")) {
                    //----------- RECUPERAR TRANSACCIONES DE IPC002 -------\\

                    // #2 Sí trae datos. Llamar método de extracción llenarListaTransacciones();
                    listaPost = llenarListaTransacciones(trxs);
                    System.out.println("Recuperadas de IPC002          " + trxs + " transacciones..");
                    System.out.println("Declarando a servidor de la IP " + trxs + " transacciones..");

                    //----------- PREPARAR POSTEO A LA IP -------\\

                    ObjectMapper mapper = new ObjectMapper();
                    jsonRequest = mapper.writeValueAsString(listaPost);
                    PostTransaccionesController controller = new PostTransaccionesController();
                    ConciliacionResponse response = controller.postDatosTransacciones(9, "BANCO CUSCATLAN", id, jsonRequest);

                    // #2 we create object PCML  and set system and path
                    ProgramCallDocument pcml = new ProgramCallDocument("SRVP013I");


                    pcml.setSystem(AS400);
                    pcml.setPath("RESPONSECONCILIACION", PATH);

                    // --- PROTECCIÓN ---
                    if (response != null && response.getDatos() != null) {
                        if (response.getDatos().getPa01() == null) {
                            response.getDatos().setPa01("");
                        }
                        pcml.setValue("RESPONSECONCILIACION.WFECHA", response.getDatos().getFecha());
                    } else {
                        // Si no hay datos, enviamos la fecha vacía
                        pcml.setValue("RESPONSECONCILIACION.WFECHA", "");
                        System.out.println("--Aviso -- El servidor de la IP respondió sin datos.");
                    }
                    // ------------------

                    // #3 we validate response, we set parameters for object PCML, and call procedure
                    setCallPcml(pcml, response);

                }


            } else {
                AS400Message[] msgs = pcmlInfo.getMessageList("GETINFOPOST");
                for (AS400Message msg : msgs) {
                    System.out.println("error: " + msg.getID());
                    System.out.println("m s g: " + msg.getText());
                }
            }

        } catch (Exception e) {
            System.out.println("--error critico--. Algo fallo en las instrucciones del try principal");
            e.printStackTrace();
        }


    }

    /**
     * llenarListaTransacciones(); Lista de transacciones a POSTEAR para una conciliación.
     *
     * @param noElementos cantidad de registros vigentes a postear.
     * @return Lista clase PostTransaccion
     * @throws PcmlException exception
     */
    private static List<PostTransaccion> llenarListaTransacciones(int noElementos) throws PcmlException {
        List<PostTransaccion> transacciones = new ArrayList<>();

        int gini = 1;
        ProgramCallDocument pcml = new ProgramCallDocument("SRVP013I");

        pcml.setSystem(AS400);
        pcml.setPath("POSTIPTRANSACCIONES", PATH);

        // #1 noElementos <= MAX entonces solamente es (1) vuelta.
        if (noElementos <= MAX) {
            pcml.setValue("POSTIPTRANSACCIONES.GINI", gini);
            pcml.setValue("POSTIPTRANSACCIONES.GFIN", noElementos);

            boolean success = pcml.callProgram("POSTIPTRANSACCIONES");
            if (success) {
                int[] indx = new int[1];

                for (int polygon = 0; polygon < noElementos; polygon++) {
                    indx[0] = polygon;
                    PostTransaccion trans = new PostTransaccion();
                    trans.setId(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.ID", indx));
                    trans.setOperacion(pcml.getIntValue("POSTIPTRANSACCIONES.GPOST.OPERACION", indx));
                    trans.setComprobante(new BigInteger(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.COMPROBANTE", indx)));
                    trans.setPlaca(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.PLACA", indx));
                    trans.setTuav(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.TUAV", indx));
                    trans.setAlcaldia(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.ALCALDIA", indx));
                    trans.setSiglo21(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.SIGLO21", indx));
                    trans.setValor_placa(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.VALOR_PLACA", indx));
                    trans.setReposicion(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.REPOSICION", indx));
                    transacciones.add(trans);
                }
            } else {
                System.out.println("AS400-1");

                AS400Message[] msgs = pcml.getMessageList("POSTIPTRANSACCIONES");
                for (AS400Message msg : msgs) {
                    System.out.println("error: " + msg.getID());
                    System.out.println("m s g: " + msg.getText());
                }
            }

        } else { // #2 noElementos > MAX lo que indica mínimo una vuelta más
            float x = (float) noElementos / MAX;
            int parteEntera = (int) Math.floor(x); // Parte entera
            float parteDecimal = x - parteEntera;  // Parte decimal

            pcml.setValue("POSTIPTRANSACCIONES.GINI", gini);
            pcml.setValue("POSTIPTRANSACCIONES.GFIN", MAX);
            for (int z = 0; z < parteEntera; z++) { // Llamamos las vueltas enteras con registros MAX
                boolean success = pcml.callProgram("POSTIPTRANSACCIONES");
                if (success) {
                    int[] indx = new int[1];

                    for (int polygon = 0; polygon < MAX; polygon++) {
                        indx[0] = polygon;
                        PostTransaccion trans = new PostTransaccion();
                        trans.setId(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.ID", indx));
                        trans.setOperacion(pcml.getIntValue("POSTIPTRANSACCIONES.GPOST.OPERACION", indx));
                        trans.setComprobante(new BigInteger(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.COMPROBANTE", indx)));
                        trans.setPlaca(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.PLACA", indx));
                        trans.setTuav(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.TUAV", indx));
                        trans.setAlcaldia(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.ALCALDIA", indx));
                        trans.setSiglo21(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.SIGLO21", indx));
                        trans.setValor_placa(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.VALOR_PLACA", indx));
                        trans.setReposicion(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.REPOSICION", indx));
                        transacciones.add(trans);
                    }
                } else {
                    System.out.println("AS400-2");

                    AS400Message[] msgs = pcml.getMessageList("POSTIPTRANSACCIONES");
                    for (AS400Message msg : msgs) {
                        System.out.println("error: " + msg.getID());
                        System.out.println("m s g: " + msg.getText());
                    }
                }
                gini = gini + MAX;
                pcml.setValue("POSTIPTRANSACCIONES.GINI", gini);
            }
            // #3 Si hay parteDecimal calculamos los registros pendientes y hacemos la última llamada
            if (parteDecimal > 0) {
                int ult_registros = noElementos - (MAX * (parteEntera));
                pcml.setValue("POSTIPTRANSACCIONES.GINI", gini);
                pcml.setValue("POSTIPTRANSACCIONES.GFIN", ult_registros);
                boolean success = pcml.callProgram("POSTIPTRANSACCIONES");
                if (success) {
                    int[] indx = new int[1];

                    for (int polygon = 0; polygon < ult_registros; polygon++) {
                        indx[0] = polygon;
                        PostTransaccion trans = new PostTransaccion();
                        trans.setId(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.ID", indx));
                        trans.setOperacion(pcml.getIntValue("POSTIPTRANSACCIONES.GPOST.OPERACION", indx));
                        trans.setComprobante(new BigInteger(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.COMPROBANTE", indx)));
                        trans.setPlaca(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.PLACA", indx));
                        trans.setTuav(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.TUAV", indx));
                        trans.setAlcaldia(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.ALCALDIA", indx));
                        trans.setSiglo21(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.SIGLO21", indx));
                        trans.setValor_placa(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.VALOR_PLACA", indx));
                        trans.setReposicion(pcml.getStringValue("POSTIPTRANSACCIONES.GPOST.REPOSICION", indx));
                        transacciones.add(trans);
                    }
                } else {
                    System.out.println("AS400-3");

                    AS400Message[] msgs = pcml.getMessageList("POSTIPTRANSACCIONES");
                    for (AS400Message msg : msgs) {
                        System.out.println("error: " + msg.getID());
                        System.out.println("m s g: " + msg.getText());
                    }
                }


            }
        }// Fin else principal
        return transacciones;

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
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.PA01", response.getDatos().getPa01());


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
                            // Llamar a PCML
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
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.PA01", "");
                pcml.setValue("RESPONSECONCILIACION.WRESPONSE.NBR", 0);
                setArrayDefault(pcml); // Set array Trxs
                pcml.setValue("RESPONSECONCILIACION.WFLAG", 1);

            }

        } else {
//            System.out.println("error -6- responsé = null entonces mensaje de java personalizado");
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
        pcml.setValue("RESPONSECONCILIACION.WRESPONSE.PA01", "");
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


