/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d3.epipolar;

import georegression.fitting.MotionTransformPoint;
import georegression.fitting.se.FitSpecialEuclideanOps_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.alg.dense.decomposition.DecompositionFactory;
import org.ejml.alg.dense.decomposition.SingularValueDecomposition;
import org.ejml.alg.dense.mult.MatrixVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Implementation of the EPnP algorithm from [1] for solving the PnP problem when N >= 4.  Given a calibrated
 * camera, n pairs of 2D point observations and the known 3D world coordinates, it solves the for camera's pose..
 * This solution is non-iterative and claims to be much faster and more accurate than the alternatives.  Works
 * for both planar and non-planar configurations.
 * </p>
 *
 * <p>
 * Expresses the n 3D point as a weighted sum of four virtual control points.  Problem then becomes to estimate
 * the coordinates of the control points in the camera referential, which can be done in O(n) time.
 * </p>
 *
 * <p>
 * [1]  Vincent Lepetit, Francesc Moreno-Noguer, and Pascal Fua, "EPnP: An Accurate O(n) Solution to the PnP Problem"
 * Int. J. Comput. Visionm, vol 81, issue 2, 2009
 * </p>
 *
 * @author Peter Abeles
 */
public class PnPLepetitEPnP {

	SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(12,12,false,true,false);

	DenseMatrix64F alphas = new DenseMatrix64F(1,1);
	DenseMatrix64F M = new DenseMatrix64F(12,12);
	DenseMatrix64F MM = new DenseMatrix64F(12,12);
	DenseMatrix64F W = new DenseMatrix64F(12,12);
	
	List<Point3D_F64> nullPts[] = new ArrayList[4];

	List<Point3D_F64> controlWorldPts = new ArrayList<Point3D_F64>();
	
	List<double []> solutions = new ArrayList<double[]>();
	List<Point3D_F64> solutionPts = new ArrayList<Point3D_F64>();

	MotionTransformPoint<Se3_F64, Point3D_F64> motionFit = FitSpecialEuclideanOps_F64.fitPoints3D();

	Se3_F64 solutionMotion = new Se3_F64();
	
	public PnPLepetitEPnP() {
		if( motionFit.getMinimumPoints() > 4 )
			throw new IllegalArgumentException("Crap");
		
		for( int i = 0; i < 4; i++ ) {
			controlWorldPts.add( new Point3D_F64() );
			nullPts[i] = new ArrayList<Point3D_F64>();
			solutions.add( new double[4]);
			solutionPts.add(new Point3D_F64());
			for( int j = 0; j < 4; j++ ) {
				nullPts[i].add( new Point3D_F64());
			}
		}
	}

	public void process( List<Point3D_F64> worldPts , List<Point2D_F64> observed )
	{
		if( worldPts.size() != observed.size() )
			throw new IllegalArgumentException("Must have the same number of observations and world points");
		
		selectControlPoints(worldPts,controlWorldPts);
		computeAlphas(controlWorldPts,alphas,worldPts);
		constructM(observed,alphas,M);
		extractNullPoints(M);

		// compute 4 solutions using the null points
		estimateCase1(solutions.get(0));
		estimateCase2(solutions.get(1));

		// adjust the sign of beta for all the solutions
		for( int i = 0; i < 4; i++ ) {
			double b[] = solutions.get(i);
			for( int j = 0; j < 4; j++ ) {
				b[j] = adjustBetaSign(b[j],nullPts[j]);
			}
		}

		// TODO select best solution

		computeMotion(solutions.get(0));
		solutionMotion.set(motionFit.getMotion());
	}

	/**
	 * Given a set of scale factors for each null space, compute the sum
	 * @param beta
	 */
	private void computeMotion( double beta[] ) {

		for( int i = 0; i < 4; i++ ) {
			solutionPts.get(i).set(0,0,0);
		}
		
		for( int i = 0; i < 4; i++ ) {
			double b = beta[i];

			for( int j = 0; j < 4; j++ ) {
				Point3D_F64 s = solutionPts.get(j);
				Point3D_F64 p = nullPts[i].get(j);
				s.x += b*p.x;
				s.y += b*p.y;
				s.z += b*p.z;
			}
		}

		motionFit.process(controlWorldPts,solutionPts);
	}

	/**
	 * Use the positive depth constraint to determine the sign of beta
	 */
	private double adjustBetaSign( double beta , List<Point3D_F64> controlPts ) {
		if( beta == 0 )
			return 0;

		int N = alphas.numCols;

		int positiveCount = 0;

		for( int i = 0; i < N; i++ ) {
			double z = 0;
			for( int j = 0; j < 4; j++ ) {
				Point3D_F64 c = controlPts.get(j);
				z += alphas.get(j,i)*c.z;
			}

			if( z > 0 )
				positiveCount++;
		}

		if( positiveCount < N/2 )
			beta *= -1;
		
		System.out.println("positive count "+positiveCount+"  beta "+beta);

		return beta;
	}
	
	/**
	 * Selects control points by computing the centroid of the points and then the standard deviation
	 * along each axis.  This is an approximation of what the paper recommends because it is not aligned
	 * along the data's axis.  That can be done, but requires the covariance matrix to be computed.
	 */
	public void selectControlPoints( List<Point3D_F64> worldPts , List<Point3D_F64> controlWorldPts ) {

		Point3D_F64 mean = UtilPoint3D_F64.mean(worldPts);
		
		double stdX = 0;
		double stdY = 0;
		double stdZ = 0;
		
		final int N = worldPts.size();
		for( int i = 0; i < N; i++ ) {
			Point3D_F64 p = worldPts.get(i);
			
			double dx = p.x-mean.x;
			double dy = p.y-mean.y;
			double dz = p.z-mean.z;

			stdX += dx*dx;
			stdY += dy*dy;
			stdZ += dz*dz;
		}
		
		stdX = Math.sqrt(stdX/N);
		stdY = Math.sqrt(stdY/N);
		stdZ = Math.sqrt(stdZ/N);

		controlWorldPts.get(0).set(mean.x + stdX, mean.y, mean.z);
		controlWorldPts.get(1).set(mean.x, mean.y + stdY, mean.z);
		controlWorldPts.get(2).set(mean.x, mean.y, mean.z + stdZ);
		controlWorldPts.get(3).set(mean.x, mean.y, mean.z);
	}

	/**
	 * <p>
	 * Given the control points it computes the 4 weights for each camera point.  This is done by
	 * solving the following linear equation: C*&alpha;=X. where C is the control point matrix,
	 * &alpha; is the 4 by n matrix containing the solution, and X is the camera point matrix.
	 * N is the number of points.
	 * </p>
	 * <p>
	 * C = [ controlPts' ; ones(1,4) ]<br>
	 * X = [ cameraPts' ; ones(1,N) ]
	 * </p>
	 */
	protected static void computeAlphas( List<Point3D_F64> controlWorldPts ,
										 DenseMatrix64F alphas ,
										 List<Point3D_F64> worldPts )
	{
		alphas.reshape(4,worldPts.size(),false);
		
		DenseMatrix64F A = new DenseMatrix64F(4,4);
		for( int i = 0; i < 4; i++ ) {
			Point3D_F64 c = controlWorldPts.get(i);
			A.set(i,0,c.x);
			A.set(i,1,c.y);
			A.set(i,2,c.z);
		}
		for( int i = 0; i < 4; i++ )
			A.set(i,3,1);

		CommonOps.transpose(A);
		
		if( !CommonOps.invert(A) )
			throw new RuntimeException("Control points are singular?!?");
		
		DenseMatrix64F v = new DenseMatrix64F(4,1);
		DenseMatrix64F w = new DenseMatrix64F(4,1);
		
		// set last element to one
		v.set(3,1);

		for( int i = 0; i < worldPts.size(); i++ ) {
			Point3D_F64 p = worldPts.get(i);
			v.data[0] = p.x;
			v.data[1] = p.y;
			v.data[2] = p.z;

			MatrixVectorMult.mult(A,v,w);
			CommonOps.insert(w,alphas,0,i);
		}
	}

	/**
	 * Constructs the linear system which is to be solved.
	 *
	 * sum a_ij*x_j - a_ij*u_i*z_j = 0
	 * sum a_ij*y_j - a_ij*v_i*z_j = 0
	 *
	 * where (x,y,z) is the control point to be solved for.
	 *       (u,v) is the observed normalized point
	 *
	 */
	protected static void constructM( List<Point2D_F64> obsPts ,
									  DenseMatrix64F alphas , DenseMatrix64F M )
	{
		int N = obsPts.size();
		M.reshape(2*N,12,false);
		M.zero();
		
		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p2 = obsPts.get(i);
			
			int row = i*2;
			
			for( int j = 0; j < 4; j++ ) {
				int col = j*3;
				
				double alpha = alphas.get(j,i);
				M.set(row,col,alpha);
				M.set(row,col+2,-alpha*p2.x);
				M.set(row+1,col+1,alpha);
				M.set(row+1,col+2,-alpha*p2.y);
			}
		}
	}

	/**
	 * Computes M'*M and finds the null space.  The 4 eigenvectors with the smallest eigenvalues are found
	 * and the null points extracted from them.
	 */
	protected void extractNullPoints( DenseMatrix64F M )
	{
		CommonOps.multTransA(M,M,MM);

		// eigenvalue decomposition instead?
		if( !svd.decompose(MM) )
			throw new IllegalArgumentException("SVD failed?!?!");

		svd.getW(W);
		DenseMatrix64F V = svd.getV(false);

		SingularOps.descendingOrder(null,false,W,V,false);

		// TODO use eigenvalue decomposition instead
//		System.out.println("singular values");
//		for( int i = 0; i < 12; i++ )
//			System.out.println("     "+W.get(i,i));
		
		// extract null points from the null space
		for( int i = 0; i < 4; i++ ) {
			int column = 11-i;
			for( int j = 0; j < 4; j++ ) {
				Point3D_F64 p = nullPts[i].get(j);
				p.x = V.get(j*3+0,column);
				p.y = V.get(j*3+1,column);
				p.z = V.get(j*3+2,column);
			}
		}
	}

	/**
	 * Examines the distance each point is from the centroid to determine the scaling difference
	 * between world control points and the null points.
	 */
	protected double matchScale( List<Point3D_F64> nullPts ,
								 List<Point3D_F64> controlWorldPts ) {

		Point3D_F64 meanNull = UtilPoint3D_F64.mean(nullPts);
		Point3D_F64 meanWorld = UtilPoint3D_F64.mean(controlWorldPts);

		// compute the ratio of distance between world and null points from the centroid
		double top = 0;
		double bottom = 0;

		for( int i = 0; i < 4; i++ ) {
			Point3D_F64 wi = controlWorldPts.get(i);
			Point3D_F64 ni = nullPts.get(i);

			double dwx = wi.x-meanWorld.x;
			double dwy = wi.y-meanWorld.y;
			double dwz = wi.z-meanWorld.z;

			double dnx = ni.x-meanNull.x;
			double dny = ni.y-meanNull.y;
			double dnz = ni.z-meanNull.z;

			double n2 = dnx*dnx + dny*dny + dnz*dnz;
			double w2 = dwx*dwx + dwy*dwy + dwz*dwz;
			
			System.out.println("ratio = "+Math.sqrt(w2/n2));

			top += w2;
			bottom += n2;
		}

		// compute beta
		return Math.sqrt(top/bottom);
	}

	/**
	 * Simple analytical solution.  Just need to solve for the scale difference in one set
	 * of potential control points.
	 */
	protected void estimateCase1( double betas[] ) {
		Arrays.fill(betas,0);
		betas[0] = matchScale(nullPts[0],controlWorldPts);
	}

	protected void estimateCase2( double betas[] ) {
		Arrays.fill(betas,0);



	}

	protected void estimateCase3( DenseMatrix64F nullspace ) {

	}

	protected void estimateCase4( DenseMatrix64F nullspace ) {

	}

	public Se3_F64 getSolutionMotion() {
		return solutionMotion;
	}
}
