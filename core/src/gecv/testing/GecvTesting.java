package gecv.testing;

import gecv.struct.image.*;
import sun.awt.image.ByteInterleavedRaster;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Functions to aid in unit testing code for correctly handling sub-images
 *
 * @author Peter Abeles
 */
public class GecvTesting {

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
			Class<?> params[] = m.getParameterTypes();
			Object[] inputs = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				inputs[i] = createImage(params[i], 10, 20);
			}

			try {
				m.invoke(testClass, inputs);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			// test negative cases
			for (int target = 0; target < params.length; target++) {
				for (int i = 0; i < params.length; i++) {
					if (i != target)
						inputs[i] = createImage(params[i], 10, 20);
					else
						inputs[i] = createImage(params[i], 11, 22);
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

	private static boolean areAllInputsImages(Method m) {

		Class<?> params[] = m.getParameterTypes();

		if (params.length == 0)
			return false;

		for (Class<?> p : params) {
			if (!ImageBase.class.isAssignableFrom(p)) {
				return false;
			}
		}

		return true;
	}

	public static ImageBase createImage(Class<?> type, int width, int height) {
		if (type == ImageInt8.class) {
			return new ImageInt8(width, height);
		} else if (type == ImageInt16.class) {
			return new ImageInt16(width, height);
		} else if (type == ImageFloat32.class) {
			return new ImageFloat32(width, height);
		} else if (type == ImageInterleavedInt8.class) {
			return new ImageInterleavedInt8(width, height, 1);
		}
		throw new RuntimeException("Unknown type");
	}

	/**
	 * Tests the specified function with the original image provided and with an equivalent
	 * sub-image.  The two results are then compared. The function being tested must only
	 * have one input parameter of type {@link ImageInt8}.
	 *
	 * @param testClass   Instance of the class that contains the function being tested.
	 * @param function	The name of the function being tested.
	 * @param checkEquals Checks to see if the two images have been modified the same way on output
	 * @param orig		The original input image
	 */
	// TODO make sure pixels outside are not modified of sub-matrix
	// todo have the submatrices be from different shaped inputs
	@SuppressWarnings({"unchecked"})
	public static void checkSubImage(Object testClass,
									 String function,
									 boolean checkEquals,
									 ImageBase... orig) {
		try {
			ImageBase[] larger = new ImageBase[orig.length];
			ImageBase[] subImg = new ImageBase[orig.length];
			Class<?> param[] = new Class<?>[orig.length];

			for (int i = 0; i < orig.length; i++) {
				// copy the original image inside of a larger image
				larger[i] = orig[i]._createNew(orig[i].getWidth() + 10, orig[i].getHeight() + 12);
				// extract a sub-image and make it equivalent to the original image.
				subImg[i] = larger[i].subimage(5, 6, 5 + orig[i].getWidth(), 6 + orig[i].getHeight());
				subImg[i].setTo(orig[i]);

				param[i] = orig[i].getClass();
			}


			// first try it with the original image
			Method m = findMethod(testClass.getClass(), function, param);

			m.invoke(testClass, orig);

			// now try it with the sub-image
			m.invoke(testClass, subImg);

			// the result should be the identical
			if (checkEquals) {
				for (int i = 0; i < orig.length; i++) {
					if (orig[i] instanceof ImageInt8)
						assertEquals((ImageInt8) orig[i], (ImageInt8) subImg[i], 0);
					else if (orig[i] instanceof ImageInt16)
						assertEquals((ImageInt16) orig[i], (ImageInt16) subImg[i], 0);
					else if (orig[i] instanceof ImageInt32)
						assertEquals((ImageInt32) orig[i], (ImageInt32) subImg[i], 0);
					else if (orig[i] instanceof ImageInterleavedInt8)
						assertEquals((ImageInterleavedInt8) orig[i], (ImageInterleavedInt8) subImg[i]);
					else if (orig[i] instanceof ImageFloat32)
						assertEquals((ImageFloat32) orig[i], (ImageFloat32) subImg[i]);
					else
						throw new RuntimeException("Unknown type " + orig[i].getClass().getSimpleName() + ".  Add it here");
				}
			}

		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Searches for a function which is a perfect match.  if none it exists it checks
	 * to see if any matches that could accept an input of the specified type.  If there
	 * is only one such match that is returned.
	 */
	private static Method findMethod(Class<?> type, String name, Class<?>[] params) {
		Method methods[] = type.getMethods();

		List<Method> found = new ArrayList<Method>();
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
				if (params[i].isAssignableFrom(a[i])) {
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

		throw new RuntimeException("Couldn't find matching function to " + name);
	}

	/**
	 * Checks to see if thw two images are equivalent.  Note that this is not the same
	 * as identical since they can be sub-images.
	 *
	 * @param imgA An image.
	 * @param imgB An image.
	 */
	public static void assertEqualsGeneric(ImageBase imgA, ImageBase imgB, int tolInt, double tolFloat) {
		if (imgA.getClass() != imgB.getClass())
			throw new RuntimeException("Images are of different types");

		if (imgA.getClass() == ImageInt8.class)
			assertEquals((ImageInt8) imgA, (ImageInt8) imgB, 0);
		else if (imgA.getClass() == ImageInt16.class)
			assertEquals((ImageInt16) imgA, (ImageInt16) imgB, 0);
		else if (imgA.getClass() == ImageInt32.class)
			assertEquals((ImageInt32) imgA, (ImageInt32) imgB, 0);
		else if (imgA.getClass() == ImageFloat32.class)
			assertEquals((ImageFloat32) imgA, (ImageFloat32) imgB, 0, (float) tolFloat);
		else
			throw new RuntimeException("Unknown type");

	}

	/**
	 * Checks to see if thw two images are equivalent.  Note that this is not the same
	 * as identical since they can be sub-images.
	 *
	 * @param imgA		 An image.
	 * @param imgB		 An image.
	 * @param ignoreBorder Pixels this close to the border will not be compared.
	 */
	public static void assertEquals(ImageInt8 imgA, ImageInt8 imgB, int ignoreBorder) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = ignoreBorder; y < imgA.getHeight() - ignoreBorder; y++) {
			for (int x = ignoreBorder; x < imgA.getWidth() - ignoreBorder; x++) {
				if (imgA.get(x, y) != imgB.get(x, y))
					throw new RuntimeException("value not equal");
			}
		}
	}

	/**
	 * Checks to see if thw two images are equivalent.  Note that this is not the same
	 * as identical since they can be sub-images.
	 *
	 * @param imgA		 An image.
	 * @param imgB		 An image.
	 * @param ignoreBorder
	 */
	public static void assertEquals(ImageInt16 imgA, ImageInt16 imgB, int ignoreBorder) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = ignoreBorder; y < imgA.getHeight() - ignoreBorder; y++) {
			for (int x = ignoreBorder; x < imgA.getWidth() - ignoreBorder; x++) {
				if (imgA.get(x, y) != imgB.get(x, y))
					throw new RuntimeException("values not equal at (" + x + " " + y + ") vals " + imgA.get(x, y) + " " + imgB.get(x, y));
			}
		}
	}

	/**
	 * Checks to see if thw two images are equivalent.  Note that this is not the same
	 * as identical since they can be sub-images.
	 *
	 * @param imgA		 An image.
	 * @param imgB		 An image.
	 * @param ignoreBorder
	 */
	public static void assertEquals(ImageInt32 imgA, ImageInt32 imgB, int ignoreBorder) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = ignoreBorder; y < imgA.getHeight() - ignoreBorder; y++) {
			for (int x = ignoreBorder; x < imgA.getWidth() - ignoreBorder; x++) {
				if (imgA.get(x, y) != imgB.get(x, y))
					throw new RuntimeException("values not equal at (" + x + " " + y + ") vals " + imgA.get(x, y) + " " + imgB.get(x, y));
			}
		}
	}

	/**
	 * Checks to see if thw two images are equivalent.  Note that this is not the same
	 * as identical since they can be sub-images.
	 *
	 * @param imgA An image.
	 * @param imgB An image.
	 */
	public static void assertEquals(ImageFloat32 imgA, ImageFloat32 imgB) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				if (imgA.get(x, y) != imgB.get(x, y))
					throw new RuntimeException("values not equal at (" + x + " " + y + ") "+imgA.get(x,y) + "  "+imgB.get(x,y));
			}
		}
	}

	/**
	 * Checks to see if thw two images are equivalent.  Note that this is not the same
	 * as identical since they can be sub-images.
	 *
	 * @param imgA		 An image.
	 * @param imgB		 An image.
	 * @param ignoreBorder
	 */
	public static void assertEquals(ImageFloat32 imgA, ImageFloat32 imgB, int ignoreBorder, float tol) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = ignoreBorder; y < imgA.getHeight() - ignoreBorder; y++) {
			for (int x = ignoreBorder; x < imgA.getWidth() - ignoreBorder; x++) {
				if (Math.abs(imgA.get(x, y) - imgB.get(x, y)) > tol)
					throw new RuntimeException("values not equal at (" + x + " " + y + ") " + imgA.get(x, y) + "  " + imgB.get(x, y));
			}
		}
	}

	public static void assertEquals(ImageInterleavedInt8 imgA, ImageInterleavedInt8 imgB) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		if (imgA.numBands != imgB.numBands)
			throw new RuntimeException("Number of bands are not equal");

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				for (int k = 0; k < imgA.numBands; k++)
					if (imgA.getBand(x, y, k) != imgB.getBand(x, y, k))
						throw new RuntimeException("value not equal");
			}
		}
	}

	/**
	 * Checks to see if the BufferedImage has the same intensity values as the ImageInt8
	 *
	 * @param imgA BufferedImage
	 * @param imgB ImageInt8
	 */
	public static void checkEquals(BufferedImage imgA, ImageInt8 imgB) {

		if (imgA.getRaster() instanceof ByteInterleavedRaster) {
			ByteInterleavedRaster raster = (ByteInterleavedRaster) imgA.getRaster();

			if (raster.getNumBands() == 1) {
				// handle a special case where the RGB conversion is screwed
				for (int i = 0; i < imgA.getHeight(); i++) {
					for (int j = 0; j < imgA.getWidth(); j++) {
						int valB = imgB.get(j, i);
						int valA = raster.getDataStorage()[i * imgA.getWidth() + j];

						if (valA != valB)
							throw new RuntimeException("Images are not equal");
					}
				}
				return;
			}
		}

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				int rgb = imgA.getRGB(x, y);

				int gray = (byte) ((((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3);
				int grayB = imgB.get(x, y);

				if (Math.abs(gray - grayB) != 0) {
					throw new RuntimeException("images are not equal: ");
				}
			}
		}
	}

	/**
	 * Checks to see if the BufferedImage has the same intensity values as the ImageInt8
	 *
	 * @param imgA BufferedImage
	 * @param imgB ImageInt8
	 */
	public static void checkEquals(BufferedImage imgA, ImageInterleavedInt8 imgB) {

		if (imgA.getRaster() instanceof ByteInterleavedRaster) {
			ByteInterleavedRaster raster = (ByteInterleavedRaster) imgA.getRaster();

			if (raster.getNumBands() == 1) {
				// handle a special case where the RGB conversion is screwed
				for (int i = 0; i < imgA.getHeight(); i++) {
					for (int j = 0; j < imgA.getWidth(); j++) {
						byte valB = imgB.getBand(j, i, 0);
						byte valA = raster.getDataStorage()[i * imgA.getWidth() + j];

						if (valA != valB)
							throw new RuntimeException("Images are not equal");
					}
				}
				return;
			}
		}

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				int rgb = imgA.getRGB(x, y);

				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;

				if (Math.abs(b - imgB.getBand(x, y, 0) & 0xFF) != 0)
					throw new RuntimeException("images are not equal: ");
				if (Math.abs(g - imgB.getBand(x, y, 1) & 0xFF) != 0)
					throw new RuntimeException("images are not equal: ");
				if (Math.abs(r - imgB.getBand(x, y, 2) & 0xFF) != 0)
					throw new RuntimeException("images are not equal: ");
			}
		}
	}
}
