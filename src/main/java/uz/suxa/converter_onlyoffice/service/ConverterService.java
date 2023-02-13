package uz.suxa.converter_onlyoffice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.suxa.converter_onlyoffice.model.Convert;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ConverterService {

    @Value("${files.docservice.url.site}")
    private String docsSiteUrl;

    @Value("${files.docservice.url.converter}")
    private String docsConverterUrl;

    @Value("${files.docservice.secret}")
    private String docsSecret;

    private static final Integer MAX_KEY_LENGTH = 20;
    private static final Integer CONVERTER_TIMEOUT = 120000;
    private static final Long FULL_LOADING_IN_PERCENT = 100L;
    private static final Integer KILOBYTE_SIZE = 1024;
    private String correctedFilename;

    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    public ConverterService(HttpServletRequest request, ObjectMapper objectMapper) {
        this.request = request;
        this.objectMapper = objectMapper;
    }

    public void convertFile(MultipartFile file) {
        saveFileLocal(file);

        String fileName = file.getOriginalFilename();

        // get URL for downloading a file with the specified name
        String fileUri = getDownloadUrl(fileName);

        // get file extension
        String fileExt = getExtension(fileName);

        // get an editor internal extension (".docx", ".xlsx" or ".pptx")
        String internalFileExt = getInternalFileExtension(fileName);

        try {
                String key = generateRevisionId(fileUri);  // generate document key
                String newFileUri = getConvertedUri(fileUri, fileExt, internalFileExt, key, true);

                /* get a file name of an internal file extension with an index if the file
                 with such a name already exists */
                String nameWithInternalExt = getFileNameWithoutExtension(fileName) + internalFileExt;
                String correctedName = correctedFilename;

                URL url = new URL(newFileUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();  // get input stream of the converted file

                if (stream == null) {
                    connection.disconnect();
                    throw new RuntimeException("Input stream is null");
                }

                // create the converted file with input stream
                createFile(Path.of(getFileLocation(correctedName)), stream);

            // create meta information about the converted file with the user ID and name specified
//            return createUserMetadata(uid, fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // if the operation of file converting is unsuccessful, an error occurs
//        return "{ \"error\": \"" + "The file can't be converted.\"}";
    }

    private boolean createFile(Path path, InputStream stream) {
        if (Files.exists(path)) {
            return true;
        }
        try {
            File file = Files.createFile(path).toFile();  // create a new file in the specified path
            try (FileOutputStream out = new FileOutputStream(file)) {
                int read;
                final byte[] bytes = new byte[KILOBYTE_SIZE];
                while ((read = stream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);  // write bytes to the output stream
                }
                out.flush();  // force write data to the output stream that can be cached in the current thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getFileLocation(final String fileName) {
        if (fileName.contains(File.separator)) {
            return getStorageLocation() + fileName;
        }
        return getStorageLocation() + (fileName);
    }

    private String getConvertedUri(final String documentUri, final String fromExtension,
                                   final String toExtension, final String documentRevisionId,
                                   final Boolean isAsync) {
        // check if the fromExtension parameter is defined; if not, get it from the document url
        String fromExt =  fromExtension;

        // check if the file name parameter is defined; if not, get random uuid for this file
        String title = UUID.randomUUID().toString();

        String documentRevId = documentRevisionId == null || documentRevisionId.isEmpty()
                ? documentUri : documentRevisionId;

        documentRevId = generateRevisionId(documentRevId);  // create document token

        // write all the necessary parameters to the body object
        Convert body = new Convert();
        body.setUrl(documentUri);
        body.setOutputtype(toExtension.replace(".", ""));
        body.setFiletype(fromExt.replace(".", ""));
        body.setTitle(title);
        body.setKey(documentRevId);
        if (isAsync) {
            body.setAsync(true);
        }

        String headerToken = "";
            HashMap<String, Object> map = new HashMap<>();
            map.put("url", body.getUrl());
            map.put("outputtype", body.getOutputtype());
            map.put("filetype", body.getFiletype());
            map.put("title", body.getTitle());
            map.put("key", body.getKey());
            if (isAsync) {
                map.put("async", body.getAsync());
            }

            // add token to the body if it is enabled
            String token = createToken(map);
            body.setToken(token);

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("payload", map);  // create payload object
            headerToken = createToken(payloadMap);  // create header token

        String jsonString = postToServer(body, headerToken);

        return getResponseUri(jsonString);
    }

    @SneakyThrows
    private String getResponseUri(final String jsonString) {
        JSONObject jsonObj = convertStringToJSON(jsonString);

        // check if the conversion is completed and save the result to a variable
        Boolean isEndConvert = (Boolean) jsonObj.get("endConvert");

        Long resultPercent;
        String responseUri = null;

        if (isEndConvert) {  // if the conversion is completed
            resultPercent = FULL_LOADING_IN_PERCENT;
            responseUri = (String) jsonObj.get("fileUrl");  // get the file URL
        } else {  // if the conversion isn't completed
            resultPercent = (Long) jsonObj.get("percent");

            // get the percentage value of the conversion process
            resultPercent = resultPercent >= FULL_LOADING_IN_PERCENT ? FULL_LOADING_IN_PERCENT - 1 : resultPercent;
        }

        return resultPercent >= FULL_LOADING_IN_PERCENT ? responseUri : "";
    }

    @SneakyThrows
    private String postToServer(Convert body, String headerToken) {
        String bodyString = objectMapper.writeValueAsString(body);
        URL url;
        java.net.HttpURLConnection connection = null;
        InputStream response;
        String jsonString = null;

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);  // convert body string into bytes
        try {
            // set the request parameters
            url = new URL(docsSiteUrl + docsConverterUrl);
            connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setFixedLengthStreamingMode(bodyByte.length);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(CONVERTER_TIMEOUT);

            connection.connect();

            int statusCode = connection.getResponseCode();
            if (statusCode != HttpStatus.OK.value()) {  // checking status code
                connection.disconnect();
                throw new RuntimeException("Convertation service returned status: " + statusCode);
            }

            try (OutputStream os = connection.getOutputStream()) {
                os.write(bodyByte);  // write bytes to the output stream
                os.flush();  // force write data to the output stream that can be cached in the current thread
            }

            response = connection.getInputStream();  // get the input stream
            jsonString = convertStreamToString(response);  // convert the response stream into a string
        } finally {
            connection.disconnect();
            return jsonString;
        }
    }

    @SneakyThrows
    private String convertStreamToString(InputStream stream) {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);  // create an object to get incoming stream
        StringBuilder stringBuilder = new StringBuilder();  // create a string builder object

        // create an object to read incoming streams
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();  // get incoming streams by lines

        while (line != null) {
            stringBuilder.append(line);  // concatenate strings using the string builder
            line = bufferedReader.readLine();
        }

        String result = stringBuilder.toString();

        return result;
    }

    private String createToken(Map<String, Object> payloadClaims) {
        try {
            // build a HMAC signer using a SHA-256 hash
            Signer signer = HMACSigner.newSHA256Signer(docsSecret);
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet()) {  // run through all the keys from the payload
                jwt.addClaim(key, payloadClaims.get(key));  // and write each claim to the jwt
            }
            return JWT.getEncoder().encode(jwt, signer);  // sign and encode the JWT to a JSON string representation
        } catch (Exception e) {
            return "";
        }
    }

    private String generateRevisionId(String expectedKey) {
        String formatKey = expectedKey.length() > MAX_KEY_LENGTH
                ? Integer.toString(expectedKey.hashCode()) : expectedKey;
        String key = formatKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), MAX_KEY_LENGTH));
    }

    private String getDownloadUrl(String fileName) {
        String serverPath = getServerUrl();
        String storageAddress = getStorageLocation();
        try {
            String userAddress = "&userAddress=" + URLEncoder
                    .encode(storageAddress, java.nio.charset.StandardCharsets.UTF_8.toString());
            String query = "/download" + "?fileName="
                    + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + userAddress;

            return serverPath + query;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private String getServerUrl() {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
    }

    @SneakyThrows
    private JSONObject convertStringToJSON(final String jsonString) {
        Object obj = new Gson().toJson(jsonString);  // parse json string
        JSONObject jsonObj = (JSONObject) obj;  // and turn it into a json object

        return jsonObj;
    }

    private void saveFileLocal(MultipartFile file) {
        Path path = generateFilepath(getStorageLocation(), file.getOriginalFilename());
        try {
            Files.write(path, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private String getStorageLocation() {
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

    private Path generateFilepath(final String directory, final String fullFileName) {
        String fileName = getFileNameWithoutExtension(fullFileName);  // get file name without extension
        String fileExtension = getInternalFileExtension(fullFileName);  // get file extension
        Path path = Paths.get(directory + fullFileName);  // get the path to the files with the specified name

        for (int i = 1; Files.exists(path); i++) {  // run through all the files with the specified name
            // get a name of each file without extension and add an index to it
            fileName = getFileNameWithoutExtension(fullFileName) + "(" + i + ")";

            // create a new path for this file with the correct name and extension
            path = Paths.get(directory + fileName + fileExtension);
        }

        correctedFilename = fileName + fileExtension;

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

    private String getFileNameWithoutExtension(String filename) {
        String fileNameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        return fileNameWithoutExt;
    }

    private String getExtension(String filename) {
        String fileExt = filename.substring(filename.lastIndexOf("."));
        return fileExt.toLowerCase();
    }

    private String getInternalFileExtension(String filename) {
        String extension = "";

        int i = filename.lastIndexOf('.');
        if (i > 0) {
            extension = filename.substring(i + 1);
        }
        return extension;

    }
}
