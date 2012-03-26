Embedded Cargo Container for OSGi Framework Launcher
====================================================

There are alternative ways such as custom JUnit runners or suites that could do 
the same thing without Cargo and there even is Pax Exam and Junit4OSGi already
available that implement this approach, but Cargo gives freedom to run the tests 
that contain custom JUnit runners and retains the possibility to use them as 
Eclipse PDE Junit Plugin Tests. Since there is no direct relation to test 
frameworks even TestNG or other test launching mechanisms such as bundle 
fragments containing tests can be used. 

OSGi bundles can already be deployed to various containers such as JOnAS, but 
the startup times and download sizes of these containers are quite high due to 
JavaEE demands. Micro JOnAS deals with these issues, but at the time of writing
this it did not start. 

OSGi frameworks such as Apache Felix can have both API and implementation 
packaged together which would cause API incompatibilities because the container 
itself depends on OSGi API. This is resolved by proxying framework 
implementation against the API used by the container. This approach gives 
freedom to choose both OSGi API and implementation that support Launcher 
Framework.

Usage:

	<plugin>
		<groupId>org.codehaus.cargo</groupId>
		<artifactId>cargo-maven2-plugin</artifactId>
		<version>1.2.0</version>
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
					</dependency>
				</dependencies>
				<deployables>
					<deployable>
						<groupId>org.ops4j.pax.logging</groupId>
						<artifactId>pax-logging-api</artifactId>
					</deployable>
					<deployable>
						<groupId>org.ops4j.pax.logging</groupId>
						<artifactId>pax-logging-service</artifactId>
					</deployable>
					<deployable>
						<groupId>org.apache.felix</groupId>
						<artifactId>org.apache.felix.configadmin</artifactId>
					</deployable>
				</deployables>
			</container>
			<configuration>
				<type>runtime</type>
				<properties>
					<org.osgi.framework.storage>${project.build.directory}/osgi</org.osgi.framework.storage>
					<org.osgi.framework.storage.clean>onFirstInit</org.osgi.framework.storage.clean>
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
				<artifactId>cargo-core-container-osgi</artifactId>
				<version>1.2.0</version>
			</dependency>
		</dependencies>
	</plugin>