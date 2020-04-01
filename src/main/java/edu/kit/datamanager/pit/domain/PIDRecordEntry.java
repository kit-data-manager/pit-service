/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 *
 * @author Torridity
 */
@Data
public class PIDRecordEntry {

    private String key;
    private String name;
    private String value;
    
    @JsonIgnore
    private TypeDefinition resolvedTypeDefinition;
}
