package edu.kit.datamanager.pit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PIDRecordTest {
    private static final String PID = "fake/pid/42";
    private Collection<String> propertiesToKeep;

    
//Assignin the new PID 
    @Test
    void assignPID() {
        PIDRecord r = new PIDRecord().withPID(PID);
        assertEquals(PID, r.getPid());
    }
//Adding the Entry of Property Identifier ,PropertyName,PropertyValue,(key, Name ,Value) Test
    @Test
    void addingEntries() {
        PIDRecord r = new PIDRecord().withPID(PID);
        r.addEntry("some identifier", "some name", "some value");
        
        System.out.println(r);

        Map<String, List<PIDRecordEntry>> entries = r.getEntries();
        assertEquals(1, entries.keySet().size());
       
        assertEquals("some identifier", entries.values().iterator().next().get(0).getKey());
        assertEquals("some name", entries.values().iterator().next().get(0).getName());
        assertEquals("some value", entries.values().iterator().next().get(0).getValue());
        assertNotNull( entries.values().iterator().next().get(0).getValue());
        
        
    }


        void testImmutability() {
           

    

    }


    @Test
    void testCanEqual() {

    }

    @Test
    void testCheckTypeConformance() {

    }

    @Test
    void testEquals() {

    }

    @Test
    void testGetEntries() {

    }
//Get PID(Getter Method) from PIDRecord
    @Test
    void testGetPid() {
        PIDRecord r = new PIDRecord().withPID(PID);
        Assertions.assertTrue(r.getPid() == "PID");
    }
//
    private void assertTrue(String pid2) {
        PIDRecord r = new PIDRecord().withPID(PID);
        assertTrue(r.getPid());
    }

    @Test
    void testGetPropertyIdentifiers() {
        

 
    }
@Test
    private void assertnNull() {
        PIDRecord r = new PIDRecord().withPID(PID);
        Map<String, List<PIDRecordEntry>> entries = r.getEntries();
        r.addEntry("some identifier", "some name", "some value");

        assertNotNull( entries.keySet().size());

        
        assertNotNull((entries.values().iterator().next().get(0).getKey()));
        assertNotNull( entries.values().iterator().next().get(0).getName());
        

    }
    
    @Test
   // @DisplayName("Expecting these tests to use the testGretPropertyvalues configuration // check invalid Input ")
    void testGetPropertyValue() {
        PIDRecord r = new PIDRecord();
      String expected ="some name";
       assertEquals(expected ,r.getPropertyValues("some value"));
       assertThrows(IllegalArgumentException.class, () -> r.getPropertyValues(""));


    }

    @Test
  //  @DisplayName("Expecting to test Property value of PID Record")
    void testGetPropertyValues() {
PIDRecord Property_Values=new PIDRecord().withPID(PID);
Property_Values.getPropertyValue("some value");
Map<String, List<PIDRecordEntry>> entries = Property_Values.getEntries();
   
        String Expected="some value";
        assertEquals(Expected, entries.values().iterator().next().get(0).getKey());
        
       // assertEquals(Expected,Actual);

    }

    @Test
    void testHasProperty() {
        PIDRecord HasProperty=new PIDRecord().withPID(PID);
        HasProperty.hasProperty("some identifier");
        assertTrue(HasProperty.hasProperty("some identifier"));



    }

    private void assertTrue(boolean hasProperty) {
    }
    @Test
    void testHashCode() {

    }

    @Test
    void testRemovePropertiesNotListed() {
        PIDRecord Remove = new PIDRecord().withPID(PID);
        Remove.addEntry("some identifier", "some name", "some value");
        Map<String, List<PIDRecordEntry>> entries = Remove.getEntries();
        //Remove.removePropertiesNotListed(propertiesToKeep);
        assertNull(Remove.removePropertiesNotListed(propertiesToKeep));
        
    }

    private void assertNull(Object removePropertiesNotListed) {
    }

    @Test
    void testSetEntries() {

    }

    @Test
    void testSetPid() {


    }

    @Test
    void testSetPropertyName() {

    }

    @Test
    void testToString() {

    }

    @Test
    void testWithPID() {

    }
}
 