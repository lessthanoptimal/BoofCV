/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.UtilFeature;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

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
		extends SelectAlgorithmAndInputPanel {

	InterestPointDetector<T> detector;
	DescribeRegionPoint describe;
	AssociateDescription<TupleDesc> matcher;
	OrientationImage<T> orientation;

	MultiSpectral<T> imageLeft;
	MultiSpectral<T> imageRight;
	T grayLeft;
	T grayRight;

	Class<T> imageType;

	// which type of association it should perform
	boolean associateBackwards;

	AssociationPanel panel = new AssociationPanel(20);

	boolean processedImage = false;
	// tells the progress monitor how far along it is
	volatile int progress;

	public VisualizeAssociationMatchesApp(Class<T> imageType, Class<D> derivType) {
		super(3);
		this.imageType = imageType;

		GeneralFeatureDetector<T, D> alg;

		addAlgorithm(0, "Fast Hessian",FactoryInterestPoint.fastHessian(new ConfigFastHessian( 1, 2, 200, 1, 9, 4, 4)));
		if( imageType == ImageFloat32.class )
			addAlgorithm(0, "SIFT", FactoryInterestPoint.siftDetector(null,new ConfigSiftDetector(2,5,200,5)));
		alg = FactoryDetectPoint.createShiTomasi(new ConfigGeneralDetector(500,2,1), false, derivType);
		addAlgorithm(0, "Shi-Tomasi", FactoryInterestPoint.wrapPoint(alg, 1, imageType, derivType));

		addAlgorithm(1, "SURF-S", FactoryDescribeRegionPoint.surfStable(null, imageType));
		addAlgorithm(1, "SURF-S Color", FactoryDescribeRegionPoint.surfColorStable(null, ImageType.ms(3, imageType)));
		if( imageType == ImageFloat32.class )
			addAlgorithm(1, "SIFT", FactoryDescribeRegionPoint.sift(null,null));
		addAlgorithm(1, "BRIEF", FactoryDescribeRegionPoint.brief(new ConfigBrief(true), imageType));
		addAlgorithm(1, "BRIEFSO", FactoryDescribeRegionPoint.brief(new ConfigBrief(false), imageType));
		addAlgorithm(1, "Pixel 11x11", FactoryDescribeRegionPoint.pixel(11, 11, imageType));
		addAlgorithm(1, "NCC 11x11", FactoryDescribeRegionPoint.pixelNCC(11, 11, imageType));

		addAlgorithm(2, "Greedy", false);
		addAlgorithm(2, "Backwards", true);

		// estimate orientation using this once since it is fast and accurate
		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(null, integralType);
		orientation = FactoryOrientation.convertImage(orientationII,imageType);

		imageLeft = new MultiSpectral<T>(imageType,1,1,3);
		imageRight = new MultiSpectral<T>(imageType,1,1,3);
		grayLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		grayRight = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		setMainGUI(panel);
	}

	public void process(final BufferedImage buffLeft, final BufferedImage buffRight) {
		imageLeft.reshape(buffLeft.getWidth(), buffLeft.getHeight());
		imageRight.reshape(buffRight.getWidth(), buffRight.getHeight());
		grayLeft.reshape(buffLeft.getWidth(), buffLeft.getHeight());
		grayRight.reshape(buffRight.getWidth(), buffRight.getHeight());

		ConvertBufferedImage.convertFromMulti(buffLeft, imageLeft, true, imageType);
		ConvertBufferedImage.convertFromMulti(buffRight, imageRight, true, imageType);
		GConvertImage.average(imageLeft, grayLeft);
		GConvertImage.average(imageRight, grayRight);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setImages(buffLeft, buffRight);
				processedImage = true;
				doRefreshAll();
			}
		});
	}

	@Override
	public void loadConfigurationFile(String fileName) {
	}

	@Override
	public synchronized void refreshAll(Object[] cookies) {
		detector = (InterestPointDetector<T>) cookies[0];
		describe = (DescribeRegionPoint<T, TupleDesc>) cookies[1];
		associateBackwards = (Boolean)cookies[2];
		matcher = createMatcher();

		processImage();
	}

	private AssociateDescription createMatcher() {
		ScoreAssociation scorer = FactoryAssociation.defaultScore(describe.getDescriptionType());
		return FactoryAssociation.greedy(scorer, Double.MAX_VALUE, associateBackwards);
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if (detector == null || describe == null || matcher == null)
			return;

		switch (indexFamily) {
			case 0:
				detector = (InterestPointDetector<T>) cookie;
				break;

			case 1:
				describe = (DescribeRegionPoint<T, TupleDesc>) cookie;
				// need to update association since the descriptor type changed
				matcher = createMatcher();
				break;

			case 2:
				associateBackwards = (Boolean)cookie;
				matcher = createMatcher();
				break;
		}

		processImage();
	}

	private void processImage() {
		final List<Point2D_F64> leftPts = new ArrayList<Point2D_F64>();
		final List<Point2D_F64> rightPts = new ArrayList<Point2D_F64>();
		FastQueue<TupleDesc> leftDesc = UtilFeature.createQueue(describe, 10);
		FastQueue<TupleDesc> rightDesc = UtilFeature.createQueue(describe,10);

		final ProgressMonitor progressMonitor = new ProgressMonitor(this,
				"Associating Features",
				"Detecting Left", 0, 3);

		// show a progress dialog if it is slow.  Needs to be in its own thread so if this stalls
		// the window will pop up
		progress = 0;
		new Thread() {
			public synchronized void run() {
				while (progress < 3) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							progressMonitor.setProgress(progress);
						}
					});
					try {
						wait(100);
					} catch (InterruptedException e) {
					}
				}
				progressMonitor.close();
			}
		}.start();


		// find feature points  and descriptions
		extractImageFeatures(imageLeft,grayLeft, leftDesc, leftPts);
		progress++;
		extractImageFeatures(imageRight,grayRight, rightDesc, rightPts);
		progress++;
		matcher.setSource(leftDesc);
		matcher.setDestination(rightDesc);
		matcher.associate();
		progress = 3;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setAssociation(leftPts, rightPts, matcher.getMatches());
				repaint();
			}
		});
	}

	private void extractImageFeatures(MultiSpectral<T> color , T gray, FastQueue<TupleDesc> descs, List<Point2D_F64> locs) {
		detector.detect(gray);
		if( describe.getImageType().getFamily() == ImageType.Family.SINGLE_BAND )
			describe.setImage(gray);
		else
			describe.setImage(color);
		orientation.setImage(gray);

		if (detector.hasScale()) {
			for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				double scale = detector.getScale(i);
				if (describe.requiresOrientation()) {
					orientation.setScale(scale);
					yaw = orientation.compute(pt.x, pt.y);
				}

				TupleDesc d = descs.grow();
				if (describe.process(pt.x, pt.y, yaw, scale, d)) {
					locs.add(pt.copy());
				} else {
					descs.removeTail();
				}
			}
		} else {
			orientation.setScale(1);
			for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				if (describe.requiresOrientation()) {
					yaw = orientation.compute(pt.x, pt.y);
				}

				TupleDesc d = descs.grow();
				if (describe.process(pt.x, pt.y, yaw, 1, d)) {
					locs.add(pt.copy());
				} else {
					descs.removeTail();
				}
			}
		}
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage left = media.openImage(inputRefs.get(index).getPath(0));
		BufferedImage right = media.openImage(inputRefs.get(index).getPath(1));

		process(left, right);
	}

	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main(String args[]) {
		Class imageType = ImageFloat32.class;
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		VisualizeAssociationMatchesApp app = new VisualizeAssociationMatchesApp(imageType, derivType);

		List<PathLabel> inputs = new ArrayList<PathLabel>();

		inputs.add(new PathLabel("Cave", "../data/evaluation/stitch/cave_01.jpg", "../data/evaluation/stitch/cave_02.jpg"));
		inputs.add(new PathLabel("Kayak", "../data/evaluation/stitch/kayak_02.jpg", "../data/evaluation/stitch/kayak_03.jpg"));
		inputs.add(new PathLabel("Forest", "../data/evaluation/scale/rainforest_01.jpg", "../data/evaluation/scale/rainforest_02.jpg"));
		inputs.add(new PathLabel("Building", "../data/evaluation/stitch/apartment_building_01.jpg", "../data/evaluation/stitch/apartment_building_02.jpg"));
		inputs.add(new PathLabel("Trees Rotate", "../data/evaluation/stitch/trees_rotate_01.jpg", "../data/evaluation/stitch/trees_rotate_03.jpg"));

		app.setPreferredSize(new Dimension(1000, 500));
		app.setSize(1000, 500);
		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Associated Features");
	}
}
