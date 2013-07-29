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

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.ws.BaseDeployment.WarDeployment;
import org.junit.After;
import org.junit.Test;

public final class DeployAfterReloadIT extends CLITestUtils
{

   private static final String NAME = "CLIWebservicesWsdlPortTestCase";
   private static final String WAR_NAME = NAME + WAR_EXTENSTION;


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

   @After
   public void after() throws Exception {
      undeployQuietly(WAR_NAME);
   }

   @Test
   public void testWarDeployUndeployDeploy() throws Exception
   {
      WebArchive war = createWarDeployment(WAR_NAME).createArchive();
      executeAssertedCLIdeploy(war);
      assertSuccessfulCLIResult(undeploy(war.getName()));
      executeAssertedCLIdeploy(war);
      assertSuccessfulCLIResult(undeploy(war.getName()));
   }

   @Test
   public void testWarDeployUndeployDeployWithReload() throws Exception
   {
      WebArchive war = createWarDeployment(WAR_NAME).createArchive();
      executeAssertedCLIdeploy(war);
      assertSuccessfulCLIResult(undeploy(war.getName()));
      restartServer(); //remove when https://bugzilla.redhat.com/show_bug.cgi?id=987904 is resolved
      executeAssertedCLIReload();
      executeAssertedCLIdeploy(war);
      assertSuccessfulCLIResult(undeploy(war.getName()));
   }




}
