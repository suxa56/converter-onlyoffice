package uz.suxa.converter_onlyoffice.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String main(Model model) {
        model.addAttribute("downloadLink", "");
        return "index";
    }

    @PostMapping("/")
    public String convert(
            @RequestParam("conv_input") final MultipartFile file,
            @RequestParam("conv_output") final String outputType,
            Model model
    ) {
        String downloadLink = service.convertFile(file, outputType);
        model.addAttribute("downloadLink", downloadLink);
        return "index";
    }

    @GetMapping(path = "/download")
    public ResponseEntity<Resource> download(@RequestParam("fileName") final String fileName) {
        return service.downloadFile(fileName);
    }
}
