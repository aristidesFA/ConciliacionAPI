package hn.sinap.conciliacion.service;

import hn.sinap.conciliacion.model.ConciliacionResponse;
import hn.sinap.conciliacion.model.TransaccionesResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ApiService {
    private static final String API_URL = "https://conciliacionrv.sinap.hn:8000/conciliacion/";
    private static final String API_URL_T = "https://conciliacionrv.sinap.hn:8000/transacciones/";
    private static final String API_URL_PA = "https://conciliacionrv.sinap.hn:8000/pa01/";


//    private CloseableHttpClient createHttpClient() {
//        RequestConfig config = RequestConfig.custom()
//                .setCookieSpec("ignoreCookies") // Ignora las cookies
//                .build();
//
//        return HttpClients.custom()
//                .setDefaultRequestConfig(config)
//                .build();
//    }

    private CloseableHttpClient createHttpClient() {
        try {
            // 1. Crear un contexto SSL que confíe en cualquier certificado
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(new TrustAllStrategy())
                    .build();

            // 2. Configurar la fábrica de sockets para que no verifique el Hostname
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE);

            // 3. Mantener tu configuración original de cookies Y AGREGAR TIMEOUTS
            RequestConfig config = RequestConfig.custom()
                    .setCookieSpec("ignoreCookies")
                    .setConnectTimeout(60000) // 20 segundos máximo para conectar
                    .setSocketTimeout(60000)  // 20 segundos máximo para recibir la data
                    .build();

            // 4. Construir el cliente con el contexto SSL relajado
            return HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setDefaultRequestConfig(config)
                    .build();

        } catch (Exception e) {
            System.out.println("Error configurando contexto SSL: " + e.getMessage());
            // Fallback al cliente por defecto si algo falla en la configuración SSL
            return HttpClients.custom()
                    .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec("ignoreCookies").build())
                    .build();
        }
    }
    public ConciliacionResponse getConciliacionData(String jwtToken, String fecha) throws Exception {

        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet request = new HttpGet(API_URL + fecha);
            request.addHeader("Authorization", "Bearer " + jwtToken);
            request.addHeader("Accept", "application/json");
            HttpResponse response = client.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            // 1. Primero, convertimos el JSON en tu objeto Java
            ObjectMapper mapper = new ObjectMapper();
            ConciliacionResponse conciliacionResponse = mapper.readValue(jsonResponse, ConciliacionResponse.class);

            // 2. Contamos las transacciones de forma segura (validando que no vengan nulas)
            int cantidadTransacciones = 0;
            if (conciliacionResponse != null &&
                    conciliacionResponse.getDatos() != null &&
                    conciliacionResponse.getDatos().getTransacciones() != null) {

                cantidadTransacciones = conciliacionResponse.getDatos().getTransacciones().size();
            }

            // 3. Imprimimos el mensaje limpio en lugar del JSON crudo
            System.out.println("Recuperadas " + cantidadTransacciones + " transacciones satisfactoriamente.\nVerificar sus transacciones en IPC001 e IPC002.\nFin de proceso.");

            // 4. Retornamos el objeto para que Main.java siga trabajando normal
            return conciliacionResponse;
        }

    }

//    public ConciliacionResponse getConciliacionData(String jwtToken, String fecha) throws Exception {
//
//        try (CloseableHttpClient client = createHttpClient()) {
////            System.out.println("GET " + API_URL + fecha);
//
//
//            HttpGet request = new HttpGet(API_URL + fecha);
//            request.addHeader("Authorization", "Bearer " + jwtToken);
//            request.addHeader("Accept", "application/json");
//            HttpResponse response = client.execute(request);
//            String jsonResponse = EntityUtils.toString(response.getEntity());
//
//            System.out.println("JSON : \n" + jsonResponse);
//
//            ObjectMapper mapper = new ObjectMapper();
//            return mapper.readValue(jsonResponse, ConciliacionResponse.class);
//        }
//
//    }

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
//            System.out.println("POST " + API_URL + id);
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

//            System.out.println("JSON : \n" + jsonResponse);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonResponse, ConciliacionResponse.class);
        }


    }


    public File downloadZipFile(String jwtToken, String pa01, String outputFilePath) throws Exception {
        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet request = new HttpGet(API_URL_PA + pa01);
//            System.out.println("\nrequest: " + request);
            request.addHeader("Authorization", "Bearer " + jwtToken);
            request.addHeader("Accept", "application/zip");

            HttpResponse response = client.execute(request);

            // Verificar código de estado
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new RuntimeException("Error al descargar archivo. Código: " + statusCode);
            }

            File outputFile = new File(outputFilePath);


//            // Crear directorio de salida si no existe
//            outputFile.getParentFile().mkdirs();

            // Guardar el archivo ZIP
            try (InputStream inputStream = response.getEntity().getContent();
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                long totalSize = response.getEntity().getContentLength();
                long downloaded = 0;

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    System.out.printf("Descargado: %d/%d bytes (%.2f%%)%n",
                            downloaded, totalSize, (downloaded * 100.0 / totalSize));
                }
            }

            return outputFile;
        }
    }
}