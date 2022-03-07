package com.hugs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Site {
    private String url;
    // spelling mistake here is an external one, can't fix
    private boolean atack;
}
