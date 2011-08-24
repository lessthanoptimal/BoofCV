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

package gecv.alg.feature.orientation;

import gecv.abst.filter.derivative.AnyImageDerivative;
import gecv.alg.feature.detect.interest.GeneralFeatureDetector;
import gecv.alg.transform.gss.UtilScaleSpace;
import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.inst.FactoryImageGenerator;
import gecv.factory.feature.describe.FactoryRegionOrientationAlgs;
import gecv.factory.feature.detect.interest.FactoryCornerDetector;
import gecv.gui.SelectAlgorithmPanel;
import gecv.gui.feature.FancyInterestPointRender;
import gecv.gui.image.ImagePanel;
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

		addAlgorithm("Average",FactoryRegionOrientationAlgs.average(radius,false,derivType));
		addAlgorithm("Average Weighted",FactoryRegionOrientationAlgs.average(radius,true,derivType));
		addAlgorithm("Histogram 10",FactoryRegionOrientationAlgs.histogram(10,radius,false,derivType));
		addAlgorithm("Histogram 10 Weighted",FactoryRegionOrientationAlgs.histogram(10,radius,true,derivType));
		addAlgorithm("Sliding Window",FactoryRegionOrientationAlgs.sliding(20,Math.PI/3.0,radius,false,derivType));
		addAlgorithm("Sliding Window Weighted",FactoryRegionOrientationAlgs.sliding(20,Math.PI/3.0,radius,true,derivType));
		addAlgorithm("Average Haar II",FactoryRegionOrientationAlgs.average_ii(radius,false));
		addAlgorithm("Average Haar II Weighted",FactoryRegionOrientationAlgs.average_ii(radius,true));

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

//		ShowFeatureOrientationApp<ImageFloat32,ImageFloat32> app =
//				new ShowFeatureOrientationApp<ImageFloat32,ImageFloat32>(input,ImageFloat32.class, ImageFloat32.class);
		ShowFeatureOrientationApp<ImageUInt8, ImageSInt16> app =
				new ShowFeatureOrientationApp<ImageUInt8,ImageSInt16>(input,ImageUInt8.class, ImageSInt16.class);

		ShowImages.showWindow(app,"Feature Orientation");
//		input = UtilImageIO.loadImage(fileName);
//		doStuff(input, ImageUInt8.class, ImageSInt16.class);
		System.out.println("Done");
	}
}
