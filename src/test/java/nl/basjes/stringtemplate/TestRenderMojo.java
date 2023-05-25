/*
 *  StringTemplate Maven Plugin
 *  Copyright (C) 2023 Niels Basjes
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an AS IS BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package nl.basjes.stringtemplate;

import io.takari.maven.testing.TestMavenRuntime5;
import io.takari.maven.testing.TestResources5;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import static io.takari.maven.testing.AbstractTestResources.assertFileContents;
import static io.takari.maven.testing.TestMavenRuntime.newParameter;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TestRenderMojo {

    @RegisterExtension
    final TestResources5 resources = new TestResources5();

    @RegisterExtension
    final TestMavenRuntime5 maven = new TestMavenRuntime5();

    private static final String TEMPLATE_FILENAME = "GenerateTest.stg";
    private static final String OUTPUT_FILENAME = "target/tests/TestRenderMojo/Generated.yaml";
    private static final String TEST_PROPERTIES = "test.properties";


    private Xpp3Dom getProperties(String... kvList) {
        Xpp3Dom result = new Xpp3Dom("properties");
        int i = 0;
        while (kvList.length > i+1) {
            String key = kvList[i];
            String value = kvList[i+1];
            Xpp3Dom setting = new Xpp3Dom(key);
            setting.setValue(value);
            result.addChild(setting);
            i+=2;
        }
        return result;
    }


    @Test
    void testNormal() throws Exception {
        File basedir = expectPass(
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            OUTPUT_FILENAME
        );

        assertFileContents(
            "properties:\n" +
                "  overridden: 'properties file'\n" +
                "  something.name: 'Noot'\n" +
                "  test: 'aap'\n",
            basedir,
            OUTPUT_FILENAME
        );
    }

    @Test
    void testNormalPropertyViaPomXml() throws Exception {
        File basedir = expectPass(
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            getProperties("overridden","pom.xml", "mies","wIm")
        );

        assertFileContents(
            "properties:\n" +
            "  mies: 'wIm'\n" +
            "  overridden: 'pom.xml'\n" +
            "  something.name: 'Noot'\n" +
            "  test: 'aap'\n",
            basedir,
            OUTPUT_FILENAME
        );
    }

    @Test
    void testNormalOutputDirectoryAlreadyExists() throws Exception {
        File basedir = expectPass(
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            "Generated.yaml"
        );

        assertFileContents(
            "properties:\n" +
                "  overridden: 'properties file'\n" +
                "  something.name: 'Noot'\n" +
                "  test: 'aap'\n",
            basedir,
            "Generated.yaml"
        );
    }

    @Test
    void testTemplateFileAccessDenied() throws Exception {
        File basedir = getBasedir();
        assertNotNull(basedir);

        Files.setPosixFilePermissions(
            FileSystems.getDefault().getPath(
                basedir.toPath().toAbsolutePath().toString(),
                TEMPLATE_FILENAME),
            PosixFilePermissions.fromString("---------"));

        expectFailWithMessage(
            basedir,
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "Unable to find group \"Render\" in template file"
        );
    }


    @Test
    void testPropertiesFileAccessDenied() throws Exception {
        File basedir = getBasedir();
        assertNotNull(basedir);

        Files.setPosixFilePermissions(
            FileSystems.getDefault().getPath(
                basedir.toPath().toAbsolutePath().toString(),
                TEST_PROPERTIES),
            PosixFilePermissions.fromString("---------"));

        expectFailWithMessage(
            basedir,
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "Unable to read the provided properties file"
        );
    }

    @Test
    void testOutputDirAccessDenied() throws Exception {
        File basedir = getBasedir();
        assertNotNull(basedir);

        File outputDir = FileSystems.getDefault().getPath(
            basedir.toPath().toAbsolutePath().toString(),
            OUTPUT_FILENAME).getParent().toAbsolutePath().toFile();

        assertTrue(outputDir.mkdirs(), "Unable to create outputDir");

        Files.
            setPosixFilePermissions(
                outputDir.toPath(),
                PosixFilePermissions.fromString("---------"));

        expectFailWithMessage(
            basedir,
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "Unable to write output file"
        );
    }

    @Test
    void testOutputDirCreationDenied() throws Exception {
        File basedir = getBasedir();
        assertNotNull(basedir);

        File outputDir = FileSystems.getDefault().getPath(
            basedir.toPath().toAbsolutePath().toString(),
            OUTPUT_FILENAME).getParent().getParent().toAbsolutePath().toFile();

        assertTrue(outputDir.mkdirs(), "Unable to create outputDir");

        Files.
            setPosixFilePermissions(
                outputDir.toPath(),
                PosixFilePermissions.fromString("---------"));

        expectFailWithMessage(
            basedir,
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "Needed output directory was not present and could not be created."
        );
    }


    @Test
    void testTemplateFileMissing() {
        expectFailWithMessage(
            null,
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "No templateFile specified"
        );
    }

    @Test
    void testTemplateFileNotExist() {
        expectFailWithMessage(
            "NoSuchFile.stg",
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "Specified templateFile does not exist"
        );
    }

    @Test
    void testTemplateFileNotAFile() {
        expectFailWithMessage(
            "..",
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "Specified templateFile is not a file"
        );
    }

    @Test
    void testTemplateFileEmpty() {
        expectFailWithMessage(
            "",
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "No templateFile specified"
        );
    }

    @Test
    void testTemplateFileBlank() {
        expectFailWithMessage(
            "     ",
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "No templateFile specified"
        );
    }


    @Test
    void testTemplateNoRender() {
        expectFailWithMessage(
            "BadTemplateNoRender.stg",
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "Unable to find group \"Render\" in template file"
        );
    }

    @Test
    void testTemplateParamsMissing() {
        expectFailWithMessage(
            "BadTemplateParamsMissing.stg",
            TEST_PROPERTIES,
            OUTPUT_FILENAME,
            "no such attribute: properties"
        );
    }

    @Test
    void testTemplateParamsExtra() {
        expectPass(
            "BadTemplateParamsExtra.stg",
            TEST_PROPERTIES,
            OUTPUT_FILENAME
        );
    }

    @Test
    void testOutputFileMissing() {
        expectFailWithMessage(
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            null,
            "No outputFile specified"
        );
    }

    @Test
    void testOutputFileEmpty() {
        expectFailWithMessage(
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            "",
            "No outputFile specified"
        );
    }

    @Test
    void testOutputFileBlank() {
        expectFailWithMessage(
            TEMPLATE_FILENAME,
            TEST_PROPERTIES,
            "   ",
            "No outputFile specified"
        );
    }

    @Test
    void testPropertiesFileNotExist() {
        expectFailWithMessage(
            TEMPLATE_FILENAME,
            "NoSuch.properties",
            OUTPUT_FILENAME,
            "Specified propertiesFile does not exist"
        );
    }

    @Test
    void testPropertiesFileNotAFile() {
        expectFailWithMessage(
            TEMPLATE_FILENAME,
            "..",
            OUTPUT_FILENAME,
            "Specified propertiesFile is not a file"
        );
    }

    @Test
    void testPropertiesFileEmpty() throws IOException {
        File basedir = expectPass(
            TEMPLATE_FILENAME,
            "",
            OUTPUT_FILENAME
        );

        assertFileContents(
            "properties:\n",
            basedir,
            OUTPUT_FILENAME);
    }

    @Test
    void testPropertiesFileBlank() throws IOException {
        File basedir = expectPass(
            TEMPLATE_FILENAME,
            "   ",
            OUTPUT_FILENAME
        );

        assertFileContents(
            "properties:\n",
            basedir,
            OUTPUT_FILENAME);
    }

    private File getBasedir() {
        try {
            return resources.getBasedir("base");
        } catch(Exception e) {
            fail("This should not happen");
        }
        return null;
    }

    private void expectFailWithMessage(
        final String templateFile,
        final String propertiesFile,
        final String outputFile,
        final String expectedMessage
    ) {
        expectFailWithMessage(
            getBasedir(),
            templateFile,
            propertiesFile,
            outputFile,
            expectedMessage
        );
    }

    private void expectFailWithMessage(
        final File baseDir,
        final String templateFile,
        final String propertiesFile,
        final String outputFile,
        final String expectedMessage
    ) {
        List<Xpp3Dom> parameters = new ArrayList<>();

        if (templateFile != null) {
            parameters.add(newParameter("templateFile", templateFile));
        }
        if (propertiesFile != null) {
            parameters.add(newParameter("propertiesFile", propertiesFile));
        }
        if (outputFile != null) {
            parameters.add(newParameter("outputFile", outputFile));
        }
        MojoExecutionException mojoExecutionException = assertThrows(MojoExecutionException.class,
            () -> maven.executeMojo(baseDir, "render", parameters.toArray(new Xpp3Dom[0])));
        assertTrue(mojoExecutionException.getMessage().contains(expectedMessage),
            "Missing expected message: \""+expectedMessage+"\"; Actual message: \""+mojoExecutionException.getMessage()+"\".");
    }

    private File expectPass(
        String templateFile,
        String propertiesFile,
        String outputFile
    ) {
        return expectPass(
            templateFile,
            propertiesFile,
            outputFile,
            null);
    }

    private File expectPass(
        String templateFile,
        String propertiesFile,
        String outputFile,
        Xpp3Dom properties
    ) {
        final File basedir = getBasedir();
        List<Xpp3Dom> parameters = new ArrayList<>();

        if (templateFile != null) {
            parameters.add(newParameter("templateFile", templateFile));
        }
        if (propertiesFile != null) {
            parameters.add(newParameter("propertiesFile", propertiesFile));
        }
        if (properties != null) {
            parameters.add(properties);
        }
        if (outputFile != null) {
            parameters.add(newParameter("outputFile", outputFile));
        }
        assertDoesNotThrow(
            () -> maven.executeMojo(basedir, "render", parameters.toArray(new Xpp3Dom[0])));

        return basedir;
    }


}
