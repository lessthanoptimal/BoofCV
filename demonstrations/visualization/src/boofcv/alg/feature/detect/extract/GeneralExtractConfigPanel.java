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

package boofcv.alg.feature.detect.extract;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * GUI component for configuring GeneralFeatureIntensity.
 *
 * @author Peter Abeles
 */
public class GeneralExtractConfigPanel extends StandardAlgConfigPanel implements ActionListener , ChangeListener {
	JComboBox selectImage;
	JSpinner selectSeparation;
	JSpinner selectThreshold;
	JSpinner selectNumFeatures;

	Listener listener;

	public GeneralExtractConfigPanel() {
		selectImage = new JComboBox();
		selectImage.addItem("Original");
		selectImage.addItem("Intensity");
		selectImage.addItem("Features");
		selectImage.addActionListener(this);
		selectImage.setMaximumSize(new Dimension(175,(int)selectImage.getPreferredSize().getHeight()));

		selectSeparation = new JSpinner(new SpinnerNumberModel(1,1,20,1));
		selectSeparation.addChangeListener(this);
		int w = (int)selectSeparation.getPreferredSize().getWidth()+20;
		int h = (int)selectSeparation.getPreferredSize().getHeight() + 10;

		selectSeparation.setPreferredSize(new Dimension(w,h));
		selectSeparation.setMaximumSize(new Dimension(w,h));

		selectThreshold = new JSpinner(new SpinnerNumberModel(0.01,0,1,0.01));
		selectThreshold.addChangeListener(this);
		selectThreshold.setPreferredSize(new Dimension(w,h));
		selectThreshold.setMaximumSize(new Dimension(w,h));

		selectNumFeatures = new JSpinner(new SpinnerNumberModel(200,0,1000,20));
		selectNumFeatures.addChangeListener(this);
		selectNumFeatures.setPreferredSize(new Dimension(w,h));
		selectNumFeatures.setMaximumSize(new Dimension(w,h));

		addLabeled(selectImage,"Display",this);
		addLabeled(selectNumFeatures,"Total",this);
		addLabeled(selectSeparation,"Separation",this);
		addLabeled(selectThreshold,"Threshold",this);
		add(Box.createVerticalGlue());
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void setImageIndex( int index ) {
		selectImage.setSelectedIndex(index);
	}

	public void setFeatureSeparation( int radius ) {
		selectSeparation.setValue(radius);
	}

	public void setThreshold( double value ) {
		selectThreshold.setValue(value);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == selectImage ) {
			int selected = selectImage.getSelectedIndex();
			listener.changeImage(selected);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == selectThreshold ) {
			double value = ((Number)selectThreshold.getValue()).doubleValue();
			listener.changeThreshold(value);
		} else if( e.getSource() == selectSeparation ) {
			int radius =  ((Number)selectSeparation.getValue()).intValue();
			listener.changeFeatureSeparation(radius);
		} else if( e.getSource() == selectNumFeatures ) {
			int total =  ((Number)selectNumFeatures.getValue()).intValue();
			listener.changeNumFeatures(total);
		}
	}

	public static interface Listener
	{
		public void changeImage( int index );
		public void changeFeatureSeparation(int radius);
		public void changeThreshold( double value );
		public void changeNumFeatures( int total );
	}
}
