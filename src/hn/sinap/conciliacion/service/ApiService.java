package hn.sinap.conciliacion.service;

import hn.sinap.conciliacion.model.ConciliacionResponse;
import hn.sinap.conciliacion.model.PostTransaccion;
import hn.sinap.conciliacion.model.TransaccionesResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class ApiService {
    private static final String API_URL = "https://conciliacionrv.sinap.hn:8000/conciliacion/";
    private static final String API_URL_T = "https://conciliacionrv.sinap.hn:8000/transacciones/";


    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
                .setCookieSpec("ignoreCookies") // Ignora las cookies
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }


    public ConciliacionResponse getConciliacionData(String jwtToken, String fecha) throws Exception {

        try (CloseableHttpClient client = createHttpClient()) {
//            System.out.println("GET " + API_URL + fecha);


            HttpGet request = new HttpGet(API_URL + fecha);
            request.addHeader("Authorization", "Bearer " + jwtToken);
            request.addHeader("Accept", "application/json");
            HttpResponse response = client.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity());

//            System.out.println("JSON : \n" + jsonResponse);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonResponse, ConciliacionResponse.class);
        }

    }

    public TransaccionesResponse getConciliacionDataT(String jwtToken, String fecha) throws Exception {

        try (CloseableHttpClient client = createHttpClient()) {
//            System.out.println("GET " + API_URL_T + fecha);

            HttpGet request = new HttpGet(API_URL_T + fecha);
            request.addHeader("Authorization", "Bearer " + jwtToken);
            request.addHeader("Accept", "application/json");
            HttpResponse response = client.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity());

//            System.out.println("JSON : \n" + jsonResponse);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonResponse, TransaccionesResponse.class);
        }


    }

    public ConciliacionResponse postDataTransacciones(String jwtToken, String id, String jsonRequest) throws Exception {

        try (CloseableHttpClient client = createHttpClient()) {
            System.out.println("POST " + API_URL + id);
            HttpPost request = new HttpPost(API_URL + id);

            // Configurar headers
            request.addHeader("Authorization", "Bearer " + jwtToken);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");

            // Establecer el cuerpo de la petición
            request.setEntity(new StringEntity(jsonRequest));

            // Ejecutar la petición
            HttpResponse response = client.execute(request);
           // Procesar la respuesta
            int statusCode = response.getStatusLine().getStatusCode();
            String jsonResponse = EntityUtils.toString(response.getEntity());

            System.out.println("JSON : \n" + jsonResponse);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonResponse, ConciliacionResponse.class);
        }


    }
}