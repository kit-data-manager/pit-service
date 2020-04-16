/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

/**
 *
 * @author Torridity
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProvenanceInformation {

    private Set<Contributor> contributors = new HashSet<>();
    private Date creationDate;
    private Date lastModificationDate;

    public void addContributor(String identifiedBy, String name, String details) {
        Contributor c = new Contributor();
        c.setIdentifiedUsing(identifiedBy);
        c.setName(name);
        c.setDetails(details);
        contributors.add(c);
    }

}
