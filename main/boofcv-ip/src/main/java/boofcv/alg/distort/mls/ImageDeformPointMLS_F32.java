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

	// control points that specify the distortion
	FastQueue<Control> controls = new FastQueue<>(Control::new);

	// size of interpolation grid
	int gridRows,gridCols;
	// points inside interpolation grid
	FastQueue<Point2D_F32> deformationGrid = new FastQueue<>(Point2D_F32::new);

	// DESIGN NOTE:  Because the aspect ratio is maintained it's likely that some points in the grid are unreachable
	//               a small speed boost could be brought about by adjusting the grid size so that the minimum number
	//               of cells are used

	// parameter used to adjust how distance between control points is weighted
	float alpha = 3.0f/2.0f;

	// scale between image and grid, adjusted to ensure aspect ratio doesn't change
	float scaleX,scaleY;

	// Pixel distortion model
	Model model;

	//--------------------------- Internal Workspace --------------------------------------------
	GrowQueue_F32 weights = new GrowQueue_F32(); // weight of each control point
	FastQueue<FMatrix2x2> matrices = new FastQueue<>(FMatrix2x2::new);
	GrowQueue_F32 A = new GrowQueue_F32(); // As as the variable 'A' in the paper
	Point2D_F32 aveP = new Point2D_F32();  // average control point for given weights
	Point2D_F32 aveQ = new Point2D_F32();  // average distorted point for given weights
	float totalWeight;
	float mu;                              // mu for simularity

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

		deformationGrid.resize(gridCols*gridRows);
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
		model.allocate(weights,A,matrices);
		for (int row = 0; row < gridRows; row++) {
			for (int col = 0; col < gridCols; col++) {

				float v_x = col;
				float v_y = row;

				computeWeights(v_x, v_y,weights.data);
				computeAverageP( weights.data);
				computeAverageQ( weights.data);

				model.computeIntermediate( v_x, v_y);
				model.computeDeformed(col, row, getGrid(row,col));
			}
		}
	}

	/**
	 * Computes the average P given the weights at this cached point
	 */
	void computeAverageP(float[] weights) {
		float x = 0;
		float y = 0;

		for (int i = 0; i < controls.size; i++) {
			Control c = controls.get(i);
			float w = weights[i];
			x += c.p.x * w;
			y += c.p.y * w;
		}
		aveP.set(x / totalWeight, y / totalWeight);
	}

	/**
	 * Computes the average Q given the weights at this cached point
	 */
	void computeAverageQ(float[] weights) {
		float x = 0;
		float y = 0;

		for (int i = 0; i < controls.size; i++) {
			Control c = controls.get(i);
			float w = weights[i];
			x += c.q.x * w;
			y += c.q.y * w;
		}
		aveQ.set(x / totalWeight, y / totalWeight);
	}

	/**
	 * Computes the weight/influence of each control point when distorting point v.
	 * @param v_x undistorted grid coordinate of cached point.
	 * @param v_y undistorted grid coordinate of cached point.
	 */
	void computeWeights(float v_x, float v_y, float[] weights) {
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
		this.totalWeight = totalWeight;
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
		out.deformationGrid = deformationGrid;
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
		Point2D_F32 d00 = getGrid(y0,x0);
		Point2D_F32 d01 = getGrid(y0,x1);
		Point2D_F32 d10 = getGrid(y1,x0);
		Point2D_F32 d11 = getGrid(y1,x1);

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

	/**
	 * Returns distorted control point in the deformation grid
	 */
	Point2D_F32 getGrid(int row , int col ) {
		return deformationGrid.data[row*gridCols + col];
	}

	/**
	 * See paper section 2.1
	 */
	public class AffineModel implements Model {

		@Override
		public void allocate(GrowQueue_F32 weights, GrowQueue_F32 A, FastQueue<FMatrix2x2> matrices) {
			weights.resize(controls.size);
			A.resize(controls.size);
			matrices.resize(controls.size);
		}

		@Override
		public void computeIntermediate(float v_x, float v_y) {
			// compute the weighted covariance 2x2 matrix
			// Two below equation 5
			// sum hat(p[i])'*w[i]*hat(p[i])
			float inner00 = 0, inner01 = 0, inner11 = 0;

			for (int i = 0; i < controls.size; i++) {
				Control c = controls.get(i);
				float w = weights.data[i];

				float hat_p_x = c.p.x-aveP.x;
				float hat_p_y = c.p.y-aveP.y;

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
			float v_m_ap_x = v_x - aveP.x;
			float v_m_ap_y = v_y - aveP.y;
			float tmp0 = v_m_ap_x * inv00 + v_m_ap_y * inv01;
			// (v-p*)*inv(stuff)
			float tmp1 = v_m_ap_x * inv01 + v_m_ap_y * inv11;

			for (int i = 0; i < controls.size; i++) {
				Control c = controls.get(i);

				float hat_p_x = c.p.x-aveP.x;
				float hat_p_y = c.p.y-aveP.y;

				// mistake in paper that w[i] was omitted?
				A.data[i] = (tmp0 * hat_p_x + tmp1 * hat_p_y)*weights.data[i];
			}
		}

		@Override
		public void computeDeformed(float v_x, float v_y, Point2D_F32 deformed) {
			deformed.set(0,0);

			final int totalControls =  controls.size;
			for (int i = 0; i < totalControls; i++) {
				Control c = controls.data[i];
				final float a = A.data[i];
				deformed.x += a*(c.q.x-aveQ.x);
				deformed.y += a*(c.q.y-aveQ.y);
			}
			deformed.x += aveQ.x;
			deformed.y += aveQ.y;
		}
	}

	/**
	 * See paper section 2.2
	 */
	public class SimilarityModel implements Model
	{
		@Override
		public void allocate(GrowQueue_F32 weights, GrowQueue_F32 A, FastQueue<FMatrix2x2> matrices) {
			weights.resize(controls.size);
			matrices.resize(controls.size);
		}

		@Override
		public void computeIntermediate(float v_x, float v_y) {
			final float[] weights = ImageDeformPointMLS_F32.this.weights.data;
			mu = 0;

			// mu = sum{ w[i]*dot( hat(p). hat(p) ) }
			// A[i] = w[i]*( hat(p); hat(p^|) )( v-p*; -(v-p*)^|)'
			// where ^| means perpendicular to vector
			final int totalControls =  controls.size;
			for (int i = 0; i < totalControls; i++) {
				Control c = controls.get(i);
				float w = weights[i];

				float hat_p_x = c.p.x-aveP.x;
				float hat_p_y = c.p.y-aveP.y;

				mu += w*(hat_p_x*hat_p_x + hat_p_y*hat_p_y);

				float v_ps_x = v_x - aveP.x;
				float v_ps_y = v_y - aveP.y;

				FMatrix2x2 m = matrices.get(i);

				m.a11 = w*(hat_p_x*v_ps_x + hat_p_y*v_ps_y);
				m.a12 = w*(hat_p_x*v_ps_y - hat_p_y*v_ps_x);
				m.a21 = -m.a12;
				m.a22 = m.a11;
			}
			// point being sampled and the key point are exactly the same
			if( mu == 0.0f )
				mu = 1.0f;
		}

		@Override
		public void computeDeformed(float v_x, float v_y, Point2D_F32 deformed) {
			deformed.set(0,0);

			final int totalControls =  controls.size;
			for (int i = 0; i < totalControls; i++) {
				Control c = controls.get(i);

				FMatrix2x2 m = matrices.data[i];
				float hat_q_x = c.q.x-aveQ.x;
				float hat_q_y = c.q.y-aveQ.y;

				deformed.x += hat_q_x*m.a11 + hat_q_y*m.a21;
				deformed.y += hat_q_x*m.a12 + hat_q_y*m.a22;
			}
			deformed.x = deformed.x/mu + aveQ.x;
			deformed.y = deformed.y/mu + aveQ.y;
		}
	}

	/**
	 * Paper section 2.3
	 */
	public class RigidModel extends SimilarityModel {

		@Override
		public void computeDeformed(float v_x, float v_y, Point2D_F32 deformed) {

			// f_r[v] equation just above equation 8
			float fr_x = 0, fr_y = 0;
			for (int i = 0; i < controls.size; i++) {
				Control c = controls.get(i);

				float hat_q_x = c.q.x - aveQ.x;
				float hat_q_y = c.q.y - aveQ.y;

				FMatrix2x2 m = matrices.get(i);
				fr_x += (hat_q_x*m.a11 + hat_q_y*m.a21);
				fr_y += (hat_q_x*m.a12 + hat_q_y*m.a22);
			}

			// equation 8
			float v_avep_x = v_x - aveP.x;
			float v_avep_y = v_y - aveP.y;

			float norm_fr = (float)Math.sqrt(fr_x*fr_x + fr_y*fr_y);
			float norm_vp = (float)Math.sqrt(v_avep_x*v_avep_x + v_avep_y*v_avep_y);

			// point being sampled and the key point are exactly the same
			if( norm_fr == 0.0f && norm_vp == 0.0f ) {
				deformed.x = aveQ.x;
				deformed.y = aveQ.y;
			} else {
				float scale = norm_vp / norm_fr;

				deformed.x = scaleX * fr_x * scale + aveQ.x;
				deformed.y = scaleY * fr_y * scale + aveQ.y;
			}
		}
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	/**
	 * Distortion model interface
	 */
	private interface Model {
		/** Pre-allocates the size of each of these arrays */
		void allocate( GrowQueue_F32 weights, GrowQueue_F32 A , FastQueue<FMatrix2x2> matrices );

		/** Computes intermediate results needed for the distortion*/
		void computeIntermediate(float v_x, float v_y);

		/** Computes the deformation at each control point */
		void computeDeformed(float v_x, float v_y, Point2D_F32 deformed);
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
