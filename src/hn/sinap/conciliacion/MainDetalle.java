package hn.sinap.conciliacion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;

import com.ibm.as400.data.PcmlException;
import com.ibm.as400.data.ProgramCallDocument;
import hn.sinap.conciliacion.model.DetallePagoPendiente;
import hn.sinap.conciliacion.model.PagosPendientes;
import hn.sinap.conciliacion.util.Funcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MainDetalle.jar en el AS400. de INPREMA
 * <p>
 * Esta clase forma parte de proyecto /webapi (Plataforma de Recaudos)
 * <p>
 * Pero va embebida en el IFS del As400 para que sea invocada por los procesos de fin de dia.
 * <p>
 * Y su objetivo es Aplicar el PAGO Y DETALLE VÍA COLAS DE DATOS, de los pagos de
 * <p>
 * Planillas recaudadas por la plataforma.
 * <p>
 * Actualmente, esto lo hace el departamento de Operaciones de forma manual vía VISA ALTERNA. Un endpoint
 * <p>
 * le retorna a INPRENET los pagos recaudados pendientes de aplicar su detalle. Entonces vía visa alterna
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
 * 1. Usaremos los procedimientos del  endpoint: /webapi/get-pagos-pendientes que retorna exactamente los pagos
 * <p>
 * que necesitamos procesar vía colas de DATOS.
 * <p>
 * 2. Una vez recuperada esa lista. Procedemos a actualizar la propiedad codigoCajeroByte en dicha Lista.
 * <p>
 * Esto lo obtendremos de recuperar vía SQL la lista de Recaudadores del DPKEYS que es donde está ese dato.
 * <p>
 * 3. Una vez completada la lista con los datos que necesitamos completos, hacemos un ciclo leyendo cada
 * <p>
 * elemento y procesándolo en el método de colas que incorporaremos y dejando log de arranque del proceso y sus
 * <p>
 * respectivos resultados.
 *
 * @Domingo Arístides Figueroa, escrito el 2 de abril del 2026, solo en la casa de Loarque acompañado de mi pug
 * <p>
 * Polito, quien nunca abandona.
 *
 * <p>
 */
public class MainDetalle {
    //    private static final AS400 AS400 = new AS400("localhost", "*CURRENT", "*CURRENT");
    private static final String PATH = "/QSYS.LIB/ICLIBOBJ.LIB/SRVP002.SRVPGM";
    private static final String DIRECTORIO = "DetallePlanillas";
    private static final String DIR_LOG = "log";
    private static final String NAME_LOG = "proceso_";
    private static String directorioGeneral = "";
    private static String nameLog = "";
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
            com.ibm.as400.access.CommandCall cmd = new com.ibm.as400.access.CommandCall(as400);

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
            fechaByte = getFechaByte(as400);

            // 4. Llamamos a procedimiento para recuperar los pagos facturados pendientes de procesar vía colas de datos
            PagosPendientes pagosPendientes = getPagosPendientes(as400);

            // Validamos que el objeto no sea nulo y que el status sea 200
            if (pagosPendientes != null && pagosPendientes.getStatus() == 200) {
                System.out.println("Se encontraron pagos pendientes. Extrayendo DPKEYS...");
                contenido = "-----------------------------------------\n"
                        + "INICIO DE PROCESO " + LocalDateTime.now() + "\n"
                        + " Fecha Byte  : " + fechaByte + "\n"
                        + " " + pagosPendientes.getNoPagosPendientes() + " Transacciones de Pago Pendientes de procesar.";
                registrarLogProceso_1(contenido,
                        directorioGeneral,
                        nameLog);
                // 5. Llamamos al método SQL para extraer la lista del archivo DPKEYS
                List<BancoCajero> listaBancos = getListaBancosCajeros();
                if (!listaBancos.isEmpty()) {
                    System.out.println("Lista DPKEYS recuperada con éxito. Total bancos: " + listaBancos.size());
                    contenido = " " + listaBancos.size() + "  Códigos Cajero Byte recuperados."
                            + "\n Procesando Pagos para Planillas tipo  = 1";
                    registrarLogProceso_1(contenido,
                            directorioGeneral,
                            nameLog);

                    System.out.println("--- Detalle de Bancos y Cajeros extraídos ---");
                    for (BancoCajero bc : listaBancos) {
                        System.out.println("Banco: " + bc.getCodigoBanco() + " | Cajero Byte: " + bc.getCodigoCajeroByte());
                    }
                    System.out.println("---------------------------------------------");

                    // 6. Ahora procesamos los pagosPendientes tipoPlanilla == 1 para procesar
                    List<DetallePagoPendiente> listaPagos = pagosPendientes.getDetallePagoPendientes();

                    if (listaPagos != null && !listaPagos.isEmpty()) {
                        for (DetallePagoPendiente pago : listaPagos) {

                            // Validamos que el tipo de planilla sea estrictamente 1
                            if (pago.getTipoPlanilla() == 1) {

                                System.out.println("Planilla Tipo 1 encontrada - Colegio: " + pago.getCodigoColegio());

                                // --------------------------------------------------------
                                // AQUÍ ES DONDE LLAMAREMOS AL MÉTODO DE COLAS DE DATOS.
                                // Ejemplo:
                                // boolean exit = enviarPagoPorDtaq(as400, pago, listaBancos);
                                // --------------------------------------------------------

                            } else {
                                // Opcional: Imprimir en consola si se omite por no ser tipo 1
                                System.out.println("Planilla omitida (Tipo " + pago.getTipoPlanilla() + ") - Colegio: " + pago.getCodigoColegio());
                            }


                        }
                    } else {
                        System.out.println("La lista de pagos pendientes viene vacía desde el objeto.");
                    }

                    contenido = "FIN  DE  PROCESO  " + LocalDateTime.now();
                    registrarLogProceso_1(contenido,
                            directorioGeneral,
                            nameLog);

                } else {
                    System.out.println("Lista DPKEYS no tiene elementos. Vuelva a ejecutar y si persiste problema, informar ");
                    contenido = " Lista DPKEYS no tiene elementos. Vuelva a ejecutar y si "
                            + "persiste problema, informar al administrador del sistema. "
                            + "FIN DE PROCESO " + LocalDateTime.now();
                    registrarLogProceso_1(contenido,
                            directorioGeneral,
                            nameLog);
                }

                // --- AQUÍ ABAJO IRÁ TU LÓGICA DE ACTUALIZACIÓN POSTERIOR ---
                // (Donde cruzarás la lista de pagosPendientes con listaBancos)

            } else {
                int status = (pagosPendientes != null) ? pagosPendientes.getStatus() : -1;
                contenido = " No hay transacciones pendientes de procesar."
                        + "FIN  DE  PROCESO  " + LocalDateTime.now() + "\n";
                registrarLogProceso_1(contenido,
                        directorioGeneral,
                        nameLog);
                System.out.println("Fin del proceso: No hay pagos pendientes por procesar o la consulta falló. Status: " + status);
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
                fechaByte = pcml.getStringValue("GETFECHABYTE.GFECHAHOY");
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
            System.out.println("Conectando vía JDBC para extraer DPKEYS...");

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


}


