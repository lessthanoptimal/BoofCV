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
import boofcv.factory.shape.ConfigSplitMergeLineFit;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Control panel for configuring a polyline algorithm
 *
 * @author Peter Abeles
 */
public class PolylineControlPanel extends StandardAlgConfigPanel
		implements ActionListener, ChangeListener
{
	ShapeGuiListener owner;

	JSpinner spinnerMinSides;
	JSpinner spinnerMaxSides;
	JCheckBox checkConvex;
	JCheckBox checkLooping;

	int whichAlgorithm = 0;

	SplitMergePanel panelSplitMerge = new SplitMergePanel();
	OldSplitMergePanel panelOldSplitMerge = new OldSplitMergePanel();

	// parameters that are common to all algorithms
	int minSides = 3;
	int maxSides = 6;
	boolean convex = true;
	boolean looping = true;

	public PolylineControlPanel(ShapeGuiListener owner) {
		this.owner = owner;

		setBorder(BorderFactory.createEmptyBorder());

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

		addLabeled(spinnerMinSides, "Minimum Sides: ", this);
		addLabeled(spinnerMaxSides, "Maximum Sides: ", this);
		addAlignLeft(checkConvex, this);
		addAlignLeft(checkLooping, this);

		if( whichAlgorithm == 0 )
			add(panelSplitMerge);
		else
			add(panelOldSplitMerge);
		addVerticalGlue(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == checkConvex ) {
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
		} else {
			throw new RuntimeException("Egads");
		}

		owner.configUpdate();
	}

	class SplitMergePanel extends StandardAlgConfigPanel
		implements ChangeListener
	{
		ConfigPolylineSplitMerge config = new ConfigPolylineSplitMerge();

		JSpinner spinnerConsiderSides;
		JSpinner spinnerMinSideLength;
		JSpinner spinnerCornerPenalty;
		JSpinner spinnerSideSplitScore;
		JSpinner spinnerConvexTest;
		JSpinner spinnerMaxSideError;

		JSpinner spinnerSideSamples;

		public SplitMergePanel() {
			setBorder(BorderFactory.createEmptyBorder());
			spinnerMaxSideError   = spinner(config.maxSideError,0,500,0.2,1,2);
			spinnerConsiderSides  = spinner(config.extraConsider.fraction, 0, 5.0, 0.25,1,3);
			spinnerMinSideLength  = spinner(config.minimumSideLength, 1, 1000, 1);
			spinnerCornerPenalty  = spinner(config.cornerScorePenalty,0,100,0.1);
			spinnerSideSplitScore = spinner(config.thresholdSideSplitScore,0,100,0.1);
			spinnerSideSamples    = spinner(config.maxNumberOfSideSamples, 5, 1000, 5);
			spinnerConvexTest     = spinner(config.convexTest, 0.0, 20.0, 0.25,2,2);

			addLabeled(spinnerMaxSideError, "Max Error:", this);
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
			} else if (spinnerMaxSideError == e.getSource()) {
				config.maxSideError = ((Number) spinnerMaxSideError.getValue()).doubleValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			owner.configUpdate();
		}
	}

	class OldSplitMergePanel extends StandardAlgConfigPanel
			implements ChangeListener
	{
		ConfigSplitMergeLineFit config = new ConfigSplitMergeLineFit();

		JSpinner spinnerContourSplit;
		JSpinner spinnerContourMinSplit;
		JSpinner spinnerContourIterations;
		JSpinner spinnerSplitPenalty;

		public OldSplitMergePanel() {
			setBorder(BorderFactory.createEmptyBorder());
			spinnerContourSplit = spinner(config.splitFraction,0.0,1.0,0.01,1,2);
			spinnerContourMinSplit = spinner(config.minimumSide.fraction,0.0, 1.0, 0.001,1,3);
			spinnerContourIterations = spinner(config.iterations,1, 200, 1,1,2);
//			spinnerContourIterations.setMaximumSize(spinnerContourIterations.getPreferredSize());
//			spinnerContourIterations.addChangeListener(this);
			spinnerSplitPenalty = spinner(config.pruneSplitPenalty, 0.0, 100.0, 1.0,1,1);

			addLabeled(spinnerContourSplit, "Split Fraction: ", this);
			addLabeled(spinnerContourMinSplit, "Min Split: ", this);
			addLabeled(spinnerContourIterations, "Max Iterations: ", this);
			addLabeled(spinnerSplitPenalty, "Split Penalty: ", this);
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == spinnerContourSplit ) {
				config.splitFraction = ((Number) spinnerContourSplit.getValue()).doubleValue();
			} else if( e.getSource() == spinnerContourMinSplit ) {
				config.minimumSide.fraction = ((Number) spinnerContourMinSplit.getValue()).doubleValue();
			} else if( e.getSource() == spinnerContourIterations ) {
				config.iterations = ((Number) spinnerContourIterations.getValue()).intValue();
			} else if( e.getSource() == spinnerSplitPenalty ) {
				config.pruneSplitPenalty = ((Number) spinnerSplitPenalty.getValue()).doubleValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			owner.configUpdate();
		}
	}

	public ConfigPolylineSplitMerge getConfigSplitMerge() {
		ConfigPolylineSplitMerge config = panelSplitMerge.config;
		config.minSides = minSides;
		config.maxSides = maxSides;
		config.convex = convex;

		return config;
	}


	public ConfigSplitMergeLineFit getConfigSplitMergeOld() {
		ConfigSplitMergeLineFit config = panelOldSplitMerge.config;
		config.loop = looping;
		return config;
	}
}
