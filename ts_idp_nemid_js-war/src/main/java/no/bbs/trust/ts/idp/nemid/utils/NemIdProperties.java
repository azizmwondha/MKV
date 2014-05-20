/**
 * Copyright (c) 2010, DanID A/S
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of the DanID A/S nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.bbs.trust.ts.idp.nemid.utils;

import org.openoces.ooapi.environment.Environments;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class NemIdProperties {
    private static String serverUrlPrefix;
    private static String appletParameterSigningKeystore;
    private static String appletParameterSigningKeystorePassword;
    private static String appletParameterSigningKeystoreAlias;
    private static String appletParameterSigningKeystoreKeyPassword;
    private static String serviceProviderId;
    private static String openOcesLocation;
    private static String openOcesJar;

    private static Environments.Environment[] environments;
    private static Environments.Environment pidEnvironment;

    static {
        readProperties();
    }

    private static void readProperties() {
        Properties properties = new Properties();
        try {
            InputStream propertiesAsStream = NemIdProperties.class.getResourceAsStream("/nemid.properties");
            if (propertiesAsStream == null) {
                throw new IllegalStateException("/nemid.properties not found on classpath");
            }
            properties.load(propertiesAsStream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read property file nemid.properties from classpath", e);
        }

        serverUrlPrefix = getRequiredProperty(properties, "nemid.applet.server.url.prefix", " to the URL of the applet providing server, eg. https://applet.danid.dk");
        appletParameterSigningKeystore = getRequiredProperty(properties, "nemid.applet.parameter.signing.keystore", " to a classpath path to the keystore, eg. /applet-parameter-signing-keystore-cvr30808460-uid1263281782319.jks");
        appletParameterSigningKeystorePassword = getRequiredProperty(properties, "nemid.applet.parameter.signing.keystore.password", " to the password to the keystore pointed to by nemid.applet.parameter.signing.keystore");
        appletParameterSigningKeystoreAlias = getRequiredProperty(properties, "nemid.applet.parameter.signing.keystore.alias", " to the alias of the key to be used in the keystore pointed to by nemid.applet.parameter.signing.keystore");
        appletParameterSigningKeystoreKeyPassword = getRequiredProperty(properties, "nemid.applet.parameter.signing.keystore.keypassword", " to the password for the key pointed to by nemid.applet.parameter.signing.keystore.alias");
        serviceProviderId = getRequiredProperty(properties, "nemid.pidservice.serviceproviderid", " to your service provider id");
        environments = getEnvironmentsFromProperty(properties, "nemid.environment");
        pidEnvironment = getPidEnvironmentFromProperty(properties, "nemid.pidservice.environment");
        openOcesLocation = getRequiredProperty(properties, "openoces.applet.server.url", " to the URL of the opensign applet providing server");
        openOcesJar = getRequiredProperty(properties, "openoces.applet.name", " to the name of the the opensign applet");
    }

    private static Environments.Environment[] getEnvironmentsFromProperty(Properties properties, String s) {
        String environments = getRequiredProperty(properties, s, " to the environments to run against, eg. OCESI_DANID_ENV_PROD, " +
                "OCESII_DANID_ENV_PROD or OCESII_DANID_ENV_EXTERNALTEST (use \",\" to separate environments)").toUpperCase();
        String[] envs = environments.split(",");
        Environments.Environment[] environmentArray = new Environments.Environment[envs.length];
        for (int i = 0; i < envs.length; i++) {
            environmentArray[i] = Environments.Environment.valueOf(envs[i]);
        }
        return environmentArray;
    }

    private static Environments.Environment getPidEnvironmentFromProperty(Properties properties, String s) {
        return Environments.Environment.valueOf(getRequiredProperty(properties, s, " to the environment to check PID against, eg. OCESI_DANID_ENV_PROD, " +
                "OCESII_DANID_ENV_PROD or OCESII_DANID_ENV_EXTERNALTEST").toUpperCase());
    }

    private static String getRequiredProperty(Properties properties, String key, String helpMsg) {
        String value = properties.getProperty(key);
        if (value == null || value.length()==0) {
            throw new IllegalStateException("You must set property " + key + " in nemid.properties" + helpMsg);
        }

        return value;
    }

    public static String getServerUrlPrefix() {
        return serverUrlPrefix;
    }

    public static String getAppletParameterSigningKeystore() {
        return appletParameterSigningKeystore;
    }

    public static String getAppletParameterSigningKeystorePassword() {
        return appletParameterSigningKeystorePassword;
    }

    public static String getAppletParameterSigningKeystoreAlias() {
        return appletParameterSigningKeystoreAlias;
    }

    public static String getAppletParameterSigningKeyPassword() {
        return appletParameterSigningKeystoreKeyPassword;
    }

    public static String getServiceProviderId() {
        return serviceProviderId;
    }

    public static Environments.Environment[] getEnvironments() {
        return environments;
    }

    public static Environments.Environment getPidEnvironment() {
        return pidEnvironment;
    }

    public static String getOpenSignAppletName() {
        return openOcesJar + "?time=" + System.currentTimeMillis();
    }

    public static String getOpenSignAppletUrl() {
        return openOcesLocation;
    }
}
