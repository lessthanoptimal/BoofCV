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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryBlobDetector;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays a window showing the selected corner-laplace features across different scale spaces.
 *
 * @author Peter Abeles
 */
public class DetectFeaturePointApp<T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmImagePanel implements ProcessInput
{

	static int maxFeatures = 400;
	static int maxScaleFeatures = maxFeatures/3;
	
	public double[] scales = new double[]{1,1.5,2,3,4,6,8,12};
	int radius = 2;
	float thresh = 1;
	T grayImage;
	Class<T> imageType;
	boolean processImage = false;
	BufferedImage input;
	BufferedImage workImage;
	FancyInterestPointRender render = new FancyInterestPointRender();

	ImagePanel panel;

	public DetectFeaturePointApp( Class<T> imageType , Class<D> derivType ) {
		super(1);
		this.imageType = imageType;

		GeneralFeatureDetector<T,D> alg;

		alg = FactoryCornerDetector.createHarris(radius,thresh,maxFeatures,derivType);
		addAlgorithm(0, "Harris",FactoryInterestPoint.fromCorner(alg,imageType,derivType));
		alg = FactoryCornerDetector.createKlt(radius,thresh,maxFeatures,derivType);
		addAlgorithm(0, "KLT", FactoryInterestPoint.fromCorner(alg,imageType,derivType));
		alg = FactoryCornerDetector.createFast(radius,10,maxFeatures,imageType);
		addAlgorithm(0, "Fast", FactoryInterestPoint.fromCorner(alg,imageType,derivType));
		alg = FactoryCornerDetector.createKitRos(radius,thresh,maxFeatures,derivType);
		addAlgorithm(0, "KitRos",FactoryInterestPoint.fromCorner(alg,imageType,derivType));
		alg = FactoryCornerDetector.createMedian(radius,thresh,maxFeatures,imageType);
		addAlgorithm(0, "Median",FactoryInterestPoint.fromCorner(alg,imageType,derivType));
		alg = FactoryBlobDetector.createLaplace(radius,thresh,maxFeatures,derivType, HessianBlobIntensity.Type.DETERMINANT);
		addAlgorithm(0, "Hessian",FactoryInterestPoint.fromCorner(alg,imageType,derivType));
		alg = FactoryBlobDetector.createLaplace(radius,thresh,maxFeatures,derivType, HessianBlobIntensity.Type.TRACE);
		addAlgorithm(0, "Laplace",FactoryInterestPoint.fromCorner(alg,imageType,derivType));

		FeatureLaplaceScaleSpace<T,D> flss = FactoryInterestPointAlgs.hessianLaplace(radius,thresh,maxScaleFeatures,imageType,derivType);
		addAlgorithm(0, "Hess Lap SS",FactoryInterestPoint.fromFeatureLaplace(flss,scales,imageType));
		FeatureLaplacePyramid<T,D> flp = FactoryInterestPointAlgs.hessianLaplacePyramid(radius,thresh,maxScaleFeatures,imageType,derivType);
		addAlgorithm(0, "Hess Lap P",FactoryInterestPoint.fromFeatureLaplace(flp,scales,imageType));
		addAlgorithm(0, "FastHessian",FactoryInterestPoint.<T>fromFastHessian(maxScaleFeatures,9,4,4));

		panel = new ImagePanel();
		setMainGUI(panel);
	}

	public void process( BufferedImage input ) {
		setInputImage(input);
		this.input = input;
		grayImage = ConvertBufferedImage.convertFrom(input,null,imageType);
		workImage = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_BGR);
		panel.setBufferedImage(workImage);
		doRefreshAll();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(workImage.getWidth(), workImage.getHeight()));
				processImage = true;
			}});
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( input == null )
			return;

		final InterestPointDetector<T> det = (InterestPointDetector<T>)cookie;
		det.detect(grayImage);

		render.reset();
		if( det.hasScale() ) {
			for( int i = 0; i < det.getNumberOfFeatures(); i++ ) {
				Point2D_I32 p = det.getLocation(i);
				int radius = (int)Math.ceil(det.getScale(i)*3);
				render.addCircle(p.x,p.y,radius);
			}
		} else {
			for( int i = 0; i < det.getNumberOfFeatures(); i++ ) {
				Point2D_I32 p = det.getLocation(i);
				render.addPoint(p.x,p.y,3,Color.RED);
			}
		}

		Graphics2D g2 = workImage.createGraphics();
		g2.drawImage(input,0,0,grayImage.width,grayImage.height,null);
		render.draw(g2);
		panel.repaint();
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processImage;
	}

	public static void main( String args[] ) {
		DetectFeaturePointApp app = new DetectFeaturePointApp(ImageFloat32.class,ImageFloat32.class);

		ImageListManager manager = new ImageListManager();
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");
		manager.add("beach","data/scale/beach02.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Point Feature");

		System.out.println("Done");
	}
}
