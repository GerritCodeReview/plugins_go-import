include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')
include_defs('//bucklets/maven_jar.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])

TEST_DEPS = GERRIT_PLUGIN_API + GERRIT_TESTS + [
   ':go-import__plugin',
   ':mockito',
]

gerrit_plugin(
   name = 'go-import',
   srcs = SOURCES,
   resources = RESOURCES,
   manifest_entries = [
     'Gerrit-PluginName: go-import',
     'Gerrit-ApiType: plugin',
     'Gerrit-HttpModule: com.ericsson.gerrit.plugins.goimport.HttpModule',
     'Implementation-Title: go-import plugin',
     'Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/go-import',
     'Implementation-Vendor: Ericsson',
   ],
)

java_library(
   name = 'classpath',
   deps = TEST_DEPS,
)

java_test(
   name = 'go-import_tests',
   labels = ['go-import'],
   srcs = glob(['src/test/java/**/*.java']),
   deps = TEST_DEPS,
)

java_sources(
   name = 'go-import-sources',
   srcs = SOURCES + RESOURCES,
)

maven_jar(
  name = 'mockito',
  id = 'org.mockito:mockito-core:2.7.19',
  sha1 = '9e0dbe97eca58ef4c26e3b9e1a12ee42e76a63a5',
  license = 'DO_NOT_DISTRIBUTE',
  deps = [
    ':byte-buddy',
    ':objenesis',
  ],
)
maven_jar(
  name = 'byte-buddy',
  id = 'net.bytebuddy:byte-buddy:1.6.11',
  sha1 = '8a8f9409e27f1d62c909c7eef2aa7b3a580b4901',
  license = 'DO_NOT_DISTRIBUTE',
  attach_source = False,
)
maven_jar(
  name = 'objenesis',
  id = 'org.objenesis:objenesis:2.5',
  sha1 = '612ecb799912ccf77cba9b3ed8c813da086076e9',
  license = 'DO_NOT_DISTRIBUTE',
  attach_source = False,
)
