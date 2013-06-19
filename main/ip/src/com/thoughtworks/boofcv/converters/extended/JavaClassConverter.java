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
package com.thoughtworks.boofcv.converters.extended;

import com.thoughtworks.boofcv.core.util.XStreamClassLoader;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.core.util.Primitives;

/**
 * Converts a java.lang.Class to XML.
 * 
 * @author Aslak Helles&oslash;y
 * @author Joe Walnes
 * @author Matthew Sandoz
 * @author J&ouml;rg Schaible
 */
public class JavaClassConverter extends AbstractSingleValueConverter {

    private XStreamClassLoader classLoader;

    public JavaClassConverter(XStreamClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public boolean canConvert(Class clazz) {
        return Class.class.equals(clazz); // :)
    }

    public String toString(Object obj) {
        return ((Class) obj).getName();
    }

    public Object fromString(String str) {
        try {
            return loadClass(str);
        } catch (ClassNotFoundException e) {
            throw new ConversionException("Cannot load java class " + str, e);
        }
    }

    private Class loadClass(String className) throws ClassNotFoundException {
        Class resultingClass = Primitives.primitiveType(className);
        if( resultingClass != null ){
            return resultingClass;
        }
        int dimension;
        for(dimension = 0; className.charAt(dimension) == '['; ++dimension);
        if (dimension > 0) {
            final ClassLoader classLoaderToUse;
            if (className.charAt(dimension) == 'L') {
                String componentTypeName = className.substring(dimension + 1, className.length() - 1);
                classLoaderToUse = classLoader.loadClass(componentTypeName).getClassLoader();
            } else {
                classLoaderToUse = null;
            }
            return Class.forName(className, false, classLoaderToUse);
        }
        return classLoader.loadClass(className);
    }
}
