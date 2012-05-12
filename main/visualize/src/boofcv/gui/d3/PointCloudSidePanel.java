/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.d3;

import boofcv.struct.image.ImageSingleBand;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Provides user controls for adjusting the view in {@link PointCloudSideViewer}.  Allows
 * both mouse and widget commands in a toolbar.
 *
 * @author Peter Abeles
 */
public class PointCloudSidePanel extends JPanel
	implements ActionListener, ChangeListener, MouseListener, MouseMotionListener
{
	// Point cloud viewer
	PointCloudSideViewer view;

	// when pressed sets the view to "home"
	JButton homeButton;
	// Adjusts the amount of zoom
	JSpinner zoomSpinner;
	// Tilts the camera up and down
	JSlider tiltSlider;

	// how much it should move the camera in and out of the view
	double zoom = 1;
	// bounds on scale adjustment
	double minZoom = 0;
	double maxZoom = 20;

	// previous mouse location
	int prevX;
	int prevY;

	public PointCloudSidePanel() {
		super(new BorderLayout());

		addMouseListener(this);
		addMouseMotionListener(this);

		view = new PointCloudSideViewer();
		JToolBar toolBar = createToolBar();

		add(toolBar, BorderLayout.PAGE_START);
		add(view, BorderLayout.CENTER);
	}

	private JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar("Controls");

		homeButton = new JButton("Home");
		homeButton.addActionListener(this);

		zoomSpinner = new JSpinner(new SpinnerNumberModel(zoom, minZoom, maxZoom, 0.2));
		zoomSpinner.addChangeListener(this);
		zoomSpinner.setMaximumSize(zoomSpinner.getPreferredSize());

		tiltSlider = new JSlider(JSlider.HORIZONTAL,
				-120, 120, view.tiltAngle);
		tiltSlider.addChangeListener(this);
		tiltSlider.setMajorTickSpacing(60);
		tiltSlider.setPaintLabels(true);

		toolBar.add(homeButton);
		toolBar.add(new JLabel("Zoom:"));
		toolBar.add(zoomSpinner);
		toolBar.add(new JLabel("Tilt:"));
		toolBar.add(tiltSlider);

		return toolBar;
	}

	/**
	 * Specified intrinsic camera parameters and disparity settings
	 */
	public void configure(double baseline,
						  DenseMatrix64F K,
						  int minDisparity, int maxDisparity) {
		view.configure(baseline, K, minDisparity, maxDisparity);
	}

	/**
	 * Updates the view, must be called in a GUI thread
	 */
	public void process( ImageSingleBand disparity , BufferedImage color ) {
		view.process(disparity,color);

		tiltSlider.removeChangeListener(this);
		tiltSlider.setValue(view.tiltAngle);
		tiltSlider.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if( e.getSource() == homeButton ) {
			view.offsetX = 0;
			view.offsetY = 0;
			view.tiltAngle = 0;
			view.zoom = 1;
			this.zoom = 1;

			tiltSlider.removeChangeListener(this);
			tiltSlider.setValue(view.tiltAngle);
			tiltSlider.addChangeListener(this);

			zoomSpinner.removeChangeListener(this);
			zoomSpinner.setValue(zoom);
			zoomSpinner.addChangeListener(this);
		}
		view.repaint();
	}

	@Override
	public void stateChanged(ChangeEvent e) {

		if( e.getSource() == zoomSpinner) {
			view.zoom = ((Number) zoomSpinner.getValue()).doubleValue();
		} else if( e.getSource() == tiltSlider ) {
			view.tiltAngle = ((Number)tiltSlider.getValue()).intValue();
		}
		view.repaint();
	}

	@Override
	public synchronized void mouseClicked(MouseEvent e) {

		double scale = this.zoom;
		if( e.isShiftDown())
			scale *= 0.75;
		else
			scale *= 1.25;

		if( scale < minZoom) scale = minZoom;
		if( scale > maxZoom) scale = maxZoom;
		zoomSpinner.setValue(scale);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		prevX = e.getX();
		prevY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public synchronized void mouseDragged(MouseEvent e) {
		final int deltaX = e.getX()-prevX;
		final int deltaY = e.getY()-prevY;

		view.offsetX += deltaX;
		view.offsetY += deltaY;

		prevX = e.getX();
		prevY = e.getY();

		view.repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}
}
