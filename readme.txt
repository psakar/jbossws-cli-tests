Running
-------
mvn -s settings.xml clean verify


By default following steps are done (can be switched by switching profiles)
1 download AS to target
2 starts AS
3 run integration tests
4 stop AS

To pass all tests until bug https://bugzilla.redhat.com/show_bug.cgi?id=987904 is resolved run
mvn -s settings.xml clean verify -DBZ987904

Run single test
mvn -s settings.xml clean verify -Dit.test=DeployAfterReloadIT


Endorsed libraries
------------------
project is using endorsed library (see http://docs.oracle.com/javase/7/docs/technotes/guides/standards/)
jbossws-cxf-factories-${jbossws-cxf.version}.jar installed by maven by default in directory target/endorsed (see profile endorsed-libs)


Maven settings
--------------
unzip AS repo and add to maven settings (eg. profile jboss-eap-local-maven-repository)

example:

<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
	      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	      xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <localRepository>/home/maven/repository/jbossws-cli-tests.JBEAP-6.1.1-ER3</localRepository>
  <profiles>
		<profile>
		  <id>jboss-eap-local-maven-repository</id>
		  <repositories>
		    <repository>
		      <id>jboss-eap-local</id>
		      <url>file:///home/maven/repository/jboss-eap-6.1.1-ER3</url>
		      <releases>
		        <enabled>true</enabled>
		      </releases>
		      <snapshots>
		        <enabled>false</enabled>
		      </snapshots>
		    </repository>
		  </repositories>
		  <pluginRepositories>
		    <pluginRepository>
		      <id>jboss-eap-maven-plugin-repository</id>
		      <url>file:///home/maven/repository/jboss-eap-6.1.1-ER3</url>
		      <releases>
		        <enabled>true</enabled>
		      </releases>
		      <snapshots>
		        <enabled>false</enabled>
		      </snapshots>
		    </pluginRepository>
		  </pluginRepositories>
		</profile>



		<profile>
		  <id>redhat-brew-repository</id>
		  <repositories>
		    <repository>
		        <id>redhat-brew</id>
		        <name>Red Hat Brew Repository</name>
		        <layout>default</layout>
		        <url>http://download.devel.redhat.com/brewroot/repos/jb-eap-6-rhel-6-build/latest/maven/</url>
		        <snapshots>
		            <enabled>true</enabled>
		        </snapshots>
		    </repository>
		  </repositories>
		  <pluginRepositories>
		    <pluginRepository>
		        <id>redhat-brew</id>
		        <name>Red Hat Brew Repository</name>
		        <layout>default</layout>
		        <url>http://download.devel.redhat.com/brewroot/repos/jb-eap-6-rhel-6-build/latest/maven/</url>
		        <snapshots>
		            <enabled>true</enabled>
		        </snapshots>
		    </pluginRepository>
		  </pluginRepositories>
		</profile>
		<profile>
		  <id>jboss-nexus-repository</id>
		  <repositories>
		    <repository>
		        <id>jboss-nexus-public</id>
		        <name>JBoss Nexus Public Repository</name>
		        <layout>default</layout>
		        <url>http://repository.jboss.org/nexus/content/groups/public/</url>
		        <snapshots>
		            <enabled>true</enabled>
		        </snapshots>
		    </repository>
		  </repositories>
		  <pluginRepositories>
		    <pluginRepository>
		        <id>jboss-nexus-public</id>
		        <name>JBoss Nexus Public Repository</name>
		        <layout>default</layout>
		        <url>http://repository.jboss.org/nexus/content/groups/public/</url>
		        <snapshots>
		            <enabled>true</enabled>
		        </snapshots>
		    </pluginRepository>
		  </pluginRepositories>
		</profile>
  </profiles>

  <activeProfiles>
		<activeProfile>redhat-brew-repository</activeProfile>
		<activeProfile>jboss-eap-local-maven-repository</activeProfile>
		<activeProfile>jboss-nexus-repository</activeProfile>
  </activeProfiles>

</settings>
