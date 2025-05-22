package hn.sinap.conciliacion.controller;

import hn.sinap.conciliacion.model.AuthPayload;
import hn.sinap.conciliacion.model.ConciliacionResponse;
import hn.sinap.conciliacion.service.ApiService;
import hn.sinap.conciliacion.service.JwtService;

public class ConciliacionController {
    private final JwtService jwtService;
    private final ApiService apiService;

    public ConciliacionController() {
        this.jwtService = new JwtService();
        this.apiService = new ApiService();
    }

    public ConciliacionResponse obtenerDatosConciliacion(int idInstitucion, String nombreInstitucion) {
        try {
            System.out.println("id: " + idInstitucion +" nombre: " + nombreInstitucion);

            AuthPayload payload = new AuthPayload(idInstitucion, nombreInstitucion);
            String token = jwtService.generateToken(payload);
            System.out.println("token: " + token);
            return apiService.getConciliacionData(token);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}