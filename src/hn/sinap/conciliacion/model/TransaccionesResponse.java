package hn.sinap.conciliacion.model;

import java.math.BigInteger;
import java.util.List;

public class TransaccionesResponse {
    private String mensaje;
    private String fecha_hora;
    private List<Transaccion> datos;




    public static class Transaccion {
        private String id;
        private int operacion;
        private BigInteger comprobante;
        private String placa;
        private String tuav;
        private String alcaldia;
        private String siglo21;
        private String valor_placa;
        private String reposicion;

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

        public BigInteger getComprobante() {
            return comprobante;
        }

        public void setComprobante(BigInteger comprobante) {
            this.comprobante = comprobante;
        }

        public String getPlaca() {
            return placa;
        }

        public void setPlaca(String placa) {
            this.placa = placa;
        }

        public String getTuav() {
            return tuav;
        }

        public void setTuav(String tuav) {
            this.tuav = tuav;
        }

        public String getAlcaldia() {
            return alcaldia;
        }

        public void setAlcaldia(String alcaldia) {
            this.alcaldia = alcaldia;
        }

        public String getSiglo21() {
            return siglo21;
        }

        public void setSiglo21(String siglo21) {
            this.siglo21 = siglo21;
        }

        public String getValor_placa() {
            return valor_placa;
        }

        public void setValor_placa(String valor_placa) {
            this.valor_placa = valor_placa;
        }

        public String getReposicion() {
            return reposicion;
        }

        public void setReposicion(String reposicion) {
            this.reposicion = reposicion;
        }

    }

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

    public List<Transaccion> getDatos() {
        return datos;
    }

    public void setDatos(List<Transaccion> datos) {
        this.datos = datos;
    }
}
