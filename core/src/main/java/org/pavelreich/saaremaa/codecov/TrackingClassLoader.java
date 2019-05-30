package org.pavelreich.saaremaa.codecov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TrackingClassLoader extends ClassLoader {
	private static final Logger LOG = LoggerFactory.getLogger(TrackingClassLoader.class);

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		LOG.info("loadClass: " + name);
		return super.loadClass(name, resolve);
	}
	

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		LOG.info("findClass: " + name);
		return super.findClass(name);
	}
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		LOG.info("loadClass: " + name);
		// TODO Auto-generated method stub
		return super.loadClass(name);
	}
}