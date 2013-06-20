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
package boofcv.misc;

import com.thoughtworks.boofcv.converters.extended.DynamicProxyConverter;
import com.thoughtworks.boofcv.converters.extended.JavaClassConverter;
import com.thoughtworks.boofcv.converters.extended.JavaFieldConverter;
import com.thoughtworks.boofcv.converters.extended.JavaMethodConverter;
import com.thoughtworks.boofcv.converters.reflection.ExternalizableConverter;
import com.thoughtworks.boofcv.converters.reflection.SerializableConverter;
import com.thoughtworks.boofcv.core.util.ClassLoaderReference;
import com.thoughtworks.boofcv.core.util.CompositeClassLoader;
import com.thoughtworks.boofcv.core.util.CustomObjectInputStream;
import com.thoughtworks.boofcv.core.util.XStreamClassLoader;
import com.thoughtworks.boofcv.mapper.DefaultMapper;
import com.thoughtworks.xstream.MarshallingStrategy;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.converters.basic.*;
import com.thoughtworks.xstream.converters.collections.*;
import com.thoughtworks.xstream.converters.extended.*;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SelfStreamingInstanceChecker;
import com.thoughtworks.xstream.core.*;
import com.thoughtworks.xstream.core.util.CustomObjectOutputStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StatefulWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.*;


//***********************
// Hack to avoid exception in static block

//***********************

/**
 * Simple facade to XStream library, a Java-XML serialization tool. <p/>
 * <p>
 * <hr>
 * <b>Example</b><blockquote>
 *
 * <pre>
 * XStream xstream = new XStream();
 * String xml = xstream.toXML(myObject); // serialize to XML
 * Object myObject2 = xstream.fromXML(xml); // deserialize from XML
 * </pre>
 *
 * </blockquote>
 * <hr>
 * <p/>
 * <h3>Aliasing classes</h3>
 * <p/>
 * <p>
 * To create shorter XML, you can specify aliases for classes using the <code>alias()</code>
 * method. For example, you can shorten all occurrences of element
 * <code>&lt;com.blah.MyThing&gt;</code> to <code>&lt;my-thing&gt;</code> by registering an
 * alias for the class.
 * <p>
 * <hr>
 * <blockquote>
 *
 * <pre>
 * xstream.alias(&quot;my-thing&quot;, MyThing.class);
 * </pre>
 *
 * </blockquote>
 * <hr>
 * <p/>
 * <h3>Converters</h3>
 * <p/>
 * <p>
 * XStream contains a map of {@link com.thoughtworks.xstream.converters.Converter} instances, each
 * of which acts as a strategy for converting a particular type of class to XML and back again. Out
 * of the box, XStream contains converters for most basic types (String, Date, int, boolean, etc)
 * and collections (Map, List, Set, Properties, etc). For other objects reflection is used to
 * serialize each field recursively.
 * </p>
 * <p/>
 * <p>
 * Extra converters can be registered using the <code>registerConverter()</code> method. Some
 * non-standard converters are supplied in the {@link com.thoughtworks.xstream.converters.extended}
 * package and you can create your own by implementing the
 * {@link com.thoughtworks.xstream.converters.Converter} interface.
 * </p>
 * <p/>
 * <p>
 * <hr>
 * <b>Example</b><blockquote>
 *
 * <pre>
 * xstream.registerConverter(new SqlTimestampConverter());
 * xstream.registerConverter(new DynamicProxyConverter());
 * </pre>
 *
 * </blockquote>
 * <hr>
 * <p>
 * The converters can be registered with an explicit priority. By default they are registered with 
 * XStream.PRIORITY_NORMAL. Converters of same priority will be used in the reverse sequence
 * they have been registered. The default converter, i.e. the converter which will be used if
 * no other registered converter is suitable, can be registered with priority
 * XStream.PRIORITY_VERY_LOW. XStream uses by default the
 * {@link com.thoughtworks.xstream.converters.reflection.ReflectionConverter} as the fallback
 * converter.
 * </p>
 * <p/>
 * <p>
 * <hr>
 * <b>Example</b><blockquote>
 *
 * <pre>
 * xstream.registerConverter(new CustomDefaultConverter(), XStream.PRIORITY_VERY_LOW);
 * </pre>
 *
 * </blockquote>
 * <hr>
 * <p/>
 * <h3>Object graphs</h3>
 * <p/>
 * <p>
 * XStream has support for object graphs; a deserialized object graph will keep references intact,
 * including circular references.
 * </p>
 * <p/>
 * <p>
 * XStream can signify references in XML using either relative/absolute XPath or IDs. The mode can be changed using
 * <code>setMode()</code>:
 * </p>
 * <p/>
 * <table border='1'>
 * <tr>
 * <td><code>xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);</code></td>
 * <td><i>(Default)</i> Uses XPath relative references to signify duplicate references. This produces XML
 * with the least clutter.</td>
 * </tr>
 * <tr>
 * <td><code>xstream.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);</code></td>
 * <td>Uses XPath absolute references to signify duplicate
 * references. This produces XML with the least clutter.</td>
 * </tr>
 * <tr>
 * <td><code>xstream.setMode(XStream.SINGLE_NODE_XPATH_RELATIVE_REFERENCES);</code></td>
 * <td>Uses XPath relative references to signify duplicate references. The XPath expression ensures that
 * a single node only is selected always.</td>
 * </tr>
 * <tr>
 * <td><code>xstream.setMode(XStream.SINGLE_NODE_XPATH_ABSOLUTE_REFERENCES);</code></td>
 * <td>Uses XPath absolute references to signify duplicate references.  The XPath expression ensures that
 * a single node only is selected always.</td>
 * </tr>
 * <tr>
 * <td><code>xstream.setMode(XStream.ID_REFERENCES);</code></td>
 * <td>Uses ID references to signify duplicate references. In some scenarios, such as when using
 * hand-written XML, this is easier to work with.</td>
 * </tr>
 * <tr>
 * <td><code>xstream.setMode(XStream.NO_REFERENCES);</code></td>
 * <td>This disables object graph support and treats the object structure like a tree. Duplicate
 * references are treated as two separate objects and circular references cause an exception. This
 * is slightly faster and uses less memory than the other two modes.</td>
 * </tr>
 * </table>
 * <h3>Thread safety</h3>
 * <p>
 * The XStream instance is thread-safe. That is, once the XStream instance has been created and
 * configured, it may be shared across multiple threads allowing objects to be
 * serialized/deserialized concurrently. <em>Note, that this only applies if annotations are not 
 * auto-detected on -the-fly.</em>
 * </p>
 * <h3>Implicit collections</h3>
 * <p/>
 * <p>
 * To avoid the need for special tags for collections, you can define implicit collections using one
 * of the <code>addImplicitCollection</code> methods.
 * </p>
 *
 * @author Joe Walnes
 * @author J&ouml;rg Schaible
 * @author Mauro Talevi
 * @author Guilherme Silveira
 */
public class XStreamAppletVersion {

    // CAUTION: The sequence of the fields is intentional for an optimal XML output of a
    // self-serialization!
    private ReflectionProvider reflectionProvider;
    private HierarchicalStreamDriver hierarchicalStreamDriver;
    private ClassLoaderReference classLoaderReference;
    private MarshallingStrategy marshallingStrategy;
    private ConverterLookup converterLookup;
    private ConverterRegistry converterRegistry;
    private Mapper mapper;

    private PackageAliasingMapper packageAliasingMapper;
    private ClassAliasingMapper classAliasingMapper;
    private FieldAliasingMapper fieldAliasingMapper;
    private AttributeAliasingMapper attributeAliasingMapper;
    private SystemAttributeAliasingMapper systemAttributeAliasingMapper;
    private AttributeMapper attributeMapper;
    private DefaultImplementationsMapper defaultImplementationsMapper;
    private ImmutableTypesMapper immutableTypesMapper;
    private ImplicitCollectionMapper implicitCollectionMapper;
    private LocalConversionMapper localConversionMapper;
    private AnnotationConfiguration annotationConfiguration;

    private transient JVM jvm = new JVM();

    public static final int NO_REFERENCES = 1001;
    public static final int ID_REFERENCES = 1002;
    public static final int XPATH_RELATIVE_REFERENCES = 1003;
    public static final int XPATH_ABSOLUTE_REFERENCES = 1004;
    public static final int SINGLE_NODE_XPATH_RELATIVE_REFERENCES = 1005;
    public static final int SINGLE_NODE_XPATH_ABSOLUTE_REFERENCES = 1006;

    public static final int PRIORITY_VERY_HIGH = 10000;
    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_LOW = -10;
    public static final int PRIORITY_VERY_LOW = -20;

    private static final String ANNOTATION_MAPPER_TYPE = "com.thoughtworks.boofcv.mapper.AnnotationMapper";

    /**
     * Constructs a default XStream. The instance will use the {@link XppDriver} as default and
     * tries to determine the best match for the {@link ReflectionProvider} on its own.
     * 
     * @throws InitializationException in case of an initialization problem
     */
    public XStreamAppletVersion() {
        this(null, (Mapper)null, new XppDriver());
    }

    /**
     * Constructs an XStream with a special {@link ReflectionProvider}. The instance will use
     * the {@link XppDriver} as default.
     * 
     * @throws InitializationException in case of an initialization problem
     */
    public XStreamAppletVersion(ReflectionProvider reflectionProvider) {
        this(reflectionProvider, (Mapper)null, new XppDriver());
    }

    /**
     * Constructs an XStream with a special {@link HierarchicalStreamDriver}. The instance will
     * tries to determine the best match for the {@link ReflectionProvider} on its own.
     * 
     * @throws InitializationException in case of an initialization problem
     */
    public XStreamAppletVersion(HierarchicalStreamDriver hierarchicalStreamDriver) {
        this(null, (Mapper)null, hierarchicalStreamDriver);
    }

    /**
     * Constructs an XStream with a special {@link HierarchicalStreamDriver} and
     * {@link ReflectionProvider}.
     * 
     * @throws InitializationException in case of an initialization problem
     */
    public XStreamAppletVersion(
        ReflectionProvider reflectionProvider, HierarchicalStreamDriver hierarchicalStreamDriver) {
        this(reflectionProvider, (Mapper)null, hierarchicalStreamDriver);
    }

    /**
     * Constructs an XStream with a special {@link HierarchicalStreamDriver} and
     * {@link ReflectionProvider} and additionally with a prepared {@link Mapper}.
     * 
     * @throws InitializationException in case of an initialization problem
     */
    public XStreamAppletVersion(
        ReflectionProvider reflectionProvider, Mapper mapper, HierarchicalStreamDriver driver) {
        this(
            reflectionProvider, driver, new ClassLoaderReference(new CompositeClassLoader()),
            mapper, new DefaultConverterLookup(), null);
    }

    /**
     * Constructs an XStream with a special {@link HierarchicalStreamDriver} and
     * {@link ReflectionProvider} and additionally with a prepared {@link ClassLoader} to use.
     * 
     * @throws InitializationException in case of an initialization problem
     * @since 1.3
     */
    public XStreamAppletVersion(
        ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver,
		XStreamClassLoader classLoader) {
        this(reflectionProvider, driver, classLoader, null);
    }

    /**
     * Constructs an XStream with a special {@link HierarchicalStreamDriver} and
     * {@link ReflectionProvider} and additionally with a prepared {@link Mapper} and the
     * {@link ClassLoader} in use.
     * <p>
     * Note, if the class loader should be changed later again, you should provide a
     * {@link com.thoughtworks.boofcv.core.util.ClassLoaderReference} as {@link ClassLoader} that is also use in the
     * {@link Mapper} chain.
     * </p>
     * 
     * @throws InitializationException in case of an initialization problem
     * @since 1.3
     */
    public XStreamAppletVersion(
        ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver,
		XStreamClassLoader classLoader, Mapper mapper) {
        this(
            reflectionProvider, driver, classLoader, mapper, new DefaultConverterLookup(), null);
    }

    /**
     * Constructs an XStream with a special {@link HierarchicalStreamDriver},
     * {@link ReflectionProvider}, a prepared {@link Mapper} and the {@link ClassLoader} in use
     * and an own {@link ConverterRegistry}.
     * <p>
     * Note, if the class loader should be changed later again, you should provide a
     * {@link com.thoughtworks.boofcv.core.util.ClassLoaderReference} as {@link ClassLoader} that is also use in the
     * {@link Mapper} chain.
     * </p>
     * 
     * @throws InitializationException in case of an initialization problem
     * @since 1.3
     */
    public XStreamAppletVersion(
        ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver,
        XStreamClassLoader classLoader, Mapper mapper, ConverterLookup converterLookup,
        ConverterRegistry converterRegistry) {
        jvm = new JVM();
        if (reflectionProvider == null) {
            reflectionProvider = jvm.bestReflectionProvider();
        }
        this.reflectionProvider = reflectionProvider;
        this.hierarchicalStreamDriver = driver;

        this.classLoaderReference = classLoader instanceof ClassLoaderReference
            ? (ClassLoaderReference)classLoader
            : new ClassLoaderReference(classLoader);
        this.converterLookup = converterLookup;
        this.converterRegistry = converterRegistry != null
            ? converterRegistry
            : (converterLookup instanceof ConverterRegistry
                ? (ConverterRegistry)converterLookup
                : null);
        this.mapper = mapper == null ? buildMapper() : mapper;

        setupMappers();
        setupAliases();
        setupDefaultImplementations();
        setupConverters();
        setupImmutableTypes();
        setMode(XPATH_RELATIVE_REFERENCES);
    }

    private Mapper buildMapper() {
        Mapper mapper = new DefaultMapper(classLoaderReference);
        if (useXStream11XmlFriendlyMapper()) {
            mapper = new XStream11XmlFriendlyMapper(mapper);
        }
        mapper = new DynamicProxyMapper(mapper);
        mapper = new PackageAliasingMapper(mapper);
        mapper = new ClassAliasingMapper(mapper);
        mapper = new FieldAliasingMapper(mapper);
        mapper = new AttributeAliasingMapper(mapper);
        mapper = new SystemAttributeAliasingMapper(mapper);
        mapper = new ImplicitCollectionMapper(mapper);
        mapper = new OuterClassMapper(mapper);
        mapper = new ArrayMapper(mapper);
        mapper = new DefaultImplementationsMapper(mapper);
        mapper = new AttributeMapper(mapper, converterLookup, reflectionProvider);
        if (JVM.is15()) {
            mapper = buildMapperDynamically(
                "com.thoughtworks.xstream.mapper.EnumMapper", new Class[]{Mapper.class},
                new Object[]{mapper});
        }
        mapper = new LocalConversionMapper(mapper);
        mapper = new ImmutableTypesMapper(mapper);
        if (JVM.is15()) {
            mapper = buildMapperDynamically(ANNOTATION_MAPPER_TYPE, new Class[]{
                Mapper.class, ConverterRegistry.class, ConverterLookup.class,
                XStreamClassLoader.class, ReflectionProvider.class, JVM.class}, new Object[]{
                mapper, converterLookup, converterLookup, classLoaderReference,
                reflectionProvider, jvm});
        }
        mapper = wrapMapper((MapperWrapper)mapper);
        mapper = new CachingMapper(mapper);
        return mapper;
    }

	private Mapper buildMapperDynamically(String className, Class[] constructorParamTypes,
										  Object[] constructorParamValues) {
		try {
			Class type = classLoaderReference.loadClass(className);
			Constructor constructor = type.getConstructor(constructorParamTypes);
			return (Mapper)constructor.newInstance(constructorParamValues);
		} catch (Exception e) {
			throw new com.thoughtworks.xstream.InitializationException(
					"Could not instantiate mapper : " + className, e);
		}
	}

    protected MapperWrapper wrapMapper(MapperWrapper next) {
        return next;
    }

    protected boolean useXStream11XmlFriendlyMapper() {
        return false;
    }

    private void setupMappers() {
        packageAliasingMapper = (PackageAliasingMapper)this.mapper
            .lookupMapperOfType(PackageAliasingMapper.class);
        classAliasingMapper = (ClassAliasingMapper)this.mapper
            .lookupMapperOfType(ClassAliasingMapper.class);
        fieldAliasingMapper = (FieldAliasingMapper)this.mapper
            .lookupMapperOfType(FieldAliasingMapper.class);
        attributeMapper = (AttributeMapper)this.mapper
            .lookupMapperOfType(AttributeMapper.class);
        attributeAliasingMapper = (AttributeAliasingMapper)this.mapper
            .lookupMapperOfType(AttributeAliasingMapper.class);
        systemAttributeAliasingMapper = (SystemAttributeAliasingMapper)this.mapper
            .lookupMapperOfType(SystemAttributeAliasingMapper.class);
        implicitCollectionMapper = (ImplicitCollectionMapper)this.mapper
            .lookupMapperOfType(ImplicitCollectionMapper.class);
        defaultImplementationsMapper = (DefaultImplementationsMapper)this.mapper
            .lookupMapperOfType(DefaultImplementationsMapper.class);
        immutableTypesMapper = (ImmutableTypesMapper)this.mapper
            .lookupMapperOfType(ImmutableTypesMapper.class);
        localConversionMapper = (LocalConversionMapper)this.mapper
            .lookupMapperOfType(LocalConversionMapper.class);
        annotationConfiguration = (AnnotationConfiguration)this.mapper
            .lookupMapperOfType(AnnotationConfiguration.class);
    }

    protected void setupAliases() {
        if (classAliasingMapper == null) {
            return;
        }

        alias("null", Mapper.Null.class);
        alias("int", Integer.class);
        alias("float", Float.class);
        alias("double", Double.class);
        alias("long", Long.class);
        alias("short", Short.class);
        alias("char", Character.class);
        alias("byte", Byte.class);
        alias("boolean", Boolean.class);
        alias("number", Number.class);
        alias("object", Object.class);
        alias("big-int", BigInteger.class);
        alias("big-decimal", BigDecimal.class);

        alias("string-buffer", StringBuffer.class);
        alias("string", String.class);
        alias("java-class", Class.class);
        alias("method", Method.class);
        alias("constructor", Constructor.class);
        alias("field", Field.class);
        alias("date", Date.class);
        alias("uri", URI.class);
        alias("url", URL.class);
        alias("bit-set", BitSet.class);

        alias("map", Map.class);
        alias("entry", Map.Entry.class);
        alias("properties", Properties.class);
        alias("list", List.class);
        alias("set", Set.class);
        alias("sorted-set", SortedSet.class);

        alias("linked-list", LinkedList.class);
        alias("vector", Vector.class);
        alias("tree-map", TreeMap.class);
        alias("tree-set", TreeSet.class);
        alias("hashtable", Hashtable.class);
        
        alias("empty-list", Collections.EMPTY_LIST.getClass());
        alias("empty-map", Collections.EMPTY_MAP.getClass());
        alias("empty-set", Collections.EMPTY_SET.getClass());
        alias("singleton-list", Collections.singletonList(this).getClass());
        alias("singleton-map", Collections.singletonMap(this, null).getClass());
        alias("singleton-set", Collections.singleton(this).getClass());

        if (jvm.supportsAWT()) {
            // Instantiating these two classes starts the AWT system, which is undesirable.
            // Calling loadClass ensures a reference to the class is found but they are not
            // instantiated.
            alias("awt-color", jvm.loadClass("java.awt.Color", false));
            alias("awt-font", jvm.loadClass("java.awt.Font", false));
            alias("awt-text-attribute", jvm.loadClass("java.awt.font.TextAttribute"));
        }

        if (jvm.supportsSQL()) {
            alias("sql-timestamp", jvm.loadClass("java.sql.Timestamp"));
            alias("sql-time", jvm.loadClass("java.sql.Time"));
            alias("sql-date", jvm.loadClass("java.sql.Date"));
        }

        alias("file", File.class);
        alias("locale", Locale.class);
        alias("gregorian-calendar", Calendar.class);

        if (JVM.is14()) {
            aliasDynamically("auth-subject", "javax.security.auth.Subject");
            alias("linked-hash-map", jvm.loadClass("java.util.LinkedHashMap"));
            alias("linked-hash-set", jvm.loadClass("java.util.LinkedHashSet"));
            alias("trace", jvm.loadClass("java.lang.StackTraceElement"));
            alias("currency", jvm.loadClass("java.util.Currency"));
            aliasType("charset", jvm.loadClass("java.nio.charset.Charset"));
        }

        if (JVM.is15()) {
            aliasDynamically("duration", "javax.xml.datatype.Duration");
            alias("concurrent-hash-map", jvm.loadClass("java.util.concurrent.ConcurrentHashMap"));
            alias("enum-set", jvm.loadClass("java.util.EnumSet"));
            alias("enum-map", jvm.loadClass("java.util.EnumMap"));
            alias("string-builder", jvm.loadClass("java.lang.StringBuilder"));
            alias("uuid", jvm.loadClass("java.util.UUID"));
        }
    }

    private void aliasDynamically(String alias, String className) {
        Class type = jvm.loadClass(className);
        if (type != null) {
            alias(alias, type);
        }
    }

    protected void setupDefaultImplementations() {
        if (defaultImplementationsMapper == null) {
            return;
        }
        addDefaultImplementation(HashMap.class, Map.class);
        addDefaultImplementation(ArrayList.class, List.class);
        addDefaultImplementation(HashSet.class, Set.class);
        addDefaultImplementation(TreeSet.class, SortedSet.class);
        addDefaultImplementation(GregorianCalendar.class, Calendar.class);
    }

    protected void setupConverters() {
        final ReflectionConverter reflectionConverter = new ReflectionConverter(
            mapper, reflectionProvider);
        registerConverter(reflectionConverter, PRIORITY_VERY_LOW);

        registerConverter(
            new SerializableConverter(mapper, reflectionProvider, classLoaderReference), PRIORITY_LOW);
        registerConverter(new ExternalizableConverter(mapper, classLoaderReference), PRIORITY_LOW);

        registerConverter(new NullConverter(), PRIORITY_VERY_HIGH);
        registerConverter(new IntConverter(), PRIORITY_NORMAL);
        registerConverter(new FloatConverter(), PRIORITY_NORMAL);
        registerConverter(new DoubleConverter(), PRIORITY_NORMAL);
        registerConverter(new LongConverter(), PRIORITY_NORMAL);
        registerConverter(new ShortConverter(), PRIORITY_NORMAL);
        registerConverter((Converter)new CharConverter(), PRIORITY_NORMAL);
        registerConverter(new BooleanConverter(), PRIORITY_NORMAL);
        registerConverter(new ByteConverter(), PRIORITY_NORMAL);

        registerConverter(new StringConverter(), PRIORITY_NORMAL);
        registerConverter(new StringBufferConverter(), PRIORITY_NORMAL);
        registerConverter(new DateConverter(), PRIORITY_NORMAL);
        registerConverter(new BitSetConverter(), PRIORITY_NORMAL);
        registerConverter(new URIConverter(), PRIORITY_NORMAL);
        registerConverter(new URLConverter(), PRIORITY_NORMAL);
        registerConverter(new BigIntegerConverter(), PRIORITY_NORMAL);
        registerConverter(new BigDecimalConverter(), PRIORITY_NORMAL);

        registerConverter(new ArrayConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new CharArrayConverter(), PRIORITY_NORMAL);
        registerConverter(new CollectionConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new MapConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new TreeMapConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new TreeSetConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new SingletonCollectionConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new SingletonMapConverter(mapper), PRIORITY_NORMAL);
        registerConverter(new PropertiesConverter(), PRIORITY_NORMAL);
        registerConverter((Converter)new EncodedByteArrayConverter(), PRIORITY_NORMAL);

        registerConverter(new FileConverter(), PRIORITY_NORMAL);
        if (jvm.supportsSQL()) {
            registerConverter(new SqlTimestampConverter(), PRIORITY_NORMAL);
            registerConverter(new SqlTimeConverter(), PRIORITY_NORMAL);
            registerConverter(new SqlDateConverter(), PRIORITY_NORMAL);
        }
        
        try {
        // exception can be thrown when security policies are in place (e.g. applets)
        registerConverter(
            new DynamicProxyConverter(mapper, classLoaderReference), PRIORITY_NORMAL);
        } catch( ExceptionInInitializerError ignored ) {}
        
        registerConverter(new JavaClassConverter(classLoaderReference), PRIORITY_NORMAL);
        registerConverter(new JavaMethodConverter(classLoaderReference), PRIORITY_NORMAL);
        registerConverter(new JavaFieldConverter(classLoaderReference), PRIORITY_NORMAL);
//        if (jvm.supportsAWT()) {
//            registerConverter(new FontConverter(), PRIORITY_NORMAL);
//            registerConverter(new ColorConverter(), PRIORITY_NORMAL);
//            try { // Hack2
//            registerConverter(new TextAttributeConverter(), PRIORITY_NORMAL);
//            } catch( ExceptionInInitializerError ignored ) {}
//        }
//        if (jvm.supportsSwing()) {
//            registerConverter(
//                new LookAndFeelConverter(mapper, reflectionProvider), PRIORITY_NORMAL);
//        }
//        registerConverter(new LocaleConverter(), PRIORITY_NORMAL);
//        registerConverter(new GregorianCalendarConverter(), PRIORITY_NORMAL);

//        if (JVM.is14()) {
//            // late bound converters - allows XStream to be compiled on earlier JDKs
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.extended.SubjectConverter",
//                PRIORITY_NORMAL, new Class[]{Mapper.class}, new Object[]{mapper});
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.extended.ThrowableConverter",
//                PRIORITY_NORMAL, new Class[]{Converter.class},
//                new Object[]{reflectionConverter});
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.extended.StackTraceElementConverter",
//                PRIORITY_NORMAL, null, null);
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.extended.CurrencyConverter",
//                PRIORITY_NORMAL, null, null);
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.extended.RegexPatternConverter",
//                PRIORITY_NORMAL, new Class[]{Converter.class},
//                new Object[]{reflectionConverter});
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.extended.CharsetConverter",
//                PRIORITY_NORMAL, null, null);
//        }
//
//        if (JVM.is15()) {
//            // late bound converters - allows XStream to be compiled on earlier JDKs
//            if (jvm.loadClass("javax.xml.datatype.Duration") != null) {
//                registerConverterDynamically(
//                    "com.thoughtworks.xstream.converters.extended.DurationConverter",
//                    PRIORITY_NORMAL, null, null);
//            }
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.enums.EnumConverter", PRIORITY_NORMAL,
//                null, null);
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.enums.EnumSetConverter", PRIORITY_NORMAL,
//                new Class[]{Mapper.class}, new Object[]{mapper});
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.enums.EnumMapConverter", PRIORITY_NORMAL,
//                new Class[]{Mapper.class}, new Object[]{mapper});
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.basic.StringBuilderConverter",
//                PRIORITY_NORMAL, null, null);
//            registerConverterDynamically(
//                "com.thoughtworks.xstream.converters.basic.UUIDConverter", PRIORITY_NORMAL,
//                null, null);
//        }

        registerConverter(
            new SelfStreamingInstanceChecker(reflectionConverter, this), PRIORITY_NORMAL);
    }

//    private void registerConverterDynamically(String className, int priority,
//        Class[] constructorParamTypes, Object[] constructorParamValues) {
//        try {
//            Class type = Class.forName(className, false, classLoaderReference.getReference());
//            Constructor constructor = type.getConstructor(constructorParamTypes);
//            Object instance = constructor.newInstance(constructorParamValues);
//            if (instance instanceof Converter) {
//                registerConverter((Converter)instance, priority);
//            } else if (instance instanceof SingleValueConverter) {
//                registerConverter((SingleValueConverter)instance, priority);
//            }
//        } catch (Exception e) {
//            throw new com.thoughtworks.xstream.InitializationException(
//                "Could not instantiate converter : " + className, e);
//        }
//    }

    protected void setupImmutableTypes() {
        if (immutableTypesMapper == null) {
            return;
        }

        // primitives are always immutable
        addImmutableType(boolean.class);
        addImmutableType(Boolean.class);
        addImmutableType(byte.class);
        addImmutableType(Byte.class);
        addImmutableType(char.class);
        addImmutableType(Character.class);
        addImmutableType(double.class);
        addImmutableType(Double.class);
        addImmutableType(float.class);
        addImmutableType(Float.class);
        addImmutableType(int.class);
        addImmutableType(Integer.class);
        addImmutableType(long.class);
        addImmutableType(Long.class);
        addImmutableType(short.class);
        addImmutableType(Short.class);

        // additional types
        addImmutableType(Mapper.Null.class);
        addImmutableType(BigDecimal.class);
        addImmutableType(BigInteger.class);
        addImmutableType(String.class);
        addImmutableType(URI.class);
        addImmutableType(URL.class);
        addImmutableType(File.class);
        addImmutableType(Class.class);

        addImmutableType(Collections.EMPTY_LIST.getClass());
        addImmutableType(Collections.EMPTY_SET.getClass());
        addImmutableType(Collections.EMPTY_MAP.getClass());

        if (jvm.supportsAWT()) {
            addImmutableTypeDynamically("java.awt.font.TextAttribute");
        }

        if (JVM.is14()) {
            // late bound types - allows XStream to be compiled on earlier JDKs
            addImmutableTypeDynamically("java.nio.charset.Charset");
            addImmutableTypeDynamically("java.util.Currency");
        }
    }

    private void addImmutableTypeDynamically(String className) {
        Class type = jvm.loadClass(className);
        if (type != null) {
            addImmutableType(type);
        }
    }

    public void setMarshallingStrategy(MarshallingStrategy marshallingStrategy) {
        this.marshallingStrategy = marshallingStrategy;
    }

    /**
     * Serialize an object to a pretty-printed XML String.
     * 
     * @throws XStreamException if the object cannot be serialized
     */
    public String toXML(Object obj) {
        Writer writer = new StringWriter();
        toXML(obj, writer);
        return writer.toString();
    }

    /**
     * Serialize an object to the given Writer as pretty-printed XML. The Writer will be flushed
     * afterwards and in case of an exception.
     * 
     * @throws XStreamException if the object cannot be serialized
     */
    public void toXML(Object obj, Writer out) {
        HierarchicalStreamWriter writer = hierarchicalStreamDriver.createWriter(out);
        try {
            marshal(obj, writer);
        } finally {
            writer.flush();
        }
    }

    /**
     * Serialize an object to the given OutputStream as pretty-printed XML. The OutputStream
     * will be flushed afterwards and in case of an exception.
     * 
     * @throws XStreamException if the object cannot be serialized
     */
    public void toXML(Object obj, OutputStream out) {
        HierarchicalStreamWriter writer = hierarchicalStreamDriver.createWriter(out);
        try {
            marshal(obj, writer);
        } finally {
            writer.flush();
        }
    }

    /**
     * Serialize and object to a hierarchical data structure (such as XML).
     * 
     * @throws XStreamException if the object cannot be serialized
     */
    public void marshal(Object obj, HierarchicalStreamWriter writer) {
        marshal(obj, writer, null);
    }

    /**
     * Serialize and object to a hierarchical data structure (such as XML).
     * 
     * @param dataHolder Extra data you can use to pass to your converters. Use this as you
     *            want. If not present, XStream shall create one lazily as needed.
     * @throws XStreamException if the object cannot be serialized
     */
    public void marshal(Object obj, HierarchicalStreamWriter writer, DataHolder dataHolder) {
        marshallingStrategy.marshal(writer, obj, converterLookup, mapper, dataHolder);
    }

    /**
     * Deserialize an object from an XML String.
     * 
     * @throws XStreamException if the object cannot be deserialized
     */
    public Object fromXML(String xml) {
        return fromXML(new StringReader(xml));
    }

    /**
     * Deserialize an object from an XML Reader.
     * 
     * @throws XStreamException if the object cannot be deserialized
     */
    public Object fromXML(Reader reader) {
        return unmarshal(hierarchicalStreamDriver.createReader(reader), null);
    }

    /**
     * Deserialize an object from an XML InputStream.
     * 
     * @throws XStreamException if the object cannot be deserialized
     */
    public Object fromXML(InputStream input) {
        return unmarshal(hierarchicalStreamDriver.createReader(input), null);
    }

    /**
     * Deserialize an object from a URL.
     * 
     * Depending on the parser implementation, some might take the file path as SystemId to
     * resolve additional references.
     * 
     * @throws XStreamException if the object cannot be deserialized
     * @since 1.4
     */
    public Object fromXML(URL url) {
        return fromXML(url, null);
    }

    /**
     * Deserialize an object from a file.
     * 
     * Depending on the parser implementation, some might take the file path as SystemId to
     * resolve additional references.
     * 
     * @throws XStreamException if the object cannot be deserialized
     * @since 1.4
     */
    public Object fromXML(File file) {
        return fromXML(file, null);
    }

    /**
     * Deserialize an object from an XML String, populating the fields of the given root object
     * instead of instantiating a new one. Note, that this is a special use case! With the
     * ReflectionConverter XStream will write directly into the raw memory area of the existing
     * object. Use with care!
     * 
     * @throws XStreamException if the object cannot be deserialized
     */
    public Object fromXML(String xml, Object root) {
        return fromXML(new StringReader(xml), root);
    }

    /**
     * Deserialize an object from an XML Reader, populating the fields of the given root object
     * instead of instantiating a new one. Note, that this is a special use case! With the
     * ReflectionConverter XStream will write directly into the raw memory area of the existing
     * object. Use with care!
     * 
     * @throws XStreamException if the object cannot be deserialized
     */
    public Object fromXML(Reader xml, Object root) {
        return unmarshal(hierarchicalStreamDriver.createReader(xml), root);
    }

    /**
     * Deserialize an object from a URL, populating the fields of the given root
     * object instead of instantiating a new one. Note, that this is a special use case! With
     * the ReflectionConverter XStream will write directly into the raw memory area of the
     * existing object. Use with care!
     * 
     * Depending on the parser implementation, some might take the file path as SystemId to
     * resolve additional references.
     * 
     * @throws XStreamException if the object cannot be deserialized
     * @since 1.4
     */
    public Object fromXML(URL url, Object root) {
        return unmarshal(hierarchicalStreamDriver.createReader(url), root);
    }

    /**
     * Deserialize an object from a file, populating the fields of the given root
     * object instead of instantiating a new one. Note, that this is a special use case! With
     * the ReflectionConverter XStream will write directly into the raw memory area of the
     * existing object. Use with care!
     * 
     * Depending on the parser implementation, some might take the file path as SystemId to
     * resolve additional references.
     * 
     * @throws XStreamException if the object cannot be deserialized
     * @since 1.4
     */
    public Object fromXML(File file, Object root) {
        HierarchicalStreamReader reader = hierarchicalStreamDriver.createReader(file);
        try {
            return unmarshal(reader, root);
        } finally {
            reader.close();
        }
    }

    /**
     * Deserialize an object from an XML InputStream, populating the fields of the given root
     * object instead of instantiating a new one. Note, that this is a special use case! With
     * the ReflectionConverter XStream will write directly into the raw memory area of the
     * existing object. Use with care!
     * 
     * @throws XStreamException if the object cannot be deserialized
     */
    public Object fromXML(InputStream input, Object root) {
        return unmarshal(hierarchicalStreamDriver.createReader(input), root);
    }

    /**
     * Deserialize an object from a hierarchical data structure (such as XML).
     * 
     * @throws XStreamException if the object cannot be deserialized
     */
    public Object unmarshal(HierarchicalStreamReader reader) {
        return unmarshal(reader, null, null);
    }

    /**
     * Deserialize an object from a hierarchical data structure (such as XML), populating the
     * fields of the given root object instead of instantiating a new one. Note, that this is a
     * special use case! With the ReflectionConverter XStream will write directly into the raw
     * memory area of the existing object. Use with care!
     * 
     * @throws XStreamException if the object cannot be deserialized
     */
    public Object unmarshal(HierarchicalStreamReader reader, Object root) {
        return unmarshal(reader, root, null);
    }

    /**
     * Deserialize an object from a hierarchical data structure (such as XML).
     * 
     * @param root If present, the passed in object will have its fields populated, as opposed
     *            to XStream creating a new instance. Note, that this is a special use case!
     *            With the ReflectionConverter XStream will write directly into the raw memory
     *            area of the existing object. Use with care!
     * @param dataHolder Extra data you can use to pass to your converters. Use this as you
     *            want. If not present, XStream shall create one lazily as needed.
     * @throws XStreamException if the object cannot be deserialized
     */
    public Object unmarshal(HierarchicalStreamReader reader, Object root, DataHolder dataHolder) {
        try {
            return marshallingStrategy.unmarshal(
                root, reader, dataHolder, converterLookup, mapper);

        } catch (ConversionException e) {
            Package pkg = getClass().getPackage();
            e.add("version", pkg != null ? pkg.getImplementationVersion() : "not available");
            throw e;
        }
    }

    /**
     * Alias a Class to a shorter name to be used in XML elements.
     * 
     * @param name Short name
     * @param type Type to be aliased
     * @throws InitializationException if no {@link ClassAliasingMapper} is available
     */
    public void alias(String name, Class type) {
        if (classAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + ClassAliasingMapper.class.getName()
                + " available");
        }
        classAliasingMapper.addClassAlias(name, type);
    }

    /**
     * Alias a type to a shorter name to be used in XML elements. Any class that is assignable
     * to this type will be aliased to the same name.
     * 
     * @param name Short name
     * @param type Type to be aliased
     * @since 1.2
     * @throws InitializationException if no {@link ClassAliasingMapper} is available
     */
    public void aliasType(String name, Class type) {
        if (classAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + ClassAliasingMapper.class.getName()
                + " available");
        }
        classAliasingMapper.addTypeAlias(name, type);
    }

    /**
     * Alias a Class to a shorter name to be used in XML elements.
     * 
     * @param name Short name
     * @param type Type to be aliased
     * @param defaultImplementation Default implementation of type to use if no other specified.
     * @throws InitializationException if no {@link DefaultImplementationsMapper} or no
     *             {@link ClassAliasingMapper} is available
     */
    public void alias(String name, Class type, Class defaultImplementation) {
        alias(name, type);
        addDefaultImplementation(defaultImplementation, type);
    }

    /**
     * Alias a package to a shorter name to be used in XML elements.
     * 
     * @param name Short name
     * @param pkgName package to be aliased
     * @throws InitializationException if no {@link DefaultImplementationsMapper} or no
     *             {@link PackageAliasingMapper} is available
     * @since 1.3.1
     */
    public void aliasPackage(String name, String pkgName) {
        if (packageAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + PackageAliasingMapper.class.getName()
                + " available");
        }
        packageAliasingMapper.addPackageAlias(name, pkgName);
    }

    /**
     * Create an alias for a field name.
     * 
     * @param alias the alias itself
     * @param definedIn the type that declares the field
     * @param fieldName the name of the field
     * @throws InitializationException if no {@link FieldAliasingMapper} is available
     */
    public void aliasField(String alias, Class definedIn, String fieldName) {
        if (fieldAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + FieldAliasingMapper.class.getName()
                + " available");
        }
        fieldAliasingMapper.addFieldAlias(alias, definedIn, fieldName);
    }

    /**
     * Create an alias for an attribute
     * 
     * @param alias the alias itself
     * @param attributeName the name of the attribute
     * @throws InitializationException if no {@link AttributeAliasingMapper} is available
     */
    public void aliasAttribute(String alias, String attributeName) {
        if (attributeAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + AttributeAliasingMapper.class.getName()
                + " available");
        }
        attributeAliasingMapper.addAliasFor(attributeName, alias);
    }

    /**
     * Create an alias for a system attribute. XStream will not write a system attribute if its
     * alias is set to <code>null</code>. However, this is not reversible, i.e. deserialization
     * of the result is likely to fail afterwards and will not produce an object equal to the
     * originally written one.
     * 
     * @param alias the alias itself (may be <code>null</code>)
     * @param systemAttributeName the name of the system attribute
     * @throws InitializationException if no {@link SystemAttributeAliasingMapper} is available
     * @since 1.3.1
     */
    public void aliasSystemAttribute(String alias, String systemAttributeName) {
        if (systemAttributeAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + SystemAttributeAliasingMapper.class.getName()
                + " available");
        }
        systemAttributeAliasingMapper.addAliasFor(systemAttributeName, alias);
    }

    /**
     * Create an alias for an attribute.
     * 
     * @param definedIn the type where the attribute is defined
     * @param attributeName the name of the attribute
     * @param alias the alias itself
     * @throws InitializationException if no {@link AttributeAliasingMapper} is available
     * @since 1.2.2
     */
    public void aliasAttribute(Class definedIn, String attributeName, String alias) {
        aliasField(alias, definedIn, attributeName);
        useAttributeFor(definedIn, attributeName);
    }

    /**
     * Use an attribute for a field or a specific type.
     * 
     * @param fieldName the name of the field
     * @param type the Class of the type to be rendered as XML attribute
     * @throws InitializationException if no {@link AttributeMapper} is available
     * @since 1.2
     */
    public void useAttributeFor(String fieldName, Class type) {
        if (attributeMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + AttributeMapper.class.getName()
                + " available");
        }
        attributeMapper.addAttributeFor(fieldName, type);
    }

    /**
     * Use an attribute for a field declared in a specific type.
     * 
     * @param fieldName the name of the field
     * @param definedIn the Class containing such field
     * @throws InitializationException if no {@link AttributeMapper} is available
     * @since 1.2.2
     */
    public void useAttributeFor(Class definedIn, String fieldName) {
        if (attributeMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + AttributeMapper.class.getName()
                + " available");
        }
        attributeMapper.addAttributeFor(definedIn, fieldName);
    }

    /**
     * Use an attribute for an arbitrary type.
     * 
     * @param type the Class of the type to be rendered as XML attribute
     * @throws InitializationException if no {@link AttributeMapper} is available
     * @since 1.2
     */
    public void useAttributeFor(Class type) {
        if (attributeMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + AttributeMapper.class.getName()
                + " available");
        }
        attributeMapper.addAttributeFor(type);
    }

    /**
     * Associate a default implementation of a class with an object. Whenever XStream encounters
     * an instance of this type, it will use the default implementation instead. For example,
     * java.util.ArrayList is the default implementation of java.util.List.
     * 
     * @param defaultImplementation
     * @param ofType
     * @throws InitializationException if no {@link DefaultImplementationsMapper} is available
     */
    public void addDefaultImplementation(Class defaultImplementation, Class ofType) {
        if (defaultImplementationsMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + DefaultImplementationsMapper.class.getName()
                + " available");
        }
        defaultImplementationsMapper.addDefaultImplementation(defaultImplementation, ofType);
    }

    /**
     * Add immutable types. The value of the instances of these types will always be written
     * into the stream even if they appear multiple times.
     * 
     * @throws InitializationException if no {@link ImmutableTypesMapper} is available
     */
    public void addImmutableType(Class type) {
        if (immutableTypesMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + ImmutableTypesMapper.class.getName()
                + " available");
        }
        immutableTypesMapper.addImmutableType(type);
    }

    public void registerConverter(Converter converter) {
        registerConverter(converter, PRIORITY_NORMAL);
    }

    public void registerConverter(Converter converter, int priority) {
        if (converterRegistry != null) {
            converterRegistry.registerConverter(converter, priority);
        }
    }

    public void registerConverter(SingleValueConverter converter) {
        registerConverter(converter, PRIORITY_NORMAL);
    }

    public void registerConverter(SingleValueConverter converter, int priority) {
        if (converterRegistry != null) {
            converterRegistry.registerConverter(
                new SingleValueConverterWrapper(converter), priority);
        }
    }

    /**
     * Register a local {@link Converter} for a field.
     * 
     * @param definedIn the class type the field is defined in
     * @param fieldName the field name
     * @param converter the converter to use
     * @since 1.3
     */
    public void registerLocalConverter(Class definedIn, String fieldName, Converter converter) {
        if (localConversionMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + LocalConversionMapper.class.getName()
                + " available");
        }
        localConversionMapper.registerLocalConverter(definedIn, fieldName, converter);
    }

    /**
     * Register a local {@link SingleValueConverter} for a field.
     * 
     * @param definedIn the class type the field is defined in
     * @param fieldName the field name
     * @param converter the converter to use
     * @since 1.3
     */
    public void registerLocalConverter(Class definedIn, String fieldName,
        SingleValueConverter converter) {
        registerLocalConverter(
            definedIn, fieldName, (Converter)new SingleValueConverterWrapper(converter));
    }

    /**
     * Retrieve the {@link Mapper}. This is by default a chain of {@link MapperWrapper
     * MapperWrappers}.
     * 
     * @return the mapper
     * @since 1.2
     */
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * Retrieve the {@link ReflectionProvider} in use.
     * 
     * @return the mapper
     * @since 1.2.1
     */
    public ReflectionProvider getReflectionProvider() {
        return reflectionProvider;
    }

    public ConverterLookup getConverterLookup() {
        return converterLookup;
    }

    /**
     * Change mode for dealing with duplicate references. Valid values are
     * <code>XPATH_ABSOLUTE_REFERENCES</code>, <code>XPATH_RELATIVE_REFERENCES</code>,
     * <code>XStream.ID_REFERENCES</code> and <code>XStream.NO_REFERENCES</code>.
     * 
     * @throws IllegalArgumentException if the mode is not one of the declared types
     * @see #XPATH_ABSOLUTE_REFERENCES
     * @see #XPATH_RELATIVE_REFERENCES
     * @see #ID_REFERENCES
     * @see #NO_REFERENCES
     */
    public void setMode(int mode) {
        switch (mode) {
        case NO_REFERENCES:
            setMarshallingStrategy(new TreeMarshallingStrategy());
            break;
        case ID_REFERENCES:
            setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
            break;
        case XPATH_RELATIVE_REFERENCES:
            setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy(
                ReferenceByXPathMarshallingStrategy.RELATIVE));
            break;
        case XPATH_ABSOLUTE_REFERENCES:
            setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy(
                ReferenceByXPathMarshallingStrategy.ABSOLUTE));
            break;
        case SINGLE_NODE_XPATH_RELATIVE_REFERENCES:
            setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy(
                  ReferenceByXPathMarshallingStrategy.RELATIVE 
                | ReferenceByXPathMarshallingStrategy.SINGLE_NODE));
            break;
        case SINGLE_NODE_XPATH_ABSOLUTE_REFERENCES:
            setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy(
                  ReferenceByXPathMarshallingStrategy.ABSOLUTE 
                | ReferenceByXPathMarshallingStrategy.SINGLE_NODE));
            break;
        default:
            throw new IllegalArgumentException("Unknown mode : " + mode);
        }
    }

    /**
     * Adds a default implicit collection which is used for any unmapped XML tag.
     * 
     * @param ownerType class owning the implicit collection
     * @param fieldName name of the field in the ownerType. This field must be a concrete
     *            collection type or matching the default implementation type of the collection
     *            type.
     */
    public void addImplicitCollection(Class ownerType, String fieldName) {
        addImplicitCollection(ownerType, fieldName, null, null);
    }

    /**
     * Adds implicit collection which is used for all items of the given itemType.
     * 
     * @param ownerType class owning the implicit collection
     * @param fieldName name of the field in the ownerType. This field must be a concrete
     *            collection type or matching the default implementation type of the collection
     *            type.
     * @param itemType type of the items to be part of this collection
     * @throws InitializationException if no {@link ImplicitCollectionMapper} is available
     */
    public void addImplicitCollection(Class ownerType, String fieldName, Class itemType) {
        addImplicitCollection(ownerType, fieldName, null, itemType);
    }

    /**
     * Adds implicit collection which is used for all items of the given element name defined by
     * itemFieldName.
     * 
     * @param ownerType class owning the implicit collection
     * @param fieldName name of the field in the ownerType. This field must be a concrete
     *            collection type or matching the default implementation type of the collection
     *            type.
     * @param itemFieldName element name of the implicit collection
     * @param itemType item type to be aliases be the itemFieldName
     * @throws InitializationException if no {@link ImplicitCollectionMapper} is available
     */
    public void addImplicitCollection(Class ownerType, String fieldName, String itemFieldName,
        Class itemType) {
        addImplicitMap(ownerType, fieldName, itemFieldName, itemType, null);
    }

    /**
     * Adds an implicit array.
     * 
     * @param ownerType class owning the implicit array
     * @param fieldName name of the array field
     * @since 1.4 
     */
    public void addImplicitArray(Class ownerType, String fieldName) {
        addImplicitCollection(ownerType, fieldName);
    }

    /**
     * Adds an implicit array which is used for all items of the given itemType when the array
     * type matches.
     * 
     * @param ownerType class owning the implicit array
     * @param fieldName name of the array field in the ownerType
     * @param itemType type of the items to be part of this array
     * @throws InitializationException if no {@link ImplicitCollectionMapper} is available or the
     * array type does not match the itemType
     * @since 1.4 
     */
    public void addImplicitArray(Class ownerType, String fieldName, Class itemType) {
        addImplicitCollection(ownerType, fieldName, itemType);
    }

    /**
     * Adds an implicit array which is used for all items of the given element name defined by
     * itemName.
     * 
     * @param ownerType class owning the implicit array
     * @param fieldName name of the array field in the ownerType
     * @param itemName alias name of the items
     * @throws InitializationException if no {@link ImplicitCollectionMapper} is available
     * @since 1.4 
     */
    public void addImplicitArray(Class ownerType, String fieldName, String itemName) {
        addImplicitCollection(ownerType, fieldName, itemName, null);
    }

    /**
     * Adds an implicit map.
     * 
     * @param ownerType class owning the implicit map
     * @param fieldName name of the field in the ownerType. This field must be a concrete
     *            map type or matching the default implementation type of the map
     *            type.
     * @param itemType type of the items to be part of this map as value
     * @param keyFieldName the name of the field of the itemType that is used for the key in the map
     * @since 1.4 
     */
    public void addImplicitMap(Class ownerType, String fieldName, Class itemType, String keyFieldName) {
        addImplicitMap(ownerType, fieldName, null, itemType, keyFieldName);
    }

    /**
     * Adds an implicit map.
     * 
     * @param ownerType class owning the implicit map
     * @param fieldName name of the field in the ownerType. This field must be a concrete
     *            map type or matching the default implementation type of the map
     *            type.
     * @param itemName alias name of the items
     * @param itemType type of the items to be part of this map as value
     * @param keyFieldName the name of the field of the itemType that is used for the key in the map
     * @since 1.4 
     */
    public void addImplicitMap(Class ownerType, String fieldName, String itemName, 
        Class itemType, String keyFieldName) {
        if (implicitCollectionMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + ImplicitCollectionMapper.class.getName()
                + " available");
        }
        implicitCollectionMapper.add(ownerType, fieldName, itemName, itemType, keyFieldName);
    }

    /**
     * Create a DataHolder that can be used to pass data to the converters. The DataHolder is
     * provided with a call to {@link #marshal(Object, HierarchicalStreamWriter, DataHolder)} or
     * {@link #unmarshal(HierarchicalStreamReader, Object, DataHolder)}.
     * 
     * @return a new {@link DataHolder}
     */
    public DataHolder newDataHolder() {
        return new MapBackedDataHolder();
    }

    /**
     * Creates an ObjectOutputStream that serializes a stream of objects to the writer using
     * XStream.
     * <p>
     * To change the name of the root element (from &lt;object-stream&gt;), use
     * {@link #createObjectOutputStream(java.io.Writer, String)}.
     * </p>
     * 
     * @see #createObjectOutputStream(com.thoughtworks.xstream.io.HierarchicalStreamWriter,
     *      String)
     * @see #createObjectInputStream(com.thoughtworks.xstream.io.HierarchicalStreamReader)
     * @since 1.0.3
     */
    public ObjectOutputStream createObjectOutputStream(Writer writer) throws IOException {
        return createObjectOutputStream(
            hierarchicalStreamDriver.createWriter(writer), "object-stream");
    }

    /**
     * Creates an ObjectOutputStream that serializes a stream of objects to the writer using
     * XStream.
     * <p>
     * To change the name of the root element (from &lt;object-stream&gt;), use
     * {@link #createObjectOutputStream(java.io.Writer, String)}.
     * </p>
     * 
     * @see #createObjectOutputStream(com.thoughtworks.xstream.io.HierarchicalStreamWriter,
     *      String)
     * @see #createObjectInputStream(com.thoughtworks.xstream.io.HierarchicalStreamReader)
     * @since 1.0.3
     */
    public ObjectOutputStream createObjectOutputStream(HierarchicalStreamWriter writer)
        throws IOException {
        return createObjectOutputStream(writer, "object-stream");
    }

    /**
     * Creates an ObjectOutputStream that serializes a stream of objects to the writer using
     * XStream.
     * 
     * @see #createObjectOutputStream(com.thoughtworks.xstream.io.HierarchicalStreamWriter,
     *      String)
     * @see #createObjectInputStream(com.thoughtworks.xstream.io.HierarchicalStreamReader)
     * @since 1.0.3
     */
    public ObjectOutputStream createObjectOutputStream(Writer writer, String rootNodeName)
        throws IOException {
        return createObjectOutputStream(
            hierarchicalStreamDriver.createWriter(writer), rootNodeName);
    }

    /**
     * Creates an ObjectOutputStream that serializes a stream of objects to the OutputStream
     * using XStream.
     * <p>
     * To change the name of the root element (from &lt;object-stream&gt;), use
     * {@link #createObjectOutputStream(java.io.Writer, String)}.
     * </p>
     * 
     * @see #createObjectOutputStream(com.thoughtworks.xstream.io.HierarchicalStreamWriter,
     *      String)
     * @see #createObjectInputStream(com.thoughtworks.xstream.io.HierarchicalStreamReader)
     * @since 1.3
     */
    public ObjectOutputStream createObjectOutputStream(OutputStream out) throws IOException {
        return createObjectOutputStream(
            hierarchicalStreamDriver.createWriter(out), "object-stream");
    }

    /**
     * Creates an ObjectOutputStream that serializes a stream of objects to the OutputStream
     * using XStream.
     * 
     * @see #createObjectOutputStream(com.thoughtworks.xstream.io.HierarchicalStreamWriter,
     *      String)
     * @see #createObjectInputStream(com.thoughtworks.xstream.io.HierarchicalStreamReader)
     * @since 1.3
     */
    public ObjectOutputStream createObjectOutputStream(OutputStream out, String rootNodeName)
        throws IOException {
        return createObjectOutputStream(
            hierarchicalStreamDriver.createWriter(out), rootNodeName);
    }

    /**
     * Creates an ObjectOutputStream that serializes a stream of objects to the writer using
     * XStream.
     * <p>
     * Because an ObjectOutputStream can contain multiple items and XML only allows a single
     * root node, the stream must be written inside an enclosing node.
     * </p>
     * <p>
     * It is necessary to call ObjectOutputStream.close() when done, otherwise the stream will
     * be incomplete.
     * </p>
     * <h3>Example</h3>
     * 
     * <pre>
     *  ObjectOutputStream out = xstream.createObjectOutputStream(aWriter, &quot;things&quot;);
     *   out.writeInt(123);
     *   out.writeObject(&quot;Hello&quot;);
     *   out.writeObject(someObject)
     *   out.close();
     * </pre>
     * 
     * @param writer The writer to serialize the objects to.
     * @param rootNodeName The name of the root node enclosing the stream of objects.
     * @see #createObjectInputStream(com.thoughtworks.xstream.io.HierarchicalStreamReader)
     * @since 1.0.3
     */
    public ObjectOutputStream createObjectOutputStream(final HierarchicalStreamWriter writer,
        String rootNodeName) throws IOException {
        final StatefulWriter statefulWriter = new StatefulWriter(writer);
        statefulWriter.startNode(rootNodeName, null);
        return new CustomObjectOutputStream(new CustomObjectOutputStream.StreamCallback() {
            public void writeToStream(Object object) {
                marshal(object, statefulWriter);
            }

            public void writeFieldsToStream(Map fields) throws NotActiveException {
                throw new NotActiveException("not in call to writeObject");
            }

            public void defaultWriteObject() throws NotActiveException {
                throw new NotActiveException("not in call to writeObject");
            }

            public void flush() {
                statefulWriter.flush();
            }

            public void close() {
                if (statefulWriter.state() != StatefulWriter.STATE_CLOSED) {
                    statefulWriter.endNode();
                    statefulWriter.close();
                }
            }
        });
    }

    /**
     * Creates an ObjectInputStream that deserializes a stream of objects from a reader using
     * XStream.
     * 
     * @see #createObjectInputStream(com.thoughtworks.xstream.io.HierarchicalStreamReader)
     * @see #createObjectOutputStream(com.thoughtworks.xstream.io.HierarchicalStreamWriter,
     *      String)
     * @since 1.0.3
     */
    public ObjectInputStream createObjectInputStream(Reader xmlReader) throws IOException {
        return createObjectInputStream(hierarchicalStreamDriver.createReader(xmlReader));
    }

    /**
     * Creates an ObjectInputStream that deserializes a stream of objects from an InputStream
     * using XStream.
     * 
     * @see #createObjectInputStream(com.thoughtworks.xstream.io.HierarchicalStreamReader)
     * @see #createObjectOutputStream(com.thoughtworks.xstream.io.HierarchicalStreamWriter,
     *      String)
     * @since 1.3
     */
    public ObjectInputStream createObjectInputStream(InputStream in) throws IOException {
        return createObjectInputStream(hierarchicalStreamDriver.createReader(in));
    }

    /**
     * Creates an ObjectInputStream that deserializes a stream of objects from a reader using
     * XStream. <h3>Example</h3>
     * 
     * <pre>
     * ObjectInputStream in = xstream.createObjectOutputStream(aReader);
     * int a = out.readInt();
     * Object b = out.readObject();
     * Object c = out.readObject();
     * </pre>
     * 
     * @see #createObjectOutputStream(com.thoughtworks.xstream.io.HierarchicalStreamWriter,
     *      String)
     * @since 1.0.3
     */
    public ObjectInputStream createObjectInputStream(final HierarchicalStreamReader reader)
        throws IOException {
        return new CustomObjectInputStream(new CustomObjectInputStream.StreamCallback() {
            public Object readFromStream() throws EOFException {
                if (!reader.hasMoreChildren()) {
                    throw new EOFException();
                }
                reader.moveDown();
                Object result = unmarshal(reader);
                reader.moveUp();
                return result;
            }

            public Map readFieldsFromStream() throws IOException {
                throw new NotActiveException("not in call to readObject");
            }

            public void defaultReadObject() throws NotActiveException {
                throw new NotActiveException("not in call to readObject");
            }

            public void registerValidation(ObjectInputValidation validation, int priority)
                throws NotActiveException {
                throw new NotActiveException("stream inactive");
            }

            public void close() {
                reader.close();
            }
        }, classLoaderReference);
    }

    /**
     * Change the ClassLoader XStream uses to load classes. Creating an XStream instance it will
     * register for all kind of classes and types of the current JDK, but not for any 3rd party
     * type. To ensure that all other types are loaded with your class loader, you should call
     * this method as early as possible - or consider to provide the class loader directly in
     * the constructor.
     * 
     * @since 1.1.1
     */
    public void setClassLoader(XStreamClassLoader classLoader) {
        classLoaderReference.setReference(classLoader);
    }

    /**
     * Retrieve the ClassLoader XStream uses to load classes.
     * 
     * @since 1.1.1
     */
    public XStreamClassLoader getClassLoader() {
        return classLoaderReference.getReference();
    }

    /**
     * Prevents a field from being serialized. To omit a field you must always provide the
     * declaring type and not necessarily the type that is converted.
     * 
     * @since 1.1.3
     * @throws InitializationException if no {@link FieldAliasingMapper} is available
     */
    public void omitField(Class definedIn, String fieldName) {
        if (fieldAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + FieldAliasingMapper.class.getName()
                + " available");
        }
        fieldAliasingMapper.omitField(definedIn, fieldName);
    }

    /**
     * Process the annotations of the given types and configure the XStream.
     * 
     * @param types the types with XStream annotations
     * @since 1.3
     */
    public void processAnnotations(final Class[] types) {
        if (annotationConfiguration == null) {
            throw new com.thoughtworks.xstream.InitializationException("No "
                + ANNOTATION_MAPPER_TYPE
                + " available");
        }
        annotationConfiguration.processAnnotations(types);
    }

    /**
     * Process the annotations of the given type and configure the XStream. A call of this
     * method will automatically turn the auto-detection mode for annotations off.
     * 
     * @param type the type with XStream annotations
     * @since 1.3
     */
    public void processAnnotations(final Class type) {
        processAnnotations(new Class[]{type});
    }

    /**
     * Set the auto-detection mode of the AnnotationMapper. Note that auto-detection implies
     * that the XStream is configured while it is processing the XML steams. This is a potential
     * concurrency problem. Also is it technically not possible to detect all class aliases at
     * deserialization. You have been warned!
     * 
     * @param mode <code>true</code> if annotations are auto-detected
     * @since 1.3
     */
    public void autodetectAnnotations(boolean mode) {
        if (annotationConfiguration != null) {
            annotationConfiguration.autodetectAnnotations(mode);
        }
    }

    /**
     * @deprecated As of 1.3, use {@link InitializationException} instead
     */
    public static class InitializationException extends XStreamException {
        /**
         * @deprecated As of 1.3, use {@link InitializationException} instead
         */
        public InitializationException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * @deprecated As of 1.3, use {@link InitializationException} instead
         */
        public InitializationException(String message) {
            super(message);
        }
    }

    private Object readResolve() {
        jvm = new JVM();
        return this;
    }

}