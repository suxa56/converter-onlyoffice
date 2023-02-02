package uz.suxa.converter_onlyoffice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uz.suxa.converter_onlyoffice.service.ConverterService;

@Controller
public class MainController {

    private final ConverterService service;

    public MainController(ConverterService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String main() {
        return "index";
    }

    @PostMapping("/")
    public String convert(
            @RequestParam("conv_input") MultipartFile file
    ) {
        service.sendFile(file);
        return "index";
    }
}
