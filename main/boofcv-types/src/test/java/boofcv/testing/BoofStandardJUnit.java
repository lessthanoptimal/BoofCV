/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.testing;

import boofcv.BoofTesting;
import boofcv.struct.Configuration;
import org.ejml.data.DMatrix;
import org.ejml.data.FMatrix;
import org.ejml.ops.MatrixFeatures_D;
import org.ejml.ops.MatrixFeatures_F;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adds tests to enforce standards, such as no printing to stdout or stderr, unless it's an error.
 *
 * @author Peter Abeles
 */
public class BoofStandardJUnit {
	// Always provide a random number generator since it's needed so often
	protected final Random rand = BoofTesting.createRandom(234);

	// Override output streams to keep log spam to a minimum
	protected final MirrorStream out = new MirrorStream(System.out);
	protected final MirrorStream err = new MirrorStream(System.err);
	protected final PrintStream systemOut = System.out;
	protected final PrintStream systemErr = System.err;

	@BeforeEach
	public void captureStreams() {
		System.setOut(new PrintStream(out));
		System.setErr(new PrintStream(err));
	}

	@AfterEach
	public void revertStreams() {
		assertFalse(out.used, "stdout was written to which is forbidden by default");
		assertFalse(err.used, "stderr was written to which is forbidden by default");
		System.setOut(systemOut);
		System.setErr(systemErr);
	}

	/**
	 * Creates an instance of the class where all fields are assigned a random valuen
	 */
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
				} else if (String.class == f.getType()) {
					// create a random string
					char[] random = new char[1 + rand.nextInt(10)];
					for (int i = 0; i < random.length; i++) {
						random[i] = (char)('a' + rand.nextInt(20));
					}
					f.set(config, new String(random));
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
				} else if (f.getType().isAssignableFrom(List.class)) {
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
				} else if (Modifier.isFinal(f.getModifiers())) {
					// if final and it has a setTo(), then create a random instance and call setTo
					try {
						Method m = f.getType().getMethod("setTo", f.getType());
						m.invoke(f.get(config), createNotDefault(f.getType(), rand));
					} catch (NoSuchMethodException | SecurityException | InvocationTargetException |
							 IllegalArgumentException | IllegalAccessException ignore) {
					}
				}
			}
			return config;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Class<?> lookUpClassFromTestName() {
		String name = getClass().getName().replaceFirst("Test", "");
		try {
			return (Class<Configuration>)Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"No class found after removing Test from name. Try using the manual constructor");
		}
	}

	protected void checkSetTo( Class<?> type, boolean returnThis ) {
		Method m;
		try {
			m = type.getMethod("setTo", type);

			Object dst = type.getConstructor().newInstance();
			Object src = createNotDefault(type, rand);

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
			if (returnThis) {
				assertSame(dst, ret, "setTo() must return 'this'");
				// you want to return 'this' so that commands can be chained
			}

			// after setTo() they should be the same
			checkSameFieldValues(fields, src, dst);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			fail("setTo() isn't implemented yet");
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			e.printStackTrace();
			fail("BAD");
		}
	}

	private void checkSameFieldValues( Field[] fields, Object src, Object dst ) throws IllegalAccessException {
		for (Field f : fields) {
			if (f.getType().isEnum() || f.getType().isPrimitive() || f.get(src) == null)
				assertEquals(f.get(src), f.get(dst), "Field Name: '" + f.getName() + "' in " + src.getClass().getSimpleName());
			else if (f.getType() != Object.class && f.getType().isAssignableFrom(List.class)) {
				List listSrc = (List)f.get(src);
				List listDst = (List)f.get(dst);
				assertEquals(listSrc.size(), listDst.size());
				for (int i = 0; i < listSrc.size(); i++) {
					throw new RuntimeException("IMplement!");
				}
			} else if (f.getType().isAssignableFrom(String.class)) {
				assertEquals(f.get(src), f.get(dst), "Field Name: '" + f.getName() + "'");
			} else {
				Object a = f.get(src);
				Object b = f.get(dst);
				assertNotSame(a, b, "Two classes are the same. field='" + f.getName() + "'");

				// Handle special cases. Anyway to remove these special cases?
				if (a instanceof DMatrix) {
					assertTrue(MatrixFeatures_D.isEquals((DMatrix)a, (DMatrix)b));
				} else if (a instanceof FMatrix) {
					assertTrue(MatrixFeatures_F.isEquals((FMatrix)a, (FMatrix)b));
				} else {
					// see if it conforms to setTo then recursively check field values
					Object fsrc = f.get(src);
					Object fdst = f.get(dst);
					try {
						Method m = fsrc.getClass().getMethod("setTo", fsrc.getClass());
						checkSameFieldValues(fsrc.getClass().getFields(), fsrc, fdst);
					} catch (NoSuchMethodException | SecurityException |
							 IllegalArgumentException | IllegalAccessException ignore) {
					}
				}
			}
			// if they are equal that means it copied the reference
		}
	}

	public static class MirrorStream extends OutputStream {

		public PrintStream out;
		public boolean used = false;

		public MirrorStream( PrintStream out ) {
			this.out = out;
		}

		@Override public void write( int b ) throws IOException {
			used = true;
			out.write(b);
		}

		@Override public void write( byte[] b, int off, int len ) throws IOException {
			used = true;
			out.write(b, off, len);
		}

		@Override public void flush() throws IOException {
			out.flush();
		}

		@Override public void close() throws IOException {
			out.close();
		}
	}
}
