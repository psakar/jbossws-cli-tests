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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.test.ws.BaseDeployment.WarDeployment;
/**
 *
 * see https://docspace.corp.redhat.com/docs/DOC-152480
 *
 */
public final class CLIWebservicesWsdlPortIT extends CLITestCase
{
   static final int PORT = 8080;
   static final int WSDL_PORT = PORT;
   private static final int WSDL_PORT_CHANGED = 8084;

   static final String NAME = "CLIWebservicesWsdlPortTestCase";
   static final String NAME2 = "CLIWebservicesWsdlPortTestCase2";


   static WarDeployment createWarDeployment(String name)
   {
      return new WarDeployment(name) { {
         archive
            .setManifest(new StringAsset("Manifest-Version: 1.0\n"
                  + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"))
            .addClass(org.jboss.test.ws.cli.AnnotatedServiceIface.class)
            .addClass(org.jboss.test.ws.cli.AnnotatedServiceImpl.class)
            .addClass(org.jboss.test.ws.cli.SayHello.class)
            .addClass(org.jboss.test.ws.cli.SayHelloResponse.class)
            ;
      } };
   }

   public CLIWebservicesWsdlPortIT()
   {
      super("/subsystem=webservices/:read-attribute(name=wsdl-port)",
            "/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_CHANGED + ")",
         "/subsystem=webservices/:undefine-attribute(name=wsdl-port)",
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
      result.isUndefinedResult();
   }

   private String createServiceURL(String contextName)
   {
      return createServiceURL(contextName, PORT);
   }

   private String createServiceURL(String contextName, int port)
   {
      return "http://" + "localhost"/*JBossWSTestHelper.getServerHost()*/ + ":" + port + "/" + contextName + "/AnnotatedSecurityService";
   }


   @Override
   protected void assertOriginalConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readUrlToString(createWsdlUrl(contextName)), contextName, WSDL_PORT);
      assertServiceIsFunctional(createServiceURL(contextName));
   }

   private void assertCorrectWsdlReturned(String wsdl, String contextName, int wsdlPort)
   {
      assertTrue(wsdl.contains("sayHelloResponse"));
      assertTrue(wsdl.contains("<soap:address location=\"" + createServiceURL(contextName, wsdlPort) + "\"/>"));
   }

   @Override
   protected void assertNewConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readUrlToString(createWsdlUrl(contextName)), contextName, WSDL_PORT_CHANGED);
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
      result.assertResultAsStringEquals(WSDL_PORT_CHANGED + "");
   }
}
