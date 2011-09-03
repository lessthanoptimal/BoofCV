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

package boofcv.alg.feature.detect;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.orientation.OrientationNoGradient;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ProcessImageSequence;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.xuggler.XugglerSimplified;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class VideoDetectInterestPoints<T extends ImageBase>
		extends ProcessImageSequence<T> {

	InterestPointDetector<T> detector;
	OrientationNoGradient<T> orientation;
	FancyInterestPointRender render = new FancyInterestPointRender();

	ImagePanel panel;

	public VideoDetectInterestPoints(SimpleImageSequence<T> sequence,
									 InterestPointDetector<T> detector ,
									 OrientationNoGradient<T> orientation ) {
		super(sequence);

		this.detector = detector;
		this.orientation = orientation;
	}


	@Override
	public void processFrame(T image) {
		detector.detect(image);
	}

	@Override
	public void updateGUI(BufferedImage guiImage, T origImage) {
		Graphics2D g2 = guiImage.createGraphics();

		if( orientation != null )
			orientation.setImage(origImage);

		render.reset();
		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_I32 pt = detector.getLocation(i);
			double scale = detector.getScale(i);

			int radius = (int)(3*scale);

			if( orientation != null ) {
				orientation.setScale(scale);
				double angle = orientation.compute(pt.x,pt.y);
				render.addCircle(pt.x,pt.y,radius,Color.red,angle);
			} else {
				render.addCircle(pt.x,pt.y,radius);
			}
		}
		render.draw(g2);
		
		if (panel == null) {
			panel = ShowImages.showWindow(guiImage, "Image Sequence");
			addComponent(panel);
		} else {
			panel.setBufferedImage(guiImage);
			panel.repaint();
		}
	}

	public static <T extends ImageBase, D extends ImageBase>
	void perform( String fileName , Class<T> imageType , Class<D> derivType )
	{
		SimpleImageSequence<T> sequence = new XugglerSimplified<T>(fileName, imageType);

		int maxCorners = 200;
		int radius = 2;

		// if null then no orientation will be computed
		OrientationNoGradient<T> orientation = null;
		orientation = FactoryOrientationAlgs.nogradient(radius,imageType);

		InterestPointDetector<T> detector;

		detector = FactoryInterestPoint.fromFastHessian(100,9,4,4);
//		FeatureScaleSpace<T,D> feature = FactoryInterestPointAlgs.hessianScaleSpace(radius,1,maxCorners,imageType,derivType);
//		detector = FactoryInterestPoint.fromFeature(feature,new double[]{1,2,4,6,8,12},imageType);

		VideoDetectInterestPoints<T> display = new VideoDetectInterestPoints<T>(sequence, detector,orientation);

		display.process();
	}

	public static void main(String args[]) {
		String fileName;

		if (args.length == 0) {
			fileName = "/media/backup/datasets/2010/snow_videos/snow_norail_stabilization.avi";
		} else {
			fileName = args[0];
		}

//		perform(fileName,ImageUInt8.class,ImageSInt16.class);
		perform(fileName, ImageFloat32.class,ImageFloat32.class);
	}
}
