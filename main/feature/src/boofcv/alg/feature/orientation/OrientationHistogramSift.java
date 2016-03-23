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

package boofcv.alg.feature.orientation;

import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.CircularIndex;
import boofcv.numerics.InterpolateArray;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageGray;
import georegression.metric.UtilAngle;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.Arrays;

/**
 * <p>
 * Computes the orientation of a region around a point in scale-space as specified in the SIFT [1] paper.  A histogram
 * of angles is computed using a weighted sum of image derivatives.  The size of the region is specified by the
 * scale function parameter.  Every pixel inside the sample region is read and contributes to the angle estimation.
 * If the image border is encountered the sample return is truncated.
 * </p>
 *
 * <p>
 * To get the orientation for the largest peak invoke {@link #getPeakOrientation()}.  Other
 * </p>
 *
 * Differences from paper:
 * <ul>
 * <li>The angle in each bin is set to the atan2(y,x) of the weighted sum of image derivative</li>
 * <li>Interpolation is done using a 2nd degree polynomial instead of a parabola.</li>
 * </ul>
 *
 * <p>
 * [1] Lowe, D. "Distinctive image features from scale-invariant keypoints".
 * International Journal of Computer Vision, 60, 2 (2004), pp.91--110.
 * </p>
 *
 * @author Peter Abeles
 */
public class OrientationHistogramSift<Deriv extends ImageGray>
{
	// How much does it inflate the scale by
	private double sigmaEnlarge;
	// Storage for orientation histogram. Each bin is for angles from i*histAngleBin to (i+1)*histAngleBin
	double histogramMag[];
	// histograms containing the sum of each derivative
	double histogramX[];
	double histogramY[];
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


	// spacial image gradient of closest image in scale-space
	private GImageGray derivX,derivY;

	InterpolateArray approximateGauss;
	double approximateStep = 0.1;

	/**
	 * Configures orientation estimation
	 *
	 * @param histogramSize Number of elements in the histogram.  Standard is 36
	 * @param sigmaEnlarge How much the scale is enlarged by.  Standard is 1.5
	 */
	public OrientationHistogramSift(int histogramSize ,
									double sigmaEnlarge ,
									Class<Deriv> derivType )
	{
		this.histogramMag = new double[ histogramSize ];
		this.histogramX = new double[ histogramSize ];
		this.histogramY = new double[ histogramSize ];

		this.sigmaEnlarge = sigmaEnlarge;

		this.histAngleBin = 2.0*Math.PI/histogramSize;

		// compute an approximation of a Gaussian distribution as a function of the distance squared
		double samples[] = new double[ (int)(4*4/approximateStep) ];
		for( int i = 0; i < samples.length; i++ ) {
			double dx2 = i*approximateStep;
			samples[i] = Math.exp(-0.5*dx2 );
		}
		approximateGauss = new InterpolateArray(samples);

		this.derivX = FactoryGImageGray.create(derivType);
		this.derivY = FactoryGImageGray.create(derivType);
	}

	/**
	 * Specify the input image
	 */
	public void setImageGradient(Deriv derivX, Deriv derivY ) {
		this.derivX.wrap(derivX);
		this.derivY.wrap(derivY);
	}

	/**
	 * Estimates the orientation(s) of a region at the specified location and scale
	 *
	 * @param c_x Location x-axis
	 * @param c_y Location y-axis
	 * @param sigma blur standard deviations of detected feature.  Also referred to as scale.
	 */
	public void process( double c_x , double c_y , double sigma )
	{
		// convert to image coordinates
		int x = (int)(c_x + 0.5);
		int y = (int)(c_y + 0.5);

		// Estimate its orientation(s)
		computeHistogram(x, y, sigma );

		// compute the descriptor
		findHistogramPeaks();
	}

	/**
	 * Constructs the histogram around the specified point.
	 *
	 * @param c_x Center x-axis
	 * @param c_y Center y-axis
	 * @param sigma Scale of feature, adjusted for local octave
	 */
	void computeHistogram(int c_x, int c_y, double sigma) {
		int r = (int)Math.ceil(sigma * sigmaEnlarge);

		// specify the area being sampled
		bound.x0 = c_x - r;
		bound.y0 = c_y - r;
		bound.x1 = c_x + r + 1;
		bound.y1 = c_y + r + 1;

		ImageGray rawDX = derivX.getImage();
		ImageGray rawDY = derivY.getImage();

		// make sure it is contained in the image bounds
		BoofMiscOps.boundRectangleInside(rawDX,bound);

		// clear the histogram
		Arrays.fill(histogramMag,0);
		Arrays.fill(histogramX,0);
		Arrays.fill(histogramY,0);

		// construct the histogram
		for( int y = bound.y0; y < bound.y1; y++ ) {
			// iterate through the raw array for speed
			int indexDX = rawDX.startIndex + y*rawDX.stride + bound.x0;
			int indexDY = rawDY.startIndex + y*rawDY.stride + bound.x0;

			for( int x = bound.x0; x < bound.x1; x++ ) {
				float dx = derivX.getF(indexDX++);
				float dy = derivY.getF(indexDY++);

				// edge intensity and angle
				double magnitude = Math.sqrt(dx*dx + dy*dy);
				double theta = UtilAngle.domain2PI(Math.atan2(dy,dx));

				// weight from gaussian
				double weight = computeWeight( x-c_x, y-c_y , sigma );

				// histogram index
				int h = (int)(theta / histAngleBin) % histogramMag.length;

				// update the histogram
				histogramMag[h] += magnitude*weight;
				histogramX[h] += dx*weight;
				histogramY[h] += dy*weight;
			}
		}
	}

	/**
	 * Finds local peaks in histogram and selects orientations.  Location of peaks is interpolated.
	 */
	void findHistogramPeaks() {
		// reset data structures
		peaks.reset();
		angles.reset();
		peakAngle = 0;

		// identify peaks and find the highest peak
		double largest = 0;
		int largestIndex = -1;
		double before = histogramMag[ histogramMag.length-2 ];
		double current = histogramMag[ histogramMag.length-1 ];
		for(int i = 0; i < histogramMag.length; i++ ) {
			double after = histogramMag[ i ];

			if( current > before && current > after ) {
				int currentIndex = CircularIndex.addOffset(i,-1,histogramMag.length);
				peaks.push(currentIndex);
				if( current > largest ) {
					largest = current;
					largestIndex = currentIndex;
				}
			}
			before = current;
			current = after;
		}

		if( largestIndex < 0 )
			return;

		// see if any of the other peaks are within 80% of the max peak
		double threshold = largest*0.8;
		for( int i = 0; i < peaks.size; i++ ) {
			int index = peaks.data[i];
			current = histogramMag[index];
			if( current >= threshold) {
				double angle = computeAngle(index);

				angles.push( angle );

				if( index == largestIndex )
					peakAngle = angle;
			}
		}
	}

	/**
	 * Compute the angle.  The angle for each neighbor bin is found using the weighted sum
	 * of the derivative.  Then the peak index is found by 2nd order polygon interpolation.  These two bits of
	 * information are combined and used to return the final angle output.
	 *
	 * @param index1 Histogram index of the peak
	 * @return angle of the peak. -pi to pi
	 */
	double computeAngle( int index1 ) {

		int index0 = CircularIndex.addOffset(index1,-1, histogramMag.length);
		int index2 = CircularIndex.addOffset(index1, 1, histogramMag.length);

		// compute the peak location using a second order polygon
		double v0 = histogramMag[index0];
		double v1 = histogramMag[index1];
		double v2 = histogramMag[index2];

		double offset = FastHessianFeatureDetector.polyPeak(v0,v1,v2);

		// interpolate using the index offset and angle of its neighbor
		return interpolateAngle(index0, index1, index2, offset);
	}

	/**
	 * Given the interpolated index, compute the angle from the 3 indexes.  The angle for each index
	 * is computed from the weighted gradients.
	 * @param offset Interpolated index offset relative to index0.  range -1 to 1
	 * @return Interpolated angle.
	 */
	double interpolateAngle(int index0, int index1, int index2, double offset) {
		double angle1 = Math.atan2(histogramY[index1],histogramX[index1]);
		double deltaAngle;
		if( offset < 0 ) {
			double angle0 = Math.atan2(histogramY[index0],histogramX[index0]);
			deltaAngle = UtilAngle.dist(angle0,angle1);

		} else {
			double angle2 = Math.atan2(histogramY[index2], histogramX[index2]);
			deltaAngle = UtilAngle.dist(angle2,angle1);
		}
		return UtilAngle.bound(angle1 + deltaAngle*offset);
	}

	/**
	 * Computes the weight based on a centered Gaussian shaped function.  Interpolation is used to speed up the process
	 */
	double computeWeight( double deltaX , double deltaY , double sigma ) {
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
