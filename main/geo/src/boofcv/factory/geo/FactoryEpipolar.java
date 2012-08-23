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

package boofcv.factory.geo;

import boofcv.abst.geo.*;
import boofcv.abst.geo.bundle.BundleAdjustmentCalibratedDense;
import boofcv.abst.geo.f.LeastSquaresFundamental;
import boofcv.abst.geo.f.WrapEssentialNister5;
import boofcv.abst.geo.f.WrapFundamentalLinear;
import boofcv.abst.geo.fitting.GenerateEpipolarMatrix;
import boofcv.abst.geo.h.LeastSquaresHomography;
import boofcv.abst.geo.h.WrapHomographyLinear;
import boofcv.abst.geo.pose.LeastSquaresPose;
import boofcv.abst.geo.pose.WrapPnPLepetitEPnP;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.ModelObservationResidualN;
import boofcv.alg.geo.h.HomographyLinear4;
import boofcv.alg.geo.h.HomographyResidualSampson;
import boofcv.alg.geo.h.HomographyResidualTransfer;
import boofcv.alg.geo.pose.PnPLepetitEPnP;
import boofcv.alg.geo.pose.PoseRodriguesCodec;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import org.ejml.data.DenseMatrix64F;

/**
 * @author Peter Abeles
 */
public class FactoryEpipolar {

	/**
	 * Creates bundle adjustment for a camera with a know and fixed intrinsic calibration
	 *
	 * @param tol Convergence tolerance.  Try 1e-8
	 * @param maxIterations Maximum number of iterations. Try 200 or more
	 * @return Bundle Adjustment
	 */
	public static BundleAdjustmentCalibrated bundleCalibrated(double tol , int maxIterations) {
		return new BundleAdjustmentCalibratedDense(tol,maxIterations);
	}

	/**
	 * Returns an algorithm for estimating a homography matrix given a set of
	 * {@link AssociatedPair}.
	 *
	 * @return Fundamental algorithm.
	 */
	public static EpipolarMatrixEstimator computeHomography( boolean normalize ) {
		HomographyLinear4 alg = new HomographyLinear4(normalize);
		return new WrapHomographyLinear(alg);
	}

	/**
	 * Creates a non-linear optimizer for refining estimates of homography matrices.
	 *
	 * @param tol Tolerance for convergence.  Try 1e-8
	 * @param maxIterations Maximum number of iterations it will perform.  Try 100 or more.
	 * @return Refinement
	 */
	public static RefineEpipolarMatrix refineHomography( double tol , int maxIterations  ,
														 EpipolarError type ) {
		ModelObservationResidualN residuals;
		switch( type ) {
			case SIMPLE:
				residuals = new HomographyResidualSampson();
				break;

			case SAMPSON:
				residuals = new HomographyResidualTransfer();
				break;

			default:
				throw new IllegalArgumentException("Type not supported: "+type);
		};

		return new LeastSquaresHomography(tol,maxIterations,residuals);
	}

	/**
	 * Returns an algorithm for estimating a fundamental matrix given a set of
	 * {@link AssociatedPair} in pixel coordinates.
	 *
	 * @param minPoints Selects which algorithms to use.  Can be 7 or 8.
	 * @return Fundamental algorithm.
	 */
	public static EpipolarMatrixEstimator computeFundamental( int minPoints ) {
		return new WrapFundamentalLinear(true,minPoints);
	}

	/**
	 * Returns an algorithm for estimating an /essential matrix given a set of
	 * {@link AssociatedPair} in normalized image coordinates.
	 *
	 * @param minPoints Selects which algorithms to use.  Can be 5, 7, or 8.
	 * @return Fundamental algorithm.
	 */
	public static EpipolarMatrixEstimator computeEssential( int minPoints ) {
		if( minPoints == 5 )
			return new WrapEssentialNister5();
		else
			return new WrapFundamentalLinear(false,minPoints);
	}

	/**
	 * Creates a robust model generator for use with {@link boofcv.numerics.fitting.modelset.ModelMatcher},
	 *
	 * @param fundamentalAlg The algorithm which is being wrapped.
	 * @return ModelGenerator
	 */
	public static ModelGenerator<DenseMatrix64F,AssociatedPair>
	robustModel( EpipolarMatrixEstimator fundamentalAlg ) {
		return new GenerateEpipolarMatrix(fundamentalAlg);
	}

	/**
	 * Creates a non-linear optimizer for refining estimates of fundamental or essential matrices.
	 *
	 * @see boofcv.alg.geo.f.FundamentalResidualSampson
	 * @see boofcv.alg.geo.f.FundamentalResidualSimple
	 *
	 * @param tol Tolerance for convergence.  Try 1e-8
	 * @param maxIterations Maximum number of iterations it will perform.  Try 100 or more.
	 * @return Refinement
	 */
	public static RefineEpipolarMatrix refineFundamental( double tol , int maxIterations ,
														  EpipolarError type ) {
		switch( type ) {
			case SAMPSON:
				return new LeastSquaresFundamental(tol,maxIterations,true);
			case SIMPLE:
				return new LeastSquaresFundamental(tol,maxIterations,false);
		}

		throw new IllegalArgumentException("Type not supported: "+type);
	}

	/**
	 * Returns a solution to the PnP problem for 4 or more points using EPnP. Fast and fairly
	 * accurate algorithm.  Can handle general and planar scenario automatically.
	 *
	 * @see PnPLepetitEPnP
	 *
	 * @param numIterations If more then zero then non-linear optimization is done.  More is not always better.  Try 10
	 * @param magicNumber Affects how the problem is linearized.  See comments in {@link PnPLepetitEPnP}.  Try 0.1
	 * @return  PerspectiveNPoint
	 */
	public static PerspectiveNPoint pnpEfficientPnP( int numIterations , double magicNumber ) {
		PnPLepetitEPnP alg = new PnPLepetitEPnP(magicNumber);
		alg.setNumIterations(numIterations);
		return new WrapPnPLepetitEPnP(alg);
	}

	/**
	 * Returns a solution to the PnP problem for 4 or more points using EPnP. Fast and fairly
	 * accurate algorithm.  Can handle general and planar scenario automatically.
	 *
	 * @see PnPLepetitEPnP
	 *
	 * @param numIterations If more then zero then non-linear optimization is done.  More is not always better.  Try 10
	 * @param magicNumber Affects how the problem is linearized.  See comments in {@link PnPLepetitEPnP}.  Try 0.1
	 * @return  PerspectiveNPoint
	 */
	public static RefinePerspectiveNPoint refinePnpEfficient( int numIterations , double magicNumber ) {
		PnPLepetitEPnP alg = new PnPLepetitEPnP(magicNumber);
		alg.setNumIterations(numIterations);
		return new WrapPnPLepetitEPnP(alg);
	}

	/**
	 * Refines a pose solution to the PnP problem using non-linear least squares..
	 *
	 * @param tol Convergence tolerance. Try 1e-8
	 * @param maxIterations Maximum number of iterations.  Try 200
	 */
	public static RefinePerspectiveNPoint refinePnP( double tol , int maxIterations ) {
		return new LeastSquaresPose(tol,maxIterations,new PoseRodriguesCodec());
	}
}
