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

package boofcv.alg.feature.disparity;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Controls GUI and settings for disparity calculation
 *
 * @author Peter Abeles
 */
public class DisparityDisplayPanel extends StandardAlgConfigPanel
		implements ChangeListener, ItemListener
{

	// how much the input should be scaled down by
	JSpinner inputScaleSpinner;
	// selects which image to view
	JComboBox viewSelector;
	// toggles if invalid pixels are black or not
	JCheckBox invalidToggle;

	JSpinner minDisparitySpinner;
	JSpinner maxDisparitySpinner;
	JCheckBox subpixelToggle;
	JSpinner radiusSpinner;
	JSpinner errorSpinner;
	JSpinner reverseSpinner;
	JSpinner textureSpinner;

	// which image to show
	int selectedView;

	boolean colorInvalid = false;
	boolean useSubpixel = true;

	// minimum disparity to calculate
	int minDisparity = 0;
	// maximum disparity to calculate
	int maxDisparity = 50;
	// maximum allowed per pixel error
	int pixelError = 30;
	// reverse association tolerance
	int reverseTol = 6;
	// how large the region radius is
	int regionRadius = 3;
	// How diverse the texture needs to be
	double texture = 0.1;
	// scale factor for input images
	double inputScale = 0.5;


	// listener for changes in states
	Listener listener;

	public DisparityDisplayPanel() {

		inputScaleSpinner = new JSpinner(new SpinnerNumberModel(inputScale,0.25, 1, 0.1));
		inputScaleSpinner.addChangeListener(this);
		inputScaleSpinner.setMaximumSize(inputScaleSpinner.getPreferredSize());

		viewSelector = new JComboBox();
		viewSelector.addItem("Disparity");
		viewSelector.addItem("Left");
		viewSelector.addItem("Right");
		viewSelector.addItem("View 3D");
		viewSelector.addItemListener(this);
		viewSelector.setMaximumSize(viewSelector.getPreferredSize());

		invalidToggle = new JCheckBox("Color Invalid");
		invalidToggle.setSelected(colorInvalid);
		invalidToggle.addItemListener(this);
		invalidToggle.setMaximumSize(invalidToggle.getPreferredSize());

		minDisparitySpinner = new JSpinner(new SpinnerNumberModel(minDisparity,0, 255, 5));
		minDisparitySpinner.addChangeListener(this);
		minDisparitySpinner.setMaximumSize(minDisparitySpinner.getPreferredSize());

		maxDisparitySpinner = new JSpinner(new SpinnerNumberModel(maxDisparity,1, 255, 5));
		maxDisparitySpinner.addChangeListener(this);
		maxDisparitySpinner.setMaximumSize(maxDisparitySpinner.getPreferredSize());

		subpixelToggle = new JCheckBox("Subpixel");
		subpixelToggle.setSelected(useSubpixel);
		subpixelToggle.addItemListener(this);
		subpixelToggle.setMaximumSize(invalidToggle.getPreferredSize());

		radiusSpinner = new JSpinner(new SpinnerNumberModel(regionRadius,1, 30, 1));
		radiusSpinner.addChangeListener(this);
		radiusSpinner.setMaximumSize(radiusSpinner.getPreferredSize());

		errorSpinner = new JSpinner(new SpinnerNumberModel(pixelError,-1, 80, 5));
		errorSpinner.addChangeListener(this);
		errorSpinner.setMaximumSize(errorSpinner.getPreferredSize());

		reverseSpinner = new JSpinner(new SpinnerNumberModel(reverseTol,-1, 50, 1));
		reverseSpinner.addChangeListener(this);
		reverseSpinner.setMaximumSize(reverseSpinner.getPreferredSize());

		textureSpinner = new JSpinner(new SpinnerNumberModel(texture,0.0, 1, 0.05));
		textureSpinner.addChangeListener(this);
		textureSpinner.setPreferredSize(new Dimension(60,reverseSpinner.getPreferredSize().height));
		textureSpinner.setMaximumSize(textureSpinner.getPreferredSize());

		addLabeled(viewSelector, "View ", this);
		addAlignLeft(invalidToggle,this);
		addSeparator(100);
		addLabeled(minDisparitySpinner, "Min Disparity", this);
		addLabeled(maxDisparitySpinner, "Max Disparity", this);
		addAlignLeft(subpixelToggle,this);
		addLabeled(radiusSpinner,    "Region Radius", this);
		addLabeled(errorSpinner,     "Max Error", this);
		addLabeled(textureSpinner,   "Texture", this);
		addLabeled(reverseSpinner,   "Reverse", this);
		addSeparator(100);
		addLabeled(inputScaleSpinner, "Image Scale", this);
		addVerticalGlue(this);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == inputScaleSpinner) {
			inputScale = ((Number) inputScaleSpinner.getValue()).doubleValue();
			listener.changeInputScale();
			return;
		} else if( e.getSource() == reverseSpinner) {
			reverseTol = ((Number) reverseSpinner.getValue()).intValue();
		} else if( e.getSource() == minDisparitySpinner) {
			minDisparity = ((Number) minDisparitySpinner.getValue()).intValue();
		} else if( e.getSource() == maxDisparitySpinner) {
			maxDisparity = ((Number) maxDisparitySpinner.getValue()).intValue();
		} else if( e.getSource() == errorSpinner) {
			pixelError = ((Number) errorSpinner.getValue()).intValue();
		} else if( e.getSource() == radiusSpinner) {
			regionRadius = ((Number) radiusSpinner.getValue()).intValue();
		} else if( e.getSource() == textureSpinner) {
			texture = ((Number) textureSpinner.getValue()).doubleValue();
		}

		if( minDisparity >= maxDisparity ) {
			minDisparity = maxDisparity-1;
			minDisparitySpinner.setValue(minDisparity);
		} else {
			listener.disparitySettingChange();
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == viewSelector ) {
			selectedView = viewSelector.getSelectedIndex();
			listener.disparityGuiChange();
		} else if( e.getSource() == invalidToggle) {
			colorInvalid = invalidToggle.isSelected();
			listener.disparityRender();
		} else if( e.getSource() == subpixelToggle ) {
			useSubpixel = subpixelToggle.isSelected();
			listener.disparitySettingChange();
		}
	}

	public void setActiveGui( boolean error , boolean reverse ) {
		setEnabled(12,error);
		setEnabled(13,reverse);
		setEnabled(14,error);
		setEnabled(15,reverse);
	}

	public void setListener(Listener listener ) {
		this.listener = listener;
	}

	public int getReverseTol() {
		return reverseTol;
	}

	public int getMaxDisparity() {

		return maxDisparity;
	}

	public int getPixelError() {
		return pixelError;
	}

	public int getSelectedView() {
		return selectedView;
	}

	public int getRegionRadius() {
		return regionRadius;
	}

	public double getTexture() {
		return texture;
	}

	public static interface Listener
	{
		public void disparitySettingChange();

		public void disparityGuiChange();

		public void disparityRender();

		public void changeInputScale();
	}
}
