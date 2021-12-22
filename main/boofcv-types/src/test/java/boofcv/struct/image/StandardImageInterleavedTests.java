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

package boofcv.struct.image;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standard tests for children of {@link ImageGray}. Ensures that they contain
 * all the expected functions and that they have the expected behavior. This is done
 * through extensive use of reflections.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes") public abstract class StandardImageInterleavedTests<T extends ImageInterleaved<T>> extends BoofStandardJUnit {

	public Random rand = new Random(234);

	public abstract T createImage( int width, int height, int numBands );

	public abstract T createImage();

	public abstract Number randomNumber();

	public abstract Number getNumber( Number value );

	/**
	 * Sets each element in the image to a random value.
	 */
	public void setRandom( T img ) {
		Object data = img._getData();

		int N = Array.getLength(data);
		for (int i = 0; i < N; i++) {
			Array.set(data, i, randomNumber());
		}
	}

	/**
	 * Checks to see if the implementation specific to ImageInterleavedTests
	 * works
	 */
	@Test void isSubimage() {
		T a = createImage(10, 20, 3);

		assertFalse(a.isSubimage());

		assertTrue(a.subimage(0, 5, 0, 5, null).isSubimage());
		assertTrue(a.subimage(2, 5, 2, 5, null).isSubimage());
	}

	/**
	 * Check for a positive case of get() and set()
	 */
	@Test void get_set() {
		T img = createImage(10, 20, 3);
		setRandom(img);

		for (int y = 0; y < img.height; y++) {
			for (int x = 0; x < img.width; x++) {
				Object expected = createPixelArray(img);
				Object orig = call(img, "get", 2, null, x, y);

				// make sure the two are not equal
				assertFalse(compareArrays(expected, orig, img.getNumBands()));

				// set the expected to the point in the image
				call(img, "set", 2, expected, x, y);
				Object found = call(img, "get", 2, null, x, y);
				assertTrue(compareArrays(expected, found, img.getNumBands()));
			}
		}
	}

	/**
	 * Check for a positive case of get() and set()
	 */
	@Test void getBand_setBand() {
		T img = createImage(10, 20, 2);
		setRandom(img);

		for (int y = 0; y < img.height; y++) {
			for (int x = 0; x < img.width; x++) {
				for (int b = 0; b < img.numBands; b++) {
					Number expected = randomNumber();
					Number orig = (Number)call(img, "getBand", 0, null, x, y, b);

					// make sure the two are not equal
					assertNotEquals(orig, expected);

					// set the expected to the point in the image
					call(img, "setBand", 1, expected, x, y, b);
					Number found = (Number)call(img, "getBand", 0, null, x, y, b);
					assertEquals(found.doubleValue(), getNumber(expected).doubleValue());
				}
			}
		}
	}

	/**
	 * Makes sure all the accessors do proper bounds checking
	 */
	@Test void accessorBounds() {
		T img = createImage(10, 20, 3);

		checkBound(img, "get", 2, null);
		checkBoundBand(img, "getBand", 0, null);
		checkBound(img, "set", 2, null);
		checkBoundBand(img, "setBand", 1, randomNumber());
	}

	private void checkBound( T img, String method, int type, Object typeData ) {
		checkException(img, method, type, typeData, -1, 0);
		checkException(img, method, type, typeData, 0, -1);
		checkException(img, method, type, typeData, img.getWidth(), 0);
		checkException(img, method, type, typeData, 0, img.getHeight());
	}

	private void checkBoundBand( T img, String method,
								 int type, Object typeData ) {
		checkException(img, method, type, typeData, -1, 0, 0);
		checkException(img, method, type, typeData, 0, -1, 0);
		checkException(img, method, type, typeData, img.getWidth(), 0, 0);
		checkException(img, method, type, typeData, 0, img.getHeight(), 0);
		checkException(img, method, type, typeData, 0, 0, img.getNumBands());
	}

	private void checkException( ImageInterleaved img, String method,
								 int type, Object typeData, int... where ) {
		boolean found = false;
		try {
			call(img, method, type, typeData, where);
		} catch (ImageAccessException e) {
			found = true;
		}

		assertTrue(found, "No exception was thrown");
	}

	private Object call( ImageInterleaved img, String method,
						 int type, Object typeData, int... where ) {
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
				paramTypes[index] = img.getPrimitiveDataType();
				args[index] = typeData;
			} else if (type == 2) {
				String name = "[" + img.getPrimitiveDataType().getName().toUpperCase().charAt(0);
				if (name.charAt(1) == 'L')
					name = "[J";
				paramTypes[index] = Class.forName(name);
				args[index] = typeData;
			}

			Method m = img.getClass().getMethod(method, paramTypes);

			return m.invoke(img, args);
		} catch (ClassNotFoundException | IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			fail("The method " + method + " needs to be implemented");
		} catch (InvocationTargetException e) {
			throw (RuntimeException)e.getCause();
		}
		throw new RuntimeException("Shouldn't be here");
	}

	private Object createPixelArray( ImageInterleaved img ) {
		int numBands = img.getNumBands();

		Object ret = Array.newInstance(img.getPrimitiveDataType(), numBands);

		for (int i = 0; i < numBands; i++)
			Array.set(ret, i, randomNumber());

		return ret;
	}

	private boolean compareArrays( Object a, Object b, int length ) {
		for (int i = 0; i < length; i++) {
			Number valA = (Number)Array.get(a, i);
			Number valB = (Number)Array.get(b, i);

			if (!valA.equals(valB))
				return false;
		}
		return true;
	}

	@Test void setNumberOfBands() {
		T a = createImage(10, 20, 3);

		a.setNumberOfBands(2);

		assertEquals(2, a.getNumBands());
		assertEquals(2, a.getImageType().getNumBands());
		assertEquals(2*10, a.stride);

		assertTrue(Array.getLength(a._getData()) >= 10*20*2);

		a.setNumberOfBands(4);

		assertEquals(4, a.getNumBands());
		assertEquals(4, a.getImageType().getNumBands());
		assertEquals(4*10, a.stride);
		assertTrue(Array.getLength(a._getData()) >= 10*20*4);
	}

	@Test void checkImageTypeSet() {
		T a = createImage(10, 20, 3);

		assertNotNull(a.getImageType());
		assertEquals(3, a.getImageType().getNumBands());
	}

	@Test void checkNoArgumentConstructor() {
		T a = createImage();

		assertNotNull(a._getData());
		assertNotNull(a.getPrimitiveDataType());
		assertNotNull(a.getImageType());
	}

	@Test void copyRow() {
		T img = createImage(10, 20, 3);
		setRandom(img);

		Object arrayRow = img.getDataType().newArray(img.width*img.numBands);

		copyRow(1, 0, 10, img, arrayRow);
		copyRow(19, 0, 10, img, arrayRow);
		copyRow(1, 5, 10, img, arrayRow);
		copyRow(1, 5, 6, img, arrayRow);
	}

	private void copyRow( int row, int col0, int col1, T img, Object array ) {
		img.copyRow(row, col0, col1, 0, array);

		boolean signed = img.getDataType().isSigned();
		int idx = 0;
		for (int x = col0; x < col1; x++) {
			for (int band = 0; band < img.numBands; band++) {
				double valA = GeneralizedImageOps.get(img, x, row, band);
				double valB = GeneralizedImageOps.arrayElement(array, idx++, signed);
				assertEquals(valA, valB, UtilEjml.TEST_F64);
			}
		}
	}

	@Test void copyCol() {
		T img = createImage(10, 20, 3);
		setRandom(img);

		Object arrayCol = img.getDataType().newArray(img.height*img.numBands);

		copyCol(0, 0, 20, img, arrayCol);
		copyCol(9, 0, 20, img, arrayCol);
		copyCol(1, 5, 20, img, arrayCol);
		copyCol(1, 5, 6, img, arrayCol);
	}

	private void copyCol( int col, int row0, int row1, T img, Object array ) {
		img.copyCol(col, row0, row1, 0, array);

		boolean signed = img.getDataType().isSigned();
		int idx = 0;
		for (int y = row0; y < row1; y++) {
			for (int band = 0; band < img.numBands; band++) {
				double valA = GeneralizedImageOps.get(img, col, y, band);
				double valB = GeneralizedImageOps.arrayElement(array, idx++, signed);
				assertEquals(valA, valB, UtilEjml.TEST_F64);
			}
		}
	}
}
