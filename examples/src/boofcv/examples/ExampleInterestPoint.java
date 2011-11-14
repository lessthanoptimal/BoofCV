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

package boofcv.examples;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Demonstrates how to detect interest points using the easy to use {@link InterestPointDetector} interface.
 * InterestPointDetector makes it easy to switch between algorithms, but due to its abstraction it also cause
 * calculations to be done multiple times.  For example, if a detector requires the gradient and so does the description
 * algorithm then that calculation will have to be done twice when using this interface.  If the algorithms had
 * been used directly then extra calculations could be avoided.
 *
 * @author Peter Abeles
 */
public class ExampleInterestPoint {

	public static <T extends ImageBase>
	void detect( BufferedImage image , Class<T> imageType ) {
		T input = ConvertBufferedImage.convertFrom(image,null,imageType);

		// create a Fast Hessian detector from the SURF paper.
		// Other detectors can be used in this example too.
		InterestPointDetector<T> detector = FactoryInterestPoint.fromFastHessian(10, 2, 100,2,9,3,4);

		// find interest points in the image
		detector.detect(input);

		// Draw the features
		Graphics2D g2 = image.createGraphics();
		FancyInterestPointRender render = new FancyInterestPointRender();

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_F64 pt = detector.getLocation(i);

			// note how it checks the capabilities of the detector
			if( detector.hasScale() ) {
				double scale = detector.getScale(i);
				render.addCircle((int)pt.x,(int)pt.y,(int)(scale*2.5));
			} else {
				render.addPoint((int) pt.x, (int) pt.y);
			}
		}
		// make the circle's thicker
		g2.setStroke(new BasicStroke(3));

		// just draw the features onto the input image
		render.draw(g2);
		ShowImages.showWindow(image,"Detected Features");
	}

	public static void main( String args[] ) {
		BufferedImage image = UtilImageIO.loadImage("../evaluation/data/sunflowers.png");
		detect(image, ImageFloat32.class);
	}
}
