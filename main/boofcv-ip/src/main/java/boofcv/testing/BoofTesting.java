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

package boofcv.testing;

import boofcv.concurrency.WorkArrays;
import boofcv.core.image.*;
import boofcv.struct.image.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Functions to aid in unit testing code for correctly handling sub-images
 *
 * @author Peter Abeles
 */
// todo remove all comwapare with border functions and use sub-images instead
@SuppressWarnings("ALL")
public class BoofTesting {

	public static final long BASE_SEED = 0xBEEF;

	/**
	 * Creates Random but using a fixed seed with an offset. The idea if we want to test to see if a test is brittle
	 * we can do that across the project by changing the base seed.
	 * @param offset Value added to base seed.
	 * @return Random
	 */
	public static Random createRandom( long offset ) {
		return new Random(0xBEEF+offset);
	}

	public static <T> T convertToGenericType(Class<?> type) {
		if (type == GrayS8.class || type == GrayU8.class)
			return (T) GrayI8.class;
		if (type == GrayS16.class || type == GrayU16.class)
			return (T) GrayI16.class;
		if (type == InterleavedS8.class || type == InterleavedU8.class)
			return (T) InterleavedI8.class;
		if (type == InterleavedS16.class || type == InterleavedU16.class)
			return (T) InterleavedI16.class;
		return (T) type;
	}

	public static ImageDataType convertToGenericType(ImageDataType type) {
		if (type.isInteger()) {
			if (type.getNumBits() == 8)
				return ImageDataType.I8;
			else if (type.getNumBits() == 16)
				return ImageDataType.I16;
		}

		return type;
	}

	/**
	 * <p>
	 * Returns an image which is a sub-image but contains the same values of the input image.  Use for
	 * testing compliance with sub-images.  The subimage is created by creating a larger image,
	 * copying over the input image into the inner portion, then creating a subimage of the copied part.
	 * </p>
	 */
	@SuppressWarnings({"unchecked"})
	public static <T extends ImageBase<T>> T createSubImageOf(T input) {
		if( input instanceof ImageGray) {
			return (T)createSubImageOf_S((ImageGray)input);
		} else if( input instanceof Planar) {
			return (T)createSubImageOf_PL((Planar) input);
		} else if( input instanceof ImageInterleaved ) {
			return (T)createSubImageOf_I((ImageInterleaved) input);
		} else {
			throw new IllegalArgumentException("Add support for this image type");
		}
	}

	public static <T extends ImageGray<T>> T createSubImageOf_S(T input) {
		// create the larger image
		T ret = (T) input.createNew(input.width + 10, input.height + 12);
		// create a sub-image of the inner portion
		ret = (T) ret.subimage(5, 7, input.width + 5, input.height + 7, null);
		// copy input image into the subimage
		ret.setTo(input);

		return ret;
	}

	public static <T extends ImageInterleaved<T>> T createSubImageOf_I(T input) {
		// create the larger image
		T ret = (T) input.createNew(input.width + 10, input.height + 12);
		// create a sub-image of the inner portion
		ret = (T) ret.subimage(5, 7, input.width + 5, input.height + 7, null);
		// copy input image into the subimage
		ret.setTo(input);

		return ret;
	}

	public static <T extends Planar> T createSubImageOf_PL(T input) {
		T ret = (T)new Planar(input.type,input.width,input.height,input.getNumBands());

		for( int i = 0; i < input.getNumBands(); i++ ) {
			ret.bands[i] = createSubImageOf_S(input.getBand(i));
		}

		ret.stride = ret.bands[0].stride;
		ret.startIndex = ret.bands[0].startIndex;

		return ret;
	}

	/**
	 * Searches for functions that accept only images and makes sure they only accept
	 * images which have he same width and height.
	 *
	 * @param testClass Instance of the class being tested
	 */
	public static void checkImageDimensionValidation(Object testClass, int numFunctions) {
		int count = 0;
		Method methods[] = testClass.getClass().getMethods();

		for (Method m : methods) {
			// see if the inputs are all images
			if (!areAllInputsImages(m))
				continue;

			// test a positive case
			Class params[] = m.getParameterTypes();
			Object[] inputs = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				inputs[i] = GeneralizedImageOps.createSingleBand(params[i], 10, 20);
			}

			try {
				m.invoke(testClass, inputs);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			// test negative cases
			for (int target = 0; target < params.length; target++) {
				for (int i = 0; i < params.length; i++) {
					if (i != target)
						inputs[i] = GeneralizedImageOps.createSingleBand(params[i], 10, 20);
					else
						inputs[i] = GeneralizedImageOps.createSingleBand(params[i], 11, 22);
				}

				try {
					m.invoke(testClass, inputs);
					throw new RuntimeException("Expected an exception here");
				} catch (InvocationTargetException e) {
					if (e.getTargetException().getClass() != IllegalArgumentException.class)
						throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
			count++;
		}

		if (count != numFunctions)
			throw new RuntimeException("Unexpected number of functions");
	}

	/**
	 * Searches for functions that accept only images and makes sure they only accept
	 * images which have he same width and height.
	 *
	 * @param testClass Instance of the class being tested
	 */
	public static void checkImageDimensionReshape(Object testClass, int numFunctions) {
		int count = 0;
		Method methods[] = testClass.getClass().getMethods();

		for (Method m : methods) {
			// see if the inputs are all images
			if (!areAllInputsImages(m))
				continue;

			// test a positive case
			Class params[] = m.getParameterTypes();
			Object[] inputs = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				inputs[i] = GeneralizedImageOps.createSingleBand(params[i], 10, 20);
			}

			try {
				m.invoke(testClass, inputs);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			// test negative cases
			// skip input which will be fixed size
			for (int target = 1; target < params.length; target++) {
				for (int i = 0; i < params.length; i++) {
					if (i != target)
						inputs[i] = GeneralizedImageOps.createSingleBand(params[i], 10, 20);
					else
						inputs[i] = GeneralizedImageOps.createSingleBand(params[i], 11, 22);
				}

				try {
					m.invoke(testClass, inputs);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
				for (int i = 1; i < params.length; i++) {
					if(10 !=((ImageBase)inputs[i]).width || 20 != ((ImageBase)inputs[i]).height)
						throw new RuntimeException("Wasn't reshaped");
				}
			}
			count++;
		}

		if (count != numFunctions)
			throw new RuntimeException("Unexpected number of functions. cnt="+count+" funcs="+numFunctions);
	}

	private static boolean areAllInputsImages(Method m) {

		Class<?> params[] = m.getParameterTypes();

		if (params.length == 0)
			return false;

		for (Class<?> p : params) {
			if (!ImageGray.class.isAssignableFrom(p)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Tests the specified function with the original image provided and with an equivalent
	 * sub-image.  The two results are then compared. The function being tested must only
	 * have one input parameter of type {@link GrayU8}.
	 *
	 * @param testClass   Instance of the class that contains the function being tested.
	 * @param function    The name of the function being tested.
	 * @param checkEquals Checks to see if the two images have been modified the same way on output
	 * @param inputParam  The original input parameters
	 */
	// TODO make sure pixels outside are not modified of sub-matrix
	// todo have the submatrices be from different shaped inputs
	public static void checkSubImage(Object testClass,
									 String function,
									 boolean checkEquals,
									 Object... inputParam) {
		try {
			ImageBase[] larger = new ImageBase[inputParam.length];
			ImageBase[] subImg = new ImageBase[inputParam.length];
			Class<?> paramDesc[] = new Class<?>[inputParam.length];
			Object[] inputModified = new Object[inputParam.length];

			for (int i = 0; i < inputParam.length; i++) {
				if (ImageBase.class.isAssignableFrom(inputParam[i].getClass())) {
					ImageBase<?> img = (ImageBase<?>) inputParam[i];

					// copy the original image inside of a larger image
					larger[i] = img.createNew(img.getWidth() + 10, img.getHeight() + 12);
					// extract a sub-image and make it equivalent to the original image.
					subImg[i] = larger[i].subimage(5, 6, 5 + img.getWidth(), 6 + img.getHeight(), null);
					subImg[i].setTo(img);
				}

				// the first time it is called use the original inputs
				inputModified[i] = inputParam[i];
				paramDesc[i] = inputParam[i].getClass();
			}

			// first try it with the original image
			Method m = findMethod(testClass.getClass(), function, paramDesc);

			m.invoke(testClass, inputModified);

			// now try it with the sub-image
			for (int i = 0; i < inputModified.length; i++) {
				if (subImg[i] != null)
					inputModified[i] = subImg[i];
			}
			m.invoke(testClass, inputModified);

			// the result should be the identical
			if (checkEquals) {
				for (int i = 0; i < inputParam.length; i++) {
					if (subImg[i] == null)
						continue;
					assertEquals((ImageBase)inputModified[i], subImg[i], 0);
				}
			}

		} catch (InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Searches for a function which is a perfect match.  if none it exists it checks
	 * to see if any matches that could accept an input of the specified type.  If there
	 * is only one such match that is returned.
	 */
	public static Method findMethod(Class<?> type, String name, Class<?>... params) {
		Method methods[] = type.getMethods();

		List<Method> found = new ArrayList<>();
		for (Method m : methods) {
			if (m.getName().compareTo(name) != 0)
				continue;

			Class<?> a[] = m.getParameterTypes();
			if (a.length != params.length)
				continue;

			boolean match = true;
			for (int i = 0; i < a.length; i++) {
				if (a[i] != params[i]) {
					match = false;
					break;
				}
			}

			if (match) {
				// its a perfect match
				return m;
			}

			// see if it could be called with these parameters
			match = true;
			for (int i = 0; i < a.length; i++) {
				if (params[i] == a[i])
					continue;
				if (a[i].isPrimitive()) {
					if (a[i] == Boolean.TYPE && params[i] == Boolean.class)
						continue;
					if (a[i] == Byte.TYPE && params[i] == Byte.class)
						continue;
					if (a[i] == Short.TYPE && params[i] == Short.class)
						continue;
					if (a[i] == Integer.TYPE && params[i] == Integer.class)
						continue;
					if (a[i] == Long.TYPE && params[i] == Long.class)
						continue;
					if (a[i] == Float.TYPE && params[i] == Float.class)
						continue;
					if (a[i] == Double.TYPE && params[i] == Double.class)
						continue;
				}
				if (!a[i].isAssignableFrom(params[i])) {
					match = false;
					break;
				}
			}

			if (match) {
				found.add(m);
			}
		}

		if (found.size() == 1) {
			return found.get(0);
		}

		throw new RuntimeException("Couldn't find matching *public* function to " + name);
	}

	/**
	 * Looks up the static method then passes in the specified inputs.
	 */
	public static void callStaticMethod(Class<?> classType, String name, Object... inputs) {
		Class<?> params[] = new Class[inputs.length];

		for( int i = 0; i < inputs.length; i++ ) {
			params[i] = inputs[i].getClass();
		}

		Method m = findMethod(classType,name,params);

		if( m == null ) {
			for( int i = 0; i < inputs.length; i++ ) {
				if( params[i] == Integer.class ) {
					params[i] = int.class;
				} else if( params[i] == Float.class ) {
					params[i] = float.class;
				} else if( params[i] == Double.class ) {
					params[i] = double.class;
				}
			}
			m = findMethod(classType,name,params);
		}
		if( m == null )
			throw new IllegalArgumentException("Method not found");

		try {
			m.invoke(null,inputs);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Searches for all functions with the specified name in the target class.  Once it finds
	 * that function it invokes the specified function in the owner class. That function must
	 * take in a Method as its one and only parameter.  The method will be one of the matching
	 * ones in the target class.
	 *
	 * @param owner
	 * @param ownerMethod
	 * @param target
	 * @param targetMethod
	 * @return The number of times 'targetMethod' was found and called.
	 */
	public static int findMethodThenCall(Object owner, String ownerMethod, Class target, String targetMethod) {
		int total = 0;
		Method[] list = target.getMethods();

		try {
			Method om = owner.getClass().getMethod(ownerMethod, Method.class);

			for (Method m : list) {
				if (!m.getName().equals(targetMethod))
					continue;

				om.invoke(owner, m);

				total++;
			}
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		return total;
	}

	public static void assertEquals(double a[], double b[], double tol) {
		for (int i = 0; i < a.length; i++) {
			double diff = Math.abs(a[i] - b[i]);
			if (diff > tol)
				throw new RuntimeException("Element " + i + " not equals. " + a[i] + " " + b[i]);
		}
	}

	public static void assertEquals(double a[], float b[], double tol) {
		for (int i = 0; i < a.length; i++) {
			double diff = Math.abs(a[i] - b[i]);
			if (diff > tol)
				throw new RuntimeException("Element " + i + " not equals. " + a[i] + " " + b[i]);
		}
	}

	public static void assertEquals(double a[], int b[]) {
		for (int i = 0; i < a.length; i++) {
			double diff = Math.abs((int) a[i] - b[i]);
			if (diff != 0)
				throw new RuntimeException("Element " + i + " not equals. " + a[i] + " " + b[i]);
		}
	}

	public static void assertEquals(byte a[], byte b[]) {
		for (int i = 0; i < a.length; i++) {
			int diff = Math.abs(a[i] - b[i]);
			if (diff != 0)
				throw new RuntimeException("Element " + i + " not equals. " + a[i] + " " + b[i]);
		}
	}

	public static void assertEquals(short a[], short b[]) {
		for (int i = 0; i < a.length; i++) {
			int diff = Math.abs(a[i] - b[i]);
			if (diff != 0)
				throw new RuntimeException("Element " + i + " not equals. " + a[i] + " " + b[i]);
		}
	}

	public static void assertEquals(int a[], int b[]) {
		for (int i = 0; i < a.length; i++) {
			int diff = Math.abs(a[i] - b[i]);
			if (diff != 0)
				throw new RuntimeException("Element " + i + " not equals. " + a[i] + " " + b[i]);
		}
	}

	public static void assertEquals(long a[], long b[]) {
		for (int i = 0; i < a.length; i++) {
			long diff = Math.abs(a[i] - b[i]);
			if (diff != 0)
				throw new RuntimeException("Element " + i + " not equals. " + a[i] + " " + b[i]);
		}
	}

	public static void assertEquals(float a[], float b[], float tol) {
		for (int i = 0; i < a.length; i++) {
			double diff = Math.abs(a[i] - b[i]);
			if (diff > tol)
				throw new RuntimeException("Element " + i + " not equals. " + a[i] + " " + b[i]);
		}
	}

	public static void assertEquals(ImageBase imgA, ImageBase imgB, double tol ) {

		// if no specialized check exists, use a slower generalized approach
		if( imgA instanceof ImageGray) {
			GImageGray a = FactoryGImageGray.wrap((ImageGray)imgA);
			GImageGray b = FactoryGImageGray.wrap((ImageGray)imgB);

			for( int y = 0; y < imgA.height; y++ ) {
				for( int x = 0; x < imgA.width; x++ ) {
					double valA = a.get(x,y).doubleValue();
					double valB = b.get(x,y).doubleValue();

					double difference = valA - valB;
					if( Math.abs(difference) > tol )
						throw new RuntimeException("Values not equal at ("+x+","+y+") "+valA+"  "+valB);
				}
			}
		} else if( imgA instanceof Planar && imgB instanceof Planar){
			Planar a = (Planar)imgA;
			Planar b = (Planar)imgB;

			if( a.getNumBands() != b.getNumBands() )
				throw new RuntimeException("Number of bands not equal");

			for( int band = 0; band < a.getNumBands(); band++ ) {
				assertEquals(a.getBand(band), b.getBand(band), tol);
			}
		} else if( imgA instanceof ImageMultiBand && imgB instanceof ImageMultiBand) {
			ImageMultiBand a = (ImageMultiBand)imgA;
			ImageMultiBand b = (ImageMultiBand)imgB;

			if( a.getNumBands() != b.getNumBands() )
				throw new RuntimeException("Number of bands not equal");

			int numBands = a.getNumBands();

			for( int y = 0; y < imgA.height; y++ ) {
				for( int x = 0; x < imgA.width; x++ ) {
					for( int band = 0; band < numBands; band++ ) {
						double valA = GeneralizedImageOps.get( a, x, y, band);
						double valB = GeneralizedImageOps.get( b, x, y, band);

						double difference = valA - valB;
						if( Math.abs(difference) > tol )
							throw new RuntimeException("Values not equal at ("+x+","+y+") "+valA+"  "+valB);
					}
				}
			}

		} else {
			throw new RuntimeException("Unknown image type");
		}
	}

	public static void assertEqualsInner(ImageBase imgA, ImageBase imgB, double tol , int borderX , int borderY ,
										 boolean relative ) {

		// if no specialized check exists, use a slower generalized approach
		if( imgA instanceof ImageGray) {
			GImageGray a = FactoryGImageGray.wrap((ImageGray)imgA);
			GImageGray b = FactoryGImageGray.wrap((ImageGray)imgB);

			for( int y = borderY; y < imgA.height-borderY; y++ ) {
				for( int x = borderX; x < imgA.width-borderX; x++ ) {
					double valA = a.get(x,y).doubleValue();
					double valB = b.get(x,y).doubleValue();

					double error = Math.abs(valA - valB);
					if( relative ) {
						double denominator = Math.abs(valA) + Math.abs(valB);
						if( denominator == 0 )
							denominator = 1;
						error /= denominator;
					}
					if( error > tol )
						throw new RuntimeException("Values not equal at ("+x+","+y+") "+valA+"  "+valB);
				}
			}
		} else if( imgA instanceof Planar){
			Planar a = (Planar)imgA;
			Planar b = (Planar)imgB;

			if( a.getNumBands() != b.getNumBands() )
				throw new RuntimeException("Number of bands not equal");

			for( int band = 0; band < a.getNumBands(); band++ ) {
				assertEqualsInner(a.getBand(band), b.getBand(band), tol, borderX, borderY, relative);
			}
		} else {
			throw new RuntimeException("Unknown image type");
		}
	}

	public static void assertEqualsInner(ImageBase imgA, ImageBase imgB, double tol ,
										 int borderX0 , int borderY0 , int borderX1 , int borderY1 ,
										 boolean relative ) {

		// if no specialized check exists, use a slower generalized approach
		if( imgA instanceof ImageGray) {
			GImageGray a = FactoryGImageGray.wrap((ImageGray) imgA);
			GImageGray b = FactoryGImageGray.wrap((ImageGray) imgB);

			for (int y = borderY0; y < imgA.height - borderY1; y++) {
				for (int x = borderX0; x < imgA.width - borderX1; x++) {
					double valA = a.get(x, y).doubleValue();
					double valB = b.get(x, y).doubleValue();

					double error = Math.abs(valA - valB);
					if (relative) {
						double denominator = Math.abs(valA) + Math.abs(valB);
						if (denominator == 0)
							denominator = 1;
						error /= denominator;
					}
					if (error > tol)
						throw new RuntimeException("Values not equal at (" + x + "," + y + ") " + valA + "  " + valB);
				}
			}
		} else if( imgA instanceof ImageInterleaved ) {
			GImageMultiBand a = FactoryGImageMultiBand.wrap((ImageInterleaved) imgA);
			GImageMultiBand b = FactoryGImageMultiBand.wrap((ImageInterleaved) imgB);

			int numBands = a.getNumberOfBands();

			for (int y = borderY0; y < imgA.height - borderY1; y++) {
				for (int x = borderX0; x < imgA.width - borderX1; x++) {
					for (int band = 0; band < numBands; band++) {
						double valA = a.get(x, y, band).doubleValue();
						double valB = b.get(x, y, band).doubleValue();

						double error = Math.abs(valA - valB);
						if (relative) {
							double denominator = Math.abs(valA) + Math.abs(valB);
							if (denominator == 0)
								denominator = 1;
							error /= denominator;
						}
						if (error > tol)
							throw new RuntimeException("Values not equal at (" + x + "," + y + ","+band+") " + valA + "  " + valB);
					}

				}
			}
		} else if( imgA instanceof Planar){
			Planar a = (Planar)imgA;
			Planar b = (Planar)imgB;

			if( a.getNumBands() != b.getNumBands() )
				throw new RuntimeException("Number of bands not equal");

			for( int band = 0; band < a.getNumBands(); band++ ) {
				assertEqualsInner(a.getBand(band), b.getBand(band), tol, borderX0, borderY0, borderX1, borderY1, relative);
			}
		} else {
			throw new RuntimeException("Unknown image type");
		}
	}

	public static void assertEqualsRelative(ImageBase imgA, ImageBase imgB, double tolFrac ) {

		// if no specialized check exists, use a slower generalized approach
		if( imgA instanceof ImageGray) {
			GImageGray a = FactoryGImageGray.wrap((ImageGray) imgA);
			GImageGray b = FactoryGImageGray.wrap((ImageGray) imgB);

			for (int y = 0; y < imgA.height; y++) {
				for (int x = 0; x < imgA.width; x++) {
					double valA = a.get(x, y).doubleValue();
					double valB = b.get(x, y).doubleValue();

					double difference = valA - valB;
					double max = Math.max(Math.abs(valA), Math.abs(valB));
					if (max == 0)
						max = 1;
					if (Math.abs(difference) / max > tolFrac)
						throw new RuntimeException("Values not equal at (" + x + "," + y + ") " + valA + "  " + valB);
				}
			}
		} else if( imgA instanceof ImageInterleaved) {
			GImageMultiBand a = FactoryGImageMultiBand.wrap(imgA);
			GImageMultiBand b = FactoryGImageMultiBand.wrap(imgB);

			float valueA[] = new float[ a.getNumberOfBands() ];
			float valueB[] = new float[ b.getNumberOfBands() ];

			for (int y = 0; y < imgA.height; y++) {
				for (int x = 0; x < imgA.width; x++) {
					a.get(x,y, valueA);
					b.get(x,y, valueB);

					for (int i = 0; i < a.getNumberOfBands(); i++) {
						double valA = valueA[i];
						double valB = valueB[i];

						double difference = valA - valB;
						double max = Math.max(Math.abs(valA), Math.abs(valB));
						if (max == 0)
							max = 1;
						if (Math.abs(difference) / max > tolFrac)
							throw new RuntimeException("Values not equal at (" + x + "," + y + ") " + valA + "  " + valB);
					}
				}
			}
		} else if( imgA instanceof Planar){
			Planar a = (Planar)imgA;
			Planar b = (Planar)imgB;

			if( a.getNumBands() != b.getNumBands() )
				throw new RuntimeException("Number of bands not equal");

			for( int band = 0; band < a.getNumBands(); band++ ) {
				assertEqualsRelative(a.getBand(band),b.getBand(band),tolFrac );
			}
		} else {
			throw new RuntimeException("Unknown image type");
		}
	}

	public static void assertEqualsBorder(ImageBase imgA, ImageBase imgB, double tol, int borderX, int borderY) {
		if( imgA instanceof ImageGray ) {
			assertEqualsBorder((ImageGray)imgA, (ImageGray)imgB, tol, borderX, borderY);
		} else if ( imgA instanceof ImageInterleaved ) {
			assertEqualsBorder((ImageInterleaved) imgA, (ImageInterleaved) imgB, tol, borderX, borderY);
		} else if( imgA instanceof Planar ) {
			Planar _imgA_ = (Planar)imgA;
			Planar _imgB_ = (Planar)imgB;
			final int numBands = _imgA_.getNumBands();
			for (int band = 0; band < numBands; band++) {
				assertEqualsBorder(_imgA_.getBand(band), _imgB_.getBand(band), tol, borderX, borderY);
			}
		} else {
			throw new RuntimeException("Unsupported image type");
		}
	}
	/**
	 * Checks to see if only the image borders are equal to each other within tolerance
	 */
	public static void assertEqualsBorder(ImageGray imgA, ImageGray imgB, double tol, int borderX, int borderY) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		GImageGray a = FactoryGImageGray.wrap(imgA);
		GImageGray b = FactoryGImageGray.wrap(imgB);

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < borderX; x++) {
				compareValues(tol, a, b, x, y);
			}
			for (int x = imgA.getWidth() - borderX; x < imgA.getWidth(); x++) {
				compareValues(tol, a, b, x, y);
			}
		}

		for (int x = borderX; x < imgA.getWidth() - borderX; x++) {
			for (int y = 0; y < borderY; y++) {
				compareValues(tol, a, b, x, y);
			}
			for (int y = imgA.getHeight() - borderY; y < imgA.getHeight(); y++) {
				compareValues(tol, a, b, x, y);
			}
		}
	}

	/**
	 * Checks to see if only the image borders are equal to each other within tolerance
	 */
	public static void assertEqualsBorder(ImageInterleaved imgA, ImageInterleaved imgB, double tol, int borderX, int borderY) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		int numBands = imgA.numBands;

		GImageMultiBand a = FactoryGImageMultiBand.wrap(imgA);
		GImageMultiBand b = FactoryGImageMultiBand.wrap(imgB);

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < borderX; x++) {
				for (int band = 0; band < numBands; band++) {
					compareValues(tol, a, b, x, y, band);
				}
			}
			for (int x = imgA.getWidth() - borderX; x < imgA.getWidth(); x++) {
				for (int band = 0; band < numBands; band++) {
					compareValues(tol, a, b, x, y, band);
				}
			}
		}

		for (int x = borderX; x < imgA.getWidth() - borderX; x++) {
			for (int y = 0; y < borderY; y++) {
				for (int band = 0; band < numBands; band++) {
					compareValues(tol, a, b, x, y, band);
				}
			}
			for (int y = imgA.getHeight() - borderY; y < imgA.getHeight(); y++) {
				for (int band = 0; band < numBands; band++) {
					compareValues(tol, a, b, x, y, band);
				}
			}
		}
	}

	private static void compareValues(double tol, GImageGray a, GImageGray b, int x, int y) {
		double normalizer = Math.abs(a.get(x, y).doubleValue()) + Math.abs(b.get(x, y).doubleValue());
		if (normalizer < 1.0) normalizer = 1.0;
		if (Math.abs(a.get(x, y).doubleValue() - b.get(x, y).doubleValue()) / normalizer > tol)
			throw new RuntimeException("values not equal at (" + x + " " + y + ") " + a.get(x, y) + "  " + b.get(x, y));
	}

	private static void compareValues(double tol, GImageMultiBand a, GImageMultiBand b, int x, int y, int band) {
		double normalizer = Math.abs(a.get(x, y, band).doubleValue()) + Math.abs(b.get(x, y, band).doubleValue());
		if (normalizer < 1.0) normalizer = 1.0;
		if (Math.abs(a.get(x, y, band).doubleValue() - b.get(x, y, band).doubleValue()) / normalizer > tol)
			throw new RuntimeException("values not equal at (" + x + " " + y + " "+band+") " + a.get(x, y, band) + "  " + b.get(x, y, band));
	}

	public static void checkBorderZero(ImageGray outputImage, int border) {
		GImageGray img = FactoryGImageGray.wrap(outputImage);

		for (int y = 0; y < img.getHeight(); y++) {
			if (y >= border && y < img.getHeight() - border)
				continue;

			for (int x = 0; x < img.getWidth(); x++) {
				if (x >= border && x < img.getWidth() - border)
					continue;
				if (img.get(x, y).intValue() != 0)
					throw new RuntimeException("The border is not zero: "+x+" "+y);
			}
		}
	}

	public static void checkBorderZero(ImageGray outputImage, int borderX0 , int borderY0 , int borderX1 , int borderY1 ) {
		GImageGray img = FactoryGImageGray.wrap(outputImage);

		for (int y = 0; y < img.getHeight(); y++) {
			if (y >= borderY0  && y < img.getHeight() - borderY1)
				continue;

			for (int x = 0; x < img.getWidth(); x++) {
				if (x >= borderX0 && x < img.getWidth() - borderX1)
					continue;
				if (img.get(x, y).intValue() != 0)
					throw new RuntimeException("The border is not zero: "+x+" "+y);
			}
		}
	}

	public static void printDiff(ImageGray imgA, ImageGray imgB) {

		GImageGray a = FactoryGImageGray.wrap(imgA);
		GImageGray b = FactoryGImageGray.wrap(imgB);

		System.out.println("------- Difference -----------");
		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				double diff = Math.abs(a.get(x, y).doubleValue() - b.get(x, y).doubleValue());
				System.out.printf("%2d ", (int) diff);
			}
			System.out.println();
		}
	}

	public static void printDiffBinary(GrayU8 imgA, GrayU8 imgB) {

		System.out.println("------- Difference -----------");
		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				if( imgA.unsafe_get(x,y) != imgB.unsafe_get(x,y))
					System.out.print(" x");
				else
					System.out.print(" .");
			}
			System.out.println();
		}
	}

	public static Object randomArray( Class type , int length , Random rand ) {
		Object ret;

		if( type == byte[].class ) {
			byte[] data = new byte[length];
			for (int i = 0; i < length; i++) {
				data[i] = (byte)(rand.nextInt(Byte.MAX_VALUE-Byte.MIN_VALUE)+Byte.MIN_VALUE);
			}
			ret = data;
		} else if( type == short[].class ) {
			short[] data = new short[length];
			for (int i = 0; i < length; i++) {
				data[i] = (short)(rand.nextInt(Short.MAX_VALUE-Short.MIN_VALUE)+Short.MIN_VALUE);
			}
			ret = data;
		} else if( type == int[].class ) {
			int[] data = new int[length];
			for (int i = 0; i < length; i++) {
				data[i] = rand.nextInt(1000)-500;
			}
			ret = data;
		} else if( type == long[].class ) {
			long[] data = new long[length];
			for (int i = 0; i < length; i++) {
				data[i] = rand.nextLong();
			}
			ret = data;
		} else if( type == float[].class ) {
			float[] data = new float[length];
			for (int i = 0; i < length; i++) {
				data[i] = rand.nextFloat()-0.5f;
			}
			ret = data;
		} else if( type == double[].class ) {
			double[] data = new double[length];
			for (int i = 0; i < length; i++) {
				data[i] = rand.nextDouble()-0.5;
			}
			ret = data;
		} else {
			throw new RuntimeException("Unknown. "+type.getSimpleName());
		}

		return ret;
	}

	public static Object primitive( Object v , Class type ) {
		Number value = (Number)v;
		if( type == byte.class ) {
			return value.byteValue();
		} else if( type == short.class ) {
			return value.shortValue();
		} else if( type ==int.class ) {
			return value.intValue();
		} else if( type == long.class ) {
			return value.longValue();
		} else if( type == float.class ) {
			return value.floatValue();
		} else if( type == double.class ) {
			return value.doubleValue();
		} else {
			throw new RuntimeException("Unknown. "+type.getSimpleName());
		}
	}

	public static Object createInstance(Class<?> type) {
		try {
			return type.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static WorkArrays createWorkArray(Class<?> type, int length ) {
		WorkArrays w = (WorkArrays)createInstance(type);
		w.reset(length);
		return w;
	}
}
