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

import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HttpModuleTest {

  private HttpModule unitUnderTest;
  @Mock private AnonymousUser mockAnon;
  @Mock private PermissionBackend mockPerms;
  @Mock private ProjectCache mockProjectCache;

  @Before
  public void setUp() throws Exception {
    unitUnderTest = new HttpModule();
    assertThat(unitUnderTest).isNotNull();
  }

  @Test
  public void testConfigureServlets() throws Exception {
    Injector injector = Guice.createInjector(unitUnderTest, new TestModule());

    GoImportFilter filter1 = injector.getInstance(GoImportFilter.class);
    assertThat(filter1).isNotNull();
    GoImportFilter filter2 = injector.getInstance(GoImportFilter.class);
    assertThat(filter1).isSameAs(filter2);
  }

  public class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(AnonymousUser.class).toInstance(mockAnon);
      bind(PermissionBackend.class).toInstance(mockPerms);
      bind(ProjectCache.class).toInstance(mockProjectCache);
    }

    @Provides
    @CanonicalWebUrl
    String url() {
      return "url";
    }
  }
}
