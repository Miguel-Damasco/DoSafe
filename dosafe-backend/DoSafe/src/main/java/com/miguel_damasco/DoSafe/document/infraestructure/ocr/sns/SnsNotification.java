package com.miguel_damasco.DoSafe.document.infraestructure.ocr.sns;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SnsNotification {

    @JsonProperty("Type")
    private String type;

    @JsonProperty("Message")
    private String message;
}
