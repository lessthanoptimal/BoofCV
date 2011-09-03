/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d2;

import boofcv.abst.feature.tracker.PointSequentialTracker;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.d2.stabilization.PointImageStabilization;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.gui.geo.DrawAssociatedPairs;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ProcessImageSequence;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.xuggler.XugglerSimplified;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.affine.Affine2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;


/**
 * Used to experiment with different
 *
 * @author Peter Abeles
 */
public class StabilizeImageSequence<I extends ImageBase> extends ProcessImageSequence<I> {

	static int thresholdChange = 80;
	static int thresholdReset = 30;
	static double thresholdDistance = 50;
	static int maxFeatures = 200;

	EvaluateImageStabilization<I> evaluator = new EvaluateImageStabilization<I>();

	DrawAssociatedPairs drawFeatures = new DrawAssociatedPairs(3);
	PointImageStabilization<I> stabilizer;

	ImagePanel panelStabilized;
	ImagePanel panelOriginal;

	BufferedImage stabilizedBuff;


	public StabilizeImageSequence(SimpleImageSequence<I> imageSequence) {
		super(imageSequence);
	}

	private static ModelMatcher<Affine2D_F64,AssociatedPair> createModelMatcher() {
		ModelFitterAffine2D modelFitter = new ModelFitterAffine2D();
		DistanceAffine2DSq distance = new DistanceAffine2DSq();
//		DistanceAffine2D distance = new DistanceAffine2D();
//		Affine2DCodec codec = new Affine2DCodec();

		int numSample =  modelFitter.getMinimumPoints();

		return new SimpleInlierRansac<Affine2D_F64,AssociatedPair>(123123,
				modelFitter,distance,30,numSample,numSample,10000,2.0);

//		return new LeastMedianOfSquares<Affine2D_F32,AssociatedPair>(123123,
//				numSample,25,1.1,0.6,modelFitter,distance);

//		return new StatisticalDistanceModelMatcher<Affine2D_F32,AssociatedPair>(25,0.001,0.001,1.2,
//				numSample, StatisticalDistance.MEAN,2,modelFitter,distance,codec);

//		return new StatisticalDistanceModelMatcher<Affine2D_F32,AssociatedPair>(25,0.001,0.001,1.2,
//				numSample, StatisticalDistance.PERCENTILE,0.9,modelFitter,distance,codec);

	}

	public void setStabilizer(PointImageStabilization<I> stabilizer) {
		this.stabilizer = stabilizer;
	}

	@Override
	public void processFrame(I image) {
		stabilizer.process(image);
	}

	@Override
	public void updateGUI(BufferedImage guiImage, I origImage) {
		evaluator.update(origImage,stabilizer.getDistortion(),stabilizer.isKeyFrame());

		I stabilizedImage = stabilizer.getStabilizedImage();
		stabilizedBuff = ConvertBufferedImage.convertTo(stabilizedImage,stabilizedBuff);

		Graphics2D g2 = guiImage.createGraphics();
		drawFeatures.setColor(Color.red);
		drawFeatures.drawCurrent(g2,stabilizer.getTracker().getActiveTracks());
		drawFeatures.setColor(Color.blue);
		drawFeatures.drawCurrent(g2,stabilizer.getInlierFeatures());
		drawFeatures.drawNumber(g2,stabilizer.getInlierFeatures());

		if (panelStabilized == null) {
			panelStabilized = ShowImages.showWindow(stabilizedBuff, "Stabilized Image");
			addComponent(panelStabilized);
			panelOriginal = ShowImages.showWindow(guiImage,"Original");
		} else {
			panelStabilized.setBufferedImage(stabilizedBuff);
			panelStabilized.repaint();
			panelOriginal.setBufferedImage(guiImage);
			panelOriginal.repaint();
		}
	}

	@Override
	public void finishedSequence() {
		evaluator.printMetrics();
	}


	private static <I extends ImageBase,D extends ImageBase>
	void doStuff( String fileName , Class<I> imageType )
	{
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		SimpleImageSequence<I> sequence = new XugglerSimplified<I>(fileName, imageType);

		PointSequentialTracker<I> tracker;

		I image = sequence.next();

		tracker = FactoryPointSequentialTracker.klt(maxFeatures,new int[]{1,2,4},imageType,derivType);
//		tracker = FactoryPointSequentialTracker.surf(maxFeatures,400,1,imageType);

		ModelMatcher<Affine2D_F64,AssociatedPair> fitter = createModelMatcher();

		PointImageStabilization<I> stabilizer = new PointImageStabilization<I>(
				imageType,tracker,fitter,thresholdChange,thresholdReset,thresholdDistance);

		StabilizeImageSequence<I> app = new StabilizeImageSequence<I>(sequence);
		app.setStabilizer(stabilizer);

		app.process();
	}

	public static void main( String args[] ) {
		String fileName;

		if (args.length == 0) {
			fileName = "/media/backup/datasets/2010/snow_videos/snow_norail_stabilization.avi";
		} else {
			fileName = args[0];
		}

		doStuff(fileName, ImageFloat32.class);
//		doStuff(fileName, ImageUInt8.class);
	}
}
