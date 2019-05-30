package org.pavelreich.saaremaa.codecov;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorClassLoading implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(MonitorClassLoading.class);
	public static void main(final String[] args) throws Exception {
		MyClassLoaderListener x = new MyClassLoaderListener() {
			
			@Override
			public void classLoaded(Class<?> c) {
				LOG.info("loaded:" + c);
			}
		}; 
		MyClassLoader cl = new MyClassLoader(Thread.currentThread().getContextClassLoader(), x);
		Thread.currentThread().setContextClassLoader(cl);
		Thread thread = new Thread((Runnable) cl.loadClass(MonitorClassLoading.class.getCanonicalName()).newInstance());
		thread.start();
		thread.join();
	}
	public void run() {
		LOG.info("args: " + Arrays.asList("x"));
		new ArrayList();
		Calculadora calculator = new Calculadora();
		LOG.info("calc.cl:" + calculator.getClass().getClassLoader());
	}
}
