package org.apache.maven.surefire.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.surefire.booter.BooterDeserializer;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ForkedBooter;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SystemPropertyManager;
import org.apache.maven.surefire.booter.TypeEncodedValue;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class Booter extends ForkedBooter
{

    public Booter()
    {
        super();
    }

    public static void _main(String[] args) throws Throwable
    {
        System.out.print(Booter.class.getPackage().getName());
        System.out.print(':');
        System.out.print(Booter.class.getSimpleName());
        for (int i = 1; i < args.length; i++)
        {
            System.out.print(' ');
            System.out.print('\'');
            System.out.print(args[i].replace("'", "'\\''"));
            System.out.print('\'');
        }
        System.out.println();
        try
        {
            if (args.length > 2)
            {
                SystemPropertyManager.setSystemProperties(new File(args[2]));
            }
            File surefirePropertiesFile = new File(args[1]);
            InputStream stream =
                surefirePropertiesFile.exists() ? new FileInputStream(surefirePropertiesFile)
                    : null;
            BooterDeserializer booterDeserializer = new BooterDeserializer(stream);
            ProviderConfiguration providerConfiguration = booterDeserializer.deserialize();
            StartupConfiguration startupConfiguration =
                booterDeserializer.getProviderConfiguration();
            ClasspathConfiguration classpathConfiguration =
                startupConfiguration.getClasspathConfiguration();
            ClassLoader testClassLoader = classpathConfiguration.createTestClassLoader();
            Set<BundleWiring> bundleWirings = Collections.emptySet();
            {
                String symbolicName;
                Version version;
                URL resource = testClassLoader.getResource(JarFile.MANIFEST_NAME);
                InputStream inputStream = resource.openStream();
                try
                {
                    Manifest manifest = new Manifest(inputStream);
                    Attributes mainAttributes = manifest.getMainAttributes();
                    symbolicName = mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
                    String bundleVersion = mainAttributes.getValue(Constants.BUNDLE_VERSION);
                    version = new Version(bundleVersion);
                }
                finally
                {
                    inputStream.close();
                }
                Bundle bundle = FrameworkUtil.getBundle(Booter.class);
                BundleContext bundleContext = bundle.getBundleContext();
                Bundle[] candidateBundles = bundleContext.getBundles();
                for (Bundle candidateBundle : candidateBundles)
                {
                    String bundleSymbolicName = candidateBundle.getSymbolicName();
                    Version bundleVersion = candidateBundle.getVersion();
                    if (bundleSymbolicName.equals(symbolicName) && bundleVersion.equals(version))
                    {
                        Dictionary<String, String> headers = candidateBundle.getHeaders();
                        String fragmentHost = headers.get(Constants.FRAGMENT_HOST);
                        if (fragmentHost == null)
                        {
                            BundleWiring bundleWiring = candidateBundle.adapt(BundleWiring.class);
                            bundleWirings = Collections.singleton(bundleWiring);
                        }
                        else
                        {
                            BundleRevisions bundleRevisions =
                                candidateBundle.adapt(BundleRevisions.class);
                            if (bundleRevisions != null)
                            {
                                List<BundleRevision> candidateBundleRevisions =
                                    bundleRevisions.getRevisions();
                                bundleWirings =
                                    new HashSet<BundleWiring>(candidateBundleRevisions.size());
                                for (BundleRevision candidateBundleRevision : candidateBundleRevisions)
                                {
                                    BundleWiring candidateBundleWiring =
                                        candidateBundleRevision.getWiring();
                                    if (candidateBundleWiring != null)
                                    {
                                        List<BundleWire> candidateBundleWires =
                                            candidateBundleWiring.getRequiredWires(null);
                                        if (candidateBundleWires != null)
                                        {
                                            for (BundleWire candidateBundleWire : candidateBundleWires)
                                            {
                                                BundleWiring bundleWiring =
                                                    candidateBundleWire.getProviderWiring();
                                                bundleWirings.add(bundleWiring);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
            for (BundleWiring bundleWiring : bundleWirings)
            {
                ClassLoader classLoader = bundleWiring.getClassLoader();
                TypeEncodedValue forkedTestSet = providerConfiguration.getTestForFork();
                Object testSet =
                    forkedTestSet != null ? forkedTestSet.getDecodedValue(classLoader) : null;
                runSuitesInProcess(testSet, classLoader, startupConfiguration,
                    providerConfiguration);
            }
        }
        finally
        {
            System.out.println("Z,0,BYE!");
        }
    }

}
