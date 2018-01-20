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

package boofcv.gui;

import boofcv.struct.ConfigLength;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

/**
 * Control for setting the value of a {@link ConfigLength} class.
 */
public class JConfigLength extends JPanel implements PropertyChangeListener , ActionListener, ChangeListener {
	JCheckBox toggleFixed = new JCheckBox();
	JSpinner spinnerFraction = new JSpinner(new SpinnerNumberModel(0.1,0.0,1.0,0.01));
	JFormattedTextField textLength;

	boolean integerMode;
	boolean modeRelative = true;
	ConfigLength valueFraction = new ConfigLength(1,0.1);
	ConfigLength valueFixed = new ConfigLength(1,-1);

	Listener listener;

	double minimumLength = -Double.MAX_VALUE;
	double maximumLength = Double.MAX_VALUE;

	public JConfigLength( Listener listener, boolean integerMode ) {
		setLayout(new BoxLayout(this,BoxLayout.X_AXIS));

		this.listener = listener;
		this.integerMode = integerMode;

		if( integerMode ) {
			textLength = new JFormattedTextField(NumberFormat.getIntegerInstance());
		} else {
			textLength = new JFormattedTextField(NumberFormat.getNumberInstance());
		}

		toggleFixed.setSelected(modeRelative);
		toggleFixed.addActionListener(this);
		toggleFixed.setMaximumSize(toggleFixed.getPreferredSize());
		spinnerFraction.setValue(valueFraction.fraction);
		JFormattedTextField jftf = ((JSpinner.DefaultEditor)spinnerFraction.getEditor()).getTextField();
		jftf.setColumns(3);
		spinnerFraction.addChangeListener(this);
		textLength.setValue(valueFraction.length);
		textLength.setColumns(4);
		textLength.addPropertyChangeListener("value",this);

		add(toggleFixed);
		add(spinnerFraction);
		add(textLength);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		if( enabled ) {
			toggleFixed.setEnabled(true);
			if(modeRelative)
				toggleFixed.setEnabled(true);
			textLength.setEnabled(true);
		} else {
			toggleFixed.setEnabled(false);
			spinnerFraction.setEnabled(false);
			textLength.setEnabled(false);
		}
	}

	public void setLengthBounds(double min , double max ) {
		this.minimumLength = min;
		this.maximumLength = max;
	}

	public void setValue( ConfigLength v ) {
//		BoofSwingUtil.checkGuiThread();

		handleModeChange(v.isRelative(),false);
		updateValues(v.fraction,v.length);
	}

	private void updateValues( double fraction , double length ) {
		spinnerFraction.removeChangeListener(this);
		textLength.removePropertyChangeListener("value",this);

		if(modeRelative) {
			valueFraction.fraction = fraction;
			valueFraction.length = length;
			spinnerFraction.setEnabled(true);
			spinnerFraction.setValue(fraction);
		} else {
			valueFixed.length = length;
			spinnerFraction.setEnabled(false);
			spinnerFraction.setValue(0);
		}

		if( integerMode )
			textLength.setValue((int)length);
		else
			textLength.setValue(length);

		spinnerFraction.addChangeListener(this);
		textLength.addPropertyChangeListener("value",this);
	}

	private void handleModeChange( boolean relative , boolean updateValues) {
		if( modeRelative == relative )
			return;

		this.modeRelative = relative;
		toggleFixed.removeActionListener(this);
		toggleFixed.setSelected(relative);
		toggleFixed.addActionListener(this);

		if( updateValues ) {
			if (modeRelative) {
				updateValues(valueFraction.fraction, valueFraction.length);
			} else {
				updateValues(valueFixed.fraction, valueFixed.length);
			}
		}

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		double l = ((Number) textLength.getValue()).doubleValue();

		boolean boundsIssue = l < minimumLength || l > maximumLength;
		if( boundsIssue ) {
			l = Math.min(maximumLength,Math.max(minimumLength,l));
		}

		if(modeRelative) {
			valueFraction.length = l;
		} else {
			valueFixed.length = l;
		}

		if( boundsIssue ) {
			textLength.removePropertyChangeListener("value",this);
			if( integerMode )
				textLength.setValue((int)l);
			else
				textLength.setValue(l);
			textLength.addPropertyChangeListener("value",this);
		}

		notifyListener();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == toggleFixed ) {
			handleModeChange(toggleFixed.isSelected(),true);
			notifyListener();
		}
	}

	public ConfigLength getValue() {
		if(modeRelative)
			return valueFraction;
		else
			return valueFixed;
	}

	public void notifyListener() {
		if(modeRelative) {
			listener.changeConfigLength(this,valueFraction.fraction,valueFraction.length);
		} else {
			listener.changeConfigLength(this, -1, valueFixed.length);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerFraction ) {
			if(modeRelative) {
				valueFraction.fraction = ((Number)spinnerFraction.getValue()).doubleValue();
				notifyListener();
			}
		}
	}

	public interface Listener
	{
		void changeConfigLength(JConfigLength source, double fraction, double length);
	}
}
