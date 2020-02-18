/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.fiducial;

import boofcv.abst.fiducial.Uchiya_to_FiducialDetector;
import boofcv.demonstrations.shapes.DetectBlackShapeAppBase;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.factory.fiducial.ConfigUchiyaMarker;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.UtilIO;
import boofcv.io.fiducial.FiducialIO;
import boofcv.io.fiducial.UchiyaDefinition;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Demonstration application for detecting {@link boofcv.alg.fiducial.dots.UchiyaMarkerTracker}.
 *
 * @author Peter Abeles
 */
public class DetectUchiyaMarkerApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase<T>
{

	ConfigUchiyaMarker config = new ConfigUchiyaMarker();

	UchiyaDefinition definition;
	//------------ BEGIN LOCK
	final Object trackerLock = new Object();
	Uchiya_to_FiducialDetector<T> tracker;
	//------------ END LOCK

	VisualizePanel gui = new VisualizePanel();
	ControlPanel controlPanel = new ControlPanel();

	public DetectUchiyaMarkerApp(List<String> examples , Class<T> imageType) {
		super(examples, imageType);

		setupGui(gui,controlPanel);
	}

	@Override
	protected void createDetector(boolean initializing) {
		synchronized (trackerLock) {
			if( definition == null ) {
				return;
			}
			config.markerLength = definition.markerWidth;
			tracker = FactoryFiducial.uchiya(config, imageClass);
			for (int i = 0; i < definition.markers.size(); i++) {
				tracker.addMarker(definition.markers.get(i));
			}
		}
	}

	@Override
	protected void detectorProcess(T input, GrayU8 binary) {}

	@Override
	public void openFile(File file) {
		if(FilenameUtils.getExtension(file.getName()).toLowerCase().equals("yaml")) {
			loadDefinition(file);
		} else {
			super.openFile(file);
		}
	}

	private void loadDefinition(File file) {
		try {
			synchronized (trackerLock) {
				definition = FiducialIO.loadUchiyaYaml(file);
			}
			createDetector(false);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Failed to read Uchiya YAML definition");
		}
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		super.handleInputChange(source, method, width, height);

		URL url = UtilIO.ensureURL(inputFilePath);
		if( url != null && url.getProtocol().equals("file") ) {
			try {
				String path = URLDecoder.decode(url.getPath(), "UTF-8");
				File directory = new File(path).getParentFile();
				File fileDef = new File(directory, "uchiya_description.yaml");
				if (fileDef.exists()) {
					System.out.println("Found uchiya_description");
					loadDefinition(fileDef);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		}
	}

	@Override
	public void processImage(int sourceID, long frameID, final BufferedImage buffered, ImageBase input) {
		synchronized (bufferedImageLock) {
			original = ConvertBufferedImage.checkCopy(buffered, original);
			work = ConvertBufferedImage.checkDeclare(buffered, work);
		}

		Uchiya_to_FiducialDetector<T> tracker;
		synchronized (trackerLock) {
			tracker = this.tracker;
		}
		if( tracker == null ) {
			System.err.println("Tried to process an image with no tracker");
			return;
		}
		long before = System.nanoTime();
		tracker.detect((T)input);
		long after = System.nanoTime();
		double timeInSeconds = (after-before)*1e-9;

		System.out.println("Found "+tracker.totalFound());
		for (int i = 0; i < tracker.totalFound(); i++) {
			System.out.println("   ID = "+tracker.getId(i));
		}

		SwingUtilities.invokeLater(()-> {
			controls.setProcessingTimeS(timeInSeconds);
			guiImage.setImage(original);
			guiImage.repaint();
		});
	}

	@Override
	public void imageThresholdUpdated() {

	}

	class ControlPanel extends DetectBlackShapePanel implements ActionListener, ChangeListener {

		int view = 0;

		JComboBox<String> comboView = combo(view,"Input","Binary","Black");

		// show definitions file
		// total markers defined

		// show ellipses
		// show contours
		// show squares
		// show 3D boxes
		// show dot centers

		// ellipse controls
		// contour controls
		// binary controls


		public ControlPanel() {
			selectZoom = spinner(1.0,MIN_ZOOM,MAX_ZOOM,1);

			addLabeled(processingTimeLabel,"Time");
			addLabeled(imageSizeLabel,"Size");
			addLabeled(comboView, "View");
			addLabeled(selectZoom,"Zoom");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == comboView ) {
				view = comboView.getSelectedIndex();
				gui.repaint();
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == selectZoom ) {
				zoom = ((Number) selectZoom.getValue()).doubleValue();
				guiImage.setScale(zoom);
				gui.repaint();
			}
		}
	}

	class VisualizePanel extends ShapeVisualizePanel {

		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {

		}
	}

	public static void main(String[] args) {
		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("fiducial/uchiya/image01.jpg"));
		examples.add(UtilIO.pathExample("fiducial/uchiya/image02.jpg"));
		examples.add(UtilIO.pathExample("fiducial/uchiya/image03.jpg"));
		examples.add(UtilIO.pathExample("fiducial/uchiya/image04.jpg"));
		examples.add(UtilIO.pathExample("fiducial/qrcode/movie.mp4"));

		SwingUtilities.invokeLater(()->{
			var app = new DetectUchiyaMarkerApp<>(examples, GrayU8.class);
			app.openExample(examples.get(0));
			app.display("Uchiya Marker Tracker");
		});
	}
}
