/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public abstract class StandardConfigurationChecks {

	protected Random rand = new Random(234);
	Class<Configuration> type;

	public StandardConfigurationChecks(Class type ) {
		this.type = type;
	}

	public StandardConfigurationChecks() {
		String name = getClass().getName().replaceFirst("Test","");
		try {
			type = (Class<Configuration>) Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"No class found after removing Test from name. Try using the manual constructor");
		}
	}

	/**
	 * Selects non-default values public primitive fields.
	 */
	public Configuration createNotDefault(Random rand ) {
		try {
			Configuration config = type.getConstructor().newInstance();
			Field[] fields = type.getFields();
			for (Field f : fields) {
				if (f.getType().isEnum()) {
					// Select an e num value which isn't the same as the current value
					Object[] values = f.getType().getEnumConstants();
					Object o = f.get(config);
					for (int i = 0; i < values.length; i++) {
						if( values[i] != o ) {
							o = values[i];
							break;
						}
					}
					f.set(config,o);
				} else if ( f.getType().isPrimitive()) {
					// just add one to all primitive types to make them difference
					Object o = f.get(config);
					if( boolean.class == f.getType() ) {
						o = !((boolean)o);
					} else if( byte.class == f.getType() ) {
						o = (byte)(((byte)o)+1);
					} else if( char.class == f.getType() ) {
						o = (char)(((char)o)+1);
					} else if( short.class == f.getType() ) {
						o = (short)(((short)o)+1);
					} else if( int.class == f.getType() ) {
						o = (int)(((int)o)+1);
					} else if( long.class == f.getType() ) {
						o = (long)(((long)o)+1);
					} else if( float.class == f.getType() ) {
						o = (float)(((float)o)+1);
					} else if( double.class == f.getType() ) {
						o = (double)(((double)o)+1);
					} else {
						throw new RuntimeException("BUG "+f.getType().getSimpleName());
					}
					f.set(config,o);
				}
			}
			return config;
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void setTo() {
		Method m;
		Configuration src = createNotDefault(rand);
		try {
			Configuration dst = type.getConstructor().newInstance();
			m = type.getMethod("setTo", type);

			Field[] fields = type.getFields();
			// first see if the not default configuration was set up correctly
			for (Field f : fields) {
				assertNotEquals(f.get(src), f.get(dst));
			}

			m.invoke(dst, src);

			// after setTo() they should be the same
			for (Field f : fields) {
				if (f.getType().isEnum() || f.getType().isPrimitive())
					assertEquals(f.get(src), f.get(dst), f.getName());
				else
					assertNotEquals(f.get(src), f.get(dst), f.getName());
				// if they are equal that means it copied the reference
			}

		} catch( NoSuchMethodException e ) {
			e.printStackTrace();
			fail("setTo() isn't implemented yet");
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			e.printStackTrace();
			fail("BAD");
		}
	}

	private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

	public static boolean isWrapperType(Class<?> clazz)
	{
		return WRAPPER_TYPES.contains(clazz);
	}

	private static Set<Class<?>> getWrapperTypes()
	{
		Set<Class<?>> ret = new HashSet<Class<?>>();
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
