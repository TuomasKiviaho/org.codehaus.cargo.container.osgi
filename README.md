Embedded Cargo Container for OSGi Framework Launcher
====================================================

OSGi Service Platform Release 4 Version 4.2 introduced a Framework Launcher that 
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
fullblown D-OSGi were considered but this aproach was closest to the spirit of Surefire Booter API.

Usage:
	<!-- osgi.shell.telnet.port can be obtainer using build-helper-maven-plugin -->
	<plugin>
		<groupId>org.codehaus.cargo</groupId>
		<artifactId>cargo-maven2-plugin</artifactId>
		<version>1.2.1</version>
		<executions>
			<execution>
				<id>default-cli</id>
				<goals>
					<goal>run</goal>
				</goals>
			</execution>
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
						<groupId>org.apache.felix</groupId>
						<artifactId>org.apache.felix.gogo.command</artifactId>
						<type>bundle</type>
					</deployable>
				</deployables>
			</container>
			<configuration>
				<type>runtime</type>
				<properties>
					<org.osgi.framework.storage>${project.build.directory}/osgi</org.osgi.framework.storage>
					<org.osgi.framework.storage.clean>onFirstInit</org.osgi.framework.storage.clean>
					<osgi.shell.telnet.port>${osgi.shell.telnet.port}</osgi.shell.telnet.port>
				</properties>
				<deployables>
					<deployable />
					<deployable>
						<classifier>tests</classifier> 
					</deployable>
				</deployables>
			</configuration>
		</configuration>
		<dependencies>
			<dependency>
				<groupId>org.codehaus.cargo</groupId>
				<artifactId>cargo-container-osgi</artifactId>
				<version>1.2.1</version>
			</dependency>
		</dependencies>
	</plugin>

	<!-- aspectjweaver using maven-dependency-plugin -->
	<plugin>
		<artifactId>maven-failsafe-plugin</artifactId>
		<version>2.12</version>
		<executions>
			<execution>
				<goals>
					<goal>integration-test</goal>
					<goal>verify</goal>
				</goals>
				<configuration>
					<systemPropertyVariables>
						<osgi.shell.telnet.port>${osgi.shell.telnet.port}</osgi.shell.telnet.port>
					</systemPropertyVariables>
					<argLine>-javaagent:${project.build.directory}/javaagent/aspectjweaver.jar</argLine>
					<failIfNoTests>true</failIfNoTests>
				</configuration>
			</execution>
		</executions>
	</plugin>