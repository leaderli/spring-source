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

package org.springframework.core.io;

import org.junit.Test;
import org.springframework.core.env.StandardEnvironment;

import java.beans.PropertyEditor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link ResourceEditor} class.
 *
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Dave Syer
 */
public class ResourceEditorTests {

    @Test
    public void sunnyDay() {
        PropertyEditor editor = new ResourceEditor();
        editor.setAsText("classpath:org/springframework/core/io/ResourceEditorTests.class");
        Resource resource = (Resource) editor.getValue();
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
    }

    @Test
    public void ctorWithNullCtorArgs() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new ResourceEditor(null, null));
    }

    @Test
    public void setAndGetAsTextWithNull() {
        PropertyEditor editor = new ResourceEditor();
        editor.setAsText(null);
        assertThat(editor.getAsText()).isEqualTo("");
    }

    @Test
    public void setAndGetAsTextWithWhitespaceResource() {
        PropertyEditor editor = new ResourceEditor();
        editor.setAsText("  ");
        assertThat(editor.getAsText()).isEqualTo("");
    }

    @Test
    public void testSystemPropertyReplacement() {
        PropertyEditor editor = new ResourceEditor();
        System.setProperty("test.prop", "foo");
        try {
            editor.setAsText("${test.prop}");
            Resource resolved = (Resource) editor.getValue();
            assertThat(resolved.getFilename()).isEqualTo("foo");
        } finally {
            System.getProperties().remove("test.prop");
        }
    }

    @Test
    public void testSystemPropertyReplacementWithUnresolvablePlaceholder() {
        PropertyEditor editor = new ResourceEditor();
        System.setProperty("test.prop", "foo");
        try {
            editor.setAsText("${test.prop}-${bar}");
            Resource resolved = (Resource) editor.getValue();
            assertThat(resolved.getFilename()).isEqualTo("foo-${bar}");
        } finally {
            System.getProperties().remove("test.prop");
        }
    }

    @Test
    public void testStrictSystemPropertyReplacementWithUnresolvablePlaceholder() {
        PropertyEditor editor = new ResourceEditor(new DefaultResourceLoader(), new StandardEnvironment(), false);
        System.setProperty("test.prop", "foo");
        try {
            assertThatIllegalArgumentException().isThrownBy(() -> {
                editor.setAsText("${test.prop}-${bar}");
                editor.getValue();
            });
        } finally {
            System.getProperties().remove("test.prop");
        }
    }

}
