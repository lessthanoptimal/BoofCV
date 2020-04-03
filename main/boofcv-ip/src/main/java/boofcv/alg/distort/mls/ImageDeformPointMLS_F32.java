/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import org.ejml.data.FMatrix2x2;

import java.util.Arrays;

/**
 * <p>Implementation of 'Moving Least Squares' (MLS) control point based image deformation models described in [1].</p>
 *
 * Usage Procedure:
 * <ol>
 *     <li>Invoke {@link #configure}</li>
 *     <li>Invoke {@link #addControl} for each control point</li>
 *     <li>Invoke {@link #setDistorted} to change the distorted location of a control point</li>
 *     <li>Invoke {@link #fixate()} when all control points have been added and after you are done changing distorted locations</li>
 * </ol>
 *
 * <p>Each control point has an undistorted and distorted location.  The fixate functions are used to precompute
 * different portions of the deformation to maximize speed by avoiding duplicate computations. Instead of computing
 * a distortion for each pixel a regular grid is used instead.  Pixel points are interpolated between grid points
 * using bilinear interpolation.
 * </p>
 *
 * <p>This should be a faithful implementation of MLS.  Potential deviations listed below:</p>
 * <ol>
 * <li>Pixels should be adjusted when converting to grid coordinates to maintain the same
 * aspect ratio as the input image.  This way the results is "independent" of the internal grids shape/size.
 * [1] does not mention this issue.</li>
 * <li>When compared against images published in [1] the rigid transform appears slightly different.  However,
 * when compared against other implementations those appear to produce nearly identical results to this
 * implementation.</li>
 * </ol>
 *
 * <p>[1] Schaefer, Scott, Travis McPhail, and Joe Warren. "Image deformation using moving least squares."
 * ACM transactions on graphics (TOG). Vol. 25. No. 3. ACM, 2006.</p>
 *
 * @author Peter Abeles
 */
public class ImageDeformPointMLS_F32 implements Point2Transform2_F32 {

	// control points that specifiy the distortion
	FastQueue<Control> controls = new FastQueue<>(Control::new);

	// size of interpolation grid
	int gridRows,gridCols;
	// points inside interpolation grid
	FastQueue<Cache> grid = new FastQueue<>(Cache::new);

	// DESIGN NOTE:  Because the aspect ratio is maintained it's likely that some points in the grid are unreachable
	//               a small speed boost could be brought about by adjusting the grid size so that the minimum number
	//               of cells are used

	// parameter used to adjust how distance between control points is weighted
	float alpha = 3.0f/2.0f;

	// scale between image and grid, adjusted to ensure aspect ratio doesn't change
	float scaleX,scaleY;

	// Pixel distortion model
	Model model;

	public ImageDeformPointMLS_F32( TypeDeformMLS type ) {
		switch( type ) {
			case AFFINE: model = new AffineModel(); break;
			case SIMILARITY: model = new SimilarityModel(); break;
			case RIGID: model = new RigidModel(); break;
			default:
				throw new RuntimeException("Unknown model type "+type);
		}
	}

	protected ImageDeformPointMLS_F32(){}

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

		// need to maintain the same ratio of pixels in the grid as in the regular image for similarity and rigid
		// to work correctly
		int s = Math.max(width,height);
		scaleX = s/(float)(gridCols-1);
		scaleY = s/(float)(gridRows-1);
		if( gridRows > gridCols ) {
			scaleY /= (gridCols-1)/ (float) (gridRows-1);
		} else {
			scaleX /= (gridRows-1)/ (float) (gridCols-1);
		}

		this.gridRows = gridRows;
		this.gridCols = gridCols;

		grid.resize(gridCols*gridRows);
		reset();
	}

	/**
	 * Adds a new control point at the specified location.  Initially the distorted and undistorted location will be
	 * set to the same
	 *
	 * @param x coordinate x-axis in image pixels
	 * @param y coordinate y-axis in image pixels
	 * @return Index of control point
	 */
	public int addControl( float x , float y ) {
		Control c = controls.grow();
		c.q.set(x,y);
		setUndistorted(controls.size-1,x,y);
		return controls.size-1;
	}

	/**
	 * Sets the location of a control point.
	 * @param x coordinate x-axis in image pixels
	 * @param y coordinate y-axis in image pixels
	 */
	public void setUndistorted(int which, float x, float y) {
		if( scaleX <= 0 || scaleY <= 0 )
			throw new IllegalArgumentException("Must call configure first");

		controls.get(which).p.set(x/scaleX,y/scaleY);
	}

	/**
	 * Function that let's you set control and undistorted points at the same time
	 * @param srcX distorted coordinate
	 * @param srcY distorted coordinate
	 * @param dstX undistorted coordinate
	 * @param dstY undistorted coordinate
	 * @return Index of control point
	 */
	public int add( float srcX , float srcY , float dstX , float dstY )
	{
		int which = addControl(srcX,srcY);
		setUndistorted(which,dstX,dstY);
		return which;
	}

	/**
	 * Sets the distorted location of a specific control point
	 * @param which Which control point
	 * @param x distorted coordinate x-axis in image pixels
	 * @param y distorted coordinate y-axis in image pixels
	 */
	public void setDistorted( int which , float x , float y ) {
		controls.get(which).q.set(x,y);
	}

	/**
	 * Precompute the portion of the equation which only concerns the undistorted location of each point on the
	 * grid even the current undistorted location of each control point.

	 * Recompute the deformation of each point in the internal grid now that the location of control points is
	 * not changing any more.
	 */
	public void fixate() {
		if( controls.size < 2 )
			throw new RuntimeException("Not enough control points specified.  Found "+controls.size);
		for (int row = 0; row < gridRows; row++) {
			for (int col = 0; col < gridCols; col++) {
				Cache cache = getGrid(row,col);

				float v_x = col;
				float v_y = row;

				float[] weights = computeWeights(cache, v_x, v_y);
				computeAverageP(cache, weights);
				computeAverageQ(cache, weights);

				float[] a = model.computeA(cache, weights, v_x, v_y);
				FMatrix2x2[] matrix = model.computeMatrix(cache, weights, v_x, v_y);
				model.computeDeformed(cache, a, matrix, col, row);
			}
		}
	}

	/**
	 * Computes the average P given the weights at this cached point
	 */
	void computeAverageP(Cache cache, float[] weights) {
		float x = 0;
		float y = 0;

		for (int i = 0; i < controls.size; i++) {
			Control c = controls.get(i);
			float w = weights[i];
			x += c.p.x * w;
			y += c.p.y * w;
		}
		cache.aveP.set(x / cache.weight, y / cache.weight);
	}

	/**
	 * Computes the average Q given the weights at this cached point
	 */
	void computeAverageQ(Cache cache, float[] weights) {
		float x = 0;
		float y = 0;

		for (int i = 0; i < controls.size; i++) {
			Control c = controls.get(i);
			float w = weights[i];
			x += c.q.x * w;
			y += c.q.y * w;
		}
		cache.aveQ.set(x / cache.weight, y / cache.weight);
	}

	/**
	 * Computes the weight/influence of each control point when distorting point v.
	 * @param cache Cache for the grid point
	 * @param v_x undistorted grid coordinate of cached point.
	 * @param v_y undistorted grid coordinate of cached point.
	 */
	float[] computeWeights(Cache cache, float v_x, float v_y) {
		float[] weights = new float[controls.size];
		// first compute the weights
		float totalWeight = 0.0f;
		for (int i = 0; i < controls.size; i++) {
			Control c = controls.get(i);

			float d2 = c.p.distance2(v_x, v_y);
			// check for the special case
			if( d2 == 0 ) {
				Arrays.fill(weights, 0);
				weights[i] = 1;
				totalWeight = 1.0f;
				break;
			} else {
				totalWeight += weights[i] = 1.0f/(float)Math.pow(d2,alpha);
			}
		}
		cache.weight = totalWeight;
		return weights;
	}

	@Override
	public void compute(float x, float y, Point2D_F32 out) {
		interpolateDeformedPoint(x/scaleX, y/scaleY, out);
	}

	@Override
	public ImageDeformPointMLS_F32 copyConcurrent() {
		ImageDeformPointMLS_F32 out = new ImageDeformPointMLS_F32();
		out.controls = controls;
		out.gridRows = gridRows;
		out.gridCols = gridCols;
		out.grid = grid;
		out.model = model;
		out.scaleX = scaleX;
		out.scaleY = scaleY;
		out.alpha = alpha;
		return out;
	}

	/**
	 * Samples the 4 grid points around v and performs bilinear interpolation
	 *
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

	Cache getGrid(int row , int col ) {
		return grid.data[row*gridCols + col];
	}

	/**
	 * See paper section 2.1
	 */
	public class AffineModel implements Model {

		@Override
		public float[] computeA(Cache cache, float[] weights, float v_x, float v_y) {
			float[] a = new float[controls.size];

			// compute the weighted covariance 2x2 matrix
			// Two below equation 5
			// sum hat(p[i])'*w[i]*hat(p[i])
			float inner00 = 0, inner01 = 0, inner11 = 0;

			for (int i = 0; i < controls.size; i++) {
				Control c = controls.get(i);
				float w = weights[i];

				float hat_p_x = c.p.x-cache.aveP.x;
				float hat_p_y = c.p.y-cache.aveP.y;

				inner00 += hat_p_x*hat_p_x*w;
				inner01 += hat_p_y*hat_p_x*w;
				inner11 += hat_p_y*hat_p_y*w;
			}

			// invert it using minor equation
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

			for (int i = 0; i < controls.size; i++) {
				Control c = controls.get(i);

				float hat_p_x = c.p.x-cache.aveP.x;
				float hat_p_y = c.p.y-cache.aveP.y;

				// mistake in paper that w[i] was omitted?
				a[i] = (tmp0 * hat_p_x + tmp1 * hat_p_y)*weights[i];
			}

			return a;
		}

		@Override
		public FMatrix2x2[] computeMatrix(Cache cache, float[] weights, float v_x, float v_y)
		{
			return null;
		}

		@Override
		public void computeDeformed(Cache cache, float[] a2, FMatrix2x2[] matrix, float v_x, float v_y ) {
			Point2D_F32 deformed = cache.deformed;
			deformed.set(0,0);

			int N = a2.length;
			for (int i = 0; i < N; i++) {
				Control c = controls.get(i);
				float a = a2[i];
				deformed.x += a*(c.q.x-cache.aveQ.x);
				deformed.y += a*(c.q.y-cache.aveQ.y);
			}
			deformed.x += cache.aveQ.x;
			deformed.y += cache.aveQ.y;
		}
	}

	/**
	 * See paper section 2.2
	 */
	public class SimilarityModel implements Model {

		@Override
		public float[] computeA(Cache cache, float[] weights, float v_x, float v_y)
		{
			return null;
		}

		@Override
		public FMatrix2x2[] computeMatrix(Cache cache, float[] weights, float v_x, float v_y) {
			FMatrix2x2[] matrix = new FMatrix2x2[controls.size];

			cache.mu = 0;

			// mu = sum{ w[i]*dot( hat(p). hat(p) ) }
			// A[i] = w[i]*( hat(p); hat(p^|) )( v-p*; -(v-p*)^|)'
			// where ^| means perpendicular to vector
			for (int i = 0; i < controls.size; i++) {
				Control c = controls.get(i);
				float w = weights[i];

				float hat_p_x = c.p.x-cache.aveP.x;
				float hat_p_y = c.p.y-cache.aveP.y;

				cache.mu += w*(hat_p_x*hat_p_x + hat_p_y*hat_p_y);

				float v_ps_x = v_x - cache.aveP.x;
				float v_ps_y = v_y - cache.aveP.y;

				FMatrix2x2 A = new FMatrix2x2();
				matrix[i] = A;

				A.a11 = w*(hat_p_x*v_ps_x + hat_p_y*v_ps_y);
				A.a12 = w*(hat_p_x*v_ps_y - hat_p_y*v_ps_x);
				A.a21 = -A.a12;
				A.a22 = A.a11;
			}
			// point being sampled and the key point are exactly the same
			if( cache.mu == 0.0f )
				cache.mu = 1.0f;

			return matrix;
		}

		@Override
		public void computeDeformed(Cache cache, float[] a, FMatrix2x2[] matrix, float v_x, float v_y ) {
			Point2D_F32 deformed = cache.deformed;
			deformed.set(0,0);

			int N = matrix.length;
			for (int i = 0; i < N; i++) {
				Control c = controls.get(i);

				FMatrix2x2 A = matrix[i];
				float hat_q_x = c.q.x-cache.aveQ.x;
				float hat_q_y = c.q.y-cache.aveQ.y;

				deformed.x += hat_q_x*A.a11 + hat_q_y*A.a21;
				deformed.y += hat_q_x*A.a12 + hat_q_y*A.a22;
			}
			deformed.x = deformed.x/cache.mu + cache.aveQ.x;
			deformed.y = deformed.y/cache.mu + cache.aveQ.y;
		}
	}

	/**
	 * Paper section 2.3
	 */
	public class RigidModel extends SimilarityModel {

		@Override
		public void computeDeformed(Cache cache, float[] a, FMatrix2x2[] matrix, float v_x, float v_y ) {

			// f_r[v] equation just above equation 8
			float fr_x = 0, fr_y = 0;
			for (int i = 0; i < controls.size; i++) {
				Control c = controls.get(i);

				float hat_q_x = c.q.x - cache.aveQ.x;
				float hat_q_y = c.q.y - cache.aveQ.y;

				FMatrix2x2 A = matrix[i];
				fr_x += (hat_q_x*A.a11 + hat_q_y*A.a21);
				fr_y += (hat_q_x*A.a12 + hat_q_y*A.a22);
			}

			// equation 8
			float v_avep_x = v_x - cache.aveP.x;
			float v_avep_y = v_y - cache.aveP.y;

			float norm_fr = (float)Math.sqrt(fr_x*fr_x + fr_y*fr_y);
			float norm_vp = (float)Math.sqrt(v_avep_x*v_avep_x + v_avep_y*v_avep_y);

			// point being sampled and the key point are exactly the same
			if( norm_fr == 0.0f && norm_vp == 0.0f ) {
				cache.deformed.x = cache.aveQ.x;
				cache.deformed.y = cache.aveQ.y;
			} else {
				float scale = norm_vp / norm_fr;

				cache.deformed.x = scaleX * fr_x * scale + cache.aveQ.x;
				cache.deformed.y = scaleY * fr_y * scale + cache.aveQ.y;
			}
		}
	}

	public static class Cache {
		// location of the final deformed point
		Point2D_F32 deformed = new Point2D_F32();
		float weight;
		Point2D_F32 aveP = new Point2D_F32(); // average control point for given weights
		Point2D_F32 aveQ = new Point2D_F32(); // average distorted point for given weights

		// mu for simularity
		float mu;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	private interface Model {
		float[] computeA(Cache cache, float[] weights, float v_x, float v_y);

		FMatrix2x2[] computeMatrix(Cache cache, float[] weights, float v_x, float v_y);

		void computeDeformed(Cache cache, float[] a, FMatrix2x2[] matrix, float v_x, float v_y);
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
