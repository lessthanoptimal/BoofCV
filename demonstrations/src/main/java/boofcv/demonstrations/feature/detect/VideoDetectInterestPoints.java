/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.detect;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.orientation.OrientationImageAverage;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ProcessImageSequence;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.BoofVideoManager;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VideoDetectInterestPoints<T extends ImageGray<T>> extends ProcessImageSequence<T> {

	InterestPointDetector<T> detector;
	OrientationImageAverage<T> orientation;
	FancyInterestPointRender render = new FancyInterestPointRender();

	ImagePanel panel;

	public VideoDetectInterestPoints( SimpleImageSequence<T> sequence,
									  InterestPointDetector<T> detector,
									  OrientationImageAverage<T> orientation ) {
		super(sequence);

		this.detector = detector;
		this.orientation = orientation;
	}

	@Override
	public void processFrame( T image ) {
		detector.detect(image);
	}

	@Override
	public void updateGUI( BufferedImage guiImage, T origImage ) {
		Graphics2D g2 = guiImage.createGraphics();

		if (orientation != null)
			orientation.setImage(origImage);

		render.reset();
		for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
			Point2D_F64 pt = detector.getLocation(i);

			int radius = (int)Math.round(detector.getRadius(i));

			if (orientation != null) {
				orientation.setObjectRadius(radius);
				double angle = orientation.compute(pt.x, pt.y);
				render.addCircle((int)pt.x, (int)pt.y, radius, Color.red, angle);
			} else {
				render.addCircle((int)pt.x, (int)pt.y, radius);
			}
		}
		render.draw(g2);

		if (panel == null) {
			panel = ShowImages.showWindow(guiImage, "Image Sequence", true);
			addComponent(panel);
		} else {
			panel.setImage(guiImage);
			panel.repaint();
		}
	}

	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	void perform( String fileName, Class<T> imageType, Class<D> derivType ) {
		SimpleImageSequence<T> sequence = BoofVideoManager.loadManagerDefault().load(fileName, ImageType.single(imageType));
		Objects.requireNonNull(sequence);

//		int maxCorners = 200;
		int radius = 2;

		// if null then no orientation will be computed
		OrientationImageAverage<T> orientation =
				FactoryOrientationAlgs.nogradient(1.0/2.0, radius, imageType);

		InterestPointDetector<T> detector;

		detector = FactoryInterestPoint.fastHessian(new ConfigFastHessian(1, 2, 100, 2, 9, 4, 4), imageType);
//		FeatureScaleSpace<T,D> feature = FactoryInterestPointAlgs.hessianScaleSpace(radius,1,maxCorners,defaultType,derivType);
//		detector = FactoryInterestPoint.wrapDetector(feature,new double[]{1,2,4,6,8,12},defaultType);

		var display = new VideoDetectInterestPoints<>(sequence, detector, orientation);

		display.process();
	}

	public static void main( String[] args ) {
		String fileName;

		if (args.length == 0) {
			fileName = UtilIO.pathExample("zoom.mjpeg");
		} else {
			fileName = args[0];
		}

//		perform(fileName,GrayU8.class,GrayS16.class);
		perform(fileName, GrayF32.class, GrayF32.class);
	}
}
