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

package boofcv.alg.feature.detect;

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.extract.WrapperNonMaximumBlock;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.intensity.WrapperGradientCornerIntensity;
import boofcv.alg.feature.detect.extract.NonMaxBlockStrict;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ProcessImageSequence;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.BoofVideoManager;
import boofcv.struct.BoofDefaults;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I16;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class VideoDetectCorners<T extends ImageSingleBand, D extends ImageSingleBand>
		extends ProcessImageSequence<T> {

	GeneralFeatureDetector<T, D> detector;
	D derivX;
	D derivY;
	D derivXX;
	D derivYY;
	D derivXY;

	Class<D> derivType;

	QueueCorner corners;

	ImagePanel panel;

	public VideoDetectCorners(SimpleImageSequence<T> sequence,
							  GeneralFeatureDetector<T, D> detector,
							  Class<D> derivType) {
		super(sequence);

		this.derivType = derivType;
		this.detector = detector;
	}


	@Override
	public void processFrame(T image) {

		if (detector.getRequiresGradient()) {
			if (derivX == null) {
				derivX = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);
				derivY = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);
			}

			// compute the image gradient
			GImageDerivativeOps.sobel(image, derivX, derivY, BoofDefaults.DERIV_BORDER_TYPE);
		}

		if (detector.getRequiresHessian()) {
			if (derivXX == null) {
				derivXX = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);
				derivYY = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);
				derivXY = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);
			}

			// compute the image gradient
			GImageDerivativeOps.hessianThree(image, derivXX, derivYY, derivXY, BoofDefaults.DERIV_BORDER_TYPE);
		}

		detector.process(image, derivX, derivY, derivXX, derivYY, derivXY);
		corners = detector.getMaximums();
	}

	@Override
	public void updateGUI(BufferedImage guiImage, T origImage) {
		Graphics2D g2 = guiImage.createGraphics();

		for (int i = 0; i < corners.size(); i++) {
			Point2D_I16 pt = corners.get(i);

			g2.setColor(Color.BLACK);
			g2.fillOval(pt.x - 4, pt.y - 4, 9, 9);
			g2.setColor(Color.RED);
			g2.fillOval(pt.x - 2, pt.y - 2, 5, 5);
		}

		if (panel == null) {
			panel = ShowImages.showWindow(guiImage, "Image Sequence");
			addComponent(panel);
		} else {
			panel.setBufferedImage(guiImage);
			panel.repaint();
		}
	}

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	void perform(String fileName, Class<T> imageType, Class<D> derivType) {
		SimpleImageSequence<T> sequence = BoofVideoManager.loadManagerDefault().load(fileName, ImageType.single(imageType));

		int maxCorners = 200;
		int radius = 2;

		GeneralFeatureIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T, D>(FactoryIntensityPointAlg.shiTomasi(radius, false, derivType));
//		GeneralFeatureIntensity<T, D> intensity =
//				new WrapperFastCornerIntensity<T, D>(FactoryIntensityPointAlg.createFast12(imageType, 8 , 12));

		NonMaxSuppression extractor = new WrapperNonMaximumBlock(new NonMaxBlockStrict.Max());
//		FeatureExtractor extractor = new WrapperNonMaximumBlock( new NonMaxExtractorNaive(radius+10,10f));
//		FeatureExtractor extractor = new WrapperNonMaxCandidate(new NonMaxCandidateStrict(radius+10, 10f));
		extractor.setIgnoreBorder(radius + 10);
		extractor.setThresholdMaximum(10f);

		GeneralFeatureDetector<T, D> detector = new GeneralFeatureDetector<T, D>(intensity, extractor);
		detector.setMaxFeatures(maxCorners);

		VideoDetectCorners<T, D> display = new VideoDetectCorners<T, D>(sequence, detector, derivType);

		display.process();
	}

	public static void main(String args[]) {
		String fileName;

		if (args.length == 0) {
			fileName = "../applet/data/zoom.mjpeg";
		} else {
			fileName = args[0];
		}

//		perform(fileName,ImageUInt8.class,ImageSInt16.class);
		perform(fileName, ImageFloat32.class, ImageFloat32.class);
	}
}
