package hn.sinap.conciliacion.service;

import hn.sinap.conciliacion.model.AuthPayload;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.FileReader;
import java.nio.file.Files;
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
        // 1. Verificar que el archivo existe
        if (!Files.exists(Paths.get(PRIVATE_KEY_PATH))) {
            throw new RuntimeException("Archivo de clave privada no encontrado en: " + PRIVATE_KEY_PATH);
        }

        // 2. Leer y parsear la clave privada
        try (FileReader keyReader = new FileReader(PRIVATE_KEY_PATH);
             PEMParser pemParser = new PEMParser(keyReader)) {

            Object pemObject = pemParser.readObject();

            if (pemObject == null) {
                throw new RuntimeException("El archivo PEM está vacío o no es válido");
            }

            PrivateKey privateKey;
            if (pemObject instanceof PrivateKeyInfo) {
                // Para claves en formato PKCS#8
                privateKey = new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) pemObject);
            } else if (pemObject instanceof org.bouncycastle.openssl.PEMKeyPair) {
                // Para claves en formato PKCS#1
                privateKey = new JcaPEMKeyConverter().getKeyPair(
                        (org.bouncycastle.openssl.PEMKeyPair) pemObject).getPrivate();
            } else {
                throw new RuntimeException("Formato de clave privada no reconocido");
            }

            // 3. Crear el token JWT
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", authPayload.getId());
            claims.put("nombre", authPayload.getNombre());

            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hora
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();
        }
    }
}



/*
package hn.sinap.conciliacion.service;

import hn.sinap.conciliacion.model.AuthPayload;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtService {
    // Ruta absoluta al archivo de llave privada
    private static final String PRIVATE_KEY_PATH = "/Users/Figueroa/documents/KeysIP/jwtRSA256.key";

    public String generateToken(AuthPayload authPayload) throws Exception {
        // Verificar que el archivo existe
        File keyFile = new File(PRIVATE_KEY_PATH);
        if (!keyFile.exists()) {
            throw new RuntimeException("Archivo de llave privada no encontrado en: " + PRIVATE_KEY_PATH);
        }

        // Leer el archivo PEM
        PemReader pemReader = new PemReader(new FileReader(keyFile));
        byte[] pemContent = pemReader.readPemObject().getContent();
        pemReader.close();

        // Crear la clave privada
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemContent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        // Crear los claims del JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", authPayload.getId());
        claims.put("nombre", authPayload.getNombre());

        // Generar y retornar el token
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hora de validez
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}


*/

/*
package hn.sinap.conciliacion.service;

import hn.sinap.conciliacion.model.AuthPayload;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.FileReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JwtService {
    private static final String PRIVATE_KEY_PATH = "/Users/Figueroa/documents/KeysIP/jwtRSA256.key";

    public String generateToken(AuthPayload authPayload) throws Exception {
        PemReader pemReader = new PemReader(new FileReader(
                getClass().getClassLoader().getResource(PRIVATE_KEY_PATH).getFile()));

//        PemReader pemReader = new PemReader(new FileReader(
//                Objects.requireNonNull(getClass().getClassLoader().getResource(PRIVATE_KEY_PATH)).getFile()));

        byte[] pemContent = pemReader.readPemObject().getContent();
        pemReader.close();

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemContent);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", authPayload.getId());
        claims.put("nombre", authPayload.getNombre());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hora
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}*/
