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

/**
 * ClassLoader that refers to another ClassLoader, allowing a single instance to be passed around the codebase that
 * can later have its destination changed.
 *
 * @author Joe Walnes
 * @author J&ouml;rg Schaible
 * @since 1.1.1
 */
public class ClassLoaderReference implements XStreamClassLoader {

    private transient XStreamClassLoader reference;

    public ClassLoaderReference(XStreamClassLoader reference) {
        this.reference = reference;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return reference.loadClass(name);
    }

   public ClassLoader getClassLoader() {
      return reference.getClassLoader();
   }

   public XStreamClassLoader getReference() {
        return reference;
    }

    public void setReference(XStreamClassLoader reference) {
        this.reference = reference;
    }
    
    private Object writeReplace() {
        return new Replacement();
    }
    
    static class Replacement {
        
        private Object readResolve() {
            return new ClassLoaderReference(new CompositeClassLoader());
        }
        
    }
}
