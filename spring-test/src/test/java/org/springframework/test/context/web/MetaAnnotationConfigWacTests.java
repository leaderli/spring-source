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

package org.springframework.test.context.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies meta-annotation support for {@link WebAppConfiguration}
 * and {@link ContextConfiguration}.
 *
 * @author Sam Brannen
 * @see WebTestConfiguration
 * @since 4.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebTestConfiguration
public class MetaAnnotationConfigWacTests {

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    protected MockServletContext mockServletContext;

    @Autowired
    protected String foo;


    @Test
    public void fooEnigmaAutowired() {
        assertThat(foo).isEqualTo("enigma");
    }

    @Test
    public void basicWacFeatures() throws Exception {
        assertThat(wac.getServletContext()).as("ServletContext should be set in the WAC.").isNotNull();

        assertThat(mockServletContext).as("ServletContext should have been autowired from the WAC.").isNotNull();

        Object rootWac = mockServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        assertThat(rootWac).as("Root WAC must be stored in the ServletContext as: "
                + WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE).isNotNull();
        assertThat(rootWac).as("test WAC and Root WAC in ServletContext must be the same object.").isSameAs(wac);
        assertThat(wac.getServletContext()).as("ServletContext instances must be the same object.").isSameAs(mockServletContext);

        assertThat(mockServletContext.getRealPath("index.jsp")).as("Getting real path for ServletContext resource.").isEqualTo(new File("src/main/webapp/index.jsp").getCanonicalPath());
    }

}
