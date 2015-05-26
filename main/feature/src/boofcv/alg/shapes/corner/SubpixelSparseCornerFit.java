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

import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
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
import georegression.struct.point.Point2D_F64;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.wrap.QuasiNewtonBFGS_to_UnconstrainedMinimization;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
 * Inner iteration:
 * <ol>
 * <li>Compute gradient for pixels within a local region around the initial estimate.  Ignore pixels right next
 * to the initial corner estimate since pixels at the corner will not have a reliable gradient.</li>
 * <li>Normalize pixel coordinates and gradient to reduce numerical issues</li>
 * <li>Run an unconstrained optimization algorithm on the corner estimate</li>
 * </ol>
 * <p>
 * The outer iteration takes the results from the inner iteration and uses that as a new initial seed for the
 * next call to the inner iteration.  The outer iteration is repeated until the pixel location no longer changes
 * or the maximum number of outer iterations has been reached.
 * </p>
 *
 * <p>
 * Pixel Weights:<br>
 * When finding the corner on a polygon it is recommended that weights be turned on.  When searching for a corner
 * in a chessboard pattern weights should be off.  It can be configured to weigh darker or lighter pixels more.
 * If pixels are not weighted the symmetric gradient operator will bias the estimator and pull it outside the shape,
 * even when a perfect corner is provided.
 * </p>
 *
 * The weight is computed as follows:<br>
 * <ol>
 * <li>Find mean intensity in local region</li>
 * <li>Find mean for all pixels less than mean and the mean for all above mean</li>
 * <li>If weighted for dark pixels set target to low mean otherwise the high mean</li>
 * <li>Spread is set to high mean minus low mean</li>
 * <li>weight = max(0,|target - value|/spread)</li>
 * </ol>
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
	private int maxInitialSeeds = 5;

	//.0 = no weight, 1 = prefer bright, -1 = prefer dark
	protected int weightToggle = 0;

	// computes the gradient for a pixel
	private SparseImageGradient<T,GradientValue> gradient;

	// storage for local search region
	protected ImageRectangle region = new ImageRectangle();

	// storage for pixels and their gradient
	protected FastQueue<PointGradient_F64> points = new FastQueue<PointGradient_F64>(PointGradient_F64.class,true);
	// magnitude of the gradient storage
	private GrowQueue_F64 mag = new GrowQueue_F64();
	// storage for significant gradients
	private List<PointGradient_F64> significant = new ArrayList<PointGradient_F64>();

	// unconstrained optimization
	private QuasiNewtonBFGS_to_UnconstrainedMinimization minimization = FactoryOptimization.createBfgsWithMore94();
	private double initialParam[] = new double[2];

	// Used to select new random initial locations
	private Random rand = new Random(234);

	// reference to input image
	protected T image;

	// parameters for weighting function
	// weight = max(0,||value-target||/spread)
	double target,spread;
	// storage for weight calculation
	double pixelValues[] = new double[0];

	// image wrapper so that type specific code doesn't need to be included in this class
	// did some benchmarking and only in trivial cases does using type specific images make a difference
	GImageSingleBand imageWrapper;

	// storage for intermediate solutions
	Point2D_F64 refined = new Point2D_F64();

	/**
	 * Constructor that specifies the type of image it can process
	 * @param imageType Type of gray scale image
	 */
	public SubpixelSparseCornerFit( Class<T> imageType ) {
		ImageBorder<T> border = FactoryImageBorder.general(imageType, BorderType.EXTENDED);
		gradient = FactoryDerivativeSparse.createSobel(imageType, border);
		imageWrapper = FactoryGImageSingleBand.create(imageType);

		CornerFitFunction_F64 function = new CornerFitFunction_F64();
		CornerFitGradient_F64 functionGradient = new CornerFitGradient_F64();
		function.setPoints(significant);
		functionGradient.setPoints(significant);

		minimization.setFunction(function, functionGradient,0);
		setLocalRadius(localRadius);
	}

	/**
	 * Turns on weighted fitting.  Can improve accuracy by removing bias from pixels not part of the corner object.
	 * Recommended you turn on for polygons and turn off for chessboard.
	 *
	 * @param toggle -1 for dark shape, 0 unweighted, 1 for white shapes
	 */
	public void setWeightToggle(int toggle) {
		if( toggle < -1 || toggle > 1 )
			throw new IllegalArgumentException("Toggle can be -1,0,1");
		this.weightToggle = toggle;
	}

	/**
	 * Sets the image which is processed.  Must be called before refine.
	 */
	public void initialize(T image) {
		this.image = image;
		gradient.setImage(image);
		imageWrapper.wrap(image);
	}

	/**
	 * Refine the location of a corner estimated to be at pixel (cx,cy).  Must call {@link #initialize(ImageSingleBand)}
	 * first.
	 *
	 * @param cx Initial estimate of the corner. x-axis
	 * @param cy Initial estimate of the corner. y-axis
	 * @param output The refined estimate for the corner's position
	 * @return true if successful and false if it failed
	 */
	public boolean refine( double cx , double cy , Point2D_F64 output ) {
		int prevPixelX = (int)(cx+0.5);
		int prevPixelY = (int)(cy+0.5);
		refined.set(output);

		boolean hasSolution = false;
		for (int i = 0; i < maxInitialSeeds; i++) {
			// optimize
			switch(performOptimization(prevPixelX,prevPixelY,refined)) {
				case -1: return false; // fatal error

				case 0: // diverged or never found a solution
					if (hasSolution) {
						return true;
					} else {
						// try optimizing from a new initial location
						refined.x = cx + rand.nextDouble()*localRadius - localRadius/2.0;
						refined.y = cy + rand.nextDouble()*localRadius - localRadius/2.0;
						prevPixelX = (int)(cx+0.5);
						prevPixelY = (int)(cy+0.5);
					}
					continue;

				case 1: hasSolution = true; break;
			}

			// set up the next search around this location
			int pixelX = (int)(refined.x+0.5);
			int pixelY = (int)(refined.y+0.5);

			// moved outside the image, that's bad
			if( !image.isInBounds(pixelX,pixelY))
				return false;

			// see if no change, if true then stop iterations early
			if( pixelX == prevPixelX && pixelY == prevPixelY ) {
//				System.out.println("Finished early on "+i);
				break;
			}
			prevPixelX = pixelX;
			prevPixelY = pixelY;
		}

		if( hasSolution ) {
			output.set(refined);
		}
		return hasSolution;
	}

	/**
	 * Sets up the optimization and runs it to find the corner.
	 *
	 * @return -1 = fatal unrecoverable, 0 = try again, 1 = success
	 */
	protected int performOptimization( int cx , int cy , Point2D_F64 output ) {
		points.reset();
		// Find the rectangular region inside the image around (cx,cy)
		defineSearchRegion(cx,cy);

		// Compute the weights
		if( weightToggle != 0 )
			initializeWeights();

		// computes the local gradient around the initial estimate
		computeLocalGradient(cx, cy);

		// adjust scale of gradient for numerical reasons and find points with significant gradients
		significant.clear();
		if( !massageGradient(significant) )
			return -1;

		// optimize the solution.  It already has a reference to the significant points list
		if( iterateOptimization((output.x-cx)/localRadius,(output.y-cy)/localRadius)) {
			double[] found = minimization.getParameters();
			// return the refined location while undoing the scale adjustment done earlier
			output.x = found[0]*localRadius + cx;
			output.y = found[1]*localRadius + cy;
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * Manually step through the minimization process.  Every time it updates the state it checks to see if it
	 * has diverged.
	 *
	 * @param x initial point in transformed pixels coordinates
	 * @param y initial point in transformed pixels coordinates
	 * @return true if a new solution has been found, false if it failed
	 */
	public boolean iterateOptimization( double x , double y ) {
		initialParam[0] = x;
		initialParam[1] = y;
		minimization.initialize(initialParam,1e-5,1e-5);
		for (int i = 0; i < maxOptimizeSteps; i++) {
			boolean updated = false;
			for (int j = 0; j < 10000 && !updated; j++) {
				boolean converged = minimization.iterate();
				if(converged || minimization.isUpdated()) {
					updated = true;
				}
			}

			// something went wrong if after 10000 iterations the state wasn't updated
			if( !updated ) {
				return false;
			}

			double curr[] = minimization.getParameters();

			// check for divergence
			double deltaX = curr[0]-x;
			double deltaY = curr[1]-y;
			double r2 = deltaX*deltaX + deltaY*deltaY;

			if( r2 >= 2 ) { // remember, normalized so that 1 = localRadius.
				return false;
			} else if( minimization.isConverged() ) {
				return true;
			}
		}

		return true;
	}

	/**
	 * Sets the region around the specified pixel while taking in account the image boundaries
	 */
	protected void defineSearchRegion( int cx , int cy ) {
		// Find the local region that it will search inside of while avoiding image border
		region.set(cx- localRadius,cy- localRadius, cx + localRadius +1,cy + localRadius +1);
		BoofMiscOps.boundRectangleInside(image, region);
	}

	/**
	 * Examines the local pixels and finds the parameters for the weight function
	 */
	protected void initializeWeights() {
		int N = region.area();

		int i = 0;
		for (int y = region.y0; y < region.y1; y++) {
			for (int x = region.x0; x < region.x1; x++) {
				pixelValues[i++] = imageWrapper.unsafe_getD(x, y);
			}
		}

		double mean = 0;
		for (int j = 0; j < N; j++) {
			mean += pixelValues[j];
		}
		mean /= N;

		double lower = 0;
		double upper = 0;
		int lowerN = 0;

		for (int j = 0; j < N; j++) {
			double v = pixelValues[j];
			if( v < mean ) {
				lower += v;
				lowerN++;
			} else {
				upper += v;
			}
		}

		lower /= lowerN;
		upper /= (N-lowerN);

		if( weightToggle == 1) {
			target = upper;
		} else {
			target = lower;
		}
		spread = upper-lower;
	}

	/**
	 * Computes a weight for a pixel at the specified image coordinate
	 * @return weight from 0 to 1, inclusive
	 */
	protected double computeWeight( int x , int y ) {
		if( weightToggle == 0 ) {
			return 1.0;
		} else {
			double value = imageWrapper.unsafe_getD(x,y);
			return Math.max(0, 1.0 - Math.abs(value - target) / spread);
		}
	}

	/**
	 * Computes the gradient locally while taking in account the image border and adjusts coordinates to be from
	 * -1 to 1.
	 */
	protected void computeLocalGradient(int cx, int cy) {

		double dRadius = (double) localRadius;

		// compute the gradient at each pixel while taking in account the ignore radius
		// scale the coordinate so that they range from -1 to 1 for numerical reasons.  not sure if necessary
		// but doesn't really hurt
		for (int y = region.y0; y < region.y1; y++) {
			double scaledY = (y-cy)/dRadius;
			for (int x = region.x0; x < region.x1; x++) {
				if( Math.abs(x-cx) <= ignoreRadius && Math.abs(y-cy) <= ignoreRadius)
					continue;

				// compute weight based on intensity
				double w = computeWeight(x,y);
				GradientValue v = gradient.compute(x,y);
				double scaledX = (x-cx)/dRadius;

				points.grow().set(scaledX,scaledY,v.getX()*w,v.getY()*w);
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
		int w = localRadius*2+1;
		if( pixelValues.length < w*w )
			pixelValues = new double[w*w];
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

	public int getMaxInitialSeeds() {
		return maxInitialSeeds;
	}

	public void setMaxInitialSeeds(int maxInitialSeeds) {
		this.maxInitialSeeds = maxInitialSeeds;
	}

	public void setRandomSeed( long seed ) {
		rand = new Random(seed);
	}
}
