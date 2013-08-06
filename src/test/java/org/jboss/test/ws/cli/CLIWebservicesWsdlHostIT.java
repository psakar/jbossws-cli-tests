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

import static org.jboss.test.ws.cli.CLIWebservicesWsdlPortIT.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
/**
 *
 * see https://docspace.corp.redhat.com/docs/DOC-152480
 *
 */
public final class CLIWebservicesWsdlHostIT extends CLITestCase
{
   private static final String WSDL_HOST = "localhost";
   private static final String WSDL_HOST_DEFAULT = "${jboss.bind.address:127.0.0.1}";//
   private static final String WSDL_HOST_CHANGED = "test.domain.com";


   public CLIWebservicesWsdlHostIT()
   {
      super("/subsystem=webservices/:read-attribute(name=wsdl-host)",
            "/subsystem=webservices/:write-attribute(name=wsdl-host,value=" + WSDL_HOST_CHANGED + ")",
         "/subsystem=webservices/:write-attribute(name=wsdl-host,value=\"" + WSDL_HOST_DEFAULT + "\")",
            createWarDeployment(NAME + WAR_EXTENSTION).createArchive(),
            createWarDeployment(NAME2 + WAR_EXTENSTION).createArchive()
            );
   }

   private URL createWsdlUrl(String name) throws MalformedURLException
   {
      return new URL(createServiceURL(name) + "?wsdl");
   }

   @Override
   protected void assertDefaultConfigurationValue(CLIResult result)
   {
      result.assertResultAsStringEquals(WSDL_HOST_DEFAULT);
   }

   private String createServiceURL(String contextName)
   {
      return createServiceURL(contextName, PORT);
   }
   private String createServiceURL(String contextName, int port)
   {
      return createServiceURL("localhost", contextName, port); ///*JBossWSTestHelper.getServerHost()*/
   }

   private String createServiceURL(String wsdlHost, String contextName, int port)
   {
      return "http://" + wsdlHost + ":" + PORT + "/" + contextName + "/AnnotatedSecurityService";
   }


   @Override
   protected void assertOriginalConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readUrlToString(createWsdlUrl(contextName)), contextName, WSDL_HOST);
      assertServiceIsFunctional(createServiceURL(contextName));
   }

   private void assertCorrectWsdlReturned(String wsdl, String contextName, String wsdlHost)
   {
      assertTrue(wsdl.contains("sayHelloResponse"));
      assertTrue(wsdl.contains("<soap:address location=\"" + createServiceURL(wsdlHost, contextName, WSDL_PORT) + "\"/>"));
   }

   @Override
   protected void assertNewConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readUrlToString(createWsdlUrl(contextName)), contextName, WSDL_HOST_CHANGED);
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
      result.assertResultAsStringEquals(WSDL_HOST_CHANGED);
   }
}
