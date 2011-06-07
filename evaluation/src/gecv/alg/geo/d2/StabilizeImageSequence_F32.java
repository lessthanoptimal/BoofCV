/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.geo.d2;

import gecv.alg.geo.AssociatedPair;
import gecv.alg.geo.PointSequentialTracker;
import gecv.alg.geo.d2.stabilization.PointImageStabilization;
import gecv.alg.geo.trackers.PstWrapperKltPyramid;
import gecv.alg.tracker.pklt.PkltManager;
import gecv.alg.tracker.pklt.PkltManagerConfig;
import gecv.io.image.SimpleImageSequence;
import gecv.io.wrapper.xuggler.XugglerSimplified;
import gecv.numerics.fitting.modelset.ModelMatcher;
import gecv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import pja.geometry.struct.affine.Affine2D_F32;


/**
 * Used to experiment with different
 *
 * @author Peter Abeles
 */
public class StabilizeImageSequence_F32 extends StabilizeImageSequenceBase<ImageFloat32> {

	PointSequentialTracker tracker;
	ModelMatcher<Affine2D_F32,AssociatedPair> fitter;
	int thresholdChange;
	int thresholdReset;
	double thresholdDistance;
	int maxFeatures;


	public StabilizeImageSequence_F32(SimpleImageSequence<ImageFloat32> imageSequence) {
		super(imageSequence);
		maxFeatures = 200;
		thresholdChange = 80;
		thresholdReset = 30;
		thresholdDistance = 50;
	}

	public void createAlg( int width , int height ) {
		stabilizer = new PointImageStabilization<ImageFloat32>(
				ImageFloat32.class,tracker,fitter,thresholdChange,thresholdReset,thresholdDistance);

		PkltManagerConfig<ImageFloat32, ImageFloat32> config =
				PkltManagerConfig.createDefault(ImageFloat32.class,ImageFloat32.class,width,height);
		config.minFeatures = 0;
		config.maxFeatures = maxFeatures;
		PkltManager<ImageFloat32, ImageFloat32> trackManager =
				new PkltManager<ImageFloat32, ImageFloat32>(config);

		ModelFitterAffine2D modelFitter = new ModelFitterAffine2D();
		DistanceAffine2D_N2 distance = new DistanceAffine2D_N2();

		PointSequentialTracker<ImageFloat32> tracker =
				new PstWrapperKltPyramid<ImageFloat32,ImageFloat32>(trackManager);
		ModelMatcher<Affine2D_F32,AssociatedPair> fitter =
				new SimpleInlierRansac<Affine2D_F32,AssociatedPair>(123123,
				modelFitter,distance,30,modelFitter.getMinimumPoints(),modelFitter.getMinimumPoints(),10000,1.0);

		PointImageStabilization<ImageFloat32> app = new PointImageStabilization<ImageFloat32>(
				ImageFloat32.class,tracker,fitter,thresholdChange,thresholdReset,thresholdDistance);

		setStabilizer(app);
	}

	public static void main( String args[] ) {
		String fileName;

		if (args.length == 0) {
			fileName = "/mnt/data/datasets/2010/snow_videos/snow_norail_stabilization.avi";
//			fileName = "/mnt/data/datasets/2010/snow_videos/snow_long_drive.avi";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageFloat32> sequence = new XugglerSimplified<ImageFloat32>(fileName, ImageFloat32.class);

		ImageBase<ImageFloat32> image = sequence.next();

		StabilizeImageSequence_F32 app = new StabilizeImageSequence_F32(sequence);

		app.createAlg(image.getWidth(),image.getHeight());

		app.process();
	}
}
