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

package boofcv.demonstrations.calibration;

import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.calibration.DisplayPinholeCalibrationPanel;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeBrown;
import org.ejml.data.DMatrixRMaj;

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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for displaying information on observed calibration grids during the calibration process.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class StereoPlanarPanel extends JPanel
		implements ListSelectionListener, ItemListener, ChangeListener, MouseListener {
	// display for calibration information on individual cameras
	DisplayPinholeCalibrationPanel leftView = new DisplayPinholeCalibrationPanel();
	DisplayPinholeCalibrationPanel rightView = new DisplayPinholeCalibrationPanel();

	// list of images and calibration results
	List<File> listLeft = new ArrayList<>();
	List<File> listRight = new ArrayList<>();

	List<CalibrationObservation> leftObservations;
	List<CalibrationObservation> rightObservations;
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

	JList<String> imageList;
	List<String> names = new ArrayList<>();

	int selectedImage;

	boolean showPoints = true;
	boolean showErrors = true;
	boolean showUndistorted = false;
	boolean showAll = false;
	boolean showNumbers = true;
	boolean showOrder = true;
	double errorScale = 20;

	public StereoPlanarPanel() {
		super(new BorderLayout());

		imageList = new JList();
		imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		imageList.addListSelectionListener(this);

		meanErrorLeft = createTextComponent();
		maxErrorLeft = createTextComponent();
		meanErrorRight = createTextComponent();
		maxErrorRight = createTextComponent();

		leftView.getImagePanel().addMouseListener(this);
		rightView.getImagePanel().addMouseListener(this);

		JToolBar toolBar = createToolBar();

		JPanel center = new JPanel();
		center.setLayout(new GridLayout(1, 2));
		center.add(leftView);
		center.add(rightView);

		add(toolBar, BorderLayout.PAGE_START);
		add(center, BorderLayout.CENTER);
		add(new SideBar(), BorderLayout.WEST);
	}

	private JTextArea createTextComponent() {
		JTextArea comp = new JTextArea(1, 6);
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

	public void setRectification( CameraPinholeBrown leftParam, DMatrixRMaj leftRect,
								  CameraPinholeBrown rightParam, DMatrixRMaj rightRect ) {
		leftView.setCalibration(leftParam, leftRect);
		rightView.setCalibration(rightParam, rightRect);
		checkUndistorted.setEnabled(true);
		setSelected(selectedImage); // this will cause it to render the rectified image
	}

	public void addPair( String name, File imageLeft, File imageRight ) {
		listLeft.add(imageLeft);
		listRight.add(imageRight);
		names.add(name);

		imageList.removeListSelectionListener(this);
		imageList.setListData(names.toArray(new String[0]));
		imageList.addListSelectionListener(this);
		if (names.size() == 1) {
//			leftView.setPreferredSize(new Dimension(imageLeft.getWidth(), imageLeft.getHeight()));
//			rightView.setPreferredSize(new Dimension(imageRight.getWidth(), imageRight.getHeight()));
			imageList.setSelectedIndex(0);
		}
		validate();
	}

	public synchronized void setObservations( List<CalibrationObservation> leftObservations, List<ImageResults> leftResults,
											  List<CalibrationObservation> rightObservations, List<ImageResults> rightResults ) {
//		if( leftObservations == null || leftResults == null || rightObservations == null || rightResults == null )
//			return;

		this.leftObservations = leftObservations;
		this.rightObservations = rightObservations;
		this.leftResults = leftResults;
		this.rightResults = rightResults;

		// synchronize configurations
		leftView.setDisplay(showPoints, showErrors, showUndistorted, showAll, showNumbers, showOrder, errorScale);
		rightView.setDisplay(showPoints, showErrors, showUndistorted, showAll, showNumbers, showOrder, errorScale);

		setSelected(selectedImage);

		updateResultsGUI();
	}

	protected void setSelected( int selected ) {
		selectedImage = selected;

		BufferedImage imageLeft = UtilImageIO.loadImage(listLeft.get(selected).getPath());
		BufferedImage imageRight = UtilImageIO.loadImage(listRight.get(selected).getPath());
		if (imageLeft == null || imageRight == null)
			throw new RuntimeException("Couldn't find left and/or right image");

		leftView.setImage(imageLeft);
		rightView.setImage(imageRight);

		if (leftObservations == null)
			return;
		leftView.setResults(leftObservations.get(selected), leftResults.get(selected), leftObservations);
		rightView.setResults(rightObservations.get(selected), rightResults.get(selected), rightObservations);

		updateResultsGUI();
	}

	private synchronized void updateResultsGUI() {
		if (leftResults != null && rightResults != null && selectedImage < leftResults.size()) {
			ImageResults r = leftResults.get(selectedImage);
			String textMean = String.format("%5.1e", r.meanError);
			String textMax = String.format("%5.1e", r.maxError);
			meanErrorLeft.setText(textMean);
			maxErrorLeft.setText(textMax);

			r = rightResults.get(selectedImage);
			textMean = String.format("%5.1e", r.meanError);
			textMax = String.format("%5.1e", r.maxError);
			meanErrorRight.setText(textMean);
			maxErrorRight.setText(textMax);
		}
	}

	@Override
	public void itemStateChanged( ItemEvent e ) {
		if (e.getSource() == checkPoints) {
			showPoints = checkPoints.isSelected();
		} else if (e.getSource() == checkErrors) {
			showErrors = checkErrors.isSelected();
		} else if (e.getSource() == checkAll) {
			showAll = checkAll.isSelected();
		} else if (e.getSource() == checkUndistorted) {
			showUndistorted = checkUndistorted.isSelected();
		} else if (e.getSource() == checkNumbers) {
			showNumbers = checkNumbers.isSelected();
		}
		leftView.setDisplay(showPoints, showErrors, showUndistorted, showAll, showNumbers, showOrder, errorScale);
		rightView.setDisplay(showPoints, showErrors, showUndistorted, showAll, showNumbers, showOrder, errorScale);
		leftView.repaint();
		rightView.repaint();
	}

	@Override
	public void valueChanged( ListSelectionEvent e ) {
		if (e.getValueIsAdjusting() || e.getFirstIndex() == -1)
			return;

		if (imageList.getSelectedIndex() >= 0) {
			setSelected(imageList.getSelectedIndex());
			leftView.repaint();
			rightView.repaint();
		}
	}

	@Override
	public void stateChanged( ChangeEvent e ) {
		if (e.getSource() == selectErrorScale) {
			errorScale = ((Number)selectErrorScale.getValue()).intValue();
		}

		leftView.setDisplay(showPoints, showErrors, showUndistorted, showAll, showNumbers, showOrder, errorScale);
		rightView.setDisplay(showPoints, showErrors, showUndistorted, showAll, showNumbers, showOrder, errorScale);
		leftView.repaint();
		rightView.repaint();
	}

	@Override
	public void mouseClicked( MouseEvent e ) {
		leftView.setLine(e.getY());
		rightView.setLine(e.getY());
		leftView.repaint();
		rightView.repaint();
	}

	@Override
	public void mousePressed( MouseEvent e ) {}

	@Override
	public void mouseReleased( MouseEvent e ) {}

	@Override
	public void mouseEntered( MouseEvent e ) {}

	@Override
	public void mouseExited( MouseEvent e ) {}

	private class SideBar extends StandardAlgConfigPanel {
		public SideBar() {
			JScrollPane scroll = new JScrollPane(imageList);

			addCenterLabel("Left");
			addLabeled(meanErrorLeft, "Mean Error");
			addLabeled(maxErrorLeft, "Max Error");
			addSeparator(200);
			addCenterLabel("Right");
			addLabeled(meanErrorRight, "Mean Error");
			addLabeled(maxErrorRight, "Max Error");
			addSeparator(200);
			add(scroll);
		}
	}
}
