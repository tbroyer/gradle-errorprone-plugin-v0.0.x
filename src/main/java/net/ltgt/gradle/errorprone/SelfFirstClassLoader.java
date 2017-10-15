package net.ltgt.gradle.errorprone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

class SelfFirstClassLoader extends URLClassLoader {

  SelfFirstClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  <T> Class<T> defineClass(Class<T> orig) throws IOException {
    try (InputStream is =
        orig.getClassLoader().getResourceAsStream(orig.getName().replace('.', '/') + ".class")) {
      byte[] buffer = new byte[4096];
      ByteArrayOutputStream baos = new ByteArrayOutputStream(buffer.length);
      while (true) {
        int read = is.read(buffer);
        if (read < 0) {
          break;
        }
        baos.write(buffer, 0, read);
      }
      @SuppressWarnings("unchecked")
      Class<T> clazz = (Class<T>) defineClass(orig.getName(), baos.toByteArray(), 0, baos.size());
      resolveClass(clazz);
      return clazz;
    }
  }

  Class<?> defineClassIfExists(String name) throws IOException {
    Class<?> orig;
    try {
      orig = this.getClass().getClassLoader().loadClass(name);
    } catch (ClassNotFoundException e) {
      return null;
    }
    return defineClass(orig);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return loadClass(name, false);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      Class<?> cls = findLoadedClass(name);
      if (cls == null) {
        try {
          cls = findClass(name);
        } catch (ClassNotFoundException cnfe) {
          // ignore, fallback to parent classloader
        }
        if (cls == null) {
          cls = getParent().loadClass(name);
        }
      }
      if (resolve) {
        resolveClass(cls);
      }
      return cls;
    }
  }

  @Override
  public URL getResource(String name) {
    URL resource = findResource(name);
    if (resource == null) {
      getParent().getResource(name);
    }
    return resource;
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration<URL> selfResources = findResources(name);
    Enumeration<URL> parentResources = getParent().getResources(name);
    if (!selfResources.hasMoreElements()) {
      return parentResources;
    }
    if (!parentResources.hasMoreElements()) {
      return selfResources;
    }
    ArrayList<URL> resources = Collections.list(selfResources);
    resources.addAll(Collections.list(parentResources));
    return Collections.enumeration(resources);
  }

  // XXX: we know URLClassLoader#getResourceAsStream calls getResource, so we don't have to
  // override it here.
}
