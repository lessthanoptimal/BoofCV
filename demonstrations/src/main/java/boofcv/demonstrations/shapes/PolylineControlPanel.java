/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.shapes.polyline.BaseConfigPolyline;
import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.factory.shape.ConfigSplitMergeLineFit;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.JConfigLength;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
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

	JComboBox selectAlgorithm;

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

	JPanel sideCountPanel = new JPanel();

	public PolylineControlPanel(ShapeGuiListener owner) {
		this(owner,new ConfigPolylineSplitMerge());
	}

	public PolylineControlPanel(ShapeGuiListener owner, BaseConfigPolyline config ) {
		this.owner = owner;

		if( config instanceof ConfigPolylineSplitMerge ) {
			panelSplitMerge = new SplitMergePanel((ConfigPolylineSplitMerge)config);
			whichAlgorithm = 0;
		} else {
			panelOldSplitMerge = new OldSplitMergePanel((ConfigSplitMergeLineFit)config);
			whichAlgorithm = 1;
		}

		setBorder(BorderFactory.createEmptyBorder());

		selectAlgorithm = new JComboBox(new String[]{"New","Old"});
		selectAlgorithm.setSelectedIndex(whichAlgorithm);
		selectAlgorithm.addActionListener(this);
		selectAlgorithm.setMaximumSize(selectAlgorithm.getPreferredSize());

		spinnerMinSides = new JSpinner(new SpinnerNumberModel(minSides, 3, 100, 1));
		spinnerMinSides.setMaximumSize(spinnerMinSides.getPreferredSize());
		spinnerMinSides.addChangeListener(this);
		spinnerMaxSides = new JSpinner(new SpinnerNumberModel(maxSides, 3, 100, 1));
		spinnerMaxSides.setMaximumSize(spinnerMaxSides.getPreferredSize());
		spinnerMaxSides.addChangeListener(this);
		sideCountPanel.setLayout(new BoxLayout(sideCountPanel,BoxLayout.X_AXIS));
		sideCountPanel.add( new JLabel("Sides:"));
		sideCountPanel.add(spinnerMinSides);
		sideCountPanel.add(Box.createRigidArea(new Dimension(10,10)));
		sideCountPanel.add( new JLabel("to"));
		sideCountPanel.add(Box.createRigidArea(new Dimension(10,10)));
		sideCountPanel.add(spinnerMaxSides);

		checkConvex = checkbox("Convex",convex);
		checkLooping = checkbox("Looping",looping);

		JPanel flagsPanel = new JPanel();
		flagsPanel.setLayout(new BoxLayout(flagsPanel,BoxLayout.X_AXIS));
		flagsPanel.add(checkConvex);
		flagsPanel.add(checkLooping);

		addLabeled(selectAlgorithm,"Algorithm");
		add(sideCountPanel);
		add(flagsPanel);

		if( whichAlgorithm == 0 )
			add(panelSplitMerge);
		else
			add(panelOldSplitMerge);
	}

	/**
	 * Removes controls related to the number of sides in the polygon. useful when it's not something that
	 * can actually be tuned. ONLY CALL BEFORE DISPLAYED OR INSIDE THE UI THREAD.
	 */
	public void removeControlNumberOfSides() {
		remove(sideCountPanel);
		validate();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == checkConvex ) {
			convex = checkConvex.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == checkLooping ) {
			looping = checkLooping.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == selectAlgorithm ) {
			if( whichAlgorithm == 0 )
				remove(panelSplitMerge);
			else
				remove(panelOldSplitMerge);
			this.whichAlgorithm = selectAlgorithm.getSelectedIndex();
			if( whichAlgorithm == 0 )
				add(panelSplitMerge);
			else
				add(panelOldSplitMerge);
			invalidate();
			validate();
			repaint();
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

	public void disableLoopingCheckBox() {
		checkLooping.setEnabled(false);
	}

	class SplitMergePanel extends StandardAlgConfigPanel
		implements ChangeListener, JConfigLength.Listener
	{
		ConfigPolylineSplitMerge config;

		JConfigLength controlExtraConsider;
		JSpinner spinnerMinSideLength;
		JSpinner spinnerCornerPenalty;
		JSpinner spinnerSideSplitScore;
		JSpinner spinnerConvexTest;
		JConfigLength controlMaxSideError;

		JSpinner spinnerSideSamples;
		JSpinner spinnerRefine;


		public SplitMergePanel() {
			this(new ConfigPolylineSplitMerge());
		}

		public SplitMergePanel(ConfigPolylineSplitMerge config ) {
			this.config = config;

			setBorder(BorderFactory.createEmptyBorder());
			controlMaxSideError = configLength(config.maxSideError,0,1000);
			controlExtraConsider = configLength(config.extraConsider,0,100);
			spinnerMinSideLength  = spinner(config.minimumSideLength, 1, 1000, 1);
			spinnerCornerPenalty  = spinner(config.cornerScorePenalty,0,100,0.1);
			spinnerSideSplitScore = spinner(config.thresholdSideSplitScore,0,100,0.1);
			spinnerSideSamples    = spinner(config.maxNumberOfSideSamples, 5, 1000, 5);
			spinnerConvexTest     = spinner(config.convexTest, 0.0, 20.0, 0.25,2,2);
			spinnerRefine         = spinner(config.refineIterations, 0, 20, 1);

			addLabeled(controlMaxSideError, "Max Side Error");
			addLabeled(controlExtraConsider, "Extra Side Consider");
			addLabeled(spinnerMinSideLength, "Min Side Length");
			addLabeled(spinnerCornerPenalty, "Corner Penalty");
			addLabeled(spinnerSideSplitScore, "Side Split");
			addLabeled(spinnerSideSamples, "Side Samples");
			addLabeled(spinnerConvexTest, "Convex Test");
			addLabeled(spinnerRefine, "Refine Iterations ");
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (spinnerMinSideLength == e.getSource()) {
				config.minimumSideLength = ((Number) spinnerMinSideLength.getValue()).intValue();
			} else if (spinnerCornerPenalty == e.getSource()) {
				config.cornerScorePenalty = ((Number) spinnerCornerPenalty.getValue()).doubleValue();
			} else if (spinnerSideSplitScore == e.getSource()) {
				config.thresholdSideSplitScore = ((Number) spinnerSideSplitScore.getValue()).doubleValue();
			} else if (spinnerSideSamples == e.getSource()) {
				config.maxNumberOfSideSamples = ((Number) spinnerSideSamples.getValue()).intValue();
			} else if (spinnerConvexTest == e.getSource()) {
				config.convexTest = ((Number) spinnerConvexTest.getValue()).doubleValue();
			} else if (spinnerRefine == e.getSource()) {
				config.refineIterations = ((Number) spinnerRefine.getValue()).intValue();
			} else {
				throw new RuntimeException("Unknown");
			}
			owner.configUpdate();
		}

		@Override
		public void changeConfigLength(JConfigLength source, double fraction, double length) {
			if( source == controlExtraConsider) {
				config.extraConsider.fraction = fraction;
				config.extraConsider.length = length;
			} else if( source == controlMaxSideError) {
				config.maxSideError.fraction = fraction;
				config.maxSideError.length = length;
			}
			owner.configUpdate();
		}
	}

	class OldSplitMergePanel extends StandardAlgConfigPanel
			implements ChangeListener
	{
		ConfigSplitMergeLineFit config;

		JSpinner spinnerContourSplit;
		JSpinner spinnerContourMinSplit;
		JSpinner spinnerContourIterations;
		JSpinner spinnerSplitPenalty;

		public OldSplitMergePanel() {
			this(new ConfigSplitMergeLineFit());
		}

		public OldSplitMergePanel(ConfigSplitMergeLineFit config) {
			this.config = config;
			setBorder(BorderFactory.createEmptyBorder());
			spinnerContourSplit = spinner(config.splitFraction,0.0,1.0,0.01,1,2);
			spinnerContourMinSplit = spinner(config.minimumSide.fraction,0.0, 1.0, 0.001,1,3);
			spinnerContourIterations = spinner(config.iterations,1, 200, 1,1,2);
//			spinnerContourIterations.setMaximumSize(spinnerContourIterations.getPreferredSize());
//			spinnerContourIterations.addChangeListener(this);
			spinnerSplitPenalty = spinner(config.pruneSplitPenalty, 0.0, 100.0, 1.0,1,1);

			addLabeled(spinnerContourSplit, "Split Fraction: ");
			addLabeled(spinnerContourMinSplit, "Min Split: ");
			addLabeled(spinnerContourIterations, "Max Iterations: ");
			addLabeled(spinnerSplitPenalty, "Split Penalty: ");
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
		config.loops = looping;
		config.minimumSides = minSides;
		config.maximumSides = maxSides;
		config.convex = convex;
		return config;
	}


	public ConfigSplitMergeLineFit getConfigSplitMergeOld() {
		ConfigSplitMergeLineFit config = panelOldSplitMerge.config;
		config.loop = looping;
		config.minimumSides = minSides;
		config.maximumSides = maxSides;
		config.convex = convex;
		return config;
	}
}
