# What is this?
A maven plugin that uses the [StringTemplate 4](https://www.stringtemplate.org/) templating engine to generate a file.

Current version does exactly one file at a time.

# Usage
The most basic template file needed to use this plugin looks like this:

    Render(properties) ::= <<
    properties:
      <properties.keys:{key | <key>: '<properties.(key)>'}; separator="\n">
    >>

Note: The `Render(properties)` entrypoint is hardcoded in the plugin.

In the `pom.xml` you need to specify the `templateFile`, the `outputFile` and the `properties`.
You can specify `properties` directly and/or by reading them from a `propertyFile`.

If both the pom.xml and the property file contain the same key then the value from the pom.xml is used.

A property file looks something like this

    # Comment
    test= aap
    something.name: Noot


Then the plugin can be called with something like this

      <plugin>
        <groupId>nl.basjes.stringtemplate</groupId>
        <artifactId>stringtemplate-maven-plugin</artifactId>
        <version>${stringtemplate-maven-plugin.version}</version>
        <executions>
          <execution>
            <id>generate it</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>render</goal>
            </goals>
            <configuration>
              <templateFile>${project.basedir}/GenerateTest.stg</templateFile>
              <outputFile>${project.basedir}/target/Generated.yaml</outputFile>
              <properties>
                <aap>AaP</aap>
                <noot>NooT</noot>
                <mies>MieS</mies>
                <project.version>${project.version}</project.version>
              </properties>
              <propertiesFile>extra.properties</propertiesFile>
            </configuration>
          </execution>
        </executions>
      </plugin>

# License
    StringTemplate Maven Plugin
    Copyright (C) 2023 Niels Basjes

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an AS IS BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
