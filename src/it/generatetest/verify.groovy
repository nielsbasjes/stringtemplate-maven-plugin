/*
 * StringTemplate Maven Plugin
 * Copyright (C) 2023 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

File generatedFile = new File( basedir, 'target/Generated.yaml' )
assert generatedFile.exists()
assert generatedFile.getText().contains("aap: 'AaP'")
assert generatedFile.getText().contains("mies: 'MieS'")
assert generatedFile.getText().contains("noot: 'NooT'")
assert generatedFile.getText().contains("something.name: 'Noot'")
assert generatedFile.getText().contains("test: 'aap'")

// Make sure the pom.xml overrides the properties file!
assert generatedFile.getText().contains("overridden: 'pom.xml'")
