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

package boofcv.factory.sfm;

import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.WrapImageMotionPtkSmartRespawn;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.robust.*;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.sfm.d2.*;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.fitting.MotionTransformPoint;
import georegression.fitting.affine.ModelManagerAffine2D_F64;
import georegression.fitting.homography.ModelManagerHomography2D_F64;
import georegression.fitting.se.ModelManagerSe2_F64;
import georegression.fitting.se.MotionSe2PointSVD_F64;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import org.ddogleg.fitting.modelset.*;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * Factory for creating algorithms related to 2D image motion.  Typically used for image stabilization, mosaic, and
 * motion detection in video feeds.
 *
 * @author Peter Abeles
 */
public class FactoryMotion2D {

	/**
	 * Estimates the 2D motion of an image using different models.
	 *
	 * @param ransacIterations Number of RANSAC iterations
	 * @param inlierThreshold Threshold which defines an inlier.
	 * @param outlierPrune If a feature is an outlier for this many turns in a row it is dropped. Try 2
	 * @param absoluteMinimumTracks New features will be respawned if the number of inliers drop below this number.
	 * @param respawnTrackFraction If the fraction of current inliers to the original number of inliers drops below
	 *                             this fraction then new features are spawned.  Try 0.3
	 * @param respawnCoverageFraction If the area covered drops by this fraction then spawn more features.  Try 0.8
	 * @param refineEstimate Should it refine the model estimate using all inliers.
	 * @param tracker Point feature tracker.
	 * @param motionModel Instance of the model model used. Affine2D_F64 or Homography2D_F64
	 * @param <I> Image input type.
	 * @param <IT> Model model
	 * @return  ImageMotion2D
	 */
	public static <I extends ImageBase, IT extends InvertibleTransform>
	ImageMotion2D<I,IT> createMotion2D( int ransacIterations , double inlierThreshold,int outlierPrune,
										int absoluteMinimumTracks, double respawnTrackFraction,
										double respawnCoverageFraction,
										boolean refineEstimate ,
										PointTracker<I> tracker , IT motionModel ) {

		ModelManager<IT> manager;
		ModelGenerator<IT,AssociatedPair> fitter;
		DistanceFromModel<IT,AssociatedPair> distance;
		ModelFitter<IT,AssociatedPair> modelRefiner = null;

		if( motionModel instanceof Homography2D_F64) {
			GenerateHomographyLinear mf = new GenerateHomographyLinear(true);
			manager = (ModelManager)new ModelManagerHomography2D_F64();
			fitter = (ModelGenerator)mf;
			if( refineEstimate )
				modelRefiner = (ModelFitter)mf;
			distance = (DistanceFromModel)new DistanceHomographySq();
		} else if( motionModel instanceof Affine2D_F64) {
			manager = (ModelManager)new ModelManagerAffine2D_F64();
			GenerateAffine2D mf = new GenerateAffine2D();
			fitter = (ModelGenerator)mf;
			if( refineEstimate )
				modelRefiner = (ModelFitter)mf;
			distance =  (DistanceFromModel)new DistanceAffine2DSq();
		} else if( motionModel instanceof Se2_F64) {
			manager = (ModelManager)new ModelManagerSe2_F64();
			MotionTransformPoint<Se2_F64, Point2D_F64> alg = new MotionSe2PointSVD_F64();
			GenerateSe2_AssociatedPair mf = new GenerateSe2_AssociatedPair(alg);
			fitter = (ModelGenerator)mf;
			distance =  (DistanceFromModel)new DistanceSe2Sq();
			// no refine, already optimal
		} else {
			throw new RuntimeException("Unknown model type: "+motionModel.getClass().getSimpleName());
		}

		ModelMatcher<IT,AssociatedPair>  modelMatcher =
				new Ransac(123123,manager,fitter,distance,ransacIterations,inlierThreshold);

		ImageMotionPointTrackerKey<I,IT> lowlevel =
				new ImageMotionPointTrackerKey<>(tracker, modelMatcher, modelRefiner, motionModel, outlierPrune);

		ImageMotionPtkSmartRespawn<I,IT> smartRespawn =
				new ImageMotionPtkSmartRespawn<>(lowlevel,
						absoluteMinimumTracks, respawnTrackFraction, respawnCoverageFraction);

		return new WrapImageMotionPtkSmartRespawn<>(smartRespawn);
	}

	/**
	 * Estimates the image motion then combines images together.  Typically used for mosaics and stabilization.
	 *
	 * @param maxJumpFraction If the area changes by this much between two consecuative frames then the transform
	 *                        is reset.
	 * @param motion2D Estimates the image motion.
	 * @param imageType Type of image processed
	 * @param <I> Image input type.
	 * @param <IT> Model model
	 * @return StitchingFromMotion2D
	 */
	@SuppressWarnings("unchecked")
	public static <I extends ImageBase, IT extends InvertibleTransform>
	StitchingFromMotion2D<I, IT>
	createVideoStitch( double maxJumpFraction , ImageMotion2D<I,IT> motion2D , ImageType<I> imageType ) {
		StitchingTransform<IT> transform;

		if( motion2D.getTransformType() == Affine2D_F64.class ) {
			transform = (StitchingTransform)FactoryStitchingTransform.createAffine_F64();
		} else {
			transform = (StitchingTransform)FactoryStitchingTransform.createHomography_F64();
		}

		InterpolatePixel<I> interp;

		if( imageType.getFamily() == ImageType.Family.GRAY || imageType.getFamily() == ImageType.Family.PLANAR ) {
			interp = FactoryInterpolation.createPixelS(0, 255, InterpolationType.BILINEAR, BorderType.EXTENDED,
					imageType.getImageClass());
		} else {
			throw new IllegalArgumentException("Unsupported image type");
		}

		ImageDistort<I,I> distorter = FactoryDistort.distort(false, interp, imageType);
		distorter.setRenderAll(false);

		return new StitchingFromMotion2D<>(motion2D, distorter, transform, maxJumpFraction);
	}
}
