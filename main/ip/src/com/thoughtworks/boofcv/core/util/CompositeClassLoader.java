/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.boofcv.core.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * ClassLoader that is composed of other classloaders. Each loader will be used to try to load the particular class, until
 * one of them succeeds. <b>Note:</b> The loaders will always be called in the REVERSE order they were added in.
 *
 * <p>The Composite class loader also has registered  the classloader that loaded xstream.jar
 * and (if available) the thread's context classloader.</p>
 *
 * <h1>Example</h1>
 * <pre><code>CompositeClassLoader loader = new CompositeClassLoader();
 * loader.add(MyClass.class.getClassLoader());
 * loader.add(new AnotherClassLoader());
 * &nbsp;
 * loader.loadClass("com.blah.ChickenPlucker");
 * </code></pre>
 *
 * <p>The above code will attempt to load a class from the following classloaders (in order):</p>
 *
 * <ul>
 *   <li>AnotherClassLoader (and all its parents)</li>
 *   <li>The classloader for MyClas (and all its parents)</li>
 *   <li>The thread's context classloader (and all its parents)</li>
 *   <li>The classloader for XStream (and all its parents)</li>
 * </ul>
 * 
 * <p>The added classloaders are kept with weak references to allow an application container to reload classes.</p>
 *
 * @author Joe Walnes
 * @author J&ouml;rg Schaible
 * @since 1.0.3
 */
public class CompositeClassLoader implements XStreamClassLoader {

    private final ReferenceQueue queue = new ReferenceQueue();
    private final List classLoaders = new ArrayList();

    public CompositeClassLoader() {
        addInternal(Object.class.getClassLoader()); // bootstrap loader.
        addInternal(getClass().getClassLoader()); // whichever classloader loaded this jar.
    }

    /**
     * Add a loader to the n
     * @param classLoader
     */
    public synchronized void add(ClassLoader classLoader) {
        cleanup();
        if (classLoader != null) {
            addInternal(classLoader);
        }
    }

    private void addInternal(ClassLoader classLoader) {
        WeakReference refClassLoader = null;
        for (Iterator iterator = classLoaders.iterator(); iterator.hasNext();) {
            WeakReference ref = (WeakReference) iterator.next();
            ClassLoader cl = (ClassLoader)ref.get();
            if (cl == null) {
                iterator.remove();
            } else if (cl == classLoader) {
                iterator.remove();
                refClassLoader = ref;
            }
        }
        classLoaders.add(0, refClassLoader != null ? refClassLoader : new WeakReference(classLoader, queue));
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        List copy = new ArrayList(classLoaders.size()) {

            public boolean addAll(Collection c) {
                boolean result = false;
                for(Iterator iter = c.iterator(); iter.hasNext(); ) {
                    result |= add(iter.next());
                }
                return result;
            }

            public boolean add(Object ref) {
                Object classLoader = ((WeakReference)ref).get();
                if (classLoader != null) {
                    return super.add(classLoader);
                }
                return false;
            }
            
        };
        synchronized(this) {
            cleanup();
            copy.addAll(classLoaders);
        }
        
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        for (Iterator iterator = copy.iterator(); iterator.hasNext();) {
            ClassLoader classLoader = (ClassLoader) iterator.next();
            if (classLoader == contextClassLoader) {
                contextClassLoader = null;
            }
            try {
                return classLoader.loadClass(name);
            } catch (ClassNotFoundException notFound) {
                // ok.. try another one
            }
        }
        
        // One last try - the context class loader associated with the current thread. Often used in j2ee servers.
        // Note: The contextClassLoader cannot be added to the classLoaders list up front as the thread that constructs
        // XStream is potentially different to thread that uses it.
        if (contextClassLoader != null) {
            return contextClassLoader.loadClass(name);
        } else {
            throw new ClassNotFoundException(name);
        }
    }

   public ClassLoader getClassLoader() {
      if( classLoaders.size() > 0 )
         return (ClassLoader)((WeakReference)classLoaders.get(0)).get();
      else
         throw new RuntimeException("No internal ClassLoaders!");
   }

   private void cleanup() {
        WeakReference ref;
        while ((ref = (WeakReference)queue.poll()) != null)
        {
            classLoaders.remove(ref);
        }
    }
}
