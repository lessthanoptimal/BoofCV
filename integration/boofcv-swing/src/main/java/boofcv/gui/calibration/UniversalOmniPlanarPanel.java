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

package boofcv.gui.calibration;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.ViewedImageInfoPanel;
import boofcv.struct.calib.CameraUniversalOmni;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * GUI interface for CalibrateMonoPlanarGuiApp. Displays results for each calibration
 * image in a window.
 *
 * @author Peter Abeles
 */
public class UniversalOmniPlanarPanel extends CalibratedPlanarPanel<CameraUniversalOmni> {
	JTextArea paramCenterX;
	JTextArea paramCenterY;
	JTextArea paramFX;
	JTextArea paramFY;
	JTextArea paramSkew;
	JTextArea paramRadial;
	JTextArea paramTangental;
	JTextArea paramOffset;

	public UniversalOmniPlanarPanel() {

		viewInfo.setListener(new ViewedImageInfoPanel.Listener() {
			@Override
			public void zoomChanged( double zoom ) {
				mainView.setScale(zoom);
			}
		});

		mainView = new DisplayFisheyeCalibrationPanel();
		mainView.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked( MouseEvent e ) {
				double scale = viewInfo.getZoom();
				viewInfo.setCursor(e.getX()/scale, e.getY()/scale);
			}
		});

		imageList = new JList();
		imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		imageList.addListSelectionListener(this);

		paramCenterX = createErrorComponent(1);
		paramCenterY = createErrorComponent(1);
		paramFX = createErrorComponent(1);
		paramFY = createErrorComponent(1);
		paramSkew = createErrorComponent(1);
		paramRadial = createErrorComponent(2);
		paramTangental = createErrorComponent(2);
		paramOffset = createErrorComponent(1);

		mainView.setDisplay(showPoints, showErrors, showUndistorted, showAll, showNumbers, showOrder, errorScale);
		mainView.addMouseWheelListener(viewInfo);

		add(new LeftPanel(), BorderLayout.WEST);
		add(mainView, BorderLayout.CENTER);
		add(new RightPanel(), BorderLayout.EAST);
	}

	@Override
	public void setObservations( List<CalibrationObservation> features ) {
		this.features = features;
	}

	@Override
	public void setResults( List<ImageResults> results ) {
		this.results = results;
		setSelected(selectedImage);
	}

	@Override
	public void setCalibration( CameraUniversalOmni intrinsic, SceneStructureMetric scene ) {
		String textX = String.format("%5.1f", intrinsic.cx);
		String textY = String.format("%5.1f", intrinsic.cy);
		paramCenterX.setText(textX);
		paramCenterY.setText(textY);

		String textA = String.format("%5.1f", intrinsic.fx);
		String textB = String.format("%5.1f", intrinsic.fy);
		paramFX.setText(textA);
		paramFY.setText(textB);
		if (intrinsic.skew == 0) {
			paramSkew.setText("");
		} else {
			String textC = String.format("%5.1e", intrinsic.skew);
			paramSkew.setText(textC);
		}
		String textD = String.format("%5.1e", intrinsic.mirrorOffset);
		paramOffset.setText(textD);

		String radial = "";
		if (intrinsic.radial != null) {
			for (int i = 0; i < intrinsic.radial.length; i++) {
				radial += String.format("%5.2e", intrinsic.radial[i]);
				if (i != intrinsic.radial.length - 1) {
					radial += "\n";
				}
			}
		}
		paramRadial.setText(radial);

		if (intrinsic.t1 != 0 && intrinsic.t2 != 0)
			paramTangental.setText(String.format("%5.2e\n%5.2e", intrinsic.t1, intrinsic.t2));
		else
			paramTangental.setText("");
	}

	@Override
	public void setCorrection( CameraUniversalOmni param ) {
		checkUndistorted.setEnabled(true);
		LensDistortionWideFOV model = LensDistortionFactory.wide(param);
		((DisplayFisheyeCalibrationPanel)mainView).setCalibration(model, param.width, param.height);
	}

	@Override
	public void valueChanged( ListSelectionEvent e ) {
		if (e.getValueIsAdjusting() || e.getFirstIndex() == -1)
			return;

		if (imageList.getSelectedIndex() >= 0) {
			setSelected(imageList.getSelectedIndex());
			mainView.repaint();
		}
	}

	@Override
	protected void updateResultsGUI() {
		if (selectedImage < results.size()) {
			ImageResults r = results.get(selectedImage);
			String textMean = String.format("%5.1e", r.meanError);
			String textMax = String.format("%5.1e", r.maxError);
			meanError.setText(textMean);
			maxError.setText(textMax);
		}
	}

	private class LeftPanel extends StandardAlgConfigPanel {
		public LeftPanel() {
			JScrollPane scroll = new JScrollPane(imageList);

			addLabeled(meanError, "Mean Error");
			addLabeled(maxError, "Max Error");
			addSeparator(200);
			addLabeled(paramCenterX, "Xc");
			addLabeled(paramCenterY, "Yc");
			addLabeled(paramFX, "fx");
			addLabeled(paramFY, "fy");
			addLabeled(paramSkew, "skew");
			addLabeled(paramRadial, "radial");
			addLabeled(paramTangental, "tangential");
			addLabeled(paramOffset, "offset");
			addCenterLabel("Images", this);
			add(scroll);
		}
	}
}
