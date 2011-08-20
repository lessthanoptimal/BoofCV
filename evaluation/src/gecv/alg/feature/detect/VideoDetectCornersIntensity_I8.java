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

package gecv.alg.feature.detect;

import gecv.abst.detect.extract.FeatureExtractor;
import gecv.abst.detect.extract.WrapperNonMax;
import gecv.abst.detect.intensity.GeneralFeatureIntensity;
import gecv.abst.detect.intensity.WrapperGradientCornerIntensity;
import gecv.alg.feature.detect.extract.FastNonMaxExtractor;
import gecv.alg.feature.detect.interest.GeneralFeatureDetector;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.alg.filter.derivative.HessianThree;
import gecv.factory.feature.detect.intensity.FactoryPointIntensityAlg;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.io.wrapper.xuggler.XugglerSimplified;
import gecv.struct.GecvDefaults;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;
import jgrl.struct.point.Point2D_I16;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class VideoDetectCornersIntensity_I8 extends ProcessImageSequence<ImageUInt8> {

	GeneralFeatureDetector<ImageUInt8, ImageSInt16> detector;
	ImageSInt16 derivX;
	ImageSInt16 derivY;
	ImageSInt16 derivXX;
	ImageSInt16 derivYY;
	ImageSInt16 derivXY;

	QueueCorner corners;

	ImagePanel panel;

	public VideoDetectCornersIntensity_I8(SimpleImageSequence<ImageUInt8> sequence,
									   GeneralFeatureDetector<ImageUInt8, ImageSInt16> detector) {
		super(sequence);

		this.detector = detector;
	}


	@Override
	public void processFrame(ImageUInt8 image) {

		if( detector.getRequiresGradient() ) {
			if (derivX == null) {
				derivX = new ImageSInt16(image.width, image.height);
				derivY = new ImageSInt16(image.width, image.height);
			}

			// compute the image gradient
			GradientSobel.process(image, derivX, derivY, GecvDefaults.DERIV_BORDER_I32);
		}

		if( detector.getRequiresHessian() ) {
			if (derivXX == null) {
				derivXX = new ImageSInt16(image.width, image.height);
				derivYY = new ImageSInt16(image.width, image.height);
				derivXY = new ImageSInt16(image.width, image.height);
			}

			// compute the image gradient
			HessianThree.process(image, derivXX, derivYY,derivXY,GecvDefaults.DERIV_BORDER_I32);
		}

		detector.process(image,derivX, derivY, derivXX , derivYY, derivXY);
		corners = detector.getFeatures();
	}

	@Override
	public void updateGUI(BufferedImage guiImage, ImageUInt8 origImage) {
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

	public static void main(String args[]) {
		String fileName;

		if (args.length == 0) {
			fileName = "/home/pja/uav_video.avi";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageUInt8> sequence = new XugglerSimplified<ImageUInt8>(fileName, ImageUInt8.class);

		ImageBase<?> image = sequence.next();

		int maxCorners = 200;
		int radius = 2;
		int width = image.width;
		int height = image.height;

		GeneralFeatureIntensity<ImageUInt8,ImageSInt16> intensity = new WrapperGradientCornerIntensity<ImageUInt8,ImageSInt16>(FactoryPointIntensityAlg.createKlt(ImageSInt16.class , radius));
//		GeneralFeatureIntensity<ImageUInt8, ImageSInt16> intensity =
//				new WrapperFastCornerIntensity<ImageUInt8, ImageSInt16>(FactoryPointIntensityAlg.createFast12_I8(width, height, 8 , 12));

		FeatureExtractor extractor = new WrapperNonMax(new FastNonMaxExtractor(radius + 10, radius + 10, 10f));
//		FeatureExtractor extractor = new WrapperNonMax( new NonMaxExtractorNaive(radius+10,10f));
//		FeatureExtractor extractor = new WrapperNonMaxCandidate(new NonMaxCandidateExtractor(radius+10, 10f));

		GeneralFeatureDetector<ImageUInt8, ImageSInt16> detector = new GeneralFeatureDetector<ImageUInt8, ImageSInt16>(intensity, extractor, maxCorners);


		VideoDetectCornersIntensity_I8 display = new VideoDetectCornersIntensity_I8(sequence, detector);

		display.process();
	}
}
