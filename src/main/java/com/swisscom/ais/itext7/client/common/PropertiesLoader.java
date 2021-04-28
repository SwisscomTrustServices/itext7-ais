/*
 * Copyright 2021 Swisscom (Schweiz) AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.swisscom.ais.itext7.client.common;

import com.swisscom.ais.itext7.client.config.ConfigurationProvider;
import com.swisscom.ais.itext7.client.config.ConfigurationProviderPropertiesImpl;
import com.swisscom.ais.itext7.client.utils.PropertyUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Function;

@SuppressWarnings("unused")
public abstract class PropertiesLoader<T> {

    private static final String ENV_VARIABLE_PREFIX = "${";
    private static final String ENV_VARIABLE_SUFFIX = "}";

    protected PropertiesLoader() {
    }

    private static void validateProperty(String propertyName, String propertyValue) {
        if (StringUtils.isBlank(propertyValue)) {
            throw new IllegalStateException(String.format("Invalid configuration. The [%s] property is missing or is empty.", propertyName));
        }
    }

    protected abstract T fromConfigurationProvider(ConfigurationProvider provider);

    public T fromPropertiesClasspathFile(String fileName) {
        return fromProperties(loadPropertiesFromClasspathFile(this.getClass(), fileName));
    }

    public T fromPropertiesFile(String fileName) {
        return fromProperties(PropertyUtils.loadPropertiesFromFile(fileName));
    }

    public T fromProperties(Properties properties) {
        return fromConfigurationProvider(new ConfigurationProviderPropertiesImpl(properties));
    }

    public Properties loadPropertiesFromClasspathFile(Class<?> clazz, String filepath) {
        try {
            Properties properties = new Properties();
            properties.load(clazz.getResourceAsStream(filepath));
            return properties;
        } catch (IOException e) {
            throw new AisClientException(String.format("Failed to load data properties from the classpath file: %s", filepath), e);
        }
    }

    public String extractStringProperty(ConfigurationProvider provider, String propertyName, boolean mandatory) {
        return extractProperty(provider, propertyName, mandatory);
    }

    public Integer extractIntProperty(ConfigurationProvider provider, String propertyName, boolean mandatory) {
        String property = extractProperty(provider, propertyName, mandatory);
        if (property == null && !mandatory) {
            return null;
        }
        return Integer.parseInt(property);
    }

    public String extractSecretProperty(ConfigurationProvider provider, String propertyName, boolean mandatory) {
        String property = extractProperty(provider, propertyName, mandatory);
        if (property == null && !mandatory) {
            return null;
        }
        return shouldExtractFromEnvVariable(property) ? System.getenv(extractEnvPropertyName(property)) : property;
    }

    private boolean shouldExtractFromEnvVariable(String property) {
        return property.startsWith(ENV_VARIABLE_PREFIX) && property.endsWith(ENV_VARIABLE_SUFFIX);
    }

    private String extractEnvPropertyName(String property) {
        return property.substring(ENV_VARIABLE_PREFIX.length(), property.length() - ENV_VARIABLE_SUFFIX.length());
    }

    public <E> E extractProperty(ConfigurationProvider provider, String propertyName, Function<String, E> mapperFunction, E defaultValue) {
        String propertyValue = provider.getProperty(propertyName);
        return StringUtils.isNotBlank(propertyValue) ? mapperFunction.apply(propertyValue) : defaultValue;
    }

    private String extractProperty(ConfigurationProvider provider, String propertyName, boolean mandatory) {
        String propertyValue = provider.getProperty(propertyName);
        if (StringUtils.isBlank(propertyValue) && !mandatory) {
            return null;
        }
        validateProperty(propertyName, propertyValue);
        return propertyValue;
    }
}
