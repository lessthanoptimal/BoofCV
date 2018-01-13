/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Controls for background model demonstration
 *
 * @author Peter Abeles
 */
// TODO Display FPS
public class BackgroundControlPanel extends StandardAlgConfigPanel
	implements ActionListener, ChangeListener
{

	Listener listener;

	JLabel processingTimeLabel = new JLabel();

	JComboBox comboModel;
//	JCheckBox checkStationary;

	JSpinner spinThreshold;
	JSpinner spinLearn;

	// basic specific

	// gaussian specific
	JSpinner spinMinDifference;

	int model = 0;
//	boolean stationary = true;
	double threshold;
	double learnRate;
	double minimumDifference;

	ConfigBackgroundGaussian configGaussian = new ConfigBackgroundGaussian(12,0.0005f);
	ConfigBackgroundBasic configBasic = new ConfigBackgroundBasic(35, 0.005f);

	public BackgroundControlPanel(Listener listener) {
		this.listener = listener;

		configGaussian.initialVariance = 100;
		configGaussian.minimumDifference = 10;

		comboModel = combo(model,"Basic","Gaussian");
//		checkStationary = checkbox("Stationary",stationary);
		spinThreshold = spinner(0,0.0,255.0,1.0);
		spinLearn = spinner(0,0.0,1.0,0.001,1,6);
		spinMinDifference = spinner(0,0.0,255.0,1.0);

		addLabeled(processingTimeLabel,"FPS");
		addLabeled(comboModel,"Model");
//		add(checkStationary);
		addLabeled(spinThreshold,"Threshold");
		addLabeled(spinLearn,"Learn Rate");
		addLabeled(spinMinDifference,"Min Diff.");

		changeModel(0);
		updateListener();
	}

	public void setFPS( double fps ) {
		processingTimeLabel.setText(String.format("%.1f",fps));
	}

	private void changeModel( int model ) {
		this.model = model;

		spinMinDifference.setEnabled(model==1);

		spinThreshold.removeChangeListener(this);
		spinLearn.removeChangeListener(this);
		spinMinDifference.removeChangeListener(this);

		if( model == 0 ) {
			threshold = configBasic.threshold;
			learnRate = configBasic.learnRate;
			spinThreshold.setValue(configBasic.threshold);
			spinLearn.setValue(configBasic.learnRate);
		} else if( model == 1 ) {
			threshold = configGaussian.threshold;
			learnRate = configGaussian.learnRate;
			minimumDifference = configGaussian.minimumDifference;
			spinThreshold.setValue(configGaussian.threshold);
			spinLearn.setValue(configGaussian.learnRate);
			spinMinDifference.setValue(configGaussian.minimumDifference);
		}

		spinThreshold.addChangeListener(this);
		spinLearn.addChangeListener(this);
		spinMinDifference.addChangeListener(this);
	}

	private void updateListener() {
		switch( model ) {
			case 0:
				configBasic.threshold = (float)threshold;
				configBasic.learnRate = (float)learnRate;
				listener.modelChanged(configBasic,true);
				break;
			case 1:
				configGaussian.threshold = (float)threshold;
				configGaussian.learnRate = (float)learnRate;
				configGaussian.minimumDifference = (float)minimumDifference;
				listener.modelChanged(configGaussian,true);
				break;
		}
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == comboModel ) {
			changeModel( comboModel.getSelectedIndex());
//		} else if( e.getSource() == checkStationary ) {
//			stationary = checkStationary.isSelected();
		}

		updateListener();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinLearn ) {
			learnRate = ((Number)spinLearn.getValue()).doubleValue();
		} else if( e.getSource() == spinMinDifference ) {
			minimumDifference = ((Number)spinMinDifference.getValue()).doubleValue();
		} else if( e.getSource() == spinThreshold ) {
			threshold = ((Number)spinThreshold.getValue()).doubleValue();
		}
		updateListener();
	}

	public interface Listener
	{
		void modelChanged( ConfigBackgroundGaussian config , boolean stationary );
		void modelChanged( ConfigBackgroundBasic config , boolean stationary );
	}
}
