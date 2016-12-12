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

package boofcv.gui.calibration;

import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.Zhang99ParamAll;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.calib.CameraPinholeRadial;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * GUI interface for CalibrateMonoPlanarGuiApp.  Displays results for each calibration
 * image in a window.
 * 
 * @author Peter Abeles
 */
public class MonoPlanarPanel extends JPanel implements ItemListener ,
		ListSelectionListener, ChangeListener 
{

	CalibratedImageGridPanel mainView = new CalibratedImageGridPanel();
	
	JCheckBox checkPoints;
	JCheckBox checkErrors;
	JCheckBox checkUndistorted;
	JCheckBox checkAll;
	JCheckBox checkNumbers;
	JCheckBox checkOrder;
	JSpinner selectErrorScale;
	
	JList imageList;

	JTextArea meanError;
	JTextArea maxError;

	JTextArea paramCenterX;
	JTextArea paramCenterY;
	JTextArea paramA;
	JTextArea paramB;
	JTextArea paramC;

	boolean showPoints = false;
	boolean showErrors = true;
	boolean showUndistorted = false;
	boolean showAll = false;
	boolean showNumbers = false;
	boolean showOrder = true;

	int selectedImage = 0;

	List<String> names = new ArrayList<>();
	List<BufferedImage> images = new ArrayList<>();
	List<CalibrationObservation> features = new ArrayList<>();
	List<ImageResults> results = new ArrayList<>();

	int errorScale = 20;

	public MonoPlanarPanel() {
		super(new BorderLayout());

		JToolBar toolBar = createToolBar();

		imageList = new JList();
		imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		imageList.addListSelectionListener(this);

		meanError = createErrorComponent();
		maxError = createErrorComponent();
		paramCenterX = createErrorComponent();
		paramCenterY = createErrorComponent();
		paramA = createErrorComponent();
		paramB = createErrorComponent();
		paramC = createErrorComponent();

		mainView.setImages(images);
		mainView.setResults(features,results);
		mainView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,showOrder,errorScale);

		add(toolBar, BorderLayout.PAGE_START);
		add(mainView, BorderLayout.CENTER);
		add(new SideBar(), BorderLayout.WEST);
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

		checkUndistorted = new JCheckBox("Undistort");
		checkUndistorted.setSelected(showUndistorted);
		checkUndistorted.addItemListener(this);
		checkUndistorted.setEnabled(false);

		checkNumbers = new JCheckBox("Numbers");
		checkNumbers.setSelected(showNumbers);
		checkNumbers.addItemListener(this);

		checkOrder = new JCheckBox("Order");
		checkOrder.setSelected(showOrder);
		checkOrder.addItemListener(this);

		selectErrorScale = new JSpinner(new SpinnerNumberModel(errorScale, 1, 100, 5));
		selectErrorScale.addChangeListener(this);
		selectErrorScale.setMaximumSize(selectErrorScale.getPreferredSize());

		toolBar.add(checkPoints);
		toolBar.add(checkErrors);
		toolBar.add(checkAll);
		toolBar.add(checkUndistorted);
		toolBar.add(checkNumbers);
		toolBar.add(checkOrder);
		toolBar.add(new JLabel("| Error Scale"));
		toolBar.add(selectErrorScale);
		return toolBar;
	}

	private JTextArea createErrorComponent() {
		JTextArea comp = new JTextArea(1,6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}
	
	public void addImage( String name , BufferedImage image )
	{
		names.add(name);
		images.add(image);

		imageList.removeListSelectionListener(this);
		imageList.setListData(new Vector<Object>(names));
		if( names.size() == 1 ) {
			imageList.addListSelectionListener(this);
			mainView.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));
			imageList.setSelectedIndex(0);
			validate();
		} else {
			// each time an image is added it resets the selected value
			imageList.setSelectedIndex(selectedImage);
			imageList.addListSelectionListener(this);
		}
	}

	public void setObservations( List<CalibrationObservation> features  ) {
		this.features = features;
	}

	public void setResults(List<ImageResults> results) {
		mainView.setResults(features,results);
		this.results = results;
		updateResultsGUI();
	}

	public void setCalibration(Zhang99ParamAll found) {
		String textX = String.format("%5.1f",found.x0);
		String textY = String.format("%5.1f", found.y0);
		paramCenterX.setText(textX);
		paramCenterY.setText(textY);

		String textA = String.format("%5.1f",found.a);
		String textB = String.format("%5.1f",found.b);
		paramA.setText(textA);
		paramB.setText(textB);
		String textC = String.format("%5.1e",found.c);
		paramC.setText(textC);
	}

	public void setCorrection( CameraPinholeRadial param )
	{
		checkUndistorted.setEnabled(true);
		mainView.setDistorted(param,null);
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
		} else if( e.getSource() == checkOrder ) {
			showOrder = checkOrder.isSelected();
		}
		mainView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,showOrder,errorScale);
		mainView.repaint();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if( e.getValueIsAdjusting() || e.getFirstIndex() == -1)
			return;

		if( imageList.getSelectedIndex() >= 0 ) {
			setSelected(imageList.getSelectedIndex());
			mainView.repaint();
		}
	}

	private void setSelected( int selected ) {
		mainView.setSelected(selected);
		selectedImage = selected;

		if( results != null ) {
			updateResultsGUI();
		}
	}

	private void updateResultsGUI() {
		if( selectedImage < results.size() ) {
			ImageResults r = results.get(selectedImage);
			String textMean = String.format("%5.1e", r.meanError);
			String textMax = String.format("%5.1e",r.maxError);
			meanError.setText(textMean);
			maxError.setText(textMax);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectErrorScale) {
			errorScale = ((Number) selectErrorScale.getValue()).intValue();
		}

		mainView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,showOrder,errorScale);
		mainView.repaint();
	}

	private class SideBar extends StandardAlgConfigPanel
	{
		public SideBar() {
			JScrollPane scroll = new JScrollPane(imageList);

			addLabeled(meanError,"Mean Error",this);
			addLabeled(maxError, "Max Error", this);
			addSeparator(200);
			addLabeled(paramCenterX,"Xc",this);
			addLabeled(paramCenterY,"Yc",this);
			addLabeled(paramA,"fx",this);
			addLabeled(paramB,"fy",this);
			addLabeled(paramC,"skew",this);
			addSeparator(200);
			add(scroll);
		}
	}
}
