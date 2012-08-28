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
import boofcv.abst.geo.f.*;
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
		}

		return new LeastSquaresHomography(tol,maxIterations,residuals);
	}

	/**
	 * <p>
	 * Returns an algorithm for estimating a fundamental or essential matrix given a set of
	 * {@link AssociatedPair} in pixel coordinates.  The number of hypotheses returned and minimum number of samples
	 * is dependent on the implementation.  The ambiguity from multiple hypotheses can be resolved using other
	 * sample points and testing additional constraints.
	 * </p>
	 *
	 * <p>
	 * There are more differences between these algorithms than the minimum number of sample points.  Consult
	 * the literature for information on critical surfaces which will work or not work with each algorithm.  In
	 * general, algorithm which require fewer samples have less issues with critical surfaces than the 8-point
	 * algorithm.
	 * </p>
	 *
	 * <p>
	 * IMPORTANT: When estimating a fundamental matrix use pixel coordinates.  When estimating an essential matrix
	 * use normalized image coordinates from a calibrated camera.
	 * </p>
	 *
	 * <p>
	 * IMPORTANT. The number of allowed sample points varies depending on the algorithm.  The 8 point algorithm can
	 * process 8 or more points.  Both the 5 an 7 point algorithms require exactly 5 and 7 points exactly. In addition
	 * the 5-point algorithm is only for the calibrated (essential) case.
	 * </p>
	 *
	 * @see boofcv.alg.geo.f.EssentialNister5
	 * @see boofcv.alg.geo.f.FundamentalLinear7
	 * @see boofcv.alg.geo.f.FundamentalLinear8
	 *
	 * @param minimumSamples Selects which algorithms to use.  Can be 5, 7 or 8. See above.
	 * @param isFundamental If true then a Fundamental matrix is estimated, otherwise false for essential matrix.
	 * @return Fundamental or essential estimation algorithm that returns multiple hypotheses.
	 */
	public static EpipolarMatrixEstimatorN computeFundamentalMulti( int minimumSamples , boolean isFundamental ) {
		if( minimumSamples == 8 ) {
			return new Epipolar1toN(new WrapFundamentalLinear8(isFundamental));
		} else if( minimumSamples == 7 ) {
			return new WrapFundamentalLinear7(isFundamental);
		} else if( minimumSamples == 5 ) {
			if( isFundamental )
				throw new IllegalArgumentException("The 5-point algorithm only generates essential matrices");
			return new WrapEssentialNister5();
		}
		throw new IllegalArgumentException("No algorithm matches specified parameters");
	}

	/**
	 * <p>
	 * Similar to {@link #computeFundamentalMulti(int, boolean)}, but it returns only a single hypothesis.  If
	 * the underlying algorithm generates multiple hypotheses they are resolved by considering additional
	 * sample points. For example, if you are using the 7 point algorithm at least one additional sample point
	 * is required to resolve that ambiguity.  So 8 or more sample points are now required.
	 * </p>
	 *
	 * <p>
	 * See {@link #computeFundamentalMulti(int, boolean)} for a description of the algorithms and what 'minimumSamples'
	 * and 'isFundamental' do.
	 * </p>
	 *
	 * <p>
	 * The 8-point algorithm already returns a single hypothesis and ignores the 'numRemoveAmbiguity' parameter.
	 * All other algorithms require one or more points to remove ambiguity.  Understanding a bit of theory is required
	 * to understand what a good number of points is.  If a single point is used then to select the correct answer that
	 * point must be in the inlier set.  If more than one point, say 10, then not all of those points must be in the
	 * inlier set,
	 * </p>
	 *
	 * @see FundamentalNto1
	 *
	 * @param minimumSamples Selects which algorithms to use.  Can be 5, 7 or 8. See above.
	 * @param isFundamental If true then a Fundamental matrix is estimated, otherwise false for essential matrix.
	 * @param numRemoveAmbiguity Number of sample points used to prune hypotheses.
	 * @return Fundamental or essential estimation algorithm that returns a single hypothesis.
	 */
	public static EpipolarMatrixEstimator computeFundamentalOne( int minimumSamples ,
																 boolean isFundamental ,
																 int numRemoveAmbiguity ) {
		if( minimumSamples == 8 ) {
			return new WrapFundamentalLinear8(isFundamental);
		} else {
			if( numRemoveAmbiguity <= 0 )
				throw new IllegalArgumentException("numRemoveAmbiguity must be greater than zero");

			EpipolarMatrixEstimatorN alg = computeFundamentalMulti(minimumSamples,isFundamental);
			return new FundamentalNto1(alg,numRemoveAmbiguity);
		}
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
