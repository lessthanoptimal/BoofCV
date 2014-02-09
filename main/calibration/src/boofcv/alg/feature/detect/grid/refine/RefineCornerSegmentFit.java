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

package boofcv.alg.feature.detect.grid.refine;

import boofcv.alg.feature.detect.InvalidCalibrationTarget;
import boofcv.alg.feature.detect.grid.FitGaussianPrune;
import boofcv.alg.feature.detect.grid.HistogramTwoPeaks;
import boofcv.alg.feature.detect.grid.IntensityHistogram;
import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint2D_F64;
import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedMinimization;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoS;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Given an image it assumes a corner of a square is contained inside.  The square
 * is assumed to be much darker than the white background. Local statistics are used to
 * declare which pixels are part of the black square and interpolation is taken in
 * account when performing non-linear optimization.  Typically a subimage is passed in
 * that only contains a local region around a single corner.
 * </p>
 *
 * <p>
 * STEPS:<br>
 * 1) Local statistics of the square and background are found by first computing the mean then splitting pixels into two
 * groups which are above or below the mean.<br>
 * 2) The mean and standard deviation of each group is computed while removing outliers.<br>
 * 3) Using these statistics two binary images are computed using a threshold 1/2 between
 *    the light and dark, and another one much closer to the light.<br>
 * 4) Using logical operators the edge region is found.<br>
 * 5) Points are extract from the edge and weighted based on how dark they are. <br>
 * 6) The corner is modeled as the intersection of two lines.  An initial estimate is extracted
 *    and optimized.
 * </p>
 *
 * <p>
 * Noise is removed when thresholding by only keeping the largest island of pixels.  The cost
 * function being optimized minimizes the distance of a closest point on either line to a point.
 * </p>
 *
 * @author Peter Abeles
 */

// BUG:  If a corner is close to an edge of another object  then the assumption about it being entirely white and black
//       will be broken.  This can happen when a foreground object is obstructing the view.
public class RefineCornerSegmentFit {

	// computes statistics of white and black sections
	private FitGaussianPrune low = new FitGaussianPrune(20,3,5);
	private FitGaussianPrune high = new FitGaussianPrune(20,3,5);
	
	// binary images signifying the background and square
	private ImageUInt8 regionWhite = new ImageUInt8(1,1);
	private ImageUInt8 regionBlack = new ImageUInt8(1,1);

	private ImageUInt8 binary = new ImageUInt8(1,1);

	private ImageUInt8 binaryHigh = new ImageUInt8(1,1);
	private ImageUInt8 binaryMiddle = new ImageUInt8(1,1);

	// stores points in the boundary region
	private FastQueue<PointInfo> points = new FastQueue<PointInfo>(100,PointInfo.class,true);
	
	// saved clustered binary image
	private ImageSInt32 blobs = new ImageSInt32(1,1);

	// list of pixels along the image's edge
	private Point2D_I32 pointsAlongEdge[] = new Point2D_I32[]{new Point2D_I32()};

	// information about the image being processed in a more convenient format
	private int width;
	private int height;
	private int numEdges;
	
	// initial line parameter estimate
	private InitialEstimate initial = new InitialEstimate();

	// structures used to refine pixel estimate to sub-pixel accuracy
	private CostFunction func = new CostFunction();
	private UnconstrainedMinimization alg = FactoryOptimization.unconstrained();

	// output corner
	private Point2D_F64 corner;

	// used for computing local thresholds
	IntensityHistogram histHighRes = new IntensityHistogram(256,256);
	IntensityHistogram histLowRes = new IntensityHistogram(20,256);

	/**
	 * Detects and refines to sub-pixel accuracy a corner in the provided image
	 *
	 * @param image  Image containing a single corner from a black square
	 */
	public void process( ImageFloat32 image ) {
		init(image);

		computeStatistics(image);

		detectEdgePoints(image);

		selectEdgeParam(image);
		selectCornerParam();

		corner = optimizeFit();
	}

	/**
	 * Returns the estimated corner pixel to sub-pixel accuracy
	 *
	 * @return Corner point.
	 */
	public Point2D_F64 getCorner() {
		return corner;
	}

	/**
	 * Initialized data structures
	 */
	private void init(ImageFloat32 image) {
		this.width = image.width;
		this.height = image.height;
		numEdges = 2*(width+height-2);

		regionWhite.reshape(width, height);
		regionBlack.reshape(width, height);
		binaryHigh.reshape(width, height);
		binaryMiddle.reshape(width, height);
		binary.reshape(width,height);
		blobs.reshape(width,height);

		setupEdgeArray();
	}

	/**
	 * Use a histogram to find the two peaks caused by dark and light pixels.  Then segment
	 * the pixel intensities into two groups using the mid point between the two peaks.  statistics
	 * are then computed from both those group after pruning.
	 */
	HistogramTwoPeaks peaks = new HistogramTwoPeaks(6);
	private void computeStatistics(ImageFloat32 image) {

		// Find the high and low peaks using a histogram
		histHighRes.reset();
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				histHighRes.add(image.get(x,y));
			}
		}

		histLowRes.reset();
		histLowRes.downSample(histHighRes);

		peaks.computePeaks(histLowRes);

		int indexThresh = (int)((peaks.peakLow+peaks.peakHigh)/2.0);
		
		low.process(histHighRes,0,indexThresh);
		high.process(histHighRes,indexThresh,255);
		
//		System.out.printf("peaks: %6.2f %6.2f -  mean %6.2f  %6.2f   %5.2f  %5.2f\n"
//				,peaks.peakLow,peaks.peakHigh,low.getMean(),high.getMean(),low.getSigma(),high.getSigma());
	}

	/**
	 * Creates a list of points along the image's edge.  Simplifies code later on.
	 */
	private void setupEdgeArray() {
		if( pointsAlongEdge.length < numEdges ) {
			pointsAlongEdge = new Point2D_I32[numEdges];
			for( int i = 0; i < numEdges; i++ ) {
				pointsAlongEdge[i] = new Point2D_I32();
			}
		}

		int index = 0;
		for( int i = 0; i < width; i++ ) {
			pointsAlongEdge[index++].set(i, 0);
		}
		for( int i = 1; i < height-1; i++ ) {
			pointsAlongEdge[index++].set(width - 1, i);
		}
		for( int i = width - 1; i >= 0; i-- ) {
			pointsAlongEdge[index++].set(i, height - 1);
		}
		for(int i = height-2; i >= 1; i-- ) {
			pointsAlongEdge[index++].set(0, i);
		}
	}

	/**
	 * Detects edges along the black square.  If the square is partially inside a pixel the
	 * pixel's value will be between the dark and light value.   In a perfect sensor the border
	 * region would be at most two pixels thick, both values lighter than the pure black square.
	 * This border is captured using two threshold, one 1/2 between light and dark, and one much
	 * close to light.
	 *
	 * One case is not handled correctly given perfect observations.  If the line splits 1/2 way down
	 * two pixels then the edge will only be one pixel thick and not two.
	 */
	private void detectEdgePoints( ImageFloat32 image ) {
		// Added or subtracted one to handle pathological case where sigma is zero
		double lowThresh = low.getMean()+low.getSigma()*3+1;
		double highThresh = high.getMean()-high.getSigma()*3-1;

		// sanity check
		if( highThresh <= lowThresh ) {
//			BoofMiscOps.print(image);
//			for( int i = 0; i < histHighRes.histogram.length; i++ ) {
//				int c = histHighRes.histogram[i];
//				if( c != 0 ) {
//					System.out.println("["+i+"] = "+c);
//				}
//			}
//			return;
			throw new InvalidCalibrationTarget("Bad statistics");
		}

		// do a threshold in the middle first
		double middleThresh = (lowThresh*0.5+highThresh*0.5);

		// extract two regions at different threshold levels
		GThresholdImageOps.threshold(image, binaryMiddle,middleThresh,true);
		removeBinaryNoise(binaryMiddle);
		// binaryMiddle will be a subset of binaryHigh
		GThresholdImageOps.threshold(image, binaryHigh,highThresh,true);
		removeBinaryNoise(binaryHigh);

//		UtilImageIO.print(image);
//		UtilImageIO.print(binaryHigh);

		
		// find the region outside of 'binaryMiddle' in 'binaryHigh' and include
		// the edges in 'binaryMiddle'
		BinaryImageOps.logicXor(binaryMiddle, binaryHigh, binaryHigh);
		BinaryImageOps.edge4(binaryMiddle, binary);
		BinaryImageOps.logicOr(binaryHigh,binary,binary);

//		binary.print();
//		System.out.println("--------------------");

		// extract the points from the binary image and compute weights
		// weight is a linear function of distance from black square value
		points.reset();
		double middle = (high.getMean()+low.getMean())/2;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( binary.get(x,y) == 1 ) {
					PointInfo p = points.grow();
					p.set(x,y);
					double v = image.get(x,y);
					// weight pixels more that are close to the middle value since only an
					// edge point can have that value
					p.weight = 1.0-Math.abs(middle-v)/middle;
					p.weight = p.weight*p.weight;
				}
			}
		}
	}

	/**
	 * Remove all but the largest blob.  Any sporadic pixels will be zapped this way
	 * 
	 * @param binary Initial binary image.  Modified
	 */
	private void removeBinaryNoise(ImageUInt8 binary) {
		// remove potential noise by only saving the largest cluster
		List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.FOUR, blobs);

		// find the largest blob
		Contour largest = null;
		for( Contour c : contours ) {
			if( largest == null || largest.external.size() < c.external.size() ) {
				largest = c;
			}
		}

		if( largest == null )
			return;

		// recreate the binary image using only the largest blob
		int labels[] = new int[ contours.size() + 1 ];
		labels[ largest.id ] = 1;

		BinaryImageOps.relabel(blobs,labels);
		BinaryImageOps.labelToBinary(blobs,binary);
	}

	/**
	 * Given the initial estimate of the corner parameters, perform non-linear estimation to
	 * find the best fit corner
	 *
	 * @return best corner
	 */
	private Point2D_F64 optimizeFit() {
		
		double param[] = new double[4];
		param[0] = initial.corner.x;
		param[1] = initial.corner.y;
		param[2] = Math.atan2(initial.sideA.y-initial.corner.y,initial.sideA.x-initial.corner.x);
		param[3] = Math.atan2(initial.sideB.y-initial.corner.y,initial.sideB.x-initial.corner.x);

		alg.setFunction(func,null,0);
		alg.initialize(param,0,1e-8);

		if( !UtilOptimize.process(alg,500) ) {
			throw new InvalidCalibrationTarget("Minimization failed?!? "+alg.getWarning());
		}

		double found[] = alg.getParameters();

		return new Point2D_F64(found[0],found[1]);
	}

	/**
	 * Selects initial corner parameter as two points on image edge
	 */
	private void selectEdgeParam( ImageFloat32 image ) {
		// find points on edge
		List<Integer> onEdge = new ArrayList<Integer>();

		for( int i = 0; i < numEdges; i++ ) {
			Point2D_I32 p = pointsAlongEdge[i];
			if( binary.get(p.x,p.y) == 1 ) {
				onEdge.add(i);
			}
		}

		// At least two points should hit the image edge
		if( onEdge.size() < 2 )
			throw new InvalidCalibrationTarget("Less than two edge points");

		// find the two points which are farthest part
		int bestDistance = -1;
		int bestA = -1;
		int bestB = -1;
		for( int i = 0; i < onEdge.size(); i++ ) {
			int first = onEdge.get(i);
			for( int j = i+1; j < onEdge.size(); j++ ) {
				int second = onEdge.get(j);
				int distance = UtilCalibrationGrid.distanceCircle(first, second, numEdges);
				if( distance > bestDistance ) {
					bestDistance = distance;
					bestA = first;
					bestB = second;
				}
			}
		}

		initial.sideA = pointsAlongEdge[bestA];
		initial.sideB = pointsAlongEdge[bestB];

	}

	/**
	 * Initial estimate of the corner location is selected to be the point farthest away from the
	 * two points select on the image's edge
	 */
	private void selectCornerParam() {
		Point2D_I32 a = initial.sideA;
		Point2D_I32 b = initial.sideB;

		PointInfo bestPoint = null;
		double bestDist = -1;

		for( int i = 0; i < points.size(); i++ ) {
			PointInfo p = points.get(i);

			double distA = UtilPoint2D_F64.distance(a.x, a.y, p.x, p.y);
			double distB = UtilPoint2D_F64.distance(b.x, b.y, p.x, p.y);

			double sum = distA+distB;

			if( sum > bestDist ) {
				bestDist = sum;
				bestPoint = p;
			}
		}

		initial.corner = bestPoint;
	}

	/**
	 * Structure containing initial corner estimate
	 */
	private static class InitialEstimate
	{
		Point2D_I32 sideA;
		Point2D_I32 sideB;
		Point2D_F64 corner;
	}

	/**
	 * Cost function which computes the cost as the sum of distances between the set of points
	 * and the corner.  Distance from a point to the corner is defined as the minimum distance of
	 * a point from the two lines.
	 */
	private class CostFunction implements FunctionNtoS
	{
		LineParametric2D_F64 lineA = new LineParametric2D_F64();
		LineParametric2D_F64 lineB = new LineParametric2D_F64();

		@Override
		public int getNumOfInputsN() {
			return 4;
		}

		@Override
		public double process(double[] input) {
			double x = input[0];
			double y = input[1];
			double thetaA = input[2];
			double thetaB = input[3];
			
			lineA.p.set(x,y);
			lineB.p.set(x, y);

			lineA.slope.set(Math.cos(thetaA),Math.sin(thetaA));
			lineB.slope.set(Math.cos(thetaB),Math.sin(thetaB));
			
			double cost = 0;

			for( int i = 0; i < points.size; i++ ) {

				PointInfo p = points.get(i);

				double distA = Distance2D_F64.distanceSq(lineA, p);
				double distB = Distance2D_F64.distanceSq(lineB, p);
				
				cost += p.weight*Math.min(distA,distB);
			}

			return cost;
		}
	}

	/**
	 * Location of a boundary point and its weight.
	 */
	public static class PointInfo extends Point2D_F64
	{
		public double weight;
	}

}
