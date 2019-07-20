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

import boofcv.abst.feature.detect.peak.SearchLocalPeak;
import boofcv.abst.filter.binary.BinaryContourFinderLinearExternal;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.detect.intensity.XCornerAbeles2019Intensity;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.filter.binary.ContourPacked;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.ImageLineIntegral;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.feature.detect.peak.ConfigMeanShiftSearch;
import boofcv.factory.feature.detect.peak.FactorySearchLocalPeak;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.ConnectRule;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

import static boofcv.misc.CircularIndex.addOffset;

/**
 *
 * @author Peter Abeles
 */
public class DetectChessboardCorners2<T extends ImageGray<T>, D extends ImageGray<D>> {
	// Threshold used to filter out corners
	double cornerIntensityThreshold = 1.0;

	int radius = 4;
	float intensityThresh = 2.0f*0.10f; // max possible value for intensity is 2.0

	float edgeRatioThreshold = 0.05f;

	T blurred;
	BlurFilter<T> blurFilter;

	XCornerAbeles2019Intensity computeIntensity = new XCornerAbeles2019Intensity();
	SearchLocalPeak<GrayF32> meanShift;

	FastQueue<Point2D_F32> maximums = new FastQueue<>(Point2D_F32.class,true);
	FastQueue<ChessboardCorner> corners = new FastQueue<>(ChessboardCorner.class,true);
	List<ChessboardCorner> filtered = new ArrayList<>();

	// storage for corner detector output
	GrayF32 intensity = new GrayF32(1,1);

	// Used to compute line integrals of spokes around a corner
	ImageBorder<T> borderImg;
	ImageLineIntegral integral = new ImageLineIntegral();

	// for mean-shift
	InterpolatePixelS<GrayF32> inputInterp = FactoryInterpolation.bilinearPixelS(GrayF32.class,BorderType.ZERO);
	InterpolatePixelS<GrayF32> intensityInterp = FactoryInterpolation.bilinearPixelS(GrayF32.class,BorderType.ZERO);
	public boolean useMeanShift = true;

	private GrayU8 binary = new GrayU8(1,1);

	// predeclare memory for compute a feature's orientation
	private final int numLines = 16;
	private final double lines[] = new double[numLines];
	private final double smoothed[] = new double[numLines];
	private final Kernel1D_F64 kernelSmooth = FactoryKernelGaussian.gaussian(1,true,64,-1,numLines/4);

	FastQueue<Point2D_I32> outsideCircle4 = new FastQueue<>(Point2D_I32.class,true);
	FastQueue<Point2D_I32> outsideCircle3 = new FastQueue<>(Point2D_I32.class,true);

	// input image type
	Class<T> imageType;
	Class<D> derivType;

	// blob feature detector
	FastQueue<Point2D_I32> contour = new FastQueue<>(Point2D_I32.class,true);
	BinaryContourFinderLinearExternal contourFinder = new BinaryContourFinderLinearExternal();

	/**
	 * Declares internal data structures
	 */
	public DetectChessboardCorners2(Class<T> imageType ) {
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		blurFilter = FactoryBlurFilter.gaussian(imageType,-1,1);
		blurred = GeneralizedImageOps.createSingleBand(imageType,1,1);

		// just give it something. this will be changed later
		borderImg = FactoryImageBorder.single(imageType,BorderType.ZERO);
		borderImg.setImage(GeneralizedImageOps.createSingleBand(imageType,1,1));
		integral.setImage(FactoryGImageGray.wrap(borderImg));

		{
			ConfigMeanShiftSearch config = new ConfigMeanShiftSearch(5,1e-6);
			config.positiveOnly = true;
			config.odd = false;
			meanShift = FactorySearchLocalPeak.meanShiftGaussian(config,GrayF32.class);
			meanShift.setSearchRadius(2);
		}

		{
			contourFinder.setConnectRule(ConnectRule.EIGHT);
			contourFinder.setMaxContour(25);
			contourFinder.setMinContour(5);
		}

		DiscretizedCircle.coordinates(4, outsideCircle4);
		DiscretizedCircle.coordinates(3, outsideCircle3);
	}

	/**
	 * Computes chessboard corners inside the image
	 * @param input Gray image. Not modified.
	 */
	public void process( T input ) {
//		System.out.println("ENTER CHESSBOARD CORNER "+input.width+" x "+input.height);
		borderImg.setImage(input);

		blurFilter.process(input,blurred);
		computeIntensity.process((GrayF32)blurred, intensity);

		intensityInterp.setImage(intensity);
		inputInterp.setImage((GrayF32)blurred);
		meanShift.setImage(intensity);

		contourMaximums();

		double maxEdge = 0;
		double maxIntensity = 0;

		filtered.clear();
		corners.reset();
//		System.out.println("  * features.size = "+packed.size());
		for (int i = 0; i < maximums.size(); i++) {
			Point2D_F32 p = maximums.get(i);

			int xx = (int)(p.x+0.5f);
			int yy = (int)(p.y+0.5f);

			if( !checkNegativeInside(xx,yy,12)) {
				continue;
			}

			if( !checkNegativeOutside3(xx,yy,outsideCircle4,4,4)) {
				continue;
			}

			if( !checkNegativeOutside3(xx,yy,outsideCircle3,3,4)) {
				continue;
			}

			// TODO much faster if negative region can be used to eliminate at this stage

			ChessboardCorner c = corners.grow();

			// compensate for the bias caused by how pixels are counted.
			// Example: a 4x4 region is expected. Center should be at (2,2) but will instead be (1.5,1.5)
			c.x = p.x;
			c.y = p.y;

			if( useMeanShift ) {
				meanShift.search((float)c.x,(float)c.y);
				c.x = meanShift.getPeakX();
				c.y = meanShift.getPeakY();
			}

			// account for bias due to discretion
			c.x += 0.5f;
			c.y += 0.5f;

			computeFeatures(c.x,c.y,c);

			boolean accepted =
					c.intensity >= cornerIntensityThreshold;
					checkCircular(c);

//			boolean accepted = true;

			if( !accepted ) {
				corners.removeTail();
			} else {
				maxEdge = Math.max(maxEdge,c.edge);
				maxIntensity = Math.max(c.intensity,maxIntensity);
			}
		}


		// Filter corners based on edge intensity of found corners
		for (int i = corners.size-1; i >= 0;  i--) {
			if( corners.get(i).edge >= edgeRatioThreshold*maxEdge ) {
				filtered.add(corners.get(i));
			}
		}

		System.out.println("  corners "+corners.size+"  filters "+filtered.size());

//		System.out.println("Dropped "+dropped+" / "+packed.size());
	}

	private void contourMaximums() {
		GThresholdImageOps.threshold(intensity,binary,intensityThresh,false);
		contourFinder.process(binary);

		List<ContourPacked> packed = contourFinder.getContours();
//		System.out.println("  * features.size = "+packed.size());
		maximums.reset();
		Point2D_F32 meanPt = new Point2D_F32();

		int totalFailed = 0;
		for (int i = 0; i < packed.size(); i++) {
			contourFinder.loadContour(i, contour);

			if( validateRepeatedPoints(meanPt) ) {
				totalFailed++;
				continue;
			}

			// NOTE: Commented out because it filtered corners in fisheye images at the image border
//			// the size is known. Filter if it's a long and skinny contour
//			boolean failed = false;
//			float r_max = 0;
//			for (int j = 0; j < contour.size; j++) {
//				Point2D_I32 c = contour.get(j);
//				float dx = c.x-meanPt.x;
//				float dy = c.y-meanPt.y;
//				float rr = dx*dx + dy*dy;
//				if( rr > r_max ) {
//					r_max = rr;
//				}
//			}
//
////			System.out.println("ratio "+(contour.size/Math.sqrt(r_max)));
//			if( contour.size/Math.sqrt(r_max) < 3.5f ) {
//				totalFailed++;
//				continue;
//			}

			maximums.grow().set(meanPt);
		}
		System.out.println("failed "+totalFailed+" / "+packed.size());
	}

	/**
	 * In oddly shaped contours it has to double back, these are typically false positives
	 * compute the mean point here too to avoid double counting
	 * @return
	 */
	private boolean validateRepeatedPoints(Point2D_F32 meanPt ) {
		meanPt.set(0,0);
		int count = 0;
		for (int i = 0; i < contour.size; i++) {
			Point2D_I32 p = contour.get(i);
			if( binary.get(p.x,p.y) == 2 ) {
				count++;
			} else {
				meanPt.x += p.x;
				meanPt.y += p.y;
				binary.set(p.x,p.y,2);
			}
		}
		for (int i = 0; i < contour.size; i++) {
			Point2D_I32 p = contour.get(i);
			binary.set(p.x,p.y,1);
		}

		meanPt.x /= (contour.size-count);
		meanPt.y /= (contour.size-count);

		return count >= 3 || (count >= 1 && contour.size <= 6);
	}

	private boolean checkPositiveInside(int cx , int cy , int threshold ) {
		int radius = 1;
		if( cx < radius || cx >= intensity.width-radius || cy < radius || cy >= intensity.height-radius )
			return false;

		final int x0 = cx - radius;
		final int y0 = cy - radius;
		final int x1 = cx + radius + 1;
		final int y1 = cy + radius + 1;

		int count=0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if( intensity.unsafe_get(x,y) >= intensityThresh )
					count++;
			}
		}
		return count >= threshold;
	}

	private float averageInside( int cx , int cy , int radius ) {
		final int x0 = cx - radius;
		final int y0 = cy - radius;
		final int x1 = cx + radius + 1;
		final int y1 = cy + radius + 1;

		float sum = 0;
		int count = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				float v = intensity.unsafe_get(x,y);
				if( v > 0 ) {
					count++;
					sum += v;
				}
			}
		}

		return sum/count;
	}

	private boolean checkNegativeInside(int cx , int cy , int threshold ) {
		int radius = 3;
		if( cx < radius || cx >= intensity.width-radius || cy < radius || cy >= intensity.height-radius )
			return false;

		final int x0 = cx - radius;
		final int y0 = cy - radius;
		final int x1 = cx + radius + 1;
		final int y1 = cy + radius + 1;

//		float a = averageInside(cx,cy,1)/5.0f;

		int count=0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if( intensity.unsafe_get(x,y) <= -intensityThresh*0.5 )
					count++;
			}
		}

//		System.out.println("negative count "+count);

		return count >= threshold;
	}

	/**
	 * Checks to see if there is a set of pixels with negative x-corner intensity values surrounding
	 */
	private boolean checkNegativeOutside(int cx , int cy , int radius , int threshold ) {
		if( cx < radius || cx >= intensity.width-radius || cy < radius || cy >= intensity.height-radius )
			return false;

		final int x0 = cx - radius;
		final int y0 = cy - radius;
		final int x1 = cx + radius;
		final int y1 = cy + radius;

		int w = radius*2;

		int sides = 0;

		int count0 = countNegative(x0,y0,1,0,w) + countNegative(x0,y0+1,1,0,w);
		if( count0 >= threshold) sides++;
		int count1 = countNegative(x1,y0,0,1,w) + countNegative(x1-1,y0,0,1,w);
		if( count1 >= threshold) sides++;
		int count2 = countNegative(x1,y1,-1,0,w) + countNegative(x1,y1-1,-1,0,w);
		if( count2 >= threshold) sides++;
		int count3 = countNegative(x0,y1,0,-1,w) + countNegative(x0+1,y1,0,-1,w);
		if( count3 >= threshold) sides++;

		return sides >= 4;
	}

	private boolean checkNegativeOutside3(float cx , float cy , FastQueue<Point2D_I32> outside , int min , int max ) {
		int radius = 4;
		if( cx < radius || cx >= intensity.width-radius || cy < radius || cy >= intensity.height-radius )
			return false;

		float mean = 0;
		for (int i = 0; i < outside.size; i++) {
			Point2D_I32 p = outside.get(i);
			mean += inputInterp.get(cx+p.x,cy+p.y);
		}
		mean /= outside.size;

		Point2D_I32 p = outside.get(0);
		int numUpDown = 0;
		int prevDir = inputInterp.get(cx+p.x,cy+p.y) > mean ? 1 : -1;
		for (int i = 1; i < outside.size; i++) {
			p = outside.get(i);
			int dir = inputInterp.get(cx+p.x,cy+p.y) > mean ? 1 : -1;
			if( prevDir != dir ) {
				numUpDown++;
				prevDir = dir;
			}
		}

//		System.out.println("U0Down "+numUpDown+"  circle.size "+outside.size);
//		return numUpDown <= 5 && numUpDown >= 4;
		return numUpDown >= min && numUpDown <= max;
	}

	private int countNegative( int x0 , int y0 , int stepX , int stepY , int length ) {
		int count = 0;

		for (int i = 0; i < length; i++) {
			if( intensity.unsafe_get(x0,y0) <= -intensityThresh) {
				count++;
			}
			x0 += stepX;
			y0 += stepY;
		}
		return count;
	}

	private boolean checkNegativeOutside2(int cx , int cy , int threshold ) {

		int radius = 4;
		if( cx < radius || cx >= intensity.width-radius || cy < radius || cy >= intensity.height-radius )
			return false;

		int count = 0;
		for (int i = 0; i < outsideCircle4.size; i++) {
			Point2D_I32 p = outsideCircle4.get(i);

			if( intensity.get(cx+p.x,cy+p.y) <= -intensityThresh*0.5f )
				count++;
			else if( intensity.get(cx+p.x,cy+p.y) >= intensityThresh )
				count--;
		}

//		System.out.println("Count "+count);
		return count >= threshold;
	}


	private boolean checkCircular(ChessboardCorner c ) {
		int radius = 3;

		int cx = (int)(c.x+0.5f);
		int cy = (int)(c.y+0.5f);

		float xx=0,yy=0,xy=0;

		float totalWeight=0;

		int width = radius*2+1;

		int idx = 0;
		for (int iy = 0; iy < width; iy++) {
			for (int ix = 0; ix < width; ix++, idx++) {

				float y = cy + iy - radius;
				float x = cx + ix - radius;

				float dx = inputInterp.get(x+0.5f,y)- inputInterp.get(x-0.5f,y);
				float dy = inputInterp.get(x,y+0.5f)- inputInterp.get(x,y-0.5f);

				float weight = Math.abs(intensityInterp.get(x,y));

				if( weight > intensityThresh ) {
					xx += dx * dx * weight;
					xy += dx * dy * weight;
					yy += dy * dy * weight;
					totalWeight += weight;
				}
			}
		}

		xx /= totalWeight;
		xy /= totalWeight;
		yy /= totalWeight;

		float left = (xx + yy) * 0.5f;
		float b = (xx - yy) * 0.5f;
		float right = (float)Math.sqrt(b * b + xy * xy);

		// tempting to use edge intensity as a way to filter out false positives
		// but that makes the corner no longer invariant to affine changes in light, e.g. changes in scale and offset
		c.edge = left-right; // smallest eigen value
		// the smallest eigenvalue divided by largest
		c.circleRatio = (left - right)/(left+right);

		return c.circleRatio >= 0.2;
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
		double r = 3;

		// magnitude of the difference is used remove false chessboard corners caused by the corners on black
		// squares. In that situation there will be a large difference between the left and right values
		// in the integral below for 1/2 the lines
		double sumDifference = 0;
		for (int i = 0; i < numLines; i++) {
			// TODO precompute sines and cosines
			double angle = Math.PI*i/ numLines -Math.PI/2.0;
			double c = Math.cos(angle);
			double s = Math.sin(angle);

			double x0 = cx-r*c;
			double y0 = cy-r*s;
			double x1 = cx+r*c;
			double y1 = cy+r*s;

			double left = integral.compute(cx,cy,x0,y0);
			double right = integral.compute(cx,cy,x1,y1);

			sumDifference += Math.abs(left-right);

			lines[i] = left+right;
		}

		// smooth by applying a block filter. This will ensure it doesn't point towards an edge which just happens
		// to be slightly darker than the center
		int r_smooth = kernelSmooth.getRadius();
		int w_smooth = kernelSmooth.getWidth();
		for (int i = 0; i < numLines; i++) {
			int start = addOffset(i,-r_smooth, numLines);

			double sum = 0;
			for (int j = 0; j < w_smooth; j++) {
				int index = addOffset(start,j, numLines);
				sum += lines[index]*kernelSmooth.data[j];
			}
			smoothed[i] = sum;
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

		// Score the corner's fit quality using the fact that the function would be oscilate (sin/cosine)
		// and values 90 degrees offset are at the other side
		double intensity = 0;
		for (int i = 0; i < numLines /2; i++) {
			int idx0 = addOffset(indexMin,i- numLines /4, numLines);
			int idx1 = addOffset(idx0, numLines /2, numLines);
			intensity += smoothed[idx1] - smoothed[idx0];
		}
		corner.intensity = intensity/(r*2+1+ numLines/2);
		corner.intensity /= (sumDifference/numLines);
	}

	public GrayF32 getIntensity() {
		return intensity;
	}

	public List<ChessboardCorner> getCorners() {
		return filtered;
	}

	public double getCornerIntensityThreshold() {
		return cornerIntensityThreshold;
	}

	public void setCornerIntensityThreshold(double cornerIntensityThreshold) {
		this.cornerIntensityThreshold = cornerIntensityThreshold;
	}

	public Class<T> getImageType() {
		return imageType;
	}

	public Class<D> getDerivType() {
		return derivType;
	}

	public GrayU8 getBinary() {
		return binary;
	}
}
