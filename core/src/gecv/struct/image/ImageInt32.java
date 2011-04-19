package gecv.struct.image;

/**
 * <p>
 * An image where the primitive type is an 'int'.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImageInt32 extends ImageBase<ImageInt32> {

	public int data[];

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public ImageInt32(int width, int height) {
		super(width, height);
	}

	public ImageInt32() {
	}

	/**
	 * Returns the value of the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return an intensity value.
	 */
	public int get(int x, int y) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		return data[getIndex(x, y)];
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	public void set(int x, int y, int value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		data[getIndex(x, y)] = (short) value;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (int[]) data;
	}

	@Override
	public ImageInt32 _createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new ImageInt32();
		return new ImageInt32(imgWidth, imgHeight);
	}

	@Override
	protected Class<?> _getPrimitiveType() {
		return int.class;
	}
}
