package hn.sinap.conciliacion.service;

import hn.sinap.conciliacion.model.AuthPayload;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.FileReader;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtService {
    private static final String PRIVATE_KEY_PATH = "/IPKeys/jwtRSA256.key";
//    Este es PATH para probar local en mi MackBook pro
//    private static final String PRIVATE_KEY_PATH = "/Users/Figueroa/documents/IPKeys/jwtRSA256.key";


    public String generateToken(AuthPayload authPayload) throws Exception {
        // 1. Verificación robusta del archivo
//        System.out.println("// 1. Verificación robusta del archivo");

        if (!Paths.get(PRIVATE_KEY_PATH).toFile().exists()) {
            throw new RuntimeException("Archivo de clave privada no encontrado en: " + PRIVATE_KEY_PATH);
        }

        // 2. Lectura flexible de la clave (soporta PKCS#1 y PKCS#8)
//        System.out.println("// 2. Lectura flexible de la clave (soporta PKCS#1 y PKCS#8)");

        try (FileReader keyReader = new FileReader(PRIVATE_KEY_PATH);
             PEMParser pemParser = new PEMParser(keyReader)) {

            Object pemObject = pemParser.readObject();
            PrivateKey privateKey;

            if (pemObject instanceof PEMKeyPair) {
                // Para claves en formato PKCS#1 (RSA PRIVATE KEY)
//                System.out.println("// Para claves en formato PKCS#1 (RSA PRIVATE KEY)");
                PEMKeyPair pemKeyPair = (PEMKeyPair) pemObject;
                privateKey = new JcaPEMKeyConverter().getKeyPair(pemKeyPair).getPrivate();
            } else if (pemObject instanceof PrivateKeyInfo) {
                // Para claves en formato PKCS#8 (PRIVATE KEY)
//                System.out.println("// Para claves en formato PKCS#8 (PRIVATE KEY)");
                privateKey = new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) pemObject);
            } else {
                throw new RuntimeException("Formato de clave no reconocido. Debe ser PKCS#1 o PKCS#8");
            }

            // 3. Configuración de claims según especificaciones del servidor
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", authPayload.getId());         // "id": 99
            claims.put("nombre", authPayload.getNombre()); // "nombre": "INSTITUCIÓN X"

            // 4. Generación del token con cabeceras explícitas
            return Jwts.builder()
                    .setHeaderParam("typ", "JWT")
                    .setClaims(claims)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hora
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();
        }
    }
}
