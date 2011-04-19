package gecv.struct.image;

/**
 * <p>
 * An image where the primitive type is a 32-bit floating point number.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImageFloat32 extends ImageBase<ImageFloat32> {

	public float data[];

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public ImageFloat32(int width, int height) {
		super(width, height);
	}

	public ImageFloat32() {
	}

	/**
	 * Returns the value of the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return an intensity value.
	 */
	public float get(int x, int y) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds: ( " + x + " , " + y + " )");

		return data[getIndex(x, y)];
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	public void set(int x, int y, float value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		data[getIndex(x, y)] = value;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (float[]) data;
	}

	@Override
	public ImageFloat32 _createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new ImageFloat32();
		return new ImageFloat32(imgWidth, imgHeight);
	}

	@Override
	protected Class<?> _getPrimitiveType() {
		return float.class;
	}
}
