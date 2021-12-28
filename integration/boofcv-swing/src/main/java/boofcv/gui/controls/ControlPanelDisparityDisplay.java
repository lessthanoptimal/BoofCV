/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.controls;

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
@SuppressWarnings({"NullAway.Init"})
public class ControlPanelDisparityDisplay extends StandardAlgConfigPanel
		implements ChangeListener, ActionListener {
	// which image to show
	public int selectedView;

	public double zoom = 1;

	public boolean concurrent = true;
	public boolean recompute = true;

	// scale factor for input images
	public int inputScale = 100;

	// Background color for disparity
	public int backgroundColorDisparity = 0x000000;

	protected JLabel processingTimeLabel = new JLabel();
	protected JLabel imageSizeLabel = new JLabel();

	// For zooming in and out of images
	protected JSpinner selectZoom = spinner(1, MIN_ZOOM, MAX_ZOOM, 0.1);

	// how much the input should be scaled down by
	JSpinner inputScaleSpinner = spinner(inputScale, 5, 100, 10);
	// selects which image to view
	JComboBox viewSelector = combo(selectedView, "Disparity", "Left", "Right", "View 3D");

	public ControlCustomCloud controlCloud = new ControlCustomCloud();

	JCheckBox checkRecompute = checkbox("Recompute", recompute);
	JCheckBox checkConcurrent = checkbox("concurrent", concurrent);

	public ControlPanelDisparityDense controlDisparity;

	// listener for changes in states
	Listener listener;

	public ControlPanelDisparityDisplay( int disparityMin, int disparityRange, Class imageType ) {
		controlDisparity = ControlPanelDisparityDense.createRange(disparityMin, disparityRange, imageType);
		controlDisparity.setListener(() -> listener.algorithmChanged());

		// Slim it down a little bit
		controlCloud.setBorder(BorderFactory.createEmptyBorder());
		controlCloud.setCallbackModified(() -> listener.changeView3D());
		controlCloud.setCallbackBackground(() -> listener.changeBackgroundColor());

		controlCloud.handleViewChange();

		addLabeled(processingTimeLabel, "Time (ms)");
		addLabeled(imageSizeLabel, "Image Size");
		addLabeled(viewSelector, "View");
		addLabeled(selectZoom, "Zoom");
		add(controlCloud);
		add(controlDisparity);
		addLabeled(inputScaleSpinner, "Scale Input");
		addAlignLeft(checkRecompute);
		addAlignLeft(checkConcurrent);
		addVerticalGlue();

		setPreferredSize(new Dimension(200, 0));
	}

	/**
	 * Disable any control which can cause a request for the disparity to be recomputed by the user
	 */
	public void enableAlgControls( boolean enable ) {
		BoofSwingUtil.checkGuiThread();
		BoofSwingUtil.recursiveEnable(controlDisparity, enable);
		if (enable)
			controlDisparity.updateControlEnabled();
		inputScaleSpinner.setEnabled(enable);
		checkRecompute.setEnabled(enable);
		checkConcurrent.setEnabled(enable);
	}

	public void setZoom( double _zoom ) {
		_zoom = Math.max(MIN_ZOOM, _zoom);
		_zoom = Math.min(MAX_ZOOM, _zoom);
		if (_zoom == zoom)
			return;
		zoom = _zoom;

		BoofSwingUtil.invokeNowOrLater(() -> selectZoom.setValue(zoom));
	}

	@Override
	public void stateChanged( ChangeEvent e ) {
		if (listener == null)
			return;

		if (e.getSource() == inputScaleSpinner) {
			inputScale = ((Number)inputScaleSpinner.getValue()).intValue();
			listener.changeInputScale();
		} else if (e.getSource() == selectZoom) {
			zoom = ((Number)selectZoom.getValue()).doubleValue();
			listener.changeZoom();
		} else {
			throw new RuntimeException("Egads");
		}
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (listener == null)
			return;

		if (e.getSource() == viewSelector) {
			selectedView = viewSelector.getSelectedIndex();
			controlCloud.handleViewChange();
			listener.disparityGuiChange();
		} else if (e.getSource() == checkRecompute) {
			recompute = checkRecompute.isSelected();
			listener.recompute();
		} else if (e.getSource() == checkConcurrent) {
			concurrent = checkConcurrent.isSelected();
			listener.recompute();
		}
	}

	public void setImageSize( final int width, final int height ) {
		BoofSwingUtil.invokeNowOrLater(() -> imageSizeLabel.setText(width + " x " + height));
	}

	public void setProcessingTimeMS( double ms ) {
		BoofSwingUtil.checkGuiThread();
		processingTimeLabel.setText(String.format("%7.1f", ms));
	}

	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	public class ControlCustomCloud extends ControlPanelPointCloud {

		private void handleViewChange() {
			setColorButtonColor(getActiveBackgroundColor());
			// disable controls which can't be used
			boolean view3D = selectedView == 3;
			comboColorizer.setEnabled(view3D);
			sliderOffsetColor.setEnabled(view3D);
			sliderPeriodColor.setEnabled(view3D);
			sliderSpeed3D.setEnabled(view3D);
			// Color is useful for disparity and 3D
			bColorBackGround.setEnabled(selectedView == 0 || selectedView == 3);
		}

		@Override
		public int getActiveBackgroundColor() {
			if (selectedView == 0) {
				return backgroundColorDisparity;
			} else if (selectedView == 3) {
				return backgroundColor3D;
			} else {
				return bColorBackGround.getBackground().getRGB();
			}
		}

		@Override
		public void setColorButtonColor( int colorRGB ) {
			if (selectedView == 0) {
				backgroundColorDisparity = colorRGB;
			} else if (selectedView == 3) {
				backgroundColor3D = colorRGB;
			} else {
				return;
			}
			bColorBackGround.repaint();
		}
	}

	public interface Listener {
		void algorithmChanged();
		void recompute();
		void disparityGuiChange();
		void disparityRender();
		void changeInputScale();
		void changeView3D();
		void changeZoom();
		void changeBackgroundColor();
	}
}
