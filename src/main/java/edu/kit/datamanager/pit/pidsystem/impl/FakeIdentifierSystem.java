/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.pidsystem.impl;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Torridity
 */
public class FakeIdentifierSystem implements IIdentifierSystem {

    private Map<String, PIDRecord> records;

    public FakeIdentifierSystem() {
        records = new HashMap<>();
        PIDRecord record = new PIDRecord();
        record.addEntry("21.T11148/076759916209e5d62bd5", "KernelInformationProfile", "21.T11148/b9b76f887845e32d29f7");
        record.addEntry("21.T11148/1c699a5d1b4ad3ba4956", "digitalObjectType", "21.T11148/1c699a5d1b4ad3ba4956");
        record.addEntry("21.T11148/b8457812905b83046284", "digitalObjectLocation", "http://www.heise.de");
        record.addEntry("21.T11148/8074aed799118ac263ad", "digitalObjectPolicy", "21.T11148/8074aed799118ac263ad");
        record.addEntry("21.T11148/92e200311a56800b3e47", "etag", "{\"md5sum\":\"md5:a3cca2b2aa1e3b5b3b5aad99a8529074\"}");
        record.addEntry("21.T11148/397d831aa3a9d18eb52c", "dateModified", "2018-04-01T11:01:52.469Z");
        record.addEntry("21.T11148/29f92bd203dd3eaa5a1f", "dateCreated", "2019-04-01 11:01:52");
        record.addEntry("21.T11148/c692273deb2772da307f", "version", "1");
        record.addEntry("21.T11148/e0efd6b4c8e71c6d077b", "metadataDocument", "{\n"
                + "  \"metadataScheme\": \"http://hdl.handle.net/21.T11148/8bcd7b4a8a9c74402c71\",\n"
                + "  \"@id\":\"http://hdl.handle.net/21.T11148/8bcd7b4a8a9c74402c71\",\n"
                + "  \"@type\":\"http://hdl.handle.net/21.T11148/8bcd7b4a8a9c74402c71\"\n"
                + "}");
        record.addEntry("21.T11148/e0efd6b4c8e71c6d077b", "metadataDocument", "{\n"
                + "  \"metadataScheme\": \"http://hdl.handle.net/21.T11148/8bcd7b4a8a9c74402c71\",\n"
                + "  \"@id\":\"http://hdl.handle.net/21.T11148/8bcd7b4a8a9c74402c71\",\n"
                + "  \"@type\":\"http://hdl.handle.net/21.T11148/8bcd7b4a8a9c74402c71\"\n"
                + "}");
        record.addEntry("21.T11148/dc54ae4b6807f5887fda", "license", "CC-BY");

        records.put("123/456789", record);
    }

    @Override
    public boolean isIdentifierRegistered(String pid) throws IOException {
        return records.containsKey(pid);
    }

    @Override
    public PIDRecord queryAllProperties(String pid) throws IOException {
        return records.get(pid);
    }

    @Override
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException {
        return records.get(pid).getPropertyValue(typeDefinition.getIdentifier());
    }

    @Override
    public String registerPID(Map<String, String> properties) throws IOException {
        PIDRecord r = new PIDRecord();
        Set<Entry<String, String>> entries = properties.entrySet();
        r.setPid("123/456789" + records.size());
        entries.forEach(
            entry -> r.addEntry(entry.getKey(), null, entry.getValue())
        );
        records.put(r.getPid(), r);
        return r.getPid();
    }

    @Override
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException {
        PIDRecord allProps = queryAllProperties(pid);
        // only return properties listed in the type def
        Set<String> typeProps = typeDefinition.getAllProperties();
        PIDRecord result = new PIDRecord();
        for (String propID : allProps.getPropertyIdentifiers()) {
            if (typeProps.contains(propID)) {
                String[] values = allProps.getPropertyValues(propID);
                for (String value : values) {
                    result.addEntry(propID, "", value);
                }
            }
        }
        return result;
    }

    @Override
    public boolean deletePID(String pid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getResolvingUrl(String pid) {
        // TODO Auto-generated method stub
        return "";
    }

}
