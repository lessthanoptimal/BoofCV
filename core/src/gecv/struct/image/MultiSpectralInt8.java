package gecv.struct.image;

/**
 * Multiple spectral image composed {@link ImageInt8} images.
 *
 * @author Peter Abeles
 */
public class MultiSpectralInt8 extends MultiSpectral<ImageInt8> {

	public MultiSpectralInt8(int width, int height, int numBands) {
		super(ImageInt8.class, width, height, numBands);
	}

	public MultiSpectralInt8(int numBands) {
		super(ImageInt8.class, numBands);
	}

	@Override
	protected ImageInt8 declareImage(int width, int height) {
		return new ImageInt8(width, height);
	}

	public byte[] get(int x, int y, byte[] storage) {
		if (storage == null) {
			storage = new byte[bands.length];
		}

		for (int i = 0; i < bands.length; i++) {
			storage[i] = (byte) bands[i].get(x, y);
		}

		return storage;
	}

	public void set(int x, int y, byte[] value) {
		for (int i = 0; i < bands.length; i++) {
			bands[i].set(x, y, value[i]);
		}
	}
}
