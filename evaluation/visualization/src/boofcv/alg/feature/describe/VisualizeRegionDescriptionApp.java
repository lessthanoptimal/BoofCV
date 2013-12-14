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

package boofcv.alg.feature.describe;

import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.SelectRegionDescriptionPanel;
import boofcv.gui.feature.TupleDescPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;


/**
 *  Allows the user to select a point and show the description of the region at that point
 *
 * @author Peter Abeles
 */
public class VisualizeRegionDescriptionApp <T extends ImageSingleBand, D extends ImageSingleBand>
	extends SelectAlgorithmAndInputPanel implements SelectRegionDescriptionPanel.Listener
{
	boolean processedImage = false;

	Class<T> imageType;
	BufferedImage image;

	DescribeRegionPoint describe;

	SelectRegionDescriptionPanel panel = new SelectRegionDescriptionPanel();

	TupleDescPanel tuplePanel = new TupleDescPanel();

	// most recently requested pixel description.  Used when the algorithm is changed
	Point2D_I32 targetPt;
	double targetRadius;
	double targetOrientation;

	public VisualizeRegionDescriptionApp( Class<T> imageType , Class<D> derivType  ) {
		super(1);

		this.imageType = imageType;

		addAlgorithm(0,"SURF-S", FactoryDescribeRegionPoint.surfStable(null, imageType));
		addAlgorithm(0,"SURF-S Color", FactoryDescribeRegionPoint.surfColorStable(null, ImageType.ms(3, imageType)));
		if( imageType == ImageFloat32.class )
			addAlgorithm(0,"SIFT", FactoryDescribeRegionPoint.sift(null,null));
		addAlgorithm(0,"BRIEF", FactoryDescribeRegionPoint.brief(new ConfigBrief(true), imageType));
		addAlgorithm(0,"BRIEFO", FactoryDescribeRegionPoint.brief(new ConfigBrief(false), imageType));
		addAlgorithm(0,"Pixel 5x5", FactoryDescribeRegionPoint.pixel(5, 5, imageType));
		addAlgorithm(0,"NCC 5x5", FactoryDescribeRegionPoint.pixelNCC(5, 5, imageType));

		panel.setListener(this);
		tuplePanel.setPreferredSize(new Dimension(100,50));
		add(tuplePanel,BorderLayout.SOUTH);
		setMainGUI(panel);
	}

	public void process( final BufferedImage image ) {
		this.image = image;
		setDescriptorInput();


		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setBackground(image);
				panel.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));
				processedImage = true;
			}});


		doRefreshAll();
	}

	private void setDescriptorInput() {
		if( describe != null )  {
			if( describe.getImageType().getFamily() == ImageType.Family.SINGLE_BAND ) {
				T input = ConvertBufferedImage.convertFromSingle(image, null, imageType);
				describe.setImage(input);
			} else {
				MultiSpectral<T> input = ConvertBufferedImage.convertFromMulti(image, null, true, imageType);
				describe.setImage(input);
			}
		}
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		this.describe = (DescribeRegionPoint<T,TupleDesc>)cookie;
		setDescriptorInput();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateTargetDescription();
				repaint();
			}});
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		process(image);
	}

	@Override
	public synchronized void descriptionChanged(Point2D_I32 pt, double radius, double orientation) {
		if( pt == null || radius < 1) {
			targetPt = null;
		} else {
			this.targetPt = pt;
			this.targetRadius = radius;
			this.targetOrientation = orientation;
		}
		updateTargetDescription();
	}

	/**
	 * Extracts the target description and updates the panel.  Should only be called from a swing thread
	 */
	private void updateTargetDescription() {
		if( targetPt != null ) {
			double scale = targetRadius/ BoofDefaults.SCALE_SPACE_CANONICAL_RADIUS;

			TupleDesc feature = describe.createDescription();
			describe.process(targetPt.x,targetPt.y,targetOrientation,scale,feature);
			tuplePanel.setDescription(feature);
		} else {
			tuplePanel.setDescription(null);
		}
		tuplePanel.repaint();
	}


	public static void main( String args[] ) {
		Class imageType = ImageFloat32.class;
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		VisualizeRegionDescriptionApp app = new VisualizeRegionDescriptionApp(imageType,derivType);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Cave","../data/evaluation/stitch/cave_01.jpg"));
		inputs.add(new PathLabel("Kayak","../data/evaluation/stitch/kayak_02.jpg"));
		inputs.add(new PathLabel("Forest","../data/evaluation/scale/rainforest_01.jpg"));

		app.setPreferredSize(new Dimension(500,500));
		app.setSize(500,500);
		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Region Descriptor Visualization");
	}
}
