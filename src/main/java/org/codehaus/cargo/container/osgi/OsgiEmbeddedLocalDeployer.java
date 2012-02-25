package org.codehaus.cargo.container.osgi;

import java.io.InputStream;
import java.util.Dictionary;

import org.codehaus.cargo.container.EmbeddedLocalContainer;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableException;
import org.codehaus.cargo.container.spi.deployer.AbstractEmbeddedLocalDeployer;
import org.codehaus.cargo.util.FileHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

public class OsgiEmbeddedLocalDeployer extends AbstractEmbeddedLocalDeployer {

	public OsgiEmbeddedLocalDeployer(
			EmbeddedLocalContainer embeddedLocalContainer) {
		super(embeddedLocalContainer);
	}

	@Override
	protected OsgiEmbeddedLocalContainer getContainer() {
		return (OsgiEmbeddedLocalContainer) super.getContainer();
	}

	private BundleContext getBundleContext() {
		OsgiEmbeddedLocalContainer container = this.getContainer();
		Framework framework = container.getFramework();
		BundleContext bundleContext = framework.getBundleContext();
		return bundleContext;
	}

	@Override
	public void deploy(Deployable deployable) {
		String file = deployable.getFile();
		FileHandler fileHandler = this.getFileHandler();
		InputStream inputStream = fileHandler.getInputStream(file);
		try {
			BundleContext bundleContext = this.getBundleContext();
			bundleContext.installBundle(file, inputStream);
		} catch (BundleException e) {
			throw new DeployableException(file, e);
		}
	}

	@Override
	public void start(Deployable deployable) {
		BundleContext bundleContext = this.getBundleContext();
		Bundle[] bundles = bundleContext.getBundles();
		String file = deployable.getFile();
		for (Bundle bundle : bundles) {
			String location = bundle.getLocation();
			if (location.equals(file)) {
				Dictionary<?, ?> headers = bundle.getHeaders();
				String fragmentHost = (String) headers
						.get(Constants.FRAGMENT_HOST);
				if (fragmentHost == null) {
					try {
						bundle.start(Bundle.START_ACTIVATION_POLICY);
					} catch (BundleException e) {
						throw new DeployableException(file, e);
					}
				}
				break;
			}
		}
	}

	@Override
	public void stop(Deployable deployable) {
		BundleContext bundleContext = this.getBundleContext();
		Bundle[] bundles = bundleContext.getBundles();
		String file = deployable.getFile();
		for (Bundle bundle : bundles) {
			String location = bundle.getLocation();
			if (location.equals(file)) {
				try {
					bundle.stop();
				} catch (BundleException e) {
					throw new DeployableException(file, e);
				}
				break;
			}
		}
	}

	@Override
	public void undeploy(Deployable deployable) {
		BundleContext bundleContext = this.getBundleContext();
		Bundle[] bundles = bundleContext.getBundles();
		String file = deployable.getFile();
		for (Bundle bundle : bundles) {
			String location = bundle.getLocation();
			if (location.equals(file)) {
				try {
					bundle.uninstall();
				} catch (BundleException e) {
					throw new DeployableException(file, e);
				}
				break;
			}
		}
	}

	@Override
	public void redeploy(Deployable deployable) {
		BundleContext bundleContext = this.getBundleContext();
		Bundle[] bundles = bundleContext.getBundles();
		String file = deployable.getFile();
		for (Bundle bundle : bundles) {
			String location = bundle.getLocation();
			if (location.equals(file)) {
				FileHandler fileHandler = this.getFileHandler();
				InputStream inputStream = fileHandler.getInputStream(file);
				try {
					bundle.update(inputStream);
				} catch (BundleException e) {
					throw new DeployableException(file, e);
				}
				break;
			}
		}
	}

}
