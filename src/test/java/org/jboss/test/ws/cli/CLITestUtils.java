package org.jboss.test.ws.cli;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

public class CLITestUtils
{

   public static final String WAR_EXTENSTION = ".war";
   public static final String JAR_EXTENSTION = ".jar";
   public static final String EAR_EXTENSTION = ".ear";

   public static void assertServiceIsNotAvailable(String serviceURL) throws MalformedURLException
   {
      QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "AnnotatedSecurityService");
      URL wsdlURL = new URL(serviceURL + "?wsdl");
      try {
         Service service = Service.create(wsdlURL, serviceName);
         AnnotatedServiceIface proxy = service.getPort(AnnotatedServiceIface.class);
         proxy.sayHello();
         throw new IllegalStateException("Service " + serviceURL + " should not be accessible");
      } catch (WebServiceException e) {
         //expected
      }
   }

   public static void assertServiceIsFunctional(String serviceURL) throws MalformedURLException
   {
      QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "AnnotatedSecurityService");
      URL wsdlURL = new URL(serviceURL + "?wsdl");
      Service service = Service.create(wsdlURL, serviceName);
      AnnotatedServiceIface proxy = service.getPort(AnnotatedServiceIface.class);
      assertEquals(AnnotatedServiceImpl.HELLO_WORLD, proxy.sayHello());
   }

   public static String executeAssertedCLICommand(String command) throws IOException, CommandLineException {
      String result = executeCLICommand(command);
      assertSuccessfulCLIResult(result);
      return result;
   }

   public static String executeCLICommand(String command) throws IOException, CommandLineException {
           // Initialize the CLI context
           final CommandContext ctx;
           try {
               ctx = CommandContextFactory.getInstance().newCommandContext();
           } catch(CliInitializationException e) {
               throw new IllegalStateException("Failed to initialize CLI context", e);
           }

           try {
               // connect to the server controller
              ctx.connectController();
   //            ctx.connectController("http-remoting", "localhost", 9990); //TestSuiteEnvironment.getServerPort());
   //           ctx.connectController("localhost", 9990); //TestSuiteEnvironment.getServerPort());
              //ctx.connectController("http", "localhost", 9990); //TestSuiteEnvironment.getServerPort());


               ModelNode request = ctx.buildRequest(command);
               ModelControllerClient client = ctx.getModelControllerClient();
               ModelNode result = client.execute(request);
               return result.asString();
           } finally {
               ctx.terminateSession();
           }
      }

   public static void assertUrlIsNotAccessible(URL url)
   {
      InputStream stream = null;
      try {
         stream  = url.openStream();
         throw new IllegalStateException("Url " + url.toString() + " should not be accessible");
      }
      catch (IOException e)
      {
         //expected
      } finally {
         IOUtils.closeQuietly(stream);
      }
   }

   public static String readUrlToString(URL url) throws UnsupportedEncodingException, IOException
   {
      InputStreamReader inputStream = new InputStreamReader(url.openStream(), "UTF-8");
      String wsdl = IOUtils.toString(inputStream);
      IOUtils.closeQuietly(inputStream);
      return wsdl;
   }

   public static void assertSuccessfulCLIResult(String result)
   {
      assertTrue("Unexpected result " + result, result.contains("\"outcome\" => \"success\""));
   }

   public static String executeAssertedCLIdeploy(Archive<?> archive) throws IOException, CommandLineException {
      String result = executeCLIdeploy(archive);
      assertSuccessfulCLIResult(result);
      return result;
   }

   public static String executeCLIdeploy(Archive<?> archive) throws IOException, CommandLineException
   {
      String archiveName = archive.getName();
      assertArchiveNameContainsExtension(archiveName);
      File file = new File(FileUtils.getTempDirectory(), archiveName);
      archive.as(ZipExporter.class).exportTo(file, true);
      return executeCLICommand("deploy " + file.getAbsolutePath());
   }

   private static void assertArchiveNameContainsExtension(String archiveName)
   {
      String extension = "." + FilenameUtils.getExtension(archiveName);
      if (!(WAR_EXTENSTION.equals(extension) || JAR_EXTENSTION.equals(extension) || EAR_EXTENSTION.equals(extension)))
         throw new IllegalArgumentException("Archive " + archiveName + " extension have to be either " + JAR_EXTENSTION + " or " + WAR_EXTENSTION + " or " + EAR_EXTENSTION);

   }

   public static String executeCLICommandQuietly(String command) throws IOException, CommandLineException
   {
      try {
         return executeCLICommand(command);
      } catch (Exception e) {
         // ignore
         // FIXME debug log
      }
      return null;
   }

   public static String undeploy(String deploymentName) throws IOException, CommandLineException {
      return executeCLICommand("undeploy " + deploymentName);
   }

   public static String undeployQuietly(String deploymentName)
   {
      try {
         return undeploy(deploymentName);
      } catch (Exception e) {
         // ignore
         // FIXME debug log
      }
      return null;
   }

   public static void assertCLIOperationRequiesReload(String result)
   {
      assertTrue(result.contains("\"operation-requires-reload\" => true"));
   }

   public static void assertCLIResultIsReloadRequired(String result)
   {
      assertTrue(result.contains("\"process-state\" => \"reload-required\""));
   }

}
