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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.orientation.OrientationImageAverage;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryCornerDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.feature.AssociationScorePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * Shows how tightly focused the score is around the best figure by showing the relative
 * size and number of features which have a similar score visually in the image.  For example,
 * lots of other featurse with similar sized circles means the distribution is spread widely
 * while only one or two small figures means it is very narrow.
 *
 * @author Peter Abeles
 */
public class VisualizeAssociationScoreApp<T extends ImageBase, D extends ImageBase>
	extends SelectAlgorithmImagePanel implements ProcessInput
{

	// These classes process the input images and compute association score
	InterestPointDetector<T> detector;
	DescribeRegionPoint<T> describe;
	ScoreAssociation<TupleDesc_F64> scorer;
	OrientationImageAverage<T> orientation;

	// gray scale versions of input image
	T imageLeft;
	T imageRight;
	Class<T> imageType;

	// visualizes association score
	AssociationScorePanel<TupleDesc_F64> scorePanel;

	// has the image been processed yet
	boolean processedImage = false;

	public VisualizeAssociationScoreApp( Class<T> imageType,
										 Class<D> derivType )
	{
		super(3);
		this.imageType = imageType;

		imageLeft = GeneralizedImageOps.createImage(imageType,1,1);
		imageRight = GeneralizedImageOps.createImage(imageType,1,1);

		GeneralFeatureDetector<T,D> alg;

		addAlgorithm(0,"Fast Hessian",FactoryInterestPoint.fromFastHessian(1, 200, 1, 9,4,4));
		alg = FactoryCornerDetector.createKlt(2,1,500,derivType);
		addAlgorithm(0,"KLT",FactoryInterestPoint.fromCorner(alg,imageType,derivType));

		addAlgorithm(1,"SURF", FactoryDescribeRegionPoint.surf(true, imageType));
		addAlgorithm(1,"BRIEF", FactoryDescribeRegionPoint.brief(16, 512, -1, 4, true, imageType));
		addAlgorithm(1,"BRIEFO", FactoryDescribeRegionPoint.brief(16, 512, -1, 4, false, imageType));
		addAlgorithm(1,"Gaussian 12", FactoryDescribeRegionPoint.gaussian12(20, imageType, derivType));
		addAlgorithm(1,"Gaussian 14", FactoryDescribeRegionPoint.steerableGaussian(20, false, imageType, derivType));

		addAlgorithm(2,"norm",new ScoreAssociateEuclidean());
		addAlgorithm(2,"norm^2",new ScoreAssociateEuclideanSq());
		addAlgorithm(2,"correlation",new ScoreAssociateCorrelation());

		orientation = FactoryOrientationAlgs.nogradient(5, imageType);

		scorePanel = new AssociationScorePanel<TupleDesc_F64>(3);
		setMainGUI(scorePanel);
	}

	public void process( BufferedImage buffLeft , BufferedImage buffRight ) {
		// copy the input images
		imageLeft.reshape(buffLeft.getWidth(),buffLeft.getHeight());
		imageRight.reshape(buffRight.getWidth(),buffRight.getHeight());

		ConvertBufferedImage.convertFrom(buffLeft,imageLeft,imageType);
		ConvertBufferedImage.convertFrom(buffRight,imageRight,imageType);

		// update the GUI's background images
		scorePanel.setImages(buffLeft,buffRight);

		processedImage = true;

		// tell it to update everything
		doRefreshAll();
	}

	@Override
	public void refreshAll(Object[] cookies) {
		detector = (InterestPointDetector<T>)cookies[0];
		describe = (DescribeRegionPoint<T>)cookies[1];
		scorer = (ScoreAssociation<TupleDesc_F64>)cookies[2];

		processImage();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		switch( indexFamily ) {
			case 0:
				detector = (InterestPointDetector<T>)cookie;
				break;

			case 1:
				describe = (DescribeRegionPoint<T>)cookie;
				break;

			case 2:
				scorer = (ScoreAssociation<TupleDesc_F64>)cookie;
				break;
		}

		processImage();
	}

	/**
	 * Extracts image information and then passes that info onto scorePanel for display.  Data is not
	 * recycled to avoid threading issues.
	 */
	private void processImage() {
		final List<Point2D_F64> leftPts = new ArrayList<Point2D_F64>();
		final List<Point2D_F64> rightPts = new ArrayList<Point2D_F64>();
		final List<TupleDesc_F64> leftDesc = new ArrayList<TupleDesc_F64>();
		final List<TupleDesc_F64> rightDesc = new ArrayList<TupleDesc_F64>();

		final ProgressMonitor progressMonitor = new ProgressMonitor(this,
				"Compute Feature Information",
				"", 0, 4);
		extractImageFeatures(progressMonitor,0,imageLeft,leftDesc,leftPts);
		extractImageFeatures(progressMonitor,2,imageRight,rightDesc,rightPts);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressMonitor.close();
				scorePanel.setScorer(scorer);
				scorePanel.setLocation(leftPts,rightPts,leftDesc,rightDesc);
				repaint();
			}});
	}

	/**
	 * Detects the locations of the features in the image and extracts descriptions of each of
	 * the features.
	 */
	private void extractImageFeatures( final ProgressMonitor progressMonitor , final int progress ,
									   T image ,
									   List<TupleDesc_F64> descs , List<Point2D_F64> locs ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressMonitor.setNote("Detecting");
			}});
		detector.detect(image);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressMonitor.setProgress(progress+1);
				progressMonitor.setNote("Describing");
			}});
		describe.setImage(image);
		orientation.setImage(image);

		// See if the detector can detect the feature's scale
		if( detector.hasScale() ) {
			for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				double scale = detector.getScale(i);
				if( describe.requiresOrientation() ) {
					orientation.setRadius((int)(describe.getRadius()*scale));
					yaw = orientation.compute(pt.x,pt.y);
				}

				TupleDesc_F64 d = describe.process(pt.x,pt.y,yaw,scale,null);
				if( d != null ) {
					descs.add( d.copy() );
					locs.add( pt.copy());
				}
			}
		} else {
			// just set the scale to one in this case
			orientation.setRadius(describe.getRadius());
			for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				if( describe.requiresOrientation() ) {
					yaw = orientation.compute(pt.x,pt.y);
				}

				TupleDesc_F64 d = describe.process(pt.x,pt.y,yaw,1,null);
				if( d != null ) {
					descs.add( d.copy() );
					locs.add( pt.copy());
				}
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressMonitor.setProgress(progress+2);
			}});
	}


	@Override
	public void changeImage(String name, int index) {
		ImageListManager m = getInputManager();
		BufferedImage left = m.loadImage(index,0);
		BufferedImage right = m.loadImage(index,1);

		process(left,right);
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main( String args[] ) {

		Class imageType = ImageFloat32.class;
		Class derivType = ImageFloat32.class;

		VisualizeAssociationScoreApp app = new VisualizeAssociationScoreApp(imageType,derivType);

		ImageListManager manager = new ImageListManager();
		manager.add("Cave","data/stitch/cave_01.jpg","data/stitch/cave_02.jpg");
		manager.add("Kayak","data/stitch/kayak_02.jpg","data/stitch/kayak_03.jpg");
		manager.add("Forest","data/scale/rainforest_01.jpg","data/scale/rainforest_02.jpg");

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
