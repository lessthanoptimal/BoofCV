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

package boofcv.io.serialize;

import boofcv.struct.Configuration;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for java serialization of public field variables. Custom error handling is provided by
 * the {@link #errorHandler} function. By default any error will throw an exception.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway", "rawtypes", "unchecked"})
public class SerializeFieldsYamlBase {

	/** Used to provide custom error handling */
	public HandleError errorHandler = ( type, description, e ) -> {
		if (e != null)
			throw e;
		else throw new RuntimeException(description);
	};

	/**
	 * Serializes the specified config. If a 'canonical' reference is provided then only what is not identical
	 * in value to the canonical is serialized.
	 *
	 * @param config Object that is to be serialized
	 * @param canonical Canonical object.
	 */
	public Map<String, Object> serialize( Object config, @Nullable Object canonical ) {
		Map<String, Object> state = new HashMap<>();
		Class type = config.getClass();
		Field[] fields = type.getFields();
		// Get a list of active fields, if a list is specified by the configuration
		List<String> active = new ArrayList<>();
		if (config instanceof Configuration) {
			active = ((Configuration)config).serializeActiveFields();
		}
		for (Field f : fields) {
			if (!(active.isEmpty() || active.contains(f.getName())))
				continue;

			try {
				if (f.getType().isEnum() || f.getType().isPrimitive() || f.getType().getName().equals("java.lang.String")) {
					try {
						// Only add if they are not identical
						Object targetValue = f.get(config);
						Object canonicalValue = canonical == null ? null : f.get(canonical);
						if (canonicalValue == null && targetValue == null)
							continue;
						if (targetValue == null || !targetValue.equals(canonicalValue))
							state.put(f.getName(), f.getType().isEnum() ? ((Enum<?>)targetValue).name() : targetValue);
					} catch (RuntimeException e) {
						errorHandler.handle(ErrorType.MISC, "class=" + type.getSimpleName() + " field=" + f.getName(), e);
					}
					continue;
				}

				// FastArray are a special case. Serialize each element in the list
				if (FastAccess.class.isAssignableFrom(f.getType())) {
					FastAccess<?> list = (FastAccess<?>)f.get(config);

					// See if the lists are identical. If they are then skip them
					escape:
					if (canonical != null) {
						FastAccess<?> listCanon = (FastAccess<?>)f.get(canonical);

						if (list.size() != listCanon.size())
							break escape;
						if (list.isEmpty())
							continue;
						for (int i = 0; i < list.size(); i++) {
							if (!list.get(i).equals(listCanon.get(i)))
								break escape;
						}
						continue;
					}

					// Encode the list. Basic types are just copied. Everything else is serialized.
					Class<?> itemType = list.type;
					boolean basic = itemType.isEnum() || itemType.isPrimitive() || itemType.getName().equals("java.lang.String");

					// Create the canonical if possible
					Object canonicalOfData = null;
					if (canonical != null && !basic) {
						try {
							canonicalOfData = list.type.getConstructor().newInstance();
						} catch (Exception e) {
							errorHandler.handle(ErrorType.MISC, "Failed to create instance of '" + list.type.getSimpleName() + "'", null);
						}
					}

					List<Object> serializedList = new ArrayList<>();
					for (int i = 0; i < list.size(); i++) {
						Object value = list.get(i);
						if (basic) {
							serializedList.add(itemType.isEnum() ? ((Enum<?>)value).name() : value);
						} else {
							serializedList.add(serialize(value, canonicalOfData));
						}
					}
					state.put(f.getName(), serializedList);
					continue;
				}

				if (List.class.isAssignableFrom(f.getType())) {
					errorHandler.handle(ErrorType.UNSUPPORTED, "Can't serialize lists. Use FastArray instead " +
							"since it specifies the item type. name=" + f.getName(), null);
					continue;
				}

				// handle primitive arrays
				if (f.getType().isArray()) {
					state.put(f.getName(), f.get(config));
					continue;
				}

				try {
					// All Configuration must have setTo()
					// We don't check to see if implements Configuration to allow objects outside of BoofCV
					// to work.
					type.getMethod("setTo", type);
				} catch (NoSuchMethodException e) {
					// This is intentionally annoying as a custom class specific serialization solution
					// needs to be created. For now, it will complain and skip
					errorHandler.handle(ErrorType.UNSUPPORTED, "Referenced object which is not enum, primitive, " +
							"or a valid class. class='" + type.getSimpleName() + "' field_name='" + f.getName() + "'", null);
					continue;
				}
				Map<String, Object> result = canonical != null ?
						serialize(f.get(config), f.get(canonical)) : serialize(f.get(config), null);
				// If everything is identical then the returned object will be empty
				if (!result.isEmpty())
					state.put(f.getName(), result);
			} catch (IllegalAccessException e) {
				errorHandler.handle(ErrorType.REFLECTION,
						String.format("IllegalAccess. class='%s' field='%s'", type.getSimpleName(), f.getName()),
						new RuntimeException(e));
			}
		}
		return state;
	}

	protected void deserialize( Object parent, Map<String, Object> state ) {
		Class type = parent.getClass();
		for (String key : state.keySet()) {
			try {
				Field f = type.getField(key);
				Class ftype = f.getType();
				if (ftype.isEnum()) {
					f.set(parent, Enum.valueOf(ftype, (String)state.get(key)));
				} else if (ftype.isPrimitive()) {
					deserializePrimitive(parent, state, key, f, ftype);
				} else if (ftype.isArray()) {
					deserializePrimitiveArray(parent, state, key, f, ftype);
				} else if (ftype.getName().equals("java.lang.String")) {
					f.set(parent, state.get(key));
				} else if (FastAccess.class.isAssignableFrom(ftype)) {
					deserializeFastAccess(parent, state, key, f);
				} else {
					Object child = ftype.getConstructor().newInstance();
					deserialize(child, (Map<String, Object>)state.get(key));
					Class c = child.getClass();
					c.getMethod("setTo", c).invoke(f.get(parent), child);
				}
			} catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException |
					InstantiationException | InvocationTargetException e) {
				errorHandler.handle(ErrorType.REFLECTION,
						String.format("%s class='%s' key='%s'", e.getClass().getSimpleName(), type.getSimpleName(), key), null);
			}
		}
	}

	private void deserializePrimitive( Object parent, Map<String, Object> state, String key, Field f, Class ftype )
			throws IllegalAccessException {
		Object value = state.get(key);
		if (ftype == boolean.class)
			f.set(parent, value);
		else if (ftype == byte.class)
			f.set(parent, ((Number)value).byteValue());
		else if (ftype == char.class)
			f.set(parent, value);
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
			errorHandler.handle(ErrorType.UNEXPECTED_TYPE, "Unknown primitive " + ftype, null);
	}

	private void deserializePrimitiveArray( Object parent, Map<String, Object> state, String key, Field f, Class ftype )
			throws IllegalAccessException {
		Object value = state.get(key);
		if (ftype == boolean[].class)
			f.set(parent, ((boolean[])value).clone());
		else if (ftype == byte[].class)
			f.set(parent, ((byte[])value).clone());
		else if (ftype == char[].class)
			f.set(parent, ((char[])value).clone());
		else if (ftype == short[].class)
			f.set(parent, ((short[])value).clone());
		else if (ftype == int[].class) {
			List<Integer> list = (List<Integer>)value;
			int[] array = new int[list.size()];
			for (int i = 0; i < array.length; i++) {
				array[i] = list.get(i);
			}
			f.set(parent, array);
		} else if (ftype == long[].class)
			f.set(parent, ((long[])value).clone());
		else if (ftype == float[].class)
			f.set(parent, ((float[])value).clone());
		else if (ftype == double[].class)
			f.set(parent, ((double[])value).clone());
		else
			errorHandler.handle(ErrorType.UNEXPECTED_TYPE, "Unknown primitive array " + ftype, null);
	}

	private void deserializeFastAccess( Object parent, Map<String, Object> state, String key, Field f )
			throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		// See if the list is empty and there's nothing to do
		List listOfStates = (List)state.get(key);
		if (listOfStates.isEmpty())
			return;

		// See if we are dealing with a regular array or DogArray
		if (FastArray.class.isAssignableFrom(f.get(parent).getClass())) {
			FastArray<Object> plist = (FastArray<Object>)f.get(parent);
			Class<?> itemType = plist.type;
			boolean basic = itemType.isEnum() || itemType.isPrimitive() || itemType.getName().equals("java.lang.String");

			// deserialize each element and add it to the list
			plist.reset();
			for (int i = 0; i < listOfStates.size(); i++) {
				Object value = listOfStates.get(i);
				if (basic) {
					// since numeric values are stored as objects this should work too. Not tested.
					if (itemType.isEnum())
						plist.add(Enum.valueOf((Class)itemType, (String)value));
					else
						plist.add(value);
				} else {
					Object dst = itemType.getConstructor().newInstance();
					deserialize(dst, (Map)value);
					plist.add(dst);
				}
			}
		} else {
			DogArray<Object> plist = (DogArray<Object>)f.get(parent);

			// predeclare all the required elements
			plist.resetResize(listOfStates.size());

			// deserialize each element and add it to the list
			for (int i = 0; i < listOfStates.size(); i++) {
				Object value = listOfStates.get(i);
				Object dst = plist.get(i);
				deserialize(dst, (Map)value);
			}
		}
	}

	public static Yaml createYmlObject() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return new Yaml(options);
	}

	public enum ErrorType {
		/** Exception related to Java Reflections */
		REFLECTION,
		/** Miscellaneous error */
		MISC,
		/** Unsupported data type */
		UNSUPPORTED,
		/** Encountered an unknown field name when deserializing */
		UNKNOWN_FIELD,
		/** Encountered an unexpected type when deserializing */
		UNEXPECTED_TYPE
	}

	@FunctionalInterface
	public interface HandleError {
		/**
		 * Handle the error. If no exception is thrown it will continue processing
		 *
		 * @param e The exception which caused the error. Null if there was no exception
		 */
		void handle( ErrorType type, String description, @Nullable RuntimeException e );
	}
}
