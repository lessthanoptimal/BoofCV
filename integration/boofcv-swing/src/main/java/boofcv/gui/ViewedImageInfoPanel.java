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

package boofcv.gui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Shows information about the current view. Image size, amount of zoom, where
 * the user has clicked
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ViewedImageInfoPanel extends StandardAlgConfigPanel
		implements ChangeListener, MouseWheelListener {
	double zoomMin, zoomMax, zoomInc;

	protected JLabel processingTimeLabel = new JLabel();

	JTextField textImageSize = new JTextField(8);

	// cursor location
	JTextField textCursorX = new JTextField(8);
	JTextField textCursorY = new JTextField(8);

	JSpinner selectZoom;

	Listener listener;

	double zoom = 1;

	public ViewedImageInfoPanel() {
		this(BoofSwingUtil.MIN_ZOOM, BoofSwingUtil.MAX_ZOOM, 0.5, true);
	}

	public ViewedImageInfoPanel( double zoomMin, double zoomMax, double zoomInc, boolean showClick ) {

		this.zoomMin = zoomMin;
		this.zoomMax = zoomMax;
		this.zoomInc = zoomInc;

		textImageSize.setEditable(false);
		textImageSize.setHorizontalAlignment(SwingConstants.RIGHT);
		textImageSize.setMaximumSize(textImageSize.getPreferredSize());

		textCursorX.setEditable(false);
		textCursorX.setMaximumSize(textCursorX.getPreferredSize());
		textCursorY.setEditable(false);
		textCursorY.setMaximumSize(textCursorY.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(zoom, zoomMin, zoomMax, zoomInc));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		addLabeled(textImageSize, "Input Shape");
		addLabeled(processingTimeLabel, "Time (ms)");
		if (showClick) {
			addLabeled(textCursorX, "Click X");
			addLabeled(textCursorY, "Click Y");
		}
		addLabeled(selectZoom, "Zoom");
	}

	public void setImageSize( int width, int height ) {
		textImageSize.setText(String.format("%d x %d", width, height));
	}

	public void setCursor( double x, double y ) {
		textCursorX.setText(String.format("%5.3f", x));
		textCursorY.setText(String.format("%5.3f", y));
	}

	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	public double getZoom() {
		return zoom;
	}

	@Override
	public void stateChanged( ChangeEvent e ) {
		if (selectZoom == e.getSource()) {
			zoom = ((Number)selectZoom.getValue()).doubleValue();
			if (listener != null)
				listener.zoomChanged(zoom);
		} else {
			super.stateChanged(e);
		}
	}

	@Override
	public void mouseWheelMoved( MouseWheelEvent e ) {
		setScale(BoofSwingUtil.mouseWheelImageZoom(zoom, e));
	}

	public void setScale( double scale ) {
		BoofSwingUtil.checkGuiThread();
		if (((Number)selectZoom.getValue()).doubleValue() == scale)
			return;

		double curr = scale;

		if (curr < zoomMin) curr = zoomMin;
		if (curr > zoomMax) curr = zoomMax;

		selectZoom.setValue(curr);
	}

	public void setProcessingTimeS( double seconds ) {
		BoofSwingUtil.checkGuiThread();
		processingTimeLabel.setText(String.format("%7.1f", (seconds*1000)));
	}

	public void setProcessingTimeMS( double ms ) {
		BoofSwingUtil.checkGuiThread();
		processingTimeLabel.setText(String.format("%7.1f", ms));
	}

	public interface Listener {
		void zoomChanged( double zoom );
	}
}
