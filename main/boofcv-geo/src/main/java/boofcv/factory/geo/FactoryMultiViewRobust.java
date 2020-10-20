/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.abst.geo.Triangulate2ViewsMetric;
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
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.ImageDimension;
import georegression.fitting.homography.ModelManagerHomography2D_F64;
import georegression.fitting.se.ModelManagerSe3_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating robust false-positive tolerant estimation algorithms in multi-view geometry.  These
 * algorithms tend to have a lot of boilerplate associated with them and the goal of this factory
 * is to make their use much easier and less error prone.
 *
 * @author Peter Abeles
 */
public class FactoryMultiViewRobust {

	/**
	 * Robust solution to PnP problem using {@link LeastMedianOfSquares LMedS}.  Input observations are
	 * in normalized image coordinates.
	 *
	 * <ul>
	 *     <li>Input observations are in normalized image coordinates NOT pixels</li>
	 *     <li>Error units are pixels squared.</li>
	 * </ul>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param configPnP PnP parameters.  Can't be null.
	 * @param configLMedS Parameters for LMedS.  Can't be null.
	 * @return Robust Se3_F64 estimator
	 */
	public static ModelMatcherMultiview<Se3_F64, Point2D3D> pnpLMedS(@Nullable ConfigPnP configPnP,
																	 ConfigLMedS configLMedS)
	{
		if( configPnP == null )
			configPnP = new ConfigPnP();
		configPnP.checkValidity();
		configLMedS.checkValidity();

		Estimate1ofPnP estimatorPnP = FactoryMultiView.pnp_1( configPnP.which , configPnP.epnpIterations, configPnP.numResolve);

		DistanceFromModelMultiView<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();
		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Point2D3D> generator = new EstimatorToGenerator<>(estimatorPnP);

		LeastMedianOfSquaresMultiView<Se3_F64, Point2D3D> lmeds =
				new LeastMedianOfSquaresMultiView<>(configLMedS.randSeed, configLMedS.totalCycles, manager, generator, distance);
		lmeds.setErrorFraction(configLMedS.errorFraction);
		return lmeds;
	}

	/**
	 * Robust solution to PnP problem using {@link Ransac}.  Input observations are in normalized
	 * image coordinates. Found transform is from world to camera.
	 *
	 * <p>NOTE: Observations are in normalized image coordinates NOT pixels.</p>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @see Estimate1ofPnP
	 *
	 * @param pnp PnP parameters.  Can't be null.
	 * @param ransac Parameters for RANSAC.  Can't be null.
	 * @return Robust Se3_F64 estimator
	 */
	public static ModelMatcherMultiview<Se3_F64, Point2D3D> pnpRansac( @Nullable ConfigPnP pnp,
																	   ConfigRansac ransac )
	{
		if( pnp == null )
			pnp = new ConfigPnP();
		pnp.checkValidity();
		ransac.checkValidity();

		Estimate1ofPnP estimatorPnP = FactoryMultiView.pnp_1(pnp.which, pnp.epnpIterations, pnp.numResolve);
		DistanceFromModelMultiView<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();
		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Point2D3D> generator =
				new EstimatorToGenerator<>(estimatorPnP);

		// convert from pixels to pixels squared
		double threshold = ransac.inlierThreshold*ransac.inlierThreshold;

		return new RansacMultiView<>(ransac.randSeed, manager, generator, distance, ransac.iterations, threshold);
	}

	/**
	 * Robust solution for estimating {@link Se3_F64} using epipolar geometry from two views with
	 * {@link LeastMedianOfSquares LMedS}.  Input observations are in normalized image coordinates.
	 *
	 * <ul>
	 *     <li>Error units is pixels squared times two</li>
	 * </ul>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param essential Essential matrix estimation parameters.  Can't be null.
	 * @param lmeds Parameters for RANSAC.  Can't be null.
	 * @return Robust Se3_F64 estimator
	 */
	public static ModelMatcherMultiview<Se3_F64, AssociatedPair> baselineLMedS( @Nullable ConfigEssential essential,
																				ConfigLMedS lmeds )
	{
		if( essential == null )
			essential = new ConfigEssential();
		else
			essential.checkValidity();

		Estimate1ofEpipolar epipolar = FactoryMultiView.
				essential_1(essential.which, essential.numResolve);

		Triangulate2ViewsMetric triangulate = FactoryMultiView.triangulate2ViewMetric(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));
		ModelManager<Se3_F64> manager = new ModelManagerSe3_F64();
		ModelGenerator<Se3_F64, AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(epipolar, triangulate);

		DistanceFromModelMultiView<Se3_F64, AssociatedPair> distanceSe3 = new DistanceSe3SymmetricSq(triangulate);

		LeastMedianOfSquaresMultiView<Se3_F64, AssociatedPair> config = new LeastMedianOfSquaresMultiView<>
				(lmeds.randSeed, lmeds.totalCycles, manager, generateEpipolarMotion, distanceSe3);
		config.setErrorFraction(lmeds.errorFraction);
		return config;
	}

	public static ModelMatcher<DMatrixRMaj, AssociatedPair> fundamentalLMedS(ConfigFundamental fundamental,
																			 ConfigLMedS lmeds ) {

		fundamental.checkValidity();
		lmeds.checkValidity();

		ModelManager<DMatrixRMaj> managerF = new ModelManagerEpipolarMatrix();
		Estimate1ofEpipolar estimateF = FactoryMultiView.fundamental_1(fundamental.which,
				fundamental.numResolve);
		GenerateEpipolarMatrix generateF = new GenerateEpipolarMatrix(estimateF);

		// How the error is measured
		DistanceFromModel<DMatrixRMaj,AssociatedPair> errorMetric;

		switch( fundamental.errorModel ) {
			case SAMPSON:
				errorMetric = new DistanceFromModelResidual<>(new FundamentalResidualSampson());
				break;

			case GEOMETRIC:
				errorMetric = new DistanceFundamentalGeometric();
				break;

			default:
				throw new RuntimeException("Unknown");
		}


		LeastMedianOfSquares<DMatrixRMaj, AssociatedPair> config = new LeastMedianOfSquares<>
				(lmeds.randSeed, lmeds.totalCycles, managerF, generateF, errorMetric);
		config.setErrorFraction(lmeds.errorFraction);
		return config;
	}

	/**
	 * Robust solution for estimating the stereo baseline {@link Se3_F64} using epipolar geometry from two views with
	 * {@link RansacMultiView}.  Input observations are in normalized image coordinates.
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param essential Essential matrix estimation parameters.
	 * @param ransac Parameters for RANSAC.  Can't be null.
	 * @return Robust Se3_F64 estimator
	 */
	public static ModelMatcherMultiview<Se3_F64, AssociatedPair> baselineRansac(@Nullable ConfigEssential essential,
																				ConfigRansac ransac )
	{
		if( essential == null )
			essential = new ConfigEssential();
		else
			essential.checkValidity();
		ransac.checkValidity();

		if( essential.errorModel != ConfigEssential.ErrorModel.GEOMETRIC) {
			throw new RuntimeException("Error model has to be Euclidean");
		}

		Estimate1ofEpipolar epipolar = FactoryMultiView.
				essential_1(essential.which, essential.numResolve);

		Triangulate2ViewsMetric triangulate = FactoryMultiView.triangulate2ViewMetric(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));
		ModelManager<Se3_F64> manager = new ModelManagerSe3_F64();
		ModelGenerator<Se3_F64, AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(epipolar, triangulate);

		DistanceFromModelMultiView<Se3_F64, AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate);

		double ransacTOL = ransac.inlierThreshold * ransac.inlierThreshold * 2.0;

		return new RansacMultiView<>(ransac.randSeed, manager, generateEpipolarMotion, distanceSe3,
				ransac.iterations, ransacTOL);
	}

	public static ModelMatcherMultiview<DMatrixRMaj, AssociatedPair>  essentialRansac(@Nullable ConfigEssential essential,
																					  ConfigRansac ransac )
	{
		if( essential == null )
			essential = new ConfigEssential();
		else
			essential.checkValidity();
		ransac.checkValidity();

		if( essential.errorModel == ConfigEssential.ErrorModel.GEOMETRIC) {
			// The best error has been selected. Compute using the baseline algorithm then convert back into
			// essential matrix
			return new MmmvSe3ToEssential(baselineRansac(essential,ransac));
		}

		ModelManager<DMatrixRMaj> managerE = new ModelManagerEpipolarMatrix();
		Estimate1ofEpipolar estimateF = FactoryMultiView.essential_1(essential.which,
				essential.numResolve);
		GenerateEpipolarMatrix generateE = new GenerateEpipolarMatrix(estimateF);

		// How the error is measured
		DistanceFromModelMultiView<DMatrixRMaj,AssociatedPair> errorMetric =
				new DistanceMultiView_EssentialSampson();
		double ransacTOL = ransac.inlierThreshold * ransac.inlierThreshold;

		return new RansacMultiView<>(ransac.randSeed, managerE, generateE, errorMetric,
				ransac.iterations, ransacTOL);
	}


	public static ModelMatcher<DMatrixRMaj, AssociatedPair> fundamentalRansac(
			ConfigFundamental fundamental,
			ConfigRansac ransac ) {

		fundamental.checkValidity();
		ransac.checkValidity();

		ModelManager<DMatrixRMaj> managerF = new ModelManagerEpipolarMatrix();
		Estimate1ofEpipolar estimateF = FactoryMultiView.fundamental_1(fundamental.which,
				fundamental.numResolve);
		GenerateEpipolarMatrix generateF = new GenerateEpipolarMatrix(estimateF);

		// How the error is measured
		DistanceFromModel<DMatrixRMaj,AssociatedPair> errorMetric;

		switch( fundamental.errorModel ) {
			case SAMPSON:
				errorMetric = new DistanceFromModelResidual<>(new FundamentalResidualSampson());
				break;

			case GEOMETRIC:
				errorMetric = new DistanceFundamentalGeometric();
				break;

			default:
				throw new RuntimeException("Unknown");
		}

		double ransacTOL = ransac.inlierThreshold * ransac.inlierThreshold;

		return new Ransac<>(ransac.randSeed, managerF, generateF, errorMetric, ransac.iterations, ransacTOL);
	}

	/**
	 * Robust solution for estimating {@link Homography2D_F64} with {@link LeastMedianOfSquares LMedS}.  Input
	 * observations are in pixel coordinates.
	 *
	 * <ul>
	 *     <li>Four point linear is used internally</p>
	 *     <li>inlierThreshold is in pixels</p>
	 * </ul>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param homography Homography estimation parameters.  If null default is used.
	 * @param configLMedS Parameters for LMedS.  Can't be null.
	 * @return Homography estimator
	 */
	public static LeastMedianOfSquares<Homography2D_F64,AssociatedPair>
	homographyLMedS(@Nullable ConfigHomography homography , ConfigLMedS configLMedS )
	{
		if( homography == null )
			homography = new ConfigHomography();

		ModelManager<Homography2D_F64> manager = new ModelManagerHomography2D_F64();
		GenerateHomographyLinear modelFitter = new GenerateHomographyLinear(homography.normalize);
		DistanceHomographySq distance = new DistanceHomographySq();

		LeastMedianOfSquares<Homography2D_F64,AssociatedPair> lmeds= new LeastMedianOfSquares<>
				(configLMedS.randSeed, configLMedS.totalCycles, manager, modelFitter, distance);
		lmeds.setErrorFraction(configLMedS.errorFraction);
		return lmeds;
	}

	/**
	 * Robust solution for estimating {@link Homography2D_F64} with {@link Ransac}.  Input
	 * observations are in pixel coordinates.
	 *
	 * <ul>
	 *     <li>Four point linear is used internally</p>
	 *     <li>inlierThreshold is in pixels</p>
	 * </ul>
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param homography Homography estimation parameters.  If null default is used.
	 * @param ransac Parameters for RANSAC.  Can't be null.
	 * @return Homography estimator
	 */
	public static Ransac<Homography2D_F64,AssociatedPair>
	homographyRansac( @Nullable ConfigHomography homography , ConfigRansac ransac )
	{
		if( homography == null )
			homography = new ConfigHomography();

		ModelManager<Homography2D_F64> manager = new ModelManagerHomography2D_F64();
		GenerateHomographyLinear modelFitter = new GenerateHomographyLinear(homography.normalize);
		DistanceHomographySq distance = new DistanceHomographySq();

		double ransacTol = ransac.inlierThreshold*ransac.inlierThreshold;

		return new Ransac<>(ransac.randSeed, manager, modelFitter, distance, ransac.iterations, ransacTol);
	}

	/**
	 * Estimates a homography from normalized image coordinates but computes the error in pixel coordinates
	 *
	 * @see GenerateHomographyLinear
	 * @see DistanceHomographyCalibratedSq
	 *
	 * @param ransac RANSAC configuration
	 * @return Ransac
	 */
	public static RansacMultiView<Homography2D_F64,AssociatedPair>
	homographyCalibratedRansac( ConfigRansac ransac )
	{
		ModelManager<Homography2D_F64> manager = new ModelManagerHomography2D_F64();
		GenerateHomographyLinear modelFitter = new GenerateHomographyLinear(false);
		DistanceHomographyCalibratedSq distance = new DistanceHomographyCalibratedSq();

		double ransacTol = ransac.inlierThreshold*ransac.inlierThreshold;

		return new RansacMultiView<>
				(ransac.randSeed, manager, modelFitter, distance, ransac.iterations, ransacTol);
	}

	/**
	 * Robust RANSAC based estimator for
	 *
	 * @see FactoryMultiView#trifocal_1
	 *
	 * @param trifocal Configuration for trifocal tensor calculation
	 * @param error Configuration for how trifocal error is computed
	 * @param ransac Configuration for RANSAC
	 * @return RANSAC
	 */
	public static Ransac<TrifocalTensor, AssociatedTriple>
	trifocalRansac( @Nullable ConfigTrifocal trifocal ,
					@Nullable ConfigTrifocalError error,
					ConfigRansac ransac ) {
		if( trifocal == null )
			trifocal = new ConfigTrifocal();
		if( error == null )
			error = new ConfigTrifocalError();

		trifocal.checkValidity();

		double ransacTol;
		DistanceFromModel<TrifocalTensor,AssociatedTriple> distance;

		switch (error.model) {
			case REPROJECTION -> {
				ransacTol = 3.0 * ransac.inlierThreshold * ransac.inlierThreshold;
				distance = new DistanceTrifocalReprojectionSq();
			}
			case REPROJECTION_REFINE -> {
				ransacTol = 3.0 * ransac.inlierThreshold * ransac.inlierThreshold;
				distance = new DistanceTrifocalReprojectionSq(error.converge.gtol, error.converge.maxIterations);
			}
			case POINT_TRANSFER -> {
				ransacTol = 2.0 * ransac.inlierThreshold * ransac.inlierThreshold;
				distance = new DistanceTrifocalTransferSq();
			}
			default -> throw new IllegalArgumentException("Unknown error model " + error.model);
		}

		Estimate1ofTrifocalTensor estimator = FactoryMultiView.trifocal_1(trifocal);
		ModelManager<TrifocalTensor> manager = new ManagerTrifocalTensor();
		ModelGenerator<TrifocalTensor,AssociatedTriple> generator = new GenerateTrifocalTensor(estimator);

		return new Ransac<>(ransac.randSeed, manager, generator, distance, ransac.iterations, ransacTol);
	}

	/**
	 * Projective to metric self calibration from 3-views
	 *
	 * @param selfcalib (Input) configuration for self calibration
	 * @param ransac (Input) configuration for RANSAC
	 * @return RANSAC
	 */
	public static RansacProjective<MetricCameraTriple, AssociatedTriple>
	metricThreeViewRansac( @Nullable ConfigPixelsToMetric selfcalib,
						   ConfigRansac ransac)
	{
		// Pixel error squared in two views
		double ransacTol = ransac.inlierThreshold*ransac.inlierThreshold*2;

		// lint:forbidden ignore_below 1
		var generator = FactoryMultiView.selfCalibThree(selfcalib);
		var manager = new ModelManagerMetricCameraTriple();
		var distance = new DistanceFromModelIntoViews<MetricCameraTriple, AssociatedTriple, ImageDimension>
				(new DistanceMetricTripleReprojection23(),3);

		return new RansacProjective<>(ransac.randSeed, manager, generator, distance, ransac.iterations, ransacTol);
	}
}
