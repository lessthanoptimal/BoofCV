package gecv.alg.filter.binary;

import gecv.struct.image.ImageInt8;

/**
 * <p>
 * DESIGN NOTE: 8-bit integer images ({@link ImageInt8}) are used instead of images composed of boolean values because
 * there is no performance advantage.  According to the virtual machines specification binary arrays are stored as
 * byte arrays with 1 representing true and 0 representing false.
 * </p>
 *
 * @author Peter Abeles
 */
// todo benchmark byte and boolean images to see which one is fastest to work with
// stronger typing of a binary image would be good...
public class BinaryImageOps {

	public static ImageInt8 erode(ImageInt8 input, ImageInt8 output) {
		return null;
	}

	public static ImageInt8 dilate(ImageInt8 input, ImageInt8 output) {
		return null;
	}

	public static ImageInt8 removePointNoise(ImageInt8 input, ImageInt8 output) {
		return null;
	}
}
