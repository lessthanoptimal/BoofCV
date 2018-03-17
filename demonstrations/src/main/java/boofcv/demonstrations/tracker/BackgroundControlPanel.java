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
import boofcv.factory.background.ConfigBackgroundGmm;
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
	JSpinner spinVariance;

	// gmm specific
	JSpinner spinNumberOfGaussians;
	JSpinner spinSignificant;
	JSpinner spinDecay;

	int model = 0;
//	boolean stationary = true;
	double threshold;
	double learnRate;
	double initVariance;
	double minimumDifference;
	int numberOfGaussians;
	double significantWeight;
	double decayRate;

	ConfigBackgroundGmm configGmm = new ConfigBackgroundGmm();
	ConfigBackgroundGaussian configGaussian = new ConfigBackgroundGaussian(12,0.0005f);
	ConfigBackgroundBasic configBasic = new ConfigBackgroundBasic(35, 0.005f);

	public BackgroundControlPanel(Listener listener) {
		this.listener = listener;

		configGaussian.initialVariance = 100;
		configGaussian.minimumDifference = 10;

		comboModel = combo(model,"Basic","Gaussian","GMM");
//		checkStationary = checkbox("Stationary",stationary);
		spinThreshold = spinner(0,0.0,255.0,1.0);
		spinLearn = spinner(0,0.0,1.0,0.001,1,6);
		spinMinDifference = spinner(0,0.0,255.0,1.0);
		spinVariance = spinner(10,1,255*255,10);
		spinNumberOfGaussians = spinner(1,1,100,1);
		spinSignificant = spinner(0.0,0.0,2,0.001,"0.0E0",10);
		spinDecay = spinner(0.0,0.0,1.0,0.001,"0.0E0",10);

		addLabeled(processingTimeLabel,"FPS");
		addLabeled(comboModel,"Model");
//		add(checkStationary);
		addLabeled(spinThreshold,"Threshold");
		addLabeled(spinLearn,"Learn Rate");
		addLabeled(spinVariance,"Init Variance");
		addLabeled(spinMinDifference,"Min Diff.");
		addLabeled(spinNumberOfGaussians,"Num. Gauss.");
		addLabeled(spinSignificant,"Significant");
		addLabeled(spinDecay,"Decay Rate");

		changeModel(0);
		updateListener();
	}

	public void setFPS( double fps ) {
		processingTimeLabel.setText(String.format("%.1f",fps));
	}

	private void changeModel( int model ) {
		this.model = model;


		spinThreshold.removeChangeListener(this);
		spinLearn.removeChangeListener(this);
		spinMinDifference.removeChangeListener(this);
		spinVariance.removeChangeListener(this);
		spinNumberOfGaussians.removeChangeListener(this);
		spinSignificant.removeChangeListener(this);
		spinDecay.removeChangeListener(this);

		if( model == 0 ) {
			threshold = configBasic.threshold;
			learnRate = configBasic.learnRate;
			spinThreshold.setValue(configBasic.threshold);
			spinLearn.setValue(configBasic.learnRate);

			spinThreshold.setEnabled(true);
			spinLearn.setEnabled(true);
			spinMinDifference.setEnabled(false);
			spinVariance.setEnabled(false);
			spinNumberOfGaussians.setEnabled(false);
			spinSignificant.setEnabled(false);
			spinDecay.setEnabled(false);

		} else if( model == 1 ) {
			threshold = configGaussian.threshold;
			learnRate = configGaussian.learnRate;
			initVariance = configGaussian.initialVariance;
			minimumDifference = configGaussian.minimumDifference;
			spinThreshold.setValue(configGaussian.threshold);
			spinLearn.setValue(configGaussian.learnRate);
			spinMinDifference.setValue(configGaussian.minimumDifference);
			spinVariance.setValue(initVariance);

			spinThreshold.setEnabled(true);
			spinLearn.setEnabled(true);
			spinMinDifference.setEnabled(true);
			spinVariance.setEnabled(true);
			spinNumberOfGaussians.setEnabled(false);
			spinSignificant.setEnabled(false);
			spinDecay.setEnabled(false);
		} else if( model == 2 ) {
			learnRate = 1.0/configGmm.learningPeriod;
			initVariance = configGmm.initialVariance;
			numberOfGaussians = configGmm.numberOfGaussian;
			significantWeight = configGmm.significantWeight;
			decayRate = configGmm.decayCoefient;

			spinLearn.setValue(learnRate);
			spinVariance.setValue(initVariance);
			spinNumberOfGaussians.setValue(numberOfGaussians);
			spinSignificant.setValue(significantWeight);
			spinDecay.setValue(decayRate);

			spinThreshold.setEnabled(false);
			spinLearn.setEnabled(true);
			spinMinDifference.setEnabled(false);
			spinVariance.setEnabled(true);
			spinNumberOfGaussians.setEnabled(true);
			spinSignificant.setEnabled(true);
			spinDecay.setEnabled(true);
		}

		spinThreshold.addChangeListener(this);
		spinLearn.addChangeListener(this);
		spinMinDifference.addChangeListener(this);
		spinVariance.addChangeListener(this);
		spinNumberOfGaussians.addChangeListener(this);
		spinSignificant.addChangeListener(this);
		spinDecay.addChangeListener(this);
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
				configGaussian.initialVariance = (float)initVariance;
				configGaussian.learnRate = (float)learnRate;
				configGaussian.minimumDifference = (float)minimumDifference;
				listener.modelChanged(configGaussian,true);
				break;

			case 2:
				configGmm.initialVariance = (float)initVariance;
				configGmm.learningPeriod = (float)(1.0/learnRate);
				configGmm.numberOfGaussian = numberOfGaussians;
				configGmm.significantWeight = (float)significantWeight;
				configGmm.decayCoefient = (float)decayRate;
				System.out.println(configGmm);
				listener.modelChanged(configGmm,true);
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
		} else if( e.getSource() == spinVariance ) {
			initVariance = ((Number)spinVariance.getValue()).doubleValue();
		} else if( e.getSource() == spinNumberOfGaussians ) {
			numberOfGaussians = ((Number)spinNumberOfGaussians.getValue()).intValue();
		} else if( e.getSource() == spinSignificant ) {
			significantWeight = ((Number)spinSignificant.getValue()).doubleValue();
		} else if( e.getSource() == spinDecay ) {
			decayRate = ((Number)spinDecay.getValue()).doubleValue();
		}
		updateListener();
	}

	public interface Listener
	{
		void modelChanged( ConfigBackgroundGaussian config , boolean stationary );
		void modelChanged( ConfigBackgroundBasic config , boolean stationary );
		void modelChanged( ConfigBackgroundGmm config , boolean stationary );
	}
}
