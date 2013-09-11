/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.orientation;

import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.misc.BoofMiscOps;
import boofcv.numerics.InterpolateArray;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageFloat32;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Computes the orientation of a region around a point in scale-space as specified in the SIFT [1] paper.  A
 * histogram of gradients is computed and the largest peaks are returned as the region's direction.  The
 * gradient is weighted using a Gaussian distribution.
 * </p>
 *
 * <p>
 * INTERPOLATION: Instead of fitting a curve to adjacent bins, the solution is computed by summing
 * up dx and dy independently in each bin.  Then when a bin is selected the angle is set to the atan() of dx,dy sums.
 * </p>
 *
 * <p>
 * [1] Lowe, D. "Distinctive image features from scale-invariant keypoints".
 * International Journal of Computer Vision, 60, 2 (2004), pp.91--110.
 * </p>
 *
 * @author Peter Abeles
 */
public class OrientationHistogramSift {

	// Converts a distribution's sigma into a region radius to sample
	private double sigmaToRadius;

	// How much does it inflate the scale by
	private double sigmaEnlarge;
	// Storage for orientation histogram. Each bin is for angles from i*histAngleBin to (i+1)*histAngleBin
	private double histogram[];
	// histograms containing the sum of each derivative
	private double histogramX[];
	private double histogramY[];
	// Number of radians each bin corresponds to.  histAngleBin = 2*PI/histogram.length
	private double histAngleBin;

	// peaks in histogram
	private GrowQueue_I32 peaks = new GrowQueue_I32(10);

	// Found orientations of the feature
	private GrowQueue_F64 angles = new GrowQueue_F64(10);
	// Angle of the largest peak
	private double peakAngle;

	// local region from which the orientation is computed
	private ImageRectangle bound = new ImageRectangle();

	// which image in the scale space is being processed
	private int imageIndex;
	// the pixel scale of the image being processed
	private double pixelScale;

	// Image scale space
	private SiftImageScaleSpace ss;

	// local scale space from which the orientation is computed from
	private ImageFloat32 image;
	private ImageFloat32 derivX;
	private ImageFloat32 derivY;

	InterpolateArray approximateGauss;
	double approximateStep = 0.1;

	/**
	 * Configures orientation estimation
	 *
	 * @param histogramSize Number of elements in the histogram.  Standard is 36
	 * @param sigmaToRadius Convert a sigma to region radius.  Try 2.5
	 * @param sigmaEnlarge How much the scale is enlarged by.  Standard is 1.5
	 */
	public OrientationHistogramSift( int histogramSize ,
									 double sigmaToRadius,
									 double sigmaEnlarge) {
		this.histogram = new double[ histogramSize ];
		this.histogramX = new double[ histogramSize ];
		this.histogramY = new double[ histogramSize ];

		this.sigmaToRadius = sigmaToRadius;
		this.sigmaEnlarge = sigmaEnlarge;

		this.histAngleBin = 2.0*Math.PI/histogramSize;

		// compute an approximation of a Gaussian distribution as a function of the distance squared
		double samples[] = new double[ (int)(4*4/approximateStep) ];
		for( int i = 0; i < samples.length; i++ ) {
			double dx2 = i*approximateStep;
			samples[i] = Math.exp(-0.5*dx2 );
		}
		approximateGauss = new InterpolateArray(samples);
	}

	/**
	 * Specify the input
	 *
	 * @param ss Scale space representation of input image
	 */
	public void setScaleSpace( SiftImageScaleSpace ss ) {
		this.ss = ss;
	}

	/**
	 * Estimates the orientation(s) of a region at the specified location and scale
	 *
	 * @param c_x Location x-axis
	 * @param c_y Location y-axis
	 * @param scale Scale (blur standard deviations)
	 */
	public void process( double c_x , double c_y , double scale )
	{
		// determine where this feature lies inside the scale-space
		imageIndex = ss.scaleToImageIndex( scale );
		pixelScale = ss.imageIndexToPixelScale( imageIndex );

		image = ss.getPyramidLayer(imageIndex);
		derivX = ss.getDerivativeX(imageIndex);
		derivY = ss.getDerivativeY(imageIndex);

		// convert to image coordinates
		int x = (int)(c_x/pixelScale + 0.5);
		int y = (int)(c_y/pixelScale + 0.5);
		double adjustedScale = scale/pixelScale;

		// Estimate its orientation(s)
		computeHistogram(x, y, adjustedScale);

		// compute the descriptor
		computeOrientations();
	}

	/**
	 * Constructs the histogram around the specified point.
	 *
	 * @param c_x Center x-axis
	 * @param c_y Center y-axis
	 * @param scale Scale of feature, adjusted for local octave
	 */
	private void computeHistogram(int c_x, int c_y, double scale) {
		double localSigma = scale*sigmaEnlarge;
		int r = (int)Math.ceil(localSigma * sigmaToRadius);

		// specify the area being sampled
		bound.x0 = c_x - r;
		bound.y0 = c_y - r;
		bound.x1 = c_x + r + 1;
		bound.y1 = c_y + r + 1;

		// make sure it is contained in the image bounds
		BoofMiscOps.boundRectangleInside(image,bound);

		// clear the histogram
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i] = 0;
			histogramX[i] = 0;
			histogramY[i] = 0;
		}

		// construct the histogram
		for( int y = bound.y0; y < bound.y1; y++ ) {
			// iterate through the raw array for speed
			int indexDX = derivX.startIndex + y*derivX.stride + bound.x0;
			int indexDY = derivY.startIndex + y*derivY.stride + bound.x0;

			for( int x = bound.x0; x < bound.x1; x++ ) {
				float dx = derivX.data[indexDX++];
				float dy = derivY.data[indexDY++];

				// edge intensity and angle
				double m = Math.sqrt(dx*dx + dy*dy);
				double theta = Math.atan2(dy,dx) + Math.PI;
				// weight
				double w = computeWeight( x-c_x, y-c_y , localSigma );

				// histogram index
				int h = (int)(theta / histAngleBin);

				// pathological case
				if( h == histogram.length )
					h = 0;

				// update the histogram
				histogram[h] += m*w;
				histogramX[h] += dx;
				histogramY[h] += dy;
			}
		}
	}

	/**
	 * Finds peaks in histogram and selects orientations.  Location of peaks is interpolated.
	 */
	private void computeOrientations() {
		// identify peaks an find the highest peak
		peaks.reset();
		double largest = 0;
		int largestIndex = -1;
		double before = histogram[ histogram.length-1 ];
		double current = histogram[ 0 ];
		for( int i = 0; i < histogram.length; i++ ) {
			double after = histogram[ (i + 1) % histogram.length ];

			if( current > before && current > after ) {
				peaks.push(i);
				if( current > largest ) {
					largest = current;
					largestIndex = i;
				}
			}
			before = current;
			current = after;
		}

		if( largestIndex < 0 )
			return;

		// see if any of the other peaks are within 80% of the max peak
		angles.reset();
		for( int i = 0; i < peaks.size; i++ ) {
			int index = peaks.data[i];
			current = histogram[index];
			if( largest*0.8 <= current ) {

				double angle = Math.atan2(histogramY[index],histogramX[index]);

				angles.push( angle );

				if( index == largestIndex )
					peakAngle = angle;
			}
		}
	}

	/**
	 * Computes the weigthing using a Gaussian shaped function.  Interpolation is used to speed up the process
	 */
	private double computeWeight( double deltaX , double deltaY , double sigma ) {
		// the exact equation
//		return Math.exp(-0.5 * ((deltaX * deltaX + deltaY * deltaY) / (sigma * sigma)));

		// approximation below.  when validating this approach it produced results that were within
		// floating point tolerance of the exact solution, but much faster
		double d =  ((deltaX * deltaX + deltaY * deltaY) / (sigma * sigma))/approximateStep;
		if( approximateGauss.interpolate(d) ) {
			return approximateGauss.value;
		} else
			return 0;
	}


	/**
	 * Which image in the scale space is being used.
	 */
	public int getImageIndex() {
		return imageIndex;
	}

	/**
	 * The ratio of pixels in the octave to original image
	 */
	public double getPixelScale() {
		return pixelScale;
	}

	/**
	 * A list of found orientations
	 *
	 * @return orientations
	 */
	public GrowQueue_F64 getOrientations() {
		return angles;
	}

	/**
	 * Orientation of the largest peak
	 */
	public double getPeakOrientation() {
		return peakAngle;
	}
}
