package org.jboss.test.ws;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public abstract class BaseDeployment<T extends org.jboss.shrinkwrap.api.Archive<T>>
{

   protected T archive;

   protected String testResourcesPath = "src/test/resources/";

   public BaseDeployment(Class<T> clazz)
   {
      archive = ShrinkWrap.create(clazz);
   }

   public BaseDeployment(Class<T> clazz, String name)
   {
      archive = ShrinkWrap.create(clazz, name);
   }

   public T createArchive()
   {
      return archive;
   }

   public T writeToFile(File file)
   {
      archive.as(ZipExporter.class).exportTo(file, true);
      return archive;
   }

   public String getName()
   {
      return archive.getName();
   }

   public T getArchive()
   {
      return archive;
   }

   public static abstract class WarDeployment extends BaseDeployment<WebArchive>
   {
      public WarDeployment(String name)
      {
         super(WebArchive.class, name);
      }
      public WarDeployment()
      {
         super(WebArchive.class);
      }
   }

   public static abstract class JarDeployment extends BaseDeployment<JavaArchive>
   {
      public JarDeployment()
      {
         super(JavaArchive.class);
      }
   }
}
