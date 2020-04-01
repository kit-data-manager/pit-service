/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 *
 * @author Torridity
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Contributor {

    private String identifiedUsing;
    private String name;
    private String details;
}
