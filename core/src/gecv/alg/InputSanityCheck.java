package gecv.alg;

import gecv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
public class InputSanityCheck {

	public static void checkSameShape(ImageBase<?> imgA, ImageBase<?> imgB) {
		if (imgA.width != imgB.width)
			throw new IllegalArgumentException("Image widths do not match.");
		if (imgA.height != imgB.height)
			throw new IllegalArgumentException("Image heights do not match.");
	}

	public static void checkSameShape(ImageBase<?> imgA, ImageBase<?> imgB, ImageBase<?> imgC) {
		if (imgA.width != imgB.width || imgA.width != imgC.width)
			throw new IllegalArgumentException("Image widths do not match.");
		if (imgA.height != imgB.height || imgA.height != imgC.height)
			throw new IllegalArgumentException("Image heights do not match.");
	}
}
