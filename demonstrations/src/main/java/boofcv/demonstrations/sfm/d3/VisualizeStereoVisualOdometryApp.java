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

import boofcv.BoofVerbose;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.abst.sfm.d3.WrapVisOdomDualTrackPnP;
import boofcv.abst.sfm.d3.WrapVisOdomMonoStereoDepthPnP;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.SelectLimitTypes;
import boofcv.factory.sfm.ConfigStereoDualTrackPnP;
import boofcv.factory.sfm.ConfigStereoMonoTrackPnP;
import boofcv.factory.sfm.ConfigStereoQuadPnP;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.ControlPanelPointCloud;
import boofcv.gui.controls.ControlPanelStereoDualTrackPnP;
import boofcv.gui.controls.ControlPanelStereoMonoTrackPnP;
import boofcv.gui.controls.ControlPanelStereoQuadPnP;
import boofcv.gui.dialogs.OpenStereoSequencesChooser;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.settings.GlobalDemoSettings;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.struct.RotationType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static boofcv.gui.BoofSwingUtil.*;
import static boofcv.io.image.ConvertBufferedImage.checkCopy;
import static boofcv.io.image.ConvertBufferedImage.convertFrom;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

/**
 * Visualizes stereo visual odometry.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeStereoVisualOdometryApp<T extends ImageGray<T>> extends DemonstrationBase {
	// Main GUI elements for the app
	ControlPanel controls = new ControlPanel();
	StereoPanel stereoPanel = new StereoPanel();
	PointCloudPanel cloudPanel = new PointCloudPanel();

	StereoVisualOdometry<T> alg;
	T inputLeft, inputRight;
	StereoParameters stereoParameters;

	//-------------- Control lock on features
	final DogArray<FeatureInfo> features = new DogArray<>(FeatureInfo::new);
	final DogArray<Se3_F64> egoMotion_cam_to_world = new DogArray<>(Se3_F64::new); // estimated ego motion
	final DogArray_I32 visibleTracks = new DogArray_I32(); // index of tracks visible in current frame in 'features'
	final TLongIntMap trackId_to_arrayIdx = new TLongIntHashMap(); // track ID to array Index
	volatile long latestFrameID; // the frame ID after processing the most recent image
	//-------------- END lock

	// if true that means it will change the view to match the latest estimate
	boolean followCamera = false;

	// If the input in a single stream and the stream should be split into two images
	boolean splitFrame = false;

	// number if stereo frames processed
	int frame = 0;
	// total distance traveled
	double traveled = 0;
	final Se3_F64 prev_to_world = new Se3_F64();

	ImageType imageType;

	public VisualizeStereoVisualOdometryApp( List<PathLabel> examples, Class<T> imageType ) {
		super(true, false, examples, ImageType.single(imageType));
		useCustomOpenFiles = true;
		this.imageType = ImageType.single(imageType);

		addViewMenu();

		alg = createSelectedAlgorithm();
		inputLeft = alg.getImageType().createImage(1, 1);
		inputRight = alg.getImageType().createImage(1, 1);

		var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stereoPanel, cloudPanel);
		split.setDividerLocation(320);
		setAutomaticImageResize(split);

		controls.setPreferredSize(new Dimension(220, 0));

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, split);

		setPreferredSize(new Dimension(1200, 600));
	}

	/**
	 * Create a view menu that let's you easily set the 3D view to different locations
	 */
	private void addViewMenu() {
		JMenu menuView = new JMenu("View");
		menuView.add(BoofSwingUtil.createMenuItem("Home", this::setViewToHome));
		menuView.add(BoofSwingUtil.createMenuItem("Latest", this::setViewToLatest));
		menuView.add(BoofSwingUtil.createMenuItem("Follow", KeyEvent.VK_F, KeyEvent.VK_F, this::setViewToFollow));
		menuBar.add(menuView);
	}

	/**
	 * Resize the image view automatically as the divider is changed
	 */
	private void setAutomaticImageResize( JSplitPane split ) {
		split.addPropertyChangeListener(changeEvent -> {
			String propertyName = changeEvent.getPropertyName();
			if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
				final T left = inputLeft;
				if (left == null)
					return;
				double scale = split.getDividerLocation()/(double)left.width;
				controls.setZoom(Math.min(1.0, scale));
			}
		});
	}

	@Override protected void openFileMenuBar() {
		OpenStereoSequencesChooser.Selected s = BoofSwingUtil.openStereoChooser(window, null, true, false);
		if (s == null)
			return;

		final var files = new ArrayList<File>();
		files.add(s.left);
		if (!s.isSplit()) {
			files.add(s.right);
		}
		files.add(s.calibration);
		splitFrame = s.isSplit();

		openFiles(files);
	}

	@Override protected void customAddToFileMenu( JMenu menuFile ) {
		menuFile.addSeparator();

		var itemSaveConfiguration = new JMenuItem("Save Configuration");
		itemSaveConfiguration.addActionListener(e -> saveConfiguration());
		menuFile.add(itemSaveConfiguration);

		var itemSavePath = new JMenuItem("Save Path");
		itemSavePath.addActionListener(e -> savePath());
		menuFile.add(itemSavePath);

		var itemSaveCloud = new JMenuItem("Save Point Cloud");
		itemSaveCloud.addActionListener(e -> savePointCloud());
		menuFile.add(itemSaveCloud);
	}

	/**
	 * Saves the sparse cloud created by VO
	 */
	private void savePointCloud() {
		// Make sure the cloud isn't being modified by stopping any processing that might be going on
		stopAllInputProcessing();
		// Save it to disk
		BoofSwingUtil.savePointCloudDialog(this, KEY_PREVIOUS_DIRECTORY, cloudPanel.gui);
	}

	/**
	 * Save the estimate path the left camera took
	 */
	private void savePath() {
		// Stop processing so that the ego motion list isn't being modified
		stopAllInputProcessing();

		String fileName = "camera_pose.csv";

		// Select a file but keep the name as the default
		File selected = BoofSwingUtil.fileChooser(null, this, false, new File(fileName).getPath(),
				( path ) -> new File(new File(path).getParentFile(), fileName).getPath(), FileTypes.YAML);
		if (selected == null)
			return;

		UtilIO.savePoseListCsv(egoMotion_cam_to_world.toList(), RotationType.QUATERNION, selected);
	}

	/**
	 * Save visual odometry configuration to disk
	 */
	private void saveConfiguration() {
		String fileName = "StereoVisualOdometry.yaml";

		// Select a file but keep the name as the default
		File selected = BoofSwingUtil.fileChooser(null, this, false, new File(fileName).getPath(),
				( path ) -> new File(new File(path).getParentFile(), fileName).getPath(), FileTypes.YAML);
		if (selected == null)
			return;

		switch (controls.approach) {
			case 0 -> UtilIO.saveConfig(controls.controlMonoTrack.createConfiguration(),
					new ConfigStereoMonoTrackPnP(), selected);
			case 1 -> UtilIO.saveConfig(controls.controlDualTrack.config, new ConfigStereoDualTrackPnP(), selected);
			case 2 -> UtilIO.saveConfig(controls.controlQuad.config, new ConfigStereoQuadPnP(), selected);
			default -> throw new RuntimeException("Unknown approach " + controls.approach);
		}
	}

	/**
	 * Stop whatever it's doing, create a new visual odometry instance, start processing input sequence from the start
	 */
	public void resetWithNewAlgorithm() {
		BoofSwingUtil.checkGuiThread();

		// disable now that we are using it's controls
		controls.bUpdateAlg.setEnabled(false);

		stopAllInputProcessing();
		alg = createSelectedAlgorithm();
		reprocessInput();
	}

	public StereoVisualOdometry<T> createSelectedAlgorithm() {
		Class<T> imageType = getImageType(0).getImageClass();

		return switch (controls.approach) {
			case 0 -> controls.controlMonoTrack.createVisOdom(imageType);
			case 1 -> controls.controlDualTrack.createVisOdom(imageType);
			case 2 -> controls.controlQuad.createVisOdom(imageType);
			default -> throw new RuntimeException("Unknown approach " + controls.approach);
		};
	}

	private static ConfigStereoMonoTrackPnP createConfigStereoMonoPnP() {
		ConfigStereoMonoTrackPnP config = new ConfigStereoMonoTrackPnP();

		config.tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;

		config.tracker.klt.maximumTracks.setRelative(0.0016, 300);
		config.tracker.klt.toleranceFB = 3;
		config.tracker.klt.pruneClose = true;
		config.tracker.klt.config.maxIterations = 25;
		config.tracker.klt.templateRadius = 4;
		config.tracker.klt.pyramidLevels = ConfigDiscreteLevels.minSize(40);

		config.tracker.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
		config.tracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.tracker.detDesc.detectPoint.shiTomasi.radius = 3;
		config.tracker.detDesc.detectPoint.general.threshold = 1.0f;
		config.tracker.detDesc.detectPoint.general.radius = 3;
		config.tracker.detDesc.detectPoint.general.selector.type = SelectLimitTypes.SELECT_N;

		config.tracker.associate.greedy.scoreRatioThreshold = 0.75;
		config.tracker.associate.nearestNeighbor.scoreRatioThreshold = 0.75;
		config.tracker.associate.maximumDistancePixels.setRelative(0.25, 0);

		config.scene.maxKeyFrames = 4;

		config.disparity.disparityMin = 0;
		config.disparity.disparityRange = 50;
		config.disparity.regionRadiusX = 3;
		config.disparity.regionRadiusY = 3;
		config.disparity.maxPerPixelError = 30;
		config.disparity.texture = 0.05;
		config.disparity.subpixel = true;
		config.disparity.validateRtoL = 1;

		return config;
	}

	private static ConfigStereoDualTrackPnP createConfigStereoDualPnP() {
		var config = new ConfigStereoDualTrackPnP();

		config.tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;

		config.tracker.klt.maximumTracks.setRelative(0.0016, 300);
		config.tracker.klt.toleranceFB = 3;
		config.tracker.klt.pruneClose = true;
		config.tracker.klt.config.maxIterations = 25;
		config.tracker.klt.templateRadius = 4;
		config.tracker.klt.pyramidLevels = ConfigDiscreteLevels.minSize(40);

		config.tracker.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
		config.tracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.tracker.detDesc.detectPoint.shiTomasi.radius = 3;
		config.tracker.detDesc.detectPoint.general.threshold = 1.0f;
		config.tracker.detDesc.detectPoint.general.radius = 5;
		config.tracker.detDesc.detectPoint.general.selector.type = SelectLimitTypes.SELECT_N;

		config.tracker.associate.greedy.scoreRatioThreshold = 0.75;
		config.tracker.associate.nearestNeighbor.scoreRatioThreshold = 0.75;
		config.tracker.associate.maximumDistancePixels.setRelative(0.25, 0);

		config.scene.maxKeyFrames = 6;
		config.scene.ransac.inlierThreshold = 1.5;
		config.stereoDescribe.type = ConfigDescribeRegion.Type.BRIEF;
		config.stereoRadius = 6;
		config.epipolarTol = 1.0;

		config.scene.keyframes.geoMinCoverage = 0.4;

		return config;
	}

	private static ConfigStereoQuadPnP createConfigStereoQuadPnP() {
		var config = new ConfigStereoQuadPnP();
		config.associateF2F.maximumDistancePixels.setRelative(0.25, 0);
		config.epipolarTol = 4;
		config.ransac.iterations = 1000;
		config.refineIterations = 5;

		config.detectDescribe.typeDetector = ConfigDetectInterestPoint.Type.POINT;
		config.detectDescribe.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.detectDescribe.detectPoint.shiTomasi.radius = 3;
		config.detectDescribe.detectPoint.general.radius = 3;
		config.detectDescribe.detectPoint.general.maxFeatures = 1000;

		config.associateF2F.greedy.scoreRatioThreshold = 0.9;

		return config;
	}

	@Override protected boolean openCustomFiles( String[] filePaths, List<String> outSequence, List<String> outImages ) {
		int videoCount = splitFrame ? 1 : 2;
		if (filePaths.length == videoCount) {
			File f = new File(filePaths[0]).getParentFile();
			stereoParameters = CalibrationIO.load(new File(f, "stereo.yaml"));
		} else if (filePaths.length == videoCount + 1) {
			stereoParameters = CalibrationIO.load(filePaths[videoCount]);
		} else {
			throw new RuntimeException("Unexpected number of files " + filePaths.length);
		}

		outSequence.add(filePaths[0]);
		if (!splitFrame)
			outSequence.add(filePaths[1]);
		return true;
	}

	@Override protected void openVideo( boolean reopen, String... filePaths ) {
		// See if the last argument is a configuration. If so load and remove it otherwise it will cause
		// a crash later on
		try {
			stereoParameters = CalibrationIO.load(filePaths[filePaths.length - 1]);
			String[] temp = new String[filePaths.length - 1];
			System.arraycopy(filePaths, 0, temp, 0, temp.length);
			filePaths = temp;
		} catch (RuntimeException ignore) {}

		// If there is just one input then it must be a split image
		splitFrame = filePaths.length == 1;

		// The base class assumes the number of streams is constant. So we hack it to be dynamic.
		if (splitFrame)
			setImageTypes(imageType);
		else
			setImageTypes(imageType, imageType);

		super.openVideo(reopen, filePaths);
	}

	@Override protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		if (stereoParameters == null)
			throw new RuntimeException("stereoParameters should have been loaded in openFiles()");
		followCamera = false;
		frame = 0;
		traveled = 0.0;
		prev_to_world.reset();
		egoMotion_cam_to_world.reset();
		alg.reset();
		alg.setCalibration(stereoParameters);

		configureVerbosity();

		synchronized (features) {
			features.reset();
			trackId_to_arrayIdx.clear();
			visibleTracks.reset();
		}

		double hfov = PerspectiveOps.computeHFov(stereoParameters.left);

		int leftWidth = splitFrame ? width/2 : width;
		int leftHeight = height;

		SwingUtilities.invokeLater(() -> {
			// change the scale so that the entire image is visible
			stereoPanel.setPreferredSize(new Dimension(leftWidth, leftHeight*2));
			double scale = BoofSwingUtil.selectZoomToShowAll(stereoPanel, leftWidth, leftHeight*2);
			controls.setZoom(scale);
			cloudPanel.configureViewer(hfov);
			cloudPanel.gui.setTranslationStep(stereoParameters.getBaseline());
		});
	}

	private void configureVerbosity() {
		// Turn on verbose output if requested in global settings
		GlobalDemoSettings settings = GlobalDemoSettings.SETTINGS.copy();
		Set<String> configuration = new HashSet<>();
		if (settings.verboseTracking)
			configuration.add(VisualOdometry.VERBOSE_TRACKING);
		if (settings.verboseRuntime)
			configuration.add(BoofVerbose.RUNTIME);
		if (configuration.isEmpty())
			alg.setVerbose(null, null);
		else
			alg.setVerbose(System.out, configuration);
	}

	@Override public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		if (splitFrame) {
			int leftWidth = splitFrame ? buffered.getWidth()/2 : buffered.getWidth();
			int height = buffered.getHeight();
			T full = (T)input;
			inputLeft.setTo(full.subimage(0, 0, leftWidth, height));
			inputRight.setTo(full.subimage(leftWidth, 0, buffered.getWidth(), height));


			stereoPanel.left = checkCopy(buffered.getSubimage(0, 0, leftWidth, height), stereoPanel.left);
			stereoPanel.right = checkCopy(buffered.
					getSubimage(leftWidth, 0, buffered.getWidth() - leftWidth, height), stereoPanel.right);
		} else {
			switch (sourceID) {
				case 0 -> {
					stereoPanel.left = checkCopy(buffered, stereoPanel.left);
					convertFrom(buffered, true, inputLeft);
				}
				case 1 -> {
					stereoPanel.right = checkCopy(buffered, stereoPanel.right);
					convertFrom(buffered, true, inputRight);
				}
				default -> throw new RuntimeException("BUG");
			}

			if (sourceID == 0)
				return;
		}

		long time0 = System.nanoTime();
		boolean success = alg.process(inputLeft, inputRight);
		long time1 = System.nanoTime();

		latestFrameID = alg.getFrameID();
		Se3_F64 camera_to_world = alg.getCameraToWorld();
		Se3_F64 world_to_camera = camera_to_world.invert(null);

		// Save the camera motion history
		egoMotion_cam_to_world.grow().setTo(camera_to_world);

		if (success) {
			// Sum up the total distance traveled. This will be approximate since it optimizes past history
			traveled += prev_to_world.concat(world_to_camera, null).T.norm();
			prev_to_world.setTo(camera_to_world);
		}

		if (alg instanceof AccessPointTracks3D) {
			extractFeatures(world_to_camera, (AccessPointTracks3D)alg, buffered);
		}

		final int bundleTracks = countTracksUsedInBundleAdjustment();

		// Update the visualization
		int frame = this.frame;
		SwingUtilities.invokeLater(() -> {
			if (followCamera) {
				cloudPanel.gui.setCameraToWorld(camera_to_world);
			}
			controls.setFrame(frame);
			controls.setProcessingTimeMS((time1 - time0)*1e-6);
			controls.setImageSize(buffered.getWidth(), buffered.getHeight());
			controls.setBundleTracks(bundleTracks);
			controls.setDistanceTraveled(traveled);
			stereoPanel.repaint();
			cloudPanel.update();
		});
		this.frame++;
	}

	/**
	 * Counts the number of tracks being used by bundle adjustment
	 */
	private int countTracksUsedInBundleAdjustment() {
		FastAccess<BTrack> tracks = null;
		int bundleTracks;
		if (alg instanceof WrapVisOdomMonoStereoDepthPnP) {
			tracks = ((WrapVisOdomMonoStereoDepthPnP)alg).getAlgorithm().getBundleViso().tracks;
		} else if (alg instanceof WrapVisOdomDualTrackPnP) {
			tracks = ((WrapVisOdomDualTrackPnP)alg).getAlgorithm().getBundleViso().tracks;
		}

		if (tracks != null) {
			int total = 0;
			for (int i = 0; i < tracks.size; i++) {
				BTrack bt = tracks.get(i);
				if (bt.selected)
					total++;
			}
			bundleTracks = total;
		} else {
			bundleTracks = -1;
		}
		return bundleTracks;
	}

	/**
	 * Extracts and copies information about the features currently visible
	 */
	public void extractFeatures( Se3_F64 world_to_camera, AccessPointTracks3D access, BufferedImage image ) {
		synchronized (features) {
			visibleTracks.reset();
			final var camera3D = new Point3D_F64();
			final int N = access.getTotalTracks();
			int totalInliers = 0;
			for (int i = 0; i < N; i++) {
				long id = access.getTrackId(i);
				if (id == -1)
					throw new RuntimeException("BUG! Got id = -1");
				int arrayIndex;
				FeatureInfo f;
				if (access.isTrackNew(i)) {
					arrayIndex = features.size;
					f = features.grow();
					f.id = id;
					f.firstFrame = frame;
					if (trackId_to_arrayIdx.containsKey(id))
						System.err.println("BUG! Already contains key of new track: " + id);
					trackId_to_arrayIdx.put(id, arrayIndex);
					access.getTrackPixel(i, f.pixel);
					// Grab the pixel's color for visualization
					int x = (int)f.pixel.x;
					int y = (int)f.pixel.y;
					if (BoofMiscOps.isInside(image.getWidth(), image.getHeight(), x, y)) {
						f.rgb = image.getRGB(x, y);
					} else {
						// default to the color red if it's outside the image. Pure red is unusual and should make
						// it easy to see bugs
						f.rgb = 0xFF0000;
					}
				} else {
					arrayIndex = trackId_to_arrayIdx.get(id);
					f = features.get(arrayIndex);
					access.getTrackPixel(i, f.pixel);
				}
				f.lastFrame = frame;
				visibleTracks.add(arrayIndex);

				access.getTrackWorld3D(i, f.world);
				SePointOps_F64.transform(world_to_camera, f.world, camera3D);
				f.depth = camera3D.z;
				f.inlier = access.isTrackInlier(i);
				if (f.inlier)
					totalInliers++;
			}

			int _totalInliers = totalInliers;
			BoofSwingUtil.invokeNowOrLater(() -> {
				controls.setInliersTracks(_totalInliers);
				controls.setVisibleTracks(N);
			});
		}
	}

	// Information copied from VO algorithm. This increased how much the processing thread is decoupled from
	// the GUI thread leading to a better user experience
	static class FeatureInfo {
		// Most recently visible pixel coordinate
		public final Point2D_F64 pixel = new Point2D_F64();
		// 3D Location of feature in world coordinates
		public final Point3D_F64 world = new Point3D_F64();
		public boolean inlier; // if it is an inlier that was used to estimate VO
		public double depth; // depth in current camera frame
		public long id; // unique tracker id for the feature
		public int rgb; // color in first frame
		public int firstFrame; // first frame the track was seen in
		public int lastFrame; // last frame the track was seen in
	}

	class ControlPanel extends DetectBlackShapePanel {
		// flags that toggle what's visualized in stereo panel view
		boolean showInliers = false;
		boolean showNew = false;

		int approach = 0; // which visual odometry approach has been selected

		double maxDepth = 0; // Maximum depth a feature is from the camera when last viewed
		boolean showCameras = true; // show camera locations in 3D view
		boolean showCloud = true; // Show point cloud created from feature tracks
		int showEveryCameraN = 5; // show the camera every N frames
		// the maximum number of frames in the past the track was first spawned and stuff be visible. 0 = infinite
		int maxTrackAge = 0;
		int minTrackDuration = 1;// Only show in 3D if the number of frames the track was seen was at least this amount
		int trackColors = 0; // 0 = depth, 1 = age

		final JLabel videoFrameLabel = new JLabel();
		final JButton bPause = buttonIcon("Pause24.gif", true);
		final JButton bStep = buttonIcon("StepForward24.gif", true);
		final JButton bUpdateAlg = button("Update", false, ( e ) -> resetWithNewAlgorithm());

		// Statistical Info
		protected JLabel labelInliersN = new JLabel();
		protected JLabel labelVisibleN = new JLabel();
		protected JLabel labelBundleN = new JLabel();
		protected JLabel labelTraveled = new JLabel();

		// Panel which contains all controls
		final JComboBox<String> comboApproach = combo(approach, "Mono Stereo", "Dual Track", "Quad View");
		final JPanel panelApproach = new JPanel(new BorderLayout());

		// Image Visualization Controls
		final JCheckBox checkInliers = checkbox("Inliers", showInliers, "Only draw inliers");
		final JCheckBox checkNew = checkbox("New", showNew, "Highlight new tracks");
		final JComboBox<String> comboTrackColors = combo(trackColors, "Depth", "Age");

		// Cloud Visualization Controls
		final JSpinner spinMaxDepth = spinner(maxDepth, 0.0, 100.0, 1.0);
		final JCheckBox checkCameras = checkbox("Cameras", showCameras, "Render camera locations");
		final JSpinner spinCameraN = spinner(showEveryCameraN, 1, 500, 1);
		final JCheckBox checkCloud = checkbox("Cloud", showCloud, "Show sparse point cloud");
		final JSpinner spinMaxTrackAge = spinner(maxTrackAge, 0, 999, 5);
		final JSpinner spinMinDuration = spinner(minTrackDuration, 1, 999, 1);
		final ControlPanelPointCloud cloudControl = new ControlPanelPointCloud(() -> cloudPanel.updateVisuals(true));

		// controls for different algorithms
		ControlPanelStereoDualTrackPnP controlDualTrack = new ControlPanelStereoDualTrackPnP(
				createConfigStereoDualPnP(), () -> bUpdateAlg.setEnabled(true));
		ControlPanelStereoMonoTrackPnP controlMonoTrack = new ControlPanelStereoMonoTrackPnP(
				createConfigStereoMonoPnP(), () -> bUpdateAlg.setEnabled(true));
		ControlPanelStereoQuadPnP controlQuad = new ControlPanelStereoQuadPnP(
				createConfigStereoQuadPnP(), () -> bUpdateAlg.setEnabled(true));

		public ControlPanel() {
			selectZoom = spinner(1.0, MIN_ZOOM, MAX_ZOOM, 1);

			cloudControl.setBorder(BorderFactory.createEmptyBorder()); // save screen real estate

			spinCameraN.setToolTipText("Show the camera location every N frames");

			var panelInfo = new StandardAlgConfigPanel();
			panelInfo.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), "Statistics"));
			panelInfo.addLabeled(labelInliersN, "Inliers Tracks", "Tracks that were inliers in this frame");
			panelInfo.addLabeled(labelVisibleN, "Visible Tracks", "Total number of active visible tracks");
			panelInfo.addLabeled(labelBundleN, "Bundle Tracks", "Features included in bundle adjustment");
			panelInfo.addLabeled(labelTraveled, "Distance", "Distance traveled in world units");

			var panelImage = new StandardAlgConfigPanel();
			panelImage.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), "Image"));
			panelImage.add(fillHorizontally(gridPanel(0, 2, 0, 2, checkInliers, checkNew)));
			panelImage.addLabeled(comboTrackColors, "Color", "How tracks are colored in the image");

			var panelCloud = new StandardAlgConfigPanel();
			panelCloud.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), "Cloud"));
			panelCloud.addLabeled(spinMaxDepth, "Max Depth", "Maximum distance relative to stereo baseline");
			panelCloud.addLabeled(spinMaxTrackAge, "Max Age", "Only draw tracks which were first seen than this value");
			panelCloud.addLabeled(spinMinDuration, "Min Duration", "Only draw tracks if they have been seen for this many frames");
			panelCloud.add(fillHorizontally(gridPanel(2, checkCameras, spinCameraN)));
			panelCloud.addAlignLeft(checkCloud);
			panelCloud.add(cloudControl);

			var panelVisuals = new JPanel();
			panelVisuals.setLayout(new BoxLayout(panelVisuals, BoxLayout.Y_AXIS));
			panelVisuals.add(fillHorizontally(panelInfo));
			panelVisuals.add(fillHorizontally(panelImage));
			panelVisuals.add(fillHorizontally(panelCloud));

			var panelTuning = new JPanel();
			panelTuning.setLayout(new BoxLayout(panelTuning, BoxLayout.Y_AXIS));
			panelTuning.add(comboApproach);
			panelTuning.add(panelApproach);
			addVerticalGlue(panelTuning);

			panelApproach.add(BorderLayout.CENTER, getControlVisOdom());

			var tabbedTopPane = new JTabbedPane() {
				@Override 			public Dimension getPreferredSize() {
					// This hack is needed so that it the scrollbar will only expand vertically
					Dimension d = super.getPreferredSize();
					return new Dimension(200, d.height);
				}
			};
			tabbedTopPane.addTab("Visuals", panelVisuals);
			tabbedTopPane.addTab("Configure", panelTuning);

			// put it in a scroll pane so that it is possible to see everything even in a small screen
			var scrollTopPane = new JScrollPane(tabbedTopPane,
					VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);

			addLabeled(videoFrameLabel, "Frame");
			addLabeled(processingTimeLabel, "Processing (ms)");
			addLabeled(imageSizeLabel, "Image");
			addLabeled(selectZoom, "Zoom");
			add(scrollTopPane);
			add(createHorizontalPanel(bUpdateAlg, bPause, bStep));
		}

		public void setInliersTracks( int count ) {labelInliersN.setText("" + count);}

		public void setVisibleTracks( int count ) {labelVisibleN.setText("" + count);}

		public void setBundleTracks( int count ) {labelBundleN.setText("" + count);}

		public void setDistanceTraveled( double distance ) {labelTraveled.setText(String.format("%.1f", distance));}

		public void setFrame( int frame ) {videoFrameLabel.setText("" + frame);}

		@Override public void controlChanged( Object source ) {
			if (source == selectZoom) {
				zoom = (Double)selectZoom.getValue();
				stereoPanel.setScale(zoom);
			} else if (source == bPause) {
				streamPaused = !streamPaused;
			} else if (source == bStep) {
				streamPaused = false;
				streamStepCounter = 1;
			} else if (source == checkInliers) {
				showInliers = checkInliers.isSelected();
				stereoPanel.repaint();
			} else if (source == checkNew) {
				showNew = checkNew.isSelected();
				stereoPanel.repaint();
			} else if (source == comboTrackColors) {
				trackColors = comboTrackColors.getSelectedIndex();
				stereoPanel.repaint();
			} else if (source == checkCloud) {
				showCloud = checkCloud.isSelected();
				cloudPanel.update();
			} else if (source == checkCameras) {
				showCameras = checkCameras.isSelected();
				spinCameraN.setEnabled(showCameras);
				cloudPanel.update();
			} else if (source == spinCameraN) {
				showEveryCameraN = ((Number)spinCameraN.getValue()).intValue();
				cloudPanel.update();
			} else if (source == spinMaxDepth) {
				maxDepth = ((Number)spinMaxDepth.getValue()).doubleValue();
				cloudPanel.update();
			} else if (source == spinMaxTrackAge) {
				maxTrackAge = ((Number)spinMaxTrackAge.getValue()).intValue();
				cloudPanel.update();
			} else if (source == spinMinDuration) {
				minTrackDuration = ((Number)spinMinDuration.getValue()).intValue();
				cloudPanel.update();
			} else if (source == comboApproach) {
				approach = comboApproach.getSelectedIndex();
				JComponent control = getControlVisOdom();
				panelApproach.removeAll();
				panelApproach.add(BorderLayout.CENTER, control);
				panelApproach.validate();
				panelApproach.repaint();
				bUpdateAlg.setEnabled(true);
			}
		}

		private JComponent getControlVisOdom() {
			return switch (approach) {
				case 0 -> controlMonoTrack;
				case 1 -> controlDualTrack;
				case 2 -> controlQuad;
				default -> throw new IllegalArgumentException("Unknown");
			};
		}
	}

	class StereoPanel extends JPanel {
		BufferedImage left, right;
		double scale = 1.0;
		BasicStroke strokeThin = new BasicStroke(3.0f);

		public void setScale( double scale ) {
			if (this.scale == scale)
				return;
			this.scale = scale;
			repaint();
		}

		@Override protected void paintComponent( Graphics g ) {
			super.paintComponent(g);

			final BufferedImage left = this.left;
			final BufferedImage right = this.right;
			if (left == null || right == null)
				return;

			final int lh = left.getHeight();
			final double scale = this.scale;

			Graphics2D g2 = BoofSwingUtil.antialiasing(g);

			// Draw the scaled images
			var tranLeft = new AffineTransform(scale, 0, 0, scale, 0, 0);
			g2.drawImage(left, tranLeft, null);
			var tranRight = new AffineTransform(scale, 0, 0, scale, 0, lh*scale);
			g2.drawImage(right, tranRight, null);

			final long latestFrameID = VisualizeStereoVisualOdometryApp.this.latestFrameID;

			// Draw point features
			synchronized (features) {
				if (visibleTracks.size == 0)
					return;

				// Adaptive colorize depths based on distribution in current frame
				var depths = new DogArray_F64();
				long maxAge = Long.MAX_VALUE;
				depths.reset();
				for (int i = 0; i < visibleTracks.size; i++) {
					FeatureInfo f = features.get(visibleTracks.get(i));
					maxAge = Math.min(maxAge, f.firstFrame);
					depths.add(f.depth);
				}
				maxAge = latestFrameID - maxAge;
				depths.sort();
				double depthScale = depths.getFraction(0.8);

				for (int i = 0; i < visibleTracks.size; i++) {
					FeatureInfo f = features.get(visibleTracks.get(i));

					// if showInliers is true then only draw if it's an inlier
					if (!(!controls.showInliers || f.inlier))
						continue;

					int color;
					if (controls.trackColors == 0) { // Colorized based on depth
						double r = f.depth/depthScale;
						if (r < 0) r = 0;
						else if (r > 1) r = 1;
						color = (255 << 16) | ((int)(255*r) << 8);
					} else { // Colorize based on age
						double fraction = (latestFrameID - f.firstFrame)/(double)maxAge;
						double fractionGreen = Math.max(0, 0.3 - fraction)/0.3;
						color = ((int)(255*fraction) << 16) | ((int)(255*fractionGreen) << 8) | 0x99;
					}

					VisualizeFeatures.drawPoint(g2, f.pixel.x*scale, f.pixel.y*scale, 4.0, new Color(color), false);

					// if requested, draw a circle around tracks spawned in this frame
					if (controls.showNew && f.firstFrame == (frame - 1)) {
						g2.setStroke(strokeThin);
						g2.setColor(Color.GREEN);
						VisualizeFeatures.drawCircle(g2, f.pixel.x*scale, f.pixel.y*scale, 5.0);
					}
				}
			}
		}
	}

	/** Set's the 3D camera view to be the origin */
	void setViewToHome() {
		BoofSwingUtil.checkGuiThread();
		followCamera = false;
		cloudPanel.gui.setCameraToWorld(new Se3_F64());
	}

	/** Set's the 3D camera view to be latest estimate */
	void setViewToLatest() {
		BoofSwingUtil.checkGuiThread();
		followCamera = false;
		if (egoMotion_cam_to_world.size <= 0) {
			return;
		}
		Se3_F64 last = egoMotion_cam_to_world.getTail();
		cloudPanel.gui.setCameraToWorld(last);
	}

	/** Set's the 3D camera view continuous be set to the latest */
	void setViewToFollow() {
		BoofSwingUtil.checkGuiThread();
		followCamera = !followCamera;
	}

	class PointCloudPanel extends JPanel {
		PointCloudViewer gui = VisualizeData.createPointCloudViewer();
		DogArray<Point3D_F64> vertexes = new DogArray<>(Point3D_F64::new);

		public PointCloudPanel() {
			super(new BorderLayout());

			gui.setDotSize(1);

			add(BorderLayout.CENTER, gui.getComponent());
		}

		public void configureViewer( double hfov ) {
			gui.setCameraHFov(hfov);
		}

		public void updateVisuals( boolean repaint ) {
			double d = stereoParameters.getBaseline();
			controls.cloudControl.configure(gui, d*10, d/2.0);
			if (repaint)
				repaint();
		}

		/**
		 * Updates using the latest set of features visible
		 */
		public void update() {
			BoofSwingUtil.checkGuiThread();
			updateVisuals(false);
			final double maxDepth = controls.maxDepth*stereoParameters.getBaseline()*10;
			final double r = stereoParameters.getBaseline()/2.0;
			final long frameID = alg.getFrameID();

			gui.clearPoints();
			if (controls.showCameras) {
				synchronized (features) {
					// if configured to do so, only render the more recent cameras
					int startIndex = 0;
					if (controls.maxTrackAge > 0)
						startIndex = Math.max(0, egoMotion_cam_to_world.size - controls.maxTrackAge);
					for (int i = startIndex; i < egoMotion_cam_to_world.size; i += controls.showEveryCameraN) {
						Se3_F64 cam_to_world = egoMotion_cam_to_world.get(i);

						// Represent the camera with a box
						vertexes.reset();
						vertexes.grow().setTo(-r, -r, 0);
						vertexes.grow().setTo(r, -r, 0);
						vertexes.grow().setTo(r, r, 0);
						vertexes.grow().setTo(-r, r, 0);

						for (int j = 0; j < vertexes.size; j++) {
							var p = vertexes.get(j);
							SePointOps_F64.transform(cam_to_world, p, p);
						}

						gui.addWireFrame(vertexes.toList(), true, 0xFF0000, 1);
					}
				}
			}

			if (controls.showCloud) {
				synchronized (features) {
					for (int i = 0; i < features.size; i++) {
						FeatureInfo f = features.get(i);
						if (!f.inlier)
							continue;
						if (controls.maxDepth > 0 && f.depth > maxDepth)
							continue;
						if (controls.maxTrackAge > 0 && frameID > f.firstFrame + controls.maxTrackAge)
							continue;
						if (f.lastFrame - f.firstFrame + 1 < controls.minTrackDuration)
							continue;
						gui.addPoint(f.world.x, f.world.y, f.world.z, f.rgb);
					}
				}
			}
			repaint();
		}
	}

	private static PathLabel createExample( String name ) {
		String path0 = UtilIO.pathExample("vo/" + name + "/left.mjpeg");
		String path1 = UtilIO.pathExample("vo/" + name + "/right.mjpeg");

		return new PathLabel(name, path0, path1);
	}

	public static void main( String[] args ) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(createExample("backyard"));
		examples.add(createExample("rockville"));
		examples.add(createExample("library"));

		SwingUtilities.invokeLater(() -> {
			var app = new VisualizeStereoVisualOdometryApp<>(examples, GrayU8.class);

			// Processing time takes a bit so don't open right away
			app.openExample(examples.get(0));
			app.displayImmediate("Stereo Visual Odometry");
		});
	}
}
