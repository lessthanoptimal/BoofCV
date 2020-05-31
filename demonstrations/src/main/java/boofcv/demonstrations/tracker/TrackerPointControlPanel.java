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

package boofcv.demonstrations.tracker;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.ControlPanelPointTrackers;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Control panel for {@link VideoTrackerPointFeaturesApp}
 *
 * @author Peter Abeles
 */
public class TrackerPointControlPanel
		extends StandardAlgConfigPanel
		implements ActionListener , ChangeListener
{
	public Colorization colorization = Colorization.TRACK_ID;
	public int videoPeriod = 33;
	public int minFeatures = 600;
	public int minDuration = 2;
	public Marker markerType = Marker.Dot;
	public boolean paused = false;
	public boolean step = false;

	public

	JLabel labelSize = new JLabel();
	JLabel labelFrame = new JLabel();
	JLabel labelTimeMS = new JLabel();
	JLabel labelTrackCount = new JLabel();
	JLabel labelDuration50 = new JLabel();
	JLabel labelDuration95 = new JLabel();

	ControlPanelPointTrackers controlTracker;
	JComboBox<String> comboColor = combo(colorization.ordinal(),(Object[])Colorization.values());

	// Spawn features when tracks drop below this value
	JSpinner spinnerMinFeats = spinner(minFeatures,50,10000,10);

	JTextArea textArea = new JTextArea();

	JButton buttonPause = new JButton("Pause");
	JButton buttonStep = new JButton("Step");

	JSpinner spinnerTargetPeriod = spinner(videoPeriod,0,1000,5);
	JSpinner spinnerMinDuration = spinner(minDuration,0,1000,1);
	JComboBox<String> comboMarker = combo(markerType.ordinal(),(Object[])Marker.values());

	Listener listener;

	public TrackerPointControlPanel( Listener listener ) {
		this.listener = listener;

		controlTracker = new ControlPanelPointTrackers(listener::handleAlgorithmUpdated,createConfig());

		textArea.setEditable(false);
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);

		buttonPause.addActionListener(e->{
			step = false;
			setPauseState(!paused);
			listener.handlePause(paused);
		});

		buttonStep.addActionListener(e->{
			step = true;
			if(paused) {
				listener.handlePause(false);
			}
		});

		addLabeled(labelSize,"Size");
		addLabeled(labelFrame,"Frame");
		addLabeled(labelTimeMS,"Track Speed");
		addLabeled(spinnerTargetPeriod,"Video (ms)");
		addLabeled(labelTrackCount,"Tracks");
		addLabeled(labelDuration50,"Duration 50%");
		addLabeled(labelDuration95,"Duration 95%");
		addLabeled(comboMarker,"Markers");
		addLabeled(comboColor,"Colors");
		addLabeled(spinnerMinDuration,"Min. Duration");
		addLabeled(spinnerMinFeats,"Min. Feats");
		add(controlTracker);
		add(textArea);
		addVerticalGlue();
		add(createHorizontalPanel(buttonStep,buttonPause));
	}

	private static ConfigPointTracker createConfig() {
		int maxFeatures = 800;
		int detRadius = 5;

		ConfigPointTracker config = new ConfigPointTracker();
		config.detDesc.detectPoint.general.maxFeatures = maxFeatures;
		config.detDesc.detectPoint.general.radius = detRadius;
		config.klt.pyramidLevels = ConfigDiscreteLevels.levels(4);
		config.klt.templateRadius = 3;

		config.detDesc.detectFastHessian.maxFeaturesPerScale = 500;
		config.detDesc.detectSift.maxFeaturesPerScale = 500;

		return config;
	}


	public void setPauseState( boolean paused ) {
		this.paused = paused;
		buttonPause.setText(paused?"Resume":"Pause");
	}

	public void setImageSize( int width , int height ) {
		labelSize.setText(String.format("%d x %d",width,height));
	}

	public void setFrame( int frame ) {
		labelFrame.setText(""+frame);
	}

	public void setTime( double time ) {
		labelTimeMS.setText(String.format("%7.1f (ms)",time));
	}

	public void setTrackCount( int count ) {
		labelTrackCount.setText(""+count);
	}

	public void setDuration( double d50 , double d95 ) {
		labelDuration50.setText(String.format("%4.1f",d50));
		labelDuration95.setText(String.format("%4.1f",d95));
	}

	public void setVideoPeriod(int value ) {
		if( videoPeriod == value )
			return;
		spinnerTargetPeriod.setValue(value);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == comboColor ) {
			colorization = Colorization.values()[comboColor.getSelectedIndex()];
			listener.handleVisualizationUpdated();
		} else if( e.getSource() == comboMarker ) {
			markerType = Marker.values()[comboMarker.getSelectedIndex()];
			listener.handleVisualizationUpdated();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerMinFeats ) {
			minFeatures = ((Number)spinnerMinFeats.getValue()).intValue();
		} else if( e.getSource() == spinnerMinDuration ) {
			minDuration = ((Number) spinnerMinDuration.getValue()).intValue();
			listener.handleVisualizationUpdated();
		} else if( e.getSource() == spinnerTargetPeriod) {
			videoPeriod = ((Number) spinnerTargetPeriod.getValue()).intValue();
			listener.handleVisualizationUpdated();
		}
	}

	class ControlsGeneric extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		public ConfigGeneralDetector detector = new ConfigGeneralDetector(1000,4,3.0f);
		private JSpinner spinnerDetectRadius = spinner(detector.radius,1,500,1);

		public ControlsGeneric() {
			setBorder(BorderFactory.createEmptyBorder());
			addLabeled(spinnerDetectRadius,"Detect Radius");
		}

		@Override
		public void actionPerformed(ActionEvent e) {

		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == spinnerDetectRadius) {
				detector.radius = ((Number) spinnerDetectRadius.getValue()).intValue();
				listener.handleAlgorithmUpdated();
			}
		}
	}

	static ConfigPKlt createKltConfig() {
		ConfigPKlt klt = new ConfigPKlt();
		klt.pruneClose = true;
		klt.toleranceFB = 4;
		klt.pyramidLevels = ConfigDiscreteLevels.levels(4);
		return klt;
	}

	static ConfigPointDetector createKltDetectConfig( int maxFeatures ) {
		ConfigPointDetector config = new ConfigPointDetector();
		config.type = PointDetectorTypes.SHI_TOMASI;
		config.general.maxFeatures = maxFeatures;
		config.general.radius = 5;
		config.general.threshold = 3.0f;
		return config;
	}

//	class ControlsKLT extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
//		public ConfigGeneralDetector detector = new ConfigGeneralDetector(maxFeatures,5,3.0f);
//		public ConfigPKlt klt = createKltConfig();
//
//		private JSpinner spinnerLevels = spinner(klt.pyramidLevels.numLevelsRequested,1,20,1);
//		private JCheckBox checkPruneClose = checkbox("Prune Close", klt.pruneClose);
//		private JSpinner spinnerIterations = spinner(klt.config.maxIterations,1,500,1);
//		private JSpinner spinnerMaxError = spinner(klt.config.maxPerPixelError,0.0,255.0,5.0);
//		private JSpinner spinnerDescRadius = spinner(klt.templateRadius,1,100,1);
//		private JSpinner spinnerDetectThresh = spinner(detector.threshold,0.0,100.0,1.0);
//		private JSpinner spinnerDetectRadius = spinner(detector.radius,1,500,1);
//		private JSpinner spinnerForwardsBackwards = spinner(klt.toleranceFB,-1,100.0,1.0);
//
//		public ControlsKLT() {
//			setBorder(BorderFactory.createEmptyBorder());
//
//			addLabeled(spinnerLevels,"Pyr. Levels");
//			addAlignLeft(checkPruneClose);
//			addLabeled(spinnerIterations,"Max Iter.");
//			addLabeled(spinnerMaxError,"Max Error");
//			addLabeled(spinnerDetectThresh,"Detect Thresh");
//			addLabeled(spinnerDetectRadius,"Detect Radius");
//			addLabeled(spinnerDescRadius,"Desc. Radius");
//			addLabeled(spinnerForwardsBackwards,"F-to-B Tol.");
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			if( e.getSource() == checkPruneClose ) {
//				klt.pruneClose = checkPruneClose.isSelected();
//				listener.handleAlgorithmUpdated();
//			}
//		}
//
//		@Override
//		public void stateChanged(ChangeEvent e) {
//			if( e.getSource() == spinnerLevels) {
//				klt.pyramidLevels.numLevelsRequested = ((Number) spinnerLevels.getValue()).intValue();
//				listener.handleAlgorithmUpdated();
//			} else if( e.getSource() == spinnerDescRadius) {
//				klt.templateRadius = ((Number) spinnerDescRadius.getValue()).intValue();
//				listener.handleAlgorithmUpdated();
//			} else if( e.getSource() == spinnerMaxError) {
//				klt.config.maxPerPixelError = ((Number) spinnerMaxError.getValue()).floatValue();
//				listener.handleAlgorithmUpdated();
//			} else if( e.getSource() == spinnerDetectThresh) {
//				detector.threshold = ((Number) spinnerDetectThresh.getValue()).floatValue();
//				listener.handleAlgorithmUpdated();
//			} else if( e.getSource() == spinnerDetectRadius) {
//				detector.radius = ((Number) spinnerDetectRadius.getValue()).intValue();
//				listener.handleAlgorithmUpdated();
//			} else if( e.getSource() == spinnerForwardsBackwards ) {
//				klt.toleranceFB = ((Number) spinnerForwardsBackwards.getValue()).doubleValue();
//				listener.handleAlgorithmUpdated();
//			} else if( e.getSource() == spinnerIterations ) {
//				klt.config.maxIterations = ((Number) spinnerIterations.getValue()).intValue();
//				listener.handleAlgorithmUpdated();
//			}
//		}
//	}

	public interface Listener {
		void handleAlgorithmUpdated();

		void handleVisualizationUpdated();

		void handlePause( boolean paused );
	}

	enum Colorization {
		TRACK_ID,
		FLOW,
		FLOW_LOG
	}

	enum Marker {
		Dot,
		Circle,
		Line
	}
}
