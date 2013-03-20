Embedded Cargo Container for OSGi Framework Launcher
====================================================

OSGi Service Platform Release 4 Version 4.3 introduced a Framework Launcher that 
allows OSGi framework to be started and stopped via common API which this implementation
of Codehaus Cargo Embedded Container is build around.

There is also a basic deployer present that enables starting stopping of individual bundles 
and provides installation and uninstallation procedures via Cargo API.

Codehaus Cargo Container API uses a separate classloader for embedded container so that 
Maven Plugin or Ant Tasks would not be present for the servers that containers lauch and
in the case of OSGi this feature is more than welcome. On the other hand separatation of 
classloaders between container and containee bring up class incompatibilities when container
has it's own instances of OSGi framework API which it uses to access the embedded OSGi 
framework. This problem is circumvented by using stub/skeleton pattern via reflection Proxy 
provided by combination of JavaAssist and Objenesis and it is not visible to the users 
in any way. The implementation is available also separately and has no bindings whatsoever 
to either OSGi or Cargo.

Test cases can also be bundled up and executed by a separate Java Agent for Maven Surefire/Failsafe. 
There are alternative ways such as custom JUnit runners or suites that could do the same 
thing without Cargo and there even is Pax Exam and Junit4OSGi already available that implement 
this approach but Cargo gives freedom to run the tests that contain custom JUnit runners - such
as suites - and retains the possibility to use them as Eclipse PDE Junit Plugin Tests. TestNG
can also be used since there is no direct relation to test frameworks. A Java Agent approach was
chosen due to the restrictions of Maven Surefire Booter API and the fact that this is not something
that could utilize the Provider API. Idea is to grab control of the forking of the process and 
instead run the tests inside OSGi framework. This is done using both Apache Felix Gogo and Remote 
Shell that provide a lightweight remoting via CLI over telnet. JMX and other mechanisms such as
full blown D-OSGi were considered but this approach was closest to the spirit of Surefire Booter API.

Usage:

	<plugin>
		<groupId>org.codehaus.mojo</groupId>
		<artifactId>build-helper-maven-plugin</artifactId>
		<version>${build-helper-version}</version>
		<executions>
			<execution>
				<phase>pre-integration-test</phase>
				<goals>
					<goal>reserve-network-port</goal>
				</goals>
				<configuration>
					<portNames>
						<portName>osgi.shell.telnet.port</portName>
					</portNames>
				</configuration>
			</execution>
		</executions>
	</plugin>

	<plugin>
		<groupId>org.codehaus.cargo</groupId>
		<artifactId>cargo-maven2-plugin</artifactId>
		<version>${cargo-version}</version>
		<executions>
			<execution>
				<id>pre-integration-test</id>
				<phase>pre-integration-test</phase>
				<goals>
					<goal>start</goal>
					<goal>deploy</goal>
					<goal>deployer-start</goal>
				</goals>
			</execution>
			<execution>
				<id>post-integration-test</id>
				<phase>post-integration-test</phase>
				<goals>
					<goal>deployer-stop</goal>
					<goal>undeploy</goal>
					<goal>stop</goal>
				</goals>
			</execution>
		</executions>
		<configuration>
			<container>
				<containerId>osgi</containerId>
				<type>embedded</type>
				<timeout>0</timeout>
				<dependencies>
					<dependency>
						<groupId>org.apache.felix</groupId>
						<artifactId>org.apache.felix.framework</artifactId>
						<type>bundle</type>
					</dependency>
				</dependencies>
			</container>
			<configuration>
				<properties>
					<org.osgi.framework.storage>${project.build.directory}/felix-cache</org.osgi.framework.storage>
					<org.osgi.framework.storage.clean>onFirstInit</org.osgi.framework.storage.clean>
					<osgi.shell.telnet.port>${osgi.shell.telnet.port}</osgi.shell.telnet.port>
				</properties>
			</configuration>
			<deployables>
				<dependency>
					<groupId>org.apache.felix</groupId>
					<artifactId>org.apache.felix.shell</artifactId>
					<type>bundle</type>
				</dependency>
				<dependency>
					<groupId>org.apache.felix</groupId>
					<artifactId>org.apache.felix.shell.remote</artifactId>
					<type>bundle</type>
				</dependency>
				<deployable>
					<groupId>org.apache.felix</groupId>
					<artifactId>org.apache.felix.gogo.runtime</artifactId>
					<type>bundle</type>
				</deployable>
				<deployable>
					<groupId>org.apache.felix</groupId>
					<artifactId>org.apache.felix.gogo.shell</artifactId>
					<type>bundle</type>
				</deployable>
				<deployable>
					<groupId>org.codehaus.cargo</groupId>
					<artifactId>cargo-container-osgi</artifactId>
					<type>bundle</type>
				</deployable>
				<deployable />
				<deployable>
					<classifier>tests</classifier> 
				</deployable>
			</deployables>
		</configuration>
		<dependencies>
			<dependency>
				<groupId>org.codehaus.cargo</groupId>
				<artifactId>cargo-container-osgi</artifactId>
				<version>${cargo-version}</version>
			</dependency>
		</dependencies>
	</plugin>

	<plugin>
		<artifactId>maven-dependency-plugin</artifactId>
		<version>${dependency-version}</version>
		<executions>
			<execution>
				<phase>pre-integration-test</phase>
				<goals>
					<goal>copy</goal>
				</goals>
				<configuration>
					<stripVersion>true</stripVersion>
					<outputDirectory>${project.build.directory}/javaagent</outputDirectory>
					<artifactItems>
						<artifactItem>
							<groupId>org.aspectj</groupId>
							<artifactId>aspectjweaver</artifactId>
							<version>${aspectj-version}</version>
						</artifactItem>
					</artifactItems>
				</configuration>
			</execution>
		</executions>
	</plugin>
			
	<plugin>
		<artifactId>maven-failsafe-plugin</artifactId>
		<version>${failsafe-version}</version>
		<executions>
			<execution>
				<goals>
					<goal>integration-test</goal>
					<goal>verify</goal>
				</goals>
				<configuration>
					properties>
						<property>
							<name>org.apache.maven.surefire.osgi.port</name>
							<value>${osgi.shell.telnet.port}</value>
						</property>
					</properties>
					<argLine>-javaagent:${project.build.directory}/javaagent/aspectjweaver.jar</argLine>
				</configuration>
			</execution>
		</executions>
	</plugin>