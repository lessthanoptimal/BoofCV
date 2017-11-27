/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.shapes;

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.ConnectRule;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * @author Peter Abeles
 */
public class PolylineControlPanel extends DetectBlackShapePanel
		implements ActionListener, ChangeListener
{
	ShapeGuiListener owner;

	// selects which image to view
	JComboBox imageView;

	JCheckBox showCorners;
	JCheckBox showLines;
	JCheckBox showContour;

	boolean bShowCorners = true;
	boolean bShowLines = true;
	boolean bShowContour = false;

	ThresholdControlPanel threshold;

	JSpinner spinnerContourConnect;
	JSpinner spinnerMinContourSize;
	JSpinner spinnerMinSides;
	JSpinner spinnerMaxSides;
	JCheckBox checkConvex;
	JCheckBox checkLooping;

	int whichAlgorithm = 0;

	SplitMergePanel2 panelSplitMerge2 = new SplitMergePanel2();

	// parameters that are common to all algorithms
	int minSides = 3;
	int maxSides = 6;
	boolean convex = true;
	boolean looping = true;
	int minimumContourSize = 10;
	ConnectRule connectRule = ConnectRule.FOUR;


	public PolylineControlPanel(ShapeGuiListener owner) {
		this.owner = owner;

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		spinnerContourConnect = spinner(connectRule.ordinal(), ConnectRule.values());

		showCorners = new JCheckBox("Corners");
		showCorners.addActionListener(this);
		showCorners.setSelected(bShowCorners);
		showLines = new JCheckBox("Lines");
		showLines.setSelected(bShowLines);
		showLines.addActionListener(this);
		showContour = new JCheckBox("Contour");
		showContour.addActionListener(this);
		showContour.setSelected(bShowContour);

		threshold = new ThresholdControlPanel(owner);

		spinnerMinContourSize = new JSpinner(new SpinnerNumberModel(minimumContourSize,
				5,10000,2));
		spinnerMinContourSize.setMaximumSize(spinnerMinContourSize.getPreferredSize());
		spinnerMinContourSize.addChangeListener(this);
		spinnerMinSides = new JSpinner(new SpinnerNumberModel(minSides, 3, 20, 1));
		spinnerMinSides.setMaximumSize(spinnerMinSides.getPreferredSize());
		spinnerMinSides.addChangeListener(this);
		spinnerMaxSides = new JSpinner(new SpinnerNumberModel(maxSides, 3, 20, 1));
		spinnerMaxSides.setMaximumSize(spinnerMaxSides.getPreferredSize());
		spinnerMaxSides.addChangeListener(this);

		checkConvex = new JCheckBox("Convex");
		checkConvex.addActionListener(this);
		checkConvex.setSelected(convex);
		checkLooping = new JCheckBox("Looping");
		checkLooping.addActionListener(this);
		checkLooping.setSelected(looping);

		addLabeled(processingTimeLabel,"Time (ms)", this);
		addLabeled(imageSizeLabel,"Size", this);
		addLabeled(imageView, "View: ", this);
		addLabeled(selectZoom,"Zoom",this);
		addAlignLeft(showCorners, this);
		addAlignLeft(showLines, this);
		addAlignLeft(showContour, this);
		add(threshold);
		addLabeled(spinnerMinContourSize, "Min Contour Size: ", this);
		addLabeled(spinnerContourConnect, "Contour Connect: ", this);
		addLabeled(spinnerMinSides, "Minimum Sides: ", this);
		addLabeled(spinnerMaxSides, "Maximum Sides: ", this);
		addAlignLeft(checkConvex, this);
		addAlignLeft(checkLooping, this);
		add(panelSplitMerge2);
		addVerticalGlue(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == imageView ) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		} else if( e.getSource() == showCorners ) {
			bShowCorners = showCorners.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showLines ) {
			bShowLines = showLines.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showContour ) {
			bShowContour = showContour.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == checkConvex ) {
			convex = checkConvex.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == checkLooping ) {
			looping = checkLooping.isSelected();
			owner.configUpdate();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerMinSides ) {
			minSides = ((Number) spinnerMinSides.getValue()).intValue();
			if( minSides > maxSides ) {
				maxSides = minSides;
				spinnerMaxSides.setValue(minSides);
			}
		} else if( e.getSource() == spinnerMaxSides ) {
			maxSides = ((Number) spinnerMaxSides.getValue()).intValue();
			if (maxSides < minSides) {
				minSides = maxSides;
				spinnerMinSides.setValue(minSides);
			}
		} else if( e.getSource() == selectZoom ) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
			return;
		} else if( e.getSource() == spinnerMinContourSize ) {
			minimumContourSize = ((Number) spinnerMinContourSize.getValue()).intValue();
		} else if( e.getSource() == spinnerContourConnect ) {
			connectRule = (ConnectRule)spinnerContourConnect.getValue();
		} else {
			throw new RuntimeException("Egads");
		}

		owner.configUpdate();
	}

	class SplitMergePanel2 extends StandardAlgConfigPanel
		implements ChangeListener
	{
		ConfigPolylineSplitMerge config = new ConfigPolylineSplitMerge();

		JSpinner spinnerConsiderSides;
		JSpinner spinnerMinSideLength;
		JSpinner spinnerCornerPenalty;
		JSpinner spinnerSideSplitScore;
		JSpinner spinnerConvexTest;

		JSpinner spinnerSideSamples;

		public SplitMergePanel2() {
			spinnerConsiderSides  = spinner(config.extraConsider.fraction, 0, 5.0, 0.25,1,3);
			spinnerMinSideLength  = spinner(config.minimumSideLength, 1, 1000, 1);
			spinnerCornerPenalty  = spinner(config.cornerScorePenalty,0,100,0.1);
			spinnerSideSplitScore = spinner(config.thresholdSideSplitScore,0,100,0.1);
			spinnerSideSamples    = spinner(config.maxNumberOfSideSamples, 5, 1000, 5);
			spinnerConvexTest     = spinner(config.convexTest, 0.0, 20.0, 0.25,2,2);

			addLabeled(spinnerConsiderSides, "Extra Consider:", this);
			addLabeled(spinnerMinSideLength, "Min Side Length: ", this);
			addLabeled(spinnerCornerPenalty, "Corner Penalty: ", this);
			addLabeled(spinnerSideSplitScore, "Side Split: ", this);
			addLabeled(spinnerSideSamples, "Side Samples: ", this);
			addLabeled(spinnerConvexTest, "Convex Test: ", this);
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (spinnerConsiderSides == e.getSource()) {
				config.extraConsider.fraction = ((Number) spinnerConsiderSides.getValue()).doubleValue();
			} else if (spinnerMinSideLength == e.getSource()) {
				config.minimumSideLength = ((Number) spinnerMinSideLength.getValue()).intValue();
			} else if (spinnerCornerPenalty == e.getSource()) {
				config.cornerScorePenalty = ((Number) spinnerCornerPenalty.getValue()).doubleValue();
			} else if (spinnerSideSplitScore == e.getSource()) {
				config.thresholdSideSplitScore = ((Number) spinnerSideSplitScore.getValue()).doubleValue();
			} else if (spinnerSideSamples == e.getSource()) {
				config.maxNumberOfSideSamples = ((Number) spinnerSideSamples.getValue()).intValue();
			} else if (spinnerConvexTest == e.getSource()) {
				config.convexTest = ((Number) spinnerConvexTest.getValue()).doubleValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			owner.configUpdate();
		}
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}
}
