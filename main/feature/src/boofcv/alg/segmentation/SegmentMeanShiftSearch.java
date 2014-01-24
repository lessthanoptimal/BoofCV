/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation;

import boofcv.alg.weights.WeightDistance_F32;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Performs the search step in mean-shift image segmentation [1].  The mode of a pixel is the point at which mean-shift
 * converges when initialized at that pixel.  Pixels which have the same mode belong to the same segment.
 * Likelihood (or weights) used to compute the mean value comes from a 2D spacial kernel and color distance functions.
 * Color distance is computed as the difference between the mean and the color value at a pixel which is then
 * converted into a weight.  The two weights are then multiplied together and normalized based on the total
 * weight from all the samples.
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
 * <li>The kernel's spacial radius is specified by the radius of 'weightSpacial' and the gray/color radius is specified
 * by 'weightGray'.  Those two functions also specified the amount of weight assigned to each sample in the
 * mean-shift kernel based on its distance from the spacial and color means.<li>
 * <li>The distance passed into the 'weightColor' function is the Euclidean distance squared.E.g. ||a-b||<sup>2</sup></li>
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

	// specifies the size of the mean-shift kernel
	protected int radiusX,radiusY;
	protected int widthX,widthY;
	// Sample weight given location relative to center
	protected WeightPixel_F32 weightSpacial;
	// Sample weight given difference in gray scale value
	protected WeightDistance_F32 weightColor;

	// converts a pixel location into the index of the mode that mean-shift converged to
	protected ImageSInt32 pixelToMode = new ImageSInt32(1,1);

	// Quick look up for the index of a mode from an image pixel.  It is possible for a pixel that is a mode
	// to have mean-shift converge to a different pixel
	protected ImageSInt32 quickMode = new ImageSInt32(1,1);

	// location of each peak in image pixel indexes
	protected FastQueue<Point2D_I32> modeLocation = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

	// number of members in this peak
	protected GrowQueue_I32 modeMemberCount = new GrowQueue_I32();

	// storage for segment colors
	protected FastQueue<float[]> modeColor;

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
	 * @param weightSpacial Weighting function/kernel for spacial component
	 * @param weightColor Weighting function/kernel for distance of color component
	 * @param fast Improve runtime by approximating running mean-shift on each pixel. Try true.
	 */
	public SegmentMeanShiftSearch(int maxIterations, float convergenceTol,
								  WeightPixel_F32 weightSpacial,
								  WeightDistance_F32 weightColor,
								  boolean fast ) {
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;
		this.weightSpacial = weightSpacial;
		this.weightColor = weightColor;
		this.fast = fast;

		this.radiusX = weightSpacial.getRadiusX();
		this.radiusY = weightSpacial.getRadiusY();
		this.widthX = radiusX*2+1;
		this.widthY = radiusY*2+1;
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
	 * From peak index to pixel index
	 */
	public ImageSInt32 getPixelToRegion() {
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
}
