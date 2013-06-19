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
package com.thoughtworks.boofcv.mapper;

import com.thoughtworks.boofcv.core.util.XStreamClassLoader;
import com.thoughtworks.xstream.InitializationException;
import com.thoughtworks.xstream.annotations.*;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.DependencyInjectionFactory;
import com.thoughtworks.xstream.mapper.*;

import java.lang.reflect.*;
import java.util.*;


/**
 * A mapper that uses annotations to prepare the remaining mappers in the chain.
 * 
 * @author J&ouml;rg Schaible
 * @since 1.3
 */
public class AnnotationMapper extends MapperWrapper implements AnnotationConfiguration {

    private boolean locked;
    private final Object[] arguments;
    private final ConverterRegistry converterRegistry;
    private final ClassAliasingMapper classAliasingMapper;
    private final DefaultImplementationsMapper defaultImplementationsMapper;
    private final ImplicitCollectionMapper implicitCollectionMapper;
    private final FieldAliasingMapper fieldAliasingMapper;
    private final AttributeMapper attributeMapper;
    private final LocalConversionMapper localConversionMapper;
    private final Map<Class<?>, Map<List<Object>, Converter>> converterCache =
            new HashMap<Class<?>, Map<List<Object>, Converter>>();
    private final Set<Class<?>> annotatedTypes = 
            Collections.synchronizedSet(new HashSet<Class<?>>());

    /**
     * Construct an AnnotationMapper.
     * 
     * @param wrapped the next {@link com.thoughtworks.xstream.mapper.Mapper} in the chain
     * @since 1.3
     */
    public AnnotationMapper(
        final Mapper wrapped, final ConverterRegistry converterRegistry, final ConverterLookup converterLookup,
        final XStreamClassLoader classLoader, final ReflectionProvider reflectionProvider,
        final JVM jvm) {
        super(wrapped);
        this.converterRegistry = converterRegistry;
        annotatedTypes.add(Object.class);
        classAliasingMapper = (ClassAliasingMapper) lookupMapperOfType(ClassAliasingMapper.class);
        defaultImplementationsMapper = (DefaultImplementationsMapper) lookupMapperOfType(DefaultImplementationsMapper.class);
        implicitCollectionMapper = (ImplicitCollectionMapper) lookupMapperOfType(ImplicitCollectionMapper.class);
        fieldAliasingMapper = (FieldAliasingMapper) lookupMapperOfType(FieldAliasingMapper.class);
        attributeMapper = (AttributeMapper) lookupMapperOfType(AttributeMapper.class);
        localConversionMapper = (LocalConversionMapper) lookupMapperOfType(LocalConversionMapper.class);
        locked = true;
        arguments = new Object[]{this, classLoader, reflectionProvider, jvm, converterLookup};
    }

    @Override
    public String realMember(final Class type, final String serialized) {
        if (!locked) {
            processAnnotations(type);
        }
        return super.realMember(type, serialized);
    }

    @Override
    public String serializedClass(final Class type) {
        if (!locked) {
            processAnnotations(type);
        }
        return super.serializedClass(type);
    }

    @Override
    public Class defaultImplementationOf(final Class type) {
        if (!locked) {
            processAnnotations(type);
        }
        final Class defaultImplementation = super.defaultImplementationOf(type);
        if (!locked) {
            processAnnotations(defaultImplementation);
        }
        return defaultImplementation;
    }

    @Override
    public Converter getLocalConverter(final Class definedIn, final String fieldName) {
        if (!locked) {
            processAnnotations(definedIn);
        }
        return super.getLocalConverter(definedIn, fieldName);
    }

    public void autodetectAnnotations(final boolean mode) {
        locked = !mode;
    }

    public void processAnnotations(final Class[] initialTypes) {
        if (initialTypes == null || initialTypes.length == 0) {
            return;
        }
        locked = true;
        
        final Set<Class<?>> types = new UnprocessedTypesSet();
        for (final Class initialType : initialTypes) {
            types.add(initialType);
        }
        processTypes(types);
    }

    private void processAnnotations(final Class initialType) {
        if (initialType == null) {
            return;
        }
        
        final Set<Class<?>> types = new UnprocessedTypesSet();
        types.add(initialType);
        processTypes(types);
    }

    private void processTypes(final Set<Class<?>> types) {
        while (!types.isEmpty()) {
            final Iterator<Class<?>> iter = types.iterator();
            final Class<?> type = iter.next();
            iter.remove();

            synchronized(type) {
                if (annotatedTypes.contains(type)) {
                    continue;
                }
                try {
                    if (type.isPrimitive()) {
                        continue;
                    }
    
                    addParametrizedTypes(type, types);
    
                    processConverterAnnotations(type);
                    processAliasAnnotation(type, types);
    
                    if (type.isInterface()) {
                        continue;
                    }
    
                    processImplicitCollectionAnnotation(type);
    
                    final Field[] fields = type.getDeclaredFields();
                    for (int i = 0; i < fields.length; i++ ) {
                        final Field field = fields[i];
                        if (field.isEnumConstant()
                            || (field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) > 0) {
                            continue;
                        }
    
                        addParametrizedTypes(field.getGenericType(), types);
    
                        if (field.isSynthetic()) {
                            continue;
                        }
    
                        processFieldAliasAnnotation(field);
                        processAsAttributeAnnotation(field);
                        processImplicitAnnotation(field);
                        processOmitFieldAnnotation(field);
                        processLocalConverterAnnotation(field);
                    }
                } finally {
                    annotatedTypes.add(type);
                }
            }
        }
    }

    private void addParametrizedTypes(Type type, final Set<Class<?>> types) {
        final Set<Type> processedTypes = new HashSet<Type>();
        final Set<Type> localTypes = new LinkedHashSet<Type>() {

            @Override
            public boolean add(final Type o) {
                if (o instanceof Class) {
                    return types.add((Class<?>)o);
                }
                return o == null || processedTypes.contains(o) ? false : super.add(o);
            }

        };
        while (type != null) {
            processedTypes.add(type);
            if (type instanceof Class) {
                final Class<?> clazz = (Class<?>)type;
                types.add(clazz);
                if (!clazz.isPrimitive()) {
                    final TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
                    for (final TypeVariable<?> typeVariable : typeParameters) {
                        localTypes.add(typeVariable);
                    }
                    localTypes.add(clazz.getGenericSuperclass());
                    for (final Type iface : clazz.getGenericInterfaces()) {
                        localTypes.add(iface);
                    }
                }
            } else if (type instanceof TypeVariable) {
                final TypeVariable<?> typeVariable = (TypeVariable<?>)type;
                final Type[] bounds = typeVariable.getBounds();
                for (final Type bound : bounds) {
                    localTypes.add(bound);
                }
            } else if (type instanceof ParameterizedType) {
                final ParameterizedType parametrizedType = (ParameterizedType)type;
                localTypes.add(parametrizedType.getRawType());
                final Type[] actualArguments = parametrizedType.getActualTypeArguments();
                for (final Type actualArgument : actualArguments) {
                    localTypes.add(actualArgument);
                }
            } else if (type instanceof GenericArrayType) {
                final GenericArrayType arrayType = (GenericArrayType)type;
                localTypes.add(arrayType.getGenericComponentType());
            }

            if (!localTypes.isEmpty()) {
                final Iterator<Type> iter = localTypes.iterator();
                type = iter.next();
                iter.remove();
            } else {
                type = null;
            }
        }
    }

    private void processConverterAnnotations(final Class<?> type) {
        if (converterRegistry != null) {
            final XStreamConverters convertersAnnotation = type
                .getAnnotation(XStreamConverters.class);
            final XStreamConverter converterAnnotation = type
                .getAnnotation(XStreamConverter.class);
            final List<XStreamConverter> annotations = convertersAnnotation != null
                ? new ArrayList<XStreamConverter>(Arrays.asList(convertersAnnotation.value()))
                : new ArrayList<XStreamConverter>();
            if (converterAnnotation != null) {
                annotations.add(converterAnnotation);
            }
            for (final XStreamConverter annotation : annotations) {
                final Converter converter = cacheConverter(
                    annotation, converterAnnotation != null ? type : null);
                if (converter != null) {
                    if (converterAnnotation != null || converter.canConvert(type)) {
                        converterRegistry.registerConverter(converter, annotation.priority());
                    } else {
                        throw new InitializationException("Converter "
                            + annotation.value().getName()
                            + " cannot handle annotated class "
                            + type.getName());
                    }
                }
            }
        }
    }

    private void processAliasAnnotation(final Class<?> type, final Set<Class<?>> types) {
        final XStreamAlias aliasAnnotation = type.getAnnotation(XStreamAlias.class);
        if (aliasAnnotation != null) {
            if (classAliasingMapper == null) {
                throw new InitializationException("No "
                    + ClassAliasingMapper.class.getName()
                    + " available");
            }
            if (aliasAnnotation.impl() != Void.class) {
                // Alias for Interface/Class with an impl
                classAliasingMapper.addClassAlias(aliasAnnotation.value(), type);
                defaultImplementationsMapper.addDefaultImplementation(
                    aliasAnnotation.impl(), type);
                if (type.isInterface()) {
                    types.add(aliasAnnotation.impl()); // alias Interface's impl
                }
            } else {
                classAliasingMapper.addClassAlias(aliasAnnotation.value(), type);
            }
        }
    }

    @Deprecated
    private void processImplicitCollectionAnnotation(final Class<?> type) {
        final XStreamImplicitCollection implicitColAnnotation = type
            .getAnnotation(XStreamImplicitCollection.class);
        if (implicitColAnnotation != null) {
            if (implicitCollectionMapper == null) {
                throw new InitializationException("No "
                    + ImplicitCollectionMapper.class.getName()
                    + " available");
            }
            final String fieldName = implicitColAnnotation.value();
            final String itemFieldName = implicitColAnnotation.item();
            final Field field;
            try {
                field = type.getDeclaredField(fieldName);
            } catch (final NoSuchFieldException e) {
                throw new InitializationException(type.getName()
                    + " does not have a field named '"
                    + fieldName
                    + "' as required by "
                    + XStreamImplicitCollection.class.getName());
            }
            Class itemType = null;
            final Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                final Type typeArgument = ((ParameterizedType)genericType)
                    .getActualTypeArguments()[0];
                itemType = getClass(typeArgument);
            }
            if (itemType == null) {
                implicitCollectionMapper.add(type, fieldName, null, Object.class);
            } else {
                if (itemFieldName.equals("")) {
                    implicitCollectionMapper.add(type, fieldName, null, itemType);
                } else {
                    implicitCollectionMapper.add(type, fieldName, itemFieldName, itemType);
                }
            }
        }
    }

    private void processFieldAliasAnnotation(final Field field) {
        final XStreamAlias aliasAnnotation = field.getAnnotation(XStreamAlias.class);
        if (aliasAnnotation != null) {
            if (fieldAliasingMapper == null) {
                throw new InitializationException("No "
                    + FieldAliasingMapper.class.getName()
                    + " available");
            }
            fieldAliasingMapper.addFieldAlias(
                aliasAnnotation.value(), field.getDeclaringClass(), field.getName());
        }
    }

    private void processAsAttributeAnnotation(final Field field) {
        final XStreamAsAttribute asAttributeAnnotation = field
            .getAnnotation(XStreamAsAttribute.class);
        if (asAttributeAnnotation != null) {
            if (attributeMapper == null) {
                throw new InitializationException("No "
                    + AttributeMapper.class.getName()
                    + " available");
            }
            attributeMapper.addAttributeFor(field);
        }
    }

    private void processImplicitAnnotation(final Field field) {
        final XStreamImplicit implicitAnnotation = field.getAnnotation(XStreamImplicit.class);
        if (implicitAnnotation != null) {
            if (implicitCollectionMapper == null) {
                throw new InitializationException("No "
                    + ImplicitCollectionMapper.class.getName()
                    + " available");
            }
            final String fieldName = field.getName();
            final String itemFieldName = implicitAnnotation.itemFieldName();
            final String keyFieldName = implicitAnnotation.keyFieldName();
            boolean isMap = Map.class.isAssignableFrom(field.getType());
            Class itemType = null;
            if (!field.getType().isArray()) {
                final Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    final Type[] actualTypeArguments = ((ParameterizedType)genericType)
                        .getActualTypeArguments();
                    final Type typeArgument = actualTypeArguments[isMap ? 1 : 0];
                    itemType = getClass(typeArgument);
                }
            }
            if (isMap) {
                implicitCollectionMapper.add(
                    field.getDeclaringClass(), fieldName,
                    itemFieldName != null && !"".equals(itemFieldName) ? itemFieldName : null,
                    itemType, keyFieldName != null && !"".equals(keyFieldName)
                        ? keyFieldName
                        : null);
            } else {
                if (itemFieldName != null && !"".equals(itemFieldName)) {
                    implicitCollectionMapper.add(
                        field.getDeclaringClass(), fieldName, itemFieldName, itemType);
                } else {
                    implicitCollectionMapper
                        .add(field.getDeclaringClass(), fieldName, itemType);
                }
            }
        }
    }

    private void processOmitFieldAnnotation(final Field field) {
        final XStreamOmitField omitFieldAnnotation = field
            .getAnnotation(XStreamOmitField.class);
        if (omitFieldAnnotation != null) {
            if (fieldAliasingMapper == null) {
                throw new InitializationException("No "
                    + FieldAliasingMapper.class.getName()
                    + " available");
            }
            fieldAliasingMapper.omitField(field.getDeclaringClass(), field.getName());
        }
    }

    private void processLocalConverterAnnotation(final Field field) {
        final XStreamConverter annotation = field.getAnnotation(XStreamConverter.class);
        if (annotation != null) {
            final Converter converter = cacheConverter(annotation, field.getType());
            if (converter != null) {
                if (localConversionMapper == null) {
                    throw new InitializationException("No "
                        + LocalConversionMapper.class.getName()
                        + " available");
                }
                localConversionMapper.registerLocalConverter(
                    field.getDeclaringClass(), field.getName(), converter);
            }
        }
    }

    private Converter cacheConverter(final XStreamConverter annotation,
        final Class targetType) {
        Converter result = null;
        final Object[] args;
        final List<Object> parameter = new ArrayList<Object>();
        if (targetType != null) {
            parameter.add(targetType);
        }
        final List<Object> arrays = new ArrayList<Object>();
        arrays.add(annotation.booleans());
        arrays.add(annotation.bytes());
        arrays.add(annotation.chars());
        arrays.add(annotation.doubles());
        arrays.add(annotation.floats());
        arrays.add(annotation.ints());
        arrays.add(annotation.longs());
        arrays.add(annotation.shorts());
        arrays.add(annotation.strings());
        arrays.add(annotation.types());
        for(Object array : arrays) {
            if (array != null) {
                int length = Array.getLength(array);
                for (int i = 0; i < length; i++ ) {
                    Object object = Array.get(array, i);
                    if (!parameter.contains(object)) {
                        parameter.add(object);
                    }
                }
            }
        }
        final Class<? extends ConverterMatcher> converterType = annotation.value();
        Map<List<Object>, Converter> converterMapping = converterCache.get(converterType);
        if (converterMapping != null) {
            result = converterMapping.get(parameter);
        }
        if (result == null) {
            int size = parameter.size();
            if (size > 0) {
                args = new Object[arguments.length + size];
                System.arraycopy(arguments, 0, args, size, arguments.length);
                System.arraycopy(parameter.toArray(new Object[size]), 0, args, 0, size);
            } else {
                args = arguments;
            }

            final Converter converter;
            try {
                if (SingleValueConverter.class.isAssignableFrom(converterType)
                    && !Converter.class.isAssignableFrom(converterType)) {
                    final SingleValueConverter svc = (SingleValueConverter) DependencyInjectionFactory
							.newInstance(converterType, args);
                    converter = new SingleValueConverterWrapper(svc);
                } else {
                    converter = (Converter) DependencyInjectionFactory.newInstance(
							converterType, args);
                }
            } catch (final Exception e) {
                throw new InitializationException("Cannot instantiate converter "
                    + converterType.getName()
                    + (targetType != null ? " for type " + targetType.getName() : ""), e);
            }
            if (converterMapping == null) {
                converterMapping = new HashMap<List<Object>, Converter>();
                converterCache.put(converterType, converterMapping);
            }
            converterMapping.put(parameter, converter);
            result = converter;
        }
        return result;
    }

    private Class<?> getClass(final Type typeArgument) {
        Class<?> type = null;
        if (typeArgument instanceof ParameterizedType) {
            type = (Class<?>)((ParameterizedType)typeArgument).getRawType();
        } else if (typeArgument instanceof Class) {
            type = (Class<?>)typeArgument;
        }
        return type;
    }

    private final class UnprocessedTypesSet extends LinkedHashSet<Class<?>> {
        @Override
        public boolean add(Class<?> type) {
            if (type == null) {
                return false;
            }
            while (type.isArray()) {
                type = type.getComponentType();
            }
            final String name = type.getName();
            if (name.startsWith("java.") || name.startsWith("javax.")) {
                return false;
            }
            final boolean ret = annotatedTypes.contains(type) ? false : super.add(type);
            if (ret) {
                final XStreamInclude inc = type.getAnnotation(XStreamInclude.class);
                if (inc != null) {
                    final Class<?>[] incTypes = inc.value();
                    if (incTypes != null) {
                        for (final Class<?> incType : incTypes) {
                            add(incType);
                        }
                    }
                }
            }
            return ret;
        }
    }
}
