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
import jgrl.struct.affine.Affine2D_F32;
import jgrl.struct.affine.Affine2D_F64;


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

	private ModelMatcher<Affine2D_F64,AssociatedPair> createModelMatcher() {
		ModelFitterAffine2D modelFitter = new ModelFitterAffine2D();
		DistanceAffine2DSq distance = new DistanceAffine2DSq();
//		DistanceAffine2D distance = new DistanceAffine2D();
		Affine2DCodec codec = new Affine2DCodec();

		int numSample =  modelFitter.getMinimumPoints();

		return new SimpleInlierRansac<Affine2D_F64,AssociatedPair>(123123,
				modelFitter,distance,30,numSample,numSample,10000,1.0);

//		return new LeastMedianOfSquares<Affine2D_F32,AssociatedPair>(123123,
//				numSample,25,1.1,0.6,modelFitter,distance);

//		return new StatisticalDistanceModelMatcher<Affine2D_F32,AssociatedPair>(25,0.001,0.001,1.2,
//				numSample, StatisticalDistance.MEAN,2,modelFitter,distance,codec);

//		return new StatisticalDistanceModelMatcher<Affine2D_F32,AssociatedPair>(25,0.001,0.001,1.2,
//				numSample, StatisticalDistance.PERCENTILE,0.9,modelFitter,distance,codec);

	}

	public void createAlg( int width , int height ) {
		PkltManagerConfig<ImageFloat32, ImageFloat32> config =
				PkltManagerConfig.createDefault(ImageFloat32.class,ImageFloat32.class,width,height);
//		config.pyramidScaling = new int[]{2,2,2};
		config.maxFeatures = maxFeatures;
		PkltManager<ImageFloat32, ImageFloat32> trackManager = new PkltManager<ImageFloat32, ImageFloat32>(config);

		PointSequentialTracker<ImageFloat32> tracker =
				new PstWrapperKltPyramid<ImageFloat32,ImageFloat32>(trackManager);
		ModelMatcher<Affine2D_F64,AssociatedPair> fitter = createModelMatcher();

		PointImageStabilization<ImageFloat32> app = new PointImageStabilization<ImageFloat32>(
				ImageFloat32.class,tracker,fitter,thresholdChange,thresholdReset,thresholdDistance);

		setStabilizer(app);
	}

	public static void main( String args[] ) {
		String fileName;

		if (args.length == 0) {
//			fileName = "/home/pja/00004.MTS";
//			fileName = "/home/pja/kayak_shake.mpg";
			fileName = "/home/pja/Videos/00029.MTS";
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
