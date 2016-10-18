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

package boofcv.alg.geo.pose;

import georegression.fitting.MotionTransformPoint;
import georegression.fitting.se.FitSpecialEuclideanOps_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.alg.dense.mult.MatrixVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Implementation of the EPnP algorithm from [1] for solving the PnP problem when N &ge; 5 for the general case
 * and N &ge; 4 for planar data (see note below).  Given a calibrated camera, n pairs of 2D point observations and the known 3D
 * world coordinates, it solves the for camera's pose.. This solution is non-iterative and claims to be much
 * faster and more accurate than the alternatives.  Works for both planar and non-planar configurations.
 * </p>
 *
 * <p>
 * Expresses the N 3D point as a weighted sum of four virtual control points.  Problem then becomes to estimate
 * the coordinates of the control points in the camera referential, which can be done in O(n) time.
 * </p>
 *
 * <p>
 * After estimating the control points the solution can be refined using Gauss Newton non-linear
 * optimization.  The objective function being optimizes
 * reduces the difference between world and camera control point distances by adjusting
 * the values of beta.  Optimization is very fast, but can degrade accuracy if over optimized.
 * See warning below.  To turn on non-linear optimization set {@link #setNumIterations(int)} to
 * a positive number.
 * </p>
 *
 * <p>
 * After experimentation there doesn't seem to be any universally best way to choose the control
 * point distribution.  To allow tuning for specific problems the 'magic number' has been provided.
 * Larger values increase the control point distribution's size. In general smaller numbers appear
 * to be better for noisier data, but can degrade results if too small.
 * </p>
 *
 * <p>
 * MINIMUM POINTS: According to the original paper [1] only 4 points are needed for the general case.
 * In practice it produces very poor results on average.  The matlab code provided by the original author
 * also exhibits instability in the minimum case.  The article also does not show results for the minimum
 * case, making it hard to judge what should be expected.  However, I'm not ruling out there being a bug
 * in relinearization.  See that code for comments.  Correspondence with the original author indicates that
 * he did not expect results from relinearization to be as accurate as the other cases.
 * </p>
 *
 * <p>
 * NOTES: This implementation deviates in some minor ways from what was described in the paper.  However, their
 * own example code (Matlab and C++) are mutually different in significant ways too.  See how solutions are scored,
 * linear systems are solved, and how world control points are computed.  How control points are computed here
 * is inspired from their C++ example (technique used in their matlab example has some stability issues), but
 * there is probably room for more improvement.
 * </p>
 *
 * <p>
 * WARNING: Setting the number of optimization iterations too high can actually degrade accuracy.  The
 * objective function being minimized is not observation residuals.  Locally it appears
 * to be a good approximation, but can diverge and actually produce worse results. Because
 * of this behavior, more advanced optimization routines are unnecessary and counter productive.
 * </p>
 *
 * <p>
 * [1]  Vincent Lepetit, Francesc Moreno-Noguer, and Pascal Fua, "EPnP: An Accurate O(n) Solution to the PnP Problem"
 * Int. J. Comput. Visionm, vol 81, issue 2, 2009
 * </p>
 *
 * @author Peter Abeles
 */
// TODO change so that it returns a list of N solutions and let other algorithm select the best?  Re-read the paper
public class PnPLepetitEPnP {

	// used to solve various linear problems
	private SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(12, 12, false, true, false);
	private LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(6, 4);
	private LinearSolver<DenseMatrix64F> solverPinv = LinearSolverFactory.pseudoInverse(true);

	// weighting factor to go from control point into world coordinate
	protected DenseMatrix64F alphas = new DenseMatrix64F(1,1);
	private DenseMatrix64F M = new DenseMatrix64F(12,12);
	private DenseMatrix64F MM = new DenseMatrix64F(12,12);

	// linear constraint matrix
	protected DenseMatrix64F L_full = new DenseMatrix64F(6,10);
	private DenseMatrix64F L = new DenseMatrix64F(6,6);
	// distance of world control points from each other
	protected DenseMatrix64F y = new DenseMatrix64F(6,1);
	// solution for betas
	private DenseMatrix64F x = new DenseMatrix64F(6,1);

	// how many controls points 4 for general case and 3 for planar
	protected int numControl;

	// control points extracted from the null space of M'M
	protected List<Point3D_F64> nullPts[] = new ArrayList[4];

	// control points in world coordinate frame
	protected FastQueue<Point3D_F64> controlWorldPts = new FastQueue<>(4, Point3D_F64.class, true);

	// list of found solutions
	private List<double []> solutions = new ArrayList<>();
	protected FastQueue<Point3D_F64> solutionPts = new FastQueue<>(4, Point3D_F64.class, true);

	// estimates rigid body motion between two associated sets of points
	private MotionTransformPoint<Se3_F64, Point3D_F64> motionFit = FitSpecialEuclideanOps_F64.fitPoints3D();

	// mean location of world points
	private Point3D_F64 meanWorldPts = new Point3D_F64();

	// number of iterations it will perform
	private int numIterations;

	// adjusts world control point distribution
	private double magicNumber;

	// handles the hard case #4
	Relinearlize relinearizeBeta = new Relinearlize();

	// declaring data for local use inside a function
	// in general its good to avoid declaring and destroying massive amounts of data in Java
	// this is probably going too far though
	private List<Point3D_F64> tempPts0 = new ArrayList<>(); // 4 points stored in it
	DenseMatrix64F A_temp = new DenseMatrix64F(1,1);
	DenseMatrix64F v_temp = new DenseMatrix64F(3,1);
	DenseMatrix64F w_temp = new DenseMatrix64F(1,1);

	/**
	 * Constructor which uses the default magic number
	 */
	public PnPLepetitEPnP() {
		this(0.1);
	}

	/**
	 * Constructor which allows configuration of the magic number.
	 *
	 * @param magicNumber Magic number changes distribution of world control points.
	 *                    Values less than one seem to work best. Try 0.1
	 */
	public PnPLepetitEPnP( double magicNumber ) {

		this.magicNumber = magicNumber;

		// just a sanity check
		if( motionFit.getMinimumPoints() > 4 )
			throw new IllegalArgumentException("Crap");

		for( int i = 0; i < 4; i++ ) {
			tempPts0.add( new Point3D_F64());
			nullPts[i] = new ArrayList<>();
			solutions.add( new double[4]);
			for( int j = 0; j < 4; j++ ) {
				nullPts[i].add( new Point3D_F64());
			}
		}
	}

	/**
	 * Used to turn on and off non-linear optimization.  To turn on set to a positive number.
	 * See warning in class description about setting the number of iterations too high.
	 *
	 * @param numIterations  Number of iterations.  Try 10.
	 */
	public void setNumIterations(int numIterations) {
		this.numIterations = numIterations;
	}

	/**
	 * Compute camera motion given a set of features with observations and 3D locations
	 *
	 * @param worldPts Known location of features in 3D world coordinates
	 * @param observed Observed location of features in normalized camera coordinates
	 * @param solutionModel Output: Storage for the found solution.
	 */
	public void process( List<Point3D_F64> worldPts , List<Point2D_F64> observed , Se3_F64 solutionModel )
	{
		if( worldPts.size() != observed.size() )
			throw new IllegalArgumentException("Must have the same number of observations and world points");

		// select world control points using the points statistics
		selectWorldControlPoints(worldPts, controlWorldPts);
		// compute barycentric coordinates for the world control points
		computeBarycentricCoordinates(controlWorldPts, alphas, worldPts );
		// create the linear system whose null space will contain the camera control points
		constructM(observed, alphas, M);
		// the camera points are a linear combination of these null points
		extractNullPoints(M);

		// compute the full constraint matrix, the others are extracted from this
		if( numControl == 4 ) {
			L_full.reshape(6, 10);
			y.reshape(6,1);
			UtilLepetitEPnP.constraintMatrix6x10(L_full,y,controlWorldPts,nullPts);

			// compute 4 solutions using the null points
			estimateCase1(solutions.get(0));
			estimateCase2(solutions.get(1));
			estimateCase3(solutions.get(2));
			// results are bad in general, so skip unless needed
			// always considering this case seems to hurt runtime speed a little bit
			if( worldPts.size() == 4 )
				estimateCase4(solutions.get(3));
		} else {
			L_full.reshape(3, 6);
			y.reshape(3,1);
			UtilLepetitEPnP.constraintMatrix3x6(L_full, y, controlWorldPts, nullPts);

			estimateCase1(solutions.get(0));
			estimateCase2(solutions.get(1));
			if( worldPts.size() == 3 )
				estimateCase3_planar(solutions.get(2));
		}

		computeResultFromBest(solutionModel);
	}

	/**
	 * Selects the best motion hypothesis based on the actual observations and optionally
	 * optimizes the solution.
	 */
	private void computeResultFromBest( Se3_F64 solutionModel ) {
		double bestScore = Double.MAX_VALUE;
		int bestSolution=-1;
		for( int i = 0; i < numControl; i++ ) {
			double score = score(solutions.get(i));
			if( score < bestScore ) {
				bestScore = score;
				bestSolution = i;
			}
//			System.out.println(i+" score "+score);
		}

		double []solution = solutions.get(bestSolution);
		if( numIterations > 0 ) {
			gaussNewton(solution);
		}

		UtilLepetitEPnP.computeCameraControl(solution,nullPts,solutionPts,numControl);
		motionFit.process(controlWorldPts.toList(), solutionPts.toList());

		solutionModel.set(motionFit.getTransformSrcToDst());
	}

	/**
	 * Score a solution based on distance between control points.  Closer the camera
	 * control points are from the world control points the better the score.  This is
	 * similar to how optimization score works and not the way recommended in the original
	 * paper.
	 */
	private double score(double betas[]) {
		UtilLepetitEPnP.computeCameraControl(betas,nullPts, solutionPts,numControl);

		int index = 0;
		double score = 0;
		for( int i = 0; i < numControl; i++ ) {
			Point3D_F64 si = solutionPts.get(i);
			Point3D_F64 wi = controlWorldPts.get(i);

			for( int j = i+1; j < numControl; j++ , index++) {
				double ds = si.distance(solutionPts.get(j));
				double dw = wi.distance(controlWorldPts.get(j));

				score += (ds-dw)*(ds-dw);
			}
		}
		return score;
	}


	/**
	 * Selects control points along the data's axis and the data's centroid.  If the data is determined
	 * to be planar then only 3 control points are selected.
	 *
	 * The data's axis is determined by computing the covariance matrix then performing SVD.  The axis
	 * is contained along the
	 */
	public void selectWorldControlPoints(List<Point3D_F64> worldPts, FastQueue<Point3D_F64> controlWorldPts) {

		UtilPoint3D_F64.mean(worldPts,meanWorldPts);

		// covariance matrix elements, summed up here for speed
		double c11=0,c12=0,c13=0,c22=0,c23=0,c33=0;

		final int N = worldPts.size();
		for( int i = 0; i < N; i++ ) {
			Point3D_F64 p = worldPts.get(i);

			double dx = p.x- meanWorldPts.x;
			double dy = p.y- meanWorldPts.y;
			double dz = p.z- meanWorldPts.z;

			c11 += dx*dx;c12 += dx*dy;c13 += dx*dz;
			c22 += dy*dy;c23 += dy*dz;
			c33 += dz*dz;
		}
		c11/=N;c12/=N;c13/=N;c22/=N;c23/=N;c33/=N;

		DenseMatrix64F covar = new DenseMatrix64F(3,3,true,c11,c12,c13,c12,c22,c23,c13,c23,c33);

		// find the data's orientation and check to see if it is planar
		svd.decompose(covar);
		double []singularValues = svd.getSingularValues();
		DenseMatrix64F V = svd.getV(null,false);

		SingularOps.descendingOrder(null,false,singularValues,3,V,false);

		// planar check
		if( singularValues[0]<singularValues[2]*1e13 ) {
			numControl = 4;
		} else {
			numControl = 3;
		}

		// put control points along the data's major axises
		controlWorldPts.reset();
		for( int i = 0; i < numControl-1; i++ ) {
			double m = Math.sqrt(singularValues[1])*magicNumber;

			double vx = V.unsafe_get(0, i)*m;
			double vy = V.unsafe_get(1, i)*m;
			double vz = V.unsafe_get(2, i)*m;

			controlWorldPts.grow().set(meanWorldPts.x + vx, meanWorldPts.y + vy, meanWorldPts.z + vz);
		}
		// set a control point to be the centroid
		controlWorldPts.grow().set(meanWorldPts.x, meanWorldPts.y, meanWorldPts.z);
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
	protected void computeBarycentricCoordinates(FastQueue<Point3D_F64> controlWorldPts,
												 DenseMatrix64F alphas,
												 List<Point3D_F64> worldPts )
	{
		alphas.reshape(worldPts.size(),numControl,false);
		v_temp.reshape(3,1);
		A_temp.reshape(3, numControl - 1);

		for( int i = 0; i < numControl-1; i++ ) {
			Point3D_F64 c = controlWorldPts.get(i);
			A_temp.set(0, i, c.x - meanWorldPts.x);
			A_temp.set(1, i, c.y - meanWorldPts.y);
			A_temp.set(2, i, c.z - meanWorldPts.z);
		}

		// invert the matrix
		solverPinv.setA(A_temp);
		A_temp.reshape(A_temp.numCols, A_temp.numRows);
		solverPinv.invert(A_temp);

		w_temp.reshape(numControl - 1, 1);

		for( int i = 0; i < worldPts.size(); i++ ) {
			Point3D_F64 p = worldPts.get(i);
			v_temp.data[0] = p.x- meanWorldPts.x;
			v_temp.data[1] = p.y- meanWorldPts.y;
			v_temp.data[2] = p.z- meanWorldPts.z;

			MatrixVectorMult.mult(A_temp, v_temp, w_temp);

			int rowIndex = alphas.numCols*i;
			for( int j = 0; j < numControl-1; j++ )
				alphas.data[rowIndex++] = w_temp.data[j];

			if( numControl == 4 )
				alphas.data[rowIndex] = 1 - w_temp.data[0] - w_temp.data[1] - w_temp.data[2];
			else
				alphas.data[rowIndex] = 1 - w_temp.data[0] - w_temp.data[1];
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
	protected static void constructM(List<Point2D_F64> obsPts,
									 DenseMatrix64F alphas, DenseMatrix64F M)
	{
		int N = obsPts.size();
		M.reshape(3*alphas.numCols,2*N,false);

		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p2 = obsPts.get(i);

			int row = i*2;

			for( int j = 0; j < alphas.numCols; j++ ) {
				int col = j*3;

				double alpha = alphas.unsafe_get(i, j);
				M.unsafe_set(col, row, alpha);
				M.unsafe_set(col + 1, row, 0);
				M.unsafe_set(col + 2, row, -alpha * p2.x);
				M.unsafe_set(col    , row + 1, 0);
				M.unsafe_set(col + 1, row + 1, alpha);
				M.unsafe_set(col + 2, row + 1, -alpha * p2.y);
			}
		}
	}

	/**
	 * Computes M'*M and finds the null space.  The 4 eigenvectors with the smallest eigenvalues are found
	 * and the null points extracted from them.
	 */
	protected void extractNullPoints( DenseMatrix64F M )
	{
		// compute MM and find its null space
		MM.reshape(M.numRows,M.numRows,false);
		CommonOps.multTransB(M, M, MM);

		if( !svd.decompose(MM) )
			throw new IllegalArgumentException("SVD failed?!?!");

		double []singularValues = svd.getSingularValues();
		DenseMatrix64F V = svd.getV(null,false);

		SingularOps.descendingOrder(null,false,singularValues,3,V,false);

		// extract null points from the null space
		for( int i = 0; i < numControl; i++ ) {
			int column = M.numRows-1-i;
//			nullPts[i].clear();
			for( int j = 0; j < numControl; j++ ) {
				Point3D_F64 p = nullPts[i].get(j);
//				Point3D_F64 p = new Point3D_F64();
				p.x = V.get(j*3+0,column);
				p.y = V.get(j*3+1,column);
				p.z = V.get(j*3+2,column);
//				nullPts[i].add(p);
			}
		}
	}

	/**
	 * Examines the distance each point is from the centroid to determine the scaling difference
	 * between world control points and the null points.
	 */
	protected double matchScale( List<Point3D_F64> nullPts ,
								 FastQueue<Point3D_F64> controlWorldPts ) {

		Point3D_F64 meanNull = UtilPoint3D_F64.mean(nullPts,numControl,null);
		Point3D_F64 meanWorld = UtilPoint3D_F64.mean(controlWorldPts.toList(),numControl,null);

		// compute the ratio of distance between world and null points from the centroid
		double top = 0;
		double bottom = 0;

		for( int i = 0; i < numControl; i++ ) {
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

			top += w2;
			bottom += n2;
		}

		// compute beta
		return Math.sqrt(top/bottom);
	}

	/**
	 * Use the positive depth constraint to determine the sign of beta
	 */
	private double adjustBetaSign( double beta , List<Point3D_F64> nullPts ) {
		if( beta == 0 )
			return 0;

		int N = alphas.numRows;

		int positiveCount = 0;

		for( int i = 0; i < N; i++ ) {
			double z = 0;
			for( int j = 0; j < numControl; j++ ) {
				Point3D_F64 c = nullPts.get(j);
				z += alphas.get(i,j)*c.z;
			}

			if( z > 0 )
				positiveCount++;
		}

		if( positiveCount < N/2 )
			beta *= -1;

		return beta;
	}

	/**
	 * Given the set of betas it computes a new set of control points and adjust sthe scale
	 * using the {@llin #matchScale} function.
	 */
	private void refine( double betas[] ) {
		for( int i = 0; i < numControl; i++ ) {
			double x=0,y=0,z=0;

			for( int j = 0; j < numControl; j++ ) {
				Point3D_F64 p = nullPts[j].get(i);
				x += betas[j]*p.x;
				y += betas[j]*p.y;
				z += betas[j]*p.z;
			}

			tempPts0.get(i).set(x,y,z);
		}

		double adj = matchScale(tempPts0,controlWorldPts);
		adj = adjustBetaSign(adj,tempPts0);

		for( int i = 0; i < 4; i++ ) {
			betas[i] *= adj;
		}
	}

	/**
	 * Simple analytical solution.  Just need to solve for the scale difference in one set
	 * of potential control points.
	 */
	protected void estimateCase1( double betas[] ) {
		betas[0] = matchScale(nullPts[0], controlWorldPts);
		betas[0] = adjustBetaSign(betas[0],nullPts[0]);
		betas[1] = 0; betas[2] = 0; betas[3] = 0;
	}

	protected void estimateCase2( double betas[] ) {

		x.reshape(3,1,false);
		if( numControl == 4 ) {
			L.reshape(6,3,false);
			UtilLepetitEPnP.constraintMatrix6x3(L_full,L);
		} else {
			L.reshape(3,3,false);
			UtilLepetitEPnP.constraintMatrix3x3(L_full,L);
		}

		if( !solver.setA(L) )
			throw new RuntimeException("Oh crap");

		solver.solve(y,x);

		betas[0] = Math.sqrt(Math.abs(x.get(0)));
		betas[1] = Math.sqrt(Math.abs(x.get(2)));
		betas[1] *= Math.signum(x.get(0))*Math.signum(x.get(1));
		betas[2] = 0; betas[3] = 0;

		refine(betas);
	}

	protected void estimateCase3( double betas[] ) {
		Arrays.fill(betas, 0);

		x.reshape(6,1,false);
		L.reshape(6,6,false);
		UtilLepetitEPnP.constraintMatrix6x6(L_full, L);

		if( !solver.setA(L) )
			throw new RuntimeException("Oh crap");

		solver.solve(y,x);

		betas[0] = Math.sqrt(Math.abs(x.get(0)));
		betas[1] = Math.sqrt(Math.abs(x.get(3)));
		betas[2] = Math.sqrt(Math.abs(x.get(5)));
		betas[1] *= Math.signum(x.get(0))*Math.signum(x.get(1));
		betas[2] *= Math.signum(x.get(0))*Math.signum(x.get(2));
		betas[3] = 0;

		refine(betas);
	}

	/**
	 * If the data is planar use relinearize to estimate betas
	 */
	protected void estimateCase3_planar( double betas[] ) {
		relinearizeBeta.setNumberControl(3);
		relinearizeBeta.process(L_full,y,betas);

		refine(betas);
	}

	protected void estimateCase4( double betas[] ) {

		relinearizeBeta.setNumberControl(4);
		relinearizeBeta.process(L_full,y,betas);

		refine(betas);
	}

	/**
	 * Optimize beta values using Gauss Newton.
	 *
	 * @param betas Beta values being optimized.
	 */
	private void gaussNewton( double betas[] ) {

		A_temp.reshape(L_full.numRows, numControl);
		v_temp.reshape(L_full.numRows, 1);
		x.reshape(numControl,1,false);

		// don't check numControl inside in hope that the JVM can optimize the code better
		if( numControl == 4 ) {
			for( int i = 0; i < numIterations; i++ ) {
				UtilLepetitEPnP.jacobian_Control4(L_full,betas, A_temp);
				UtilLepetitEPnP.residuals_Control4(L_full,y,betas,v_temp.data);

				if( !solver.setA(A_temp) )
					return;

				solver.solve(v_temp,x);

				for( int j = 0; j < numControl; j++ ) {
					betas[j] -= x.data[j];
				}
			}
		} else {
			for( int i = 0; i < numIterations; i++ ) {
				UtilLepetitEPnP.jacobian_Control3(L_full,betas, A_temp);
				UtilLepetitEPnP.residuals_Control3(L_full,y,betas,v_temp.data);

				if( !solver.setA(A_temp) )
					return;

				solver.solve(v_temp,x);

				for( int j = 0; j < numControl; j++ ) {
					betas[j] -= x.data[j];
				}
			}
		}
	}

	/**
	 * Returns the minimum number of points required to make an estimate.  Technically
	 * it is 4 for general and 3 for planar.  The minimum number of point cases
	 * seem to be a bit unstable in some situations.  minimum + 1 or more is stable.
	 *
	 * @return minimum number of points
	 */
	public int getMinPoints () {
		return 5;
	}
}
