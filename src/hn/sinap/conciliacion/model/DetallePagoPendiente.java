package hn.sinap.conciliacion.model;

import java.math.BigDecimal;

public class DetallePagoPendiente {

    private String codigoColegio;
    private int tipoPlanilla; // 1= ordinaria, los otros para manejar 13º y 14º mes
    private int anoPlanilla;
    private int mesPlanilla;
    private int noDocentes;
    private BigDecimal totalSalarios;
    private BigDecimal totalAportaciones;
    private BigDecimal totalCotizaciones;
    private BigDecimal totalRecargos;
    private BigDecimal totalCuotasPrestamos;
    private BigDecimal totalPagado;
    private int anoDePago;
    private int mesDePago;
    private int diaDePago;
    private String horaPago;
    private int fechaPagoByte;
    private String cajero;
    private String agencia;
    private int codigoBanco;
    private String codigoDeConfirmacionDePago;

    public DetallePagoPendiente() {
    }

    public String getCodigoColegio() {
        return codigoColegio;
    }

    public void setCodigoColegio(String codigoColegio) {
        this.codigoColegio = codigoColegio;
    }

    public int getTipoPlanilla() {
        return tipoPlanilla;
    }

    public void setTipoPlanilla(int tipoPlanilla) {
        this.tipoPlanilla = tipoPlanilla;
    }

    public int getAnoPlanilla() {
        return anoPlanilla;
    }

    public void setAnoPlanilla(int anoPlanilla) {
        this.anoPlanilla = anoPlanilla;
    }

    public int getMesPlanilla() {
        return mesPlanilla;
    }

    public void setMesPlanilla(int mesPlanilla) {
        this.mesPlanilla = mesPlanilla;
    }

    public int getNoDocentes() {
        return noDocentes;
    }

    public void setNoDocentes(int noDocentes) {
        this.noDocentes = noDocentes;
    }

    public BigDecimal getTotalSalarios() {
        return totalSalarios;
    }

    public void setTotalSalarios(BigDecimal totalSalarios) {
        this.totalSalarios = totalSalarios;
    }

    public BigDecimal getTotalAportaciones() {
        return totalAportaciones;
    }

    public void setTotalAportaciones(BigDecimal totalAportaciones) {
        this.totalAportaciones = totalAportaciones;
    }

    public BigDecimal getTotalCotizaciones() {
        return totalCotizaciones;
    }

    public void setTotalCotizaciones(BigDecimal totalCotizaciones) {
        this.totalCotizaciones = totalCotizaciones;
    }

    public BigDecimal getTotalRecargos() {
        return totalRecargos;
    }

    public void setTotalRecargos(BigDecimal totalRecargos) {
        this.totalRecargos = totalRecargos;
    }

    public BigDecimal getTotalCuotasPrestamos() {
        return totalCuotasPrestamos;
    }

    public void setTotalCuotasPrestamos(BigDecimal totalCuotasPrestamos) {
        this.totalCuotasPrestamos = totalCuotasPrestamos;
    }

    public BigDecimal getTotalPagado() {
        return totalPagado;
    }

    public void setTotalPagado(BigDecimal totalPagado) {
        this.totalPagado = totalPagado;
    }

    public int getAnoDePago() {
        return anoDePago;
    }

    public void setAnoDePago(int anoDePago) {
        this.anoDePago = anoDePago;
    }

    public int getMesDePago() {
        return mesDePago;
    }

    public void setMesDePago(int mesDePago) {
        this.mesDePago = mesDePago;
    }

    public int getDiaDePago() {
        return diaDePago;
    }

    public void setDiaDePago(int diaDePago) {
        this.diaDePago = diaDePago;
    }

    public String getHoraPago() {
        return horaPago;
    }

    public void setHoraPago(String horaPago) {
        this.horaPago = horaPago;
    }

    public int getFechaPagoByte() {
        return fechaPagoByte;
    }

    public void setFechaPagoByte(int fechaPagoByte) {
        this.fechaPagoByte = fechaPagoByte;
    }

    public String getCajero() {
        return cajero;
    }

    public void setCajero(String cajero) {
        this.cajero = cajero;
    }

    public String getAgencia() {
        return agencia;
    }

    public void setAgencia(String agencia) {
        this.agencia = agencia;
    }

    public int getCodigoBanco() {
        return codigoBanco;
    }

    public void setCodigoBanco(int codigoBanco) {
        this.codigoBanco = codigoBanco;
    }

    public String getCodigoDeConfirmacionDePago() {
        return codigoDeConfirmacionDePago;
    }

    public void setCodigoDeConfirmacionDePago(String codigoDeConfirmacionDePago) {
        this.codigoDeConfirmacionDePago = codigoDeConfirmacionDePago;
    }
}
