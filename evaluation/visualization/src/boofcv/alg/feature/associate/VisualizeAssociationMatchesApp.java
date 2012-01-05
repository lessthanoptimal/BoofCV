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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.orientation.OrientationImageAverage;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.TupleDescQueue;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;



/**
 * Visually shows the location of matching pairs of associated features in two images.
 *
 * @author Peter Abeles
 */
// todo add waiting tool bar
// todo show partial results
public class VisualizeAssociationMatchesApp<T extends ImageSingleBand, D extends ImageSingleBand>
	extends SelectAlgorithmImagePanel implements ProcessInput
{
	int maxMatches = 200;

	InterestPointDetector<T> detector;
	DescribeRegionPoint<T> describe;
	GeneralAssociation<TupleDesc_F64> matcher;
	OrientationImageAverage<T> orientation;

	T imageLeft;
	T imageRight;
	Class<T> imageType;

	AssociationPanel panel = new AssociationPanel(20);

	boolean processedImage = false;

	public VisualizeAssociationMatchesApp( Class<T> imageType , Class<D> derivType )
	{
		super(3);
		this.imageType = imageType;

		GeneralFeatureDetector<T,D> alg;
		addAlgorithm(0,"Fast Hessian",FactoryInterestPoint.fastHessian(1, 2, 200, 1, 9, 4, 4));
		alg = FactoryCornerDetector.createKlt(2,1,500,derivType);
		addAlgorithm(0,"KLT",FactoryInterestPoint.wrapCorner(alg, imageType, derivType));

		addAlgorithm(1,"SURF", FactoryDescribeRegionPoint.surf(true, imageType));
		addAlgorithm(1,"BRIEF", FactoryDescribeRegionPoint.brief(16, 512, -1, 4, true, imageType));
		addAlgorithm(1,"BRIEFO", FactoryDescribeRegionPoint.brief(16, 512, -1, 4, false, imageType));
		addAlgorithm(1,"Gaussian 12", FactoryDescribeRegionPoint.gaussian12(20, imageType, derivType));
		addAlgorithm(1,"Gaussian 14", FactoryDescribeRegionPoint.steerableGaussian(20, false, imageType, derivType));
		addAlgorithm(1,"Pixel 5x5", FactoryDescribeRegionPoint.pixel(5,5, imageType));
		addAlgorithm(1,"NCC 5x5", FactoryDescribeRegionPoint.pixelNCC(5,5, imageType));

		ScoreAssociation<TupleDesc_F64> scorer = new ScoreAssociateEuclideanSq();

		addAlgorithm(2,"Greedy", FactoryAssociation.greedy(scorer, Double.MAX_VALUE, maxMatches, false));
		addAlgorithm(2,"Backwards", FactoryAssociation.greedy(scorer, Double.MAX_VALUE, maxMatches, true));

		orientation = FactoryOrientationAlgs.nogradient(5,imageType);

		imageLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		imageRight = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		setMainGUI(panel);
	}

	public void process( final BufferedImage buffLeft , final BufferedImage buffRight ) {
		imageLeft.reshape(buffLeft.getWidth(),buffLeft.getHeight());
		imageRight.reshape(buffRight.getWidth(),buffRight.getHeight());

		ConvertBufferedImage.convertFromSingle(buffLeft, imageLeft, imageType);
		ConvertBufferedImage.convertFromSingle(buffRight, imageRight, imageType);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setImages(buffLeft,buffRight);
				doRefreshAll();
				processedImage = true;
			}});
	}

	@Override
	public synchronized void refreshAll(Object[] cookies) {
		detector = (InterestPointDetector<T>)cookies[0];
		describe = (DescribeRegionPoint<T>)cookies[1];
		matcher = (GeneralAssociation<TupleDesc_F64>)cookies[2];

		processImage();
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( detector == null || describe == null || matcher == null )
			return;

		switch( indexFamily ) {
			case 0:
				detector = (InterestPointDetector<T>)cookie;
				break;

			case 1:
				describe = (DescribeRegionPoint<T>)cookie;
				break;

			case 2:
				matcher = (GeneralAssociation<TupleDesc_F64>)cookie;
				break;
		}

		processImage();
	}

	private void processImage() {
		final List<Point2D_F64> leftPts = new ArrayList<Point2D_F64>();
		final List<Point2D_F64> rightPts = new ArrayList<Point2D_F64>();
		TupleDescQueue leftDesc = new TupleDescQueue(describe.getDescriptionLength(), true);
		TupleDescQueue rightDesc = new TupleDescQueue(describe.getDescriptionLength(), true);

		// find feature points  and descriptions
		extractImageFeatures(imageLeft,leftDesc,leftPts);
		extractImageFeatures(imageRight,rightDesc,rightPts);

		matcher.associate(leftDesc,rightDesc);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setAssociation(leftPts,rightPts,matcher.getMatches());
				repaint();
			}});
	}

	private void extractImageFeatures( T image , FastQueue<TupleDesc_F64> descs , List<Point2D_F64> locs ) {
		detector.detect(image);
		describe.setImage(image);
		orientation.setImage(image);

		if( detector.hasScale() ) {
			for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				double scale = detector.getScale(i);
				if( describe.requiresOrientation() ) {
					orientation.setRadius((int)(describe.getCanonicalRadius()*scale));
					yaw = orientation.compute(pt.x,pt.y);
				}

				TupleDesc_F64 d = describe.process(pt.x,pt.y,yaw,scale,null);
				if( d != null ) {
					descs.pop().set(d.value);
					locs.add( pt.copy());
				}
			}
		} else {
			orientation.setRadius(describe.getCanonicalRadius());
			for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				if( describe.requiresOrientation() ) {
					yaw = orientation.compute(pt.x,pt.y);
				}

				TupleDesc_F64 d = describe.process(pt.x,pt.y,yaw,1,null);
				if( d != null ) {
					descs.pop().set(d.value);
					locs.add( pt.copy());
				}
			}
		}
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager m = getInputManager();
		BufferedImage left = m.loadImage(index,0);
		BufferedImage right = m.loadImage(index,1);

		process(left,right);
	}

	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main( String args[] ) {
		Class imageType = ImageFloat32.class;
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		VisualizeAssociationMatchesApp app = new VisualizeAssociationMatchesApp(imageType,derivType);

		ImageListManager manager = new ImageListManager();
		manager.add("Cave","../data/evaluation/stitch/cave_01.jpg","../data/evaluation/stitch/cave_02.jpg");
		manager.add("Kayak","../data/evaluation/stitch/kayak_02.jpg","../data/evaluation/stitch/kayak_03.jpg");
		manager.add("Forest","../data/evaluation/scale/rainforest_01.jpg","../data/evaluation/scale/rainforest_02.jpg");
		manager.add("Building","../data/evaluation/stitch/apartment_building_01.jpg","../data/evaluation/stitch/apartment_building_02.jpg");
		manager.add("Trees Rotate","../data/evaluation/stitch/trees_rotate_01.jpg","../data/evaluation/stitch/trees_rotate_03.jpg");

		app.setPreferredSize(new Dimension(1000,500));
		app.setSize(1000,500);
		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Association Relative Score");

	}
}
