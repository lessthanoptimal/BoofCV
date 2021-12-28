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

package boofcv.demonstrations.sfm.d3;

import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.DepthVisualOdometry;
import boofcv.abst.sfm.d3.PyramidDirectColorDepth_to_DepthVisualOdometry;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.SelectLimitTypes;
import boofcv.factory.sfm.ConfigVisOdomTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.d3.Polygon3DSequenceViewer;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.struct.calib.VisualDepthParameters;
import boofcv.struct.distort.DoNothing2Transform2_F32;
import boofcv.struct.image.*;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Visualizes data from {@link DepthVisualOdometry}.
 *
 * @author Peter Abeles
 */
// TODO custom visualization for direct method
// TODO   - Key Fraction - Diversity - Key Frames
// TODO   - Show image unwarping
// TODO   - Show depth of pixel that is in view
// TODO add algorithm specific tuning parameters?
// TODO Add feature point cloud to VO view?
// TODO Click on polygon to get the frame it was generated from?
// TODO Add log to file option for location and 3D cloud
@SuppressWarnings({"rawtypes", "unchecked", "NullAway.Init"})
public class VisualizeDepthVisualOdometryApp
		extends DemonstrationBase implements VisualOdometryPanel2.Listener, ActionListener {
	VisualOdometryPanel2 statusPanel;
	VisualOdometryAlgorithmPanel algorithmPanel;
	VisualOdometryFeatureTrackerPanel featurePanel;
	VisualOdometryDirectPanel directPanel;

	JPanel mainPanel = new JPanel();
	JSplitPane viewPanel;
	ImagePanel guiLeft;
	ImagePanel guiDepth;
	Polygon3DSequenceViewer guiCam3D;

	BufferedImage renderedDepth;

	DepthVisualOdometry alg;

	boolean noFault;

	boolean showTracks;
	boolean showInliers;

	int numFaults;
	int numTracks;
	int numInliers;
	double fractionInBounds;
	int whichAlg = -1;

	AlgType algType = AlgType.UNKNOWN;

	double fps;
	int frameNumber;

	ImageBase imageRGB;
	GrayU16 imageDepth;

	BufferedImage bufferedRGB;

	protected VisualDepthParameters config;
	JComboBox<String> selectAlgorithm;

	public VisualizeDepthVisualOdometryApp( List<PathLabel> examples ) {
		super(true, false, examples);


		selectAlgorithm = new JComboBox<>();
		selectAlgorithm.addItem("Single P3P : KLT");
		selectAlgorithm.addItem("Single P3P : ST-BRIEF");
		selectAlgorithm.addItem("Single P3P : ST-SURF-KLT");
		selectAlgorithm.addItem("Direct");

		selectAlgorithm.addActionListener(this);
		selectAlgorithm.setMaximumSize(selectAlgorithm.getPreferredSize());
		menuBar.add(selectAlgorithm);

		statusPanel = new VisualOdometryPanel2(VisualOdometryPanel2.Type.DEPTH);
		guiLeft = new ImagePanel();
		guiDepth = new ImagePanel();
		guiCam3D = new Polygon3DSequenceViewer();

		viewPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				guiLeft, guiDepth);

		algorithmPanel = new VisualOdometryAlgorithmPanel();
		featurePanel = new VisualOdometryFeatureTrackerPanel();
		directPanel = new VisualOdometryDirectPanel();

		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(viewPanel, BorderLayout.CENTER);
		mainPanel.add(algorithmPanel, BorderLayout.NORTH);

		add(statusPanel, BorderLayout.WEST);
		add(mainPanel, BorderLayout.CENTER);

//		guiLeft.addMouseListener(this);
		statusPanel.setListener(this);

		changeSelectedAlgortihm(0);
	}

	@Override
	public void openFile( File file ) {
		inputFilePath = file.getPath();

		try (Reader r = media.openFileNotNull(file.getPath())) {
			BufferedReader in = new BufferedReader(r);
			String path = file.getParent();

			String lineConfig = in.readLine();
			String line1 = in.readLine();
			String line2 = in.readLine();

			// adjust for relative paths
			if (lineConfig.charAt(0) != '/')
				lineConfig = path + "/" + lineConfig;
			if (line1.charAt(0) != '/')
				line1 = path + "/" + line1;
			if (line2.charAt(0) != '/')
				line2 = path + "/" + line2;

			config = CalibrationIO.load(media.openFileNotNull(lineConfig));
			openVideo(false, line1, line2);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void reprocessInput() {
		openFile(new File(Objects.requireNonNull(inputFilePath)));
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		if (source != 0)
			return;

		fps = -1;
		numFaults = 0;
		frameNumber = 0;
		alg.reset();
		alg.setCalibration(config.visualParam, new DoNothing2Transform2_F32());

		statusPanel.reset();

		handleRunningStatus(Status.RUNNING);

		guiCam3D.init();
		// set the step size dynamically based on the scene
		guiCam3D.setFocalLength(config.visualParam.fx);
		guiCam3D.setPreferredSize(new Dimension(config.visualParam.width, config.visualParam.height));

		viewPanel.setPreferredSize(new Dimension(width*2 + 20, height));
		viewPanel.setDividerLocation(width);
		viewPanel.setMaximumSize(viewPanel.getPreferredSize());
	}

	@Override protected void openVideo( boolean reopen, String... filePaths ) {
		if (filePaths.length==1) {
			openFile(new File(filePaths[0]));
			return;
		}
		super.openVideo(reopen, filePaths);
	}

	@Override
	protected void handleInputClose( int source ) {
		if (source != 0)
			return;
		handleRunningStatus(Status.FINISHED);
	}

	private void drawFeatures( AccessPointTracks3D tracker, BufferedImage image ) {

		numInliers = 0;

		Graphics2D g2 = image.createGraphics();

		List<Point2D_F64> points = tracker.getAllTracks(null);

		if (points.size() == 0)
			return;

		double[] ranges = new double[points.size()];

		Point3D_F64 world = new Point3D_F64();
		for (int i = 0; i < points.size(); i++) {
			tracker.getTrackWorld3D(i, world);
			ranges[i] = world.z;
		}
		Arrays.sort(ranges);
		double maxRange = ranges[(int)(ranges.length*0.8)];

		// Attempt to set the step size dynamically based on the scale of the scene
		if (points.size() > 0 && guiCam3D.getStepSize() == 0.0) {
			guiCam3D.setStepSize(0.02*ranges[(int)(ranges.length*0.5)]);
		}


		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 pixel = points.get(i);

			if (showTracks && tracker.isTrackNew(i)) {
				VisualizeFeatures.drawPoint(g2, (int)pixel.x, (int)pixel.y, 3, Color.GREEN);
				continue;
			}

			if (tracker.isTrackInlier(i)) {
				if (showInliers)
					VisualizeFeatures.drawPoint(g2, (int)pixel.x, (int)pixel.y, 7, Color.BLUE, false);
				numInliers++;
			}

			if (!showTracks)
				continue;

			tracker.getTrackWorld3D(i, world);
			double r = world.z/maxRange;
			if (r < 0) r = 0;
			else if (r > 1) r = 1;

			int color = (255 << 16) | ((int)(255*r) << 8);


			VisualizeFeatures.drawPoint(g2, (int)pixel.x, (int)pixel.y, 3, new Color(color));
		}

		numTracks = points.size();

//		g2.setColor(Color.BLACK);
//		g2.fillRect(25,15,80,45);
//		g2.setColor(Color.CYAN);
//		g2.drawString("Total: " + numTracks, 30, 30);
//		g2.drawString("Inliers: "+numInliers,30,50);
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		if (sourceID == 0) {
			imageRGB = input;
			bufferedRGB = buffered;
		} else if (sourceID == 1) {
			imageDepth = (GrayU16)input;
			frameNumber++;

			long before = System.nanoTime();
			noFault = alg.process(imageRGB, imageDepth);
			long after = System.nanoTime();
			double elapsed = (after - before)/1e9;

			if (fps < 0) {
				fps = 1.0/elapsed;
			} else {
				double lambda = 0.95;
				fps = lambda*fps + (1.0 - lambda)*(1.0/elapsed);
			}

			if (!noFault) {
				alg.reset();
				guiCam3D.init();
			}

			if (frameNumber - 1 == statusPanel.getStopFrame()) {
				streamPaused = true;
			}

			updateGUI();
		}
	}

	protected void updateGUI() {
		if (!noFault) {
			numFaults++;
			return;
		}

		showTracks = statusPanel.isShowAll();
		showInliers = statusPanel.isShowInliers();

		if (renderedDepth == null) {
			renderedDepth = new BufferedImage(imageDepth.width, imageDepth.height, BufferedImage.TYPE_INT_RGB);
		}

		switch (algType) {
			case FEATURE -> drawFeatures((AccessPointTracks3D)alg, bufferedRGB);
			case DIRECT -> fractionInBounds = ((PyramidDirectColorDepth_to_DepthVisualOdometry)alg).getFractionInBounds();
			default -> {}
		}

		final Se3_F64 leftToWorld = ((Se3_F64)alg.getCameraToWorld()).copy();

		// TODO magic value from kinect. Add to config file?
		VisualizeImageData.disparity(imageDepth, renderedDepth, 10000, 0);

		SwingUtilities.invokeLater(() -> {
			guiLeft.setImage(bufferedRGB);
			guiDepth.setImage(renderedDepth);
			guiLeft.autoSetPreferredSize();
			guiDepth.autoSetPreferredSize();
			guiLeft.repaint();
			guiDepth.repaint();

			statusPanel.setCameraToWorld(leftToWorld);
			statusPanel.setNumFaults(numFaults);

			statusPanel.setFps(fps);
			statusPanel.setFrameNumber(frameNumber - 1);

			statusPanel.setPaused(streamPaused);

			switch (algType) {
				case FEATURE -> {
					featurePanel.setNumTracks(numTracks);
					featurePanel.setNumInliers(numInliers);
				}
				case DIRECT -> {
					directPanel.setInBounds(fractionInBounds);
				}
				default -> {}
			}
		});

		double r = 0.15;

		Point3D_F64 p1 = new Point3D_F64(-r, -r, 0);
		Point3D_F64 p2 = new Point3D_F64(r, -r, 0);
		Point3D_F64 p3 = new Point3D_F64(r, r, 0);
		Point3D_F64 p4 = new Point3D_F64(-r, r, 0);

		SePointOps_F64.transform(leftToWorld, p1, p1);
		SePointOps_F64.transform(leftToWorld, p2, p2);
		SePointOps_F64.transform(leftToWorld, p3, p3);
		SePointOps_F64.transform(leftToWorld, p4, p4);

		guiCam3D.add(p1, p2, p3, p4);
		guiCam3D.repaint();
	}

	private void changeSelectedAlgortihm( int whichAlg ) {

		this.whichAlg = whichAlg;
		AlgType prevAlgType = this.algType;


		Class imageType = GrayU8.class;
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DepthSparse3D<GrayU16> sparseDepth = new DepthSparse3D.I<>(1e-3);

		var configVO = new ConfigVisOdomTrackPnP();
		configVO.refineIterations = 40;
		configVO.ransac.inlierThreshold = 1.5;
		configVO.ransac.iterations = 200;

		ConfigPointTracker configTracker = new ConfigPointTracker();

		configTracker.klt.templateRadius = 3;
		configTracker.klt.pyramidLevels = ConfigDiscreteLevels.levels(4);

		configTracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		configTracker.detDesc.detectPoint.scaleRadius = 12;
		configTracker.detDesc.detectPoint.shiTomasi.radius = 3;
		configTracker.detDesc.detectPoint.general.threshold = 1.0f;
		configTracker.detDesc.detectPoint.general.radius = 4;
		configTracker.detDesc.detectPoint.general.maxFeatures = 400;
		configTracker.detDesc.detectPoint.general.selector.type = SelectLimitTypes.SELECT_N;

		configTracker.associate.type = ConfigAssociate.AssociationType.GREEDY;
		configTracker.associate.greedy.forwardsBackwards = true;
		configTracker.associate.greedy.scoreRatioThreshold = 0.8;

		algType = AlgType.UNKNOWN;
		if (whichAlg == 0) {
			algType = AlgType.FEATURE;

			configTracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
			configTracker.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;

			PointTracker tracker = FactoryPointTracker.tracker(configTracker, imageType, derivType);
			alg = FactoryVisualOdometry.rgbDepthPnP(configVO, sparseDepth, tracker, imageType, GrayU16.class);

//			ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600, 3, 1);
//			PointTrackerTwoPass tracker = FactoryPointTrackerTwoPass.klt(configPKlt, configDetector,
//					imageType, derivType);
//			alg = FactoryVisualOdometry.
//					depthDepthPnP(1.5, 120, 2, 200, 50, false, sparseDepth, tracker, imageType, GrayU16.class);
		} else if (whichAlg == 1) {
			algType = AlgType.FEATURE;

			configTracker.typeTracker = ConfigPointTracker.TrackerType.DDA;
			configTracker.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
			configTracker.detDesc.typeDescribe = ConfigDescribeRegion.Type.BRIEF;

			PointTracker tracker = FactoryPointTracker.tracker(configTracker, imageType, derivType);

			alg = FactoryVisualOdometry.rgbDepthPnP(configVO, sparseDepth, tracker, imageType, GrayU16.class);

//			alg = FactoryVisualOdometry.
//					depthDepthPnP(1.5, 80, 3, 200, 50, false, sparseDepth, tracker, imageType, GrayU16.class);
		} else if (whichAlg == 2) {
			algType = AlgType.FEATURE;

			configTracker.typeTracker = ConfigPointTracker.TrackerType.HYBRID;
			configTracker.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
			configTracker.detDesc.typeDescribe = ConfigDescribeRegion.Type.SURF_STABLE;

			PointTracker tracker = FactoryPointTracker.tracker(configTracker, imageType, derivType);

			alg = FactoryVisualOdometry.rgbDepthPnP(configVO, sparseDepth, tracker, imageType, GrayU16.class);

//			PointTracker tracker = FactoryPointTracker.
//					combined_ST_SURF_KLT(new ConfigGeneralDetector(600, 3, 1),
//							configPKlt, 50, null, null, imageType, derivType);
//
//			PointTrackerTwoPass twopass = new PointTrackerToTwoPass<>(tracker);
//
//			alg = FactoryVisualOdometry.
//					depthDepthPnP(1.5, 120, 3, 200, 50, false, sparseDepth, twopass, imageType, GrayU16.class);
		} else if (whichAlg == 3) {
			algType = AlgType.DIRECT;
			alg = FactoryVisualOdometry.
					depthDirect(sparseDepth, ImageType.pl(3, GrayF32.class), GrayU16.class);
		} else {
			throw new RuntimeException("Unknown selection");
		}

		if (algType != prevAlgType) {
			switch (prevAlgType) {
				case FEATURE -> mainPanel.remove(featurePanel);
				case DIRECT -> mainPanel.remove(directPanel);
				default -> mainPanel.remove(algorithmPanel);
			}

			switch (algType) {
				case FEATURE -> mainPanel.add(featurePanel, BorderLayout.NORTH);
				case DIRECT -> mainPanel.add(directPanel, BorderLayout.NORTH);
				default -> mainPanel.add(algorithmPanel, BorderLayout.NORTH);
			}
			mainPanel.invalidate();
		}

		setImageTypes(alg.getVisualType(), ImageType.single(alg.getDepthType()));
	}

	protected void handleRunningStatus( Status status ) {
		final String text;
		final Color color = switch (status) {
			case RUNNING -> Color.BLACK;
			case PAUSED -> Color.RED;
			case FINISHED -> Color.RED;
			default -> Color.BLUE;
		};

		text = status.name();

		SwingUtilities.invokeLater(() -> {
			switch (algType) {
				case FEATURE -> featurePanel.setStatus(text, color);
				case DIRECT -> directPanel.setStatus(text, color);
				default -> algorithmPanel.setStatus(text, color);
			}
		});
	}

	@Override
	public void eventVoPanel( final int view ) {
		SwingUtilities.invokeLater(() -> {
			if (view == 0) {
				viewPanel.setRightComponent(guiDepth);
			} else {
				viewPanel.setRightComponent(guiCam3D);
			}
			viewPanel.revalidate();
			viewPanel.repaint();
		});
	}

	@Override
	public void handlePausedToggle() {
		streamPaused = statusPanel.paused;
	}

	@Override
	public void handleStep() {
		if (streamPaused) {
			streamPaused = false;
		}
		streamStepCounter = 1;
	}

	@Override
	protected void enterPausedState() {
		SwingUtilities.invokeLater(() -> statusPanel.setPaused(true));
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() != selectAlgorithm)
			return;

		int which = selectAlgorithm.getSelectedIndex();

		if (which == whichAlg)
			return;

		stopAllInputProcessing();
		changeSelectedAlgortihm(which);
		reprocessInput();
	}

	enum Status {
		RUNNING,
		PAUSED,
		FINISHED,
		UNKNOWN
	}

	enum AlgType {
		UNKNOWN,
		FEATURE,
		DIRECT
	}

	public static void main( String[] args ) throws FileNotFoundException {

//		List<PathLabel> inputs = new ArrayList<>();
//		inputs.add(new PathLabel("Circle", UtilIO.pathExample("kinect/circle/config.txt")));
//		inputs.add(new PathLabel("Hallway", UtilIO.pathExample("kinect/straight/config.txt")));
//
//		VisualizeDepthVisualOdometryApp app = new VisualizeDepthVisualOdometryApp(inputs);
//
//		app.openFile(new File(inputs.get(0).getPath()));
//		app.waitUntilInputSizeIsKnown();
//
//		ShowImages.showWindow(app, "Depth Visual Odometry",true);

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Circle", UtilIO.pathExample("kinect/circle/config.txt")));
		inputs.add(new PathLabel("Hallway", UtilIO.pathExample("kinect/straight/config.txt")));

		SwingUtilities.invokeLater(() -> {
			var app = new VisualizeDepthVisualOdometryApp(inputs);

			// Processing time takes a bit so don't open right away
			app.openExample(inputs.get(0));
			app.displayImmediate("RGB-D Visual Odometry");
		});
	}
}
