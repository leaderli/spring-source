/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.env;

import org.junit.Test;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Profiles}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 5.1
 */
public class ProfilesTests {

    private static Predicate<String> activeProfiles(String... profiles) {
        return new MockActiveProfiles(profiles);
    }

    @Test
    public void ofWhenNullThrowsException() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                Profiles.of((String[]) null))
                .withMessageContaining("Must specify at least one profile");
    }

    @Test
    public void ofWhenEmptyThrowsException() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                Profiles.of())
                .withMessageContaining("Must specify at least one profile");
    }

    @Test
    public void ofNullElement() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                Profiles.of((String) null))
                .withMessageContaining("must contain text");
    }

    @Test
    public void ofEmptyElement() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                Profiles.of("  "))
                .withMessageContaining("must contain text");
    }

    @Test
    public void ofSingleElement() {
        Profiles profiles = Profiles.of("spring");
        assertThat(profiles.matches(activeProfiles("spring"))).isTrue();
        assertThat(profiles.matches(activeProfiles("framework"))).isFalse();
    }

    @Test
    public void ofSingleInvertedElement() {
        Profiles profiles = Profiles.of("!spring");
        assertThat(profiles.matches(activeProfiles("spring"))).isFalse();
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    }

    @Test
    public void ofMultipleElements() {
        Profiles profiles = Profiles.of("spring", "framework");
        assertThat(profiles.matches(activeProfiles("spring"))).isTrue();
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("java"))).isFalse();
    }

    @Test
    public void ofMultipleElementsWithInverted() {
        Profiles profiles = Profiles.of("!spring", "framework");
        assertThat(profiles.matches(activeProfiles("spring"))).isFalse();
        assertThat(profiles.matches(activeProfiles("spring", "framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("java"))).isTrue();
    }

    @Test
    public void ofMultipleElementsAllInverted() {
        Profiles profiles = Profiles.of("!spring", "!framework");
        assertThat(profiles.matches(activeProfiles("spring"))).isTrue();
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("java"))).isTrue();
        assertThat(profiles.matches(activeProfiles("spring", "framework"))).isFalse();
        assertThat(profiles.matches(activeProfiles("spring", "framework", "java"))).isFalse();
    }

    @Test
    public void ofSingleExpression() {
        Profiles profiles = Profiles.of("(spring)");
        assertThat(profiles.matches(activeProfiles("spring"))).isTrue();
        assertThat(profiles.matches(activeProfiles("framework"))).isFalse();
    }

    @Test
    public void ofSingleExpressionInverted() {
        Profiles profiles = Profiles.of("!(spring)");
        assertThat(profiles.matches(activeProfiles("spring"))).isFalse();
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    }

    @Test
    public void ofSingleInvertedExpression() {
        Profiles profiles = Profiles.of("(!spring)");
        assertThat(profiles.matches(activeProfiles("spring"))).isFalse();
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    }

    @Test
    public void ofOrExpression() {
        Profiles profiles = Profiles.of("(spring | framework)");
        assertOrExpression(profiles);
    }

    @Test
    public void ofOrExpressionWithoutSpaces() {
        Profiles profiles = Profiles.of("(spring|framework)");
        assertOrExpression(profiles);
    }

    private void assertOrExpression(Profiles profiles) {
        assertThat(profiles.matches(activeProfiles("spring"))).isTrue();
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("spring", "framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("java"))).isFalse();
    }

    @Test
    public void ofAndExpression() {
        Profiles profiles = Profiles.of("(spring & framework)");
        assertAndExpression(profiles);
    }

    @Test
    public void ofAndExpressionWithoutSpaces() {
        Profiles profiles = Profiles.of("spring&framework)");
        assertAndExpression(profiles);
    }

    @Test
    public void ofAndExpressionWithoutParentheses() {
        Profiles profiles = Profiles.of("spring & framework");
        assertAndExpression(profiles);
    }

    private void assertAndExpression(Profiles profiles) {
        assertThat(profiles.matches(activeProfiles("spring"))).isFalse();
        assertThat(profiles.matches(activeProfiles("framework"))).isFalse();
        assertThat(profiles.matches(activeProfiles("spring", "framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("java"))).isFalse();
    }

    @Test
    public void ofNotAndExpression() {
        Profiles profiles = Profiles.of("!(spring & framework)");
        assertOfNotAndExpression(profiles);
    }

    @Test
    public void ofNotAndExpressionWithoutSpaces() {
        Profiles profiles = Profiles.of("!(spring&framework)");
        assertOfNotAndExpression(profiles);
    }

    private void assertOfNotAndExpression(Profiles profiles) {
        assertThat(profiles.matches(activeProfiles("spring"))).isTrue();
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("spring", "framework"))).isFalse();
        assertThat(profiles.matches(activeProfiles("java"))).isTrue();
    }

    @Test
    public void ofAndExpressionWithInvertedSingleElement() {
        Profiles profiles = Profiles.of("!spring & framework");
        assertOfAndExpressionWithInvertedSingleElement(profiles);
    }

    @Test
    public void ofAndExpressionWithInBracketsInvertedSingleElement() {
        Profiles profiles = Profiles.of("(!spring) & framework");
        assertOfAndExpressionWithInvertedSingleElement(profiles);
    }

    @Test
    public void ofAndExpressionWithInvertedSingleElementInBrackets() {
        Profiles profiles = Profiles.of("! (spring) & framework");
        assertOfAndExpressionWithInvertedSingleElement(profiles);
    }

    @Test
    public void ofAndExpressionWithInvertedSingleElementInBracketsWithoutSpaces() {
        Profiles profiles = Profiles.of("!(spring)&framework");
        assertOfAndExpressionWithInvertedSingleElement(profiles);
    }

    @Test
    public void ofAndExpressionWithInvertedSingleElementWithoutSpaces() {
        Profiles profiles = Profiles.of("!spring&framework");
        assertOfAndExpressionWithInvertedSingleElement(profiles);
    }

    private void assertOfAndExpressionWithInvertedSingleElement(Profiles profiles) {
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("java"))).isFalse();
        assertThat(profiles.matches(activeProfiles("spring", "framework"))).isFalse();
        assertThat(profiles.matches(activeProfiles("spring"))).isFalse();
    }

    @Test
    public void ofOrExpressionWithInvertedSingleElementWithoutSpaces() {
        Profiles profiles = Profiles.of("!spring|framework");
        assertOfOrExpressionWithInvertedSingleElement(profiles);
    }

    private void assertOfOrExpressionWithInvertedSingleElement(Profiles profiles) {
        assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("java"))).isTrue();
        assertThat(profiles.matches(activeProfiles("spring", "framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("spring"))).isFalse();
    }

    @Test
    public void ofNotOrExpression() {
        Profiles profiles = Profiles.of("!(spring | framework)");
        assertOfNotOrExpression(profiles);
    }

    @Test
    public void ofNotOrExpressionWithoutSpaces() {
        Profiles profiles = Profiles.of("!(spring|framework)");
        assertOfNotOrExpression(profiles);
    }

    private void assertOfNotOrExpression(Profiles profiles) {
        assertThat(profiles.matches(activeProfiles("spring"))).isFalse();
        assertThat(profiles.matches(activeProfiles("framework"))).isFalse();
        assertThat(profiles.matches(activeProfiles("spring", "framework"))).isFalse();
        assertThat(profiles.matches(activeProfiles("java"))).isTrue();
    }

    @Test
    public void ofComplexExpression() {
        Profiles profiles = Profiles.of("(spring & framework) | (spring & java)");
        assertComplexExpression(profiles);
    }

    @Test
    public void ofComplexExpressionWithoutSpaces() {
        Profiles profiles = Profiles.of("(spring&framework)|(spring&java)");
        assertComplexExpression(profiles);
    }

    private void assertComplexExpression(Profiles profiles) {
        assertThat(profiles.matches(activeProfiles("spring"))).isFalse();
        assertThat(profiles.matches(activeProfiles("spring", "framework"))).isTrue();
        assertThat(profiles.matches(activeProfiles("spring", "java"))).isTrue();
        assertThat(profiles.matches(activeProfiles("java", "framework"))).isFalse();
    }

    @Test
    public void malformedExpressions() {
        assertMalformed(() -> Profiles.of("("));
        assertMalformed(() -> Profiles.of(")"));
        assertMalformed(() -> Profiles.of("a & b | c"));
    }

    @Test
    public void sensibleToString() {
        assertThat(Profiles.of("spring & framework", "java | kotlin").toString()).isEqualTo("spring & framework or java | kotlin");
    }

    private void assertMalformed(Supplier<Profiles> supplier) {
        assertThatIllegalArgumentException().isThrownBy(
                supplier::get)
                .withMessageContaining("Malformed");
    }

    private static class MockActiveProfiles implements Predicate<String> {

        private final List<String> activeProfiles;

        MockActiveProfiles(String[] activeProfiles) {
            this.activeProfiles = Arrays.asList(activeProfiles);
        }

        @Override
        public boolean test(String profile) {
            // The following if-condition (which basically mimics
            // AbstractEnvironment#validateProfile(String)) is necessary in order
            // to ensure that the Profiles implementation returned by Profiles.of()
            // never passes an invalid (parsed) profile name to the active profiles
            // predicate supplied to Profiles#matches(Predicate<String>).
            if (!StringUtils.hasText(profile) || profile.charAt(0) == '!') {
                throw new IllegalArgumentException("Invalid profile [" + profile + "]");
            }
            return this.activeProfiles.contains(profile);
        }

    }

}
