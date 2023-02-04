package uz.suxa.converter_onlyoffice.service;

import org.json.JSONObject;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Service
public class ConverterService {

    @Value("${files.docservice.url.site}")
    private String docsSiteUrl;

    @Value("${files.docservice.url.converter}")
    private String docsConverterUrl;

    @Value("${files.docservice.secret}")
    private String docsSecret;

    public void convertFile(MultipartFile file) {
        String json = buildJson(file.getOriginalFilename());

        try {
            URL url = new URL(docsSiteUrl+docsConverterUrl);
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST"); // PUT is another valid option
            http.setDoOutput(true);
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            int length = out.length;
            String responseMessage;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("Accept", "application/json");
            http.connect();
            try (OutputStream os = http.getOutputStream()) {
                os.write(out);
            }
            InputStream inputStream = http.getInputStream();
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                responseMessage = scanner.useDelimiter("\\A").next();
            }
            System.out.println(responseMessage);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private String buildJson(String filename) {
        String fileType = getExtension(filename);
        String json;
        String token;

        try {
            Signer signer = HMACSigner.newSHA256Signer(docsSecret);
            JWT jwt = new JWT();
            jwt
                    .addClaim("async", "false")
                    .addClaim("filetype", fileType)
                    .addClaim("key", docsSecret)
                    .addClaim("outputtype", "pdf")
                    .addClaim("title", filename)
                    .addClaim("url", docsSiteUrl + filename);
            token = JWT.getEncoder().encode(jwt, signer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        json = new JSONObject()
                .put("token", token)
                .toString();
        return json;
    }

    private String getExtension(String filename) {
        String extension = "";

        int i = filename.lastIndexOf('.');
        if (i > 0) {
            extension = filename.substring(i + 1);
        }
        return extension;
    }
}
