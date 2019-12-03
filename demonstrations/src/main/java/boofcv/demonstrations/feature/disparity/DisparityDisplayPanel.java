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

	int colorScheme = 0;
	// scale factor for input images
	int inputScale = 100;

	// Default background color for 3D cloud
	int backgroundColor = 0x000000;

	protected JLabel processingTimeLabel = new JLabel();
	protected JLabel imageSizeLabel = new JLabel();

	// For zooming in and out of images
	protected JSpinner selectZoom = spinner(1,MIN_ZOOM,MAX_ZOOM,0.1);

	JButton bColorBackGround;

	// how much the input should be scaled down by
	JSpinner inputScaleSpinner = spinner(inputScale,5,100,10);
	// selects which image to view
	JComboBox viewSelector = combo(selectedView,"Disparity","Left","Right","View 3D");
	// If the point cloud should be colorized or not
	JComboBox comboColorizer = combo(0,"Color","X-YZ","Y-XZ","Z-XY","RB-X","RB-Y","RB-Z","RGB-X","RGB-Y","RGB-Z");

	JSlider sliderPeriodColor = slider(0,1000,periodAdjust,120);
	JSlider sliderOffsetColor = slider(0,1000,offsetAdjust,120);
	JSlider sliderSpeed3D = slider(0,1000,speedAdjust,120);

	// toggles if invalid pixels are black or not
	JCheckBox invalidToggle = checkbox("Color Invalid",colorInvalid);

	JCheckBox checkRecompute  = checkbox("Recompute",recompute);
	JCheckBox checkConcurrent = checkbox("concurrent",concurrent);

	DisparityControlPanel controlDisparity;

	// listener for changes in states
	Listener listener;

	public DisparityDisplayPanel( int disparityMin , int disparityRange,  Class imageType ) {
		controlDisparity = DisparityControlPanel.createRange(disparityMin,disparityRange,imageType);
		controlDisparity.setListener(()->listener.algorithmChanged());

		update3DControls();

		addLabeled(processingTimeLabel, "Time (ms)");
		addLabeled(imageSizeLabel,"Image Size");
		addLabeled(viewSelector, "View");
		addLabeled(selectZoom,"Zoom");
		addLabeled(createColorPanel(),"Color");
		addLabeled(sliderOffsetColor,"Offset");
		addLabeled(sliderPeriodColor,"Period");
		addLabeled(sliderSpeed3D,"Speed");
		addAlignLeft(invalidToggle);
		add(controlDisparity);
		addLabeled(inputScaleSpinner, "Scale Input");
		addAlignLeft(checkRecompute);
		addAlignLeft(checkConcurrent);
		addVerticalGlue();

		setPreferredSize(new Dimension(200,0));
	}

	/**
	 * Button for selecting background color and another for selecting how to colorize
	 */
	private JPanel createColorPanel() {
		bColorBackGround = new JButton();
		bColorBackGround.addActionListener(e->{
			Color newColor = JColorChooser.showDialog(
					DisparityDisplayPanel.this,
					"Background Color",
					new Color(backgroundColor));
			backgroundColor = newColor.getRGB();
			bColorBackGround.setBackground(newColor);
			listener.changeBackgroundColor();
		});
		bColorBackGround.setPreferredSize(new Dimension(20,20));
		bColorBackGround.setMinimumSize(bColorBackGround.getPreferredSize());
		bColorBackGround.setMaximumSize(bColorBackGround.getPreferredSize());
		bColorBackGround.setBackground(new Color(backgroundColor));

		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.setBorder(BorderFactory.createEmptyBorder());
		p.add(bColorBackGround);
		p.add(Box.createRigidArea(new Dimension(5,5)));
		p.add(comboColorizer);
		return p;
	}

	/**
	 * Disable any control which can cause a request for the disparity to be recomputed by the user
	 */
	public void enableAlgControls( boolean enable ) {
		BoofSwingUtil.checkGuiThread();
		BoofSwingUtil.recursiveEnable(controlDisparity,enable);
		inputScaleSpinner.setEnabled(enable);
		checkRecompute.setEnabled(enable);
		checkConcurrent.setEnabled(enable);
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
		} if( e.getSource() == sliderPeriodColor ) {
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
		} else {
			throw new RuntimeException("Egads");
		}
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
		} else if( e.getSource() == checkRecompute ) {
			recompute = checkRecompute.isSelected();
			listener.recompute();
		} else if( e.getSource() == checkConcurrent ) {
			concurrent = checkConcurrent.isSelected();
			listener.recompute();
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
		void recompute();
		void disparityGuiChange();
		void disparityRender();
		void changeInputScale();
		void changeView3D();
		void changeZoom();
		void changeBackgroundColor();
	}
}
