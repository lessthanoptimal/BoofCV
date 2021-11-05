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

package boofcv.struct;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public abstract class StandardConfigurationChecks extends BoofStandardJUnit {

	Class<Configuration> type;

	protected StandardConfigurationChecks( Class type ) {
		this.type = type;
	}

	protected StandardConfigurationChecks() {
		String name = getClass().getName().replaceFirst("Test", "");
		try {
			type = (Class<Configuration>)Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"No class found after removing Test from name. Try using the manual constructor");
		}
	}

	/**
	 * Selects non-default values public primitive fields.
	 */
	public Configuration createNotDefault( Random rand ) {
		return (Configuration)createNotDefault(type, rand);
	}

	public static Object createNotDefault( Class<?> type, Random rand ) {
		try {
			Object config = type.getConstructor().newInstance();
			Field[] fields = type.getFields();
			for (Field f : fields) {
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				if (f.getType().isEnum()) {
					// Select an e num value which isn't the same as the current value
					Object[] values = f.getType().getEnumConstants();
					Object o = f.get(config);
					for (int i = 0; i < values.length; i++) {
						if (values[i] != o) {
							o = values[i];
							break;
						}
					}
					f.set(config, o);
				} else if (f.getType().isPrimitive()) {
					// just add one to all primitive types to make them difference
					Object o = f.get(config);
					if (boolean.class == f.getType()) {
						o = !((boolean)o);
					} else if (byte.class == f.getType()) {
						o = (byte)(((byte)o) + 1);
					} else if (char.class == f.getType()) {
						o = (char)(((char)o) + 1);
					} else if (short.class == f.getType()) {
						o = (short)(((short)o) + 1);
					} else if (int.class == f.getType()) {
						int before = (int)o;
						int value = rand.nextInt();
						while (value == before)
							value = rand.nextInt();
						o = value;
					} else if (long.class == f.getType()) {
						long before = (long)o;
						long value = rand.nextLong();
						while (value == before)
							value = rand.nextLong();
						o = value;
					} else if (float.class == f.getType()) {
						float before = (float)o;
						float value = rand.nextFloat();
						while (value == before)
							value = rand.nextFloat();
						o = value;
					} else if (double.class == f.getType()) {
						double before = (double)o;
						double value = rand.nextDouble();
						while (value == before)
							value = rand.nextDouble();
						o = value;
					} else {
						throw new RuntimeException("BUG " + f.getType().getSimpleName());
					}
					f.set(config, o);
				} else if( f.getType().isAssignableFrom(List.class)) {
					// If it's a list, create a "non-default" element for each item in the original
					List originalList = (List)f.get(config);
					if (!originalList.isEmpty()) {
						List outputList = new ArrayList();
						for (int listIdx = 0; listIdx < originalList.size(); listIdx++) {
							Object o = originalList.get(listIdx);
							outputList.add(createNotDefault(o.getClass(), rand));
						}
						// It might be final. So just add the elements in after clearing it
						originalList.clear();
						originalList.addAll(outputList);
					}
				}
			}
			return config;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test void setTo() {
		Method m;
		Configuration src = (Configuration)createNotDefault(type, rand);
		try {
			Configuration dst = type.getConstructor().newInstance();
			m = type.getMethod("setTo", type);

			Field[] fields = type.getFields();
			// first see if the not default configuration was set up correctly
			for (Field f : fields) {
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				boolean onlyOneOption = false;

				// If it's an enum with only one value then it can't be not equals
				if (f.getType().isEnum()) {
					if (f.getType().getEnumConstants().length == 1)
						onlyOneOption = true;
				}
				// if it's null it should stay null
				if (f.get(src) == null)
					onlyOneOption = true;

				if (onlyOneOption)
					assertEquals(f.get(src), f.get(dst), "Field Name: '" + f.getName() + "'");
				else if (f.getType().isAssignableFrom(List.class)) {
					List listSrc = (List)f.get(src);
					List listDst = (List)f.get(dst);

					// if the original list is empty there's nothing that can be tested.
					if (listSrc.isEmpty()) {
						// The type is unknown so nothing should have been added to dst
						assertTrue(listDst.isEmpty());
					} else {
						throw new RuntimeException("Handle this!");
					}
				} else {
					assertNotEquals(f.get(src), f.get(dst), "Field Name: '" + f.getName() + "'");
				}
			}

			Object ret = m.invoke(dst, src);
			assertSame(dst, ret, "setTo() must return 'this'");
			// you want to return 'this' so that commands can be chained

			// after setTo() they should be the same
			for (Field f : fields) {
				if (f.getType().isEnum() || f.getType().isPrimitive() || f.get(src) == null)
					assertEquals(f.get(src), f.get(dst), "Field Name: '" + f.getName() + "'");
				else if (f.getType().isAssignableFrom(List.class)) {
					List listSrc = (List)f.get(src);
					List listDst = (List)f.get(dst);
					assertEquals(listSrc.size(), listDst.size());
					for (int i = 0; i < listSrc.size(); i++) {
						throw new RuntimeException("IMplement!");
					}
				} else
					assertNotEquals(f.get(src), f.get(dst), "Field Name: '" + f.getName() + "'");
				// if they are equal that means it copied the reference
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			fail("setTo() isn't implemented yet");
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			e.printStackTrace();
			fail("BAD");
		}
	}

	private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

	public static boolean isWrapperType( Class<?> clazz ) {
		return WRAPPER_TYPES.contains(clazz);
	}

	private static Set<Class<?>> getWrapperTypes() {
		Set<Class<?>> ret = new HashSet<>();
		ret.add(Boolean.class);
		ret.add(Character.class);
		ret.add(Byte.class);
		ret.add(Short.class);
		ret.add(Integer.class);
		ret.add(Long.class);
		ret.add(Float.class);
		ret.add(Double.class);
		ret.add(Void.class);
		return ret;
	}
}
