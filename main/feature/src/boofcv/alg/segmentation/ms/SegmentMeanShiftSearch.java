/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.ms;

import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Performs the search step in mean-shift image segmentation [1].  The mode of a pixel is the point at which mean-shift
 * converges when initialized at that pixel.  Pixels which have the same mode belong to the same segment. The weight
 * kernel G(|x-y|^2/h) has independent normalization factors h for spacial and color components.  A precomputed
 * Normal distribution is used for the weight kernel.
 * </p>
 * <p>
 * Output is provided in the form of an image where each pixel contains the index of a region the pixel belongs to.
 * Three other lists provide color value of the region, number of pixels in the region and the location
 * of the mean-shift peak for that region.  This output is unlikely to be final processing step since it will over
 * segment the image.  Merging of similar modes and pruning of small regions is a common next step.
 * </p>
 *
 * <p>
 * An approximation of running mean-shift on each pixel is performed if the 'fast' flag is set to true.  The
 * approximation is about 5x faster and works by saving the mean-shift trajectory [2].  All points along the trajectory
 * are given the same mode. When performing mean-shift if a pixel is encountered which has already been assigned a
 * mode the search stops. This approximation tends to produce more regions and reduces clustering quality in high
 * texture regions.
 * </p>
 *
 * <p>
 * NOTES:
 * <ul>
 * <li>Spacial distance is normalized by dividing the found Euclidean distance squared by the maximum possible
 * Euclidean distance squared, thus ensuring it will be between 0 and 1.</li>
 * <li>Color distance is normalized by dividing it by the maximum allows Euclidean distance squared.  If its distance
 * is more than the maximum allowed value then G() will be zero.</li>
 * <li>Image edges are handled by truncating the spacial kernel.  This truncation
 * will create an asymmetric kernel, but there is really no good way to handle image edges.</li>
 * </ul>
 * </p>
 *
 * <p>
 * CITATIONS:<br>
 * <ol>
 * <li>Comaniciu, Dorin, and Peter Meer. "Mean shift analysis and applications." Computer Vision, 1999.
 * The Proceedings of the Seventh IEEE International Conference on. Vol. 2. IEEE, 1999.</li>
 * <li>Christoudias, Christopher M., Bogdan Georgescu, and Peter Meer. "Synergism in low level vision."
 * Pattern Recognition, 2002. Proceedings. 16th International Conference on. Vol. 4. IEEE, 2002.</li>
 * </ol>
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SegmentMeanShiftSearch<T extends ImageBase> {

	// used to detect convergence of mean-shift
	protected int maxIterations;
	protected float convergenceTol;

	// specifies the size of the mean-shift kernel in spacial pixels
	protected int radiusX,radiusY;
	protected int widthX,widthY;
	// specifies the maximum Euclidean distance squared for the color components
	protected float maxColorDistanceSq;

	// converts a pixel location into the index of the mode that mean-shift converged to
	protected GrayS32 pixelToMode = new GrayS32(1,1);

	// Quick look up for the index of a mode from an image pixel.  It is possible for a pixel that is a mode
	// to have mean-shift converge to a different pixel
	protected GrayS32 quickMode = new GrayS32(1,1);

	// location of each peak in image pixel indexes
	protected FastQueue<Point2D_I32> modeLocation = new FastQueue<>(Point2D_I32.class, true);

	// number of members in this peak
	protected GrowQueue_I32 modeMemberCount = new GrowQueue_I32();

	// storage for segment colors
	protected FastQueue<float[]> modeColor;

	// quick lookup for spacial distance
	protected float[] spacialTable;

	// quick lookup for Gaussian kernel
	protected float weightTable[] = new float[100];

	// If true it will use the fast approximation of mean-shift
	boolean fast;

	// The input image
	protected T image;

	// mode of mean-shift
	protected float modeX, modeY;

	/**
	 * Configures mean-shift segmentation
	 *
	 * @param maxIterations Maximum number of mean-shift iterations.  Try 30
	 * @param convergenceTol When the change is less than this amount stop.  Try 0.005
	 * @param radiusX Spacial kernel radius x-axis
	 * @param radiusY Spacial kernel radius y-axis
	 * @param maxColorDistance Maximum allowed Euclidean distance squared for the color component
	 * @param fast Improve runtime by approximating running mean-shift on each pixel. Try true.
	 */
	public SegmentMeanShiftSearch(int maxIterations, float convergenceTol,
								  int radiusX , int radiusY , float maxColorDistance ,
								  boolean fast ) {
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;
		this.fast = fast;

		this.radiusX = radiusX;
		this.radiusY = radiusY;
		this.widthX = radiusX*2+1;
		this.widthY = radiusY*2+1;

		this.maxColorDistanceSq = maxColorDistance*maxColorDistance;

		// precompute the distance each pixel is from the sample point
		// normalize the values such that the maximum distance will be 1
		spacialTable = new float[widthX*widthY];
		int indexKernel = 0;
		float maxRadius = radiusX*radiusX + radiusY*radiusY;
		for( int y = -radiusY; y <= radiusY; y++ ) {
			for( int x = -radiusX; x <= radiusX; x++ ) {
				spacialTable[indexKernel++] = (x*x + y*y)/maxRadius;
			}
		}

		// precompute the weight table for inputs from 0 to 1, inclusive
		for( int i = 0; i < weightTable.length; i++ ) {
			weightTable[i] = (float)Math.exp(-i/(float)(weightTable.length-1));
		}
	}

	/**
	 * Performs mean-shift clustering on the input image
	 *
	 * @param image Input image
	 */
	public abstract void process( T image );

	/**
	 * Returns the Euclidean distance squared between the two vectors
	 */
	public static float distanceSq( float[] a , float[]b ) {
		float ret = 0;
		for( int i = 0; i < a.length; i++ ) {
			float d = a[i] - b[i];
			ret += d*d;
		}
		return ret;
	}

	/**
	 * Returns the weight given the normalized distance.  Instead of computing the kernel distance every time
	 * a lookup table with linear interpolation is used.  The distance has a domain from 0 to 1, inclusive
	 *
	 * @param distance Normalized Euclidean distance squared.  From 0 to 1.
	 * @return Weight.
	 */
	protected float weight( float distance ) {
		float findex = distance*100f;
		int index = (int)findex;

		if( index >= 99 )
			return weightTable[99];

		float sample0 = weightTable[index];
		float sample1 = weightTable[index+1];

		float w = findex-index;
		return sample0*(1f-w) + sample1*w;
	}

	/**
	 * From peak index to pixel index
	 */
	public GrayS32 getPixelToRegion() {
		return pixelToMode;
	}

	/**
	 * Location of each peak in the image
	 */
	public FastQueue<Point2D_I32> getModeLocation() {
		return modeLocation;
	}

	/**
	 * Number of pixels which each peak as a member
	 */
	public GrowQueue_I32 getRegionMemberCount() {
		return modeMemberCount;
	}

	public FastQueue<float[]> getModeColor() {
		return modeColor;
	}

	public abstract ImageType<T> getImageType();
}
