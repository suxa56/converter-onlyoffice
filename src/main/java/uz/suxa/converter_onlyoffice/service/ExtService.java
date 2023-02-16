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

        configuration.put("DocInputExtList", String.join(",", getDocInputExt()));
        configuration.put("SpreadsheetInputExtList", String.join(",", getSpreadsheetInputExt()));
        configuration.put("PresentationInputExtList", String.join(",", getPresentationInputExt()));
        configuration.put("DocOutputExtList", String.join(",", getDocOutputExt()));
        configuration.put("SpreadsheetOutputExtList", String.join(",", getSpreadsheetOutputExt()));
        configuration.put("PresentationOutputExtList", String.join(",", getPresentationOutputExt()));

        return configuration;
    }

    private List<String> getDocInputExt() {
        return Arrays.asList(docsInput.split("\\|"));
    }

    private List<String> getSpreadsheetInputExt() {
        return Arrays.asList(spreadsheetInput.split("\\|"));
    }

    private List<String> getPresentationInputExt() {
        return Arrays.asList(presentationInput.split("\\|"));
    }

    private List<String> getDocOutputExt() {
        return Arrays.asList(docsOutput.split("\\|"));
    }

    private List<String> getSpreadsheetOutputExt() {
        return Arrays.asList(spreadsheetOutput.split("\\|"));
    }

    private List<String> getPresentationOutputExt() {
        return Arrays.asList(presentationOutput.split("\\|"));
    }
}
