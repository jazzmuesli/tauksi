package org.pavelreich.saaremaa.codecov;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * from https://stackoverflow.com/questions/13315358/custom-java-classloader-not-being-used-to-load-dependencies
 * @author preich
 *
 */
public class MyClassLoader
  extends ClassLoader {
  MyClassLoaderListener listener;
	static final Logger LOG = LoggerFactory.getLogger(MyClassLoader.class);

  MyClassLoader(ClassLoader parent, MyClassLoaderListener listener) {
    super(parent);
    this.listener = listener;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException {
    // respect the java.* packages.
	  //LOG.info("loadClass: " + name);
    if( name.startsWith("java.") || name.startsWith("org.junit")) {
      return super.loadClass(name, resolve);
    }
    else {
      // see if we have already loaded the class.
      Class<?> c = findLoadedClass(name);
      if( c != null ) return c;

      // the class is not loaded yet.  Since the parent class loader has all of the
      // definitions that we need, we can use it as our source for classes.
      InputStream in = null;
      try {
        // get the input stream, throwing ClassNotFound if there is no resource.
        in = getParent().getResourceAsStream(name.replaceAll("\\.", "/")+".class");
        if( in == null ) {
        	throw new ClassNotFoundException("Could not find "+name);
        }

        // read all of the bytes and define the class.
        byte[] cBytes = toByteArray(in);
        c = defineClass(name, cBytes, 0, cBytes.length);
        if( resolve ) resolveClass(c);
        if( listener != null ) listener.classLoaded(c);
        return c;
      } catch (IOException e) {
        throw new ClassNotFoundException("Could not load "+name, e);
      }
      finally {
        closeQuietly(in);
      }
    }
  }
}