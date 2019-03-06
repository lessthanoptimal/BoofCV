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

package boofcv.alg.geo.h;


import boofcv.alg.geo.LowLevelMultiViewOps;
import boofcv.alg.geo.NormalizationPoint2D;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedPair3D;
import boofcv.struct.geo.AssociatedPairConic;
import georegression.geometry.UtilCurves_F64;
import georegression.struct.curve.ConicGeneral_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.row.linsol.svd.SolveNullSpaceSvd_DDRM;
import org.ejml.interfaces.SolveNullSpace;
import org.ejml.simple.SimpleMatrix;

import javax.annotation.Nullable;
import java.util.List;

/**
 * <p>
 * Using linear algebra it computes a planar homography matrix using 2D points, 3D points, or conics. Typically used
 * as an initial estimate for a non-linear optimization. See [1] for 2D and 3D points, and [2] for conics.
 * </p>
 *
 * <p>
 * The algorithm works by solving the equation below:<br>
 * hat(x<sub>2</sub>)*H*x<sub>1</sub> = 0<br>
 * where hat(x) is the skew symmetric cross product matrix. To solve this equation is is reformatted into
 * A*H<sup>s</sup>=0 using the Kronecker product and the null space solved for.
 * </p>
 *
 * <p>
 * [1] Chapter 4, "Multiple View Geometry in Computer Vision"  2nd Ed. but uses normalization
 * from "An Invitation to 3-D Vision" 2004.<br>
 * [2] Kannala, Juho, Mikko Salo, and Janne Heikkilä.  "Algorithms for Computing a Planar Homography from
 * Conics in Correspondence." BMVC. 2006.
 * </p>
 *
 * @author Peter Abeles
 */
public class HomographyDirectLinearTransform {

	// contains the set of equations that are solved
	protected DMatrixRMaj A = new DMatrixRMaj(1,9);
	protected SolveNullSpace<DMatrixRMaj> solverNullspace = new SolveNullSpaceSvd_DDRM();

	// Used to normalize input points
	protected NormalizationPoint2D N1 = new NormalizationPoint2D();
	protected NormalizationPoint2D N2 = new NormalizationPoint2D();

	// pick a reasonable scale and sign
	private AdjustHomographyMatrix adjust = new AdjustHomographyMatrix();

	// normalize image coordinates to avoid numerical errors?
	boolean normalize;
	// if it's actually normalizing points
	private boolean shouldNormalize;

	/**
	 * Configure homography calculation
	 *
	 * @param normalizeInput Should image coordinate be normalized?  Needed when coordinates are in units of pixels.
	 */
	public HomographyDirectLinearTransform(boolean normalizeInput) {
		this.normalize = normalizeInput;
	}

	/**
	 * <p>
	 * Computes the homography matrix given a set of observed points in two images.  A set of {@link AssociatedPair}
	 * is passed in.  The computed homography 'H' is found such that the attributes 'p1' and 'p2' in {@link AssociatedPair}
	 * refers to x1 and x2, respectively, in the equation  below:<br>
	 * x<sub>2</sub> = H*x<sub>1</sub>
	 * </p>
	 *
	 * @param points A set of observed image points that are generated from a planar object.  Minimum of 4 pairs required.
	 * @param foundH Output: Storage for the found solution. 3x3 matrix.
	 * @return True if successful. False if it failed.
	 */
	public boolean process( List<AssociatedPair> points , DMatrixRMaj foundH ) {
		return process(points,null,null,foundH);
	}

	/**
	 * More versatile process function. Lets any of the supporting data structures be passed in.
	 *
	 * @param points2D List of 2D point associations. Can be null.
	 * @param points3D List of 3D point or 2D line associations. Can be null.
	 * @param conics List of conics. Can be null
	 * @param foundH (Output) The estimated homography
	 * @return True if successful. False if it failed.
	 */
	public boolean process( @Nullable List<AssociatedPair> points2D ,
							@Nullable List<AssociatedPair3D> points3D ,
							@Nullable List<AssociatedPairConic> conics ,
							DMatrixRMaj foundH ) {
		int num2D = points2D != null ? points2D.size() : 0;
		int num3D = points3D != null ? points3D.size() : 0;
		int numConic = conics != null ? conics.size() : 0;

		int numRows = computeTotalRows(num2D,num3D,numConic);

		if( numRows < 8 )
			throw new IllegalArgumentException("Must be at least 8 constraints. Found "+numRows);

		// only 2D points need to be normalzied because of the implicit z=1
		// 3D points are homogenous or lines and the vector can be normalized to 1
		// same goes for the conic equation
		shouldNormalize = normalize && points2D != null;

		if( shouldNormalize ) {
			LowLevelMultiViewOps.computeNormalization(points2D, N1, N2);
		}
		A.reshape(numRows,9);
		A.zero();

		int rows = 0;
		if( points2D != null )
			rows = addPoints2D(points2D,A,rows);
		if( points3D != null )
			rows = addPoints3D(points3D,A,rows);
		if( conics != null )
			addConics(conics,A,rows);

		// compute the homograph matrix up to a scale factor
		if (computeH(A,foundH))
			return false;

		if( shouldNormalize )
			undoNormalizationH(foundH,N1,N2);

		// pick a good scale and sign for H
		if( points2D != null )
			adjust.adjust(foundH,points2D.get(0));

		return true;
	}


	/**
	 * Computes the SVD of A and extracts the homography matrix from its null space
	 */
	protected boolean computeH(DMatrixRMaj A, DMatrixRMaj H) {

		if( !solverNullspace.process(A.copy(),1,H) )
			return true;

		H.numRows = 3;
		H.numCols = 3;

		return false;
	}

	/**
	 * Undoes normalization for a homography matrix.
	 */
	public static void undoNormalizationH(DMatrixRMaj M, NormalizationPoint2D N1, NormalizationPoint2D N2) {
		SimpleMatrix a = SimpleMatrix.wrap(M);
		SimpleMatrix b = SimpleMatrix.wrap(N1.matrix());
		SimpleMatrix c_inv = SimpleMatrix.wrap(N2.matrixInv());

		SimpleMatrix result = c_inv.mult(a).mult(b);

		M.set(result.getDDRM());
	}

	private void adjustPoint( AssociatedPair pair , Point2D_F64 a1 , Point2D_F64 a2 ) {
		if( shouldNormalize ) {
			N1.apply(pair.p1, a1);
			N2.apply(pair.p2, a2);
		} else {
			a1.set(pair.p1);
			a2.set(pair.p2);
		}
	}

	int computeTotalRows( int num2D , int num3D , int numConic ) {
		return 2*num2D + 2*num3D + 3*numConic;
	}

	private void adjustPoint(AssociatedPair3D pair , Point3D_F64 a1 , Point3D_F64 a2 ) {
		if( shouldNormalize ) {
			N1.apply(pair.p1, a1);
			N2.apply(pair.p2, a2);
		} else {
			a1.set(pair.p1);
			a2.set(pair.p2);
		}
	}

	private void adjustConic(AssociatedPairConic pair , ConicGeneral_F64 a1 , ConicGeneral_F64 a2 ) {
		if( shouldNormalize ) {
			N1.apply(pair.p1, a1);
			N2.apply(pair.p2, a2);
		} else {
			a1.set(pair.p1);
			a2.set(pair.p2);
		}
	}

	protected int addPoints2D( List<AssociatedPair> points , DMatrixRMaj A, int rows ) {
		Point2D_F64 f = new Point2D_F64();
		Point2D_F64 s = new Point2D_F64();

		for( int i = 0; i < points.size(); i++ ) {
			AssociatedPair p = points.get(i);

			adjustPoint(p,f,s);

			A.set(rows , 3 , -f.x);
			A.set(rows , 4 , -f.y);
			A.set(rows , 5 , -1);
			A.set(rows , 6 , s.y*f.x);
			A.set(rows , 7 , s.y*f.y);
			A.set(rows , 8 , s.y);
			rows++;
			A.set(rows , 0 , f.x);
			A.set(rows , 1 , f.y);
			A.set(rows , 2 , 1);
			A.set(rows , 6 , -s.x*f.x);
			A.set(rows , 7 , -s.x*f.y);
			A.set(rows , 8 , -s.x);
			rows++;
		}
		return rows;
	}

	protected int addPoints3D(List<AssociatedPair3D> points , DMatrixRMaj A, int rows ) {
		Point3D_F64 f = new Point3D_F64();
		Point3D_F64 s = new Point3D_F64();

		for( int i = 0; i < points.size(); i++ ) {
			AssociatedPair3D p = points.get(i);

			adjustPoint(p,f,s);

			A.set(rows , 3 , -s.z*f.x);
			A.set(rows , 4 , -s.z*f.y);
			A.set(rows , 5 , -s.z*f.z);
			A.set(rows , 6 , s.y*f.x);
			A.set(rows , 7 , s.y*f.y);
			A.set(rows , 8 , s.y*f.z);
			rows++;
			A.set(rows , 0 , s.z*f.x);
			A.set(rows , 1 , s.z*f.y);
			A.set(rows , 2 , s.z*f.z);
			A.set(rows , 6 , -s.x*f.x);
			A.set(rows , 7 , -s.x*f.y);
			A.set(rows , 8 , -s.x*f.z);
			rows++;
		}
		return rows;
	}

	/**
	 * inv(C[1]')*(C[2]')*H - H*invC[1]*C[2] == 0
	 */
	protected int addConics(List<AssociatedPairConic> points , DMatrixRMaj A, int rows ) {

		DMatrix3x3 C1 = new DMatrix3x3();
		DMatrix3x3 C2 = new DMatrix3x3();
		DMatrix3x3 C1_inv = new DMatrix3x3();
		DMatrix3x3 C2_inv = new DMatrix3x3();

		DMatrix3x3 L = new DMatrix3x3();
		DMatrix3x3 R = new DMatrix3x3();

		for( int i = 0; i < points.size(); i++ ) {
			AssociatedPairConic p = points.get(i);
			UtilCurves_F64.convert(p.p1, C1);
			UtilCurves_F64.convert(p.p2, C2);

			CommonOps_DDF3.invert(C1,C1_inv);
			CommonOps_DDF3.invert(C2,C2_inv);

			CommonOps_DDF3.mult(C1_inv,C2,L);
			CommonOps_DDF3.mult(C2_inv,C1,R);



		}

		return rows;
	}
}
