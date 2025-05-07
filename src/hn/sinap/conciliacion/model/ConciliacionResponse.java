package hn.sinap.conciliacion.model;

import java.math.BigDecimal;
import java.util.List;

public class ConciliacionResponse {
    private String mensaje;
    private String fecha_hora;
    private Datos datos;

    public static class Datos {
        private String id;
        private int banco;
        private String fecha;
        private String estado;
        private List<Transaccion> transacciones;

        // Getters y Setters

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getBanco() {
            return banco;
        }

        public void setBanco(int banco) {
            this.banco = banco;
        }

        public String getFecha() {
            return fecha;
        }

        public void setFecha(String fecha) {
            this.fecha = fecha;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public List<Transaccion> getTransacciones() {
            return transacciones;
        }

        public void setTransacciones(List<Transaccion> transacciones) {
            this.transacciones = transacciones;
        }
    }

    public static class Transaccion {
        private String id;
        private int operacion;
        private int comprobante;
        private String placa;
        private BigDecimal tuav;
        private BigDecimal alcaldia;
        private BigDecimal siglo21;
        private BigDecimal valor_placa;
        private String estado;

        // Getters y Setters

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getOperacion() {
            return operacion;
        }

        public void setOperacion(int operacion) {
            this.operacion = operacion;
        }

        public int getComprobante() {
            return comprobante;
        }

        public void setComprobante(int comprobante) {
            this.comprobante = comprobante;
        }

        public String getPlaca() {
            return placa;
        }

        public void setPlaca(String placa) {
            this.placa = placa;
        }

        public BigDecimal getTuav() {
            return tuav;
        }

        public void setTuav(BigDecimal tuav) {
            this.tuav = tuav;
        }

        public BigDecimal getAlcaldia() {
            return alcaldia;
        }

        public void setAlcaldia(BigDecimal alcaldia) {
            this.alcaldia = alcaldia;
        }

        public BigDecimal getSiglo21() {
            return siglo21;
        }

        public void setSiglo21(BigDecimal siglo21) {
            this.siglo21 = siglo21;
        }

        public BigDecimal getValor_placa() {
            return valor_placa;
        }

        public void setValor_placa(BigDecimal valor_placa) {
            this.valor_placa = valor_placa;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }
    }

    // Getters y Setters para la clase principal


    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getFecha_hora() {
        return fecha_hora;
    }

    public void setFecha_hora(String fecha_hora) {
        this.fecha_hora = fecha_hora;
    }

    public Datos getDatos() {
        return datos;
    }

    public void setDatos(Datos datos) {
        this.datos = datos;
    }
}