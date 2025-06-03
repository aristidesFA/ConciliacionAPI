package hn.sinap.conciliacion.controller;

import hn.sinap.conciliacion.model.AuthPayload;
import hn.sinap.conciliacion.model.ConciliacionResponse;
import hn.sinap.conciliacion.model.TransaccionesResponse;
import hn.sinap.conciliacion.service.ApiService;
import hn.sinap.conciliacion.service.JwtService;

public class TransaccionesController {
    private final JwtService jwtService;
    private final ApiService apiService;

    public TransaccionesController() {
        this.jwtService = new JwtService();
        this.apiService = new ApiService();
    }

    public TransaccionesResponse obtenerDatosConciliacion(int idInstitucion, String nombreInstitucion, String fecha) {
        try {
            AuthPayload payload = new AuthPayload(idInstitucion, nombreInstitucion);
            String token = jwtService.generateToken(payload);
            return apiService.getConciliacionDataT(token, fecha);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}