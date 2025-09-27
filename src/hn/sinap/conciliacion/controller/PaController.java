package hn.sinap.conciliacion.controller;

import hn.sinap.conciliacion.model.AuthPayload;
import hn.sinap.conciliacion.model.ConciliacionResponse;
import hn.sinap.conciliacion.service.ApiService;
import hn.sinap.conciliacion.service.JwtService;

import java.io.File;

public class PaController {
    private final JwtService jwtService;
    private final ApiService apiService;

    public PaController() {
        this.jwtService = new JwtService();
        this.apiService = new ApiService();
    }

    public File obtenerZip(int idInstitucion, String nombreInstitucion, String pa01, String ruta) {
        try {
//            System.out.println("id: " + idInstitucion +" nombre: " + nombreInstitucion);

            AuthPayload payload = new AuthPayload(idInstitucion, nombreInstitucion);
            String token = jwtService.generateToken(payload);
//            System.out.println("token: " + token);
            return apiService.downloadZipFile(token, pa01, ruta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}