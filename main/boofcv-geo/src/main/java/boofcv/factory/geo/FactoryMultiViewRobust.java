/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.PnPDistanceReprojectionSq;
import boofcv.alg.geo.robust.DistanceHomographySq;
import boofcv.alg.geo.robust.DistanceSe3SymmetricSq;
import boofcv.alg.geo.robust.GenerateHomographyLinear;
import boofcv.alg.geo.robust.Se3FromEssentialGenerator;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import georegression.fitting.homography.ModelManagerHomography2D_F64;
import georegression.fitting.se.ModelManagerSe3_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;
import org.ddogleg.fitting.modelset.ransac.Ransac;

import javax.annotation.Nonnull;

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
	public static LeastMedianOfSquares<Se3_F64, Point2D3D> pnpLMedS( ConfigPnP configPnP,
																	 ConfigLMedS configLMedS)
	{
		configPnP.checkValidity();
		configLMedS.checkValidity();

		Estimate1ofPnP estimatorPnP = FactoryMultiView.computePnP_1( configPnP.which , configPnP.epnpIterations, configPnP.numResolve);

		DistanceModelMonoPixels<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();
		distance.setIntrinsic(configPnP.intrinsic.fx,configPnP.intrinsic.fy,configPnP.intrinsic.skew);
		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Point2D3D> generator =
				new EstimatorToGenerator<>(estimatorPnP);

		LeastMedianOfSquares<Se3_F64, Point2D3D>  lmeds =
				new LeastMedianOfSquares<>(configLMedS.randSeed, configLMedS.totalCycles, manager, generator, distance);
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
	public static Ransac<Se3_F64, Point2D3D> pnpRansac( @Nonnull ConfigPnP pnp,
														@Nonnull ConfigRansac ransac)
	{
		pnp.checkValidity();
		ransac.checkValidity();

		Estimate1ofPnP estimatorPnP = FactoryMultiView.computePnP_1(pnp.which, pnp.epnpIterations, pnp.numResolve);
		DistanceModelMonoPixels<Se3_F64,Point2D3D> distance = new PnPDistanceReprojectionSq();
		distance.setIntrinsic(pnp.intrinsic.fx,pnp.intrinsic.fy,pnp.intrinsic.skew);
		ModelManagerSe3_F64 manager = new ModelManagerSe3_F64();
		EstimatorToGenerator<Se3_F64,Point2D3D> generator =
				new EstimatorToGenerator<>(estimatorPnP);

		// convert from pixels to pixels squared
		double threshold = ransac.inlierThreshold*ransac.inlierThreshold;

		return new Ransac<>(ransac.randSeed, manager, generator, distance, ransac.maxIterations, threshold);
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
	public static LeastMedianOfSquares<Se3_F64, AssociatedPair> essentialLMedS( ConfigEssential essential,
																				ConfigLMedS lmeds ) {

		essential.checkValidity();

		Estimate1ofEpipolar essentialAlg = FactoryMultiView.
				computeEssential_1(essential.which, essential.numResolve);

		return epipolarLMedS(essentialAlg, essential.intrinsic, lmeds);

	}

	public static LeastMedianOfSquares<Se3_F64, AssociatedPair> fundamentalLMedS( ConfigEssential fundamental,
																				  ConfigLMedS lmeds ) {

		fundamental.checkValidity();

		Estimate1ofEpipolar essentialAlg = FactoryMultiView.
				computeEssential_1(fundamental.which, fundamental.numResolve);

		return epipolarLMedS(essentialAlg, fundamental.intrinsic, lmeds);
	}

	private static LeastMedianOfSquares<Se3_F64, AssociatedPair> epipolarLMedS( Estimate1ofEpipolar epipolar,
																				CameraPinholeRadial intrinsic,
																				ConfigLMedS configLMedS ) {
		TriangulateTwoViewsCalibrated triangulate = FactoryMultiView.triangulateTwoGeometric();
		ModelManager<Se3_F64> manager = new ModelManagerSe3_F64();
		ModelGenerator<Se3_F64, AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(epipolar, triangulate);

		DistanceFromModel<Se3_F64, AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate,
						intrinsic.fx, intrinsic.fy, intrinsic.skew,
						intrinsic.fx, intrinsic.fy, intrinsic.skew);


		LeastMedianOfSquares<Se3_F64, AssociatedPair> config = new LeastMedianOfSquares<>
				(configLMedS.randSeed, configLMedS.totalCycles, manager, generateEpipolarMotion, distanceSe3);
		config.setErrorFraction(configLMedS.errorFraction);
		return config;
	}

	/**
	 * Robust solution for estimating {@link Se3_F64} using epipolar geometry from two views with
	 * {@link Ransac}.  Input observations are in normalized image coordinates.
	 *
	 * <p>See code for all the details.</p>
	 *
	 * @param essential Essential matrix estimation parameters.  Can't be null.
	 * @param ransac Parameters for RANSAC.  Can't be null.
	 * @return Robust Se3_F64 estimator
	 */
	public static Ransac<Se3_F64, AssociatedPair> essentialRansac( ConfigEssential essential,
																   ConfigRansac ransac ) {

		essential.checkValidity();
		ransac.checkValidity();

		Estimate1ofEpipolar essentialAlg = FactoryMultiView.
				computeEssential_1(essential.which, essential.numResolve);

		return epipolarRansac(essentialAlg, essential.intrinsic, ransac);
	}

	public static Ransac<Se3_F64, AssociatedPair> fundamentalRansac( ConfigFundamental essential,
																	 ConfigRansac ransac ) {

		essential.checkValidity();
		ransac.checkValidity();

		Estimate1ofEpipolar essentialAlg = FactoryMultiView.
				computeFundamental_1(essential.which, essential.numResolve);

		return epipolarRansac(essentialAlg, essential.intrinsic, ransac);
	}

	private static Ransac<Se3_F64, AssociatedPair> epipolarRansac(Estimate1ofEpipolar epipolar,
																 CameraPinholeRadial intrinsic,
																 ConfigRansac ransac ) {

		TriangulateTwoViewsCalibrated triangulate = FactoryMultiView.triangulateTwoGeometric();
		ModelManager<Se3_F64> manager = new ModelManagerSe3_F64();
		ModelGenerator<Se3_F64, AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(epipolar, triangulate);

		DistanceFromModel<Se3_F64, AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate,
						intrinsic.fx, intrinsic.fy, intrinsic.skew,
						intrinsic.fx, intrinsic.fy, intrinsic.skew);

		double ransacTOL = ransac.inlierThreshold * ransac.inlierThreshold * 2.0;

		return new Ransac<>(ransac.randSeed, manager, generateEpipolarMotion, distanceSe3,
				ransac.maxIterations, ransacTOL);
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
	homographyLMedS( ConfigHomography homography , ConfigLMedS configLMedS )
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
	homographyRansac( ConfigHomography homography , ConfigRansac ransac )
	{
		if( homography == null )
			homography = new ConfigHomography();

		ModelManager<Homography2D_F64> manager = new ModelManagerHomography2D_F64();
		GenerateHomographyLinear modelFitter = new GenerateHomographyLinear(homography.normalize);
		DistanceHomographySq distance = new DistanceHomographySq();

		double ransacTol = ransac.inlierThreshold*ransac.inlierThreshold;

		return new Ransac<>
				(ransac.randSeed, manager, modelFitter, distance, ransac.maxIterations, ransacTol);
	}
}
