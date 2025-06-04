package hn.sinap.conciliacion.controller;

import hn.sinap.conciliacion.model.AuthPayload;
import hn.sinap.conciliacion.model.ConciliacionResponse;
import hn.sinap.conciliacion.service.ApiService;
import hn.sinap.conciliacion.service.JwtService;

public class PostTransaccionesController {
    private final JwtService jwtService;
    private final ApiService apiService;

    public PostTransaccionesController() {
        this.jwtService = new JwtService();
        this.apiService = new ApiService();
    }

    public ConciliacionResponse postDatosTransacciones(int idInstitucion, String nombreInstitucion, String id, String jsonRequest) {
        try {

            AuthPayload payload = new AuthPayload(idInstitucion, nombreInstitucion);
            String token = jwtService.generateToken(payload);
//            System.out.println("token: " + token);
            return apiService.postDataTransacciones(token, id, jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}