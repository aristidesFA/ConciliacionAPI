package hn.sinap.conciliacion.service;

import hn.sinap.conciliacion.model.ConciliacionResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiService {
    private static final String API_URL = "https://conciliacionrv.sinap.hn:8000/conciliacion/";


//    Conexión por IP que genera error.
//    private static final String API_URL = "https://172.30.27.32:8000/conciliacion/";


    public ConciliacionResponse getConciliacionData(String jwtToken) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(API_URL);

        request.addHeader("Authorization", "Bearer " + jwtToken);
        request.addHeader("Accept", "application/json");

        HttpResponse response = client.execute(request);
        String jsonResponse = EntityUtils.toString(response.getEntity());

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonResponse, ConciliacionResponse.class);
    }
}