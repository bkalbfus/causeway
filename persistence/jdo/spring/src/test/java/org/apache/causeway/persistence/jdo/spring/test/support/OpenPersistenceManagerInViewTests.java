/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.causeway.persistence.jdo.spring.test.support;

import java.io.IOException;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.PassThroughFilterChain;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.StaticWebApplicationContext;

import org.apache.causeway.persistence.jdo.spring.support.OpenPersistenceManagerInViewFilter;
import org.apache.causeway.persistence.jdo.spring.support.OpenPersistenceManagerInViewInterceptor;

class OpenPersistenceManagerInViewTests {

	@Test
	void testOpenPersistenceManagerInViewInterceptor() throws Exception {
		PersistenceManagerFactory pmf = mock(PersistenceManagerFactory.class);
		PersistenceManager pm = mock(PersistenceManager.class);

		OpenPersistenceManagerInViewInterceptor interceptor = new OpenPersistenceManagerInViewInterceptor();
		interceptor.setPersistenceManagerFactory(pmf);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);

		given(pmf.getPersistenceManager()).willReturn(pm);
		interceptor.preHandle(new ServletWebRequest(request));
		assertTrue(TransactionSynchronizationManager.hasResource(pmf));

		// check that further invocations simply participate
		interceptor.preHandle(new ServletWebRequest(request));

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		assertTrue(TransactionSynchronizationManager.hasResource(pmf));

		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
	}

	@Test
	void testOpenPersistenceManagerInViewFilter() throws Exception {
		final PersistenceManagerFactory pmf = mock(PersistenceManagerFactory.class);
		PersistenceManager pm = mock(PersistenceManager.class);

		given(pmf.getPersistenceManager()).willReturn(pm);
		final PersistenceManagerFactory pmf2 = mock(PersistenceManagerFactory.class);
		PersistenceManager pm2 = mock(PersistenceManager.class);

		given(pmf2.getPersistenceManager()).willReturn(pm2);

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("persistenceManagerFactory", pmf);
		wac.getDefaultListableBeanFactory().registerSingleton("myPersistenceManagerFactory", pmf2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("persistenceManagerFactoryBeanName", "myPersistenceManagerFactory");

		final OpenPersistenceManagerInViewFilter filter = new OpenPersistenceManagerInViewFilter();
		filter.init(filterConfig);
		final OpenPersistenceManagerInViewFilter filter2 = new OpenPersistenceManagerInViewFilter();
		filter2.init(filterConfig2);

		final FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(pmf));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IOException, ServletException {
				assertTrue(TransactionSynchronizationManager.hasResource(pmf2));
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
		assertFalse(TransactionSynchronizationManager.hasResource(pmf2));
		filter2.doFilter(request, response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
		assertFalse(TransactionSynchronizationManager.hasResource(pmf2));
		assertNotNull(request.getAttribute("invoked"));

		verify(pm).close();
		verify(pm2).close();

		wac.close();
	}

}
