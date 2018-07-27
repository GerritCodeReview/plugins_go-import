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

import com.google.common.base.Strings;
import com.google.gerrit.httpd.AllRequestFilter;
import com.google.gerrit.httpd.HtmlDomUtil;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.RefPermission;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.util.http.CacheHeaders;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class GoImportFilter extends AllRequestFilter {

  private static final String PAGE_404 =
      "<!DOCTYPE html>\n"
          + "<html>\n"
          + "<head>\n"
          + "  <title>Gerrit-Go-Import</title>\n"
          + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
          + "</head>\n"
          + "<body>\n"
          + "NOT FOUND\n"
          + "</body>\n"
          + "</html>";

  private static final String PAGE_200 =
      "<!DOCTYPE html>\n"
          + "<html>\n"
          + "<head>\n"
          + "  <title>Gerrit-Go-Import</title>\n"
          + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
          + "  <meta name=\"go-import\" content=\"${content}\"/>\n"
          + "</head>\n"
          + "<body>\n"
          + "<div>\n"
          + "  Gerrit-Go-Import\n"
          + "</div>\n"
          + "</body>\n"
          + "</html>";

  private final Provider<AnonymousUser> anonProvider;
  private final PermissionBackend permissions;
  private final ProjectCache projectCache;
  final String webUrl;
  final String projectPrefix;

  @Inject
  GoImportFilter(
      Provider<AnonymousUser> anonProvider,
      PermissionBackend permissions,
      ProjectCache projectCache,
      @CanonicalWebUrl String webUrl)
      throws URISyntaxException {
    this.anonProvider = anonProvider;
    this.permissions = permissions;
    this.projectCache = projectCache;
    this.webUrl = webUrl.replaceFirst("/?$", "/");
    this.projectPrefix = generateProjectPrefix();
  }

  private String generateProjectPrefix() throws URISyntaxException {
    URI uri = new URI(webUrl);
    return uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + uri.getPath();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest req = (HttpServletRequest) request;
      HttpServletResponse rsp = (HttpServletResponse) response;
      String servletPath = req.getServletPath();
      if ("1".equals(req.getParameter("go-get"))) {
        // Because Gerrit allows for arbitrary-depth project names
        // (that is, both "a" and "a/b/c" are both legal), we are going
        // to find the most specific such project that matches the path.
        //
        // For example, assume that we have the following projects:
        //    a
        //    a/b
        // 1. If the requested path is "a", then project "a" would be chosen.
        // 2. If the requested path is "a/b", then project "a/b" would be chosen.
        // 3. If the requested path is "a/c", then project "a" would be chosen.
        // 4. If the requested path is "a/b/c/d", then project "a/b" would be chosen.
        // 5. If the requested path is "x/y/z", then this will fail with a 404 error.
        String existent = getLongestMatch(getProjectName(servletPath));
        byte[] toSend = PAGE_404.getBytes();
        rsp.setStatus(404);
        if (!Strings.isNullOrEmpty(existent)) {
          toSend = PAGE_200.replace("${content}", getContent(existent)).getBytes();
          rsp.setStatus(200);
        }
        CacheHeaders.setNotCacheable(rsp);
        rsp.setContentType("text/html");
        rsp.setCharacterEncoding(HtmlDomUtil.ENC.name());
        rsp.setContentLength(toSend.length);
        try (OutputStream out = rsp.getOutputStream()) {
          out.write(toSend);
        }
      } else {
        chain.doFilter(request, response);
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  private String getLongestMatch(String projectName) {
    Path projectPath = Paths.get(projectName);
    while (projectPath.getNameCount() >= 1) {
      String asString = projectPath.toString();
      if (projectExists(asString)) {
        return asString;
      }
      projectPath = projectPath.getParent();
      if (projectPath == null) {
        break;
      }
    }
    return null;
  }

  private String getProjectName(String servletPath) {
    return servletPath.replaceFirst("/", "");
  }

  private CharSequence getContent(String projectName) {
    return projectPrefix + projectName + " git " + getRepoRoot(projectName);
  }

  private String getRepoRoot(String projectName) {
    if (allowsAnonymousAccess(projectName)) {
      return webUrl + projectName;
    }

    return webUrl + "a/" + projectName;
  }

  private boolean allowsAnonymousAccess(String projectName) {
    AnonymousUser anonymous = anonProvider.get();
    Branch.NameKey heads =
        new Branch.NameKey(new Project.NameKey(projectName), RefNames.REFS_HEADS);

    return permissions.user(anonymous).ref(heads).testOrFalse(RefPermission.READ);
  }

  private boolean projectExists(String projectName) {
    ProjectState p = projectCache.get(new Project.NameKey(projectName));
    return p != null;
  }
}
