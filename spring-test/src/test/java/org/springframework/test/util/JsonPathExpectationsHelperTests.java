/*
 * Copyright 2004-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link JsonPathExpectationsHelper}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class JsonPathExpectationsHelperTests {

    private static final String CONTENT = "{" + //
            "'str':         'foo',           " + //
            "'num':         5,               " + //
            "'bool':        true,            " + //
            "'arr':         [42],            " + //
            "'colorMap':    {'red': 'rojo'}, " + //
            "'whitespace':  '    ',          " + //
            "'emptyString': '',              " + //
            "'emptyArray':  [],              " + //
            "'emptyMap':    {}               " + //
            "}";

    private static final String SIMPSONS = "{ 'familyMembers': [ " + //
            "{'name': 'Homer' }, " + //
            "{'name': 'Marge' }, " + //
            "{'name': 'Bart'  }, " + //
            "{'name': 'Lisa'  }, " + //
            "{'name': 'Maggie'}  " + //
            " ] }";


    @Test
    public void exists() throws Exception {
        new JsonPathExpectationsHelper("$.str").exists(CONTENT);
    }

    @Test
    public void existsForAnEmptyArray() throws Exception {
        new JsonPathExpectationsHelper("$.emptyArray").exists(CONTENT);
    }

    @Test
    public void existsForAnEmptyMap() throws Exception {
        new JsonPathExpectationsHelper("$.emptyMap").exists(CONTENT);
    }

    @Test
    public void existsForIndefinatePathWithResults() throws Exception {
        new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").exists(SIMPSONS);
    }

    @Test
    public void existsForIndefinatePathWithEmptyResults() throws Exception {
        String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).exists(SIMPSONS))
                .withMessageContaining("No value at JSON path \"" + expression + "\"");
    }

    @Test
    public void doesNotExist() throws Exception {
        new JsonPathExpectationsHelper("$.bogus").doesNotExist(CONTENT);
    }

    @Test
    public void doesNotExistForAnEmptyArray() throws Exception {
        String expression = "$.emptyArray";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT))
                .withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: []");
    }

    @Test
    public void doesNotExistForAnEmptyMap() throws Exception {
        String expression = "$.emptyMap";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT))
                .withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: {}");
    }

    @Test
    public void doesNotExistForIndefinatePathWithResults() throws Exception {
        String expression = "$.familyMembers[?(@.name == 'Bart')]";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).doesNotExist(SIMPSONS))
                .withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
    }

    @Test
    public void doesNotExistForIndefinatePathWithEmptyResults() throws Exception {
        new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").doesNotExist(SIMPSONS);
    }

    @Test
    public void assertValueIsEmptyForAnEmptyString() throws Exception {
        new JsonPathExpectationsHelper("$.emptyString").assertValueIsEmpty(CONTENT);
    }

    @Test
    public void assertValueIsEmptyForAnEmptyArray() throws Exception {
        new JsonPathExpectationsHelper("$.emptyArray").assertValueIsEmpty(CONTENT);
    }

    @Test
    public void assertValueIsEmptyForAnEmptyMap() throws Exception {
        new JsonPathExpectationsHelper("$.emptyMap").assertValueIsEmpty(CONTENT);
    }

    @Test
    public void assertValueIsEmptyForIndefinatePathWithEmptyResults() throws Exception {
        new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").assertValueIsEmpty(SIMPSONS);
    }

    @Test
    public void assertValueIsEmptyForIndefinatePathWithResults() throws Exception {
        String expression = "$.familyMembers[?(@.name == 'Bart')]";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsEmpty(SIMPSONS))
                .withMessageContaining("Expected an empty value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
    }

    @Test
    public void assertValueIsEmptyForWhitespace() throws Exception {
        String expression = "$.whitespace";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsEmpty(CONTENT))
                .withMessageContaining("Expected an empty value at JSON path \"" + expression + "\" but found: '    '");
    }

    @Test
    public void assertValueIsNotEmptyForString() throws Exception {
        new JsonPathExpectationsHelper("$.str").assertValueIsNotEmpty(CONTENT);
    }

    @Test
    public void assertValueIsNotEmptyForNumber() throws Exception {
        new JsonPathExpectationsHelper("$.num").assertValueIsNotEmpty(CONTENT);
    }

    @Test
    public void assertValueIsNotEmptyForBoolean() throws Exception {
        new JsonPathExpectationsHelper("$.bool").assertValueIsNotEmpty(CONTENT);
    }

    @Test
    public void assertValueIsNotEmptyForArray() throws Exception {
        new JsonPathExpectationsHelper("$.arr").assertValueIsNotEmpty(CONTENT);
    }

    @Test
    public void assertValueIsNotEmptyForMap() throws Exception {
        new JsonPathExpectationsHelper("$.colorMap").assertValueIsNotEmpty(CONTENT);
    }

    @Test
    public void assertValueIsNotEmptyForIndefinatePathWithResults() throws Exception {
        new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").assertValueIsNotEmpty(SIMPSONS);
    }

    @Test
    public void assertValueIsNotEmptyForIndefinatePathWithEmptyResults() throws Exception {
        String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(SIMPSONS))
                .withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
    }

    @Test
    public void assertValueIsNotEmptyForAnEmptyString() throws Exception {
        String expression = "$.emptyString";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
                .withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: ''");
    }

    @Test
    public void assertValueIsNotEmptyForAnEmptyArray() throws Exception {
        String expression = "$.emptyArray";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
                .withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
    }

    @Test
    public void assertValueIsNotEmptyForAnEmptyMap() throws Exception {
        String expression = "$.emptyMap";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
                .withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: {}");
    }

    @Test
    public void hasJsonPath() {
        new JsonPathExpectationsHelper("$.abc").hasJsonPath("{\"abc\": \"123\"}");
    }

    @Test
    public void hasJsonPathWithNull() {
        new JsonPathExpectationsHelper("$.abc").hasJsonPath("{\"abc\": null}");
    }

    @Test
    public void hasJsonPathForIndefinatePathWithResults() {
        new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").hasJsonPath(SIMPSONS);
    }

    @Test
    public void hasJsonPathForIndefinatePathWithEmptyResults() {
        String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).hasJsonPath(SIMPSONS))
                .withMessageContaining("No values for JSON path \"" + expression + "\"");
    }

    @Test // SPR-16339
    public void doesNotHaveJsonPath() {
        new JsonPathExpectationsHelper("$.abc").doesNotHaveJsonPath("{}");
    }

    @Test // SPR-16339
    public void doesNotHaveJsonPathWithNull() {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper("$.abc").doesNotHaveJsonPath("{\"abc\": null}"));
    }

    @Test
    public void doesNotHaveJsonPathForIndefinatePathWithEmptyResults() {
        new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").doesNotHaveJsonPath(SIMPSONS);
    }

    @Test
    public void doesNotHaveEmptyPathForIndefinatePathWithResults() {
        String expression = "$.familyMembers[?(@.name == 'Bart')]";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).doesNotHaveJsonPath(SIMPSONS))
                .withMessageContaining("Expected no values at JSON path \"" + expression + "\" " + "but found: [{\"name\":\"Bart\"}]");
    }

    @Test
    public void assertValue() throws Exception {
        new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, 5);
    }

    @Test // SPR-14498
    public void assertValueWithNumberConversion() throws Exception {
        new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, 5.0);
    }

    @Test // SPR-14498
    public void assertValueWithNumberConversionAndMatcher() throws Exception {
        new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, is(5.0), Double.class);
    }

    @Test
    public void assertValueIsString() throws Exception {
        new JsonPathExpectationsHelper("$.str").assertValueIsString(CONTENT);
    }

    @Test
    public void assertValueIsStringForAnEmptyString() throws Exception {
        new JsonPathExpectationsHelper("$.emptyString").assertValueIsString(CONTENT);
    }

    @Test
    public void assertValueIsStringForNonString() throws Exception {
        String expression = "$.bool";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsString(CONTENT))
                .withMessageContaining("Expected a string at JSON path \"" + expression + "\" but found: true");
    }

    @Test
    public void assertValueIsNumber() throws Exception {
        new JsonPathExpectationsHelper("$.num").assertValueIsNumber(CONTENT);
    }

    @Test
    public void assertValueIsNumberForNonNumber() throws Exception {
        String expression = "$.bool";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsNumber(CONTENT))
                .withMessageContaining("Expected a number at JSON path \"" + expression + "\" but found: true");
    }

    @Test
    public void assertValueIsBoolean() throws Exception {
        new JsonPathExpectationsHelper("$.bool").assertValueIsBoolean(CONTENT);
    }

    @Test
    public void assertValueIsBooleanForNonBoolean() throws Exception {
        String expression = "$.num";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsBoolean(CONTENT))
                .withMessageContaining("Expected a boolean at JSON path \"" + expression + "\" but found: 5");
    }

    @Test
    public void assertValueIsArray() throws Exception {
        new JsonPathExpectationsHelper("$.arr").assertValueIsArray(CONTENT);
    }

    @Test
    public void assertValueIsArrayForAnEmptyArray() throws Exception {
        new JsonPathExpectationsHelper("$.emptyArray").assertValueIsArray(CONTENT);
    }

    @Test
    public void assertValueIsArrayForNonArray() throws Exception {
        String expression = "$.str";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsArray(CONTENT))
                .withMessageContaining("Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
    }

    @Test
    public void assertValueIsMap() throws Exception {
        new JsonPathExpectationsHelper("$.colorMap").assertValueIsMap(CONTENT);
    }

    @Test
    public void assertValueIsMapForAnEmptyMap() throws Exception {
        new JsonPathExpectationsHelper("$.emptyMap").assertValueIsMap(CONTENT);
    }

    @Test
    public void assertValueIsMapForNonMap() throws Exception {
        String expression = "$.str";
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                new JsonPathExpectationsHelper(expression).assertValueIsMap(CONTENT))
                .withMessageContaining("Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
    }

}
