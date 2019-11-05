/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.disparity;

import boofcv.factory.transform.census.CensusType;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Controls GUI and settings for disparity calculation
 *
 * @author Peter Abeles
 */
public class DisparityDisplayPanel extends StandardAlgConfigPanel
		implements ChangeListener, ActionListener
{
	// which image to show
	int selectedView;

	public double zoom = 1;

	boolean concurrent=true;
	boolean recompute=true;
	boolean colorInvalid = false;
	boolean useSubpixel = true;

	// range 0 to 1000
	int periodAdjust=500;
	int offsetAdjust=500;
	int speedAdjust=500;

	// which algorithm to run
	int selectedAlg = 0;
	// Which error method to use
	int selectedError = 0;
	// The sub type of error
	int selectedErrorVariant = 0;
	int colorScheme = 0;
	// minimum disparity to calculate
	int minDisparity = 0;
	// maximum disparity to calculate
	int maxDisparity = 150;
	// maximum allowed per pixel error
	int pixelError = 30;
	// reverse association tolerance
	int reverseTol = 1;
	// how large the region radius is
	int regionRadius = 3;
	// How diverse the texture needs to be
	double texture = 0.15;
	// scale factor for input images
	int inputScale = 100;

	protected JLabel processingTimeLabel = new JLabel();
	protected JLabel imageSizeLabel = new JLabel();

	// For zooming in and out of images
	protected JSpinner selectZoom = spinner(1,MIN_ZOOM,MAX_ZOOM,0.1);

	// how much the input should be scaled down by
	JSpinner inputScaleSpinner = spinner(inputScale,5,100,10);
	// selects which image to view
	JComboBox viewSelector = combo(selectedView,"Disparity","Left","Right","View 3D");
	// If the point cloud should be colorized or not
	JComboBox comboColorizer = combo(0,"Color","X","Y","Z","X-YZ","Y-XZ","Z-XY");

	JSlider sliderPeriodColor = slider(0,1000,periodAdjust,120);
	JSlider sliderOffsetColor = slider(0,1000,offsetAdjust,120);
	JSlider sliderSpeed3D = slider(0,1000,speedAdjust,120);

	// toggles if invalid pixels are black or not
	JCheckBox invalidToggle = checkbox("Color Invalid",colorInvalid);

	JCheckBox checkRecompute  = checkbox("Recompute",recompute);
	JCheckBox checkConcurrent = checkbox("concurrent",concurrent);

	JComboBox comboAlg = combo(selectedAlg,"Five Regions","Region","Region Basic");
	JComboBox comboError = combo(selectedError,"SAD","Census");
	JComboBox comboErrorVariant = combo(selectedErrorVariant,"FOOBAR");
	JSpinner minDisparitySpinner = spinner(minDisparity,0,255,5);
	JSpinner maxDisparitySpinner = spinner(maxDisparity,1,255,5);
	JCheckBox subpixelToggle = checkbox("Subpixel",useSubpixel);
	JSpinner radiusSpinner = spinner(regionRadius,1,30,1);
	JSpinner errorSpinner = spinner(pixelError,-1,80,5);
	JSpinner reverseSpinner = spinner(reverseTol,-1,50,1);
	JSpinner textureSpinner = spinner(texture,0.0,1.0,0.05,1,3);

	// listener for changes in states
	Listener listener;

	public DisparityDisplayPanel() {

		update3DControls();
		updateErrorVariant();

		addLabeled(processingTimeLabel, "Time (ms)");
		addLabeled(imageSizeLabel,"Image Size");
		addLabeled(viewSelector, "View");
		addLabeled(selectZoom,"Zoom");
		addLabeled(comboColorizer,"Color");
		addLabeled(sliderOffsetColor,"Offset");
		addLabeled(sliderPeriodColor,"Period");
		addLabeled(sliderSpeed3D,"Speed");
		addAlignLeft(invalidToggle);
		addSeparator(150);
		addLabeled(comboAlg,"Method");
		addLabeled(comboError,"Error Type");
		addLabeled(comboErrorVariant,"Variant");
		addLabeled(minDisparitySpinner, "Min Disparity");
		addLabeled(maxDisparitySpinner, "Max Disparity");
		addAlignLeft(subpixelToggle);
		addLabeled(radiusSpinner,    "Region Radius");
		addLabeled(errorSpinner,     "Max Error");
		addLabeled(textureSpinner,   "Texture");
		addLabeled(reverseSpinner,   "Reverse");
		addSeparator(150);
		addLabeled(inputScaleSpinner, "Scale Input");
		addAlignLeft(checkRecompute);
		addAlignLeft(checkConcurrent);
		addVerticalGlue();

		setPreferredSize(new Dimension(180,0));
	}

	public void setZoom( double _zoom ) {
		_zoom = Math.max(MIN_ZOOM,_zoom);
		_zoom = Math.min(MAX_ZOOM,_zoom);
		if( _zoom == zoom )
			return;
		zoom = _zoom;

		BoofSwingUtil.invokeNowOrLater(() -> selectZoom.setValue(zoom));
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == inputScaleSpinner) {
			inputScale = ((Number) inputScaleSpinner.getValue()).intValue();
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
		} else if( e.getSource() == sliderPeriodColor ) {
			periodAdjust = sliderPeriodColor.getValue();
			listener.changeView3D();
			return;
		} else if( e.getSource() == sliderOffsetColor ) {
			offsetAdjust = sliderOffsetColor.getValue();
			listener.changeView3D();
			return;
		} else if( e.getSource() == sliderSpeed3D) {
			speedAdjust = sliderSpeed3D.getValue();
			listener.changeView3D();
			return;
		} else if( e.getSource() == selectZoom ) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			listener.changeZoom();
			return;
		}

		if( minDisparity >= maxDisparity ) {
			minDisparity = maxDisparity-1;
			minDisparitySpinner.setValue(minDisparity);
		} else {
			listener.disparitySettingChange();
		}
	}

	private void updateErrorVariant() {
		DefaultComboBoxModel model;
		if( selectedError == 0 ) {
			selectedErrorVariant = -1;
			model = new DefaultComboBoxModel();
			comboErrorVariant.setEnabled(false);
		} else {
			selectedErrorVariant = 0;
			model = new DefaultComboBoxModel(CensusType.values());
			comboErrorVariant.setEnabled(true);
		}
		comboErrorVariant.setModel(model);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == viewSelector ) {
			selectedView = viewSelector.getSelectedIndex();
			update3DControls();
			listener.disparityGuiChange();
		} else if( e.getSource() == comboColorizer) {
			colorScheme = comboColorizer.getSelectedIndex();
			listener.changeView3D();
		} else if( e.getSource() == comboAlg) {
			selectedAlg = comboAlg.getSelectedIndex();
			listener.algorithmChanged();
		} else if( e.getSource() == comboError) {
			selectedError = comboError.getSelectedIndex();
			updateErrorVariant();
			listener.disparitySettingChange();
		} else if( e.getSource() == comboErrorVariant) {
			selectedErrorVariant = comboErrorVariant.getSelectedIndex();
			listener.disparitySettingChange();
		} else if( e.getSource() == invalidToggle) {
			colorInvalid = invalidToggle.isSelected();
			listener.disparityRender();
		} else if( e.getSource() == subpixelToggle ) {
			useSubpixel = subpixelToggle.isSelected();
			listener.disparitySettingChange();
		} else if( e.getSource() == checkRecompute ) {
			recompute = checkRecompute.isSelected();
			listener.disparitySettingChange();
		} else if( e.getSource() == checkConcurrent ) {
			concurrent = checkConcurrent.isSelected();
			listener.disparitySettingChange();
		}
	}

	private void update3DControls() {
		// disable controls which can't be used
		boolean view3D = selectedView==3;
		comboColorizer.setEnabled(view3D);
		sliderOffsetColor.setEnabled(view3D);
		sliderPeriodColor.setEnabled(view3D);
		sliderSpeed3D.setEnabled(view3D);
	}

	public double periodScale() {
		if( periodAdjust > 500 ) {
			double f = (periodAdjust-500)/500.0;
			return 1.0+f*10;
		} else if( periodAdjust < 500 ) {
			double f = (500-periodAdjust)/500.0;
			return 1.0-0.98*f;
		} else {
			return 1.0;
		}
	}

	public double offsetScale() {
		return (offsetAdjust-500)/500.0;
	}

	public double speedScale() {
		if( speedAdjust > 500 ) {
			double f = (speedAdjust-500)/500.0;
			return Math.pow(2.0,f*6);
		} else if( speedAdjust < 500 ) {
			double f = (500-speedAdjust)/500.0;
			return 1.0-0.98*f;
		} else {
			return 1.0;
		}
	}

	public void setActiveGui( boolean error , boolean reverse ) {
		errorSpinner.setEnabled(error);
		reverseSpinner.setEnabled(reverse);
		subpixelToggle.setEnabled(reverse);
		textureSpinner.setEnabled(reverse);
	}

	public void setImageSize( final int width , final int height ) {
		BoofSwingUtil.invokeNowOrLater(() -> imageSizeLabel.setText(width+" x "+height));
	}

	public void setProcessingTimeMS(double ms ) {
		BoofSwingUtil.checkGuiThread();
		processingTimeLabel.setText(String.format("%7.1f",ms));
	}

	public void setListener(Listener listener ) {
		this.listener = listener;
	}

	public interface Listener
	{
		void algorithmChanged();
		void disparitySettingChange();
		void disparityGuiChange();
		void disparityRender();
		void changeInputScale();
		void changeView3D();
		void changeZoom();
	}
}
