package uz.suxa.converter_onlyoffice.service;

import org.json.JSONObject;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import static java.nio.file.Files.createDirectory;

@Service
public class ConverterService {

    @Value("${files.docservice.url.site}")
    private String docsSiteUrl;

    @Value("${files.docservice.url.converter}")
    private String docsConverterUrl;

    @Value("${files.docservice.secret}")
    private String docsSecret;

    public void convertFile(MultipartFile file) {

    }

    public void convertFileasd(MultipartFile file) {
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

    public String getStorageLocation() {
        String storageAddress = null;
        String storageFolder = "documents";
        try {
             storageAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        String serverPath = System.getProperty("user.dir");  // get the path to the server
        String directory;  // create the storage directory
        if (Paths.get(storageAddress).isAbsolute()) {
            directory = storageAddress + File.separator;
        } else {
            directory = serverPath
                    + File.separator + storageFolder
                    + File.separator + storageAddress
                    + File.separator;
        }
        if (!Files.exists(Paths.get(directory))) {
            createDirectory(Paths.get(directory));
        }
        return directory;
    }

    public Path generateFilepath(final String directory, final String fullFileName) {
        String fileName = getFileNameWithoutExtension(fullFileName);  // get file name without extension
        String fileExtension = getExtension(fullFileName);  // get file extension
        Path path = Paths.get(directory + fullFileName);  // get the path to the files with the specified name

        for (int i = 1; Files.exists(path); i++) {  // run through all the files with the specified name
            // get a name of each file without extension and add an index to it
            fileName = getFileNameWithoutExtension(fullFileName) + "(" + i + ")";

            // create a new path for this file with the correct name and extension
            path = Paths.get(directory + fileName + fileExtension);
        }

        path = Paths.get(directory + fileName + fileExtension);
        return path;
    }

    private void createDirectory(final Path path) {
        if (Files.exists(path)) {
            return;
        }
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
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

    private String getFileNameWithoutExtension(String filename) {
        String fileNameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        return fileNameWithoutExt;
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
