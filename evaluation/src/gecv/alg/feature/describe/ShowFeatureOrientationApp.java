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

package gecv.alg.feature.describe;

import gecv.abst.detect.point.FactoryCornerDetector;
import gecv.abst.detect.point.GeneralFeatureDetector;
import gecv.abst.filter.derivative.AnyImageDerivative;
import gecv.alg.transform.gss.UtilScaleSpace;
import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.inst.FactoryImageGenerator;
import gecv.gui.feature.FancyInterestPointRender;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;
import jgrl.struct.point.Point2D_I16;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays a window showing the selected corner-laplace features across diffferent scale spaces.
 *
 * @author Peter Abeles
 */
public class ShowFeatureOrientationApp {

//	static String fileName = "evaluation/data/outdoors01.jpg";
//	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
	static String fileName = "evaluation/data/shapes01.png";

	static int NUM_FEATURES = 500;

	public static <T extends ImageBase, D extends ImageBase>
	void doStuff( BufferedImage input , Class<T> imageType, Class<D> derivType ) {
		T workImage = ConvertBufferedImage.convertFrom(input,null,imageType);
		AnyImageDerivative<T,D> deriv = UtilScaleSpace.createDerivatives(imageType, FactoryImageGenerator.create(derivType));
		deriv.setInput(workImage);

		int r = 2;
		GeneralFeatureDetector<T,D> detector =  FactoryCornerDetector.createHarris(r,1,NUM_FEATURES,derivType);

		D derivX=null,derivY=null,derivXX=null,derivYY=null,derivXY=null;

		if( detector.getRequiresGradient() ) {
			derivX = deriv.getDerivative(true);
			derivY = deriv.getDerivative(false);
		} else if( detector.getRequiresHessian() ) {
			derivXX = deriv.getDerivative(true,true);
			derivYY = deriv.getDerivative(false,false);
			derivXY = deriv.getDerivative(true,false);
		}
		detector.process(workImage,derivX,derivY,derivXX,derivYY,derivXY);

//		OrientationHistogram<D> orientation =  FactoryRegionOrientationAlgs.histogram(10,1,false,derivType);
		OrientationAverage<D> orientation =  FactoryRegionOrientationAlgs.average(1,false,derivType);

		QueueCorner points = detector.getFeatures();
		System.out.println("Found points: "+points.size());

		FancyInterestPointRender render = new FancyInterestPointRender();

		orientation.setImage(deriv.getDerivative(true),deriv.getDerivative(false));
		for( int i = 0; i < points.num; i++ ) {
			Point2D_I16 p = points.get(i);
			int radius = 10;
			orientation.setRadius(radius);
			double angle = orientation.compute(p.x,p.y);
			render.addCircle(p.x,p.y,radius, Color.RED,angle);
		}

		render.draw(input.createGraphics());
		ShowImages.showWindow(input,"Feature Orientation: "+imageType.getSimpleName());
	}

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);
//		doStuff(input,ImageFloat32.class,ImageFloat32.class);
//		input = UtilImageIO.loadImage(fileName);
		doStuff(input, ImageUInt8.class, ImageSInt16.class);
		System.out.println("Done");
	}
}
