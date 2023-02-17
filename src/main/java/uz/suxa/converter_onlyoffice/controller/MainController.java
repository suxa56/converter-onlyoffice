package uz.suxa.converter_onlyoffice.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import uz.suxa.converter_onlyoffice.service.ConverterService;
import uz.suxa.converter_onlyoffice.service.ExtService;

import java.util.HashMap;

@Controller
public class MainController {

    private final ConverterService service;
    private final ExtService extService;

    public MainController(ConverterService service, ExtService extService) {
        this.service = service;
        this.extService = extService;
    }

    @GetMapping("/")
    public String main() {
        return "index";
    }

    @PostMapping("/")
    public String convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ext") String ext,
            Model model
            ) {
        service.convertFile(file, ext);
        model.addAttribute("uri", service.getConvertedFileUri());
        return "index";
    }

    @GetMapping(path = "/download")
    public ResponseEntity<Resource> download(@RequestParam("fileName") final String fileName) {
        return service.downloadFile(fileName);
    }

    @PostMapping(path = "/config")
    @ResponseBody
    public HashMap<String, String> configParameters() {
        return extService.configParameters();
    }
}
