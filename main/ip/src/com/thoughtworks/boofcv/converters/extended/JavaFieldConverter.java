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
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.lang.reflect.Field;

/**
 * Converts a java.lang.reflect.Field to XML.
 * 
 * @author J&ouml;rg Schaible
 */
public class JavaFieldConverter implements Converter {

    private final SingleValueConverter javaClassConverter;

    public JavaFieldConverter(XStreamClassLoader classLoader) {
        this.javaClassConverter = new JavaClassConverter(classLoader);
    }

    public boolean canConvert(Class type) {
        return type.equals(Field.class);
    }

    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Field field = (Field) source;

        writer.startNode("name");
        writer.setValue(field.getName());
        writer.endNode();

        writer.startNode("clazz");
        writer.setValue(javaClassConverter.toString(field.getDeclaringClass()));
        writer.endNode();
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String methodName = null;
        String declaringClassName = null;
        
        while((methodName == null || declaringClassName == null) && reader.hasMoreChildren()) {
            reader.moveDown();
            
            if (reader.getNodeName().equals("name")) {
                methodName = reader.getValue();
            } else if (reader.getNodeName().equals("clazz")) {
                declaringClassName = reader.getValue();
            }
            reader.moveUp();
        }
        
        Class declaringClass = (Class)javaClassConverter.fromString(declaringClassName);
        try {
            return declaringClass.getDeclaredField(methodName);
        } catch (NoSuchFieldException e) {
            throw new ConversionException(e);
        }
    }
}
