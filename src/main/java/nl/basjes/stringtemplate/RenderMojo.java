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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

@Mojo(name = "render", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@SuppressWarnings("unused")
public class RenderMojo
        extends AbstractMojo {

    @Parameter(property = "templateFile", required = true)
    private File templateFile;

    @Parameter(property = "properties")
    private Map<String, String> properties;

    @Parameter(property = "propertiesFile")
    private File propertiesFile;

    @Parameter(property = "outputFile", required = true)
    private File outputFile;

    public void execute()
            throws MojoExecutionException {
        if (templateFile == null) {
            throw new MojoExecutionException("No templateFile specified");
        }

        if (!templateFile.exists()) {
            throw new MojoExecutionException("Specified templateFile does not exist");
        }

        if (!templateFile.isFile()) {
            throw new MojoExecutionException("Specified templateFile is not a file");
        }

        STGroup group = new STGroupFile(templateFile.toPath().toString());

        // Ensure the output directory exists
        if (outputFile == null) {
            throw new MojoExecutionException("No outputFile specified");
        }

        File outputDirectory = outputFile.getParentFile();
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Needed output directory was not present and could not be created.");
            }
        }

        getLog().info("Writing " + outputFile);

        // Get the Render template
        ST template = group.getInstanceOf("Render");

        if (template == null) {
            throw new MojoExecutionException("Unable to find group \"Render\" in template file \""+templateFile.toPath()+"\"");
        }

        if (propertiesFile != null) {

            if (!propertiesFile.exists()) {
                throw new MojoExecutionException("Specified propertiesFile does not exist");
            }

            if (!propertiesFile.isFile()) {
                throw new MojoExecutionException("Specified propertiesFile is not a file");
            }

            try (InputStream input = Files.newInputStream(propertiesFile.toPath())) {
                Properties prop = new Properties();
                // load a properties file
                prop.load(input);
                for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                    String propertyKey = entry.getKey().toString();
                    if (!properties.containsKey(propertyKey)) {
                        properties.put(propertyKey, entry.getValue().toString());
                    }
                }
            } catch (IOException ioe) {
                throw new MojoExecutionException("Unable to read the provided properties file " + propertiesFile, ioe);
            }
        }

        try {
            // A TreeMap gives a predefined ordering, so it helps in making files reproducible
            template.add("properties", new TreeMap<>(properties));
        } catch (IllegalArgumentException iae) {
            throw new MojoExecutionException(iae);
        }

        // Render the content
        String renderResult = template.render();

        // Write the file
        try (FileWriter codeFileWriter = new FileWriter(outputFile)) {
            codeFileWriter.write(renderResult);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write output file (" + outputFile + "):" + e.getMessage(), e);
        }
    }
}
