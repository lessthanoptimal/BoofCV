/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.sfm.d2.*;
import boofcv.alg.sfm.robust.DistanceAffine2DSq;
import boofcv.alg.sfm.robust.DistanceHomographySq;
import boofcv.alg.sfm.robust.GenerateAffine2D;
import boofcv.alg.sfm.robust.GenerateHomographyLinear;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homo.Homography2D_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * @author Peter Abeles
 */
public class FactoryMotion2D {

	public static <I extends ImageBase, IT extends InvertibleTransform>
	ImageMotion2D<I,IT> createMotion2D( int ransacIterations , double inlierThreshold,int outlierPrune,
										int absoluteMinimumTracks, double respawnTrackFraction,
										double respawnCoverageFraction,
										boolean refineEstimate ,
										PointTracker<I> tracker , IT motionModel ) {

		ModelGenerator<IT,AssociatedPair> fitter;
		DistanceFromModel<IT,AssociatedPair> distance;
		ModelFitter<IT,AssociatedPair> modelRefiner = null;

		if( motionModel instanceof Homography2D_F64) {
			GenerateHomographyLinear mf = new GenerateHomographyLinear(true);
			fitter = (ModelGenerator)mf;
			if( refineEstimate )
				modelRefiner = (ModelFitter)mf;
			distance = (DistanceFromModel)new DistanceHomographySq();
		} else if( motionModel instanceof Affine2D_F64) {
			GenerateAffine2D mf = new GenerateAffine2D();
			fitter = (ModelGenerator)mf;
			if( refineEstimate )
				modelRefiner = (ModelFitter)mf;
			distance =  (DistanceFromModel)new DistanceAffine2DSq();
		} else {
			throw new RuntimeException("Unknown model type: "+motionModel.getClass().getSimpleName());
		}

		ModelMatcher<IT,AssociatedPair>  modelMatcher =
				new Ransac(123123,fitter,distance,ransacIterations,inlierThreshold);

		ImageMotionPointTrackerKey<I,IT> lowlevel =
				new ImageMotionPointTrackerKey<I, IT>(tracker,modelMatcher,modelRefiner,motionModel,outlierPrune);

		ImageMotionPtkSmartRespawn<I,IT> smartRespawn =
				new ImageMotionPtkSmartRespawn<I, IT>(lowlevel,
						absoluteMinimumTracks,respawnTrackFraction,respawnCoverageFraction );

		return new WrapImageMotionPtkSmartRespawn<I, IT>(smartRespawn);
	}

	public static <I extends ImageSingleBand, IT extends InvertibleTransform>
	StitchingFromMotion2D<I, IT> createVideoStitch( double maxJumpFraction ,
													ImageMotion2D<I,IT> motion2D , Class<I> imageType ) {
		StitchingTransform transform;

		if( motion2D.getTransformType() == Affine2D_F64.class ) {
			transform = FactoryStitchingTransform.createAffine_F64();
		} else {
			transform = FactoryStitchingTransform.createHomography_F64();
		}

		InterpolatePixel<I> interp = FactoryInterpolation.createPixel(0, 255, TypeInterpolate.BILINEAR, imageType);
		ImageDistort<I> distorter = FactoryDistort.distort(interp, null, imageType);

		return new StitchingFromMotion2D<I, IT>(motion2D,distorter,transform,maxJumpFraction );
	}
}
