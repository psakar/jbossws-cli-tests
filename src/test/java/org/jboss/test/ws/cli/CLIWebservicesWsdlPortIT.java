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

import org.jboss.as.cli.CommandLineException;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.ws.BaseDeployment.WarDeployment;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
/**
 *
 * see https://docspace.corp.redhat.com/docs/DOC-152480
 *
 */
public final class CLIWebservicesWsdlPortIT extends CLITestUtils
{
   private static final int WSDL_PORT = 8080;
   private static final int WSDL_PORT_CHANGED = 8084;

   private static final String NAME = "CLIWebservicesWsdlPortTestCase";
   private static final String NAME2 = "CLIWebservicesWsdlPortTestCase2";


   private WebArchive war;
   private WebArchive war2;


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
      war = createWarDeployment(NAME + WAR_EXTENSTION).createArchive();
      war2 = createWarDeployment(NAME2 + WAR_EXTENSTION).createArchive();
   }

   private URL createWsdlUrl(String name, int wsdlPort) throws MalformedURLException
   {
      return new URL(createServiceURL(name, wsdlPort) + "?wsdl");
   }

   @After
   public void after() throws Exception {
      info("After");
      undeployQuietly(war.getName());
      undeployQuietly(war2.getName());
      executeCLICommandQuietly("/subsystem=webservices/:undefine-attribute(name=wsdl-port)");
      restartServer(); //remove when https://bugzilla.redhat.com/show_bug.cgi?id=987904 is resolved
                       // add executeCLICommandQuietly("/:reload");
   }

   private String createServiceURL(String contextName, int wsdlPort)
   {
      return "http://" + "localhost"/*JBossWSTestHelper.getServerHost()*/ + ":" + wsdlPort + "/" + contextName + "/AnnotatedSecurityService";
   }

   @Test
   public void testDefaultConfiguration() throws Exception
   {
      deployWar();

      assertOriginalConfiguration();
   }

   private void deployWar() throws IOException, CommandLineException
   {
      executeCLIdeploy(war).assertSuccess();
   }

   private void assertOriginalConfiguration() throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertOriginalConfiguration(NAME);
   }
   private void assertOriginalConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readUrlToString(createWsdlUrl(contextName, WSDL_PORT)));
      assertServiceIsFunctional(createServiceURL(contextName, WSDL_PORT));

      assertUrlIsNotAccessible(createWsdlUrl(contextName, WSDL_PORT_CHANGED));
      assertServiceIsNotAvailable(createServiceURL(contextName, WSDL_PORT_CHANGED));
   }

   private void assertCorrectWsdlReturned(String wsdl)
   {
      assertTrue(wsdl.contains("sayHelloResponse"));
   }


   @Ignore
   @Test//1BA
   public void testChangeAffectsNewDeploymentsWithoutReload() throws Exception
   {
      changeConfiguration();

      deployWar();

      assertNewConfiguration();
   }
   private void assertNewConfiguration() throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertNewConfiguration(NAME);
   }

   private void assertNewConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readUrlToString(createWsdlUrl(contextName, WSDL_PORT_CHANGED)));
      assertServiceIsFunctional(createServiceURL(contextName, WSDL_PORT_CHANGED));
      assertUrlIsNotAccessible(createWsdlUrl(contextName, WSDL_PORT));
      assertServiceIsNotAvailable(createServiceURL(contextName, WSDL_PORT));
   }

   private void changeConfiguration() throws IOException, CommandLineException
   {
      String command = "/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_CHANGED + ")";
      executeAssertedCLICommand(command).assertReloadRequired();
   }

   @Test//2BA
   public void testChangeFollowedByReloadAffectsNewDeployments() throws Exception
   {
      changeConfiguration();

      reloadServer();

      deployWar();

      assertNewConfiguration();
   }

   private void reloadServer() throws Exception
   {
      temporaryFixForBZ987904();
      executeCLIReload().assertSuccess();
   }

   @Test//3BA
   public void testChangeDoesNotAffectExistingDeploymentsBeforeReload() throws Exception
   {
      deployWar();

      changeConfiguration();

      assertOriginalConfiguration();

   }



   @Test//4BA
   public void testChangeAffectsExistingDeploymentsAfterReload() throws Exception
   {
      deployWar();

      changeConfiguration();

      reloadServer();

      assertNewConfiguration();
   }



   @Test//5BA
   public void testChangeAffectsNewDeploymentsBeforeReloadAndExistingDeploymentsAfterReload() throws Exception
   {

      deployWar();

      changeConfiguration();

      deployAnotherWar();

      assertOriginalConfiguration(NAME);
      assertNewConfiguration(NAME2);

      reloadServer();

      assertNewConfiguration(NAME);
      assertNewConfiguration(NAME2);
   }

   private void deployAnotherWar() throws IOException, CommandLineException
   {
      executeCLIdeploy(war2).assertSuccess();
   }

   /*
   @Ignore
   public void testOriginalWsdlPortIsAccessibleAfterChangeForAnotherDeployment() throws Exception
   {
      executeCLIdeploy((Archive<?>) createWarDeployment(WAR_NAME).createArchive()).assertSuccess();
      CLIResult result = executeAssertedCLICommand("/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_CHANGED + ")");
      result.assertReloadRequired();

      executeCLIdeploy((Archive<?>) createWarDeployment(WAR_NAME2).createArchive()).assertSuccess();

      String wsdl = readUrlToString(createWsdlUrl(NAME2, WSDL_PORT));

      assertCorrectWsdlReturned(wsdl);
      assertServiceIsFunctional(createServiceURL(NAME2, WSDL_PORT));

      assertUrlIsNotAccessible(createWsdlUrl(NAME2, WSDL_PORT_CHANGED));
      assertServiceIsNotAvailable(createServiceURL(NAME2, WSDL_PORT_CHANGED));
   }

   @Ignore
   public void testOriginalWsdlPortIsAccessibleAfterChangeForRedeployment() throws Exception
   {

      executeCLIdeploy((Archive<?>) createWarDeployment(WAR_NAME).createArchive()).assertSuccess();
      CLIResult result = executeAssertedCLICommand("/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_CHANGED + ")");
      result.assertReloadRequired();

      executeAssertedCLICommand("/deployment=" + WAR_NAME + "/:redeploy");


      String wsdl = readUrlToString(createWsdlUrl(NAME, WSDL_PORT));

      assertCorrectWsdlReturned(wsdl);
      assertServiceIsFunctional(createServiceURL(NAME, WSDL_PORT));

      assertUrlIsNotAccessible(createWsdlUrl(NAME, WSDL_PORT_CHANGED));
      assertServiceIsNotAvailable(createServiceURL(NAME, WSDL_PORT_CHANGED));

   }

*/

}
