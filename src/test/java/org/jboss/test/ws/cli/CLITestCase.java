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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.jboss.as.cli.CommandLineException;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
/**
 *
 * see https://docspace.corp.redhat.com/docs/DOC-152480
 *
 */
public abstract class CLITestCase extends CLITestUtils
{

   private final WebArchive war;
   private final WebArchive anotherWar;

   private final String verifyConfigurationCommand;
   private final String changeConfigurationCommand;
   private final String resetConfigurationCommand;

   public CLITestCase(String verifyConfigurationCommand, String changeConfigurationCommand, String resetConfigurationCommand, WebArchive war, WebArchive anotherWar)
   {
      this.verifyConfigurationCommand = verifyConfigurationCommand;
      this.changeConfigurationCommand = changeConfigurationCommand;
      this.resetConfigurationCommand = resetConfigurationCommand;
      this.war = war;
      this.anotherWar = anotherWar;
   }

   @After
   public void after() throws Exception {
      info("After");
      undeployQuietly(war.getName());
      undeployQuietly(anotherWar.getName());
      resetConfiguration();
      reloadServer();
   }

   protected void resetConfiguration() throws IOException, CommandLineException
   {
      executeCLICommandQuietly(resetConfigurationCommand);
   }

   @Test
   public void testDefaultConfiguration() throws Exception
   {
      deployWar(war);

      assertDefaultConfigurationValue(executeCLICommand(verifyConfigurationCommand).assertSuccess());

      assertOriginalConfiguration(getContextName(war));
   }


   protected String getContextName(WebArchive war)
   {
      return war.getName().replace(".war", "");
   }


   protected abstract void assertDefaultConfigurationValue(CLIResult result);


   private void deployWar(WebArchive war) throws IOException, CommandLineException
   {
      executeCLIdeploy(war).assertSuccess();
   }

   protected abstract void assertOriginalConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException;


   @Test//1BA
   public void testChangeAffectsNewDeploymentsWithoutReload() throws Exception
   {
      changeConfiguration();

      deployWar(war);

      assertNewConfiguration(getContextName(war));
   }

   protected abstract void assertNewConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException;

   private void changeConfiguration() throws IOException, CommandLineException
   {
      assertChangeConfigurationResult(executeCLICommand(changeConfigurationCommand).assertSuccess());
      assertChangedConfigurationValue(executeCLICommand(verifyConfigurationCommand).assertSuccess());
   }


   protected abstract void assertChangedConfigurationValue(CLIResult result);

   protected abstract void assertChangeConfigurationResult(CLIResult assertSuccess);

   @Test//2BA
   public void testChangeFollowedByReloadAffectsNewDeployments() throws Exception
   {
      changeConfiguration();

      reloadServer();

      deployWar(war);

      assertNewConfiguration(getContextName(war));
   }

   @Test//3BA
   public void testChangeDoesNotAffectExistingDeploymentsBeforeReload() throws Exception
   {
      deployWar(war);

      changeConfiguration();

      assertOriginalConfiguration(getContextName(war));

   }



   @Test//4BA
   public void testChangeAffectsExistingDeploymentsAfterReload() throws Exception
   {
      deployWar(war);

      changeConfiguration();

      reloadServer();

      assertNewConfiguration(getContextName(war));
   }



   @Test//5BA
   public void testChangeAffectsNewDeploymentsBeforeReloadAndExistingDeploymentsAfterReload() throws Exception
   {
      deployWar(war);

      changeConfiguration();

      deployWar(anotherWar);

      assertOriginalConfiguration(getContextName(war));
      assertNewConfiguration(getContextName(anotherWar));

      reloadServer();

      assertNewConfiguration(getContextName(war));
      assertNewConfiguration(getContextName(anotherWar));
   }
}
