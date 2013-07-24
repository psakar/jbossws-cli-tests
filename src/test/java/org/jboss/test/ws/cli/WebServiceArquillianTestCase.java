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

import static org.jboss.test.ws.cli.CLITestUtils.*;
import static org.junit.Assert.*;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.ws.BaseDeployment.WarDeployment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public final class WebServiceArquillianTestCase
{
   private static final int WSDL_PORT = 8080;

   private static final String NAME = "WebServiceTestCase";
   private static final String WAR_NAME = NAME + ".war";
   private final String serviceURL = "http://" + "localhost"/*JBossWSTestHelper.getServerHost()*/ + ":" + WSDL_PORT + "/" + NAME + "/AnnotatedSecurityService";
   private URL wsdlURL;


   @Deployment(testable = false)
   static WebArchive getDeployment() {
      return createWarDeployment(WAR_NAME).createArchive();
   }

   private static WarDeployment createWarDeployment(String name)
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

   @Before
   public void before() throws Exception {
      wsdlURL = new URL(serviceURL + "?wsdl");
   }

   @RunAsClient
   @Test
   public void testDefaultWsdlPort() throws Exception
   {
      String wsdl = CLITestUtils.readUrlToString(wsdlURL);

      assertCorrectWsdlReturned(wsdl);
      assertServiceIsFunctional(serviceURL);

   }

   private void assertCorrectWsdlReturned(String wsdl)
   {
      assertTrue(wsdl.contains("sayHelloResponse"));
   }

}
