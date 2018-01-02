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
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.ViewedImageInfoPanel;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author Peter Abeles
 */
public abstract class CalibratedPlanarPanel<CM extends CameraModel> extends JPanel
	implements ListSelectionListener, ItemListener, ChangeListener
{
	JCheckBox checkPoints;
	JCheckBox checkErrors;
	JCheckBox checkUndistorted;
	JCheckBox checkAll;
	JCheckBox checkNumbers;
	JCheckBox checkOrder;
	JSpinner selectErrorScale;

	JTextArea meanError;
	JTextArea maxError;

	boolean showPoints = false;
	boolean showErrors = true;
	boolean showUndistorted = false;
	boolean showAll = false;
	boolean showNumbers = false;
	boolean showOrder = true;
	int errorScale = 20;

	ViewedImageInfoPanel viewInfo = new ViewedImageInfoPanel();
	public DisplayCalibrationPanel<CM> mainView;

	JList imageList;

	protected List<String> imagePaths = new ArrayList<>();
	protected List<CalibrationObservation> features = new ArrayList<>();
	protected List<ImageResults> results = new ArrayList<>();
	protected int selectedImage;

	// names of images as shown in the UI
	Vector<String> imageNames = new Vector<>();

	public CalibratedPlanarPanel() {
		super(new BorderLayout());

		meanError = createErrorComponent(1);
		maxError = createErrorComponent(1);
	}

	public void setObservations(List<CalibrationObservation> features  ) {
		this.features = features;
	}

	public void setResults(List<ImageResults> results) {
		this.results = results;
		setSelected(selectedImage);
	}

	public void showImageProcessed( final BufferedImage image ) {
		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				mainView.setBufferedImage(image);
				double zoom = BoofSwingUtil.selectZoomToShowAll(mainView,image.getWidth(),image.getHeight());
				mainView.setScale(zoom);
				mainView.repaint();
			}
		});
	}

	public void addImage( File filePath )
	{
		imagePaths.add(filePath.getPath());
		imageNames.add( filePath.getName() );

		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				imageList.removeListSelectionListener(CalibratedPlanarPanel.this);
				imageList.setListData(imageNames);
				if( imageNames.size() == 1 ) {
					imageList.addListSelectionListener(CalibratedPlanarPanel.this);
					imageList.setSelectedIndex(0);
					validate();
				} else {
					// each time an image is added it resets the selected value
					imageList.setSelectedIndex(selectedImage);
					imageList.addListSelectionListener(CalibratedPlanarPanel.this);
				}
			}
		});

	}

	public void setImages( List<File> imageFiles ) {
		for( File f : imageFiles ) {
			addImage(f);
		}
	}

	public void setImagesFailed( List<File> imageFiles ) {
		for( File f : imageFiles ) {
//			addImage(f);
		}
	}

	protected void setSelected( int selected ) {
		BoofSwingUtil.checkGuiThread();

		long start = System.currentTimeMillis();;
		BufferedImage image = UtilImageIO.loadImage(imagePaths.get(selected));
		if( image == null )
			throw new RuntimeException("Couldn't load image!");
		long stop = System.currentTimeMillis();

		System.out.println("Time to load image "+(stop-start)+" (ms)");

		if( selected < features.size() )
			mainView.setResults(features.get(selected),results.get(selected), features);

		mainView.setBufferedImage(image);
		double zoom = BoofSwingUtil.selectZoomToShowAll(mainView,image.getWidth(),image.getHeight());
		mainView.setScale(zoom);
		mainView.repaint();

		selectedImage = selected;

		viewInfo.setImageSize(image.getWidth(),image.getHeight());

		if( results != null ) {
			updateResultsGUI();
		}

	}

	protected abstract void updateResultsGUI();

	public abstract void setCalibration(Zhang99AllParam found);

	public abstract void setCorrection( CM param );

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
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectErrorScale) {
			errorScale = ((Number) selectErrorScale.getValue()).intValue();
		}

		mainView.setDisplay(showPoints,showErrors,showUndistorted,showAll,showNumbers,showOrder,errorScale);
		mainView.repaint();
	}

	JTextArea createErrorComponent(int numRows) {
		JTextArea comp = new JTextArea(numRows,6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		comp.setBorder(BorderFactory.createEmptyBorder(0,0,4,0));
		return comp;
	}

	class RightPanel extends StandardAlgConfigPanel
	{
		public RightPanel() {
			checkPoints = new JCheckBox("Show Points");
			checkPoints.setSelected(showPoints);
			checkPoints.addItemListener(CalibratedPlanarPanel.this);

			checkErrors = new JCheckBox("Show Errors");
			checkErrors.setSelected(showErrors);
			checkErrors.addItemListener(CalibratedPlanarPanel.this);

			checkAll = new JCheckBox("All Points");
			checkAll.setSelected(showAll);
			checkAll.addItemListener(CalibratedPlanarPanel.this);

			checkUndistorted = new JCheckBox("Undistort");
			checkUndistorted.setSelected(showUndistorted);
			checkUndistorted.addItemListener(CalibratedPlanarPanel.this);
			checkUndistorted.setEnabled(false);

			checkNumbers = new JCheckBox("Numbers");
			checkNumbers.setSelected(showNumbers);
			checkNumbers.addItemListener(CalibratedPlanarPanel.this);

			checkOrder = new JCheckBox("Order");
			checkOrder.setSelected(showOrder);
			checkOrder.addItemListener(CalibratedPlanarPanel.this);

			selectErrorScale = new JSpinner(new SpinnerNumberModel(errorScale, 1, 100, 5));
			selectErrorScale.addChangeListener(CalibratedPlanarPanel.this);
			selectErrorScale.setMaximumSize(selectErrorScale.getPreferredSize());

			add(viewInfo);
			addAlignLeft(checkPoints, this);
			addAlignLeft(checkErrors, this);
			addAlignLeft(checkAll, this);
			addAlignLeft(checkUndistorted, this);
			addAlignLeft(checkNumbers, this);
			addAlignLeft(checkOrder, this);
			addLabeled(selectErrorScale,"Error Scale", this);
		}
	}

}
