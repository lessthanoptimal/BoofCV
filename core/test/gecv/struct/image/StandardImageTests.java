/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.struct.image;

import gecv.core.image.GeneralizedImageOps;
import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Standard tests for children of {@link ImageBase}.  Ensures that they contain
 * all the expected functions and that they have the expected behavior.  This is done
 * through extensive use of reflections.
 *
 * @author Peter Abeles
 */
public abstract class StandardImageTests {

	public Random rand = new Random(234);

	public abstract ImageBase createImage(int width, int height);

	public abstract Number randomNumber();

	/**
	 * Sets each element in the image to a random value.
	 */
	public void setRandom(ImageBase img) {
		Object data = img._getData();

		int N = Array.getLength(data);
		for (int i = 0; i < N; i++) {
			Array.set(data, i, randomNumber());
		}
	}

	/**
	 * Check for a positive case of get() and set()
	 */
	@Test
	public void get_set() {
		ImageBase img = createImage(10, 20);
		setRandom(img);

		Number expected = randomNumber();
		Number orig = (Number) call(img, "get", 0, null, 1, 1);

		// make sure the two are not equal
		assertFalse(expected.equals(orig));

		// set the expected to the point in the image
		call(img, "set", 1, expected, 1, 1);
		Number found = (Number) call(img, "get", 0, null, 1, 1);
		if (GeneralizedImageOps.isFloatingPoint(img))
			assertEquals(expected.doubleValue(), found.doubleValue(), 1e-4);
		else
			assertTrue(expected.intValue() == found.intValue());
	}

	/**
	 * Makes sure all the accessors do proper bounds checking
	 */
	@Test
	public void accessorBounds() {
		ImageBase img = createImage(10, 20);

		checkBound(img, "get", 0, null);
		checkBound(img, "set", 1, randomNumber());
	}

	private void checkBound(ImageBase img, String method,
							int type, Object typeData) {
		checkException(img, method, type, typeData, -1, 0);
		checkException(img, method, type, typeData, 0, -1);
		checkException(img, method, type, typeData, img.getWidth(), 0);
		checkException(img, method, type, typeData, 0, img.getHeight());
	}

	private void checkException(ImageBase img, String method,
								int type, Object typeData, int... where) {
		boolean found = false;
		try {
			call(img, method, type, typeData, where);
		} catch (ImageAccessException e) {
			found = true;
		}

		assertTrue("No exception was thrown", found);
	}

	private Object call(ImageBase img, String method,
						int type, Object typeData, int... where) {
		try {
			Class<?>[] paramTypes = type == 0 ?
					new Class<?>[where.length] : new Class<?>[where.length + 1];
			Object[] args = new Object[paramTypes.length];

			int index;

			for (index = 0; index < where.length; index++) {
				paramTypes[index] = int.class;
				args[index] = where[index];
			}
			if (type == 1) {
				if (GeneralizedImageOps.isFloatingPoint(img)) {
					paramTypes[index] = float.class;
					args[index] = typeData;
				} else {
					paramTypes[index] = int.class;
					args[index] = typeData;
				}
//				paramTypes[index] = img._getPrimitiveType();
//				args[index] = typeData;
			} else if (type == 2) {
				String name = "[" + img._getPrimitiveType().getName().toUpperCase().charAt(0);
				paramTypes[index] = Class.forName(name);
				args[index] = typeData;
			}

			Method m = img.getClass().getMethod(method, paramTypes);

			return m.invoke(img, args);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			fail("The method " + method + " needs to be implemented");
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw (RuntimeException) e.getCause();
		}
		throw new RuntimeException("Shouldn't be here");
	}
}
