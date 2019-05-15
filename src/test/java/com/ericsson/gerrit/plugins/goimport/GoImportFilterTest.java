// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.ericsson.gerrit.plugins.goimport;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.RefPermission;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Provider;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GoImportFilterTest {

  private static final String PROD_URL = "https://gerrit-review.googlesource.com";
  private static final String PROJECT_URL_AUTH =
      "https://gerrit-review.googlesource.com/a/projectName";
  private static final String PROJECT_URL_ANON =
      "https://gerrit-review.googlesource.com/projectName";
  private static final String PAGE_200 =
      "<!DOCTYPE html>\n"
          + "<html>\n"
          + "<head>\n"
          + "  <title>Gerrit-Go-Import</title>\n"
          + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
          + "  <meta name=\"go-import\" content=\"gerrit-review.googlesource.com/projectName git ${content}\"/>\n"
          + "</head>\n"
          + "<body>\n"
          + "<div>\n"
          + "  Gerrit-Go-Import\n"
          + "</div>\n"
          + "</body>\n"
          + "</html>";
  private static final String PAGE_200_AUTH = PAGE_200.replace("${content}", PROJECT_URL_AUTH);
  private static final String PAGE_200_ANON = PAGE_200.replace("${content}", PROJECT_URL_ANON);

  private GoImportFilter unitUnderTest;

  @Mock private Provider<AnonymousUser> mockAnonProvider;
  @Mock private AnonymousUser mockAnon;
  @Mock private PermissionBackend mockPerms;
  @Mock private PermissionBackend.WithUser mockPermsWithUser;
  @Mock private PermissionBackend.ForRef mockPermsForRef;
  @Mock private ProjectCache mockProjectCache;
  @Mock private HttpServletRequest mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @Mock private FilterChain mockChain;
  @Mock private ServletOutputStream mockOutputStream;
  @Mock private ProjectState mockProjectState;

  @Before
  public void setUp() throws Exception {
    unitUnderTest = new GoImportFilter(mockAnonProvider, mockPerms, mockProjectCache, PROD_URL);
    assertThat(unitUnderTest).isNotNull();
    when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);

    when(mockAnonProvider.get()).thenReturn(mockAnon);
    when(mockPerms.user(mockAnon)).thenReturn(mockPermsWithUser);
    when(mockPermsWithUser.ref(any())).thenReturn(mockPermsForRef);
  }

  @Test
  public void testConstructor() throws Exception {
    assertThat(unitUnderTest.webUrl.endsWith("/")).isTrue();
    unitUnderTest =
        new GoImportFilter(
            mockAnonProvider,
            mockPerms,
            mockProjectCache,
            "http://gerrit-review.googlesource.com:8080/");
    assertThat(unitUnderTest.webUrl.endsWith("/")).isTrue();
    assertThat(unitUnderTest.projectPrefix).isNotNull();
  }

  @Test(expected = URISyntaxException.class)
  public void testConstructorWithURISyntaxException() throws Exception {
    unitUnderTest = new GoImportFilter(mockAnonProvider, mockPerms, mockProjectCache, "\\\\");
  }

  @Test
  public void testDoFilterWithNull() throws Exception {
    unitUnderTest.doFilter(null, null, mockChain);
    verify(mockChain, times(1)).doFilter(null, null);
  }

  @Test
  public void testDoFilterWithoutGoGetParameter() throws Exception {
    when(mockRequest.getServletPath()).thenReturn("/projectName");
    when(mockRequest.getParameter("go-get")).thenReturn(null);
    unitUnderTest.doFilter(mockRequest, mockResponse, mockChain);
    verify(mockOutputStream, times(0)).write(any(byte[].class));
    verify(mockChain, times(1)).doFilter(mockRequest, mockResponse);
  }

  @Test
  public void testDoFilterWithWrongGoGetParameterValue() throws Exception {
    when(mockRequest.getServletPath()).thenReturn("/projectName");
    when(mockRequest.getParameter("go-get")).thenReturn("2");
    unitUnderTest.doFilter(mockRequest, mockResponse, mockChain);
    verify(mockOutputStream, times(0)).write(any(byte[].class));
    verify(mockChain, times(1)).doFilter(mockRequest, mockResponse);
  }

  @Test
  public void testDoFilterWithExistingProject() throws Exception {
    when(mockRequest.getServletPath()).thenReturn("/projectName");
    when(mockRequest.getParameter("go-get")).thenReturn("1");
    when(mockProjectCache.get(Project.nameKey("projectName"))).thenReturn(mockProjectState);
    when(mockPermsForRef.testOrFalse(RefPermission.READ)).thenReturn(false);
    unitUnderTest.doFilter(mockRequest, mockResponse, mockChain);
    verify(mockOutputStream, times(1)).write(PAGE_200_AUTH.getBytes());
    verify(mockChain, times(0)).doFilter(mockRequest, mockResponse);
    verify(mockProjectCache, times(1)).get(any(Project.NameKey.class));
    verify(mockResponse, times(1)).setStatus(200);
    verify(mockPermsForRef, times(1)).testOrFalse(RefPermission.READ);
  }

  @Test
  public void testDoFilterWithExistingProjectAndPackage() throws Exception {
    when(mockRequest.getServletPath()).thenReturn("/projectName/my/package");
    when(mockRequest.getParameter("go-get")).thenReturn("1");
    when(mockProjectCache.get(Project.nameKey("projectName"))).thenReturn(mockProjectState);
    when(mockPermsForRef.testOrFalse(RefPermission.READ)).thenReturn(false);
    unitUnderTest.doFilter(mockRequest, mockResponse, mockChain);
    verify(mockOutputStream, times(1)).write(PAGE_200_AUTH.getBytes());
    verify(mockChain, times(0)).doFilter(mockRequest, mockResponse);
    verify(mockProjectCache, times(3)).get(any(Project.NameKey.class));
    verify(mockResponse, times(1)).setStatus(200);
    verify(mockPermsForRef, times(1)).testOrFalse(RefPermission.READ);
  }

  @Test
  public void testDoFilterWithAnonymousAccessibleProject() throws Exception {
    when(mockRequest.getServletPath()).thenReturn("/projectName");
    when(mockRequest.getParameter("go-get")).thenReturn("1");
    when(mockProjectCache.get(Project.nameKey("projectName"))).thenReturn(mockProjectState);
    when(mockPermsForRef.testOrFalse(RefPermission.READ)).thenReturn(true);
    unitUnderTest.doFilter(mockRequest, mockResponse, mockChain);
    verify(mockOutputStream, times(1)).write(PAGE_200_ANON.getBytes());
    verify(mockChain, times(0)).doFilter(mockRequest, mockResponse);
    verify(mockProjectCache, times(1)).get(any(Project.NameKey.class));
    verify(mockResponse, times(1)).setStatus(200);
    verify(mockPermsForRef, times(1)).testOrFalse(RefPermission.READ);
  }

  @Test
  public void testDoFilterWithNonExistingProject() throws Exception {
    when(mockRequest.getServletPath()).thenReturn("/projectName");
    when(mockRequest.getParameter("go-get")).thenReturn("1");
    when(mockProjectCache.get(any(Project.NameKey.class))).thenReturn(null);
    unitUnderTest.doFilter(mockRequest, mockResponse, mockChain);
    verify(mockOutputStream, times(1)).write(any(byte[].class));
    verify(mockChain, times(0)).doFilter(mockRequest, mockResponse);
    verify(mockProjectCache, times(1)).get(any(Project.NameKey.class));
    verify(mockResponse, times(1)).setStatus(404);
  }

  @Test
  public void testDoFilterWithIOException() throws Exception {
    String msg = "test-io-error";
    when(mockRequest.getServletPath()).thenReturn("/projectName");
    when(mockRequest.getParameter("go-get")).thenReturn("1");
    doThrow(new IOException(msg)).when(mockOutputStream).write(any(byte[].class));
    when(mockProjectCache.get(any(Project.NameKey.class))).thenReturn(mockProjectState);
    try {
      unitUnderTest.doFilter(mockRequest, mockResponse, mockChain);
      fail("IOException should occur!");
    } catch (IOException e) {
      assertThat(msg).isEqualTo(e.getMessage());
      verify(mockOutputStream, times(1)).write(any(byte[].class));
    }
  }

  @Test
  public void testDoFilterWithServletException() throws Exception {
    String msg = "test-serv-error";
    doThrow(new ServletException(msg)).when(mockChain).doFilter(null, null);
    try {
      unitUnderTest.doFilter(null, null, mockChain);
      fail("ServletException should occur!");
    } catch (ServletException e) {
      assertThat(msg).isEqualTo(e.getMessage());
      verify(mockChain, times(1)).doFilter(null, null);
    }
  }
}
