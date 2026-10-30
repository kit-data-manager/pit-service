package edu.kit.datamanager.pit.typeregistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributeInfoTest {
    @Test
    void valueToJsonNode_givenInteger_returnsNumberNode() {
        var numberNode = AttributeInfo.valueToJsonNode("42");
        assertTrue(numberNode.isNumber());
        assertEquals(42, numberNode.numberValue());
    }

    @Test
    void valueToJsonNode_givenString_returnsTextNode() {
        var textNode = AttributeInfo.valueToJsonNode("Hello, World!");
        assertTrue(textNode.isTextual());
        assertEquals("Hello, World!", textNode.textValue());
    }

    @Test
    void valueToJsonNode_givenBoolean_returnsBooleanNode() {
        var booleanNode = AttributeInfo.valueToJsonNode("true");
        assertTrue(booleanNode.isBoolean());
        assertTrue(booleanNode.booleanValue());

        booleanNode = AttributeInfo.valueToJsonNode("false");
        assertTrue(booleanNode.isBoolean());
        assertFalse(booleanNode.booleanValue());
    }

    @Test
    void valueToJsonNode_givenNull_returnsNullNode() {
        var nullNode = AttributeInfo.valueToJsonNode("null");
        assertTrue(nullNode.isNull());
    }

    @Test
    void valueToJsonNode_givenJsonString_returnsJsonObject() {
        var jsonString = "{\"key\": \"value\"}";
        var jsonNode = AttributeInfo.valueToJsonNode(jsonString);
        assertTrue(jsonNode.isObject());
        assertEquals("value", jsonNode.get("key").textValue());
    }

    @Test
    void valueToJsonNode_givenDecimalNumber_returnsNumberNode() {
        var numberNode = AttributeInfo.valueToJsonNode("42.5");
        assertTrue(numberNode.isNumber());
        assertEquals(42.5, numberNode.doubleValue());
    }

    @Test
    void valueToJsonNode_givenJsonArray_returnsArrayNode() {
        var jsonArray = "[1, \"test\", true]";
        var arrayNode = AttributeInfo.valueToJsonNode(jsonArray);
        assertTrue(arrayNode.isArray());
        assertEquals(3, arrayNode.size());
        assertTrue(arrayNode.get(0).isNumber());
        assertTrue(arrayNode.get(1).isTextual());
        assertTrue(arrayNode.get(2).isBoolean());
    }

    @Test
    void valueToJsonNode_givenEmptyString_returnsTextNode() {
        var node = AttributeInfo.valueToJsonNode("");
        assertTrue(node.isTextual());
        assertEquals("", node.textValue());
    }

    @Test
    void valueToJsonNode_givenWhitespaceOnly_returnsTextNode() {
        var node = AttributeInfo.valueToJsonNode("   ");
        assertTrue(node.isTextual());
        assertEquals("   ", node.textValue());
    }

    @Test
    void valueToJsonNode_givenLargeNumber_returnsNumberNode() {
        var numberNode = AttributeInfo.valueToJsonNode("9223372036854775807"); // Long.MAX_VALUE
        assertTrue(numberNode.isNumber());
        assertEquals(9223372036854775807L, numberNode.longValue());
    }
}