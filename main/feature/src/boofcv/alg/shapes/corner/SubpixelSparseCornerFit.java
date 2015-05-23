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
 * TODO write
 *
 * Assumptions:
 *
 * @author Peter Abeles
 */
public class SubpixelSparseCornerFit <T extends ImageSingleBand>{

	// ignore radius
	// all pictures are ignored inside this region around the center.
	int ignoreRadius=1;

	// radius of the region it will git the corner to
	int fitRadius=4;

	// the minimum threshold a gradient needs to be to be considered
	// relative to the maximum gradient
	double minRelThreshold = 0.02;

	// maximum number of optimization steps.  Can be used to control speed.
	int maxOptimizeSteps = 200;

	SparseImageGradient<T,GradientValue> gradient;

	// storage for local search region
	ImageRectangle region = new ImageRectangle();

	// storage for pixels and their gradient
	FastQueue<PointGradient_F64> points = new FastQueue<PointGradient_F64>(PointGradient_F64.class,true);
	// magnitude of the gradient storage
	GrowQueue_F64 mag = new GrowQueue_F64();
	// storage for significant gradients
	List<PointGradient_F64> significant = new ArrayList<PointGradient_F64>();

	// Classes related to optimization
	CornerFitFunction_F64 function = new CornerFitFunction_F64();
	CornerFitGradient_F64 functionGradient = new CornerFitGradient_F64();
	UnconstrainedMinimization minimization = FactoryOptimization.unconstrained();
	double initialParam[] = new double[2];

	// reference to input image
	T image;

	// the refined corner
	double refinedX;
	double refinedY;

	public SubpixelSparseCornerFit( Class<T> imageType ) {
		ImageBorder<T> border = FactoryImageBorder.general(imageType, BorderType.EXTENDED);
		gradient = FactoryDerivativeSparse.createSobel(imageType,border);

		function.setPoints(significant);
		functionGradient.setPoints(significant);
		minimization.setFunction(function,functionGradient,0);
	}

	/**
	 * Sets the image which
	 * @param image
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
		points.reset();

		// computes the local gradient around the initial estimate
		computeLocalGradient(cx, cy);

		// adjust scale of gradient for numerical reasons and find points with significant gradients
		significant.clear();
		massageGradient(significant);

		// optimize the solution.  It already has a reference to the significant points list
		minimization.initialize(initialParam,1e-6,1e-6);
		UtilOptimize.process(minimization,maxOptimizeSteps);
		double[] found = minimization.getParameters();

		// return the refined location while undoing the scale adjustment done earlier
		refinedX = found[0]*fitRadius + cx;
		refinedY = found[1]*fitRadius + cy;

		return true;
	}

	/**
	 * Computes the gradient locally while taking in account the image border and adjusts coordinates to be from
	 * -1 to 1.
	 *
	 */
	private void computeLocalGradient(int cx, int cy) {
		// Find the local region that it will search inside of while avoiding image border
		region.set(cx-fitRadius,cy-fitRadius, cx + fitRadius+1,cy + fitRadius+1);
		BoofMiscOps.boundRectangleInside(image, region);

		double dRadius = (double)fitRadius;

		// compute the gradient at each pixel while taking in account the ignore radius
		// scale the coordinate so that they range from -1 to 1 for numerical reasons.  not sure if neccisary
		// but doesn't really hurt
		for (int y = region.y0; y < region.y1; y++) {
			if( Math.abs(y-cy) > ignoreRadius )
				continue;
			double scaledY = (y-cy)/dRadius;
			for (int x = region.x0; x < region.x1; x++) {
				if( Math.abs(x-cx) > ignoreRadius )
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
	protected void massageGradient( List<PointGradient_F64> significant )
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

		double threshold = max*minRelThreshold;
		for (int i = 0; i < points.size; i++) {
			if( mag.get(i) >= threshold ) {
				PointGradient_F64 p = points.get(i);
				p.dx /= max;
				p.dy /= max;
				significant.add(p);
			}
		}
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
}
