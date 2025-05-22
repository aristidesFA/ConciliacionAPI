package hn.sinap.conciliacion;

import hn.sinap.conciliacion.controller.ConciliacionController;
import hn.sinap.conciliacion.model.ConciliacionResponse;

public class Main {
    public static void main(String[] args) {
        System.out.println("Ingresamos al MAIN:");

        try {
            ConciliacionController controller = new ConciliacionController();
            // Usar los valores reales de tu institución
            ConciliacionResponse response = controller.obtenerDatosConciliacion(9, "BANCO CUSCATLAN");

            if (response != null && response.getMensaje() != null) {
                System.out.println("Respuesta del servidor:");
                System.out.println("Mensaje: " + response.getMensaje());
                System.out.println("Fecha: " + response.getFecha_hora());

                if (response.getDatos() != null) {

                    System.out.println("datos.id: " + response.getDatos().getId());
                    System.out.println("datos.banco: " + response.getDatos().getBanco());
                    System.out.println("datos.fecha: " + response.getDatos().getFecha());
                    System.out.println("datos.estado: " + response.getDatos().getEstado());

                    for (ConciliacionResponse.Transaccion trx : response.getDatos().getTransacciones()) {
                        //imprimimos lista de transacciones objeto pivote
                        System.out.println("transacciones.datos.id: " + trx.getId());
                        System.out.println("transacciones.datos.operacion: " + trx.getOperacion());
                        System.out.println("transacciones.datos.comprobante: " + trx.getComprobante());
                        System.out.println("transacciones.datos.placa: " + trx.getPlaca());
                        System.out.println("transacciones.datos.alcaldia: " + trx.getAlcaldia());
                        System.out.println("transacciones.datos.siglo21: " + trx.getSiglo21());
                        System.out.println("transacciones.datos.valor_placa: " + trx.getValor_placa());
                        System.out.println("transacciones.datos.estado: " + trx.getEstado());

                    }
                }

                // Puedes agregar más lógica para mostrar los datos
            } else {
                System.out.println("La respuesta del servidor está vacía o es inválida");
            }


        } catch (Exception e) {
            System.err.println("Error al procesar la respuesta:");
            e.printStackTrace();
        }


    }
}