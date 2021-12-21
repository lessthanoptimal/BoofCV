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

import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.io.*;
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
@SuppressWarnings("rawtypes")
public abstract class StandardSingleBandTests<T extends ImageGray<T>> extends BoofStandardJUnit {

	public Random rand = new Random(234);

	public abstract T createImage( int width, int height );

	public abstract T createImage();

	public abstract Number randomNumber();

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
	 * Check for a positive case of get() and set()
	 */
	@Test void get_set() {
		T img = createImage(10, 20);
		setRandom(img);

		Number expected = randomNumber();
		Number orig = (Number)call(img, "get", 0, null, 1, 1);

		// make sure the two are not equal
		assertNotEquals(expected, orig);

		// set the expected to the point in the image
		call(img, "set", 1, expected, 1, 1);
		Number found = (Number)call(img, "get", 0, null, 1, 1);
		if (!img.getDataType().isInteger())
			assertEquals(expected.doubleValue(), found.doubleValue(), 1e-4);
		else {
			if (img.getDataType().isSigned())
				assertEquals(expected.intValue(), found.intValue());
			else
				assertEquals((expected.intValue() & 0xFFFF), found.intValue());
		}
	}

	/**
	 * Check for a positive case of get() and set()
	 */
	@Test void unsafe_get_set() {
		T img = createImage(10, 20);
		setRandom(img);

		Number expected = randomNumber();
		Number orig = (Number)call(img, "unsafe_get", 0, null, 1, 1);

		// make sure the two are not equal
		assertNotEquals(expected, orig);

		// set the expected to the point in the image
		call(img, "unsafe_set", 1, expected, 1, 1);
		Number found = (Number)call(img, "unsafe_get", 0, null, 1, 1);
		if (!img.getDataType().isInteger())
			assertEquals(expected.doubleValue(), found.doubleValue(), 1e-4);
		else {
			if (img.getDataType().isSigned())
				assertEquals(expected.intValue(), found.intValue());
			else
				assertEquals((expected.intValue() & 0xFFFF), found.intValue());
		}
	}

	/**
	 * Makes sure all the accessors do proper bounds checking
	 */
	@Test void accessorBounds() {
		ImageGray img = createImage(10, 20);

		checkBound(img, "get", 0, null);
		checkBound(img, "set", 1, randomNumber());
	}

	private void checkBound( ImageGray img, String method,
							 int type, Object typeData ) {
		checkException(img, method, type, typeData, -1, 0);
		checkException(img, method, type, typeData, 0, -1);
		checkException(img, method, type, typeData, img.getWidth(), 0);
		checkException(img, method, type, typeData, 0, img.getHeight());
	}

	private void checkException( ImageGray img, String method,
								 int type, Object typeData, int... where ) {
		boolean found = false;
		try {
			call(img, method, type, typeData, where);
		} catch (ImageAccessException e) {
			found = true;
		}

		assertTrue(found, "No exception was thrown");
	}

	private Object call( ImageGray img, String method,
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
				paramTypes[index] = img.getDataType().getSumType();
				args[index] = typeData;
			} else if (type == 2) {
				String name = "[" + img.getDataType().getDataType().getName().toUpperCase().charAt(0);
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

	@Test void subimage() {
		T img = createImage(10, 20);
		setRandom(img);

		T sub = (T)img.subimage(2, 3, 3, 5, null);

		assertSame(img.getImageType(), sub.getImageType());
		assertEquals(1, sub.getWidth());
		assertEquals(2, sub.getHeight());

		GImageGray a = FactoryGImageGray.wrap(img);
		GImageGray b = FactoryGImageGray.wrap(sub);

		assertEquals(a.get(2, 3), b.get(0, 0));
		assertEquals(a.get(2, 4), b.get(0, 1));
	}

	@Test void reshape() {
		ImageGray img = createImage(10, 20);

		// reshape to something smaller
		img.reshape(5, 4);
		assertEquals(5, img.getWidth());
		assertEquals(4, img.getHeight());

		// reshape to something larger
		img.reshape(15, 21);
		assertEquals(15, img.getWidth());
		assertEquals(21, img.getHeight());
	}

	@Test void serialize() throws IOException, ClassNotFoundException {

		// randomly fill the image
		ImageGray imgA = createImage(10, 20);
		GImageGray a = FactoryGImageGray.wrap(imgA);
		randomFill(imgA, a);

		// make a copy of the original
		ImageGray imgB = (ImageGray)imgA.clone();


		ByteArrayOutputStream streamOut = new ByteArrayOutputStream(1000);
		ObjectOutputStream out = new ObjectOutputStream(streamOut);
		out.writeObject(imgA);
		out.close();


		ByteArrayInputStream streamIn = new ByteArrayInputStream(streamOut.toByteArray());
		ObjectInputStream in = new ObjectInputStream(streamIn);

		ImageGray found = (ImageGray)in.readObject();

		// see if everything is equals
		checkEquals(imgA, imgB);
		checkEquals(imgA, found);
	}

	private void randomFill( ImageGray imgA, GImageGray a ) {
		for (int i = 0; i < imgA.getHeight(); i++) {
			for (int j = 0; j < imgA.getWidth(); j++) {
				a.set(j, i, rand.nextDouble()*200);
			}
		}
	}

	private void checkEquals( ImageGray imgA, ImageGray imgB ) {
		for (int i = 0; i < imgA.getHeight(); i++) {
			for (int j = 0; j < imgA.getWidth(); j++) {
				double valA = GeneralizedImageOps.get(imgA, j, i);
				double valB = GeneralizedImageOps.get(imgB, j, i);

				assertEquals(valA, valB, 1e-8);
			}
		}
	}

	@Test void checkNoArgumentConstructor() {
		ImageGray a = createImage();

		assertNotNull(a._getData());
		assertNotNull(a.getImageType());
	}

	@Test void copyRow() {
		ImageGray img = createImage(10, 20);
		GImageGray a = FactoryGImageGray.wrap(img);
		randomFill(img, a);

		Object arrayRow = img.getDataType().newArray(img.width);

		copyRow(1, 0, 10, img, arrayRow);
		copyRow(19, 0, 10, img, arrayRow);
		copyRow(1, 5, 10, img, arrayRow);
		copyRow(1, 5, 6, img, arrayRow);
	}

	private void copyRow( int row, int col0, int col1, ImageGray img, Object array ) {
		img.copyRow(row, col0, col1, 0, array);

		boolean signed = img.getDataType().isSigned();
		for (int x = col0; x < col1; x++) {
			double valA = GeneralizedImageOps.get(img, x, row);
			double valB = GeneralizedImageOps.arrayElement(array, x - col0, signed);
			assertEquals(valA, valB, UtilEjml.TEST_F64);
		}
	}

	@Test void copyCol() {
		ImageGray img = createImage(10, 20);
		GImageGray a = FactoryGImageGray.wrap(img);
		randomFill(img, a);

		Object arrayCol = img.getDataType().newArray(img.height);

		copyCol(0, 0, 20, img, arrayCol);
		copyCol(9, 0, 20, img, arrayCol);
		copyCol(1, 5, 20, img, arrayCol);
		copyCol(1, 5, 6, img, arrayCol);
	}

	private void copyCol( int col, int row0, int row1, ImageGray img, Object array ) {
		img.copyCol(col, row0, row1, 0, array);

		boolean signed = img.getDataType().isSigned();
		for (int y = row0; y < row1; y++) {
			double valA = GeneralizedImageOps.get(img, col, y);
			double valB = GeneralizedImageOps.arrayElement(array, y - row0, signed);
			assertEquals(valA, valB, UtilEjml.TEST_F64);
		}
	}
}
