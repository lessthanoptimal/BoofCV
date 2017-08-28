/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d3.direct;

import georegression.struct.point.Point2D_F32;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.FMatrix2x2;
import org.ejml.dense.fixed.CommonOps_FDF2;

/**
 * Checks to see if the features being tracked form
 *
 * @author Peter Abeles
 */
public class FeatureSpatialDiversity_F32 {

	FMatrix2x2 var = new FMatrix2x2();
	float meanX,meanY;

	FastQueue<Point2D_F32> norm = new FastQueue<>(Point2D_F32.class, true);

	// number of standard deviations that will be used to compute the spread
	float sigmas = 3.0f;
	// Approximate number of radians the points are spread out over  along the smallest axis
	double spread;

	public void reset() {
		norm.reset();
	}

	/**
	 * Adds the estimated 3D location of a feature.
	 */
	public void addPoint( float x , float y , float z ) {
		norm.grow().set(x/z, y/z);
	}

	/**
	 * Computes the worst case spread for how features are laid out
	 */
	public void process() {

		computeCovarince();

		float eigenvalue = smallestEigenvalue();
		// eigenvalue is the variance, convert to standard deviation
		double stdev = Math.sqrt(eigenvalue);
//		System.out.println("stdev "+stdev+"  total "+norm.size()+"  mean "+meanX+"  "+meanY);

		// approximate the spread in by doing it along the x-axis.
		// Really should be along the smallest singular axis
		double angle0 = Math.atan2(1.0,sigmas*(meanX-stdev));
		double angle1 = Math.atan2(1.0,sigmas*(meanX+stdev));

		spread = Math.abs(angle1-angle0);
	}

	private void computeCovarince() {
		meanX=0;
		meanY=0;

		for (int i = 0; i < norm.size; i++) {
			Point2D_F32 p = norm.get(i);
			meanX += p.x;
			meanY += p.y;
		}

		meanX /= norm.size;
		meanY /= norm.size;

		var.a11 = var.a12 = var.a22 = 0;

		for (int i = 0; i < norm.size; i++) {
			Point2D_F32 p = norm.get(i);

			float dx = p.x-meanX;
			float dy = p.y-meanY;

			var.a11 += dx*dx;
			var.a12 += dx*dy;
			var.a22 += dy*dy;
		}

		CommonOps_FDF2.divide(var, norm.size-1);

//		System.out.printf("  covar  %5.2f %5.2f %5.4f\n",var.a11,var.a22, var.a12);
	}

	/**
	 * Number of radians in view that the smallest features lie along
	 */
	public double getSpread() {
		return spread;
	}

	private float smallestEigenvalue() {
		// compute the smallest eigenvalue
		float left = (var.a11 + var.a22) * 0.5f;
		float b = (var.a11 - var.a22) * 0.5f;
		float right = (float)Math.sqrt(b * b + var.a12 * var.a12);

		// the smallest eigenvalue will be minus the right side
		return left - right;
	}

}

