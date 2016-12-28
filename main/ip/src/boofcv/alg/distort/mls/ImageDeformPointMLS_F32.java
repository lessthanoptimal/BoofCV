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

package boofcv.alg.distort.mls;

import boofcv.struct.distort.Point2Transform2_F32;
import georegression.struct.point.Point2D_F32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F32;

/**
 * <p>Implementation of 'Moving Least Squares' control point based image deformation models described in [1].</p>
 *
 * Usage Procedure:
 * <ol>
 *     <li>Invoke {@link #configure}</li>
 *     <li>Invoke {@link #addControl} for each control point</li>
 *     <li>Invoke {@link #fixateUndistorted()} ()} when all control points have been added</li>
 *     <li>Invoke {@link #setDistorted} to change the distorted location of a control point</li>
 *     <li>Invoke {@link #fixateDistorted()} after you are done changing distorted locations</li>
 * </ol>
 *
 * <p>Each control point has an undistorted and distorted location.  The fixate functions are used to precompute
 * different portions of the deformation to maximize speed by avoiding duplicate computations. Instead of computing
 * a distortion for each pixel a regular grid is used instead.  Pixel points are interpolated between grid points
 * using bilinear interpolation.
 * </p>
 *
 * <p>[1] Schaefer, Scott, Travis McPhail, and Joe Warren. "Image deformation using moving least squares."
 * ACM transactions on graphics (TOG). Vol. 25. No. 3. ACM, 2006.</p>
 *
 * @author Peter Abeles
 */
// TODO add similar
// TODO add rigid
public class ImageDeformPointMLS_F32 implements Point2Transform2_F32 {

	// control points that specifiy the distortion
	FastQueue<Control> controls = new FastQueue<>(Control.class,true);

	// size of interpolation grid
	int gridRows,gridCols;
	// points inside interpolation grid
	FastQueue<AffineCache> grid = new FastQueue<>(AffineCache.class, true);

	// parameter used to adjust how distance between control points is weighted
	float alpha = 3.0f/2.0f;

	// scale between image and grid
	float scaleX,scaleY;

	public ImageDeformPointMLS_F32( TypeDeformMLS type ) {
	}

	/**
	 * Discards all existing control points
	 */
	public void reset() {
		controls.reset();
	}

	/**
	 * Specifies the input image size and the size of the grid it will use to approximate the idea solution. All
	 * control points are discarded
	 *
	 * @param width Image width
	 * @param height Image height
	 * @param gridRows grid rows
	 * @param gridCols grid columns
	 */
	public void configure( int width , int height , int gridRows , int gridCols ) {
		scaleX = width/(float)(gridCols-1);
		scaleY = height/(float)(gridRows-1);

		this.gridRows = gridRows;
		this.gridCols = gridCols;

		grid.resize(gridCols*gridRows);
		reset();
	}

	/**
	 * Sets the location of a control point.  Initially the distorted and undistorted location will be set to
	 * be the same.
	 * @param x coordinate x-axis in image pixels
	 * @param y coordinate y-axis in image pixels
	 * @return Index of control point
	 */
	public int addControl( float x , float y ) {
		if( scaleX <= 0 || scaleY <= 0 )
			throw new IllegalArgumentException("Must call configure first");
		Control c = controls.grow();
		c.p.set(x/scaleX,y/scaleY);
		c.q.set(x,y);

		return controls.size()-1;
	}

	/**
	 * Sets the distorted location of a specific control point
	 * @param which Which control point
	 * @param x distorted coordinate x-axis in image pixels
	 * @param y distorted coordinate y-axis in image pixels
	 */
	public void setDistorted( int which , float x , float y ) {
		if( controls.get(which).q.distance(x,y) > 0.0001 ) {
			System.out.println("q changed "+which);
		}
		controls.get(which).q.set(x,y);
	}

	/**
	 * Precompute the portion of the equation which only concerns the undistorted location of each point on the
	 * grid even the current undistorted location of each control point.
	 */
	public void fixateUndistorted() {

		for (int row = 0; row < gridRows; row++) {
			for (int col = 0; col < gridCols; col++) {
				AffineCache cache = getGrid(row,col);
				cache.weights.resize(controls.size);
				cache.A.resize(controls.size);

				float v_x = col;
				float v_y = row;

				computeWeights(cache, v_x, v_y);
				computeAverageP(cache);
				computeAffineCache(cache, v_x, v_y);
			}
		}
	}

	/**
	 * Recompute the deformation of each point in the internal grid now that the location of control points is
	 * not changing any more.  Must call {@link #fixateUndistorted()} first.
	 */
	public void fixateDistorted() {
		for (int row = 0; row < gridRows; row++) {
			for (int col = 0; col < gridCols; col++) {
				AffineCache cache = getGrid(row,col);
				computeAverageQ( cache );
				computeAffineDeformed( cache );
				if( row == 0 && col == 1 ) {
					System.out.println("average q " + cache.aveQ);
					System.out.println("deformed " + cache.deformed);
				}
			}
		}
	}

	/**
	 * Compute deformed location for a given grid point given it's cached data
	 */
	void computeAffineDeformed(AffineCache cache ) {
		Point2D_F32 deformed = cache.deformed;
		deformed.set(0,0);

		int N = cache.A.size;
		for (int i = 0; i < N; i++) {
			Control c = controls.get(i);
			float a = cache.A.data[i];
			deformed.x += a*(c.q.x-cache.aveQ.x);
			deformed.y += a*(c.q.y-cache.aveQ.y);
		}
		deformed.x += cache.aveQ.x;
		deformed.y += cache.aveQ.y;
	}


	/**
	 * Precompute as much as possible for each control point for affine distortion.  This takes advantage of 'p'
	 * not changing.
	 * @param cache Cache for a particular point in the grid
	 * @param v_x grid coordinate for the cached point
	 * @param v_y grid coordinate for the cached point
	 */
	void computeAffineCache(AffineCache cache, float v_x, float v_y) {
		float[] weights = cache.weights.data;

		// compute the weighted covariance 2x2 matrix
		// sum hat(p[i])'*w[i]*hat(p[i])
		float inner00 = 0, inner01 = 0, inner11 = 0;

		for (int i = 0; i < controls.size(); i++) {
			Control c = controls.get(i);
			float w = weights[i];

			float hat_p_x = c.p.x-cache.aveP.x;
			float hat_p_y = c.p.y-cache.aveP.y;

			inner00 += hat_p_x*hat_p_x*w;
			inner01 += hat_p_y*hat_p_x*w;
			inner11 += hat_p_y*hat_p_y*w;
		}

		// invert it using minors equation
		float det = (inner00*inner11 - inner01*inner01);

		if( det == 0.0 ) {
			// see if a control point and grid point are exactly the same
			if( inner00 == 0.0f && inner11 == 0.0f ) {
				det = 1.0f;
			} else {
				throw new RuntimeException("Insufficient number of or geometric diversity in control points");
			}
		}

		float inv00 =  inner11 / det;
		float inv01 = -inner01 / det;
		float inv11 =  inner00 / det;

		// Finally compute A[i] for each control point
		// (v-p*)
		float v_m_ap_x = v_x - cache.aveP.x;
		float v_m_ap_y = v_y - cache.aveP.y;
		float tmp0 = v_m_ap_x * inv00 + v_m_ap_y * inv01;
		// (v-p*)*inv(stuff)
		float tmp1 = v_m_ap_x * inv01 + v_m_ap_y * inv11;

		for (int i = 0; i < controls.size(); i++) {
			Control c = controls.get(i);

			float hat_p_x = c.p.x-cache.aveP.x;
			float hat_p_y = c.p.y-cache.aveP.y;

			// mistake in paper that w[i] was ommitted?
			cache.A.data[i] = (tmp0 * hat_p_x + tmp1 * hat_p_y)*weights[i];
		}
	}

	/**
	 * Computes the average P given the weights at this cached point
	 */
	void computeAverageP(AffineCache cache) {
		float[] weights = cache.weights.data;
		cache.aveP.set(0,0);

		for (int i = 0; i < controls.size(); i++) {
			Control c = controls.get(i);
			float w = weights[i];
			cache.aveP.x += c.p.x * w;
			cache.aveP.y += c.p.y * w;
		}
		cache.aveP.x /= cache.totalWeight;
		cache.aveP.y /= cache.totalWeight;
	}

	/**
	 * Computes the average Q given the weights at this cached point
	 */
	void computeAverageQ(AffineCache cache) {
		float[] weights = cache.weights.data;
		cache.aveQ.set(0,0);

		for (int i = 0; i < controls.size(); i++) {
			Control c = controls.get(i);
			float w = weights[i];
			cache.aveQ.x += c.q.x * w;
			cache.aveQ.y += c.q.y * w;
		}
		cache.aveQ.x /= cache.totalWeight;
		cache.aveQ.y /= cache.totalWeight;
	}

	/**
	 * Computes the weight/influence of each control point when distorting point v.
	 * @param cache Cache for the grid point
	 * @param v_x undistorted grid coordinate of cached point.
	 * @param v_y undistorted grid coordinate of cached point.
	 */
	void computeWeights(AffineCache cache, float v_x, float v_y) {
		float[] weights = cache.weights.data;
		// first compute the weights
		float totalWeight = 0.0f;
		for (int i = 0; i < controls.size(); i++) {
			Control c = controls.get(i);

			float d2 = c.p.distance2(v_x, v_y);
			// check for the special case
			if( d2 == 0 ) {
				for (int j = 0; j < controls.size(); j++) {
					weights[j] = i==j ? 1.0f : 0.0f;
				}
				totalWeight = 1.0f;
				break;
			} else {
				totalWeight += weights[i] = 1.0f/(float)Math.pow(d2,alpha);
			}
		}
		cache.totalWeight = totalWeight;
	}

	@Override
	public void compute(float x, float y, Point2D_F32 out) {
		interpolateDeformedPoint(x/scaleX, y/scaleY, out);
	}

	/**
	 * Samples the 4 grid points around v and performs bilinear interpolation
	 * @param v_x Grid coordinate x-axis, undistorted
	 * @param v_y Grid coordinate y-axis, undistorted
	 * @param deformed Distorted grid coordinate in image pixels
	 */
	void interpolateDeformedPoint(float v_x , float v_y , Point2D_F32 deformed ) {

		// sample the closest point and x+1,y+1
		int x0 = (int)v_x;
		int y0 = (int)v_y;
		int x1 = x0+1;
		int y1 = y0+1;

		// make sure the 4 sample points are in bounds
		if( x1 >= gridCols )
			x1 = gridCols-1;
		if( y1 >= gridRows )
			y1 = gridRows-1;

		// weight along each axis
		float ax = v_x - x0;
		float ay = v_y - y0;

		// bilinear weight for each sample point
		float w00 = (1.0f - ax) * (1.0f - ay);
		float w01 = ax * (1.0f - ay);
		float w11 = ax * ay;
		float w10 = (1.0f - ax) * ay;

		// apply weights to each sample point
		Point2D_F32 d00 = getGrid(y0,x0).deformed;
		Point2D_F32 d01 = getGrid(y0,x1).deformed;
		Point2D_F32 d10 = getGrid(y1,x0).deformed;
		Point2D_F32 d11 = getGrid(y1,x1).deformed;

		deformed.set(0,0);
		deformed.x += w00 * d00.x;
		deformed.x += w01 * d01.x;
		deformed.x += w11 * d11.x;
		deformed.x += w10 * d10.x;

		deformed.y += w00 * d00.y;
		deformed.y += w01 * d01.y;
		deformed.y += w11 * d11.y;
		deformed.y += w10 * d10.y;
	}

	AffineCache getGrid( int row , int col ) {
		return grid.data[row*gridCols + col];
	}

	public static class Cache {
		// location of the final deformed point
		public Point2D_F32 deformed = new Point2D_F32();
	}

	public static class AffineCache extends Cache {
		float totalWeight;
		public GrowQueue_F32 weights = new GrowQueue_F32(); // weight of each control point
		public GrowQueue_F32 A = new GrowQueue_F32(); // As as the variable 'A' in the paper
		public Point2D_F32 aveP = new Point2D_F32(); // average control point for given weights
		public Point2D_F32 aveQ = new Point2D_F32(); // average distorted point for given weights
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public static class Control {
		/**
		 * Control point location in grid coordinates
		 */
		Point2D_F32 p = new Point2D_F32();
		/**
		 * Deformed control point location in image pixels
		 */
		Point2D_F32 q = new Point2D_F32();
	}
}
