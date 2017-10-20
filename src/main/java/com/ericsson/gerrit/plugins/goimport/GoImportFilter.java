// Copyright (C) 2017 Ericsson
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

import com.google.gerrit.httpd.AllRequestFilter;
import com.google.gerrit.httpd.HtmlDomUtil;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gwtexpui.server.CacheHeaders;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class GoImportFilter extends AllRequestFilter {

  private static final String PAGE_404 = "<!DOCTYPE html>\n"
      + "<html>\n"
      + "<head>\n"
      + "  <title>Gerrit-Go-Import</title>\n"
      + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
      + "</head>\n"
      + "<body>\n"
      + "NOT FOUND\n"
      + "</body>\n"
      + "</html>";

  private static final String PAGE_200 = "<!DOCTYPE html>\n"
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

  private final ProjectCache projectCache;
  final String webUrl;
  final String projectPrefix;

  @Inject
  GoImportFilter(ProjectCache projectCache,
      @CanonicalWebUrl String webUrl)
      throws URISyntaxException {
    this.projectCache = projectCache;
    this.webUrl = webUrl.replaceFirst("/?$", "/");
    this.projectPrefix = generateProjectPrefix();
  }

  private String generateProjectPrefix() throws URISyntaxException {
    URI uri = new URI(webUrl);
    return uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort())
        + uri.getPath();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest req = (HttpServletRequest) request;
      HttpServletResponse rsp = (HttpServletResponse) response;
      String servletPath = req.getServletPath();
      String goGet = req.getParameter("go-get");
      if ("1".equals(goGet)) {
        String projectName = getProjectName(servletPath);
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
        String[] pathParts = projectName.split("/");

        byte[] tosend = PAGE_404.getBytes();
        rsp.setStatus(404);
        // Start with the longest-length project name first.
        for( int length = pathParts.length; length > 0; length-- ) {
          // Create a new project name of the specified length; each time that we
          // go through this loop, the project name will become shorter and shorter.
          projectName = "";
          for( int i = 0; i < length; i++ ) {
            if( i > 0 ) {
              projectName += "/";
            }
            projectName += pathParts[i];
          }

          if (projectExists(projectName)) {
            tosend = PAGE_200.replace("${content}", getContent(projectName)).getBytes();
            rsp.setStatus(200);
            break;
          }
        }
        CacheHeaders.setNotCacheable(rsp);
        rsp.setContentType("text/html");
        rsp.setCharacterEncoding(HtmlDomUtil.ENC.name());
        rsp.setContentLength(tosend.length);
        try (OutputStream out = rsp.getOutputStream()) {
          out.write(tosend);
        }
      } else {
        chain.doFilter(request, response);
      }
    } else {
      chain.doFilter(request, response);
    }

  }

  private String getProjectName(String servletPath) {
    return servletPath.replaceFirst("/", "");
  }

  private CharSequence getContent(String projectName) {
    return projectPrefix + projectName + " git " + webUrl  + "a/" + projectName;
  }

  private boolean projectExists(String projectName) {
    ProjectState p = projectCache.get(new Project.NameKey(projectName));
    return p != null;
  }
}
