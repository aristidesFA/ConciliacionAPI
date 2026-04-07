package hn.sinap.conciliacion.model;

import java.util.List;

public class PagosPendientes {

    private int status;
    private String message;
    private int noPagosPendientes;

    private List<DetallePagoPendiente> detallePagoPendientes;

    /**
     * Constructor solamente para crear la instancia.
     */
    public PagosPendientes() {
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getNoPagosPendientes() {
        return noPagosPendientes;
    }

    public void setNoPagosPendientes(int noPagosPendientes) {
        this.noPagosPendientes = noPagosPendientes;
    }

    public List<DetallePagoPendiente> getDetallePagoPendientes() {
        return detallePagoPendientes;
    }

    public void setDetallePagoPendientes(List<DetallePagoPendiente> detallePagoPendientes) {
        this.detallePagoPendientes = detallePagoPendientes;
    }
}
