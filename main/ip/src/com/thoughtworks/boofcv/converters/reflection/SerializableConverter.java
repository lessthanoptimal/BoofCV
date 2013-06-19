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

import com.thoughtworks.boofcv.core.util.CustomObjectInputStream;
import com.thoughtworks.boofcv.core.util.XStreamClassLoader;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.*;
import com.thoughtworks.xstream.core.util.CustomObjectOutputStream;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Emulates the mechanism used by standard Java Serialization for classes that implement java.io.Serializable AND
 * implement or inherit a custom readObject()/writeObject() method.
 *
 * <h3>Supported features of serialization</h3>
 * <ul>
 *   <li>readObject(), writeObject()</li>
 *   <li>class inheritance</li>
 *   <li>readResolve(), writeReplace()</li>
 * </ul>
 *
 * <h3>Currently unsupported features</h3>
 * <ul>
 *   <li>putFields(), writeFields(), readFields()</li>
 *   <li>ObjectStreamField[] serialPersistentFields</li>
 *   <li>ObjectInputValidation</li>
 * </ul>
 *
 * @author Joe Walnes
 * @author J&ouml;rg Schaible
 */
public class SerializableConverter extends AbstractReflectionConverter {

    private static final String ELEMENT_NULL = "null";
    private static final String ELEMENT_DEFAULT = "default";
    private static final String ELEMENT_UNSERIALIZABLE_PARENTS = "unserializable-parents";
    private static final String ATTRIBUTE_CLASS = "class";
    private static final String ATTRIBUTE_SERIALIZATION = "serialization";
    private static final String ATTRIBUTE_VALUE_CUSTOM = "custom";
    private static final String ELEMENT_FIELDS = "fields";
    private static final String ELEMENT_FIELD = "field";
    private static final String ATTRIBUTE_NAME = "name";
    private final XStreamClassLoader classLoader;

    public SerializableConverter(Mapper mapper, ReflectionProvider reflectionProvider, XStreamClassLoader classLoader) {
        super(mapper, new UnserializableParentsReflectionProvider(reflectionProvider));
        this.classLoader = classLoader;
    }

    /**
     * @deprecated As of 1.4 use {@link #SerializableConverter(com.thoughtworks.xstream.mapper.Mapper, ReflectionProvider, com.thoughtworks.boofcv.core.util.XStreamClassLoader)}
     */
    public SerializableConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
        this(mapper, new UnserializableParentsReflectionProvider(reflectionProvider), null);
    }

    public boolean canConvert(Class type) {
        return isSerializable(type);
    }

    private boolean isSerializable(Class type) {
        return Serializable.class.isAssignableFrom(type)
          && ( serializationMethodInvoker.supportsReadObject(type, true)
            || serializationMethodInvoker.supportsWriteObject(type, true) );
    }

    public void doMarshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        String attributeName = mapper.aliasForSystemAttribute(ATTRIBUTE_SERIALIZATION);
        if (attributeName != null) {
            writer.addAttribute(attributeName, ATTRIBUTE_VALUE_CUSTOM);
        }

        // this is an array as it's a non final value that's accessed from an anonymous inner class.
        final Class[] currentType = new Class[1];
        final boolean[] writtenClassWrapper = {false};

        CustomObjectOutputStream.StreamCallback callback = new CustomObjectOutputStream.StreamCallback() {

            public void writeToStream(Object object) {
                if (object == null) {
                    writer.startNode(ELEMENT_NULL);
                    writer.endNode();
                } else {
                    ExtendedHierarchicalStreamWriterHelper.startNode(writer, mapper.serializedClass(object.getClass()), object.getClass());
                    context.convertAnother(object);
                    writer.endNode();
                }
            }

            public void writeFieldsToStream(Map fields) {
                ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(currentType[0]);

                writer.startNode(ELEMENT_DEFAULT);
                for (Iterator iterator = fields.keySet().iterator(); iterator.hasNext();) {
                    String name = (String) iterator.next();
                    if (!mapper.shouldSerializeMember(currentType[0], name)) {
                        continue;
                    }
                    ObjectStreamField field = objectStreamClass.getField(name);
                    Object value = fields.get(name);
                    if (field == null) {
                        throw new ObjectAccessException("Class " + value.getClass().getName()
                                + " may not write a field named '" + name + "'");
                    }
                    if (value != null) {
                        ExtendedHierarchicalStreamWriterHelper.startNode(
								writer, mapper.serializedMember(source.getClass(), name),
								value.getClass());
                        if (field.getType() != value.getClass() && !field.getType().isPrimitive()) {
                            String attributeName = mapper.aliasForSystemAttribute(ATTRIBUTE_CLASS);
                            if (attributeName != null) {
                                writer.addAttribute(attributeName, mapper.serializedClass(value.getClass()));
                            }
                        }
                        context.convertAnother(value);
                        writer.endNode();
                    }
                }
                writer.endNode();
            }

            public void defaultWriteObject() {
                boolean writtenDefaultFields = false;

                ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(currentType[0]);

                if (objectStreamClass == null) {
                    return;
                }

                ObjectStreamField[] fields = objectStreamClass.getFields();
                for (int i = 0; i < fields.length; i++) {
                    ObjectStreamField field = fields[i];
                    Object value = readField(field, currentType[0], source);
                    if (value != null) {
                        if (!writtenClassWrapper[0]) {
                            writer.startNode(mapper.serializedClass(currentType[0]));
                            writtenClassWrapper[0] = true;
                        }
                        if (!writtenDefaultFields) {
                            writer.startNode(ELEMENT_DEFAULT);
                            writtenDefaultFields = true;
                        }
                        if (!mapper.shouldSerializeMember(currentType[0], field.getName())) {
                            continue;
                        }

                        Class actualType = value.getClass();
                        ExtendedHierarchicalStreamWriterHelper.startNode(
								writer, mapper.serializedMember(source.getClass(), field.getName()), actualType);
                        Class defaultType = mapper.defaultImplementationOf(field.getType());
                        if (!actualType.equals(defaultType)) {
                            String attributeName = mapper.aliasForSystemAttribute(ATTRIBUTE_CLASS);
                            if (attributeName != null) {
                                writer.addAttribute(attributeName, mapper.serializedClass(actualType));
                            }
                        }

                        context.convertAnother(value);

                        writer.endNode();
                    }
                }
                if (writtenClassWrapper[0] && !writtenDefaultFields) {
                    writer.startNode(ELEMENT_DEFAULT);
                    writer.endNode();
                } else if (writtenDefaultFields) {
                    writer.endNode();
                }
            }

            public void flush() {
                writer.flush();
            }

            public void close() {
                throw new UnsupportedOperationException("Objects are not allowed to call ObjectOutputStream.close() from writeObject()");
            }
        };

        try {
            boolean mustHandleUnserializableParent = false;
            Iterator classHieararchy = hierarchyFor(source.getClass()).iterator();
            while (classHieararchy.hasNext()) {
                currentType[0] = (Class) classHieararchy.next();
                if (!Serializable.class.isAssignableFrom(currentType[0])) {
                    mustHandleUnserializableParent = true;
                    continue;
                } else {
                    if (mustHandleUnserializableParent) {
                        marshalUnserializableParent(writer, context, source);
                        mustHandleUnserializableParent = false;
                    }
                    if (serializationMethodInvoker.supportsWriteObject(currentType[0], false)) {
                        writtenClassWrapper[0] = true;
                        writer.startNode(mapper.serializedClass(currentType[0]));
                        if (currentType[0] != mapper.defaultImplementationOf(currentType[0])) { 
                            String classAttributeName = mapper.aliasForSystemAttribute(ATTRIBUTE_CLASS);
                            if (classAttributeName != null) {
                                writer.addAttribute(classAttributeName, currentType[0].getName());
                            }
                        }
                        CustomObjectOutputStream objectOutputStream = CustomObjectOutputStream.getInstance(context, callback);
                        serializationMethodInvoker.callWriteObject(currentType[0], source, objectOutputStream);
                        objectOutputStream.popCallback();
                        writer.endNode();
                    } else if (serializationMethodInvoker.supportsReadObject(currentType[0], false)) {
                        // Special case for objects that have readObject(), but not writeObject().
                        // The class wrapper is always written, whether or not this class in the hierarchy has
                        // serializable fields. This guarantees that readObject() will be called upon deserialization.
                        writtenClassWrapper[0] = true;
                        writer.startNode(mapper.serializedClass(currentType[0]));
                        if (currentType[0] != mapper.defaultImplementationOf(currentType[0])) { 
                            String classAttributeName = mapper.aliasForSystemAttribute(ATTRIBUTE_CLASS);
                            if (classAttributeName != null) {
                                writer.addAttribute(classAttributeName, currentType[0].getName());
                            }
                        }
                        callback.defaultWriteObject();
                        writer.endNode();
                    } else {
                        writtenClassWrapper[0] = false;
                        callback.defaultWriteObject();
                        if (writtenClassWrapper[0]) {
                            writer.endNode();
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ObjectAccessException("Could not call defaultWriteObject()", e);
        }
    }

    protected void marshalUnserializableParent(final HierarchicalStreamWriter writer, final MarshallingContext context, final Object replacedSource) {
        writer.startNode(ELEMENT_UNSERIALIZABLE_PARENTS);
        super.doMarshal(replacedSource, writer, context);
        writer.endNode();
    }

    private Object readField(ObjectStreamField field, Class type, Object instance) {
        try {
            Field javaField = type.getDeclaredField(field.getName());
            javaField.setAccessible(true);
            return javaField.get(instance);
        } catch (IllegalArgumentException e) {
            throw new ObjectAccessException("Could not get field " + field.getClass() + "." + field.getName(), e);
        } catch (IllegalAccessException e) {
            throw new ObjectAccessException("Could not get field " + field.getClass() + "." + field.getName(), e);
        } catch (NoSuchFieldException e) {
            throw new ObjectAccessException("Could not get field " + field.getClass() + "." + field.getName(), e);
        } catch (SecurityException e) {
            throw new ObjectAccessException("Could not get field " + field.getClass() + "." + field.getName(), e);
        }
    }

    protected List hierarchyFor(Class type) {
        List result = new ArrayList();
        while(type != Object.class) {
            result.add(type);
            type = type.getSuperclass();
        }

        // In Java Object Serialization, the classes are deserialized starting from parent class and moving down.
        Collections.reverse(result);

        return result;
    }

    public Object doUnmarshal(final Object result, final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        // this is an array as it's a non final value that's accessed from an anonymous inner class.
        final Class[] currentType = new Class[1];

        String attributeName = mapper.aliasForSystemAttribute(ATTRIBUTE_SERIALIZATION);
        if (attributeName != null && !ATTRIBUTE_VALUE_CUSTOM.equals(reader.getAttribute(attributeName))) {
            throw new ConversionException("Cannot deserialize object with new readObject()/writeObject() methods");
        }

        CustomObjectInputStream.StreamCallback callback = new CustomObjectInputStream.StreamCallback() {
            public Object readFromStream() {
                reader.moveDown();
                Class type = HierarchicalStreams.readClassType(reader, mapper);
                Object value = context.convertAnother(result, type);
                reader.moveUp();
                return value;
            }

            public Map readFieldsFromStream() {
                final Map fields = new HashMap();
                reader.moveDown();
                if (reader.getNodeName().equals(ELEMENT_FIELDS)) {
                    // Maintain compatibility with XStream 1.1.0
                    while (reader.hasMoreChildren()) {
                        reader.moveDown();
                        if (!reader.getNodeName().equals(ELEMENT_FIELD)) {
                            throw new ConversionException("Expected <" + ELEMENT_FIELD + "/> element inside <" + ELEMENT_FIELD + "/>");
                        }
                        String name = reader.getAttribute(ATTRIBUTE_NAME);
                        Class type = mapper.realClass(reader.getAttribute(ATTRIBUTE_CLASS));
                        Object value = context.convertAnother(result, type);
                        fields.put(name, value);
                        reader.moveUp();
                    }
                } else if (reader.getNodeName().equals(ELEMENT_DEFAULT)) {
                    // New format introduced in XStream 1.1.1
                    ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(currentType[0]);
                    while (reader.hasMoreChildren()) {
                        reader.moveDown();
                        String name = mapper.realMember(currentType[0], reader.getNodeName());
                        if (mapper.shouldSerializeMember(currentType[0], name)) {
        		    String classAttribute = HierarchicalStreams.readClassAttribute(reader, mapper);
                            Class type;
                            if (classAttribute != null) {
                                type = mapper.realClass(classAttribute);
                            } else {
                                ObjectStreamField field = objectStreamClass.getField(name);
                                if (field == null) {
                                    throw new MissingFieldException(currentType[0].getName(), name);
                                }
                                type = field.getType();
                            }
                            Object value = context.convertAnother(result, type);
                            fields.put(name, value);
                        }
                        reader.moveUp();
                    }
                } else {
                    throw new ConversionException("Expected <" + ELEMENT_FIELDS + "/> or <" +
                            ELEMENT_DEFAULT + "/> element when calling ObjectInputStream.readFields()");
                }
                reader.moveUp();
                return fields;
            }

            public void defaultReadObject() {
                if (!reader.hasMoreChildren()) {
                    return;
                }
                reader.moveDown();
                if (!reader.getNodeName().equals(ELEMENT_DEFAULT)) {
                    throw new ConversionException("Expected <" + ELEMENT_DEFAULT + "/> element in readObject() stream");
                }
                while (reader.hasMoreChildren()) {
                    reader.moveDown();

                    String fieldName = mapper.realMember(currentType[0], reader.getNodeName());
                    if (mapper.shouldSerializeMember(currentType[0], fieldName)) {
                        String classAttribute = HierarchicalStreams.readClassAttribute(reader, mapper);
                        final Class type;
                        if (classAttribute != null) {
                            type = mapper.realClass(classAttribute);
                        } else {
                            type = mapper.defaultImplementationOf(reflectionProvider.getFieldType(result, fieldName, currentType[0]));
                        }

                        Object value = context.convertAnother(result, type);
                        reflectionProvider.writeField(result, fieldName, value, currentType[0]);
                    }

                    reader.moveUp();
                }
                reader.moveUp();
            }

            public void registerValidation(final ObjectInputValidation validation, int priority) {
                context.addCompletionCallback(new Runnable() {
                    public void run() {
                        try {
                            validation.validateObject();
                        } catch (InvalidObjectException e) {
                            throw new ObjectAccessException("Cannot validate object : " + e.getMessage(), e);
                        }
                    }
                }, priority);
            }

            public void close() {
                throw new UnsupportedOperationException("Objects are not allowed to call ObjectInputStream.close() from readObject()");
            }
        };

        while (reader.hasMoreChildren()) {
            reader.moveDown();
            String nodeName = reader.getNodeName();
            if (nodeName.equals(ELEMENT_UNSERIALIZABLE_PARENTS)) {
                super.doUnmarshal(result, reader, context);
            } else {
        	String classAttribute = HierarchicalStreams.readClassAttribute(reader, mapper);
                if (classAttribute == null) {
                    currentType[0] = mapper.defaultImplementationOf(mapper.realClass(nodeName));
                } else {
                    currentType[0] = mapper.realClass(classAttribute);
                }
                if (serializationMethodInvoker.supportsReadObject(currentType[0], false)) {
                    CustomObjectInputStream objectInputStream =
                        CustomObjectInputStream.getInstance(context, callback, classLoader);
                    serializationMethodInvoker.callReadObject(currentType[0], result, objectInputStream);
                    objectInputStream.popCallback();
                } else {
                    try {
                        callback.defaultReadObject();
                    } catch (IOException e) {
                        throw new ObjectAccessException("Could not call defaultWriteObject()", e);
                    }
                }
            }
            reader.moveUp();
        }

        return result;
    }
    
    protected void doMarshalConditionally(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        if(isSerializable(source.getClass())) {
            doMarshal(source, writer, context);
        } else {
            super.doMarshal(source, writer, context);
        }
    }
    
    protected Object doUnmarshalConditionally(final Object result, final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        return isSerializable(result.getClass()) ? doUnmarshal(result, reader, context) : super.doUnmarshal(result, reader, context);
    }

    private static class UnserializableParentsReflectionProvider extends ReflectionProviderWrapper {

        public UnserializableParentsReflectionProvider(final ReflectionProvider reflectionProvider) {
            super(reflectionProvider);
        }

        public void visitSerializableFields(final Object object, final Visitor visitor) {
            wrapped.visitSerializableFields(object, new Visitor() {
                public void visit(String name, Class type, Class definedIn, Object value) {
                    if (!Serializable.class.isAssignableFrom(definedIn)) {
                        visitor.visit(name, type, definedIn, value);
                    }
                }
            });
        }
    }
}
