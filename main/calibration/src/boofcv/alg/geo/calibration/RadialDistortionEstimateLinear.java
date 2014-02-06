/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;

import java.util.List;

/**
 * <p>
 * Estimates radial lens distortion by solving a linear equation with observed features on a calibration grid.
 * Typically used as an initial estimate for use in non-linear optimization. An arbitrary number of distortion
 * parameters can be solved for, but typically only two are found.  The estimated is computed using a
 * known intrinsic camera matrix,and homographies relating grid to camera coordinates.
 * Based upon the description found in Section 3.3 in [1].
 * </p>
 *
 * <p>
 * Radial distortion is modeled using the following equations:<br>
 * u' = u + (u - u<sub>0</sub>)*[ k<sub>1</sub>(x<sup>2</sup> + x<sup>2</sup>) +k<sub>2</sub>(x<sup>2</sup> + x<sup>2</sup>)<sup>2</sup>]<br>
 * v' = v + (v - v<sub>0</sub>)*[ k<sub>1</sub>(x<sup>2</sup> + x<sup>2</sup>) +k<sub>2</sub>(x<sup>2</sup> + x<sup>2</sup>)<sup>2</sup>]<br>
 * where (u',v') is the observed distortion in pixel coordinates, (u<sub>0</sub>,v<sub>0</sub>) is the image center in
 * pixel coordinates, (x,y) is the predicted distortion free calibrated coordinates.
 * </p>
 *
 * <p>
 * The algorithm works by solving the system of equations below:<br>
 * <br>
 * [ (u-u<sub>0</sub>)*r<sup>2</sup> ,  (u-u<sub>0</sub>)*r<sup>4</sup> ][k<sub>1</sub>] = [u'-u]<br>
 * [ (v-v<sub>0</sub>)*r<sup>2</sup> ,  (v-v<sub>0</sub>)*r<sup>4</sup> ][k<sub>2</sub>] = [v'-v]<br>
 * <br>
 * where r<sup>2</sup> = (x<sup>2</sup>+y<sup>2</sup>).
 * </p>
 *
 * <p>
 * [1] Zhengyou Zhang, "Flexible Camera Calibration By Viewing a Plane From Unknown Orientations," 1999
 * </p>
 * @author Peter Abeles
 */
public class RadialDistortionEstimateLinear {
	// matrices in the linear equations
	private DenseMatrix64F A = new DenseMatrix64F(1,1);
	private DenseMatrix64F B = new DenseMatrix64F(1,1);

	// where the results are stored
	private DenseMatrix64F X;

	// linear solver
	private LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(0, 0);

	// location of grid coordinates in the world frame.
	// the z-axis is assumed to be zero
	private List<Point2D_F64> worldPoints;


	/**
	 * Creates a estimator for the specified number of distortion parameters.
	 *
	 * @param gridDesc Description of calibration grid
	 * @param numParam Number of radial distortion parameters. Two is a good number.
	 */
	public RadialDistortionEstimateLinear(PlanarCalibrationTarget gridDesc, int numParam) {
		worldPoints = gridDesc.points;
		X = new DenseMatrix64F(numParam,1);
	}

	/**
	 * Computes radial distortion using a linear method.
	 *
	 * @param cameraCalibration Camera calibration matrix.  Not modified.
	 * @param observations Observations of calibration grid. Not modified.
	 */
	public void process( DenseMatrix64F cameraCalibration ,
						 List<DenseMatrix64F> homographies ,
						 List<List<Point2D_F64>> observations )
	{
		init( observations.size() );

		setupA_and_B(cameraCalibration,homographies,observations);

		if( !solver.setA(A) )
			throw new RuntimeException("Solver had problems");

		solver.solve(B,X);
	}

	/**
	 * Declares and sets up data structures
	 *
	 * @param numGridObservations number of times that the calibration grid was observed.
	 */
	private void init( int numGridObservations )
	{
		A.reshape(2*numGridObservations*worldPoints.size(),X.numRows,false);
		B.reshape(A.numRows,1,false);
	}

	private void setupA_and_B( DenseMatrix64F K ,
							   List<DenseMatrix64F> homographies ,
							   List<List<Point2D_F64>> observations) {

		final int N = observations.size();

		// image center in pixels
		double u0 = K.get(0,2); // image center x-coordinate
		double v0 = K.get(1,2); // image center y-coordinate

		// projected predicted
		Point2D_F64 projCalibrated = new Point2D_F64();
		Point2D_F64 projPixel = new Point2D_F64();

		int pointIndex = 0;
		for( int indexObs = 0; indexObs < N; indexObs++ ) {
			DenseMatrix64F H = homographies.get(indexObs);
			List<Point2D_F64> obs = observations.get(indexObs);

			for( int gridIndex = 0; gridIndex < worldPoints.size(); gridIndex++ ) {
				// location of grid point in world coordinate (x,y,0)  assume z=0
				Point2D_F64 gridPt = worldPoints.get(gridIndex);

				// compute the predicted location of the point in calibrated units
				GeometryMath_F64.mult(H,gridPt, projCalibrated);

				// compute the predicted location in (uncalibrated) pixels
				GeometryMath_F64.mult(K,projCalibrated,projPixel);

				// construct the matrices
				double r2 = projCalibrated.x*projCalibrated.x + projCalibrated.y*projCalibrated.y;

				double a = 1.0;
				for( int i = 0; i < X.numRows; i++ ) {
					a *= r2;

					A.set(pointIndex*2+0,i,(projPixel.x-u0)*a);
					A.set(pointIndex*2+1,i,(projPixel.y-v0)*a);
				}

				// observed location
				Point2D_F64 obsPixel = obs.get(gridIndex);

				B.set(pointIndex*2+0,0,obsPixel.x-projPixel.x);
				B.set(pointIndex*2+1,0,obsPixel.y-projPixel.y);

				pointIndex++;
			}
		}
	}

	/**
	 * Returns radial distortion parameters.
	 *
	 * @return radial distortion parameters.
	 */
	public double[] getParameters() {
		return X.data;
	}

}
