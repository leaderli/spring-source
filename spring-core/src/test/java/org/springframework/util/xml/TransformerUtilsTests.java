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

package org.springframework.util.xml;

import org.junit.Test;

import javax.xml.transform.*;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link TransformerUtils}.
 *
 * @author Rick Evans
 * @author Arjen Poutsma
 */
public class TransformerUtilsTests {

    @Test
    public void enableIndentingSunnyDay() throws Exception {
        Transformer transformer = new StubTransformer();
        TransformerUtils.enableIndenting(transformer);
        String indent = transformer.getOutputProperty(OutputKeys.INDENT);
        assertThat(indent).isNotNull();
        assertThat(indent).isEqualTo("yes");
        String indentAmount = transformer.getOutputProperty("{http://xml.apache.org/xalan}indent-amount");
        assertThat(indentAmount).isNotNull();
        assertThat(indentAmount).isEqualTo(String.valueOf(TransformerUtils.DEFAULT_INDENT_AMOUNT));
    }

    @Test
    public void enableIndentingSunnyDayWithCustomKosherIndentAmount() throws Exception {
        final String indentAmountProperty = "10";
        Transformer transformer = new StubTransformer();
        TransformerUtils.enableIndenting(transformer, Integer.valueOf(indentAmountProperty));
        String indent = transformer.getOutputProperty(OutputKeys.INDENT);
        assertThat(indent).isNotNull();
        assertThat(indent).isEqualTo("yes");
        String indentAmount = transformer.getOutputProperty("{http://xml.apache.org/xalan}indent-amount");
        assertThat(indentAmount).isNotNull();
        assertThat(indentAmount).isEqualTo(indentAmountProperty);
    }

    @Test
    public void disableIndentingSunnyDay() throws Exception {
        Transformer transformer = new StubTransformer();
        TransformerUtils.disableIndenting(transformer);
        String indent = transformer.getOutputProperty(OutputKeys.INDENT);
        assertThat(indent).isNotNull();
        assertThat(indent).isEqualTo("no");
    }

    @Test
    public void enableIndentingWithNullTransformer() throws Exception {
        assertThatIllegalArgumentException().isThrownBy(() ->
                TransformerUtils.enableIndenting(null));
    }

    @Test
    public void disableIndentingWithNullTransformer() throws Exception {
        assertThatIllegalArgumentException().isThrownBy(() ->
                TransformerUtils.disableIndenting(null));
    }

    @Test
    public void enableIndentingWithNegativeIndentAmount() throws Exception {
        assertThatIllegalArgumentException().isThrownBy(() ->
                TransformerUtils.enableIndenting(new StubTransformer(), -21938));
    }

    @Test
    public void enableIndentingWithZeroIndentAmount() throws Exception {
        TransformerUtils.enableIndenting(new StubTransformer(), 0);
    }

    private static class StubTransformer extends Transformer {

        private Properties outputProperties = new Properties();

        @Override
        public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setParameter(String name, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getParameter(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearParameters() {
            throw new UnsupportedOperationException();
        }

        @Override
        public URIResolver getURIResolver() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setURIResolver(URIResolver resolver) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Properties getOutputProperties() {
            return this.outputProperties;
        }

        @Override
        public void setOutputProperties(Properties oformat) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOutputProperty(String name, String value) throws IllegalArgumentException {
            this.outputProperties.setProperty(name, value);
        }

        @Override
        public String getOutputProperty(String name) throws IllegalArgumentException {
            return this.outputProperties.getProperty(name);
        }

        @Override
        public ErrorListener getErrorListener() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
            throw new UnsupportedOperationException();
        }
    }

}
