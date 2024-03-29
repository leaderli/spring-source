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

package org.springframework.beans.factory.serviceloader;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ServiceLoaderTests {

    @Test
    public void testServiceLoaderFactoryBean() {
        assumeTrue(ServiceLoader.load(DocumentBuilderFactory.class).iterator().hasNext());

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        RootBeanDefinition bd = new RootBeanDefinition(ServiceLoaderFactoryBean.class);
        bd.getPropertyValues().add("serviceType", DocumentBuilderFactory.class.getName());
        bf.registerBeanDefinition("service", bd);
        ServiceLoader<?> serviceLoader = (ServiceLoader<?>) bf.getBean("service");
        boolean condition = serviceLoader.iterator().next() instanceof DocumentBuilderFactory;
        assertThat(condition).isTrue();
    }

    @Test
    public void testServiceFactoryBean() {
        assumeTrue(ServiceLoader.load(DocumentBuilderFactory.class).iterator().hasNext());

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        RootBeanDefinition bd = new RootBeanDefinition(ServiceFactoryBean.class);
        bd.getPropertyValues().add("serviceType", DocumentBuilderFactory.class.getName());
        bf.registerBeanDefinition("service", bd);
        boolean condition = bf.getBean("service") instanceof DocumentBuilderFactory;
        assertThat(condition).isTrue();
    }

    @Test
    public void testServiceListFactoryBean() {
        assumeTrue(ServiceLoader.load(DocumentBuilderFactory.class).iterator().hasNext());

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        RootBeanDefinition bd = new RootBeanDefinition(ServiceListFactoryBean.class);
        bd.getPropertyValues().add("serviceType", DocumentBuilderFactory.class.getName());
        bf.registerBeanDefinition("service", bd);
        List<?> serviceList = (List<?>) bf.getBean("service");
        boolean condition = serviceList.get(0) instanceof DocumentBuilderFactory;
        assertThat(condition).isTrue();
    }

}
