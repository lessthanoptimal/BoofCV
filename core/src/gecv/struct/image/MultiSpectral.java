package gecv.struct.image;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Image class for images that are composed of multiple bands
 *
 * @author Peter Abeles
 */
public abstract class MultiSpectral<T extends ImageBase> {

	Class<T> type;

	public int width;
	public int height;
	public T bands[];

	public MultiSpectral(Class<T> type, int width, int height, int numBands) {
		this.type = type;
		this.width = width;
		this.height = height;
		bands = (T[]) Array.newInstance(type, numBands);

		for (int i = 0; i < numBands; i++) {
			bands[i] = declareImage(width, height);
		}
	}

	protected MultiSpectral(Class<T> type, int numBands) {
		this.type = type;
		bands = (T[]) Array.newInstance(type, numBands);
	}

	public Class<T> getType() {
		return type;
	}

	protected abstract T declareImage(int width, int height);

	public int getNumBands() {
		return bands.length;
	}

	public T getBand(int band) {
		if (band >= bands.length || band < 0)
			throw new IllegalArgumentException("The specified band is out of bounds");

		return bands[band];
	}
}
