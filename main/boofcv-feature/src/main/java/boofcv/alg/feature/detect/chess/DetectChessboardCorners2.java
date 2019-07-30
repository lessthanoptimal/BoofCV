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
import boofcv.abst.filter.binary.BinaryContourFinderLinearExternal;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.detect.intensity.XCornerAbeles2019Intensity;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.ImageLineIntegral;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.peak.ConfigMeanShiftSearch;
import boofcv.factory.feature.detect.peak.FactorySearchLocalPeak;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.ConnectRule;
import boofcv.struct.QueueCorner;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I16;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;

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

	public float nonmaxThresholdRatio = 0.1f;
	public double edgeIntensityRatioThreshold = 0.01;
	public double edgeAspectRatioThreshold = 0.1;
	public double cornerIntensity = 20;
	int blurRadius = 1;

	// TODO Sample center radius 1 or 2
	//      if its nearly identical intensity to one of the axis drop feature

	// dynamically computed thresholds
	float nonmaxThreshold;

	T blurred;
	BlurFilter<T> blurFilter;

	XCornerAbeles2019Intensity computeIntensity = new XCornerAbeles2019Intensity(3);
	SearchLocalPeak<GrayF32> meanShift;

//	FastQueue<Point2D_F32> maximums = new FastQueue<>(Point2D_F32.class,true);
	private FastQueue<ChessboardCorner> corners = new FastQueue<>(ChessboardCorner.class,true);
	List<ChessboardCorner> filtered = new ArrayList<>();

	// storage for corner detector output
	GrayF32 intensity = new GrayF32(1,1);

	// Used to compute line integrals of spokes around a corner
	ImageBorder<T> borderImg;
	ImageLineIntegral integral = new ImageLineIntegral();

	// for mean-shift
	InterpolatePixelS<GrayF32> inputInterp = FactoryInterpolation.bilinearPixelS(GrayF32.class,BorderType.ZERO);
	InterpolatePixelS<GrayF32> blurInterp = FactoryInterpolation.bilinearPixelS(GrayF32.class,BorderType.ZERO);
	InterpolatePixelS<GrayF32> intensityInterp = FactoryInterpolation.bilinearPixelS(GrayF32.class,BorderType.ZERO);
	public boolean useMeanShift = true;

	NonMaxSuppression nonmax;
	QueueCorner foundNonmax = new QueueCorner();

	private GrayU8 binary = new GrayU8(1,1);

	// predeclare memory for compute a feature's orientation
	private final int numSpokes = 32;
	private final int numSpokeDiam = numSpokes/2;
	private final double[] spokesRadi = new double[numSpokes];
	private final double[] spokesDiam = new double[numSpokeDiam];
	private final double[] smoothedDiam = new double[numSpokeDiam];
	private final double[] scoreDiam = new double[numSpokeDiam];
	private final Kernel1D_F64 kernelSmooth = FactoryKernelGaussian.gaussian(1,true,64,-1,numSpokeDiam/4);

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

		{
			ConfigExtract config = new ConfigExtract();
			config.radius = 3;
			config.threshold = 0;
			config.detectMaximums = true;
			config.detectMinimums = false;
			config.useStrictRule = true;
			nonmax = FactoryFeatureExtractor.nonmax(config);
		}

		blurFilter = FactoryBlurFilter.gaussian(imageType,-1,blurRadius);
//		blurFilter = FactoryBlurFilter.mean(imageType,blurRadius);
		blurred = GeneralizedImageOps.createSingleBand(imageType,1,1);

		// just give it something. this will be changed later
		borderImg = FactoryImageBorder.single(imageType,BorderType.EXTENDED);
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
		filtered.clear();
		corners.reset();
		foundNonmax.reset();

//		System.out.println("ENTER CHESSBOARD CORNER "+input.width+" x "+input.height);
		borderImg.setImage(input);

		blurFilter.process(input,blurred);
		computeIntensity.process((GrayF32)blurred, intensity);

		intensityInterp.setImage(intensity);
		inputInterp.setImage((GrayF32)input);
		blurInterp.setImage((GrayF32)blurred);
		meanShift.setImage(intensity);

		// intensity is squared, so the ratio is squared too
		nonmaxThreshold = ImageStatistics.max(intensity)*nonmaxThresholdRatio*nonmaxThresholdRatio;
		// TODO better dynamic threshold. a single bright pxiel can throw it off

		nonmax.setThresholdMaximum(nonmaxThreshold);
		nonmax.process(intensity,null,null,null,foundNonmax);
		for (int i = 0; i < foundNonmax.size; i++) {
			Point2D_I16 c = foundNonmax.get(i);
			ChessboardCorner corner = corners.grow();
			corner.set(c.x,c.y);
			corner.intensityXCorner = intensity.unsafe_get(c.x,c.y);
			corner.reset();
		}

//		contourMaximums();

		double maxEdge = 0;
		double maxIntensity = 0;

//		System.out.println("  * features.size = "+packed.size());
		for (int i = 0; i < corners.size(); i++) {
			ChessboardCorner c = corners.get(i);

			int xx = (int)(c.x+0.5f);
			int yy = (int)(c.y+0.5f);

			if( !checkPositiveInside(xx,yy,4) ) {
				continue;
			}

			if( !checkNegativeInside(xx,yy,12)) {
				continue;
			}

			if( !checkChessboardCircle(xx,yy,outsideCircle4,4,4)) {
				continue;
			}

			if( !checkChessboardCircle(xx,yy,outsideCircle3,3,4)) {
				continue;
			}

			if( useMeanShift ) {
				meanShift.search((float)c.x,(float)c.y);
				c.x = meanShift.getPeakX();
				c.y = meanShift.getPeakY();
			}

			if( !checkCorner(c)) {
				c.edgeIntensity = -1;
				continue;
			}

			if( !computeFeatures(c) ) {
				c.edgeIntensity = -1;
				continue;
			}

			// account for bias due to discretion
			c.x += 0.5f;
			c.y += 0.5f;

			maxEdge = Math.max(maxEdge,c.edgeIntensity);
			maxIntensity = Math.max(c.intensity,maxIntensity);
		}

		// Filter corners based on edge intensity of found corners
		for (int i = corners.size-1; i >= 0;  i--) {
			if( corners.get(i).edgeIntensity >= edgeIntensityRatioThreshold*maxEdge ) {
//			if( corners.get(i).edgeIntensity >= 0 ) {
//				System.out.println("transitions "+corners.get(i).circleRatio );
				filtered.add(corners.get(i));
			}
		}

		int dropped = corners.size-filtered.size();
		System.out.printf("  corners %4d filters %5d dropped = %4.1f%%\n",corners.size,filtered.size(),(100*dropped/(double)corners.size));
	}

//	private void contourMaximums() {
//		GThresholdImageOps.threshold(intensity,binary,intensityThresh,false);
//		contourFinder.process(binary);
//
//		List<ContourPacked> packed = contourFinder.getContours();
////		System.out.println("  * features.size = "+packed.size());
//		maximums.reset();
//		Point2D_F32 meanPt = new Point2D_F32();
//
//		int totalFailed = 0;
//		for (int i = 0; i < packed.size(); i++) {
//			contourFinder.loadContour(i, contour);
//
//			if( validateRepeatedPoints(meanPt) ) {
//				totalFailed++;
//				continue;
//			}
//
//			// NOTE: Commented out because it filtered corners in fisheye images at the image border
////			// the size is known. Filter if it's a long and skinny contour
////			boolean failed = false;
////			float r_max = 0;
////			for (int j = 0; j < contour.size; j++) {
////				Point2D_I32 c = contour.get(j);
////				float dx = c.x-meanPt.x;
////				float dy = c.y-meanPt.y;
////				float rr = dx*dx + dy*dy;
////				if( rr > r_max ) {
////					r_max = rr;
////				}
////			}
////
//////			System.out.println("ratio "+(contour.size/Math.sqrt(r_max)));
////			if( contour.size/Math.sqrt(r_max) < 3.5f ) {
////				totalFailed++;
////				continue;
////			}
//
//			maximums.grow().set(meanPt);
//		}
//		System.out.println("failed "+totalFailed+" / "+packed.size());
//	}

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
				if( intensity.unsafe_get(x,y) >= nonmaxThreshold)
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
				if( intensity.unsafe_get(x,y) <= -nonmaxThreshold )
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

	private boolean checkChessboardCircle(float cx , float cy , FastQueue<Point2D_I32> outside , int min , int max ) {
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
			if( intensity.unsafe_get(x0,y0) <= -nonmaxThreshold) {
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

			if( intensity.get(cx+p.x,cy+p.y) <= -nonmaxThreshold *0.5f )
				count++;
			else if( intensity.get(cx+p.x,cy+p.y) >= nonmaxThreshold)
				count--;
		}

//		System.out.println("Count "+count);
		return count >= threshold;
	}

	private int countTransitions( double mean ) {
		int transitions = 0;

		boolean above = spokesRadi[numSpokes-1] > mean;
		for (int i = 0; i < numSpokes; i++) {
			boolean a = spokesRadi[i] > mean ;
			if( above != a ) {
				above = a;
				transitions++;
			}
		}
		return transitions;
	}

	private boolean checkCorner( ChessboardCorner c ) {
		int radius = 3;

		int cx = (int)(c.x+0.5f);
		int cy = (int)(c.y+0.5f);

		float xx=0,yy=0,xy=0;

		int width = radius*2+1;

		int idx = 0;
		for (int iy = 0; iy < width; iy++) {
			for (int ix = 0; ix < width; ix++, idx++) {

				int y = cy + iy - radius;
				int x = cx + ix - radius;

				// TODO change to border image
				float dx = blurInterp.get(x+1,y)- blurInterp.get(x-1,y);
				float dy = blurInterp.get(x,y+1)- blurInterp.get(x,y-1);

//				float weight = Math.abs(intensityInterp.get(x,y));

//				if( weight > intensityThresh ) {
					xx += dx * dx;
					xy += dx * dy;
					yy += dy * dy;
			}
		}

		float totalWeight=width*width;
		xx /= totalWeight;
		xy /= totalWeight;
		yy /= totalWeight;

		float left = (xx + yy) * 0.5f;
		float b = (xx - yy) * 0.5f;
		float right = (float)Math.sqrt(b * b + xy * xy);

		// tempting to use edge intensity as a way to filter out false positives
		// but that makes the corner no longer invariant to affine changes in light, e.g. changes in scale and offset
		c.edgeIntensity = left-right; // smallest eigen value
		// the smallest eigenvalue divided by largest. A perfect corner would be 1. As it approaches zero it indicates
		// that there's more of a line.
		c.edgeRatio = (left - right)/(left+right);

		// NOTE: Setting the Eigen ratio to a higher value is an effective ratio, but for fisheye images it will
		//       filter out many of the corners at the border where they are highly distorted
		return c.edgeRatio >= edgeAspectRatioThreshold;
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
	private boolean computeFeatures( ChessboardCorner corner ) {
		double r = 4;

		// magnitude of the difference is used remove false chessboard corners caused by the corners on black
		// squares. In that situation there will be a large difference between the left and right values
		// in the integral below for 1/2 the line
		double cx = corner.x;
		double cy = corner.y;
		double sumDifference = 0;
		double mean = 0;
		for (int i = 0; i < numSpokeDiam; i++) {
			int j = (i+ numSpokeDiam)%numSpokes;
			double angle = Math.PI*i/ numSpokeDiam;
			double c = Math.cos(angle);
			double s = Math.sin(angle);

			double valA = spokesRadi[i] = integral.compute(cx,cy,cx+r*c,cy+r*s)/r;
			double valB = spokesRadi[j] = integral.compute(cx,cy,cx-r*c,cy-r*s)/r;

			spokesDiam[i] = valA+valB;

			sumDifference += Math.abs(valA-valB);
			mean += valA + valB;
		}
		mean /= numSpokes;
		sumDifference /= numSpokeDiam;

		// There should be 4 transitions between above and below the mean
		if( countTransitions(mean) != 4 )
			return false;

		//
		smoothSpokeDiam();
		// Select the orientation
		int bestSpoke = -1;
		double bestScore = Double.MAX_VALUE;
		for (int i = 0; i < numSpokeDiam; i++) {
			// j = 90 off, which should be the opposite color
			int j = (i+ numSpokeDiam /2)% numSpokeDiam;
			double score = scoreDiam[i] = smoothedDiam[i] - smoothedDiam[j];
			// select black 'i', which will negative because white has a higher value
			if( score < bestScore ) {
				bestScore = score;
				bestSpoke = i;
			}
		}

		// Use a quadratic to estimate the peak's location to a sub-bin accuracy
		double value0 = scoreDiam[ addOffset(bestSpoke,-1, numSpokeDiam)];
		double value2 = scoreDiam[ addOffset(bestSpoke, 1, numSpokeDiam)];

		double adjustedIndex = bestSpoke + FastHessianFeatureDetector.polyPeak(value0,bestSpoke,value2);
		corner.orientation = UtilAngle.boundHalf(Math.PI*adjustedIndex/ numSpokeDiam);

		// Compute a how X-Corner like metric
		double stdev = 0;
		for (int i = 0; i < numSpokes; i++) {
			double diff = mean - spokesRadi[i];
			stdev += diff*diff;
		}
		stdev = Math.sqrt(stdev/numSpokes);

		// NOTE: Requiring that the center not have a similar value to the spokes is a bad idea. When a square
		//       is diagonal there's a good chance it will have a value very similar to one of the spokes, even
		//       when the image is mostly in focus
		// The value in the center should be significantly different from all the others
//		double centerValue0 = inputInterp.get((float)corner.x,(float)corner.y);
//		double centerValue1 = blurInterp.get((float)corner.x,(float)corner.y);
//		double scoreCenter = Double.MAX_VALUE;
//		double maxDifference = 0;
//		for (int i = 0; i < 4; i++) {
//			int index = (bestSpoke + i* numSpokeDiam /2)%numSpokes;
////			scoreCenter = Math.min(scoreCenter,Math.abs(centerValue0-spokesRadi[index]));
//			scoreCenter = Math.min(scoreCenter,Math.abs(centerValue1- spokesRadi[index]));
//
////			maxDifference = Math.max(maxDifference,Math.abs(centerValue0-spokesRadi[index]));
//			maxDifference = Math.max(maxDifference,Math.abs(centerValue1- spokesRadi[index]));
//		}
//
//		scoreCenter = scoreCenter/maxDifference;

//		System.out.println("ori = "+corner.orientation);

		corner.intensity = -bestScore*stdev/(sumDifference + UtilEjml.EPS);

//		System.out.printf(" scoreCenter %1.2f score %1.2f diff %4.1f stdev %5.2f intensity %8.2f\n",scoreCenter,bestScore,sumDifference,stdev,corner.intensity);

//		if( corner.distance(985,465) < 1.5 ) {
//			System.out.printf(" scoreCenter %1.2f score %1.2f diff %4.1f stdev %5.2f intensity %8.2f\n",scoreCenter,bestScore,sumDifference,stdev,corner.intensity);
//			System.out.println("False positive!");
//		}
//
//		if( corner.distance(886,475) < 1.5 ) {
//			System.out.printf(" scoreCenter %1.2f score %1.2f diff %4.1f stdev %5.2f intensity %8.2f\n",scoreCenter,bestScore,sumDifference,stdev,corner.intensity);
//			System.out.println("False positive!");
//		}
//
//		if( corner.distance(968,839) < 1.5 ) {
//			System.out.printf(" scoreCenter %1.2f score %1.2f diff %4.1f stdev %5.2f intensity %8.2f\n",scoreCenter,bestScore,sumDifference,stdev,corner.intensity);
//			System.out.printf("True positive! %.0f %.0f\n",cx,cy);
//		}
//
//		if( corner.distance(170,178) < 1.5 ) {
//			System.out.printf(" scoreCenter %1.2f score %1.2f diff %4.1f stdev %5.2f intensity %8.2f\n",scoreCenter,bestScore,sumDifference,stdev,corner.intensity);
//			System.out.printf("True positive! %.0f %.0f\n",cx,cy);
//		}

//		return scoreCenter >= 0.0;
		return corner.intensity >= cornerIntensity;
//		return true;//corner.intensity >= cornerIntensityThreshold;
	}

	private void smoothSpokeDiam() {
		// smooth by applying a block filter. This will ensure it doesn't point towards an edge which just happens
		// to be slightly darker than the center
		int r_smooth = kernelSmooth.getRadius();
		int w_smooth = kernelSmooth.getWidth();
		for (int i = 0; i < numSpokeDiam; i++) {
			int start = addOffset(i,-r_smooth, numSpokeDiam);

			double sum = 0;
			for (int j = 0; j < w_smooth; j++) {
				int index = addOffset(start,j, numSpokeDiam);
				sum += spokesDiam[index]*kernelSmooth.data[j];
			}
			smoothedDiam[i] = sum;
		}
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

	public float getNonmaxThresholdRatio() {
		return nonmaxThresholdRatio;
	}

	public void setNonmaxThresholdRatio(float nonmaxThresholdRatio) {
		this.nonmaxThresholdRatio = nonmaxThresholdRatio;
	}

	public double getEdgeIntensityRatioThreshold() {
		return edgeIntensityRatioThreshold;
	}

	public void setEdgeIntensityRatioThreshold(double edgeIntensityRatioThreshold) {
		this.edgeIntensityRatioThreshold = edgeIntensityRatioThreshold;
	}

	public int getNonmaxRadius() {
		return nonmax.getSearchRadius();
	}

	public void setNonmaxRadius(int nonmaxRadius) {
		nonmax.setSearchRadius(nonmaxRadius);
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
