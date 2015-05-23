/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.corner;

import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.filter.derivative.FactoryDerivativeSparse;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.PointGradient_F64;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseImageGradient;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedMinimization;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Refines the estimated location of a corner and places it at the "tip".  The algorithm takes advantage of the
 * gradient along an edge being perpendicular to the line radiating outwards from the corner to the point.  It
 * maximizes the magnitude of the cross product between the unit vector pointing from the corner to a pixel and
 * the pixel's gradient.  This technique works well for corners on any polygon as well as chessboard patterns.
 * </p>
 * <p>
 * This algorithm is considered sparse since it computes the gradient locally around each corner estimate.  A dense
 * algorithm would use the precomputed gradient of the entire image.
 * </p>
 * <ol>
 * <li>Compute gradient for pixels within a local region around the initial estimate.  Ignore pixels right next
 * to the initial corner estimate since pixels at the corner will not have a reliable gradient.</li>
 * <li>Normalize pixel coordinates and gradient to reduce numerical issues</li>
 * <li>Run an unconstrained optimization algorithm on the corner estimate</li>
 * </ol>
 *
 * <p>
 * Accuracy:<br>
 * From tests on synthetic images it seems to get you to within about 0.7 pixels of the corner.  If you
 * define the corner as the extremes of the black square.  This is due to how the gradient is computed.
 * Since you don't know which pixels belong to the inside or outside a symmetric gradient calculation
 * is used.  This will pull the estimate for the corner towards the outside.
 * </p>
 *
 * @author Peter Abeles
 */
public class SubpixelSparseCornerFit <T extends ImageSingleBand>{

	// ignore radius
	// all pictures are ignored inside this region around the center.
	private int ignoreRadius=1;

	// radius of the region it will search for the corner in
	private int localRadius = 4;

	// the minimum threshold a gradient needs to be to be considered
	// relative to the maximum gradient
	private double minRelThreshold = 0.02;

	// maximum number of optimization steps.  Can be used to control speed.
	private int maxOptimizeSteps = 200;

	// maximum number of iterations it will perform with a different central seed
	private int maxIterations = 5;

	// computes the gradient for a pixel
	private SparseImageGradient<T,GradientValue> gradient;

	// storage for local search region
	private ImageRectangle region = new ImageRectangle();

	// storage for pixels and their gradient
	protected FastQueue<PointGradient_F64> points = new FastQueue<PointGradient_F64>(PointGradient_F64.class,true);
	// magnitude of the gradient storage
	private GrowQueue_F64 mag = new GrowQueue_F64();
	// storage for significant gradients
	private List<PointGradient_F64> significant = new ArrayList<PointGradient_F64>();

	// unconstrained optimization
	private UnconstrainedMinimization minimization = FactoryOptimization.unconstrained();
	private double initialParam[] = new double[2];

	// reference to input image
	private T image;

	// the refined corner
	private double refinedX;
	private double refinedY;

	/**
	 * Constructor that specifies the type of image it can process
	 * @param imageType Type of gray scale image
	 */
	public SubpixelSparseCornerFit( Class<T> imageType ) {
		ImageBorder<T> border = FactoryImageBorder.general(imageType, BorderType.EXTENDED);
		gradient = FactoryDerivativeSparse.createSobel(imageType,border);

		CornerFitFunction_F64 function = new CornerFitFunction_F64();
		CornerFitGradient_F64 functionGradient = new CornerFitGradient_F64();
		function.setPoints(significant);
		functionGradient.setPoints(significant);
		minimization.setFunction(function, functionGradient,0);
	}

	/**
	 * Sets the image which is processed
	 */
	public void setImage( T image ) {
		this.image = image;
		gradient.setImage(image);
	}

	/**
	 * Refine the location of a corner estimated to be at pixel (cx,cy).  Must call {@link #setImage(ImageSingleBand)}
	 * first.
	 *
	 * @param cx Initial estimate of the corner. x-axis
	 * @param cy Initial estimate of the corner. y-axis
	 * @return true if successful and false if it failed
	 */
	public boolean refine( int cx , int cy ) {
		int prevPixelX = cx;
		int prevPixelY = cy;

		for (int i = 0; i < maxIterations; i++) {
			if( !performOptimization(prevPixelX,prevPixelY) )
				return false;

			int pixelX = (int)(refinedX+0.5);
			int pixelY = (int)(refinedY+0.5);

			// moved outside the image, that's bad
			if( !image.isInBounds(pixelX,pixelY))
				return false;
			// if it moved a large distance the solution is most likely corrupt
			if( Math.abs(pixelX-cx) > localRadius || Math.abs(pixelY-cy) > localRadius )
				return false;

			// see if no change, if true then stop iterations early
			if( pixelX == prevPixelX && pixelY == prevPixelY ) {
//				System.out.println("Finished early on "+i);
				break;
			}
			prevPixelX = pixelX;
			prevPixelY = pixelY;
		}

		return true;
	}

	/**
	 * Sets up the optimization and runs it to find the corner
	 */
	protected boolean performOptimization( int cx , int cy ) {
		points.reset();

		// computes the local gradient around the initial estimate
		computeLocalGradient(cx, cy);

		// adjust scale of gradient for numerical reasons and find points with significant gradients
		significant.clear();
		if( !massageGradient(significant) )
			return false;

		// optimize the solution.  It already has a reference to the significant points list
		minimization.initialize(initialParam,1e-6,1e-6);
		UtilOptimize.process(minimization,maxOptimizeSteps);
		double[] found = minimization.getParameters();

		// return the refined location while undoing the scale adjustment done earlier
		refinedX = found[0]* localRadius + cx;
		refinedY = found[1]* localRadius + cy;

		return true;
	}

	/**
	 * Computes the gradient locally while taking in account the image border and adjusts coordinates to be from
	 * -1 to 1.
	 *
	 */
	protected void computeLocalGradient(int cx, int cy) {
		// Find the local region that it will search inside of while avoiding image border
		region.set(cx- localRadius,cy- localRadius, cx + localRadius +1,cy + localRadius +1);
		BoofMiscOps.boundRectangleInside(image, region);

		double dRadius = (double) localRadius;

		// compute the gradient at each pixel while taking in account the ignore radius
		// scale the coordinate so that they range from -1 to 1 for numerical reasons.  not sure if neccisary
		// but doesn't really hurt
		for (int y = region.y0; y < region.y1; y++) {
			double scaledY = (y-cy)/dRadius;
			for (int x = region.x0; x < region.x1; x++) {
				if( Math.abs(x-cx) <= ignoreRadius && Math.abs(y-cy) <= ignoreRadius)
					continue;

				GradientValue v = gradient.compute(x,y);
				double scaledX = (x-cx)/dRadius;

				points.grow().set(scaledX,scaledY,v.getX(),v.getY());
			}
		}
	}

	/**
	 * Scales gradient so that maximum value has a magnitude of 1.  Puts all points with significant gradients into
	 * a list
	 */
	protected boolean massageGradient( List<PointGradient_F64> significant )
	{
		double max = 0;
		mag.reset();

		for (int i = 0; i < points.size; i++) {
			PointGradient_F64 p = points.get(i);
			double m = Math.sqrt( p.dx*p.dx + p.dy*p.dy);
			mag.add(m);
			if( m > max )
				max = m;
		}

		if( max == 0 )
			return false;

		double threshold = max*minRelThreshold;
		for (int i = 0; i < points.size; i++) {
			if( mag.get(i) >= threshold ) {
				PointGradient_F64 p = points.get(i);
				p.dx /= max;
				p.dy /= max;
				significant.add(p);
			}
		}
		return true;
	}

	/**
	 * x-coordinate of the refined corner estimate
	 */
	public double getRefinedX() {
		return refinedX;
	}

	/**
	 * y-coordinate of the refined corner estimate
	 */
	public double getRefinedY() {
		return refinedY;
	}

	public int getIgnoreRadius() {
		return ignoreRadius;
	}

	public void setIgnoreRadius(int ignoreRadius) {
		this.ignoreRadius = ignoreRadius;
	}

	public int getLocalRadius() {
		return localRadius;
	}

	public void setLocalRadius(int localRadius) {
		this.localRadius = localRadius;
	}

	public double getMinRelThreshold() {
		return minRelThreshold;
	}

	public void setMinRelThreshold(double minRelThreshold) {
		this.minRelThreshold = minRelThreshold;
	}

	public int getMaxOptimizeSteps() {
		return maxOptimizeSteps;
	}

	public void setMaxOptimizeSteps(int maxOptimizeSteps) {
		this.maxOptimizeSteps = maxOptimizeSteps;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}
}
