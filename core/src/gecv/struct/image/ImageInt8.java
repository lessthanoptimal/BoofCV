package gecv.struct.image;

/**
 * <p>
 * An image where the primitive type is a byte.  By default all operations treat elements
 * in this image as an unsigned bytes.
 * </p>
 * <p/>
 * <p>
 * NOTE: An integer is returned and not a byte since Java will convert all bytes into integers internally.  No
 * performance boost by using a byte and its more of a hassle.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImageInt8 extends ImageBase<ImageInt8> {

	public byte data[];

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public ImageInt8(int width, int height) {
		super(width, height);
	}

	public ImageInt8() {
	}

	/**
	 * <p>
	 * Returns the value of the specified pixel.
	 * </p>
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
	 * Returns the value of the specified pixel as an unsigned value.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return an intensity value.
	 */
	public int getU(int x, int y) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		return data[getIndex(x, y)] & 0xFF;
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

		data[getIndex(x, y)] = (byte) value;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (byte[]) data;
	}

	@Override
	public ImageInt8 _createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new ImageInt8();
		return new ImageInt8(imgWidth, imgHeight);
	}

	@Override
	protected Class<?> _getPrimitiveType() {
		return byte.class;
	}
}
