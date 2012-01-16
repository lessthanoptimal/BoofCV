/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_I16;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays a window showing the selected corner-laplace features across diffferent scale spaces.
 *
 * @author Peter Abeles
 */
public class ShowFeatureOrientationApp <T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectAlgorithmImagePanel implements ProcessInput
{
	ImagePanel panel;

	static int NUM_FEATURES = 500;

	int radius = 10;

	BufferedImage input;
	Class<T> imageType;
	Class<D> derivType;
	// true after it processes one image
	boolean hasProcessed = false;

	public ShowFeatureOrientationApp(Class<T> imageType, Class<D> derivType) {
		super(1);
		this.imageType = imageType;
		this.derivType = derivType;

		addAlgorithm(0, "Pixel", FactoryOrientationAlgs.nogradient(radius,imageType));
		addAlgorithm(0, "Gradient Average", FactoryOrientationAlgs.average(radius,false,derivType));
		addAlgorithm(0, "Gradient Average Weighted", FactoryOrientationAlgs.average(radius,true,derivType));
		addAlgorithm(0, "Gradient Histogram 10", FactoryOrientationAlgs.histogram(10,radius,false,derivType));
		addAlgorithm(0, "Gradient Histogram 10 Weighted", FactoryOrientationAlgs.histogram(10,radius,true,derivType));
		addAlgorithm(0, "Gradient Sliding Window", FactoryOrientationAlgs.sliding(20,Math.PI/3.0,radius,false,derivType));
		addAlgorithm(0, "Gradient Sliding Window Weighted", FactoryOrientationAlgs.sliding(20,Math.PI/3.0,radius,true,derivType));
		addAlgorithm(0, "Integral Average", FactoryOrientationAlgs.average_ii(radius,1,4,0,imageType));
		addAlgorithm(0, "Integral Average Weighted", FactoryOrientationAlgs.average_ii(radius,1,4,-1,imageType));

		panel = new ImagePanel();
		setMainGUI(panel);
	}

	public synchronized void process( final BufferedImage input ) {
		setInputImage(input);

		panel.setBufferedImage(input);
		this.input = input;
		doRefreshAll();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));
				setSize(input.getWidth(),input.getHeight());
				hasProcessed = true;
			}});
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( input == null )
			return;
		
		RegionOrientation orientation = (RegionOrientation)cookie;

		T workImage = ConvertBufferedImage.convertFromSingle(input, null, imageType);
		AnyImageDerivative<T,D> deriv = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));
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
		} else if( orientation instanceof OrientationImageAverage) {
			((OrientationImageAverage)orientation).setImage(workImage);
		} else {
			throw new IllegalArgumentException("Unknown algorithm type.");
		}

		for( int i = 0; i < points.size; i++ ) {
			Point2D_I16 p = points.get(i);
			double angle = orientation.compute(p.x,p.y);
			render.addCircle(p.x,p.y,radius, Color.RED,angle);
		}

		BufferedImage temp = new BufferedImage(input.getWidth(),input.getHeight(),input.getType());
		Graphics2D g2 = (Graphics2D)temp.getGraphics();

		g2.drawImage(input,0,0,null);
		g2.setStroke(new BasicStroke(2.5f));
		render.draw(g2);
		panel.setBufferedImage(temp);
		panel.repaint();
	}

	@Override
	public synchronized void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return hasProcessed;
	}

	public static void main( String args[] ) {
		ShowFeatureOrientationApp<ImageFloat32,ImageFloat32> app =
				new ShowFeatureOrientationApp<ImageFloat32, ImageFloat32>(ImageFloat32.class, ImageFloat32.class);

//		ShowFeatureOrientationApp<ImageUInt8, ImageSInt16> app =
//				new ShowFeatureOrientationApp<ImageUInt8,ImageSInt16>(input,ImageUInt8.class, ImageSInt16.class);

		ImageListManager manager = new ImageListManager();
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");
		manager.add("beach","data/scale/beach02.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		System.out.println("Calling show window");
		ShowImages.showWindow(app,"Feature Orientation");
		System.out.println("Done");
	}
}
