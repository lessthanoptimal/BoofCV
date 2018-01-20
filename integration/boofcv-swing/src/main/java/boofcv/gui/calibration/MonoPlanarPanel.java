/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.calibration.Zhang99AllParam;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.ViewedImageInfoPanel;
import boofcv.struct.calib.CameraPinholeRadial;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * GUI interface for CalibrateMonoPlanarGuiApp.  Displays results for each calibration
 * image in a window.
 * 
 * @author Peter Abeles
 */
public class MonoPlanarPanel extends CalibratedPlanarPanel<CameraPinholeRadial>
		implements ItemListener , ChangeListener
{
	JTextArea paramCenterX;
	JTextArea paramCenterY;
	JTextArea paramFX;
	JTextArea paramFY;
	JTextArea paramSkew;
	JTextArea paramRadial;
	JTextArea paramTangental;

	public MonoPlanarPanel() {
		viewInfo.setListener(new ViewedImageInfoPanel.Listener() {
			@Override
			public void zoomChanged(double zoom) {
				mainView.setScale(zoom);
			}
		});

		mainView = new DisplayPinholeCalibrationPanel();
		mainView.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				double scale = viewInfo.getZoom();
				viewInfo.setCursor(e.getX()/scale,e.getY()/scale);
			}
		});

		imageList = new JList();
		imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		imageList.addListSelectionListener(this);

		meanError = createErrorComponent(1);
		maxError = createErrorComponent(1);
		paramCenterX = createErrorComponent(1);
		paramCenterY = createErrorComponent(1);
		paramFX = createErrorComponent(1);
		paramFY = createErrorComponent(1);
		paramSkew = createErrorComponent(1);
		paramRadial = createErrorComponent(2);
		paramTangental = createErrorComponent(2);

		mainView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,showOrder,errorScale);
		mainView.addMouseWheelListener(viewInfo);

		add(mainView, BorderLayout.CENTER);
		add( new LeftPanel() , BorderLayout.WEST);
		add( new RightPanel() , BorderLayout.EAST );
	}

	public void setObservations( List<CalibrationObservation> features  ) {
		this.features = features;
	}

	public void setResults(List<ImageResults> results) {
		this.results = results;
		setSelected(selectedImage);
	}

	@Override
	public void setCalibration(Zhang99AllParam found) {
		CameraPinholeRadial intrinsic = (CameraPinholeRadial)found.getIntrinsic().getCameraModel();
		String textX = String.format("%5.1f",intrinsic.cx);
		String textY = String.format("%5.1f", intrinsic.cy);
		paramCenterX.setText(textX);
		paramCenterY.setText(textY);

		String textA = String.format("%5.1f",intrinsic.fx);
		String textB = String.format("%5.1f",intrinsic.fy);
		paramFX.setText(textA);
		paramFY.setText(textB);
		if( intrinsic.skew == 0 ) {
			paramSkew.setText("");
		} else {
			String textC = String.format("%5.1e", intrinsic.skew);
			paramSkew.setText(textC);
		}

		String radial = "";
		if( intrinsic.radial != null ) {
			for (int i = 0; i < intrinsic.radial.length; i++) {
				radial += String.format("%5.2e",intrinsic.radial[i]);
				if( i != intrinsic.radial.length-1) {
					radial += "\n";
				}
			}
		}
		paramRadial.setText(radial);

		if( intrinsic.t1 != 0 && intrinsic.t2 != 0 )
			paramTangental.setText(String.format("%5.2e\n%5.2e",intrinsic.t1,intrinsic.t2));
		else
			paramTangental.setText("");
	}

	@Override
	public void setCorrection( CameraPinholeRadial param )
	{
		checkUndistorted.setEnabled(true);
		mainView.setCalibration(param);
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

//	private void setSelected( int selected ) {
//		mainView.setSelected(selected);
//		selectedImage = selected;
//
//		if( results != null ) {
//			updateResultsGUI();
//		}
//	}

	@Override
	protected void updateResultsGUI() {
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

	private class LeftPanel extends StandardAlgConfigPanel
	{
		public LeftPanel() {
			JScrollPane scroll = new JScrollPane(imageList);

			addLabeled(meanError,"Mean Error",this);
			addLabeled(maxError, "Max Error", this);
			addSeparator(200);
			addLabeled(paramCenterX,"Xc",this);
			addLabeled(paramCenterY,"Yc",this);
			addLabeled(paramFX,"fx",this);
			addLabeled(paramFY,"fy",this);
			addLabeled(paramSkew,"skew",this);
			addLabeled(paramRadial,"radial",this);
			addLabeled(paramTangental,"tangential",this);
			addSeparator(200);
			add(scroll);
		}
	}
}
