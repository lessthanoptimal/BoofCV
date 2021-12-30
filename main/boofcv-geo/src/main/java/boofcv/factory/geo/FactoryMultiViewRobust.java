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

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.Triangulate2ViewsMetricH;
import boofcv.abst.geo.fitting.DistanceFromModelResidual;
import boofcv.abst.geo.fitting.GenerateEpipolarMatrix;
import boofcv.abst.geo.fitting.ModelManagerEpipolarMatrix;
import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.alg.geo.f.FundamentalResidualSampson;
import boofcv.alg.geo.pose.PnPDistanceReprojectionSq;
import boofcv.alg.geo.robust.*;
import boofcv.alg.geo.selfcalib.DistanceMetricTripleReprojection23;
import boofcv.alg.geo.selfcalib.MetricCameraTriple;
import boofcv.alg.geo.selfcalib.ModelManagerMetricCameraTriple;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.TrifocalTensor;
import georegression.fitting.homography.ModelManagerHomography2D_F64;
import georegression.fitting.se.ModelManagerSe3_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares_MT;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.fitting.modelset.ransac.Ransac_MT;
import org.ddogleg.struct.Factory;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating robust false-positive tolerant estimation algorithms in multi-view geometry. These
 * algorithms tend to have a lot of boilerplate associated with them and the goal of this factory
 * is to make their use much easier and less error prone.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway") // for NullAway bug
public class FactoryMultiViewRobust {

	/**
	 * Robust solution to PnP problem using {@link LeastMedianOfSquares LMedS}. Input observations are
	 * in normalized image coordinates.
	 *
	 * <ul>
	 *     <li>Input observations are in normalized image coordinates NOT pixels</li>
	 *     <li>Error units are pixels squared.</li>
	 * </ul>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param configPnP PnP parameters. Can't be null.
	 * @param configLMedS Parameters for LMedS. Can't be null.
	 * @return Robust Se3_F64 estimator
	 */
	public static ModelMatcherMultiview<Se3_F64, Point2D3D> pnpLMedS( @Nullable ConfigPnP configPnP,
																	  ConfigLMedS configLMedS ) {
		if (configPnP == null)
			configPnP = new ConfigPnP();
		configPnP.checkValidity();
		configLMedS.checkValidity();

		Estimate1ofPnP estimatorPnP = FactoryMultiView.pnp_1(
				configPnP.which, configPnP.epnpIterations, configPnP.numResolve);

		DistanceFromModelMultiView<Se3_F64, Point2D3D> distance = new PnPDistanceReprojectionSq();
		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64, Point2D3D> generator = new EstimatorToGenerator<>(estimatorPnP);

		LeastMedianOfSquaresMultiView<Se3_F64, Point2D3D> lmeds = new LeastMedianOfSquaresMultiView<>(
				configLMedS.randSeed, configLMedS.totalCycles, manager, generator, distance);
		lmeds.setErrorFraction(configLMedS.errorFraction);
		return lmeds;
	}

	/**
	 * Robust solution to PnP problem using {@link Ransac}. Input observations are in normalized
	 * image coordinates. Found transform is from world to camera.
	 *
	 * <p>NOTE: Observations are in normalized image coordinates NOT pixels.</p>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param configPnP PnP parameters. Can't be null.
	 * @param configRansac Parameters for RANSAC. Can't be null.
	 * @return Robust Se3_F64 estimator
	 * @see Estimate1ofPnP
	 */
	public static ModelMatcherMultiview<Se3_F64, Point2D3D> pnpRansac( @Nullable ConfigPnP configPnP,
																	   ConfigRansac configRansac ) {
		if (configPnP == null)
			configPnP = new ConfigPnP();
		configPnP.checkValidity();
		configRansac.checkValidity();

		Estimate1ofPnP estimatorPnP = FactoryMultiView.pnp_1(
				configPnP.which, configPnP.epnpIterations, configPnP.numResolve);
		DistanceFromModelMultiView<Se3_F64, Point2D3D> distance = new PnPDistanceReprojectionSq();

		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64, Point2D3D> generator = new EstimatorToGenerator<>(estimatorPnP);

		// convert from pixels to pixels squared
		double threshold = configRansac.inlierThreshold*configRansac.inlierThreshold;

		return new RansacCalibrated<>(
				configRansac.randSeed, configRansac.iterations, threshold, manager, generator, distance);
	}

	/**
	 * Robust solution for estimating {@link Se3_F64} using epipolar geometry from two views with
	 * {@link LeastMedianOfSquares LMedS}. Input observations are in normalized image coordinates.
	 *
	 * <ul>
	 *     <li>Error units is pixels squared times two</li>
	 * </ul>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param configEssential Essential matrix estimation parameters. Can't be null.
	 * @param configLMedS Parameters for RANSAC. Can't be null.
	 * @return Robust Se3_F64 estimator
	 */
	public static ModelMatcherMultiview<Se3_F64, AssociatedPair>
	baselineLMedS( @Nullable ConfigEssential configEssential, ConfigLMedS configLMedS ) {
		if (configEssential == null)
			configEssential = new ConfigEssential();
		configEssential.checkValidity();
		configLMedS.checkValidity();

		Estimate1ofEpipolar epipolar = FactoryMultiView.
				essential_1(configEssential.which, configEssential.numResolve);

		Triangulate2ViewsMetricH triangulate = FactoryMultiView.triangulate2ViewMetricH(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));
		var manager = new ModelManagerSe3_F64();
		var generateEpipolarMotion = new Se3FromEssentialGenerator(epipolar, triangulate);

		var distanceSe3 = new DistanceSe3SymmetricSq(triangulate);

		var config = new LeastMedianOfSquaresMultiView<>(
				configLMedS.randSeed, configLMedS.totalCycles, manager, generateEpipolarMotion, distanceSe3);
		config.setErrorFraction(configLMedS.errorFraction);
		return config;
	}

	public static ModelMatcher<DMatrixRMaj, AssociatedPair>
	fundamentalLMedS( @Nullable ConfigFundamental configFundamental, ConfigLMedS configLMedS ) {
		if (configFundamental == null)
			configFundamental = new ConfigFundamental();

		configFundamental.checkValidity();
		configLMedS.checkValidity();

		ConfigFundamental _configFundamental = configFundamental;

		var manager = new ModelManagerEpipolarMatrix();

		LeastMedianOfSquares<DMatrixRMaj, AssociatedPair> alg = createLMEDS(configLMedS, manager, AssociatedPair.class);
		alg.setModel(
				() -> {
					Estimate1ofEpipolar estimateF = FactoryMultiView.fundamental_1(_configFundamental.which,
							_configFundamental.numResolve);
					return new GenerateEpipolarMatrix(estimateF);
				},
				() -> switch (_configFundamental.errorModel) {
					case SAMPSON -> new DistanceFromModelResidual<>(new FundamentalResidualSampson());
					case GEOMETRIC -> new DistanceFundamentalGeometric();
				});

		return alg;
	}

	/**
	 * Robust solution for estimating the stereo baseline {@link Se3_F64} using epipolar geometry from two views with
	 * {@link RansacCalibrated}. Input observations are in normalized image coordinates.
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param configEssential Essential matrix estimation parameters.
	 * @param configRansac Parameters for RANSAC. Can't be null.
	 * @return Robust Se3_F64 estimator
	 */
	public static ModelMatcherMultiview<Se3_F64, AssociatedPair>
	baselineRansac( @Nullable ConfigEssential configEssential, ConfigRansac configRansac ) {
		if (configEssential == null)
			configEssential = new ConfigEssential();
		configEssential.checkValidity();
		configRansac.checkValidity();

		if (configEssential.errorModel != ConfigEssential.ErrorModel.GEOMETRIC) {
			throw new RuntimeException("Error model has to be Euclidean");
		}

		Estimate1ofEpipolar epipolar = FactoryMultiView.
				essential_1(configEssential.which, configEssential.numResolve);

		Triangulate2ViewsMetricH triangulate = FactoryMultiView.triangulate2ViewMetricH(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));
		ModelManager<Se3_F64> manager = new ModelManagerSe3_F64();
		ModelGenerator<Se3_F64, AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(epipolar, triangulate);

		DistanceFromModelMultiView<Se3_F64, AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate);

		double ransacTOL = configRansac.inlierThreshold*configRansac.inlierThreshold*2.0;

		return new RansacCalibrated<>(configRansac.randSeed, configRansac.iterations, ransacTOL,
				manager, generateEpipolarMotion, distanceSe3);
	}

	public static ModelMatcherMultiview<DMatrixRMaj, AssociatedPair>
	essentialRansac( @Nullable ConfigEssential configEssential, ConfigRansac configRansac ) {
		if (configEssential == null)
			configEssential = new ConfigEssential();
		configEssential.checkValidity();
		configRansac.checkValidity();

		if (configEssential.errorModel == ConfigEssential.ErrorModel.GEOMETRIC) {
			// The best error has been selected. Compute using the baseline algorithm then convert back into
			// essential matrix
			return new MmmvSe3ToEssential(baselineRansac(configEssential, configRansac));
		}

		ModelManager<DMatrixRMaj> managerE = new ModelManagerEpipolarMatrix();
		Estimate1ofEpipolar estimateF = FactoryMultiView.essential_1(configEssential.which,
				configEssential.numResolve);
		GenerateEpipolarMatrix generateE = new GenerateEpipolarMatrix(estimateF);

		// How the error is measured
		DistanceFromModelMultiView<DMatrixRMaj, AssociatedPair> errorMetric =
				new DistanceMultiView_EssentialSampson();
		double ransacTOL = configRansac.inlierThreshold*configRansac.inlierThreshold;

		return new RansacCalibrated<>(configRansac.randSeed, configRansac.iterations, ransacTOL,
				managerE, generateE, errorMetric);
	}

	public static ModelMatcher<DMatrixRMaj, AssociatedPair> fundamentalRansac(
			ConfigFundamental configFundamental,
			ConfigRansac configRansac ) {

		configFundamental.checkValidity();
		configRansac.checkValidity();

		ModelManager<DMatrixRMaj> manager = new ModelManagerEpipolarMatrix();

		double ransacTol = configRansac.inlierThreshold*configRansac.inlierThreshold;

		Ransac<DMatrixRMaj, AssociatedPair> ransac =
				createRansac(configRansac, ransacTol, manager, AssociatedPair.class);

		ransac.setModel(
				() -> {
					Estimate1ofEpipolar estimateF = FactoryMultiView.fundamental_1(configFundamental.which,
							configFundamental.numResolve);
					return new GenerateEpipolarMatrix(estimateF);
				},
				() -> switch (configFundamental.errorModel) {
					case SAMPSON -> new DistanceFromModelResidual<>(new FundamentalResidualSampson());
					case GEOMETRIC -> new DistanceFundamentalGeometric();
				});
		return ransac;
	}

	/**
	 * Robust solution for estimating {@link Homography2D_F64} with {@link LeastMedianOfSquares LMedS}. Input
	 * observations are in pixel coordinates.
	 *
	 * <ul>
	 *     <li>Four point linear is used internally</p>
	 *     <li>inlierThreshold is in pixels</p>
	 * </ul>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param configHomography Homography estimation parameters. If null default is used.
	 * @param configLMedS Parameters for LMedS. Can't be null.
	 * @return Homography estimator
	 */
	public static LeastMedianOfSquares<Homography2D_F64, AssociatedPair>
	homographyLMedS( @Nullable ConfigHomography configHomography, ConfigLMedS configLMedS ) {
		if (configHomography == null)
			configHomography = new ConfigHomography();
		configHomography.checkValidity();
		configLMedS.checkValidity();

		ConfigHomography _configHomography = configHomography;

		var manager = new ModelManagerHomography2D_F64();

		LeastMedianOfSquares<Homography2D_F64, AssociatedPair> lmeds =
				createLMEDS(configLMedS, manager, AssociatedPair.class);

		lmeds.setModel(
				() -> new GenerateHomographyLinear(_configHomography.normalize),
				DistanceHomographySq::new);
		return lmeds;
	}

	/**
	 * Robust solution for estimating {@link Homography2D_F64} with {@link Ransac}. Input
	 * observations are in pixel coordinates.
	 *
	 * <ul>
	 *     <li>Four point linear is used internally</p>
	 *     <li>inlierThreshold is in pixels</p>
	 * </ul>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param configHomography Homography estimation parameters. If null default is used.
	 * @param configRansac Parameters for RANSAC. Can't be null.
	 * @return Homography estimator
	 */
	public static Ransac<Homography2D_F64, AssociatedPair>
	homographyRansac( @Nullable ConfigHomography configHomography, ConfigRansac configRansac ) {
		if (configHomography == null)
			configHomography = new ConfigHomography();
		configHomography.checkValidity();
		configRansac.checkValidity();

		ConfigHomography _configHomography = configHomography;

		ModelManager<Homography2D_F64> manager = new ModelManagerHomography2D_F64();

		double ransacTol = configRansac.inlierThreshold*configRansac.inlierThreshold;
		Ransac<Homography2D_F64, AssociatedPair> ransac =
				createRansac(configRansac, ransacTol, manager, AssociatedPair.class);
		ransac.setModel(
				() -> new GenerateHomographyLinear(_configHomography.normalize),
				DistanceHomographySq::new);
		return ransac;
	}

	/**
	 * Estimates a homography from normalized image coordinates but computes the error in pixel coordinates
	 *
	 * @param configRansac RANSAC configuration
	 * @return Ransac
	 * @see GenerateHomographyLinear
	 * @see DistanceHomographyCalibratedSq
	 */
	public static RansacCalibrated<Homography2D_F64, AssociatedPair>
	homographyCalibratedRansac( ConfigRansac configRansac ) {
		configRansac.checkValidity();

		var manager = new ModelManagerHomography2D_F64();
		var modelFitter = new GenerateHomographyLinear(false);
		var distance = new DistanceHomographyCalibratedSq();

		double ransacTol = configRansac.inlierThreshold*configRansac.inlierThreshold;

		return new RansacCalibrated<>
				(configRansac.randSeed, configRansac.iterations, ransacTol, manager, modelFitter, distance);
	}

	/**
	 * Robust RANSAC based estimator for
	 *
	 * @param configTrifocal Configuration for trifocal tensor calculation
	 * @param configError Configuration for how trifocal error is computed
	 * @param configRansac Configuration for RANSAC
	 * @return RANSAC
	 * @see FactoryMultiView#trifocal_1
	 */
	public static Ransac<TrifocalTensor, AssociatedTriple>
	trifocalRansac( @Nullable ConfigTrifocal configTrifocal,
					@Nullable ConfigTrifocalError configError,
					ConfigRansac configRansac ) {
		if (configTrifocal == null)
			configTrifocal = new ConfigTrifocal();
		if (configError == null)
			configError = new ConfigTrifocalError();

		configTrifocal.checkValidity();
		configError.checkValidity();

		// needed for lambdas
		final ConfigTrifocal _configTrifocal = configTrifocal;
		final ConfigTrifocalError _configError = configError;

		double ransacTol;
		Factory<DistanceFromModel<TrifocalTensor, AssociatedTriple>> distance;

		switch (configError.model) {
			case REPROJECTION -> {
				ransacTol = 3.0*configRansac.inlierThreshold*configRansac.inlierThreshold;
				distance = DistanceTrifocalReprojectionSq::new;
			}
			case REPROJECTION_REFINE -> {
				ransacTol = 3.0*configRansac.inlierThreshold*configRansac.inlierThreshold;
				distance = () -> new DistanceTrifocalReprojectionSq(
						_configError.converge.gtol, _configError.converge.maxIterations);
			}
			case POINT_TRANSFER -> {
				ransacTol = 2.0*configRansac.inlierThreshold*configRansac.inlierThreshold;
				distance = DistanceTrifocalTransferSq::new;
			}
			default -> throw new IllegalArgumentException("Unknown error model " + configError.model);
		}

		ModelManager<TrifocalTensor> manager = new ManagerTrifocalTensor();

		Ransac<TrifocalTensor, AssociatedTriple> ransac =
				createRansac(configRansac, ransacTol, manager, AssociatedTriple.class);

		ransac.setModel(
				() -> new GenerateTrifocalTensor(FactoryMultiView.trifocal_1(_configTrifocal)),
				distance);
		return ransac;
	}

	/**
	 * Projective to metric self calibration from 3-views
	 *
	 * @param configSelfcalib (Input) configuration for self calibration
	 * @param configRansac (Input) configuration for RANSAC
	 * @return RANSAC
	 */
	public static RansacProjective<MetricCameraTriple, AssociatedTriple>
	metricThreeViewRansac( @Nullable ConfigPixelsToMetric configSelfcalib,
						   ConfigRansac configRansac ) {
		configRansac.checkValidity();

		// Pixel error squared in two views
		double ransacTol = configRansac.inlierThreshold*configRansac.inlierThreshold*2;

		// lint:forbidden ignore_below 1
		var generator = FactoryMultiView.selfCalibThree(configSelfcalib);
		var manager = new ModelManagerMetricCameraTriple();
		var distance = new DistanceFromModelIntoViews<MetricCameraTriple, AssociatedTriple, ElevateViewInfo>
				(new DistanceMetricTripleReprojection23(), 3);

		return new RansacProjective<>(configRansac.randSeed, manager, generator, distance, configRansac.iterations, ransacTol);
	}

	public static <Model, Point> LeastMedianOfSquares<Model, Point>
	createLMEDS( ConfigLMedS configLMedS, ModelManager<Model> manager, Class<Point> pointType ) {
		LeastMedianOfSquares<Model, Point> alg = BoofConcurrency.isUseConcurrent() ?
				new LeastMedianOfSquares_MT<>(configLMedS.randSeed, configLMedS.totalCycles, manager, pointType)
				:
				new LeastMedianOfSquares<>(configLMedS.randSeed, configLMedS.totalCycles, manager, pointType);
		alg.setErrorFraction(configLMedS.errorFraction);
		return alg;
	}

	/**
	 * Returns a new instance of RANSAC. If concurrency is turned on then a concurrent version will be returned.
	 *
	 * @param ransacTol inlier tolerance. The tolerance on config isn't used since that might have the wrong units. This
	 * lets the user easily adjust the units without modifying the config.
	 */
	public static <Model, Point> Ransac<Model, Point>
	createRansac( ConfigRansac configRansac, double ransacTol, ModelManager<Model> manager, Class<Point> pointType ) {
		return BoofConcurrency.isUseConcurrent() ?
				new Ransac_MT<>(configRansac.randSeed, configRansac.iterations, ransacTol, manager, pointType)
				:
				new Ransac<>(configRansac.randSeed, configRansac.iterations, ransacTol, manager, pointType);
	}
}
