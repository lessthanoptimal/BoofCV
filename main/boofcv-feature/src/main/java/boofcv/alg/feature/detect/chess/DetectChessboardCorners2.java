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

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.peak.SearchLocalPeak;
import boofcv.alg.feature.detect.intensity.XCornerAbeles2019Intensity;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.ImageLineIntegral;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.peak.ConfigMeanShiftSearch;
import boofcv.factory.feature.detect.peak.FactorySearchLocalPeak;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.QueueCorner;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I16;
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
	float intensityThresh = 2.0f*0.05f; // max possible value for intensity is 2.0

	XCornerAbeles2019Intensity computeIntensity = new XCornerAbeles2019Intensity();
	SearchLocalPeak<GrayF32> meanShift;

	NonMaxSuppression nonmax;
	QueueCorner maximums = new QueueCorner();
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


	private GrayU8 binary = new GrayU8(radius*2+1,radius*2+1);
	private GrayU8 binaryEdge = new GrayU8(radius*2+1,radius*2+1);;

	// predeclare memory for compute a feature's orientation
	private final int numLines = 16;
	private final double lines[] = new double[numLines];
	private final double smoothed[] = new double[numLines];
	private final Kernel1D_F64 kernelSmooth = FactoryKernelGaussian.gaussian(1,true,64,-1,numLines/4);

	// input image type
	Class<T> imageType;
	Class<D> derivType;

	/**
	 * Declares internal data structures
	 */
	public DetectChessboardCorners2(Class<T> imageType ) {
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		// just give it something. this will be changed later
		borderImg = FactoryImageBorder.single(imageType,BorderType.ZERO);
		borderImg.setImage(GeneralizedImageOps.createSingleBand(imageType,1,1));
		integral.setImage(FactoryGImageGray.wrap(borderImg));

		ConfigExtract configExtract = new ConfigExtract();
		configExtract.radius = radius; // TODO or 4?
		configExtract.threshold = intensityThresh;
		nonmax = FactoryFeatureExtractor.nonmax(configExtract);

		{
			ConfigMeanShiftSearch config = new ConfigMeanShiftSearch(20,1e-6);
			config.positiveOnly = true;
			config.odd = false;
			meanShift = FactorySearchLocalPeak.meanShiftGaussian(config,GrayF32.class);
			meanShift.setSearchRadius(2);
		}
	}

	/**
	 * Computes chessboard corners inside the image
	 * @param input Gray image. Not modified.
	 */
	public void process( T input ) {
//		System.out.println("ENTER CHESSBOARD CORNER "+input.width+" x "+input.height);
		borderImg.setImage(input);

		computeIntensity.process((GrayF32)input,intensity);
//		cornerIntensity.process(derivX,derivY,intensity);

		intensityInterp.setImage(intensity);
		inputInterp.setImage((GrayF32)input);
		meanShift.setImage(intensity);


		nonmax.process(intensity,null,null,null,maximums);

		double maxEdge = 0;
		double maxIntensity = 0;
		double maxFoo = 0;

		filtered.clear();
		corners.reset();
//		System.out.println("  * features.size = "+packed.size());
		for (int i = 0; i < maximums.size(); i++) {
			Point2D_I16 p = maximums.get(i);

			if( !checkPositiveInside(p.x,p.y,4)) {
				continue;
			}

//			if( !checkNegativeInside(p.x,p.y,20)) {
//				continue;
//			}

			// TODO much faster if negative region can be used to eliminate at this stage

			ChessboardCorner c = corners.grow();

			// compensate for the bias caused by how pixels are counted.
			// Example: a 4x4 region is expected. Center should be at (2,2) but will instead be (1.5,1.5)
			c.x = p.x;
			c.y = p.y;

//			computeFeatures(p.x,p.y,c);

//			System.out.println("radius = "+radius+" angle = "+c.angle);
//			System.out.println("x-corner intensity "+intensity.get(p.x,p.y));
			if( useMeanShift ) {
				meanShift.search((float)c.x,(float)c.y);
				c.x = meanShift.getPeakX();
				c.y = meanShift.getPeakY();
			}

			// account for bias in sampling
			c.x += 0.5f;
			c.y += 0.5f;


			int xx = (int)(c.x);
			int yy = (int)(c.y);

//			if( !checkNegativeOutside(xx,yy,4,4) ) {
//				corners.removeTail();
//				continue;
//			}

			computeFeatures(c.x,c.y,c);

			boolean accepted = checkPositiveInside(xx,yy,6) &&
//					checkNegativeOutside(p.x,p.y,3) &&
					c.intensity >= cornerIntensityThreshold &&
					checkCircular(c);

//			boolean accepted = true;

			if( !accepted ) {
				corners.removeTail();
			} else {
				maxFoo = Math.max(maxFoo,intensity.get(xx,yy));
				maxEdge = Math.max(maxEdge,c.edge);
				maxIntensity = Math.max(c.intensity,maxIntensity);
			}
		}

		// Filter corners based on edge intensity of found corners
		for (int i = corners.size-1; i >= 0;  i--) {
			if(// corners.get(i).edge >= 0.1*maxEdge &&
					corners.get(i).intensity >= 0.1*maxIntensity &&
					maxFoo >= 0.1*maxFoo
			) {
				filtered.add(corners.get(i));
			}
		}

//		System.out.println("Dropped "+dropped+" / "+packed.size());
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

	private boolean checkNegativeInside(int cx , int cy , int threshold ) {
		int radius = 3;
		if( cx < radius || cx >= intensity.width-radius || cy < radius || cy >= intensity.height-radius )
			return false;

		final int x0 = cx - radius;
		final int y0 = cy - radius;
		final int x1 = cx + radius + 1;
		final int y1 = cy + radius + 1;

		int count=0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if( intensity.unsafe_get(x,y) <= -intensityThresh )
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

		int count0 = countNegative(x0,y0,1,0,w);
		if( count0 >= threshold) sides++;
		int count1 = countNegative(x1,y0,0,1,w);
		if( count1 >= threshold) sides++;
		int count2 = countNegative(x1,y1,-1,0,w);
		if( count2 >= threshold) sides++;
		int count3 = countNegative(x0,y1,0,-1,w);
		if( count3 >= threshold) sides++;

		return sides >= 4;
//		if( sides < 3 )
//			return false;
//
//		return count0+count1+count2+count3 >= threshold;

//		if( countNegative(x0,y0,1,0,w) < threshold )
//			return false;
//		if( countNegative(x1,y0,0,1,w) < threshold )
//			return false;
//		if( countNegative(x1,y1,-1,0,w) < threshold )
//			return false;
//		if( countNegative(x0,y1,0,-1,w) < threshold )
//			return false;

//		return true;
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


	private boolean checkCircular(ChessboardCorner c ) {
		int radius = 3;

		int cx = (int)c.x;
		int cy = (int)c.y;

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

	/**
	 * Use mean shift to improve the accuracy of the corner's location. A kernel is selected which is slightly larger
	 * than the "flat" intensity of the corner should be when over a chess pattern.
	 */
	public void meanShiftLocation( ChessboardCorner c ) {
		float meanX = (float)c.x;
		float meanY = (float)c.y;

		// The peak in intensity will be in -r to r region, but smaller values will be -2*r to 2*r
		int radius = 3;
		for (int iteration = 0; iteration < 5; iteration++) {
			float adjX = 0;
			float adjY = 0;
			float total = 0;

			for (int y = -radius; y <= radius; y++) {
				float yy = y;
				for (int x = -radius; x <= radius; x++) {
					float xx = x;
					float v = Math.max(0f,intensityInterp.get(meanX+xx,meanY+yy));
					adjX += xx*v;
					adjY += yy*v;
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
}
