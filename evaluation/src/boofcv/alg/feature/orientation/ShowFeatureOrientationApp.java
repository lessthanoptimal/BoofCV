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

package boofcv.alg.feature.orientation;

import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.transform.gss.UtilScaleSpace;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.SelectAlgorithmPanel;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays a window showing the selected corner-laplace features across diffferent scale spaces.
 *
 * @author Peter Abeles
 */
public class ShowFeatureOrientationApp <T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmPanel {

//	static String fileName = "evaluation/data/outdoors01.jpg";
//	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
	static String fileName = "evaluation/data/shapes01.png";

	ImagePanel panel;

	static int NUM_FEATURES = 500;

	int radius = 10;

	BufferedImage input;
	Class<T> imageType;
	Class<D> derivType;

	public ShowFeatureOrientationApp(BufferedImage input, Class<T> imageType, Class<D> derivType) {
		this.input = input;
		this.imageType = imageType;
		this.derivType = derivType;

		addAlgorithm("No Gradient", FactoryOrientationAlgs.nogradient(radius,imageType));
		addAlgorithm("Average", FactoryOrientationAlgs.average(radius,false,derivType));
		addAlgorithm("Average Weighted", FactoryOrientationAlgs.average(radius,true,derivType));
		addAlgorithm("Histogram 10", FactoryOrientationAlgs.histogram(10,radius,false,derivType));
		addAlgorithm("Histogram 10 Weighted", FactoryOrientationAlgs.histogram(10,radius,true,derivType));
		addAlgorithm("Sliding Window", FactoryOrientationAlgs.sliding(20,Math.PI/3.0,radius,false,derivType));
		addAlgorithm("Sliding Window Weighted", FactoryOrientationAlgs.sliding(20,Math.PI/3.0,radius,true,derivType));
		addAlgorithm("Average II", FactoryOrientationAlgs.average_ii(radius,false,imageType));
		addAlgorithm("Average II Weighted", FactoryOrientationAlgs.average_ii(radius,true,imageType));

		panel = new ImagePanel(input);
		add(panel, BorderLayout.CENTER);
	}

	@Override
	public void setActiveAlgorithm(String name, Object cookie) {
		RegionOrientation orientation = (RegionOrientation)cookie;

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

		QueueCorner points = detector.getFeatures();

		FancyInterestPointRender render = new FancyInterestPointRender();

		if( orientation instanceof OrientationGradient ) {
			((OrientationGradient<D>)orientation).setImage(deriv.getDerivative(true),deriv.getDerivative(false));
		} else if( orientation instanceof OrientationIntegral ) {
			T ii = GIntegralImageOps.transform(workImage,null);
			((OrientationIntegral<T>)orientation).setImage(ii);
		} else if( orientation instanceof OrientationNoGradient ) {
			((OrientationNoGradient)orientation).setImage((ImageFloat32)workImage);
		} else {
			throw new IllegalArgumentException("Unknown algorithm type.");
		}

		for( int i = 0; i < points.num; i++ ) {
			Point2D_I16 p = points.get(i);
			double angle = orientation.compute(p.x,p.y);
			render.addCircle(p.x,p.y,radius, Color.RED,angle);
		}

		BufferedImage temp = new BufferedImage(input.getWidth(),input.getHeight(),input.getType());
		Graphics2D g2 = (Graphics2D)temp.getGraphics();

		g2.drawImage(input,0,0,null);
		render.draw(g2);
		panel.setBufferedImage(temp);
		panel.repaint();
	}

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);

		ShowFeatureOrientationApp<ImageFloat32,ImageFloat32> app =
				new ShowFeatureOrientationApp<ImageFloat32, ImageFloat32>(input,ImageFloat32.class, ImageFloat32.class);
//		ShowFeatureOrientationApp<ImageUInt8, ImageSInt16> app =
//				new ShowFeatureOrientationApp<ImageUInt8,ImageSInt16>(input,ImageUInt8.class, ImageSInt16.class);

		ShowImages.showWindow(app,"Feature Orientation");
//		input = UtilImageIO.loadImage(fileName);
//		doStuff(input, ImageUInt8.class, ImageSInt16.class);
		System.out.println("Done");
	}
}
