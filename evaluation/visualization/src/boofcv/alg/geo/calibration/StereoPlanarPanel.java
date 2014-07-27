/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.abst.calib.ImageResults;
import boofcv.alg.distort.ImageDistort;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Panel for displaying information on observed calibration grids during the calibration process.
 *
 * @author Peter Abeles
 */
public class StereoPlanarPanel extends JPanel
	implements ListSelectionListener, ItemListener, ChangeListener, MouseListener
{
	// display for calibration information on individual cameras
	CalibratedImageGridPanel leftView = new CalibratedImageGridPanel();
	CalibratedImageGridPanel rightView = new CalibratedImageGridPanel();

	// list of images and calibration results
	List<BufferedImage> listLeft = new ArrayList<BufferedImage>();
	List<BufferedImage> listRight = new ArrayList<BufferedImage>();

	List<ImageResults> leftResults;
	List<ImageResults> rightResults;

	// GUI components
	JCheckBox checkPoints;
	JCheckBox checkErrors;
	JCheckBox checkUndistorted;
	JCheckBox checkAll;
	JCheckBox checkNumbers;
	JSpinner selectErrorScale;

	JTextArea meanErrorLeft;
	JTextArea maxErrorLeft;
	JTextArea meanErrorRight;
	JTextArea maxErrorRight;

	JList imageList;
	List<String> names = new ArrayList<String>();

	int selectedImage;

	boolean showPoints = true;
	boolean showErrors = true;
	boolean showUndistorted = false;
	boolean showAll = false;
	boolean showNumbers = true;
	double errorScale = 20;

	public StereoPlanarPanel() {
		super(new BorderLayout());

		leftView.setImages(listLeft);
		rightView.setImages(listRight);

		imageList = new JList();
		imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		imageList.addListSelectionListener(this);

		meanErrorLeft = createTextComponent();
		maxErrorLeft = createTextComponent();
		meanErrorRight = createTextComponent();
		maxErrorRight = createTextComponent();

		leftView.addMouseListener(this);
		rightView.addMouseListener(this);

		JToolBar toolBar = createToolBar();

		JPanel center = new JPanel();
		center.setLayout(new GridLayout(1,2));
		center.add(leftView);
		center.add(rightView);

		add(toolBar, BorderLayout.PAGE_START);
		add(center, BorderLayout.CENTER);
		add(new SideBar(), BorderLayout.WEST);
	}

	private JTextArea createTextComponent() {
		JTextArea comp = new JTextArea(1,6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}

	private JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar("Controls");

		checkPoints = new JCheckBox("Show Points");
		checkPoints.setSelected(showPoints);
		checkPoints.addItemListener(this);

		checkErrors = new JCheckBox("Show Errors");
		checkErrors.setSelected(showErrors);
		checkErrors.addItemListener(this);

		checkAll = new JCheckBox("All Points");
		checkAll.setSelected(showAll);
		checkAll.addItemListener(this);

		checkUndistorted = new JCheckBox("Rectify");
		checkUndistorted.setSelected(showUndistorted);
		checkUndistorted.addItemListener(this);
		checkUndistorted.setEnabled(false);

		checkNumbers = new JCheckBox("Numbers");
		checkNumbers.setSelected(showNumbers);
		checkNumbers.addItemListener(this);

		selectErrorScale = new JSpinner(new SpinnerNumberModel(errorScale, 5, 100, 5));
		selectErrorScale.addChangeListener(this);
		selectErrorScale.setMaximumSize(selectErrorScale.getPreferredSize());

		toolBar.add(checkPoints);
		toolBar.add(checkErrors);
		toolBar.add(checkAll);
		toolBar.add(checkUndistorted);
		toolBar.add(checkNumbers);
		toolBar.add(new JLabel("| Error Scale"));
		toolBar.add(selectErrorScale);
		return toolBar;
	}

	public void setRectification( ImageDistort<ImageFloat32,ImageFloat32> rectifyLeft ,
								  ImageDistort<ImageFloat32,ImageFloat32> rectifyRight ) {
		leftView.setDistorted(rectifyLeft);
		rightView.setDistorted(rectifyRight);
		checkUndistorted.setEnabled(true);
	}

	public void addPair( String name , BufferedImage imageLeft , BufferedImage imageRight )
	{
		listLeft.add(imageLeft);
		listRight.add(imageRight);
		names.add(name);

		imageList.removeListSelectionListener(this);
		imageList.setListData(new Vector<Object>(names));
		if( names.size() == 1 ) {
			imageList.addListSelectionListener(this);
			leftView.setPreferredSize(new Dimension(imageLeft.getWidth(), imageLeft.getHeight()));
			rightView.setPreferredSize(new Dimension(imageRight.getWidth(), imageRight.getHeight()));
			imageList.setSelectedIndex(0);
		} else {
			imageList.addListSelectionListener(this);
		}
		validate();
	}

	public synchronized void setObservations( List<List<Point2D_F64>> leftObservations , List<ImageResults> leftResults ,
											  List<List<Point2D_F64>> rightObservations , List<ImageResults> rightResults ) {
		leftView.setResults(leftObservations,leftResults);
		rightView.setResults(rightObservations,rightResults);

		this.leftResults = leftResults;
		this.rightResults = rightResults;

		// synchronize configurations
		leftView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,errorScale);
		rightView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,errorScale);

		updateResultsGUI();
	}

	protected void setSelected( int selected ) {
		selectedImage = selected;
		leftView.setSelected(selected);
		rightView.setSelected(selected);

		updateResultsGUI();
	}

	private synchronized void updateResultsGUI() {
		if( leftResults != null && rightResults != null && selectedImage < leftResults.size() ) {
			ImageResults r = leftResults.get(selectedImage);
			String textMean = String.format("%5.1e", r.meanError);
			String textMax = String.format("%5.1e",r.maxError);
			meanErrorLeft.setText(textMean);
			maxErrorLeft.setText(textMax);

			r = rightResults.get(selectedImage);
			textMean = String.format("%5.1e", r.meanError);
			textMax = String.format("%5.1e",r.maxError);
			meanErrorRight.setText(textMean);
			maxErrorRight.setText(textMax);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( e.getSource() == checkPoints ) {
			showPoints = checkPoints.isSelected();
		} else if( e.getSource() == checkErrors ) {
			showErrors = checkErrors.isSelected();
		} else if( e.getSource() == checkAll ) {
			showAll = checkAll.isSelected();
		} else if( e.getSource() == checkUndistorted ) {
			showUndistorted = checkUndistorted.isSelected();
		} else if( e.getSource() == checkNumbers ) {
			showNumbers = checkNumbers.isSelected();
		}
		leftView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,errorScale);
		rightView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,errorScale);
		leftView.repaint();
		rightView.repaint();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if( e.getValueIsAdjusting() || e.getFirstIndex() == -1)
			return;

		if( imageList.getSelectedIndex() >= 0 ) {
			setSelected(imageList.getSelectedIndex());
			leftView.repaint();
			rightView.repaint();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectErrorScale) {
			errorScale = ((Number) selectErrorScale.getValue()).intValue();
		}

		leftView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,errorScale);
		rightView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,errorScale);
		leftView.repaint();
		rightView.repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		leftView.setLine(e.getY());
		rightView.setLine(e.getY());
		leftView.repaint();
		rightView.repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	private class SideBar extends StandardAlgConfigPanel
	{
		public SideBar() {
			JScrollPane scroll = new JScrollPane(imageList);

			addCenterLabel("Left",this);
			addLabeled(meanErrorLeft,"Mean Error",this);
			addLabeled(maxErrorLeft, "Max Error", this);
			addSeparator(200);
			addCenterLabel("Right",this);
			addLabeled(meanErrorRight,"Mean Error",this);
			addLabeled(maxErrorRight, "Max Error", this);
			addSeparator(200);
			add(scroll);
		}
	}


}
