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
package com.thoughtworks.boofcv.converters.reflection;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * An abstract converter implementation for constants of
 * {@link java.text.AttributedCharacterIterator.Attribute} and derived types.
 * 
 * @author J&ouml;rg Schaible
 * @since 1.2.2
 */
public class AbstractAttributedCharacterIteratorAttributeConverter extends
		AbstractSingleValueConverter {

    private static Method getName;
    static {
        try {
            getName = AttributedCharacterIterator.Attribute.class.getDeclaredMethod(
                "getName", (Class[])null);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError("Missing AttributedCharacterIterator.Attribute.getName()");
        } catch( AccessControlException e ) {
           // happens inside of applets
           getName = null;
        }
    }

    private final Class type;
    private transient Map attributeMap;
    private transient FieldDictionary fieldDictionary;

    public AbstractAttributedCharacterIteratorAttributeConverter(final Class type) {
        super();
       if( getName != null ) {
          this.type = type;
          readResolve();
       } else {
          this.type = null;
       }
    }

    public boolean canConvert(final Class type) {
        return type == this.type;
    }

    public String toString(final Object source) {
        AttributedCharacterIterator.Attribute attribute = (AttributedCharacterIterator.Attribute)source;
        try {
            if (!getName.isAccessible()) {
                getName.setAccessible(true);
            }
            return (String)getName.invoke(attribute, (Object[])null);
        } catch (IllegalAccessException e) {
            throw new ObjectAccessException(
                "Cannot get name of AttributedCharacterIterator.Attribute", e);
        } catch (InvocationTargetException e) {
            throw new ObjectAccessException(
                "Cannot get name of AttributedCharacterIterator.Attribute", e
                    .getTargetException());
        }
    }

    public Object fromString(final String str) {
        return attributeMap.get(str);
    }

    private Object readResolve() {
        fieldDictionary = new FieldDictionary();
        attributeMap = new HashMap();
        for (final Iterator iterator = fieldDictionary.fieldsFor(type); iterator
            .hasNext();) {
            final Field field = (Field)iterator.next();
            if (field.getType() == type && Modifier.isStatic(field.getModifiers())) {
                try {
                    final Object attribute = field.get(null);
                    attributeMap.put(toString(attribute), attribute);
                } catch (IllegalAccessException e) {
                    throw new ObjectAccessException("Cannot get object of " + field, e);
                }
            }
        }
        return this;
    }

}
