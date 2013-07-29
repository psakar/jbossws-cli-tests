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
   private final int shutdownWaitMillis;
   private final int reloadWaitMillis;
   private final int startupWaitMillis;

   public CLITestUtils()
   {
      shutdownWaitMillis = 4000;
      reloadWaitMillis = 3000;
      startupWaitMillis = 4000;
   }

   public void assertServiceIsNotAvailable(String serviceURL) throws MalformedURLException
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

   public void assertServiceIsFunctional(String serviceURL) throws MalformedURLException
   {
      QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "AnnotatedSecurityService");
      URL wsdlURL = new URL(serviceURL + "?wsdl");
      Service service = Service.create(wsdlURL, serviceName);
      AnnotatedServiceIface proxy = service.getPort(AnnotatedServiceIface.class);
      assertEquals(AnnotatedServiceImpl.HELLO_WORLD, proxy.sayHello());
   }

   public String executeAssertedCLICommand(String command) throws IOException, CommandLineException {
      return assertSuccessfulCLIResult(executeCLICommand(command));
   }

   public void restartServer() throws Exception {
      shutdownServer();
      startServer();
   }

   public void startServer() throws Exception
   {
      info("Start server");
      String startServerCommand = System.getProperty("jboss.start");
      if (startServerCommand == null) {
         String jbossHome = System.getenv("jboss.home");
         if (jbossHome == null)
            jbossHome = System.getProperty("jboss.home");
         if (jbossHome != null) {
            String extension = ".sh";
            startServerCommand = "/bin/sh -c " + FilenameUtils.normalizeNoEndSeparator(jbossHome) + File.separator + "bin" + File.separator + "standalone" + extension;
         }
      }
      if (startServerCommand == null)
         throw new IllegalStateException("Specify either java property jboss.start or jboss.home");
      info("Start server using command " + startServerCommand);
      Runtime.getRuntime().exec(startServerCommand);
      sleep(startupWaitMillis, "Start server");
   }

   public void shutdownServer() throws Exception
   {
      info("Shutdown server");
      executeCLICommandQuietly("shutdown");
      sleep(shutdownWaitMillis, "Shutdown server");
   }

   public void info(String message)
   {
      System.err.println(message);
   }

   public String executeCLICommand(String command) throws IOException, CommandLineException
   {
      // Initialize the CLI context
      final CommandContext ctx;
      try
      {
         ctx = CommandContextFactory.getInstance().newCommandContext();
      }
      catch (CliInitializationException e)
      {
         throw new IllegalStateException("Failed to initialize CLI context", e);
      }

      try
      {
         // connect to the server controller
         ctx.connectController();
         //            ctx.connectController("http-remoting", "localhost", 9990); //TestSuiteEnvironment.getServerPort());
         //           ctx.connectController("localhost", 9990); //TestSuiteEnvironment.getServerPort());
         //ctx.connectController("http", "localhost", 9990); //TestSuiteEnvironment.getServerPort());

         info("Execute CLI command " + command);

         ModelNode request = ctx.buildRequest(command);
         ModelControllerClient client = ctx.getModelControllerClient();
         ModelNode result = client.execute(request);
         return result.asString();
      }
      finally
      {
         ctx.terminateSession();
      }
   }

   public void assertUrlIsNotAccessible(URL url)
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

   public String readUrlToString(URL url) throws UnsupportedEncodingException, IOException
   {
      InputStreamReader inputStream = new InputStreamReader(url.openStream(), "UTF-8");
      String wsdl = IOUtils.toString(inputStream);
      IOUtils.closeQuietly(inputStream);
      return wsdl;
   }

   public String assertSuccessfulCLIResult(String result)
   {
      assertTrue("Unexpected result " + result, result.contains("\"outcome\" => \"success\""));
      return result;
   }

   public String executeAssertedCLIdeploy(Archive<?> archive) throws IOException, CommandLineException {
      return assertSuccessfulCLIResult(executeCLIdeploy(archive));
   }

   public String executeCLIdeploy(Archive<?> archive) throws IOException, CommandLineException
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

   public String executeCLICommandQuietly(String command) throws IOException, CommandLineException
   {
      try {
         return executeCLICommand(command);
      } catch (Exception e) {
         // ignore
         // FIXME debug log
      }
      return null;
   }

   public String undeploy(String deploymentName) throws IOException, CommandLineException {
      return executeCLICommand("undeploy " + deploymentName);
   }

   public String undeployQuietly(String deploymentName)
   {
      try {
         return undeploy(deploymentName);
      } catch (Exception e) {
         // ignore
         // FIXME debug log
      }
      return null;
   }

   public void assertCLIOperationRequiesReload(String result)
   {
      assertTrue(result.contains("\"operation-requires-reload\" => true"));
   }

   public void assertCLIResultIsReloadRequired(String result)
   {
      assertTrue(result.contains("\"process-state\" => \"reload-required\""));
   }

   public void executeAssertedCLIReload() throws Exception
   {
      info("CLI Reload");
      executeAssertedCLICommand("reload"); //FIXME find reliable way to find out server is reloaded //https://community.jboss.org/message/827388
      //https://issues.jboss.org/browse/AS7-3561
      sleep(reloadWaitMillis, "CLI Reload");
   }

   public void sleep(long millis, String name) throws InterruptedException
   {
      info("Waiting " + millis + " ms for " + name);
      Thread.sleep(reloadWaitMillis);
   }

}
