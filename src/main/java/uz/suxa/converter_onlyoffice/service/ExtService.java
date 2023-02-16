package uz.suxa.converter_onlyoffice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExtService {

    @Value("${files.ext.docs-input}")
    private String docsInput;

    @Value("${files.ext.docs-otput}")
    private String docsOutput;

    @Value("${files.ext.spreadsheet-input}")
    private String spreadsheetInput;

    @Value("${files.ext.spreadsheet-output}")
    private String spreadsheetOutput;

    @Value("${files.ext.present-input}")
    private String presentationInput;

    @Value("${files.ext.present-output}")
    private String presentationOutput;

    public HashMap<String, String> configParameters() {
        HashMap<String, String> configuration = new HashMap<>();

        configuration.put("InputExtList", String.join(",", getInputExt()));
        return configuration;
    }

    private List<String> getInputExt() {
        Set<String> input = new HashSet<>();
        for (String ext: Arrays.asList(docsInput.split("\\|"))) {
            input.add(ext);
        }
        for (String ext: Arrays.asList(spreadsheetInput.split("\\|"))) {
            input.add(ext);
        }
        for (String ext: Arrays.asList(presentationInput.split("\\|"))) {
            input.add(ext);
        }
        return input.stream().toList();
    }
}
