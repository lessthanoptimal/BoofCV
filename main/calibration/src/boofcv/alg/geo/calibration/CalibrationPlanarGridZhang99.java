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

package boofcv.alg.geo.calibration;

import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedLeastSquares;
import boofcv.numerics.optimization.impl.UtilOptimize;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Full implementation of the Zhang99 camera calibration algorithm using planar calibration targets.  First
 * linear approximations of camera parameters are computed, which are then refined using non-linear estimation.
 * </p>
 *
 * <p>
 * When processing the results be sure to take in account the coordinate system being left or right handed.  Calibration
 * works just fine with either coordinate system, but most 3D geometric algorithms assume a right handed coordinate
 * system while most images are left handed.
 * </p>
 *
 * <p>
 * [1] Zhengyou Zhang, "Flexible Camera Calibration By Viewing a Plane From Unknown Orientations,",
 * International Conference on Computer Vision (ICCV'99), Corfu, Greece, pages 666-673, September 1999.
 * </p>
 *
 * @author Peter Abeles
 */
public class CalibrationPlanarGridZhang99 {

	// estimation algorithms
	private Zhang99ComputeTargetHomography computeHomography;
	private Zhang99CalibrationMatrixFromHomographies computeK;
	private RadialDistortionEstimateLinear computeRadial;
	private Zhang99DecomposeHomography decomposeH = new Zhang99DecomposeHomography();

	// contains found parameters
	private ParametersZhang99 optimized;

	// description of the calibration target with point locations
	private PlanarCalibrationTarget target;

	// if true the intrinsic calibration matrix will have the skew parameter set to zero
	private boolean assumeZeroSkew;

	// optimization algorithm
	private UnconstrainedLeastSquares optimizer;

	/**
	 * Configures calibration process.
	 *
	 * @param target Description of the known calibration target
	 * @param assumeZeroSkew Should it assumed the camera has zero skew. Typically true.
	 * @param numRadialParam Number of radial distortion parameters to consider.  Typically 0,1,2.
	 */
	public CalibrationPlanarGridZhang99(PlanarCalibrationTarget target,
										boolean assumeZeroSkew,
										int numRadialParam)
	{
		computeHomography = new Zhang99ComputeTargetHomography(target);
		computeK = new Zhang99CalibrationMatrixFromHomographies(assumeZeroSkew);
		computeRadial = new RadialDistortionEstimateLinear(target,numRadialParam);
		this.target = target;
		optimized = new ParametersZhang99(numRadialParam);
		this.assumeZeroSkew = assumeZeroSkew;
	}

	/**
	 * Processes observed calibration point coordinates and computes camera intrinsic and extrinsic
	 * parameters.
	 *
	 * @param observations Set of observed grid locations in pixel coordinates.
	 * @return true if successful and false if it failed
	 */
	public boolean process( List<List<Point2D_F64>> observations ) {

		optimized.setNumberOfViews(observations.size());

		// compute initial parameter estimates using linear algebra
		ParametersZhang99 initial =  initialParam(observations);
		if( initial == null )
			return false;

		// perform non-linear optimization to improve results
		if( !optimizedParam(observations,target.points,assumeZeroSkew,initial,optimized,optimizer))
			return false;

		return true;
	}

	/**
	 * Find an initial estimate for calibration parameters using linear techniques.
	 */
	protected ParametersZhang99 initialParam( List<List<Point2D_F64>> observations )
	{
		List<DenseMatrix64F> homographies = new ArrayList<DenseMatrix64F>();
		List<Se3_F64> motions = new ArrayList<Se3_F64>();

		for( List<Point2D_F64> obs : observations ) {
			if( !computeHomography.computeHomography(obs) )
				return null;

			DenseMatrix64F H = computeHomography.getHomography();

			homographies.add(H);
		}

		computeK.process(homographies);

		DenseMatrix64F K = computeK.getCalibrationMatrix();

		decomposeH.setCalibrationMatrix(K);
		for( DenseMatrix64F H : homographies ) {
			motions.add(decomposeH.decompose(H));
		}

		computeRadial.process(K,homographies,observations);

		double distort[] = computeRadial.getParameters();

		return convertIntoZhangParam(motions, K, distort);
	}

	/**
	 * Use non-linear optimization to improve the parameter estimates
	 *
	 * @param observations Observations of calibration points in each image
	 * @param grid Location of calibration points on calibration target
	 * @param assumeZeroSkew Assume zero skew when computing calibration parameters
	 * @param initial Initial estimate of calibration parameters.
	 * @param found The refined calibration parameters.
	 * @param optimizer Algorithm used to optimize parameters
	 */
	public static boolean optimizedParam( List<List<Point2D_F64>> observations ,
										  List<Point2D_F64> grid ,
										  boolean assumeZeroSkew ,
										  ParametersZhang99 initial ,
										  ParametersZhang99 found ,
										  UnconstrainedLeastSquares optimizer )
	{
		Zhang99OptimizationFunction func = new Zhang99OptimizationFunction(
				initial.createNew(),assumeZeroSkew , grid,observations);

		if( optimizer == null ) {
//			optimizer = FactoryOptimization.leastSquaresTrustRegion(1,
//					RegionStepType.DOG_LEG_FTF,true);
			optimizer = FactoryOptimization.leastSquaresLM(1e-3,true);
//			optimizer = FactoryOptimization.leastSquareLevenberg(1e-3);
		}

		double model[] = new double[ initial.size() ];
		initial.convertToParam(assumeZeroSkew,model);

		optimizer.setFunction(func,null);
		optimizer.initialize(model,1e-10,1e-25*observations.size());

		if( !UtilOptimize.process(optimizer,500) ) {
			return false;
		}

		double param[] = optimizer.getParameters();
		found.setFromParam(assumeZeroSkew,param);

		return true;
	}

	/**
	 * Converts results fond in the linear algorithms into {@link ParametersZhang99}
	 */
	public static ParametersZhang99 convertIntoZhangParam(List<Se3_F64> motions,
														  DenseMatrix64F K,
														  double[] distort) {
		ParametersZhang99 ret = new ParametersZhang99();

		ret.a = K.get(0,0);
		ret.b = K.get(1,1);
		ret.c = K.get(0,1);
		ret.x0 = K.get(0,2);
		ret.y0 = K.get(1,2);

		ret.distortion = distort;

		ret.views = new ParametersZhang99.View[motions.size()];
		for( int i = 0; i < ret.views.length; i++ ) {
			Se3_F64 m = motions.get(i);

			ParametersZhang99.View v = new ParametersZhang99.View();
			v.T = m.getT();
			RotationMatrixGenerator.matrixToRodrigues(m.getR(), v.rotation);

			ret.views[i] = v;
		}

		return ret;
	}

	/**
	 * Applies radial distortion to the point.
	 *
	 * @param pt point in calibrated pixel coordinates
	 * @param radial radial distortion parameters
	 */
	public static void applyDistortion(Point2D_F64 pt, double[] radial)
	{
		double a = 0;
		double r2 = pt.x*pt.x + pt.y*pt.y;
		double r = r2;
		for( int i = 0; i < radial.length; i++ ) {
			a += radial[i]*r;
			r *= r2;
		}

		pt.x += pt.x*a;
		pt.y += pt.y*a;
	}

	/**
	 * Specify which optimization algorithm to use
	 */
	public void setOptimizer(UnconstrainedLeastSquares optimizer) {
		this.optimizer = optimizer;
	}

	public ParametersZhang99 getOptimized() {
		return optimized;
	}
}
