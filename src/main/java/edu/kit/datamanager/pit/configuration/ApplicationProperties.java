/*
 * Copyright 2018 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.pit.configuration;

import edu.kit.datamanager.configuration.GenericApplicationProperties;
import java.net.URL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author jejkal
 */
@Component
@Validated
public class ApplicationProperties extends GenericApplicationProperties {

  public enum IdentifierSystemImpl {
    IN_MEMORY,
    HANDLE_REST,
    HANDLE_PROTOCOL;
  }

  @Value("${pit.pidsystem.implementation}")
  private IdentifierSystemImpl identifierSystemImplementation;

  @Value("${pit.pidsystem.handle.baseURI}")
  private URL handleBaseUri;

  @Value("${pit.typeregistry.baseURI}")
  private URL typeRegistryUri;

  public IdentifierSystemImpl getIdentifierSystemImplementation() {
    return identifierSystemImplementation;
  }

  public void setIdentifierSystemImplementation(IdentifierSystemImpl identifierSystemImplementation) {
    this.identifierSystemImplementation = identifierSystemImplementation;
  }

  public URL getHandleBaseUri() {
    return handleBaseUri;
  }

  public void setHandleBaseUri(URL handleBaseUri) {
    this.handleBaseUri = handleBaseUri;
  }

  public URL getTypeRegistryUri() {
    return typeRegistryUri;
  }

  public void setTypeRegistryUri(URL typeRegistryUri) {
    this.typeRegistryUri = typeRegistryUri;
  }
}
