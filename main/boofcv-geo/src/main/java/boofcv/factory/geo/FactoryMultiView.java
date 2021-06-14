/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.geo.bundle.*;
import boofcv.abst.geo.f.*;
import boofcv.abst.geo.h.HomographyDLT_to_Epipolar;
import boofcv.abst.geo.h.HomographyTLS_to_Epipolar;
import boofcv.abst.geo.h.LeastSquaresHomography;
import boofcv.abst.geo.pose.*;
import boofcv.abst.geo.selfcalib.*;
import boofcv.abst.geo.triangulate.*;
import boofcv.abst.geo.trifocal.WrapRefineThreeViewProjectiveGeometric;
import boofcv.abst.geo.trifocal.WrapTrifocalAlgebraicPoint7;
import boofcv.abst.geo.trifocal.WrapTrifocalLinearPoint7;
import boofcv.alg.geo.ModelObservationResidualN;
import boofcv.alg.geo.bundle.*;
import boofcv.alg.geo.f.DistanceEpipolarConstraint;
import boofcv.alg.geo.h.HomographyDirectLinearTransform;
import boofcv.alg.geo.h.HomographyResidualSampson;
import boofcv.alg.geo.h.HomographyResidualTransfer;
import boofcv.alg.geo.h.HomographyTotalLeastSquares;
import boofcv.alg.geo.pose.*;
import boofcv.alg.geo.robust.ModelGeneratorViews;
import boofcv.alg.geo.selfcalib.*;
import boofcv.alg.geo.triangulate.*;
import boofcv.alg.geo.trifocal.RefineThreeViewProjectiveGeometric;
import boofcv.alg.geo.trifocal.TrifocalAlgebraicPoint7;
import boofcv.misc.ConfigConverge;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.fitting.MotionTransformPoint;
import georegression.fitting.se.FitSpecialEuclideanOps_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.FactoryOptimizationSparse;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UnconstrainedLeastSquaresSchur;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.optimization.trustregion.ConfigTrustRegion;
import org.ddogleg.solver.PolynomialOps;
import org.ddogleg.solver.RootFinderType;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating abstracted algorithms related to multi-view geometry
 *
 * @author Peter Abeles
 */
public class FactoryMultiView {

	/**
	 * Returns bundle adjustment with a sparse implementation for metric reconstruction. In most situations this is
	 * what you want to use, however dense bundle adjustment is available if the problem is small and degenerate.
	 *
	 * @param config (Optional) configuration
	 * @return bundle adjustment
	 */
	public static BundleAdjustment<SceneStructureMetric> bundleSparseMetric( @Nullable ConfigBundleAdjustment config ) {
		if (config == null)
			config = new ConfigBundleAdjustment();

		UnconstrainedLeastSquaresSchur<DMatrixSparseCSC> minimizer;

		if (config.configOptimizer instanceof ConfigTrustRegion)
			minimizer = FactoryOptimizationSparse.doglegSchur((ConfigTrustRegion)config.configOptimizer);
		else
			minimizer = FactoryOptimizationSparse.levenbergMarquardtSchur((ConfigLevenbergMarquardt)config.configOptimizer);

		return new BundleAdjustmentSchur_DSCC<>(minimizer,
				new BundleAdjustmentMetricResidualFunction(),
				new BundleAdjustmentMetricSchurJacobian_DSCC(),
				new CodecSceneStructureMetric());
	}

	/**
	 * Returns bundle adjustment with a sparse implementation for projective reconstruction. In most situations this is
	 * what you want to use, however dense bundle adjustment is available if the problem is small and degenerate.
	 *
	 * @param config (Optional) configuration
	 * @return bundle adjustment
	 */
	public static BundleAdjustment<SceneStructureProjective> bundleSparseProjective( @Nullable ConfigBundleAdjustment config ) {
		if (config == null)
			config = new ConfigBundleAdjustment();

		UnconstrainedLeastSquaresSchur<DMatrixSparseCSC> minimizer;

		if (config.configOptimizer instanceof ConfigTrustRegion)
			minimizer = FactoryOptimizationSparse.doglegSchur((ConfigTrustRegion)config.configOptimizer);
		else
			minimizer = FactoryOptimizationSparse.levenbergMarquardtSchur((ConfigLevenbergMarquardt)config.configOptimizer);

		return new BundleAdjustmentSchur_DSCC<>(minimizer,
				new BundleAdjustmentProjectiveResidualFunction(),
				new BundleAdjustmentProjectiveSchurJacobian_DSCC(),
				new CodecSceneStructureProjective());
	}

	/**
	 * Returns bundle adjustment with a dense implementation for metric reconstruction. While much slower than a
	 * sparse solver, a dense solver can handle systems which are degenerate.
	 *
	 * @param robust If true a smaller but robust solver will be used.
	 * @param config (Optional) configuration
	 * @return bundle adjustment
	 */
	public static BundleAdjustment<SceneStructureMetric> bundleDenseMetric( boolean robust,
																			@Nullable ConfigBundleAdjustment config ) {
		if (config == null)
			config = new ConfigBundleAdjustment();

		UnconstrainedLeastSquaresSchur<DMatrixRMaj> minimizer;

		if (config.configOptimizer instanceof ConfigTrustRegion)
			minimizer = FactoryOptimization.doglegSchur(robust, (ConfigTrustRegion)config.configOptimizer);
		else
			minimizer = FactoryOptimization.levenbergMarquardtSchur(robust, (ConfigLevenbergMarquardt)config.configOptimizer);

		return new BundleAdjustmentSchur_DDRM<>(minimizer,
				new BundleAdjustmentMetricResidualFunction(),
				new BundleAdjustmentMetricSchurJacobian_DDRM(),
				new CodecSceneStructureMetric());
	}

	/**
	 * Returns bundle adjustment with a sparse implementation for projective reconstruction. In most stiuations this is
	 * what you want to use, however dense bundle adjustment is available if the problem is small and degenerate.
	 *
	 * @param robust If true a smaller but robust solver will be used.
	 * @param config (Optional) configuration
	 * @return bundle adjustment
	 */
	public static BundleAdjustment<SceneStructureProjective>
	bundleDenseProjective( boolean robust, @Nullable ConfigBundleAdjustment config ) {
		if (config == null)
			config = new ConfigBundleAdjustment();

		UnconstrainedLeastSquaresSchur<DMatrixRMaj> minimizer;

		if (config.configOptimizer instanceof ConfigTrustRegion)
			minimizer = FactoryOptimization.doglegSchur(robust, (ConfigTrustRegion)config.configOptimizer);
		else
			minimizer = FactoryOptimization.levenbergMarquardtSchur(robust, (ConfigLevenbergMarquardt)config.configOptimizer);

		return new BundleAdjustmentSchur_DDRM<>(minimizer,
				new BundleAdjustmentProjectiveResidualFunction(),
				new BundleAdjustmentProjectiveSchurJacobian_DDRM(),
				new CodecSceneStructureProjective());
	}

	/**
	 * Returns an algorithm for estimating a homography matrix given a set of {@link AssociatedPair}.
	 *
	 * @param normalizeInput If input is in pixel coordinates set to true. False if in normalized image coordinates.
	 * @return Homography estimator.
	 * @see HomographyDirectLinearTransform
	 */
	public static Estimate1ofEpipolar homographyDLT( boolean normalizeInput ) {
		HomographyDirectLinearTransform alg = new HomographyDirectLinearTransform(normalizeInput);
		return new HomographyDLT_to_Epipolar(alg);
	}

	/**
	 * Returns an algorithm for estimating a homography matrix given a set of {@link AssociatedPair}.
	 *
	 * @return Homography estimator.
	 * @see HomographyTotalLeastSquares
	 */
	public static Estimate1ofEpipolar homographyTLS() {
		HomographyTotalLeastSquares alg = new HomographyTotalLeastSquares();
		return new HomographyTLS_to_Epipolar(alg);
	}

	/**
	 * Creates a non-linear optimizer for refining estimates of homography matrices.
	 *
	 * @param tol Tolerance for convergence. Try 1e-8
	 * @param maxIterations Maximum number of iterations it will perform. Try 100 or more.
	 * @return Homography refinement
	 * @see HomographyResidualSampson
	 * @see HomographyResidualTransfer
	 */
	public static RefineEpipolar homographyRefine( double tol, int maxIterations, EpipolarError type ) {
		ModelObservationResidualN residuals = switch (type) {
			case SIMPLE -> new HomographyResidualTransfer();
			case SAMPSON -> new HomographyResidualSampson();
			default -> throw new IllegalArgumentException("Type not supported: " + type);
		};

		return new LeastSquaresHomography(tol, maxIterations, residuals);
	}

	/**
	 * <p>
	 * Returns an algorithm for estimating a fundamental or essential matrix given a set of
	 * {@link AssociatedPair} in pixel coordinates. The number of hypotheses returned and minimum number of samples
	 * is dependent on the implementation. The ambiguity from multiple hypotheses can be resolved using other
	 * sample points and testing additional constraints.
	 * </p>
	 *
	 * <p>
	 * All estimated epipolar matrices will have the following constraint:<br>
	 * x'*F*x = 0, where F is the epipolar matrix, x' = currLoc, and x = keyLoc.
	 * </p>
	 *
	 * <p>
	 * There are more differences between these algorithms than the minimum number of sample points. Consult
	 * the literature for information on critical surfaces which will work or not work with each algorithm. In
	 * general, algorithm which require fewer samples have less issues with critical surfaces than the 8-point
	 * algorithm.
	 * </p>
	 *
	 * <p>
	 * IMPORTANT: When estimating a fundamental matrix use pixel coordinates. When estimating an essential matrix
	 * use normalized image coordinates from a calibrated camera.
	 * </p>
	 *
	 * <p>
	 * IMPORTANT. The number of allowed sample points varies depending on the algorithm. The 8 point algorithm can
	 * process 8 or more points. Both the 5 an 7 point algorithms require exactly 5 and 7 points exactly. In addition
	 * the 5-point algorithm is only for the calibrated (essential) case.
	 * </p>
	 *
	 * @param which Specifies which algorithm is to be created
	 * @return Fundamental or essential estimation algorithm that returns multiple hypotheses.
	 * @see boofcv.alg.geo.f.EssentialNister5
	 * @see boofcv.alg.geo.f.FundamentalLinear7
	 * @see boofcv.alg.geo.f.FundamentalLinear8
	 */
	public static EstimateNofEpipolar fundamental_N( EnumFundamental which ) {
		return switch (which) {
			case LINEAR_8 -> new Estimate1toNofEpipolar(new WrapFundamentalLinear8(true));
			case LINEAR_7 -> new WrapFundamentalLinear7(true);
		};
	}

	public static EstimateNofEpipolar essential_N( EnumEssential which ) {
		return switch (which) {
			case LINEAR_8 -> new Estimate1toNofEpipolar(new WrapFundamentalLinear8(false));
			case LINEAR_7 -> new WrapFundamentalLinear7(false);
			case NISTER_5 -> new WrapEssentialNister5();
		};
	}

	/**
	 * <p>
	 * Similar to {@link #fundamental_N}, but it returns only a single hypothesis. If
	 * the underlying algorithm generates multiple hypotheses they are resolved by considering additional
	 * sample points. For example, if you are using the 7 point algorithm at least one additional sample point
	 * is required to resolve that ambiguity. So 8 or more sample points are now required.
	 * </p>
	 *
	 * <p>
	 * All estimated epipolar matrices will have the following constraint:<br>
	 * x'*F*x = 0, where F is the epipolar matrix, x' = currLoc, and x = keyLoc.
	 * </p>
	 *
	 * <p>
	 * See {@link #fundamental_N} for a description of the algorithms and what 'minimumSamples'
	 * and 'isFundamental' do.
	 * </p>
	 *
	 * <p>
	 * The 8-point algorithm already returns a single hypothesis and ignores the 'numRemoveAmbiguity' parameter.
	 * All other algorithms require one or more points to remove ambiguity. Understanding a bit of theory is required
	 * to understand what a good number of points is. If a single point is used then to select the correct answer that
	 * point must be in the inlier set. If more than one point, say 10, then not all of those points must be in the
	 * inlier set,
	 * </p>
	 *
	 * @param which Specifies which algorithm is to be created
	 * @param numRemoveAmbiguity Number of sample points used to prune hypotheses. Ignored if only a single solution.
	 * @return Fundamental or essential estimation algorithm that returns a single hypothesis.
	 * @see GeoModelEstimatorNto1
	 */
	public static Estimate1ofEpipolar fundamental_1( EnumFundamental which, int numRemoveAmbiguity ) {
		if (which == EnumFundamental.LINEAR_8) {
			return new WrapFundamentalLinear8(true);
		}

		if (numRemoveAmbiguity <= 0)
			throw new IllegalArgumentException("numRemoveAmbiguity must be greater than zero");

		EstimateNofEpipolar alg = fundamental_N(which);
		DistanceEpipolarConstraint distance = new DistanceEpipolarConstraint();

		return new EstimateNto1ofEpipolar(alg, distance, numRemoveAmbiguity);
	}

	public static Estimate1ofEpipolar essential_1( EnumEssential which, int numRemoveAmbiguity ) {
		if (which == EnumEssential.LINEAR_8) {
			return new WrapFundamentalLinear8(false);
		}

		if (numRemoveAmbiguity <= 0)
			throw new IllegalArgumentException("numRemoveAmbiguity must be greater than zero");

		EstimateNofEpipolar alg = essential_N(which);
		DistanceEpipolarConstraint distance = new DistanceEpipolarConstraint();

		return new EstimateNto1ofEpipolar(alg, distance, numRemoveAmbiguity);
	}

	/**
	 * Creates a non-linear optimizer for refining estimates of fundamental or essential matrices.
	 *
	 * @param tol Tolerance for convergence. Try 1e-8
	 * @param maxIterations Maximum number of iterations it will perform. Try 100 or more.
	 * @return RefineEpipolar
	 * @see boofcv.alg.geo.f.FundamentalResidualSampson
	 * @see boofcv.alg.geo.f.FundamentalResidualSimple
	 */
	public static RefineEpipolar fundamentalRefine( double tol, int maxIterations, EpipolarError type ) {
		switch (type) {
			case SAMPSON:
				return new LeastSquaresFundamental(tol, maxIterations, true);
			case SIMPLE:
				return new LeastSquaresFundamental(tol, maxIterations, false);
		}

		throw new IllegalArgumentException("Type not supported: " + type);
	}

	/**
	 * Creates a trifocal tensor estimation algorithm.
	 *
	 * @param config configuration for the estimator
	 * @return Trifocal tensor estimator
	 */
	public static Estimate1ofTrifocalTensor trifocal_1( @Nullable ConfigTrifocal config ) {
		if (config == null) {
			config = new ConfigTrifocal();
		}

		switch (config.which) {
			case LINEAR_7:
				return new WrapTrifocalLinearPoint7();

			case ALGEBRAIC_7:
				ConfigConverge cc = config.converge;
				UnconstrainedLeastSquares optimizer = FactoryOptimization.levenbergMarquardt(null, false);
				TrifocalAlgebraicPoint7 alg = new TrifocalAlgebraicPoint7(optimizer,
						cc.maxIterations, cc.ftol, cc.gtol);

				return new WrapTrifocalAlgebraicPoint7(alg);
		}

		throw new IllegalArgumentException("Unknown type " + config.which);
	}

	/**
	 * Used to refine three projective views. This is the same as refining a trifocal tensor.
	 *
	 * @return RefineThreeViewProjective
	 */
	public static RefineThreeViewProjective threeViewRefine( @Nullable ConfigThreeViewRefine config ) {
		if (config == null)
			config = new ConfigThreeViewRefine();

		switch (config.which) {
			case GEOMETRIC:
				RefineThreeViewProjectiveGeometric alg = new RefineThreeViewProjectiveGeometric();
				alg.getConverge().setTo(config.converge);
				alg.setScale(config.normalizePixels);
				return new WrapRefineThreeViewProjectiveGeometric(alg);
		}

		throw new IllegalArgumentException("Unknown algorithm " + config.which);
	}

	/**
	 * Creates an estimator for the PnP problem that uses only three observations, which is the minimal case
	 * and known as P3P.
	 *
	 * <p>NOTE: Observations are in normalized image coordinates NOT pixels.</p>
	 *
	 * @param which The algorithm which is to be returned.
	 * @param numIterations Number of iterations. Only used by some algorithms and recommended number varies
	 * significantly by algorithm.
	 * @return An estimator which can return multiple estimates.
	 */
	public static EstimateNofPnP pnp_N( EnumPNP which, int numIterations ) {

		MotionTransformPoint<Se3_F64, Point3D_F64> motionFit = FitSpecialEuclideanOps_F64.fitPoints3D();

		switch (which) {
			case P3P_GRUNERT -> {
				P3PGrunert grunert = new P3PGrunert(PolynomialOps.createRootFinder(5, RootFinderType.STURM));
				return new WrapP3PLineDistance(grunert, motionFit);
			}
			case P3P_FINSTERWALDER -> {
				P3PFinsterwalder finster = new P3PFinsterwalder(PolynomialOps.createRootFinder(4, RootFinderType.STURM));
				return new WrapP3PLineDistance(finster, motionFit);
			}
			case EPNP -> {
				Estimate1ofPnP epnp = pnp_1(which, numIterations, 0);
				return new Estimate1toNofPnP(epnp);
			}
			case IPPE -> {
				Estimate1ofEpipolar H = FactoryMultiView.homographyTLS();
				return new Estimate1toNofPnP(new IPPE_to_EstimatePnP(H));
			}
		}

		throw new IllegalArgumentException("Type " + which + " not known");
	}

	/**
	 * Created an estimator for the P3P problem that selects a single solution by considering additional
	 * observations.
	 *
	 * <p>NOTE: Observations are in normalized image coordinates NOT pixels.</p>
	 *
	 * <p>
	 * NOTE: EPnP has several tuning parameters and the defaults here might not be the best for your situation.
	 * Use {@link #computePnPwithEPnP} if you wish to have access to all parameters.
	 * </p>
	 *
	 * @param which The algorithm which is to be returned.
	 * @param numIterations Number of iterations. Only used by some algorithms and recommended number varies
	 * significantly by algorithm.
	 * @param numTest How many additional sample points are used to remove ambiguity in the solutions. Not used
	 * if only a single solution is found.
	 * @return An estimator which returns a single estimate.
	 */
	public static Estimate1ofPnP pnp_1( EnumPNP which, int numIterations, int numTest ) {

		if (which == EnumPNP.EPNP) {
			PnPLepetitEPnP alg = new PnPLepetitEPnP(0.1);
			alg.setNumIterations(numIterations);
			return new WrapPnPLepetitEPnP(alg);
		} else if (which == EnumPNP.IPPE) {
			Estimate1ofEpipolar H = FactoryMultiView.homographyTLS();
			return new IPPE_to_EstimatePnP(H);
		}

		DogArray<Se3_F64> solutions = new DogArray<>(4, Se3_F64::new);

		return new EstimateNto1ofPnP(pnp_N(which, -1), solutions, numTest);
	}

	/**
	 * Projective N Point. This is PnP for uncalibrated cameras. Please read algorithms documentation for
	 * limitations
	 *
	 * @return Estimator
	 * @see PRnPDirectLinearTransform
	 */
	public static Estimate1ofPrNP prnp_1() {
		return new WrapPRnPDirectLinearTransform(new PRnPDirectLinearTransform());
	}

	/**
	 * Returns a solution to the PnP problem for 4 or more points using EPnP. Fast and fairly
	 * accurate algorithm. Can handle general and planar scenario automatically.
	 *
	 * <p>NOTE: Observations are in normalized image coordinates NOT pixels.</p>
	 *
	 * @param numIterations If more then zero then non-linear optimization is done. More is not always better. Try 10
	 * @param magicNumber Affects how the problem is linearized. See comments in {@link PnPLepetitEPnP}. Try 0.1
	 * @return Estimate1ofPnP
	 * @see PnPLepetitEPnP
	 */
	public static Estimate1ofPnP computePnPwithEPnP( int numIterations, double magicNumber ) {
		PnPLepetitEPnP alg = new PnPLepetitEPnP(magicNumber);
		alg.setNumIterations(numIterations);
		return new WrapPnPLepetitEPnP(alg);
	}

	/**
	 * Refines a pose solution to the PnP problem using non-linear least squares..
	 *
	 * @param tol Convergence tolerance. Try 1e-8
	 * @param maxIterations Maximum number of iterations. Try 200
	 */
	public static RefinePnP pnpRefine( double tol, int maxIterations ) {
		return new PnPRefineRodrigues(tol, maxIterations);
	}

	/**
	 * Estimate the camera motion give two observations and the 3D world coordinate of each points.
	 *
	 * @return PoseFromPairLinear6
	 */
	public static PoseFromPairLinear6 poseFromPair() {
		return new PoseFromPairLinear6();
	}

	/**
	 * Triangulate two view using the Discrete Linear Transform (DLT) with a calibrated camera.
	 *
	 * @return Two view triangulation algorithm
	 * @see Wrap2ViewPixelDepthLinear
	 * @see TriangulateMetricLinearDLT
	 */
	public static Triangulate2ViewsMetric triangulate2ViewMetric( @Nullable ConfigTriangulation config ) {
		if (config == null)
			config = new ConfigTriangulation();

		return switch (config.type) {
			case DLT -> new Wrap2ViewPixelDepthLinear();
			case GEOMETRIC -> new Wrap2ViewsTriangulateGeometric();
			default -> throw new IllegalArgumentException("Unknown or unsupported type " + config.type);
		};
	}

	/**
	 * Triangulate two view using the Discrete Linear Transform (DLT) with a calibrated camera.
	 *
	 * @return Two view triangulation algorithm
	 * @see TriangulateMetricLinearDLT
	 * @see Triangulate2ViewsGeometricMetric
	 */
	public static Triangulate2ViewsMetricH triangulate2ViewMetricH( @Nullable ConfigTriangulation config ) {
		if (config == null)
			config = new ConfigTriangulation();

		return switch (config.type) {
			case DLT -> new Wrap2ViewsTriangulateMetricDLTH();
			case GEOMETRIC -> new Wrap2ViewsTriangulateGeometricH();
			default -> throw new IllegalArgumentException("Unknown or unsupported type " + config.type);
		};
	}

	/**
	 * Triangulate two view using the Discrete Linear Transform (DLT) with an uncalibrated camera.
	 *
	 * @return Two view triangulation algorithm
	 * @see TriangulateProjectiveLinearDLT
	 */
	public static Triangulate2ViewsProjective triangulate2ViewProjective( @Nullable ConfigTriangulation config ) {
		if (config == null)
			config = new ConfigTriangulation();

		if (config.type == ConfigTriangulation.Type.DLT) {
			return new Wrap2ViewsTriangulateProjectiveDLT();
		}
		throw new IllegalArgumentException("Unknown or unsupported type " + config.type);
	}

	/**
	 * Triangulate N views using the Discrete Linear Transform (DLT) with a calibrated camera
	 *
	 * @return N-view triangulation algorithm
	 * @see TriangulateMetricLinearDLT
	 */
	public static TriangulateNViewsMetric triangulateNViewMetric( @Nullable ConfigTriangulation config ) {
		if (config == null)
			config = new ConfigTriangulation();

		return switch (config.type) {
			case DLT -> new WrapNViewsTriangulateMetricDLT();

			case GEOMETRIC -> {
				TriangulateNViewsMetric estimator = new WrapNViewsTriangulateMetricDLT();
				TriangulateRefineMetricLS refiner = new TriangulateRefineMetricLS(config.converge.gtol, config.converge.maxIterations);
				yield new TriangulateThenRefineMetric(estimator, refiner);
			}

			default -> throw new IllegalArgumentException("Unknown or unsupported type " + config.type);
		};
	}

	/**
	 * Triangulate N views using the Discrete Linear Transform (DLT) with a calibrated camera in homogenous coordinates
	 *
	 * @return N-view triangulation algorithm
	 * @see TriangulateMetricLinearDLT
	 */
	public static TriangulateNViewsMetricH triangulateNViewMetricH( @Nullable ConfigTriangulation config ) {
		if (config == null)
			config = new ConfigTriangulation();

		return switch (config.type) {
			case DLT -> new WrapNViewsTriangulateMetricHgDLT();

			case GEOMETRIC -> {
				var estimator = new WrapNViewsTriangulateMetricHgDLT();
				var refiner = new TriangulateRefineMetricHgLS(config.converge.gtol, config.converge.maxIterations);
				yield new TriangulateThenRefineMetricH(estimator, refiner);
			}

			default -> throw new IllegalArgumentException("Unknown or unsupported type " + config.type);
		};
	}

	/**
	 * Triangulate N views using the Discrete Linear Transform (DLT) with an uncalibrated camera
	 *
	 * @return N-view triangulation algorithm
	 * @see TriangulateProjectiveLinearDLT
	 */
	public static TriangulateNViewsProjective triangulateNViewProj( @Nullable ConfigTriangulation config ) {
		if (config == null)
			config = new ConfigTriangulation();

		return switch (config.type) {
			case DLT -> new WrapNViewsTriangulateProjectiveDLT();

			case ALGEBRAIC, GEOMETRIC -> {
				TriangulateNViewsProjective estimator = new WrapNViewsTriangulateProjectiveDLT();
				TriangulateRefineProjectiveLS refiner = new TriangulateRefineProjectiveLS(config.converge.gtol, config.converge.maxIterations);
				yield new TriangulateThenRefineProjective(estimator, refiner);
			}

			default -> throw new IllegalArgumentException("Unknown or unsupported type " + config.type);
		};
	}

	/**
	 * Refine the triangulation using Sampson error. Approximately takes in account epipolar constraints.
	 *
	 * @param config Convergence criteria
	 * @return Triangulation refinement algorithm.
	 * @see ResidualsTriangulateEpipolarSampson
	 */
	public static RefineTriangulateEpipolar triangulateRefineEpipolar( ConfigConverge config ) {
		return new TriangulateRefineEpipolarLS(config.gtol, config.maxIterations);
	}

	/**
	 * Refine the triangulation by computing the difference between predicted and actual pixel location.
	 * Does not take in account epipolar constraints.
	 *
	 * @param config Convergence criteria
	 * @return Triangulation refinement algorithm.
	 * @see ResidualsTriangulateMetricSimple
	 */
	public static RefineTriangulateMetric triangulateRefineMetric( ConfigConverge config ) {
		return new TriangulateRefineMetricLS(config.gtol, config.maxIterations);
	}

	/**
	 * Refine the triangulation by computing the difference between predicted and actual pixel location.
	 * Does not take in account epipolar constraints.
	 *
	 * @param config Convergence criteria
	 * @return Triangulation refinement algorithm.
	 * @see TriangulateRefineMetricHgLS
	 */
	public static RefineTriangulateMetricH triangulateRefineMetricH( ConfigConverge config ) {
		return new TriangulateRefineMetricHgLS(config.gtol, config.maxIterations);
	}

	/**
	 * Refines a projective triangulation
	 *
	 * @param config Convergence criteria
	 * @return Triangulation refinement algorithm.
	 * @see ResidualsTriangulateProjective
	 */
	public static RefineTriangulateProjective triangulateRefineProj( ConfigConverge config ) {
		return new TriangulateRefineProjectiveLS(config.gtol, config.maxIterations);
	}

	/**
	 * Three-view self calibration based on various methods. Read documentation carefully since partially known
	 * calibration is the norm and often the principle point needs to be zero.
	 *
	 * @param config (Input) Configuration for metric elevation. Can be null.
	 * @return ModelGenerator
	 */
	public static ModelGeneratorViews<MetricCameraTriple, AssociatedTriple, ElevateViewInfo>
	selfCalibThree( @Nullable ConfigPixelsToMetric config ) {
		if (config == null)
			config = new ConfigPixelsToMetric();
		config.checkValidity();

		Estimate1ofTrifocalTensor trifocal = trifocal_1(config.trifocal);
		ProjectiveToMetricCameras selfcalib =
				switch (config.type) {
					case DUAL_QUADRATIC -> projectiveToMetric(config.dualQuadratic);
					case ESSENTIAL_GUESS -> projectiveToMetric(config.essentialGuess);
					case PRACTICAL_GUESS -> projectiveToMetric(config.practicalGuess);
				};

		return new GenerateMetricTripleFromProjective(trifocal, selfcalib);
	}

	/**
	 * {@link ProjectiveToMetricCameras} based upon {@link SelfCalibrationLinearDualQuadratic}
	 *
	 * @param config (Input) Configuration. Can be null.
	 * @return {@link ProjectiveToMetricCameras}
	 */
	public static ProjectiveToMetricCameras
	projectiveToMetric( @Nullable ConfigSelfCalibDualQuadratic config ) {
		if (config == null)
			config = new ConfigSelfCalibDualQuadratic();
		config.checkValidity();

		final ConfigSelfCalibDualQuadratic c = config;
		var selfCalib = c.knownAspectRatio ? // lint:forbidden ignore_line
				new SelfCalibrationLinearDualQuadratic(c.aspectRatio) :
				new SelfCalibrationLinearDualQuadratic(c.zeroSkew);

		var ret = new ProjectiveToMetricCameraDualQuadratic(selfCalib);
		ret.invalidFractionAccept = c.invalidFractionAccept;
		ret.getRefiner().converge.setTo(c.refineAlgebraic);

		return ret;
	}

	/**
	 * {@link ProjectiveToMetricCameras} based upon {@link SelfCalibrationEssentialGuessAndCheck}
	 *
	 * @param config (Input) Configuration. Can be null.
	 * @return {@link ProjectiveToMetricCameras}
	 */
	public static ProjectiveToMetricCameras
	projectiveToMetric( @Nullable ConfigSelfCalibEssentialGuess config ) {
		if (config == null)
			config = new ConfigSelfCalibEssentialGuess();
		config.checkValidity();

		final ConfigSelfCalibEssentialGuess c = config;
		var selfCalib = new SelfCalibrationEssentialGuessAndCheck();
		selfCalib.fixedFocus = c.fixedFocus;
		selfCalib.numberOfSamples = c.numberOfSamples;
		selfCalib.sampleFocalRatioMin = c.sampleMin;
		selfCalib.sampleFocalRatioMax = c.sampleMax;

		return new ProjectiveToMetricCameraEssentialGuessAndCheck(selfCalib);
	}

	/**
	 * {@link ProjectiveToMetricCameras} based upon {@link SelfCalibrationPraticalGuessAndCheckFocus}
	 *
	 * @param config (Input) Configuration. Can be null.
	 * @return {@link ProjectiveToMetricCameras}
	 */
	public static ProjectiveToMetricCameras
	projectiveToMetric( @Nullable ConfigSelfCalibPracticalGuess config ) {
		if (config == null)
			config = new ConfigSelfCalibPracticalGuess();
		config.checkValidity();

		final ConfigSelfCalibPracticalGuess c = config;
		var selfCalib = new SelfCalibrationPraticalGuessAndCheckFocus();
		selfCalib.setSampling(c.sampleMin, c.sampleMax, c.numberOfSamples);
		selfCalib.setSingleCamera(c.fixedFocus);
		return new ProjectiveToMetricCameraPracticalGuessAndCheck(selfCalib);
	}

	/**
	 * Returns {@link RefineDualQuadraticAlgebraicError} which can be used to minimize the Dual Quadratic
	 * by minimizing algebraic error.
	 *
	 * @param config Converge criteria
	 */
	public static RefineDualQuadraticAlgebraicError refineDualAbsoluteQuadratic( ConfigConverge config ) {
		var alg = new RefineDualQuadraticAlgebraicError();
		alg.getConverge().setTo(config);
		return alg;
	}
}
