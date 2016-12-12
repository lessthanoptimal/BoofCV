/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.ImageGray;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Provides a simplified set of controls for changing the view in a {@link DisparityPointCloudViewer}.
 * Uses the mouse to move the camera in its local X,Y plane.  Widgets in the control bar
 * allow the user to change the camera's tilt and distance from the origin.
 *
 * @author Peter Abeles
 */
public class PointCloudTiltPanel extends JPanel
	implements ActionListener, ChangeListener, MouseListener, MouseMotionListener
{
	// Point cloud viewer
	DisparityPointCloudViewer view;

	// when pressed sets the view to "home"
	JButton homeButton;
	// Adjusts the amount of zoom
	JSpinner rangeSpinner;
	// Tilts the camera up and down
	JSlider tiltSlider;

	// bounds on scale adjustment
	double minRange = 0;
	double maxRange = 20;

	// previous mouse location
	int prevX;
	int prevY;

	public PointCloudTiltPanel() {
		super(new BorderLayout());

		addMouseListener(this);
		addMouseMotionListener(this);

		view = new DisparityPointCloudViewer();
		JToolBar toolBar = createToolBar();

		add(toolBar, BorderLayout.PAGE_START);
		add(view, BorderLayout.CENTER);
	}

	private JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar("Controls");

		homeButton = new JButton("Home");
		homeButton.addActionListener(this);

		rangeSpinner = new JSpinner(new SpinnerNumberModel(view.range, minRange, maxRange, 0.2));

		rangeSpinner.addChangeListener(this);
		rangeSpinner.setMaximumSize(rangeSpinner.getPreferredSize());

		tiltSlider = new JSlider(JSlider.HORIZONTAL,
				-120, 120, view.tiltAngle);
		tiltSlider.addChangeListener(this);
		tiltSlider.setMajorTickSpacing(60);
		tiltSlider.setPaintLabels(true);

		toolBar.add(homeButton);
		toolBar.add(new JToolBar.Separator(new Dimension(10,1)));
		toolBar.add(new JLabel("Range:"));
		toolBar.add(rangeSpinner);
		toolBar.add(new JToolBar.Separator(new Dimension(10,1)));
		toolBar.add(new JLabel("Tilt Angle:"));
		toolBar.add(tiltSlider);

		return toolBar;
	}

	/**
	 * Specified intrinsic camera parameters and disparity settings
	 *
	 * @param baseline Stereo baseline
	 * @param K rectified camera calibration matrix
	 */
	public void configure(double baseline,
						  DenseMatrix64F K,
						  Point2Transform2_F64 rectifiedToColor,
						  int minDisparity, int maxDisparity) {
		view.configure(baseline, K, rectifiedToColor, minDisparity, maxDisparity);
	}

	/**
	 * Updates the view, must be called in a GUI thread
	 */
	public void process(ImageGray disparity , BufferedImage color ) {
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
			view.range = 1;

			tiltSlider.removeChangeListener(this);
			tiltSlider.setValue(view.tiltAngle);
			tiltSlider.addChangeListener(this);

			rangeSpinner.removeChangeListener(this);
			rangeSpinner.setValue(view.range);
			rangeSpinner.addChangeListener(this);
		}
		view.repaint();
	}

	@Override
	public void stateChanged(ChangeEvent e) {

		if( e.getSource() == rangeSpinner) {
			view.range = ((Number) rangeSpinner.getValue()).doubleValue();
		} else if( e.getSource() == tiltSlider ) {
			view.tiltAngle = ((Number)tiltSlider.getValue()).intValue();
		}
		view.repaint();
	}

	@Override
	public synchronized void mouseClicked(MouseEvent e) {

		double range = view.range;
		if( e.isShiftDown())
			range *= 0.75;
		else
			range *= 1.25;

		if( range < minRange) range = minRange;
		if( range > maxRange) range = maxRange;
		rangeSpinner.setValue(range);
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
