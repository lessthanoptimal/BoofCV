/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.numerics.optimization.LevenbergMarquardt;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// todo add new skew support to non-linear
public class CalibrationPlanarGridZhang98 {

	Zhang98ComputeTargetHomography computeHomography;
	Zhang98CalibrationMatrixFromHomographies computeK;
	RadialDistortionEstimateLinear computeRadial;
	Zhang98DecomposeHomography decomposeH = new Zhang98DecomposeHomography();

	ParametersZhang98 optimized;

	List<Point2D_F64> grid;

	public CalibrationPlanarGridZhang98( CalibrationGridConfig config ,
										 boolean assumeZeroSkew ,
										 int numSkewParam )
	{
		computeHomography = new Zhang98ComputeTargetHomography(config);
		computeK = new Zhang98CalibrationMatrixFromHomographies(assumeZeroSkew);
		computeRadial = new RadialDistortionEstimateLinear(config,numSkewParam);
		grid = config.computeGridPoints();
		optimized = new ParametersZhang98(numSkewParam);
	}

	/**
	 *
	 * @param observations Set of observed grid locations in pixel coordinates.
	 */
	public boolean process(  List<List<Point2D_F64>> observations ) {

		optimized.setNumberOfViews(observations.size());

		// compute initial parameter estimates using linear algebra
		ParametersZhang98 initial =  initialParam(observations);
		if( initial == null )
			return false;

		// perform non-linear optimization to improve results
		if( !optimizedParam(observations,grid,initial,optimized))
			return false;

		return true;
	}

	protected ParametersZhang98 initialParam( List<List<Point2D_F64>> observations )
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

	public static boolean optimizedParam( List<List<Point2D_F64>> observations ,
										  List<Point2D_F64> grid ,
										  ParametersZhang98 initial ,
										  ParametersZhang98 found )
	{
		int numModelParam = initial.size();
		Zhang98OptimizationFunction func = new Zhang98OptimizationFunction(initial.createNew(),grid);

		LevenbergMarquardt<List<Point2D_F64>,Integer> lm =
				new LevenbergMarquardt<List<Point2D_F64>,Integer>(numModelParam,func);

		// todo this is stupid
		List<Integer> state = new ArrayList<Integer>();
		for( int i = 0; i < observations.size(); i++ ) {
			state.add(i);
		}

		double model[] = new double[ initial.size() ];
		initial.convertToParam(model);
		if( !lm.process(model,observations,state) ) {
			throw new RuntimeException("Egads");
		}

		double param[] = lm.getModelParameters();
		found.setFromParam(param);
		return true;
	}

	/**
	 * Converts results fond in the linear algorithms into {@link ParametersZhang98}
	 */
	public static ParametersZhang98 convertIntoZhangParam(List<Se3_F64> motions,
														  DenseMatrix64F K,
														  double[] distort) {
		ParametersZhang98 ret = new ParametersZhang98();

		ret.a = K.get(0,0);
		ret.b = K.get(1,1);
		ret.c = K.get(0,1);
		ret.x0 = K.get(0,2);
		ret.y0 = K.get(1,2);

		ret.distortion = distort;

		ret.views = new ParametersZhang98.View[motions.size()];
		for( int i = 0; i < ret.views.length; i++ ) {
			Se3_F64 m = motions.get(i);

			ParametersZhang98.View v = new ParametersZhang98.View();
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

}
