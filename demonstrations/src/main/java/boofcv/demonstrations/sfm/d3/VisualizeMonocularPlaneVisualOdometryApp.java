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

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.MonocularPlaneVisualOdometry;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.sfm.ConfigPlanarTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.VisualizeApp;
import boofcv.gui.d2.PlaneView2D;
import boofcv.gui.d3.Polygon3DSequenceViewer;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Visualizes {@link MonocularPlaneVisualOdometry}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "NullAway.Init"})
public class VisualizeMonocularPlaneVisualOdometryApp<I extends ImageGray<I>>
		extends VideoProcessAppBase<I> implements VisualizeApp, VisualOdometryPanel.Listener {

	protected MonoPlaneParameters config;
	VisualOdometryPanel guiInfo;

	ImagePanel guiLeft;
	Polygon3DSequenceViewer guiCam3D;
	PlaneView2D gui2D;

	Class<I> imageClass;

	MonocularPlaneVisualOdometry<I> alg;

	boolean hasProcessedImage = false;
	boolean noFault;

	boolean showTracks;
	boolean showInliers;

	int numFaults;
	int numTracks;
	int numInliers;
	int whichAlg;

	public VisualizeMonocularPlaneVisualOdometryApp( Class<I> imageClass ) {
		super(1, imageClass);
		this.imageClass = imageClass;

		addAlgorithm(0, "Plane-Infinity : KLT", 0);
		addAlgorithm(0, "Overhead : KLT", 1);

		guiInfo = new VisualOdometryPanel(VisualOdometryPanel.Type.MONO_PLANE);
		guiLeft = new ImagePanel();
		guiCam3D = new Polygon3DSequenceViewer();
		gui2D = new PlaneView2D(0.1);

		add(guiInfo, BorderLayout.WEST);
		add(gui2D, BorderLayout.EAST);
		setMainGUI(guiLeft);

		guiLeft.addMouseListener(this);
		guiInfo.setListener(this);
	}

	private void drawFeatures( AccessPointTracks3D tracker, BufferedImage image ) {

		numInliers = 0;

		Graphics2D g2 = image.createGraphics();

		List<Point2D_F64> points = tracker.getAllTracks(null);

		if (points.size() == 0)
			return;

		Point3D_F64 world = new Point3D_F64();
		double[] ranges = new double[points.size()];

		for (int i = 0; i < points.size(); i++) {
			tracker.getTrackWorld3D(i, world);
			ranges[i] = world.z;
		}
		Arrays.sort(ranges);
		double maxRange = ranges[(int)(ranges.length*0.8)];

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
	public void changeInput( String name, int index ) {
		stopWorker();
		Reader r = media.openFile(inputRefs.get(index).getPath());
		BufferedReader in = new BufferedReader(r);
		try {
			String path = new File(inputRefs.get(index).getPath()).getParent();

			String lineConfig = in.readLine();
			String line1 = in.readLine();

			// adjust for relative paths
			if (lineConfig.charAt(0) != '/')
				lineConfig = path + "/" + lineConfig;
			if (line1.charAt(0) != '/')
				line1 = path + "/" + line1;

			config = CalibrationIO.load(media.openFileNotNull(lineConfig));
			SimpleImageSequence<I> video = media.openVideoNotNull(line1, imageType);

			process(Objects.requireNonNull(video));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	protected void process( SimpleImageSequence<I> sequence ) {
		// stop the image processing code
		stopWorker();

		this.sequence = sequence;
		sequence.setLoop(false);

		// start everything up and resume processing
		doRefreshAll();
	}

	@Override
	protected void updateAlg( I frame, BufferedImage buffImage ) {
		if (config.intrinsic.width != frame.width || config.intrinsic.height != frame.height)
			throw new IllegalArgumentException("Miss match between calibration and actual image size");

		noFault = alg.process(frame);
		if (!noFault) {
			alg.reset();
			guiCam3D.init();
		}
	}

	@Override
	protected void updateAlgGUI( I frame1, final BufferedImage buffImage1, final double fps ) {
		if (!noFault) {
			numFaults++;
			return;
		}

		showTracks = guiInfo.isShowAll();
		showInliers = guiInfo.isShowInliers();

		if (alg instanceof AccessPointTracks3D)
			drawFeatures((AccessPointTracks3D)alg, buffImage1);

		final Se3_F64 leftToWorld = alg.getCameraToWorld().copy();

		SwingUtilities.invokeLater(() -> {
			guiLeft.setImage(buffImage1);
			guiLeft.autoSetPreferredSize();
			guiLeft.repaint();

			guiInfo.setCameraToWorld(leftToWorld);
			guiInfo.setNumFaults(numFaults);
			guiInfo.setNumTracks(numTracks);
			guiInfo.setNumInliers(numInliers);
			guiInfo.setFps(fps);
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

		gui2D.addPoint(leftToWorld.T.x, leftToWorld.T.z);
		gui2D.repaint();

		hasProcessedImage = true;
	}

	@Override
	public void refreshAll( @Nullable Object[] cookies ) {

		numFaults = 0;
		if (cookies != null)
			whichAlg = (Integer)cookies[0];
		alg = createVisualOdometry(whichAlg);
		alg.setCalibration(config);

		guiInfo.reset();
		gui2D.reset();

		handleRunningStatus(2);

		guiCam3D.init();
		guiCam3D.setFocalLength(300);
		guiCam3D.setStepSize(1);
		guiCam3D.setPreferredSize(new Dimension(config.intrinsic.width, config.intrinsic.height));
		guiCam3D.setMaximumSize(guiCam3D.getPreferredSize());
		gui2D.setPreferredSize(new Dimension(config.intrinsic.width, config.intrinsic.height));
		gui2D.setMaximumSize(gui2D.getPreferredSize());
		startWorkerThread();
	}

	private MonocularPlaneVisualOdometry<I> createVisualOdometry( int whichAlg ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageClass);

		if (whichAlg == 0) {
			var config = new ConfigPlanarTrackPnP();
			config.tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
			config.tracker.klt.pyramidLevels = ConfigDiscreteLevels.levels(4);
			config.tracker.klt.templateRadius = 3;

			config.tracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
			config.tracker.detDesc.detectPoint.general.maxFeatures = 600;
			config.tracker.detDesc.detectPoint.general.radius = 3;
			config.tracker.detDesc.detectPoint.general.threshold = 1;

			config.thresholdAdd = 75;
			config.thresholdRetire = 2;
			config.ransac.iterations = 200;
			config.ransac.inlierThreshold = 1.5;

			return FactoryVisualOdometry.monoPlaneInfinity(config, imageClass);
		} else if (whichAlg == 1) {
			ConfigPKlt configKlt = new ConfigPKlt();
			configKlt.pyramidLevels = ConfigDiscreteLevels.levels(4);
			configKlt.templateRadius = 3;
			ConfigPointDetector configDetector = new ConfigPointDetector();
			configDetector.type = PointDetectorTypes.SHI_TOMASI;
			configDetector.general.maxFeatures = 600;
			configDetector.general.radius = 3;
			configDetector.general.threshold = 1;

			PointTracker<I> tracker = FactoryPointTracker.klt(configKlt, configDetector, imageClass, derivType);

			double cellSize = 0.06;
			double inlierGroundTol = 1.5;

			return FactoryVisualOdometry.monoPlaneOverhead(cellSize, 25, 0.7,
					inlierGroundTol, 300, 2, 100, 0.5, 0.6, tracker, imageType);
		} else {
			throw new RuntimeException("Unknown selection");
		}
	}

	@Override
	public void setActiveAlgorithm( int indexFamily, String name, Object cookie ) {

		stopWorker();

		whichAlg = (Integer)cookie;

		sequence.reset();

		refreshAll(null);
	}

	@Override
	public void loadConfigurationFile( String fileName ) {}

	@Override
	public boolean getHasProcessedImage() {
		return hasProcessedImage;
	}

	@Override
	protected void handleRunningStatus( int status ) {
		final String text;
		final Color color;

		switch (status) {
			case 0:
				text = "RUNNING";
				color = Color.BLACK;
				break;

			case 1:
				text = "PAUSED";
				color = Color.RED;
				break;

			case 2:
				text = "FINISHED";
				color = Color.RED;
				break;

			default:
				text = "UNKNOWN";
				color = Color.BLUE;
		}

		SwingUtilities.invokeLater(() -> guiInfo.setStatus(text, color));
	}

	@Override
	public void eventVoPanel( final int view ) {
		SwingUtilities.invokeLater(() -> {
			if (view == 0) {
				gui2D.setPreferredSize(guiCam3D.getPreferredSize());
				remove(guiCam3D);
				add(gui2D, BorderLayout.EAST);
			} else {
				guiCam3D.setPreferredSize(gui2D.getPreferredSize());
				remove(gui2D);
				add(guiCam3D, BorderLayout.EAST);
			}
			revalidate();
			repaint();
		});
	}

	public static void main( String[] args ) {

		Class type = GrayF32.class;
//		Class type = GrayU8.class;

		VisualizeMonocularPlaneVisualOdometryApp app = new VisualizeMonocularPlaneVisualOdometryApp(type);

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Simulation", UtilIO.pathExample("vo/drc/config_plane.txt")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			BoofMiscOps.sleep(10);
		}

		ShowImages.showWindow(app, "Monocular Plane Visual Odometry", true);
	}
}
