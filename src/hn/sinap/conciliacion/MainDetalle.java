package hn.sinap.conciliacion;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ibm.as400.access.*;

import com.ibm.as400.data.PcmlException;
import com.ibm.as400.data.ProgramCallDocument;
import hn.sinap.conciliacion.model.DetallePagoPendiente;
import hn.sinap.conciliacion.model.PagosPendientes;
import hn.sinap.conciliacion.util.Funcion;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.DecimalFormat;

/**
 * MainDetalle.jar en el AS400. de INPREMA
 * <p>
 * Esta clase forma parte de proyecto /webapi (Plataforma de Recaudos)
 * <p>
 * Pero va embebida en el IFS del As400 para que sea invocada por los procesos de fin de dia.
 * <p>
 * Y su objetivo es Aplicar el PAGO Y DETALLE VÍA COLAS DE DATOS, de los pagos de
 * <p>
 * Planillas de los Colegios Privados recaudadas por la plataforma.
 * <p>
 * Actualmente, los pagos se recaudan en una tabla digamos secundaria y en el departamento de Operaciones de forma manual
 * <p>
 * los procesan al CORE de BYTE vía VISA ALTERNA. Un endpoint le retorna a INPRENET los pagos recaudados pendientes de aplicar su detalle. Entonces vía visa alterna
 * <p>
 * digitan los valores correspondientes y proceden a aplicar el pago y su detalle correspondiente a través del
 * <p>
 * CORE de BYTE, generando la afectación de la contabilidad y otros módulos.
 * <p>
 * En tal sentido esta clase contiene ya el código probado para lograr ese propósito y la montaremos sobre
 * <p>
 * el As400 para que sea sometida a QBATCH dejando rastro de su procesamiento.
 * <p>
 * A nivel técnico hacemos lo siguiente:
 * <p>
 * <p>
 * 1. Recuperamos la fecha del byte del CORE en el sistema invocando el procedimiento GETFECHABYTE.
 * <p>
 * 2. Usaremos los procedimientos del  endpoint: /webapi/get-pagos-pendientes que retorna exactamente los pagos
 * <p>
 * que necesitamos procesar vía colas de DATOS. GETPAGOSPENDIENTES.
 * <p>
 * 3. Si la llamada a GETPAGOSPENDIENTES tiene elementos entonces mediante SQL almacenamos en una lista los datos
 * código de banco y codigo cajero byte de archivo DPKEYS.
 * <p>
 * 4. Con ambas listas en memoria. Procedemos a barrer la lista de pagos pendientes y por el momento aplicamos todos
 * <p>
 * los que sean tipo de planilla =1 y dejamos registro del proceso completo de ejecución.
 *
 * <p>
 */
//                                        + " | tipoPlanilla " + pago.getTipoPlanilla()


public class MainDetalle {
    //    private static final AS400 AS400 = new AS400("localhost", "*CURRENT", "*CURRENT");
    private static final String PATH = "/QSYS.LIB/ICLIBOBJ.LIB/SRVP002.SRVPGM";
    private static final String DIRECTORIO = "DetallePlanillas";
    private static final String DIR_LOG = "log";
    private static final String NAME_LOG = "proceso_";
    private static String directorioGeneral = "";
    private static String nameLog = "";


    // For COLAS DE DATOS
    private static final String INPUT_QUEUE = "JTELLERI";
    private static final String OUTPUT_QUEUE = "JTELLERO";
    private static final String LIBRARY = "DPINPDTQ";
    private static final String TRX_BYTE_PAGO_REVERSA_COLEGIOS = "3710";

    // Constantes para el formato del mensaje
    private static final int INPUT_MESSAGE_LENGTH = 641;
    //    private static final int OUTPUT_MESSAGE_LENGTH = 1064; // Longitud de la respuesta
    private static final int KEY_LENGTH = 12;
    private static final String NS = "N";


    public static void main(String[] args) {

        directorioGeneral = Funcion.getSeparador() + DIRECTORIO + Funcion.getSeparador() + DIR_LOG;
        nameLog = NAME_LOG + LocalDate.now() + ".txt";
        String fechaByte;
        String contenido;
        try {
            System.out.println("Arrancando MainDetalle... validando librerías y conexión.");

            // 1. Inicializamos la conexión AS400
            AS400 as400 = new AS400("localhost", "*CURRENT", "*CURRENT");

            // 2. FIX: Inyectamos la lista de librerías requeridas al trabajo de JTOpen
            CommandCall cmd = new CommandCall(as400);

            System.out.println(".....inyectando librerías de trabajo.");

            // Agrega aquí todas las librerías donde vivan las tablas de datos que lee SRVP002
            // Si la tabla ya está en el sistema, el error de "ya existe" se ignora, así que es seguro.
            cmd.run("ADDLIBLE LIB(CAINPDAT) POSITION(*LAST)");
            cmd.run("ADDLIBLE LIB(ICLIBOBJ) POSITION(*LAST)");
            cmd.run("ADDLIBLE LIB(ICLIBDAT) POSITION(*LAST)");
            cmd.run("ADDLIBLE LIB(BYTOBJ00) POSITION(*LAST)");
            cmd.run("ADDLIBLE LIB(CLIMPDAT) POSITION(*LAST)");
            cmd.run("ADDLIBLE LIB(DPIMPDAT) POSITION(*LAST)");
            cmd.run("ADDLIBLE LIB(QGPL) POSITION(*LAST)");
            // 3. Llamamos a procedimiento para recuperar los pagos facturados pendientes de procesar vía colas de datos
            System.out.println(".....getFechaByte(); exitosa.");
            fechaByte = getFechaByte(as400);

            // 4. Llamamos a procedimiento para recuperar los pagos facturados pendientes de procesar vía colas de datos
            PagosPendientes pagosPendientes = getPagosPendientes(as400);

            // Validamos que el objeto no sea nulo y que el status sea 200
            if (pagosPendientes != null && pagosPendientes.getStatus() == 200) {
                contenido = "-----------------------------------------\n"
                        + "INICIO DE PROCESO " + LocalDateTime.now() + "\n"
                        + " Fecha Byte  : " + fechaByte + "\n"
                        + " " + pagosPendientes.getNoPagosPendientes() + " Transacciones de Pago Pendientes de procesar.";
                registrarLogProceso_1(contenido,
                        directorioGeneral,
                        nameLog);
                System.out.println("....." + pagosPendientes.getNoPagosPendientes() + " Transacciones de Pago Pendientes de procesar.");
                // 5. Llamamos al método SQL para extraer la lista del archivo DPKEYS
                List<BancoCajero> listaBancos = getListaBancosCajeros();
                if (!listaBancos.isEmpty()) {
                    contenido = " " + listaBancos.size() + "  Códigos Cajero Byte recuperados."
                            + "\n ............Procesando Pagos Planillas..........";
                    registrarLogProceso_1(contenido,
                            directorioGeneral,
                            nameLog);

                    // 6. Ahora procesamos los pagosPendientes tipoPlanilla == 1 para procesar
                    int noPagosProcesados = 0;
                    int noPagosRechazados = 0;
                    int noPagosOmitidos = 0;
                    List<DetallePagoPendiente> listaPagos = pagosPendientes.getDetallePagoPendientes();

                    if (listaPagos != null && !listaPagos.isEmpty()) {
                        for (DetallePagoPendiente pago : listaPagos) {

                            // Validamos que el tipo de planilla sea estrictamente 1
//                            if (pago.getTipoPlanilla() == 1) {
                                // --- Recuperando cajeroByte de <list> DPKEYS ---
                                String cajeroByte = "";
                                int bancoDelPago = pago.getCodigoBanco();

                                for (BancoCajero bc : listaBancos) {
                                    if (bc.getCodigoBanco() == bancoDelPago) {
                                        cajeroByte = bc.getCodigoCajeroByte();
                                        break; // Lo encontramos, detenemos la búsqueda para este pago
                                    }
                                }

                                boolean result = procesarPagoColasDatos(pago,
                                        NS,
                                        directorioGeneral,
                                        nameLog,
                                        cajeroByte,
                                        fechaByte,
                                        as400);

                                if (result) {
                                    noPagosProcesados++;
                                } else {
                                    noPagosRechazados++;
                                }
                                // --------------------------------------------------------
                                // AQUÍ ES DONDE LLAMAREMOS AL MÉTODO DE COLAS DE DATOS.
                                // Ejemplo:
                                // boolean exit = enviarPagoPorDtaq(as400, pago, listaBancos);
                                // --------------------------------------------------------

//                            } else {
//                                noPagosOmitidos++;
//                                DecimalFormat df = new DecimalFormat("#,##0.00");
//                                contenido = "  * OMITIDO Colegio " + pago.getCodigoColegio()
//                                        + " | tipoPlanilla " + pago.getTipoPlanilla()
//                                        + " | AñoMesDePlanilla " + pago.getAnoPlanilla() + "/" + pago.getMesPlanilla()
//                                        + " | noDocentes " + pago.getNoDocentes()
//                                        + " | totalSalarios " + df.format(pago.getTotalSalarios())
//                                        + " | totalAportaciones " + df.format(pago.getTotalAportaciones())
//                                        + " | totalCotizaciones " + df.format(pago.getTotalCotizaciones())
//                                        + " | totalRecargos " + df.format(pago.getTotalRecargos())
//                                        + " | totalCuotasPrestamos " + df.format(pago.getTotalCuotasPrestamos())
//                                        + " | totalPagado " + df.format(pago.getTotalPagado());
//                                registrarLogProceso_1(contenido,
//                                        directorioGeneral,
//                                        nameLog);
//                                // Opcional: Imprimir en consola si se omite por no ser tipo 1
//                            }


                        }
                    } else {
                        System.out.println("La lista de pagos pendientes viene vacía desde el objeto.");
                    }

                    contenido = " Resumen final:\n"
                            + " Pagos Pendientes : " + pagosPendientes.getNoPagosPendientes() + "\n"
                            + " Pagos Procesados : " + noPagosProcesados + "\n"
                            + " Pagos Rechazados : " + noPagosRechazados + "\n"
                            + " Pagos Omitidos   : " + noPagosOmitidos + "\n"
                            + "FIN  DE  PROCESO  " + LocalDateTime.now();
                    registrarLogProceso_1(contenido,
                            directorioGeneral,
                            nameLog);
                } else {
                    contenido = ".....Lista DPKEYS no tiene elementos. Vuelva a ejecutar y si "
                            + "persiste problema, informar al administrador del sistema. "
                            + "FIN DE PROCESO " + LocalDateTime.now();
                    registrarLogProceso_1(contenido,
                            directorioGeneral,
                            nameLog);
                    System.out.println(contenido);
                }

                // --- AQUÍ ABAJO IRÁ TU LÓGICA DE ACTUALIZACIÓN POSTERIOR ---
                // (Donde cruzarás la lista de pagosPendientes con listaBancos)

            } else {
                contenido = ".....No hay transacciones pendientes de procesar."
                        + "FIN  DE  PROCESO  " + LocalDateTime.now() + "\n";
                registrarLogProceso_1(contenido,
                        directorioGeneral,
                        nameLog);
                System.out.println(contenido);
            }


            System.out.println("Proceso finalizado con éxito.");
        } catch (Throwable t) {
            // Usar Throwable atrapa incluso cuando faltan librerías (Errores del Sistema)
            System.err.println("ERROR FATAL DE ARRANQUE: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * callGetPagosPendientes(); Recupera de procedimiento GETPAGOSPENDIENTES
     * <p>
     * los pagos recaudados por la plataforma, pero que están pendientes de
     * <p>
     * procesar su pago y detalle a través de colas de datos.
     * <p>
     * #1 Registramos la invocación de esta clase en un log dentro de la ruta
     * <p>
     * /PagosPlanillasColas/log/
     * <p>
     * #2 Llamamos a Procedimiento vía PCML y alimentamos lista.
     *
     * @return PagosPendientes objeto
     */
    public static PagosPendientes getPagosPendientes(AS400 as400) {


        PagosPendientes pagosPendientes = null;

        String msgId, msgText, contenido, fileName;

        try {

            // 1. Cargar el documento PCML
            ProgramCallDocument pcml = new ProgramCallDocument("SRVP002");
            pcml.setSystem(as400);

            // 2. Establecer procedimiento en el As400 a llamar y su  parámetro de entrada
            pcml.setPath("GETPAGOSPENDIENTES", PATH);

            // 2. Llamar a procedimiento
            boolean success = pcml.callProgram("GETPAGOSPENDIENTES");

            if (success) {
                pagosPendientes = new PagosPendientes();
                pagosPendientes.setStatus(pcml.getIntValue("GETPAGOSPENDIENTES.GSTATUS"));

                if (pagosPendientes.getStatus() == 200) {
                    pagosPendientes.setMessage(pcml.getStringValue("GETPAGOSPENDIENTES.GMSG"));
                    pagosPendientes.setNoPagosPendientes(pcml.getIntValue("GETPAGOSPENDIENTES.GNBR"));
                    // Recogemos Pagos Pendientes de procesar su detalle
                    pagosPendientes.setDetallePagoPendientes(getJpagosPendientes(pagosPendientes.getNoPagosPendientes(), as400));
                }
            } else {
                AS400Message[] msgs = pcml.getMessageList("GETPAGOSPENDIENTES");
                for (AS400Message msg : msgs) {
                    msgId = msg.getID();
                    msgText = msg.getText();
                    fileName = msg.getFileName();
                    contenido = "AS400Message[]: \n"
                            + "msgId  : " + msgId + "\n"
                            + "msgText: " + msgText + "\n"
                            + "fileName: " + fileName + "\n";
                    Funcion.createArchivoPrintWrite(directorioGeneral,
                            nameLog,
                            contenido);
                }
            }

        } catch (PcmlException e) {
//            e.printStackTrace();
            registrarResponsePcmlExceptionColegios(e, directorioGeneral, nameLog);
            return pagosPendientes;
        }

        return pagosPendientes;
    }

    /**
     * Método para recuperar los pagos pendientes de los colegios para procesar su detalle.
     *
     * @param gnbr total de pagos pendientes de procesar detalle.
     * @return Arreglo de pagos pendientes
     * @throws PcmlException exception
     */

    private static List<DetallePagoPendiente> getJpagosPendientes(int gnbr, AS400 as400) throws PcmlException {
        // Obtener detalle de Pagos pendientes
        int MAX = 20;
        List<DetallePagoPendiente> detallePagoPendientes = new ArrayList<>();
        int gini = 1;
        ProgramCallDocument pcml = new ProgramCallDocument(as400, "SRVP002");
        pcml.setPath("DETAPAGOSPENDIENTES", PATH);

// #1 noElementos <= MAX entonces solamente es (1) vuelta.
        if (gnbr <= MAX) {
            pcml.setValue("DETAPAGOSPENDIENTES.GINI", gini);
            pcml.setValue("DETAPAGOSPENDIENTES.GFIN", gnbr);
            boolean success = pcml.callProgram("DETAPAGOSPENDIENTES");
            if (success) {
                int[] indx = new int[1];

                for (int polygon = 0; polygon < gnbr; polygon++) {
                    indx[0] = polygon;
                    DetallePagoPendiente p1 = new DetallePagoPendiente();
                    p1.setCodigoColegio(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GCODCOLEGIO", indx));
                    p1.setTipoPlanilla(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GTIPOPLANILLA", indx));
                    p1.setAnoPlanilla(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GANO", indx));
                    p1.setMesPlanilla(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GMES", indx));
                    p1.setNoDocentes(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GNODOCENTES", indx));
                    // Extrayendo Campos Decimales
                    p1.setTotalSalarios(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTSALARIOS", indx));
                    p1.setTotalAportaciones(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTAPORTACIONES", indx));
                    p1.setTotalCotizaciones(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTCOTIZACIONES", indx));
                    p1.setTotalRecargos(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTRECARGOS", indx));
                    p1.setTotalCuotasPrestamos(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTCOUTASPRESTAMOS", indx));
                    p1.setTotalPagado(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTALPAGADO", indx));
                    p1.setAnoDePago(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GANOPAGO", indx));
                    p1.setMesDePago(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GMESPAGO", indx));
                    p1.setDiaDePago(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GDIAPAGO", indx));
                    p1.setHoraPago(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GHORAPAGO", indx));
                    p1.setFechaPagoByte(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GFECHABYTE", indx));
                    p1.setCajero(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GCAJERO", indx));
                    p1.setAgencia(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GAGENCIA", indx));
                    p1.setCodigoBanco(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GCODBANCO", indx));
                    p1.setCodigoDeConfirmacionDePago(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GCONFIRMACIONDEPAGO", indx));

                    detallePagoPendientes.add(p1);
                }
            } else {
                System.out.println("AS400-1");

                AS400Message[] msgs = pcml.getMessageList("DETAPAGOSPENDIENTES");
                for (AS400Message msg : msgs) {
                    System.out.println("error: " + msg.getID());
                    System.out.println("m s g: " + msg.getText());
                }
            }

        } else { // #2 noElementos > MAX lo que indica mínimo una vuelta más
            float x = (float) gnbr / MAX;
            int parteEntera = (int) Math.floor(x); // Parte entera
            float parteDecimal = x - parteEntera;  // Parte decimal

            pcml.setValue("DETAPAGOSPENDIENTES.GINI", gini);
            pcml.setValue("DETAPAGOSPENDIENTES.GFIN", MAX);
            for (int z = 0; z < parteEntera; z++) { // Llamamos las vueltas enteras con registros MAX
                boolean success = pcml.callProgram("DETAPAGOSPENDIENTES");
                if (success) {
                    int[] indx = new int[1];

                    for (int polygon = 0; polygon < MAX; polygon++) {
                        indx[0] = polygon;
                        DetallePagoPendiente p1 = new DetallePagoPendiente();
                        p1.setCodigoColegio(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GCODCOLEGIO", indx));
                        p1.setTipoPlanilla(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GTIPOPLANILLA", indx));
                        p1.setAnoPlanilla(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GANO", indx));
                        p1.setMesPlanilla(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GMES", indx));
                        p1.setNoDocentes(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GNODOCENTES", indx));
                        // Extrayendo Campos Decimales
                        p1.setTotalSalarios(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTSALARIOS", indx));
                        p1.setTotalAportaciones(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTAPORTACIONES", indx));
                        p1.setTotalCotizaciones(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTCOTIZACIONES", indx));
                        p1.setTotalRecargos(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTRECARGOS", indx));
                        p1.setTotalCuotasPrestamos(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTCOUTASPRESTAMOS", indx));
                        p1.setTotalPagado(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTALPAGADO", indx));
                        p1.setAnoDePago(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GANOPAGO", indx));
                        p1.setMesDePago(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GMESPAGO", indx));
                        p1.setDiaDePago(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GDIAPAGO", indx));
                        p1.setHoraPago(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GHORAPAGO", indx));
                        p1.setFechaPagoByte(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GFECHABYTE", indx));
                        p1.setCajero(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GCAJERO", indx));
                        p1.setAgencia(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GAGENCIA", indx));
                        p1.setCodigoBanco(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GCODBANCO", indx));
                        p1.setCodigoDeConfirmacionDePago(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GCONFIRMACIONDEPAGO", indx));

                        detallePagoPendientes.add(p1);
                    }
                } else {
                    System.out.println("AS400-2");

                    AS400Message[] msgs = pcml.getMessageList("DETAPAGOSPENDIENTES");
                    for (AS400Message msg : msgs) {
                        System.out.println("error: " + msg.getID());
                        System.out.println("m s g: " + msg.getText());
                    }
                }
                gini = gini + MAX;
                pcml.setValue("DETAPAGOSPENDIENTES.GINI", gini);
            }
            // #3 Si hay parteDecimal calculamos los registros pendientes y hacemos la última llamada
            if (parteDecimal > 0) {
                int ult_registros = gnbr - (MAX * (parteEntera));
                pcml.setValue("DETAPAGOSPENDIENTES.GINI", gini);
                pcml.setValue("DETAPAGOSPENDIENTES.GFIN", ult_registros);
                boolean success = pcml.callProgram("DETAPAGOSPENDIENTES");
                if (success) {
                    int[] indx = new int[1];

                    for (int polygon = 0; polygon < ult_registros; polygon++) {
                        indx[0] = polygon;
                        DetallePagoPendiente p1 = new DetallePagoPendiente();
                        p1.setCodigoColegio(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GCODCOLEGIO", indx));
                        p1.setTipoPlanilla(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GTIPOPLANILLA", indx));
                        p1.setAnoPlanilla(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GANO", indx));
                        p1.setMesPlanilla(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GMES", indx));
                        p1.setNoDocentes(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GNODOCENTES", indx));
                        // Extrayendo Campos Decimales
                        p1.setTotalSalarios(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTSALARIOS", indx));
                        p1.setTotalAportaciones(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTAPORTACIONES", indx));
                        p1.setTotalCotizaciones(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTCOTIZACIONES", indx));
                        p1.setTotalRecargos(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTRECARGOS", indx));
                        p1.setTotalCuotasPrestamos(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTCOUTASPRESTAMOS", indx));
                        p1.setTotalPagado(parseDecimal(pcml, "DETAPAGOSPENDIENTES.JPAGOSP.GTOTALPAGADO", indx));
                        p1.setAnoDePago(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GANOPAGO", indx));
                        p1.setMesDePago(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GMESPAGO", indx));
                        p1.setDiaDePago(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GDIAPAGO", indx));
                        p1.setHoraPago(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GHORAPAGO", indx));
                        p1.setFechaPagoByte(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GFECHABYTE", indx));
                        p1.setCajero(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GCAJERO", indx));
                        p1.setAgencia(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GAGENCIA", indx));
                        p1.setCodigoBanco(pcml.getIntValue("DETAPAGOSPENDIENTES.JPAGOSP.GCODBANCO", indx));
                        p1.setCodigoDeConfirmacionDePago(pcml.getStringValue("DETAPAGOSPENDIENTES.JPAGOSP.GCONFIRMACIONDEPAGO", indx));

                        detallePagoPendientes.add(p1);

                    }
                } else {
                    System.out.println("AS400-3");

                    AS400Message[] msgs = pcml.getMessageList("DETAPAGOSPENDIENTES");
                    for (AS400Message msg : msgs) {
                        System.out.println("error: " + msg.getID());
                        System.out.println("m s g: " + msg.getText());
                    }
                }


            }
        }// Fin else principal

        System.out.println(".....getJpagosPendientes(); exitosa ");

        return detallePagoPendientes;

    }

    /**
     * Recuperamos fecha de proceso actual del core de Byte
     *
     * @param as400 objeto de comunicaciones
     * @return String con Fecha de proceso Byte actual (8char)
     */

    public static String getFechaByte(AS400 as400) {


        String fechaByte = "00000000";

        String msgId, msgText, contenido, fileName;

        try {

            // 1. Cargar el documento PCML
            ProgramCallDocument pcml = new ProgramCallDocument("SRVP002");
            pcml.setSystem(as400);

            // 2. Establecer procedimiento en el As400 a llamar y su  parámetro de entrada
            pcml.setPath("GETFECHABYTE", PATH);

            // 2. Llamar a procedimiento
            boolean success = pcml.callProgram("GETFECHABYTE");

            if (success) {
                // Recuperamos el valor numérico (que puede venir sin el cero inicial)
                String rawFecha = pcml.getStringValue("GETFECHABYTE.GFECHAHOY").trim();

                // Usamos tu método existente para forzar estrictamente las 8 posiciones
                fechaByte = formatNumericField(rawFecha, 8);
            } else {
                AS400Message[] msgs = pcml.getMessageList("GETFECHABYTE");
                for (AS400Message msg : msgs) {
                    msgId = msg.getID();
                    msgText = msg.getText();
                    fileName = msg.getFileName();
                    contenido = "AS400Message[]: \n"
                            + "msgId  : " + msgId + "\n"
                            + "msgText: " + msgText + "\n"
                            + "fileName: " + fileName + "\n";
                    Funcion.createArchivoPrintWrite(directorioGeneral,
                            nameLog,
                            contenido);
                }
            }

        } catch (PcmlException e) {
//            e.printStackTrace();
            registrarResponsePcmlExceptionColegios(e, directorioGeneral, nameLog);
            return fechaByte;
        }

        return fechaByte;
    }

    /**
     * parseDecimal(); Método interno que recibe recupera data tipo
     * <p>
     * type="zoned" length="9" precision="2" pra el manejo de valores
     * <p>
     * decimales.
     *
     * @param pcml    Documento
     * @param path    data o campo especifico a recuperar
     * @param indices parámetro requerido int[] índices del array
     * @return BigDecimal
     */
    private static BigDecimal parseDecimal(ProgramCallDocument pcml, String path, int[] indices) {
        try {
            String stringValue = pcml.getStringValue(path, indices).trim();
            return stringValue.isEmpty() ? BigDecimal.ZERO : new BigDecimal(stringValue);
        } catch (Exception e) {
            System.err.println("Error parsing decimal for " + path + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }


    /**
     * registrarLogProceso_1();
     *
     * @param contenido         String
     * @param directorioGeneral folder
     * @param nameLog           nombre de archivo
     */
    private static void registrarLogProceso_1(String contenido,
                                              String directorioGeneral,
                                              String nameLog) {
        Funcion.createArchivoPrintWrite(directorioGeneral, nameLog, contenido);
    }

    /**
     * registrarResponsePcmlExceptionColegios(); Registra error producido la
     * <p>
     * llamada PCML a método del programa de servicio.
     *
     * @param e exception encontrada
     */
    private static void registrarResponsePcmlExceptionColegios(PcmlException e,
                                                               String directorioGeneral,
                                                               String nameLog) {
        String contenido = "response error PcmlException: "
                + e.getMessage();
        Funcion.createArchivoPrintWrite(directorioGeneral, nameLog, contenido);


    }


    /**
     * Extrae vía SQL la lista de Bancos y su Código de Cajero Byte del archivo DPKEYS
     *
     * @return Lista de objetos BancoCajero
     */
    public static List<BancoCajero> getListaBancosCajeros() {
        List<BancoCajero> listaBancos = new ArrayList<>();

        // USANDO LOS NOMBRES REALES DE TU ARCHIVO FÍSICO
        String sql = "SELECT KCODBCO, KCAJCOL FROM ICLIBDAT.DPKEYS";

        try {
            System.out.println(".....getListaBancosCajeros(); exitosa vía JDBC.");
            System.out.println(".....Procesando Pagos.");


            // REGISTRO MANUAL DEL DRIVER
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");

            // CONEXIÓN SIN USUARIO/CONTRASEÑA PARA QUE ASUMA *CURRENT
            Connection conn = DriverManager.getConnection("jdbc:as400://localhost");

            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // EXTRACCIÓN CON LOS NOMBRES EXACTOS DE LAS COLUMNAS
                int codBanco = rs.getInt("KCODBCO");
                String cajeroByte = rs.getString("KCAJCOL");

                if (cajeroByte != null && !cajeroByte.trim().isEmpty()) {
                    listaBancos.add(new BancoCajero(codBanco, cajeroByte.trim()));
                }
            }

            rs.close();
            pstmt.close();
            conn.close();

        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró la clase del controlador JDBC (jt400): " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Error fatal extrayendo datos de DPKEYS vía SQL: " + e.getMessage());
            e.printStackTrace();
        }

        return listaBancos;
    }

    /**
     * Clase auxiliar para almacenar el código de Banco y su Cajero Byte
     */
    public static class BancoCajero {
        private int codigoBanco;
        private String codigoCajeroByte;

        public BancoCajero(int codigoBanco, String codigoCajeroByte) {
            this.codigoBanco = codigoBanco;
            this.codigoCajeroByte = codigoCajeroByte;
        }

        public int getCodigoBanco() {
            return codigoBanco;
        }

        public String getCodigoCajeroByte() {
            return codigoCajeroByte;
        }
    }

    /**
     * procesarPagoColasDatos();
     * <p>
     * Procesa un pago recaudado por la /webapi y que está pendiente su registro en colas de datos.
     *
     * @param pagoPlanilla      recuperado de getPagosPendientes();
     * @param ns                operacion = N
     * @param directorioGeneral directorio de registros
     * @param nameLog           archivo de registro
     * @param codigoCajeroByte  cajero Byte
     * @param fechaProcesoByte  fecha de proceso Byte
     * @param as400             objeto de conexión
     * @return boolean
     */

    public static boolean procesarPagoColasDatos(DetallePagoPendiente pagoPlanilla,
                                                 String ns,
                                                 String directorioGeneral,
                                                 String nameLog,
                                                 String codigoCajeroByte,
                                                 String fechaProcesoByte,
                                                 AS400 as400) {

        DataQueue inputQueue;        // JTELLERI es FIFO
        KeyedDataQueue outputQueue;  // JTELLERO es KEYED
        String contenido;
        // 1. Creamos el formateador (Comas para miles, punto para 2 decimales)
        DecimalFormat df = new DecimalFormat("#,##0.00");

        // 2. Aplicamos el formato a cada BigDecimal
        contenido = "  * Colegio " + pagoPlanilla.getCodigoColegio()
                + " | tipoPlanilla " + pagoPlanilla.getTipoPlanilla()
                + " | AñoMesDePlanilla " + pagoPlanilla.getAnoPlanilla() + "/" + pagoPlanilla.getMesPlanilla()
                + " | noDocentes " + pagoPlanilla.getNoDocentes()
                + " | totalSalarios " + df.format(pagoPlanilla.getTotalSalarios())
                + " | totalAportaciones " + df.format(pagoPlanilla.getTotalAportaciones())
                + " | totalCotizaciones " + df.format(pagoPlanilla.getTotalCotizaciones())
                + " | totalRecargos " + df.format(pagoPlanilla.getTotalRecargos())
                + " | totalCuotasPrestamos " + df.format(pagoPlanilla.getTotalCuotasPrestamos())
                + " | totalPagado " + df.format(pagoPlanilla.getTotalPagado());
        registrarLogProceso_1(contenido,
                directorioGeneral,
                nameLog);

        //----------------------------------------
        try {
            // JTELLERI es FIFO - usar DataQueue normal
            inputQueue = new DataQueue(as400, "/QSYS.LIB/" + LIBRARY + ".LIB/" + INPUT_QUEUE + ".DTAQ");
            // JTELLERO es KEYED - usar KeyedDataQueue
            outputQueue = new KeyedDataQueue(as400, "/QSYS.LIB/" + LIBRARY + ".LIB/" + OUTPUT_QUEUE + ".DTAQ");

//            System.out.println(".....conexión establecida con colas JTELLERI and JTELLERO");

            // Enviar mensaje y recibir respuesta
            String result = executeQueryPagoColegios(
                    pagoPlanilla,
                    ns,
                    directorioGeneral,
                    nameLog,
                    codigoCajeroByte,
                    fechaProcesoByte,
                    inputQueue,
                    outputQueue,
                    as400);

            // Validamos (3) errores posibles de envío y recepción de trama de colas de datos.
            if (result.equals("Error: No se recibió respuesta del servidor")) {
                contenido = "   " + result;
                registrarLogProceso_1(contenido,
                        directorioGeneral,
                        nameLog);
                return false;
            }
            if (result.startsWith("Error en la ejecución")) {
                contenido = "   " + result;
                registrarLogProceso_1(contenido,
                        directorioGeneral,
                        nameLog);
                return false;
            }
            if (result.startsWith("Error: No se pudo enviar el mensaje")) {
                contenido = "   " + result;
                registrarLogProceso_1(contenido,
                        directorioGeneral,
                        nameLog);
                return false;
            }

            // Mostrar resultado
//            System.out.println("✓ Trama recibida, length = " + result.length());
//            System.out.println(result);
            contenido = "    TramaOut: "
                    + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                    + "\n"
                    + "    " + result + "  length " + result.length();
            registrarLogProceso_1(contenido,
                    directorioGeneral,
                    nameLog);

            // Ahora validamos el result
            // Si no hay errores de recepción de la trama entonces:

            String openId = result.substring(0, 1);
            String codError = result.substring(1, 4);
            String codAutorizacionByte = result.substring(4, 10);
            boolean autorizado = openId.equals("0") && codError.equals("000") && !(codAutorizacionByte.equals("000000"));

            if (autorizado) {
                contenido = "    Pago aceptado, código autorización: " + codAutorizacionByte;
                registrarLogProceso_1(contenido,
                        directorioGeneral,
                        nameLog);
                //----- ACA LLAMAR A PROCEDIMIENTO DE CAMBIO DE ESTADO DE P a D
                callDetalleAplicado(pagoPlanilla,
                        directorioGeneral,
                        nameLog,
                        as400);
                return true;

            } else {
                contenido = "    Pago rechazado, código error: " + codError;
                registrarLogProceso_1(contenido,
                        directorioGeneral,
                        nameLog);
                return false;
            }


        } catch (Exception e) {
            throw new RuntimeException("Error inicializando conexión AS400: " + e.getMessage(), e);
        }
    }

    /**
     * executeQueryPagoColegios();
     *
     * @param pagoPlanilla      pago a aplicar
     * @param ns                NS
     * @param directorioGeneral directorio registro
     * @param nameLog           archivo registro
     * @param codigoCajeroByte  cajero Byte
     * @param fechaProcesoByte  fecha de proceso de Byte
     * @param inputQueue        cola entrada
     * @param outputQueue       cola salida
     * @return String
     */

    public static String executeQueryPagoColegios(DetallePagoPendiente pagoPlanilla,
                                                  String ns,
                                                  String directorioGeneral,
                                                  String nameLog,
                                                  String codigoCajeroByte,
                                                  String fechaProcesoByte,
                                                  DataQueue inputQueue,
                                                  KeyedDataQueue outputQueue,
                                                  AS400 as400) {

        try {
            String uniqueKey = generateUniqueKey();
            String inputMessage = buildInputMessagePagoColegio(
                    uniqueKey,
                    ns,
                    pagoPlanilla,
                    codigoCajeroByte,
                    fechaProcesoByte);
//            System.out.println("✓ Trama de consulta, length = " + inputMessage.length());
//            String contenido = "   TramaIn: "
//                    + LocalTime.now()
//                    + "\n"
//                    + inputMessage + "  length" + inputMessage.length();
            String contenido = "    TramaIn: "
                    + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                    + "\n"
                    + "    " + inputMessage + "  length " + inputMessage.length();
            registrarLogProceso_1(contenido,
                    directorioGeneral,
                    nameLog);
//            System.out.println(inputMessage);
//
//            System.out.println("✓ Enviando trama");

            if (!sendMessageToInputQueue(inputMessage, inputQueue, as400)) {
                return "Error: No se pudo enviar el mensaje";
            }

//            System.out.println("✓ Esperando respuesta de servidor principal");
            String response = receiveMessageFromOutputQueue(uniqueKey,
                    5 * 60 * 1000,
                    as400,
                    outputQueue);

            return (response != null) ? response : "Error: No se recibió respuesta del servidor";

        } catch (Exception e) {
            return "Error en la ejecución: " + e.getMessage();
        }
    }

    /**
     * generateUniqueKey();
     *
     * @return llave única para escribir en cola de datos JTELLERI.
     */
    private static String generateUniqueKey() {
        return new SimpleDateFormat("yyMMddHHmmss").format(new Date());
    }

    /**
     * buildInputMessagePagoColegio()
     * <p>
     * Construir solicitud de Pago de Planilla de Colegio Privado
     *
     * @param uniqueKey        llave de cola
     * @param ns               N
     * @param pagoPlanilla     objeto DetallePagoPendiente
     * @param codigoCajeroByte cajero byte
     * @param fechaProcesoByte fecha de proceso
     * @return String
     */
    private static String buildInputMessagePagoColegio(String uniqueKey,
                                                       String ns,
                                                       DetallePagoPendiente pagoPlanilla,
                                                       String codigoCajeroByte,
                                                       String fechaProcesoByte) {

        StringBuilder message = new StringBuilder(INPUT_MESSAGE_LENGTH);

        // PAR01: Llave del mensaje (12 caracteres) - Será usada como clave
        message.append(formatTextField(uniqueKey, 12));
        // PAR02: Transacción N=normal or S=Reversa
        message.append(formatTextField(ns, 1)); // PAR02: Transacción N=normal or S=Reversa
        // PAR03: Código de Transacción
        message.append(formatNumericField(TRX_BYTE_PAGO_REVERSA_COLEGIOS, 4));
        // PAR04: Código de Cajero Byte
        message.append(formatNumericField(codigoCajeroByte, 5));
        // PAR05: Fecha de proceso Byte, jalada del CORE (donde no necesariamente es la del día actual
        message.append(formatNumericField(fechaProcesoByte, 8));
        // PAR06: Hora de la transacción
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
        message.append(formatTextField(timeFormat.format(new Date()), 6));
        // PAR07: Código de Agencia (Ojo: Definir esto con TOME para cada Banco)
        message.append(formatNumericField("1", 3));
        // PAR08: Código Terminal (Ojo: Definir esto con TOME para cada Banco)
        message.append(formatNumericField("1", 2));

        // PAR09: Código de Colegio
        message.append(formatNumericField(pagoPlanilla.getCodigoColegio(), 20));

        // PAR10: Año de Planilla
        message.append(formatNumericField(String.valueOf(pagoPlanilla.getAnoPlanilla()), 20));

        // PAR11: Mes de Planilla
        message.append(formatNumericField(String.valueOf(pagoPlanilla.getMesPlanilla()), 20));

        // PAR12: Tipo de Planilla
        message.append(formatNumericField(String.valueOf(pagoPlanilla.getTipoPlanilla()), 20));

        // PAR13: Total Sueldo y Salarios
        message.append(convertirBigDecimal(pagoPlanilla.getTotalSalarios()));

        // PAR14: Total Aportaciones
        message.append(convertirBigDecimal(pagoPlanilla.getTotalAportaciones()));

        // PAR15: Total Cotizaciones
        message.append(convertirBigDecimal(pagoPlanilla.getTotalCotizaciones()));

        // PAR16: Total Recargo
        message.append(convertirBigDecimal(pagoPlanilla.getTotalRecargos()));

        // PAR17: Total Préstamos
        message.append(convertirBigDecimal(pagoPlanilla.getTotalCuotasPrestamos()));

        // PAR18: Total a Pagar
        message.append(convertirBigDecimal(pagoPlanilla.getTotalPagado()));

        while (message.length() < INPUT_MESSAGE_LENGTH) {
            message.append(" ");
        }

        return message.toString();
    }

    /**
     * formatTextField();
     *
     * @param value  valor
     * @param length longitud
     * @return String
     */
    private static String formatTextField(String value, int length) {
        return String.format("%-" + length + "s", value);
    }

    /**
     * formatNumericField();
     *
     * @param value  valor
     * @param length longitud
     * @return String
     */
    private static String formatNumericField(String value, int length) {
        try {
            BigInteger numericValue = new BigInteger(value);
            return String.format("%0" + length + "d", numericValue);
        } catch (NumberFormatException e) {
            return String.format("%" + length + "s", value).replace(' ', '0');
        }
    }

    /**
     * Convierte un BigDecimal a String de longitud 20 rellenos de ceros a la izquierda,
     * eliminando el punto decimal.
     *
     * @param monto el número a convertir
     * @return String de 20 caracteres con ceros a la izquierda y sin punto decimal
     */
    public static String convertirBigDecimal(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto no puede ser nulo");
        }

        // Convertir directamente a centavos (valor entero)
        long valorEnCentavos = monto.setScale(2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .longValue();

        // Rellenar con ceros a la izquierda
        return String.format("%020d", valorEnCentavos);
    }

    /**
     * ENVIAR MENSAJE A COLA DE ENTRADA FIFO (JTELLERI)
     */
    private static boolean sendMessageToInputQueue(String message, DataQueue inputQueue, AS400 as400) {
        try {
            byte[] messageData = stringToEbcdic(message, INPUT_MESSAGE_LENGTH, as400);
            inputQueue.write(messageData);
//            System.out.println("✓ Mensaje enviado a JTELLERI (FIFO)");
            return true;

        } catch (Exception e) {
//            System.err.println("✗ Error enviando a JTELLERI: " + e.getMessage());
            return false;
        }
    }

    private static byte[] stringToEbcdic(String text, int length, AS400 as400) {
        try {
            // Usamos la conexión PCML por defecto, pero si es null, usamos la de Colas de Datos
//            AS400 systemToUse = (this.as400 != null) ? this.as400 : this.as400Dtaq;

            AS400Text converter = new AS400Text(length, as400);
            return converter.toBytes(formatTextField(text, length));
        } catch (Exception e) {
            throw new RuntimeException("Error EBCDIC: " + e.getMessage(), e);
        }
    }

    /**
     * RECIBIR MENSAJE DE COLA DE SALIDA KEYED (JTELLERO)
     * USANDO KeyedDataQueue.read(byte[] key) - MÉTODO CORRECTO
     */
    private static String receiveMessageFromOutputQueue(String expectedKey,
                                                        int timeoutMillis,
                                                        AS400 as400, KeyedDataQueue outputQueue) {
        try {
            // Convertir la clave a EBCDIC (12 bytes exactos)
            byte[] keyData = stringToEbcdic(expectedKey, KEY_LENGTH, as400);

            long startTime = System.currentTimeMillis();
//            System.out.println("Buscando en JTELLERO con clave: " + expectedKey);

            while ((System.currentTimeMillis() - startTime) < timeoutMillis) {
                try {
                    // ¡MÉTODO CORRECTO PARA COLAS KEYED!
                    KeyedDataQueueEntry response = outputQueue.read(keyData);

                    if (response != null) {
                        String responseData = ebcdicToString(response.getData(), as400);
//                        System.out.println("✓ Respuesta recibida de JTELLERO (KEYED)");
                        return responseData;
                    }

                    // Pequeña pausa antes de reintentar
                    Thread.sleep(100);

                } catch (Exception e) {
                    // Solo mostrar errores que no sean de "no entry found"
                    if (!e.getMessage().contains("No entries found") &&
                            !e.getMessage().contains("no data queue entries")) {
                        System.err.println("Error en lectura KEYED: " + e.getMessage());
                    }
                    Thread.sleep(100);
                }
            }

//            System.out.println("Timeout: No se recibió respuesta después de " + timeoutMillis + "ms");
            return null;

        } catch (Exception e) {
            System.err.println("Error grave en recepción KEYED: " + e.getMessage());
            return null;
        }
    }

    private static String ebcdicToString(byte[] data, AS400 as400) {
        try {
            // Usamos la conexión PCML por defecto, pero si es null, usamos la de Colas de Datos
//            AS400 systemToUse = (this.as400 != null) ? this.as400 : this.as400Dtaq;

            AS400Text converter = new AS400Text(data.length, as400);
            return (String) converter.toObject(data);
        } catch (Exception e) {
            return new String(data);
        }
    }

    /**
     * callDetalleAplicado();
     * <p>
     * Método de uso interno INPREMA para notificar que
     * <p>
     * se ha aplicado detalle de planilla en el As400.
     *
     * @param detalleAplicadoApago Objeto
     * @param directorioGeneral    folder
     * @param nameLog              file
     */
    public static void callDetalleAplicado(DetallePagoPendiente detalleAplicadoApago,
                                           String directorioGeneral,
                                           String nameLog,
                                           AS400 as400) {
        String msgId, msgText, contenido;


        try {
            // 1. Cargar el documento PCML
            ProgramCallDocument pcml = cargarPcmlDetalleAplicado(detalleAplicadoApago, as400);

            // 2. Llamar a procedimiento "DETALLEAPLICADO"
            boolean success = pcml.callProgram("DETALLEAPLICADO");
            if (success) {

                int status = pcml.getIntValue("DETALLEAPLICADO.GSTATUS");

                if (status == 200) {
                    contenido = "    Pago actualizado a estado -D- en archivo DPPLCOB.";
                    registrarLogProceso_1(contenido,
                            directorioGeneral,
                            nameLog);
                } else {
                    contenido = "    Pago rechazado de actualización a estado -D- en archivo DPPLCOB.";
                    registrarLogProceso_1(contenido,
                            directorioGeneral,
                            nameLog);
                }

            } else {
                AS400Message[] msgs = pcml.getMessageList("DETALLEAPLICADO");
                for (AS400Message msg : msgs) {
                    msgId = msg.getID();
                    msgText = msg.getText();
                    contenido = "    AS400Message[]: \n"
                            + "    msgId  : " + msgId + "\n"
                            + "    msgText: " + msgText + "\n";
                    Funcion.createArchivoPrintWrite(directorioGeneral,
                            nameLog,
                            contenido);
                }
            }

        } catch (PcmlException e) {
//            e.printStackTrace();
            contenido = "   " + e;
            Funcion.createArchivoPrintWrite(directorioGeneral,
                    nameLog,
                    contenido);
        }
//        System.out.println("....callDetalleAplicado() success ");

    }

    private static ProgramCallDocument cargarPcmlDetalleAplicado(DetallePagoPendiente detalleAplicadoApago,
                                                                 AS400 as400) throws PcmlException {

        ProgramCallDocument pcml = new ProgramCallDocument(as400, "SRVP002");

        // 1. Establecer procedimiento en el As400 a llamar y su  parámetro de entrada
        pcml.setPath("DETALLEAPLICADO", PATH);

        pcml.setStringValue("DETALLEAPLICADO.JDETALLEPAGO.GCODCOLEGIO", detalleAplicadoApago.getCodigoColegio());
        pcml.setIntValue("DETALLEAPLICADO.JDETALLEPAGO.GTIPOPLANILLA", detalleAplicadoApago.getTipoPlanilla());
        pcml.setIntValue("DETALLEAPLICADO.JDETALLEPAGO.GANO", detalleAplicadoApago.getAnoPlanilla());
        pcml.setIntValue("DETALLEAPLICADO.JDETALLEPAGO.GMES", detalleAplicadoApago.getMesPlanilla());
        pcml.setStringValue("DETALLEAPLICADO.JDETALLEPAGO.GUSUARIO", "runDetalle");

//        System.out.println(".....cargarPcmlDETALLEAPLICADO(); exitosa ");

        return pcml;
    }
}


