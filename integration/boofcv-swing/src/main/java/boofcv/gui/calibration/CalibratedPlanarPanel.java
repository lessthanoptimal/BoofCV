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
import boofcv.gui.ViewedImageInfoPanel;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraModel;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author Peter Abeles
 */
public abstract class CalibratedPlanarPanel<CM extends CameraModel> extends JPanel
	implements ListSelectionListener
{

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
	}

	public void setObservations(List<CalibrationObservation> features  ) {
		this.features = features;
	}

	public void setResults(List<ImageResults> results) {
		this.results = results;
		setSelected(selectedImage);
	}

	public void addImage( File filePath )
	{
		imagePaths.add(filePath.getPath());
		imageNames.add( filePath.getName() );

		imageList.removeListSelectionListener(this);
		imageList.setListData(imageNames);
		if( imageNames.size() == 1 ) {
			imageList.addListSelectionListener(this);
			imageList.setSelectedIndex(0);
			validate();
		} else {
			// each time an image is added it resets the selected value
			imageList.setSelectedIndex(selectedImage);
			imageList.addListSelectionListener(this);
		}
	}

	protected void setSelected( int selected ) {
		if( selected < features.size() )
			mainView.setResults(features.get(selected),results.get(selected), features);
		BufferedImage image = UtilImageIO.loadImage(imagePaths.get(selected));
		if( image == null )
			throw new RuntimeException("Couldn't load image!");

		mainView.setBufferedImage(image);
		selectedImage = selected;

		viewInfo.setImageSize(image.getWidth(),image.getHeight());

		if( results != null ) {
			updateResultsGUI();
		}
		mainView.repaint();
	}

	protected abstract void updateResultsGUI();

	public abstract void setCalibration(Zhang99AllParam found);

	public abstract void setCorrection( CM param );

}
