package gecv.alg.detect.corner;

import gecv.misc.DiscretizedCircle;
import gecv.struct.image.ImageFloat32;

/**
 * An implementation of {@link FastCorner12_F} algorithm that is designed to be
 * more easily read and verified for correctness.  The price is some computations are done
 * that might not always be necissary.
 * <p/>
 * In this implementation it is assumed that valid pixels must form a chain of at least
 * 12 continuous pixels
 * <p/>
 * See the paper: "Faster and better: a machine learning approach to corner detection" by
 * Edward Rosten, Reid Porter, and Tom Drummond
 * <p/>
 * A global threshold is used to select which features are used
 *
 * @author Peter Abeles
 */
public class FastCorner12_F {

	private int minCont;
	private final static int radius = 3;

	private ImageFloat32 img;
	// how similar do the pixel in the circle need to be to the center pixel
	private float pixelTol;

	// relative offsets of pixel locations in a circle
	private int[] offsets;

	// list of features it detected
	private int[] feats;
	private int numFeats;

	private ImageFloat32 featureIntensity;

	/**
	 * Constructor
	 *
	 * @param img	  The image where features are extracted from.
	 * @param pixelTol The difference in intensity value from the center pixel the circle needs to be.
	 * @param minCont  The minimum number of continuous pixels that a circle needs to be a corner.
	 */
	public FastCorner12_F(ImageFloat32 img,
						  int pixelTol, int minCont) {
		this(img.getWidth(), img.getHeight(), pixelTol, minCont);
		this.img = img;
	}

	public FastCorner12_F(int imgWidth, int imgHeight, float pixelTol, int minCont) {
		this.pixelTol = pixelTol;
		this.minCont = minCont;

		offsets = DiscretizedCircle.imageOffsets(radius, imgWidth);

		featureIntensity = new ImageFloat32(imgWidth, imgHeight);

		feats = new int[imgWidth];
	}

	public int[] getFeatures() {
		return feats;
	}

	public int getNumFeatures() {
		return numFeats;
	}

	public void setInput(ImageFloat32 image) {
		// adjust for the size of the input if possible
		if (image.getWidth() != featureIntensity.getWidth() || image.getHeight() != featureIntensity.getHeight()) {
			featureIntensity.reshape(image.getWidth(), image.getHeight());
		}

		this.img = image;
	}

	public ImageFloat32 getIntensity() {
		return featureIntensity;
	}

	public void process() {
		numFeats = 0;
		final float[] data = img.data;

		final int width = img.getWidth();
		final int yEnd = img.getHeight() - radius;

		final float[] inten = featureIntensity.data;

		int offA = offsets[0];
		int offB = offsets[4];
		int offC = offsets[8];
		int offD = offsets[12];

		for (int y = radius; y < yEnd; y++) {
			int rowStart = width * y;
			int endX = rowStart + width - radius;

			for (int index = rowStart + radius; index < endX; index++) {

				float center = data[index];

				float a = data[index + offA];
				float b = data[index + offB];
				float c = data[index + offC];
				float d = data[index + offD];

				float thresh = center - pixelTol;

				int action = 0;

				// check to see if it is significantly below tthe center pixel
				if (a < thresh && c < thresh) {
					if (b < thresh) {
						action = -1;
					} else if (d < thresh) {
						action = -1;
					}
				} else if (b < thresh && d < thresh) {
					if (a < thresh) {
						action = -1;
					} else if (c < thresh) {
						action = -1;
					}
				} else {
					// see if it is significantly more than the center pixel
					thresh = center + pixelTol;

					if (a > thresh && c > thresh) {
						if (d > thresh) {
							action = 1;
						} else if (b > thresh) {
							action = 1;
						}
					}
					if (b > thresh && d > thresh) {
						if (a > thresh) {
							action = 1;
						} else if (c > thresh) {
							action = 1;
						}
					}
				}

				// can't be a corner here so just continue to the next pixel
				if (action == 0) {
					inten[index] = 0F;
					continue;
				}

				boolean isCorner = false;

				// move until it find a valid pixel
				float totalDiff = 0;

				// see if the first pixel is valid or not
				float val = a - center;
				if ((action == -1 && val < -pixelTol) || val > pixelTol) {
					// if it is valid then it needs to deal with wrapping
					int i;
					// find the point a bad pixel is found
					totalDiff += val;
					for (i = 1; i < offsets.length; i++) {
						val = data[index + offsets[i]] - center;

						if (action == -1) {
							if (val >= -pixelTol) break;
						} else if (val <= pixelTol) break;

						totalDiff += val;
					}

					int frontLength = i;

					if (frontLength < minCont) {
						// go the other direction
						for (i = offsets.length - 1; i >= 0; i--) {
							val = data[index + offsets[i]] - center;

							if (action == -1) {
								if (val >= -pixelTol) break;
							} else if (val <= pixelTol) break;
							totalDiff += val;
						}
						if (offsets.length - 1 - i + frontLength >= minCont) {
							isCorner = true;
						}
					} else {
						isCorner = true;
					}

				} else {
					// find the first good pixel
					int start;
					for (start = 0; start < offsets.length; start++) {
						val = data[index + offsets[start]] - center;

						if (action == -1) {
							if (val < -pixelTol) break;
						} else if (val > pixelTol) break;
					}

					// find the point where the good pixels stop
					int stop;
					for (stop = start + 1; stop < offsets.length; stop++) {
						val = data[index + offsets[stop]] - center;

						if (action == -1) {
							if (val >= -pixelTol) break;
						} else if (val <= pixelTol) break;
						totalDiff += val;
					}

					isCorner = stop - start >= minCont;

				}

				if (isCorner) {
					inten[index] = action == -1 ? -totalDiff : totalDiff;
					// declare room for more features
					if (numFeats >= feats.length) {
						int temp[] = new int[numFeats * 2];
						System.arraycopy(feats, 0, temp, 0, feats.length);
						feats = temp;
					}
					feats[numFeats++] = index;
				} else {
					inten[index] = 0F;
				}
			}
		}
	}

}