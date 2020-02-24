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
import boofcv.abst.filter.binary.BinaryContourInterface;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetectorPixel;
import boofcv.demonstrations.shapes.DetectBlackShapeAppBase;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.factory.fiducial.ConfigUchiyaMarker;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.fiducial.FiducialIO;
import boofcv.io.fiducial.UchiyaDefinition;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
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
	// TODO Add controls for settings
	// TODO list all markers found

	CameraPinholeBrown intrinsic;
	ConfigUchiyaMarker config = new ConfigUchiyaMarker();
	File fileDefinitions = new File("."); // used to see if the definition was already loaded

	//------------ BEGIN LOCK
	final Object lockTracker = new Object();
	boolean addingMarkers=false;
	Uchiya_to_FiducialDetector<T> tracker;
	UchiyaDefinition definition;
	FastQueue<DetectInfo> detections = new FastQueue<>(DetectInfo::new);
	//------------ END LOCK

	VisualizePanel gui = new VisualizePanel();
	ControlPanel controlPanel = new ControlPanel();

	public DetectUchiyaMarkerApp(List<String> examples , Class<T> imageType) {
		super(examples, imageType);

		setupGui(gui,controlPanel);
	}

	@Override
	protected void createDetector(boolean initializing) {
		if( addingMarkers ) {
			JOptionPane.showMessageDialog(this,"BUG: Still adding markers");
			return;
		}
		if( definition == null ) {
			return;
		}
		config.markerLength = definition.markerWidth;
		var tracker = FactoryFiducial.uchiya(config, imageClass);
		tracker.setPrintTiming(System.out);
		if( intrinsic != null ) {
			LensDistortionNarrowFOV lens = LensDistortionFactory.narrow(intrinsic);
			tracker.setLensDistortion(lens,intrinsic.width, intrinsic.height);
		}
		addingMarkers = true;
		new Thread(()->addAllMarkers(tracker)).start();
	}

	// TODO turn off controls
	// TODO avoid reloading if a regular control was changed
	protected void addAllMarkers( Uchiya_to_FiducialDetector<T> tracker ) {
		SwingUtilities.invokeLater(()->setMenuBarEnabled(false));
		for (int i = 0; i < definition.markers.size(); i++) {
			tracker.addMarker(definition.markers.get(i));
		}
		SwingUtilities.invokeLater(()->setMenuBarEnabled(true));
		synchronized (lockTracker) {
			this.tracker = tracker;
			addingMarkers = false;
		}
		reprocessImageOnly();
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
			synchronized (lockTracker) {
				definition = FiducialIO.loadUchiyaYaml(file);
			}
			fileDefinitions = file;
			createDetector(false);
		} catch (RuntimeException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Failed to read Uchiya YAML definition");
		}
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		super.handleInputChange(source, method, width, height);

		if( method != InputMethod.IMAGE )
			return;

		URL url = UtilIO.ensureURL(inputFilePath);
		if( url != null && url.getProtocol().equals("file") ) {
			try {
				String path = URLDecoder.decode(url.getPath(), "UTF-8");
				File directory = new File(path).getParentFile();

				File fileIntrinsic = new File(directory,"intrinsic.yaml");
				if( fileIntrinsic.exists() ) {
					intrinsic = CalibrationIO.load(fileIntrinsic);
				} else {
					intrinsic = null;
				}

				File fileDef = new File(directory, "uchiya.yaml");
				if (fileDef.exists() && !fileDefinitions.getAbsolutePath().equals(fileDef.getAbsolutePath())) {
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

		// don't process the image if it's still loading the definitions file
		synchronized (lockTracker) {
			if( addingMarkers ) {
				detections.reset();
				SwingUtilities.invokeLater(()->{
					guiImage.setImage(original);
					guiImage.repaint();
				});
				return;
			}
		}

		// After the tracker has been declared this is the only function which manipulates it
		final Uchiya_to_FiducialDetector<T> tracker = this.tracker;
		if( tracker == null ) return;

		long before = System.nanoTime();
		synchronized (tracker) { // ok some GUI operations do read directly from this specific instance of the tracker
			tracker.detect((T) input);
		}
		long after = System.nanoTime();
		double timeInSeconds = (after-before)*1e-9;

		synchronized (lockTracker) {
			final int N = tracker.totalFound();
			detections.reset();
			for (int i = 0; i < N; i++) {
				DetectInfo d = detections.grow();
				d.reset();
				d.id = (int)tracker.getId(i);
				tracker.getBounds(i,d.bounds);
				tracker.getCenter(i,d.center);

				if( tracker.is3D() ) {
					tracker.getFiducialToCamera(i,d.markerToCamera);
				}

				var track = tracker.getTracks().get(i);
				d.dots.copyAll((List)track.observed.toList(),(src,dst)-> dst.set(src));
			}
		}

		controlPanel.thresholdPanel.updateHistogram((T)input);

		SwingUtilities.invokeLater(()-> {
			controls.setProcessingTimeS(timeInSeconds);
			viewUpdated();
			guiImage.repaint();
		});
	}

	@Override
	public void viewUpdated() {
		final Uchiya_to_FiducialDetector<T> tracker = this.tracker;
		if( tracker == null ) {
			System.err.println("Tried to process an image with no tracker");
			return;
		}

		synchronized (tracker) {
			// a little bit of a hack here
			super.binary = tracker.getTracker().getBinary();
			super.viewUpdated();
			super.binary = null; // sanity check to make sure this isn't used
		}
	}

	@Override
	public void imageThresholdUpdated() {
		BoofSwingUtil.checkGuiThread();
		config.threshold = controlPanel.thresholdPanel.createConfig();
		createDetector(false);
		reprocessImageOnly();
	}

	class ControlPanel extends DetectBlackShapePanel implements LlahControlPanel.Listener {
		boolean showBounds = true;
		boolean showID = true;
		boolean showCenter = false;
		boolean showEllipses = false;
		boolean showDots = false;
		boolean showContour = false;
		boolean show3D = true;

		final JComboBox<String> comboView = combo(selectedView,"Input","Binary","Black");
		final JCheckBox checkBounds = checkbox("Show Bounds",showBounds);
		final JCheckBox checkID = checkbox("Show ID",showID);
		final JCheckBox checkCenter = checkbox("Show Center",showCenter);
		final JCheckBox checkEllipses = checkbox("Show Ellipses",showEllipses);
		final JCheckBox checkDots = checkbox("Show Dots",showDots);
		final JCheckBox checkContour = checkbox("Show Contour",showContour);
		final JCheckBox check3D = checkbox("Show 3D",show3D);

		final ThresholdControlPanel thresholdPanel = new ThresholdControlPanel(DetectUchiyaMarkerApp.this,config.threshold);
		final LlahControlPanel llahPanel = new LlahControlPanel(this,config.llah);

		public ControlPanel() {
			thresholdPanel.addHistogramGraph();
			selectZoom = spinner(1.0,MIN_ZOOM,MAX_ZOOM,1);

			addLabeled(processingTimeLabel,"Time");
			addLabeled(imageSizeLabel,"Size");
			addLabeled(comboView, "View");
			addLabeled(selectZoom,"Zoom");
			addAlignLeft(checkBounds);
			addAlignLeft(checkID);
			addAlignLeft(checkCenter);
			addAlignLeft(checkEllipses);
			addAlignLeft(checkDots);
			addAlignLeft(checkContour);
			addAlignLeft(check3D);
			add(llahPanel);
			add(thresholdPanel);
		}

		@Override
		public void controlChanged(Object source) {
			if( source == comboView ) {
				selectedView = comboView.getSelectedIndex();
				viewUpdated(); return;
			} else if( source == checkBounds ) {
				showBounds = checkBounds.isSelected();
			} else if( source == checkID ) {
				showID = checkID.isSelected();
			} else if( source == checkCenter ) {
				showCenter = checkCenter.isSelected();
			} else if( source == checkEllipses ) {
				showEllipses = checkEllipses.isSelected();
			} else if( source == checkDots ) {
				showDots = checkDots.isSelected();
			} else if( source == checkContour ) {
				showContour = checkContour.isSelected();
			} else if( source == check3D ) {
				show3D = check3D.isSelected();
			} else if( source == selectZoom ) {
				zoom = ((Number) selectZoom.getValue()).doubleValue();
				guiImage.setScale(zoom);
			}
			gui.repaint();
		}

		@Override
		public void configLlahChanged() {
			createDetector(false);
			reprocessImageOnly();
		}
	}

	class VisualizePanel extends ShapeVisualizePanel {

		final Font largeFont = new Font("Serif", Font.BOLD, 42);
		final Font idFont = new Font("Serif", Font.BOLD, 24);
		final Color bgColor = new Color(0,0,0,130);

		@Override
		protected void paintOverPanel(Graphics2D g2) {
			if( addingMarkers ) {
				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				float x = (float)((getWidth())/8.0-transX);
				float y = (float)((getHeight())/2.0-transY);
				g2.setFont(largeFont);
				g2.setColor(Color.ORANGE);
				g2.drawString("Loading Definitions",x,y);
			}
		}

		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final Uchiya_to_FiducialDetector<T> tracker = DetectUchiyaMarkerApp.this.tracker;
			if( tracker == null ) return;

			synchronized (lockTracker) {
				if (controlPanel.showContour) {
					synchronized (tracker) {
						BinaryContourInterface contour = tracker.getTracker().getEllipseDetector().getContourFinder();
						List<Contour> contours = BinaryImageOps.convertContours(contour);
						g2.setStroke(new BasicStroke(1));
						VisualizeBinaryData.render(contours, null, Color.CYAN, 1.0, scale, g2);
					}
				}

				if( controlPanel.showEllipses ) {
					synchronized (tracker) {
						g2.setStroke(new BasicStroke(3.0f));
						g2.setColor(Color.ORANGE);
						List<BinaryEllipseDetectorPixel.Found> ellipses =
								tracker.getTracker().getEllipseDetector().getFound();
						for (int i = 0; i < ellipses.size(); i++) {
							var e = ellipses.get(i);
							if( e.ellipse.a > 0 )
								VisualizeShapes.drawEllipse(e.ellipse,scale,g2);
						}
					}
				}

				if( controlPanel.showDots ) {
					g2.setStroke(new BasicStroke(3.0f));
					g2.setColor(Color.BLUE);
					for (int i = 0; i < detections.size; i++) {
						List<Point2D_F64> dots = detections.get(i).dots.toList();
						for( var p : dots ) {
							VisualizeFeatures.drawPoint(g2,p.x*scale,p.y*scale,5,Color.PINK,true);
						}
					}
				}

				if (controlPanel.showCenter) {
					for (int i = 0; i < detections.size; i++) {
						Point2D_F64 p = detections.get(i).center;
						VisualizeFeatures.drawPoint(g2,p.x*scale,p.y*scale,12,Color.gray,true);
					}
				}

				if (controlPanel.showBounds) {
					g2.setStroke(new BasicStroke(6.0f));
					for (int i = 0; i < detections.size; i++) {
						VisualizeShapes.drawPolygon(detections.get(i).bounds,true,scale,Color.RED,Color.BLUE,g2);
					}
				}

				if (controlPanel.showID) {
					for (int i = 0; i < detections.size; i++) {
						int id = detections.get(i).id;
						Point2D_F64 center = detections.get(i).center;
						VisualizeFiducial.drawLabel(center,""+id, idFont,Color.ORANGE,bgColor,g2, scale);
					}
				}

				if( controlPanel.show3D && tracker.is3D() && intrinsic != null ) {
					for (int i = 0; i < detections.size; i++) {
						VisualizeFiducial.drawCube(detections.get(i).markerToCamera,
								intrinsic,definition.markerWidth,4,g2);
					}
				}
			}
		}
	}

	// Copy of tracker data so that we don't have to lock the tracker when rendering this data, at least.
	public static class DetectInfo {
		int id;
		final Polygon2D_F64 bounds = new Polygon2D_F64();
		final Point2D_F64 center = new Point2D_F64();
		final FastQueue<Point2D_F64> dots = new FastQueue<>(Point2D_F64::new);
		final Se3_F64 markerToCamera = new Se3_F64();

		public void reset() {
			id = -1;
			dots.reset();
			markerToCamera.reset();
		}
	}

	public static void main(String[] args) {
		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("fiducial/uchiya/image00.jpg"));
		examples.add(UtilIO.pathExample("fiducial/uchiya/image01.jpg"));
		examples.add(UtilIO.pathExample("fiducial/uchiya/image02.jpg"));
		examples.add(UtilIO.pathExample("fiducial/uchiya/image03.jpg"));
		examples.add(UtilIO.pathExample("fiducial/uchiya/image04.jpg"));
		examples.add(UtilIO.pathExample("fiducial/uchiya/image05.jpg"));
		examples.add(UtilIO.pathExample("fiducial/uchiya/movie.mp4"));

		SwingUtilities.invokeLater(()->{
			var app = new DetectUchiyaMarkerApp<>(examples, GrayU8.class);
			app.openExample(examples.get(0));
			app.display("Uchiya Marker Tracker");
		});
	}
}
