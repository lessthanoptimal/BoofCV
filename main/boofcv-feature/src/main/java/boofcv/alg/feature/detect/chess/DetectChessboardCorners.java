/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.abst.filter.binary.BinaryContourFinderLinearExternal;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.filter.binary.ContourPacked;
import boofcv.alg.interpolate.ImageLineIntegral;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.ConnectRule;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.List;

import static boofcv.misc.CircularIndex.addOffset;

/**
 * Detects corners found in chessboard patterns. The first step is to compute corner intensity using a Shi-Tomasi
 * corner detector. For a perfect chessboard pattern, the peak will be centered on the corner and have a flat
 * square pattern. The width of the square will be the kernel's radius * 2. It's flat because you can move the kernel
 * around the corner and get the exact same response from the corner detector. Because it has a flat response of known
 * size this can then be used to filter out other objects which cause a response. For example, a corner on a square
 * will have a single pixel peak, in the absence of noise. Random noise from trees and other complex patterns tends
 * to be much larger than a few pixels. Thus a binary image is created from the corner intensity image and blobs
 * with contours of a known size are found. The orientation of each feature is found and a more expensive corner
 * intensity computation is performed to futher remove false positives.
 *
 * Corner orientation. The orientation of a corner is only unique up the 1/2 a circle or PI radians. The corner's
 * direction is defined as the vector which points along a line that passes through the center of the dark region.
 *
 * Corner intensity. The second corner intensity that is computed (first was Shi-Tomasi) is found by computing the
 * difference between radial lines around the circle and their adjacent. Basically a line which should be black
 * is subtracted from a line which should be white with the result summed. A random point is unlikely to have
 * this pattern.
 *
 * Sub-pixel or sub-bin accuracy is found for location using mean-shift and orientation by fitting a quadratic curve.
 * See code comments for more detail.
 *
 * @author Peter Abeles
 */
public class DetectChessboardCorners {

	// intensity image is forced to have this many integer levels for OTSU
	public static final int GRAY_LEVELS = 300;
	// feature radius and width in Shi-Tomasi corner detector
	int shiRadius, shiWidth;

	// computes corner intensity image
	GradientCornerIntensity<GrayF32> cornerIntensity;

	// storage for corner detector output
	GrayF32 intensity = new GrayF32(1,1);
	// Thresholded intensity image
	GrayU8 binary = new GrayU8(1,1);
	// Thresholding algorithm
	InputToBinary<GrayF32> inputToBinary;

	// Find the small blobs that might be corner features
	BinaryContourFinderLinearExternal contourFinder = new BinaryContourFinderLinearExternal();

	// Storage for found corners
	FastQueue<ChessboardCorner> corners = new FastQueue<>(ChessboardCorner.class,true);
	// Storage for points in one blob's contour
	FastQueue<Point2D_I32> contour = new FastQueue<>(Point2D_I32.class,true);

	// Computes the image gradient
//	ImageGradient<GrayF32,GrayF32> gradient = FactoryDerivative.three(GrayF32.class,GrayF32.class);
	ImageGradient<GrayF32,GrayF32> gradient = FactoryDerivative.prewitt(GrayF32.class,GrayF32.class);

	// Storage for gradient of input image
	GrayF32 derivX = new GrayF32(1,1), derivY= new GrayF32(1,1);

	// used to sample derivative images at non-integer values
	InterpolatePixelS<GrayF32> interpX = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.ZERO);
	InterpolatePixelS<GrayF32> interpY = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.ZERO);

	// Used to compute line integrals of spokes around a corner
	ImageBorder<GrayF32> borderImg = FactoryImageBorder.single(GrayF32.class,BorderType.ZERO);
	ImageLineIntegral integral = new ImageLineIntegral();

	// for mean-shift
	InterpolatePixelS<GrayF32> intensityInterp = FactoryInterpolation.bilinearPixelS(GrayF32.class,BorderType.ZERO);
	public boolean useMeanShift = true;

	// predeclare memory for compute a feature's orientation
	private final int numLines = 16;
	private final double lines[] = new double[numLines];
	private final double smoothed[] = new double[numLines];

	/**
	 * Declares internal data structures
	 */
	public DetectChessboardCorners() {
		setKernelRadius(1);

		setThresholding(FactoryThresholdBinary.globalOtsu(0, GRAY_LEVELS,false,GrayF32.class));
		contourFinder.setConnectRule(ConnectRule.EIGHT);

		// just give it something. this will be changed later
		borderImg.setImage(derivX);
		integral.setImage(FactoryGImageGray.wrap(borderImg));
	}

	/**
	 * Computes chessboard corners inside the image
	 * @param input Gray image. Not modified.
	 */
	public void process( GrayF32 input ) {
//		System.out.println("ENTER CHESSBOARD CORNER "+input.width+" x "+input.height);
		borderImg.setImage(input);
		gradient.process(input,derivX,derivY);

		interpX.setImage(derivX);
		interpY.setImage(derivY);

		cornerIntensity.process(derivX,derivY,intensity);
		intensityInterp.setImage(intensity);

		// adjust intensity value so that its between 0 and levels for OTSU thresholding
		float featmax = ImageStatistics.max(intensity);
		PixelMath.multiply(intensity, GRAY_LEVELS /featmax,intensity);

//		int N = intensity.width*input.height;
//		for (int i = 0; i < N; i++) {
//			if( intensity.data[i] <= 2f ) {
//				intensity.data[i] = 0f;
//			}
//		}

		// convert into a binary image with high feature intensity regions being 1
		inputToBinary.process(intensity,binary);
		// find the small regions. Th se might be where corners are
		contourFinder.process(binary);

		corners.reset();
		List<ContourPacked> packed = contourFinder.getContours();
//		System.out.println("  * features.size = "+packed.size());
		for (int i = 0; i < packed.size(); i++) {
			contourFinder.loadContour(i,contour);

			ChessboardCorner c = corners.grow();

			UtilPoint2D_I32.mean(contour.toList(),c);
			// compensate for the bias caused by how pixels are counted.
			// Example: a 4x4 region is expected. Center should be at (2,2) but will instead be (1.5,1.5)
			c.x += 0.5;
			c.y += 0.5;

			computeFeatures(c.x,c.y,c);

//			System.out.println("radius = "+radius+" angle = "+c.angle);
//			System.out.println("intensity "+c.intensity);
			if( c.intensity < 50 ) { // TODO make configurable
				corners.removeTail();
			}

			if( useMeanShift )
				meanShiftLocation(c);
		}

//		System.out.println("Dropped "+dropped+" / "+packed.size());
	}

	/**
	 * Computes features for the corner (angle and intensity) using line integrals in a spokes pattern.
	 *
	 * The feature's angle has a value from -pi/2 to pi/2 radians. It is found by finding the line/spoke with the
	 * minimum value that maximizes distance from the bright lines.
	 *
	 * Intensity is found by subtracting bright lines from the dark line on the other side. dark/light lines are
	 * offset by 90 degrees.
	 */
	private void computeFeatures(double cx , double cy , ChessboardCorner corner ) {
		double r = shiRadius +2;

		for (int i = 0; i < numLines; i++) {
			// TODO precompute sines and cosines
			double angle = Math.PI*i/ numLines -Math.PI/2.0;
			double c = Math.cos(angle);
			double s = Math.sin(angle);

			double x0 = cx-r*c;
			double y0 = cy-r*s;
			double x1 = cx+r*c;
			double y1 = cy+r*s;

			lines[i] = integral.compute(x0,y0,x1,y1);
		}

		// smooth by applying a block filter. This will ensure it doesn't point towards an edge which just happens
		// to be slightly darker than the center
		int r_smooth = numLines /8; //  when viewed head on the black region will be 1/4 of a circle
		int w_smooth = r_smooth*2+1;
		for (int i = 0; i < numLines; i++) {
			int start = addOffset(i,-r_smooth, numLines);

			double sum = 0;
			for (int j = 0; j < w_smooth; j++) {
				int index = addOffset(start,j, numLines);
				sum += lines[index];
			}
			smoothed[i] = sum / w_smooth;
		}

		int indexMin = 0;
		double valueMin = Double.MAX_VALUE;
		for (int i = 0; i < numLines; i++) {
			if( smoothed[i] < valueMin ) {
				valueMin = smoothed[i];
				indexMin = i;
			}
		}

		// Use a quadratic to estimate the peak's location to a sub-bin accuracy
		double value0 = smoothed[ addOffset(indexMin,-1, numLines)];
		double value2 = smoothed[ addOffset(indexMin, 1, numLines)];

		double adjustedIndex = indexMin + FastHessianFeatureDetector.polyPeak(value0,valueMin,value2);
		corner.orientation = Math.PI*adjustedIndex/ numLines -Math.PI/2.0;

		// Score the corner's fit quality using the fact that the function would be osccilate (sin/cosine)
		// and values 90 degrees offset are at the other side
		double intensity = 0;
		for (int i = 0; i < numLines /2; i++) {
			int idx0 = addOffset(indexMin,i- numLines /4, numLines);
			int idx1 = addOffset(idx0, numLines /2, numLines);
			intensity += smoothed[idx1] - smoothed[idx0];
		}
		corner.intensity = intensity/(r*2+1+ numLines/2);
	}

	/**
	 * Use mean shift to improve the accuracy of the corner's location. A kernel is selected which is slightly larger
	 * than the "flat" intensity of the corner should be when over a chess pattern.
	 */
	public void meanShiftLocation( ChessboardCorner c ) {
		float meanX = (float)c.x;
		float meanY = (float)c.y;

		// The peak in intensity will be in -r to r region, but smaller values will be -2*r to 2*r
		int radius = this.shiRadius*2;
		for (int iteration = 0; iteration < 5; iteration++) {
			float adjX = 0;
			float adjY = 0;
			float total = 0;

			for (int y = -radius; y < radius; y++) {
				float yy = y;
				for (int x = -radius; x < radius; x++) {
					float xx = x;
					float v = intensityInterp.get(meanX+xx,meanY+yy);
					// Again, this adjustment to account for how pixels are counted. See center from contour computation
					adjX += (xx+0.5)*v;
					adjY += (yy+0.5)*v;
					total += v;
				}
			}

			meanX += adjX/total;
			meanY += adjY/total;
		}

		c.x = meanX;
		c.y = meanY;
	}

	public GrayF32 getIntensity() {
		return intensity;
	}

	public GrayU8 getBinary() {
		return binary;
	}

	public FastQueue<ChessboardCorner> getCorners() {
		return corners;
	}

	/**
	 * @param inputToBinary Thresholding algorithm. If using a histogram based approach keep in mind
	 *                      that the number of values is specified by grayLevels. OTSU is recommended
	 */
	public void setThresholding( InputToBinary<GrayF32> inputToBinary ) {
		this.inputToBinary = inputToBinary;
	}

	public void setKernelRadius( int radius ) {
		this.shiRadius = radius;
		this.shiWidth = radius*2+1;
		cornerIntensity =  FactoryIntensityPointAlg.shiTomasi(radius, false, GrayF32.class);

		// Tell it to ignore all contours which are smaller or larger than the flat region should be in the
		// shi-tomasi corner intensity image. See clas description for a definition of the flat region
		// Flat region is a square with width of (radius*2+1) where radius is the radius of the kernel.
		if( radius == 1 ) {
			contourFinder.setMaxContour((shiWidth + 1) * 4 + 4);
			contourFinder.setMinContour(shiWidth * 4 - 4);
		} else {
			contourFinder.setMaxContour((shiWidth + 1) * 4 + 4);
			contourFinder.setMinContour((shiWidth - 1) * 4 - 4);
		}
	}

	public int getKernelRadius() {
		return shiRadius;
	}

}
