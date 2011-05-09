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

package gecv.alg.detect;

import gecv.abst.detect.corner.GeneralCornerDetector;
import gecv.abst.detect.corner.GeneralCornerIntensity;
import gecv.abst.detect.corner.WrapperFastCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.WrapperNonMaxCandidate;
import gecv.alg.detect.corner.FactoryCornerIntensity;
import gecv.alg.detect.extract.NonMaxCornerCandidateExtractor;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.io.wrapper.xuggler.XugglerSimplified;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import pja.geometry.struct.point.Point2D_I16;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class VideoDetectCornersIntensity extends ProcessImageSequence<ImageInt8> {

	GeneralCornerDetector<ImageInt8,ImageInt16> detector;
	ImageInt16 derivX;
	ImageInt16 derivY;

	QueueCorner corners;

	ImagePanel panel;

	public VideoDetectCornersIntensity(SimpleImageSequence<ImageInt8> sequence,
									   GeneralCornerDetector<ImageInt8,ImageInt16> detector) {
		super(sequence);

		this.detector = detector;
	}


	@Override
	public void processFrame(ImageInt8 image) {

		if( detector.getRequiresGradient() ) {
			if (derivX == null) {
				derivX = new ImageInt16(image.width, image.height, true);
				derivY = new ImageInt16(image.width, image.height, true);
			}

			// compute the image gradient
			GradientSobel.process_I8(image, derivX, derivY);
		}

		detector.process(image,derivX, derivY);
		corners = detector.getCorners();
	}

	@Override
	public void updateGUI(BufferedImage guiImage, ImageInt8 origImage) {
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
		SimpleImageSequence<ImageInt8> sequence = new XugglerSimplified<ImageInt8>(fileName, ImageInt8.class);

		ImageBase<?> image = sequence.next();

		int maxCorners = 200;
		int radius = 2;
		int width = image.width;
		int height = image.height;

//		GeneralCornerIntensity<ImageInt8,ImageInt16> intensity = new WrapperGradientCornerIntensity<ImageInt8,ImageInt16>(FactoryCornerIntensity.createKlt_I16(width, height, radius));
		GeneralCornerIntensity<ImageInt8,ImageInt16> intensity =
				new WrapperFastCornerIntensity<ImageInt8,ImageInt16>(FactoryCornerIntensity.createFast12_I8(width, height, 10 , 12));

//		CornerExtractor extractor = new WrapperNonMax(new FastNonMaxCornerExtractor(radius + 10, radius + 10, 10f));
//		CornerExtractor extractor = new WrapperNonMax( new NonMaxCornerExtractorNaive(radius+10,10f));
		CornerExtractor extractor = new WrapperNonMaxCandidate(new NonMaxCornerCandidateExtractor(radius+10, 10f));

		GeneralCornerDetector<ImageInt8,ImageInt16> detector = new GeneralCornerDetector<ImageInt8,ImageInt16>(intensity, extractor, maxCorners);


		VideoDetectCornersIntensity display = new VideoDetectCornersIntensity(sequence, detector);

		display.process();
	}
}
