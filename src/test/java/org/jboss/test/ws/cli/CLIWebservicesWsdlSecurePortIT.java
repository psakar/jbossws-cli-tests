/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.test.ws.cli;


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.cli.CommandLineException;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.test.ws.BaseDeployment.WarDeployment;
import org.junit.Before;
/**
 *
 * see https://docspace.corp.redhat.com/docs/DOC-152480
 *
 */
public final class CLIWebservicesWsdlSecurePortIT extends CLITestCase
{
   static final String PROTOCOL_HTTP = "http";
   static final String PROTOCOL_HTTPS = "https";
   static final int PORT = 8080;
   static final int WSDL_PORT = PORT;
   private static final int WSDL_PORT_HTTPS = 8843;

   static final String NAME = "CLIWebservicesWsdlSecuredPortTestCase";
   static final String NAME2 = "CLIWebservicesWsdlSecuredPortTestCase2";

   private static final String HTTPS_CONNECTOR_NAME = "jbossws-cli-tests-https-connector";
   private static final String HTTPS_LISTENER_REALM_NAME = "jbossws-cli-tests-https-realm";

   static WarDeployment createWarDeployment(String name)
   {
      return new WarDeployment(name) { {
         archive
            .setManifest(new StringAsset("Manifest-Version: 1.0\n"
                  + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"))
            .addClass(org.jboss.test.ws.cli.AnnotatedServiceIface.class)
            .addClass(org.jboss.test.ws.cli.AnnotatedServiceHttpsImpl.class)
            .addClass(org.jboss.test.ws.cli.SayHello.class)
            .addClass(org.jboss.test.ws.cli.SayHelloResponse.class)
            .addAsWebResource("index.html")
            ;
      } };
   }

   @Before
   public void before() throws Exception {
      Map<String, String> sslOptionsMap = new HashMap<String, String>();
      addSecurityRealm(HTTPS_LISTENER_REALM_NAME, sslOptionsMap);
   }

   @Override
   protected void resetConfiguration() throws IOException, CommandLineException
   {
      super.resetConfiguration();
      String command = "/core-service=management/security-realm=" + HTTPS_LISTENER_REALM_NAME + "/:remove";
      executeCLICommandQuietly(command);
      command = "/subsystem=web/connector=" + HTTPS_CONNECTOR_NAME + "/:remove";
      executeCLICommandQuietly(command);
      reloadServer();
   }

   private void addSecurityRealm(String realm, Map<String, String> sslOptions) throws Exception {
      System.setProperty("org.jboss.security.ignoreHttpsHost", "true");
      info("org.jboss.security.ignoreHttpsHost=" + System.getProperty("org.jboss.security.ignoreHttpsHost"));
      //String command = "/core-service=management/security-realm=" + realm + "/:add";
      //executeCLICommand(command).assertSuccess();
      String command = "/subsystem=web/connector=" + HTTPS_CONNECTOR_NAME + "/:add(protocol=\"HTTP/1.1\",scheme=\"https\",secure=true, socket-binding=https,enabled=true)";
      executeCLICommand(command).assertSuccess();
      File keystoreFile = new File("src/test/resources/server.keystore");
      assertTrue(keystoreFile.exists());
      command = "/subsystem=web/connector=" + HTTPS_CONNECTOR_NAME + "/ssl=configuration:add(password=\"changeit\",certificate-key-file=\""+ keystoreFile.getAbsolutePath() + "\",verify-client=false, key-alias=tomcat, protocol=https)";
      executeCLICommand(command).assertSuccess();
      temporaryFixForBZ996558();
   }

   public CLIWebservicesWsdlSecurePortIT()
   {
      super("/subsystem=webservices/:read-attribute(name=wsdl-secure-port)",
            "/subsystem=webservices/:write-attribute(name=wsdl-secure-port,value=" + WSDL_PORT_HTTPS + ")",
         "/subsystem=webservices/:undefine-attribute(name=wsdl-secure-port)",
            createWarDeployment(NAME + WAR_EXTENSTION).createArchive(),
            createWarDeployment(NAME2 + WAR_EXTENSTION).createArchive()
            );
   }

   private URL createWsdlUrl(String protocol, String name, int wsdlPort) throws MalformedURLException
   {
      return new URL(createServiceURL(protocol, name, wsdlPort) + "?wsdl");
   }

   @Override
   protected void assertDefaultConfigurationValue(CLIResult result)
   {
      result.isUndefinedResult();
   }

   private String createServiceURL(String contextName)
   {
      return createServiceURL(PROTOCOL_HTTP, contextName, PORT);
   }

   private String createServiceURL(String protocol, String contextName, int port)
   {
      return protocol + "://" + "localhost"/*JBossWSTestHelper.getServerHost()*/ + ":" + port + "/" + contextName + "/AnnotatedSecurityService";
   }


   @Override
   protected void assertOriginalConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readUrlToString(createWsdlUrl(PROTOCOL_HTTP, contextName, WSDL_PORT)), PROTOCOL_HTTP, contextName, WSDL_PORT);
      assertServiceIsFunctional(createServiceURL(contextName));
   }

   private void assertCorrectWsdlReturned(String wsdl, String protocol, String contextName, int wsdlPort)
   {
      assertTrue(wsdl.contains("sayHelloResponse"));
      assertTrue(wsdl.contains("<soap:address location=\"" + createServiceURL(protocol, contextName, wsdlPort) + "\"/>"));
   }

   @Override
   protected void assertNewConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readUrlToString(createWsdlUrl(PROTOCOL_HTTPS, contextName, WSDL_PORT_HTTPS)), PROTOCOL_HTTPS, contextName, WSDL_PORT_HTTPS);
      // FIXME assertServiceIsFunctional(createServiceURL(contextName, WSDL_PORT_CHANGED)); will not work with wdl_port rewritten to new value
   }


   @Override
   protected void assertChangeConfigurationResult(CLIResult result)
   {
      result.assertReloadRequired();
   }

   @Override
   protected void assertChangedConfigurationValue(CLIResult result)
   {
      result.assertResultAsStringEquals(WSDL_PORT_HTTPS + "");
   }
}
