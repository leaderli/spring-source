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

package org.springframework.core.style;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ObjectUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 */
public class ToStringCreatorTests {

    private SomeObject s1, s2, s3;


    @Before
    public void setUp() throws Exception {
        s1 = new SomeObject() {
            @Override
            public String toString() {
                return "A";
            }
        };
        s2 = new SomeObject() {
            @Override
            public String toString() {
                return "B";
            }
        };
        s3 = new SomeObject() {
            @Override
            public String toString() {
                return "C";
            }
        };
    }

    @Test
    public void defaultStyleMap() {
        final Map<String, String> map = getMap();
        Object stringy = new Object() {
            @Override
            public String toString() {
                return new ToStringCreator(this).append("familyFavoriteSport", map).toString();
            }
        };
        assertThat(stringy.toString()).isEqualTo(("[ToStringCreatorTests.4@" + ObjectUtils.getIdentityHexString(stringy) +
                " familyFavoriteSport = map['Keri' -> 'Softball', 'Scot' -> 'Fishing', 'Keith' -> 'Flag Football']]"));
    }

    private Map<String, String> getMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Keri", "Softball");
        map.put("Scot", "Fishing");
        map.put("Keith", "Flag Football");
        return map;
    }

    @Test
    public void defaultStyleArray() {
        SomeObject[] array = new SomeObject[]{s1, s2, s3};
        String str = new ToStringCreator(array).toString();
        assertThat(str).isEqualTo(("[@" + ObjectUtils.getIdentityHexString(array) +
                " array<ToStringCreatorTests.SomeObject>[A, B, C]]"));
    }

    @Test
    public void primitiveArrays() {
        int[] integers = new int[]{0, 1, 2, 3, 4};
        String str = new ToStringCreator(integers).toString();
        assertThat(str).isEqualTo(("[@" + ObjectUtils.getIdentityHexString(integers) + " array<Integer>[0, 1, 2, 3, 4]]"));
    }

    @Test
    public void appendList() {
        List<SomeObject> list = new ArrayList<>();
        list.add(s1);
        list.add(s2);
        list.add(s3);
        String str = new ToStringCreator(this).append("myLetters", list).toString();
        assertThat(str).isEqualTo(("[ToStringCreatorTests@" + ObjectUtils.getIdentityHexString(this) + " myLetters = list[A, B, C]]"));
    }

    @Test
    public void appendSet() {
        Set<SomeObject> set = new LinkedHashSet<>();
        set.add(s1);
        set.add(s2);
        set.add(s3);
        String str = new ToStringCreator(this).append("myLetters", set).toString();
        assertThat(str).isEqualTo(("[ToStringCreatorTests@" + ObjectUtils.getIdentityHexString(this) + " myLetters = set[A, B, C]]"));
    }

    @Test
    public void appendClass() {
        String str = new ToStringCreator(this).append("myClass", this.getClass()).toString();
        assertThat(str).isEqualTo(("[ToStringCreatorTests@" + ObjectUtils.getIdentityHexString(this) +
                " myClass = ToStringCreatorTests]"));
    }

    @Test
    public void appendMethod() throws Exception {
        String str = new ToStringCreator(this).append("myMethod", this.getClass().getMethod("appendMethod")).toString();
        assertThat(str).isEqualTo(("[ToStringCreatorTests@" + ObjectUtils.getIdentityHexString(this) +
                " myMethod = appendMethod@ToStringCreatorTests]"));
    }


    public static class SomeObject {
    }

}
