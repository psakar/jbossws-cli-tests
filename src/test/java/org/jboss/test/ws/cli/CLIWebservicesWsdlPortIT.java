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

import java.net.URL;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.test.ws.BaseDeployment.WarDeployment;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public final class CLIWebservicesWsdlPortIT extends CLITestUtils
{
   private static final int WSDL_PORT = 8080;
   private static final int WSDL_PORT_CHANGED = 8084;

   private static final String NAME = "CLIWebservicesWsdlPortTestCase";
   private static final String WAR_NAME = NAME + WAR_EXTENSTION;
   private final String serviceURL = "http://" + "localhost"/*JBossWSTestHelper.getServerHost()*/ + ":" + WSDL_PORT + "/" + NAME + "/AnnotatedSecurityService";
   private final String serviceURLChanged = "http://" + "localhost"/*JBossWSTestHelper.getServerHost()*/ + ":" + WSDL_PORT_CHANGED + "/" + NAME + "/AnnotatedSecurityService";
   private URL wsdlURL;
   private URL wsdlURLChanged;

   private static final String NAME2 = "CLIWebservicesWsdlPortTestCase2";
   private static final String WAR_NAME2 = NAME2 + WAR_EXTENSTION;
   private final String serviceURL2 = "http://" + "localhost"/*JBossWSTestHelper.getServerHost()*/ + ":" + WSDL_PORT + "/" + NAME2 + "/AnnotatedSecurityService";
   private final String serviceURLChanged2 = "http://" + "localhost"/*JBossWSTestHelper.getServerHost()*/ + ":" + WSDL_PORT_CHANGED + "/" + NAME2 + "/AnnotatedSecurityService";
   private URL wsdlURL2;
   private URL wsdlURLChanged2;


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
      wsdlURLChanged = new URL(serviceURLChanged + "?wsdl");
      wsdlURL2 = new URL(serviceURL2 + "?wsdl");
      wsdlURLChanged2 = new URL(serviceURLChanged2 + "?wsdl");
   }

   @After
   public void after() throws Exception {
      info("After");
      undeployQuietly(WAR_NAME);
      undeployQuietly(WAR_NAME2);
      executeCLICommandQuietly("/subsystem=webservices/:undefine-attribute(name=wsdl-port)");
      restartServer(); //remove when https://bugzilla.redhat.com/show_bug.cgi?id=987904 is resolved
                       // add executeCLICommandQuietly("/:reload");
   }

   @Test
   public void testDefaultWsdlPort() throws Exception
   {
      executeAssertedCLIdeploy(createWarDeployment(WAR_NAME).createArchive());

      assertCorrectWsdlReturned(readUrlToString(wsdlURL));
      assertServiceIsFunctional(serviceURL);

      assertUrlIsNotAccessible(wsdlURLChanged);
      assertServiceIsNotAvailable(serviceURLChanged);
   }

   private void assertCorrectWsdlReturned(String wsdl)
   {
      assertTrue(wsdl.contains("sayHelloResponse"));
   }

   @Ignore
   public void testOriginalWsdlPortIsAccessibleAfterChange() throws Exception
   {
      executeAssertedCLIdeploy(createWarDeployment(WAR_NAME).createArchive());

      CLIResult result = executeAssertedCLICommand("/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_CHANGED + ")");
      assertChangeWsdlPortCommandResult(result);

      String wsdl = readUrlToString(wsdlURL);

      assertCorrectWsdlReturned(wsdl);
      assertServiceIsFunctional(serviceURL);

      assertUrlIsNotAccessible(wsdlURLChanged);
      assertServiceIsNotAvailable(serviceURLChanged);

   }

   @Ignore
   public void testOriginalWsdlPortIsAccessibleAfterChangeForAnotherDeployment() throws Exception
   {
      executeAssertedCLIdeploy(createWarDeployment(WAR_NAME).createArchive());
      CLIResult result = executeAssertedCLICommand("/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_CHANGED + ")");
      assertChangeWsdlPortCommandResult(result);

      executeAssertedCLIdeploy(createWarDeployment(WAR_NAME2).createArchive());

      String wsdl = readUrlToString(wsdlURL2);

      assertCorrectWsdlReturned(wsdl);
      assertServiceIsFunctional(serviceURL2);

      assertUrlIsNotAccessible(wsdlURLChanged2);
      assertServiceIsNotAvailable(serviceURLChanged2);
   }

   @Ignore
   public void testOriginalWsdlPortIsAccessibleAfterChangeForRedeployment() throws Exception
   {

      executeAssertedCLIdeploy(createWarDeployment(WAR_NAME).createArchive());
      CLIResult result = executeAssertedCLICommand("/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_CHANGED + ")");
      assertChangeWsdlPortCommandResult(result);

      executeAssertedCLICommand("/deployment=" + WAR_NAME + "/:redeploy");


      String wsdl = readUrlToString(wsdlURL);

      assertCorrectWsdlReturned(wsdl);
      assertServiceIsFunctional(serviceURL);

      assertUrlIsNotAccessible(wsdlURLChanged);
      assertServiceIsNotAvailable(serviceURLChanged);

   }

   private void assertChangeWsdlPortCommandResult(CLIResult result)
   {
      result.assertCLIOperationRequiesReload();
      result.assertCLIResultIsReloadRequired();
   }



}
