/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.io;

import boofcv.BoofVersion;
import boofcv.struct.Configuration;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static boofcv.io.calibration.CalibrationIO.createYmlObject;

/**
 * Serializes any BoofCV Config* into a yaml file
 *
 * @author Peter Abeles
 */
public class SerializeConfigYaml {
	public static void serialize( Configuration config, @Nullable Object canonical, Writer writer ) {
		Map<String, Object> state = new HashMap<>();

		// Add some version info to make debugging in the future easier
		state.put("BoofCV.Version", BoofVersion.VERSION);
		state.put("BoofCV.GIT_SHA", BoofVersion.GIT_SHA);
		state.put(config.getClass().getName(), serialize(config, canonical));

		// Print what this is at the top of the file
		try {
			writer.write("# Serialized " + config.getClass().getName() + "\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		Yaml yaml = createYmlObject();
		yaml.dump(state, writer);
	}

	/**
	 * Serializes the specified config. If a 'canonical' reference is provided then only what is not identical
	 * in value to the canonical is serialized.
	 *
	 * @param config Object that is to be serialized
	 * @param canonical Canonical object.
	 */
	static Map<String, Object> serialize( Object config, @Nullable Object canonical ) {
		Map<String, Object> state = new HashMap<>();
		Class type = config.getClass();
		try {
			Field[] fields = type.getFields();
			for (Field f : fields) {
				if (f.getType().isEnum() || f.getType().isPrimitive() || f.getType().getName().equals("java.lang.String")) {
					// Only add if they are not identical
					if (canonical == null || !f.get(config).equals(f.get(canonical)))
						state.put(f.getName(), f.get(config));
				} else {
					try {
						// All Configuration must have setTo()
						// We don't check to see if implements Configuration to allow objects outside of BoofCV
						// to work.
						type.getMethod("setTo", type);
					} catch (NoSuchMethodException e) {
						// This is intentionally annoying as a custom class specific serialization solution
						// needs to be created. For now, it will complain and skip
						System.err.println("Referenced object which is not enum, primitive, or a valid class. name=" + f.getName());
						continue;
					}
					Map<String, Object> result = canonical != null ?
							serialize(f.get(config), f.get(canonical)) : serialize(f.get(config), null);
					// If everything is identical then the returned object will be empty
					if (!result.isEmpty())
						state.put(f.getName(), result);
				}
			}
			return state;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T extends Configuration> T deserialize( Reader reader ) {
		Yaml yaml = createYmlObject();
		Map<String, Object> state = yaml.load(reader);
		try {
			for (String key : state.keySet()) {
				if (key.startsWith("BoofCV"))
					continue;
				T config = (T)Class.forName(key).getConstructor().newInstance();
				deserialize(config, (Map<String, Object>)state.get(key));
				return config;
			}
		} catch (InstantiationException | IllegalAccessException |
				InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException("Couldn't find object to deserialize");
	}

	private static void deserialize( Object parent, Map<String, Object> state ) {
		Class type = parent.getClass();
		try {
			for (String key : state.keySet()) {
				Field f = type.getField(key);
				Class ftype = f.getType();
				if (ftype.isEnum()) {
					f.set(parent, state.get(key));
				} else if (ftype.isPrimitive()) {
					Object value = state.get(key);
					if (ftype == boolean.class)
						f.set(parent, ((Boolean)value).booleanValue());
					else if (ftype == byte.class)
						f.set(parent, ((Number)value).byteValue());
					else if (ftype == char.class)
						f.set(parent, ((Character)value).charValue());
					else if (ftype == short.class)
						f.set(parent, ((Number)value).shortValue());
					else if (ftype == int.class)
						f.set(parent, ((Number)value).intValue());
					else if (ftype == long.class)
						f.set(parent, ((Number)value).longValue());
					else if (ftype == float.class)
						f.set(parent, ((Number)value).floatValue());
					else if (ftype == double.class)
						f.set(parent, ((Number)value).doubleValue());
					else
						throw new RuntimeException("Unknown primitive " + ftype);
				} else if (ftype.getName().equals("java.lang.String")) {
					f.set(parent, state.get(key));
				} else {
					Object child = ftype.getConstructor().newInstance();
					deserialize(child, (Map<String, Object>)state.get(key));
					Class c = child.getClass();
					c.getMethod("setTo", c).invoke(f.get(parent), child);
				}
			}
		} catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException |
				InstantiationException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
