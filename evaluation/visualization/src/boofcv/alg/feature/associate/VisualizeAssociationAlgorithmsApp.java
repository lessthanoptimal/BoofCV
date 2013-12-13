/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Using the same descriptor show how associations changes with different algorithms.
 *
 * @author Peter Abeles
 */
public class VisualizeAssociationAlgorithmsApp<T extends ImageSingleBand>
		extends SelectAlgorithmAndInputPanel
{

	DetectDescribePoint<T,TupleDesc_F64> detector;

	AssociateDescription<TupleDesc_F64> alg;

	T image0;
	T image1;
	Class<T> imageType;

	AssociationPanel panel = new AssociationPanel(20);

	boolean processedImage = false;

	FastQueue<TupleDesc_F64> features0 = new FastQueue<TupleDesc_F64>(1,TupleDesc_F64.class,false);
	FastQueue<Point2D_F64> points0 = new FastQueue<Point2D_F64>(1,Point2D_F64.class,true);
	FastQueue<TupleDesc_F64> features1 = new FastQueue<TupleDesc_F64>(1,TupleDesc_F64.class,false);
	FastQueue<Point2D_F64> points1 = new FastQueue<Point2D_F64>(1,Point2D_F64.class,true);


	public VisualizeAssociationAlgorithmsApp( Class<T> imageType ) {
		super(1);
		this.imageType = imageType;

		detector = (DetectDescribePoint) FactoryDetectDescribe.surfStable(
				new ConfigFastHessian(5, 4, 200, 1, 9, 4, 4), null, null, ImageFloat32.class);
//		detector = (DetectDescribePoint) FactoryDetectDescribe.sift(4,1,false,200);

		int DOF = detector.createDescription().size();

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class,true);

		addAlgorithm(0, "Greedy", FactoryAssociation.greedy(score, Double.MAX_VALUE, false));
		addAlgorithm(0, "Greedy Backwards", FactoryAssociation.greedy(score, Double.MAX_VALUE, true));
		addAlgorithm(0, "K-D Tree BBF", FactoryAssociation.kdtree(DOF, 75));
		addAlgorithm(0, "Random Forest", FactoryAssociation.kdRandomForest(DOF, 75, 10, 5, 1233445565));

		image0 = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		image1 = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		setMainGUI(panel);
	}

	public void process(final BufferedImage buffLeft, final BufferedImage buffRight) {
		image0.reshape(buffLeft.getWidth(), buffLeft.getHeight());
		image1.reshape(buffRight.getWidth(), buffRight.getHeight());

		ConvertBufferedImage.convertFromSingle(buffLeft, image0, imageType);
		ConvertBufferedImage.convertFromSingle(buffRight, image1, imageType);

		createSet(image0,features0,points0);
		createSet(image1,features1,points1);

		System.out.println("Found features: "+features0.size()+" "+features1.size());

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setImages(buffLeft, buffRight);
				processedImage = true;
				doRefreshAll();
			}
		});
	}

	private void createSet( T image , FastQueue<TupleDesc_F64> descs , FastQueue<Point2D_F64> points ) {

		detector.detect(image);

		descs.reset();
		points.reset();
		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			points.grow().set(detector.getLocation(i));
			descs.add( detector.getDescription(i).copy() );
		}
	}

	public void processImages() {
		long before = System.currentTimeMillis();

		alg.setSource(features0);
		alg.setDestination(features1);
		long before1 = System.currentTimeMillis();
		alg.associate();

		long after = System.currentTimeMillis();

		System.out.println("Elapsed: "+(after-before)+"  or  "+(after-before1));


		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setAssociation(points0.toList(), points1.toList(), alg.getMatches());
				repaint();
			}
		});
	}

	@Override
	public void refreshAll(Object[] cookies) {
		alg = (AssociateDescription<TupleDesc_F64>)cookies[0];
		processImages();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		alg = (AssociateDescription<TupleDesc_F64>)cookie;
		processImages();
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage left = media.openImage(inputRefs.get(index).getPath(0));
		BufferedImage right = media.openImage(inputRefs.get(index).getPath(1));

		process(left, right);
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main(String args[]) {
		Class imageType = ImageFloat32.class;

		VisualizeAssociationAlgorithmsApp app = new VisualizeAssociationAlgorithmsApp(imageType);

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
