package uz.suxa.converter_onlyoffice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Convert {
    private String url;
    private String outputtype;
    private String filetype;
    private String key;
    private String title;
    private String token;
    private Boolean async;
}
