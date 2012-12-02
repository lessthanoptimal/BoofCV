/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDescQueue;
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
		extends SelectAlgorithmAndInputPanel {
	int maxMatches = 200;

	InterestPointDetector<T> detector;
	DescribeRegionPoint<T, TupleDesc> describe;
	GeneralAssociation<TupleDesc> matcher;
	OrientationImage<T> orientation;

	T imageLeft;
	T imageRight;
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

		addAlgorithm(0, "Fast Hessian", FactoryInterestPoint.fastHessian(1, 2, 200, 1, 9, 4, 4));
		if( imageType == ImageFloat32.class )
			addAlgorithm(0, "SIFT", FactoryInterestPoint.siftDetector(1.6,5,4,false,2,1,500,10));
		alg = FactoryDetectPoint.createShiTomasi(2, false, 1, 500, derivType);
		addAlgorithm(0, "Shi-Tomasi", FactoryInterestPoint.wrapPoint(alg, imageType, derivType));

		addAlgorithm(1, "SURF", FactoryDescribeRegionPoint.surf(true, imageType));
		if( imageType == ImageFloat32.class )
			addAlgorithm(1, "SIFT", FactoryDescribeRegionPoint.sift(1.6, 5, 4, false));
		addAlgorithm(1, "BRIEF", FactoryDescribeRegionPoint.brief(16, 512, -1, 4, true, imageType));
		addAlgorithm(1, "BRIEFO", FactoryDescribeRegionPoint.brief(16, 512, -1, 4, false, imageType));
		addAlgorithm(1, "Gaussian 12", FactoryDescribeRegionPoint.gaussian12(20, imageType, derivType));
		addAlgorithm(1, "Gaussian 14", FactoryDescribeRegionPoint.steerableGaussian(20, false, imageType, derivType));
		addAlgorithm(1, "Pixel 11x11", FactoryDescribeRegionPoint.pixel(11, 11, imageType));
		addAlgorithm(1, "NCC 11x11", FactoryDescribeRegionPoint.pixelNCC(11, 11, imageType));

		addAlgorithm(2, "Greedy", false);
		addAlgorithm(2, "Backwards", true);

		// estimate orientation using this once since it is fast and accurate
		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral orientationII = FactoryOrientation.surfDefault(true, integralType);
		orientation = FactoryOrientation.convertImage(orientationII,imageType);

		imageLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		imageRight = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		setMainGUI(panel);
	}

	public void process(final BufferedImage buffLeft, final BufferedImage buffRight) {
		imageLeft.reshape(buffLeft.getWidth(), buffLeft.getHeight());
		imageRight.reshape(buffRight.getWidth(), buffRight.getHeight());

		ConvertBufferedImage.convertFromSingle(buffLeft, imageLeft, imageType);
		ConvertBufferedImage.convertFromSingle(buffRight, imageRight, imageType);

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

	private GeneralAssociation createMatcher() {
		ScoreAssociation scorer = FactoryAssociation.defaultScore(describe.getDescriptorType());
		return FactoryAssociation.greedy(scorer, Double.MAX_VALUE, maxMatches, associateBackwards);
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
		TupleDescQueue<TupleDesc> leftDesc = new TupleDescQueue(describe.getDescriptorType(), describe.getDescriptionLength(), true);
		TupleDescQueue<TupleDesc> rightDesc = new TupleDescQueue(describe.getDescriptorType(), describe.getDescriptionLength(), true);

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
		extractImageFeatures(imageLeft, leftDesc, leftPts);
		progress++;
		extractImageFeatures(imageRight, rightDesc, rightPts);
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

	private void extractImageFeatures(T image, FastQueue<TupleDesc> descs, List<Point2D_F64> locs) {
		detector.detect(image);
		describe.setImage(image);
		orientation.setImage(image);

		if (detector.hasScale()) {
			for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				double scale = detector.getScale(i);
				if (describe.requiresOrientation()) {
					orientation.setScale(scale);
					yaw = orientation.compute(pt.x, pt.y);
				}

				if (describe.isInBounds(pt.x, pt.y, yaw, scale)) {
					describe.process(pt.x, pt.y, yaw, scale, descs.grow());
					locs.add(pt.copy());
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

				if (describe.isInBounds(pt.x, pt.y, yaw, 1)) {
					describe.process(pt.x, pt.y, yaw, 1, descs.grow());
					locs.add(pt.copy());
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

		ShowImages.showWindow(app, "Association Relative Score");

	}
}
