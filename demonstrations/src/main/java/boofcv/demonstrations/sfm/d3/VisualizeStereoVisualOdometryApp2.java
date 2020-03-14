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

package boofcv.demonstrations.sfm.d3;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.abst.sfm.d3.WrapVisOdomPixelDepthPnP;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.demonstrations.feature.disparity.ControlPanelDisparitySparse;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.factory.feature.disparity.ConfigDisparityBM;
import boofcv.factory.sfm.ConfigVisOdomDepthPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.dialogs.OpenImageSetDialog;
import boofcv.gui.feature.VisualizeFeatures;
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
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.*;
import static boofcv.io.image.ConvertBufferedImage.checkCopy;
import static boofcv.io.image.ConvertBufferedImage.convertFrom;

/**
 * @author Peter Abeles
 */
public class VisualizeStereoVisualOdometryApp2<T extends ImageGray<T>>
		extends DemonstrationBase
{
	// Main GUI elements for the app
	ControlPanel controls = new ControlPanel();
	StereoPanel stereoPanel = new StereoPanel();
	PointCloudPanel cloudPanel = new PointCloudPanel();

	StereoVisualOdometry<T> alg;
	T inputLeft, inputRight;

	//-------------- Control lock on features
	final FastQueue<FeatureInfo> features = new FastQueue<>(FeatureInfo::new);
	final GrowQueue_I32 visibleTracks = new GrowQueue_I32(); // index of tracks visible in current frame in 'features'
	final TLongIntMap trackId_to_arrayIdx = new TLongIntHashMap(); // track ID to array Index
	//-------------- END lock

	// number if stereo frames processed
	int frame = 0;
	// total distance traveled
	double traveled = 0;
	final Se3_F64 prev_to_world = new Se3_F64();

	public VisualizeStereoVisualOdometryApp2(List<PathLabel> examples,
											 Class<T> imageType ) {
		super(true, false, examples, ImageType.single(imageType),ImageType.single(imageType));
		allowVideos = true;
		allowImages = false; // videos only

		alg = createSelectedAlgorithm();
		inputLeft = alg.getImageType().createImage(1,1);
		inputRight = alg.getImageType().createImage(1,1);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,stereoPanel,cloudPanel);
		split.setDividerLocation(320);

		controls.setPreferredSize(new Dimension(200,0));

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, split);

		setPreferredSize(new Dimension(1200,540));
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

		ConfigPKlt kltConfig = controls.controlKlt.configKlt;
		ConfigGeneralDetector detConfig = controls.controlKlt.configDetector;
		StereoDisparitySparse<T> disparity = controls.controlDisparity.createAlgorithm(imageType);
		PointTracker<T> tracker = FactoryPointTracker.klt(kltConfig, detConfig,imageType,null);
		ConfigVisOdomDepthPnP configPnpDepth = controls.controlPnpDepth.config;
		return FactoryVisualOdometry.stereoDepthPnP(configPnpDepth,disparity,tracker,imageType);
	}

	private static ConfigPKlt createConfigKlt() {
		var config = new ConfigPKlt();
		config.toleranceFB = 3;
		config.pruneClose = true;
		config.templateRadius = 3;
		config.pyramidLevels = ConfigDiscreteLevels.levels(4);
		return config;
	}

	private static ConfigGeneralDetector createConfigKltDetect() {
		var config = new ConfigGeneralDetector();
		config.maxFeatures = -1;
		config.radius = 4;
		config.threshold = 0.1f;
		return config;
	}

	private static ConfigDisparityBM createConfigDisparity() {
		var config = new ConfigDisparityBM();
		config.disparityMin = 2;
		config.disparityRange = 150;
		config.regionRadiusX = 3;
		config.regionRadiusY = 3;
		config.maxPerPixelError = 30;
		config.texture = 0.0;
		config.subpixel = true;

		return config;
	}

	@Override
	protected void openFileMenuBar() {
		// TODO modify to select two videos
		String[] files = BoofSwingUtil.openImageSetChooser(window, OpenImageSetDialog.Mode.EXACTLY,2);
		if( files == null )
			return;
		BoofSwingUtil.invokeNowOrLater(()->openImageSet(false,files));
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		File parent = new File(inputFileSet[0]).getParentFile();
		File pathCalibration = new File(parent,"stereo.yaml");
		StereoParameters stereo = CalibrationIO.load(pathCalibration);
		frame = 0;
		traveled = 0.0;
		prev_to_world.reset();
		alg.reset();
		alg.setCalibration(stereo);

		synchronized (features) {
			features.reset();
			trackId_to_arrayIdx.clear();
			visibleTracks.reset();
		}

		double hfov = PerspectiveOps.computeHFov(stereo.left);

		SwingUtilities.invokeLater(()-> {
			cloudPanel.configureViewer(hfov);
			cloudPanel.gui.setTranslationStep(stereo.getBaseline());
		});
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {
		switch( sourceID ) {
			case 0:
				stereoPanel.left = checkCopy(buffered,stereoPanel.left);
				convertFrom(buffered,true,inputLeft); break;
			case 1:
				stereoPanel.right = checkCopy(buffered,stereoPanel.right);
				convertFrom(buffered,true,inputRight);break;
			default: throw new RuntimeException("BUG");
		}

		if( sourceID == 0 )
			return;

		// TODO visualize success and faults
		long time0 = System.nanoTime();
		boolean success = alg.process(inputLeft,inputRight);
		long time1 = System.nanoTime();

		Se3_F64 camera_to_world = alg.getCameraToWorld();
		Se3_F64 world_to_camera = camera_to_world.invert(null);

		if( success ) {
			// Sum up the total distance traveled. This will be approximate since it optimizes past history
			traveled += prev_to_world.concat(world_to_camera,null).T.norm();
			prev_to_world.set(camera_to_world);
		}

		if( alg instanceof AccessPointTracks3D ) {
			extractFeatures(world_to_camera,(AccessPointTracks3D) alg, buffered);
		}

		// Number of tracks being optimized by bundle adjustment
		final int bundleTracks;
		if( alg instanceof WrapVisOdomPixelDepthPnP) {
			bundleTracks = ((WrapVisOdomPixelDepthPnP)alg).getAlgorithm().getBundle().tracks.size;
		} else {
			bundleTracks = -1;
		}

		// Update the visualization
		int frame = this.frame;
		SwingUtilities.invokeLater(()->{
			controls.setFrame(frame);
			controls.setProcessingTimeMS((time1-time0)*1e-6);
			controls.setImageSize(buffered.getWidth(),buffered.getHeight());
			controls.setBundleTracks(bundleTracks);
			controls.setDistanceTraveled(traveled);
			stereoPanel.repaint();
			cloudPanel.update();
		});
		this.frame++;
	}

	/**
	 * Extracts and copies information about the features currently visible
	 */
	public void extractFeatures(Se3_F64 world_to_camera, AccessPointTracks3D access , BufferedImage image ) {
		synchronized (features) {
			visibleTracks.reset();
			var camera3D = new Point3D_F64();
			final int N = access.getTotalTracks();
			int totalInliers = 0;
			for (int i = 0; i < N; i++) {
				long id = access.getTrackId(i);
				if( id == -1 )
					throw new RuntimeException("BUG! Got id = -1");
				int arrayIndex;
				FeatureInfo f;
				if( access.isTrackNew(i) ) {
					arrayIndex = features.size;
					f = features.grow();
					f.id = id;
					f.firstFrame = frame;
					if( trackId_to_arrayIdx.containsKey(id) )
						System.err.println("BUG! Already contains key of new track: "+id);
					trackId_to_arrayIdx.put(id,arrayIndex);
					access.getTrackPixel(i,f.pixel);
					// Grab the pixel's color for visualization
					int x = (int)f.pixel.x;
					int y = (int)f.pixel.y;
					if(BoofMiscOps.isInside(image.getWidth(), image.getHeight(),x,y)) {
						f.rgb = image.getRGB(x,y);
					} else {
						// default to the color red if it's outside the image. Pure red is unusual and should make
						// it easy to see bugs
						f.rgb = 0xFF0000;
					}
				} else {
					arrayIndex = trackId_to_arrayIdx.get(id);
					f = features.get(arrayIndex);
					access.getTrackPixel(i,f.pixel);
				}
				visibleTracks.add(arrayIndex);

				access.getTrackWorld3D(i,f.world);
				SePointOps_F64.transform(world_to_camera,f.world,camera3D);
				f.depth = camera3D.z;
				f.inlier = access.isTrackInlier(i);
				if( f.inlier )
					totalInliers++;
			}

			int _totalInliers = totalInliers;
			BoofSwingUtil.invokeNowOrLater(()->{
				controls.setInliersTracks(_totalInliers);
				controls.setVisibleTracks(N);
			});
		}
	}

	// Information copied from VO algorithm. This increased how much the processing thread is decoupled from
	// the GUI thread leading to a better user experience
	static class FeatureInfo
	{
		// Most recently visible pixel coordinate
		public final Point2D_F64 pixel = new Point2D_F64();
		// 3D Location of feature in world coordinates
		public final Point3D_F64 world = new Point3D_F64();
		public boolean inlier; // if it is an inlier that was used to estimate VO
		public double depth; // depth in current camera frame
		public long id; // unique tracker id for the feature
		public int rgb; // color in first frame
		public int firstFrame; // first frame the track was seen in
	}

	class ControlPanel extends DetectBlackShapePanel {

		// flags that toggle what's visualized in stereo panel view
		boolean showInliers = false;
		boolean showNew = false;

		double maxDepth=0; // Maximum depth a feature is from the camera when last viewed
		boolean showCameras=false; // show camera locations in 3D view
		boolean showCloud=true; // Show point cloud created from feature tracks
		// the maximum number of frames in the past the track was first spawned and stuff be visible. 0 = infinite
		int maxTrackAge=0;

		final JLabel videoFrameLabel = new JLabel();
		final JButton bPause = button("Pause",true);
		final JButton bStep = button("Step",true);
		final JButton bUpdateAlg = button("Update",false,(e)->resetWithNewAlgorithm());

		// Statistical Info
		protected JLabel labelInliersN = new JLabel();
		protected JLabel labelVisibleN = new JLabel();
		protected JLabel labelBundleN = new JLabel();
		protected JLabel labelTraveled = new JLabel();

		// Panel which contains all controls
		protected JPanel panelAlgControls = new JPanel(new BorderLayout());

		// Stereo Visualization Controls
		final JCheckBox checkInliers = checkbox("Inliers",showInliers,"Only draw inliers");
		final JCheckBox checkNew = checkbox("New",showNew,"Highlight new tracks");

		// Cloud Visualization Controls
		final JSpinner spinMaxDepth = spinner(maxDepth,0.0,100.0,1.0);
		final JCheckBox checkCameras = checkbox("Cameras",showCameras,"Render camera locations");
		final JCheckBox checkCloud = checkbox("Cloud",showCloud,"Show sparse point cloud");
		final JSpinner spinMaxTrackAge = spinner(maxTrackAge,0,999,5);

		// controls for different algorithms
		ControlPanelVisOdomDepthPnP controlPnpDepth = new ControlPanelVisOdomDepthPnP(()->bUpdateAlg.setEnabled(true));
		ControlPanelPointTrackerKlt controlKlt = new ControlPanelPointTrackerKlt(
				()->bUpdateAlg.setEnabled(true),createConfigKltDetect(),createConfigKlt());
		ControlPanelDisparitySparse controlDisparity = new ControlPanelDisparitySparse(createConfigDisparity(),()->bUpdateAlg.setEnabled(true));

		public ControlPanel() {
			selectZoom = spinner(1.0,MIN_ZOOM,MAX_ZOOM,1);

			var panelInfo = new StandardAlgConfigPanel();
			panelInfo.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Statistics"));
			panelInfo.addLabeled(labelInliersN,"Inliers","Tracks that were inliers in this frame");
			panelInfo.addLabeled(labelVisibleN,"Visible", "Total number of active visible tracks");
			panelInfo.addLabeled(labelBundleN,"Bundle","Features included in bundle adjustment");
			panelInfo.addLabeled(labelTraveled,"Traveled","Distance traveled in world units");

			JPanel panelStereo = gridPanel(0,2,0,2,checkInliers, checkNew);
			panelStereo.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Stereo"));

			var panelCloud = new StandardAlgConfigPanel();
			panelCloud.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Cloud"));
			panelCloud.addLabeled(spinMaxDepth,"Max Depth","Maximum distance a feature can be in the last frame it was seen");
			panelCloud.addLabeled(spinMaxTrackAge,"Max Age","Only draw tracks which were first seen than this value");
			panelCloud.addAlignLeft(checkCameras);
			panelCloud.addAlignLeft(checkCloud);

			var panelVisuals = new JPanel();
			panelVisuals.setLayout(new BoxLayout(panelVisuals,BoxLayout.Y_AXIS));
			panelVisuals.add(fillHorizontally(panelInfo));
			panelVisuals.add(fillHorizontally(panelStereo));
			panelVisuals.add(fillHorizontally(panelCloud));

			var tuningTabs = new JTabbedPane();
			tuningTabs.addTab("VO",panelAlgControls);
			tuningTabs.addTab("Tracker",controlKlt);
			tuningTabs.addTab("Stereo",controlDisparity);

			var panelTuning = new JPanel();
			panelTuning.setLayout(new BoxLayout(panelTuning,BoxLayout.Y_AXIS));
			panelTuning.add(tuningTabs);
			addVerticalGlue(panelTuning);
			addAlignCenter(bUpdateAlg,panelTuning);

			panelAlgControls.add(BorderLayout.CENTER, controlPnpDepth);

			var tabbedPane = new JTabbedPane();
			tabbedPane.addTab("Visuals",panelVisuals);
			tabbedPane.addTab("Tuning",panelTuning);

			addLabeled(videoFrameLabel,"Frame");
			addLabeled(processingTimeLabel,"Processing (ms)");
			addLabeled(imageSizeLabel,"Image");
			addLabeled(selectZoom,"Zoom");
			add(tabbedPane);
			addVerticalGlue();
			add(fillHorizontally(gridPanel(2,bPause,bStep)));
		}

		public void setInliersTracks( int count ) {
			labelInliersN.setText(""+count);
		}

		public void setVisibleTracks( int count ) {
			labelVisibleN.setText(""+count);
		}
		public void setBundleTracks( int count ) {
			labelBundleN.setText(""+count);
		}
		public void setDistanceTraveled( double distance ) {
			labelTraveled.setText(String.format("%.1f",distance));
		}

		public void setFrame( int frame ) {
			videoFrameLabel.setText(""+frame);
		}

		@Override
		public void controlChanged(Object source) {
			if( source == selectZoom ) {
				zoom = (Double)selectZoom.getValue();
				stereoPanel.setScale(zoom);
			} else if( source == bPause ) {
				boolean paused = !streamPaused;
				streamPaused = paused;
				bPause.setText(paused?"Resume":"Paused");
			} else if( source == bStep ) {
				if( streamPaused )
					streamPaused = false;
				streamStepCounter = 1;
				bPause.setText("Resume");
			} else if( source == checkInliers ) {
				showInliers = checkInliers.isSelected();
				stereoPanel.repaint();
			} else if( source == checkNew ) {
				showNew = checkNew.isSelected();
				stereoPanel.repaint();
			} else if( source == checkCloud ) {
				showCloud = checkCloud.isSelected();
				cloudPanel.update();
			} else if( source == spinMaxDepth ) {
				maxDepth = ((Number)spinMaxDepth.getValue()).doubleValue();
				cloudPanel.update();
			} else if( source == spinMaxTrackAge ) {
				maxTrackAge = ((Number)spinMaxTrackAge.getValue()).intValue();
				cloudPanel.update();
			}
		}
	}

	class StereoPanel extends JPanel {
		BufferedImage left,right;
		double scale = 1.0;
		BasicStroke strokeThin = new BasicStroke(2.0f);

		public void setScale( double scale ) {
			if( this.scale == scale )
				return;
			this.scale = scale;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			final BufferedImage left = this.left;
			final BufferedImage right = this.right;
			if( left == null || right == null )
				return;

			final int lh = left.getHeight();
			final double scale = this.scale;

			Graphics2D g2 = (Graphics2D)g;

			// Draw the scaled images
			AffineTransform tranLeft = new AffineTransform(scale,0,0,scale,0,0);
			g2.drawImage(left,tranLeft,null);
			AffineTransform tranRight = new AffineTransform(scale,0,0,scale,0,lh*scale);
			g2.drawImage(right,tranRight,null);

			// Draw point features
			synchronized (features) {
				if( visibleTracks.size == 0 )
					return;

				// Adaptive colorize depths based on distribution in current frame
				GrowQueue_F64 depths = new GrowQueue_F64();
				depths.reset();
				for (int i = 0; i < visibleTracks.size; i++) {
					depths.add( features.get(visibleTracks.get(i)).depth );
				}
				depths.sort();
				double depthScale = depths.getFraction(0.8);

				for (int i = 0; i < visibleTracks.size; i++) {
					FeatureInfo f = features.get(visibleTracks.get(i));

					// if showInliers is true then only draw if it's an inlier
					if( !(!controls.showInliers || f.inlier) )
						continue;

					double r = f.depth / depthScale;
					if (r < 0) r = 0;
					else if (r > 1) r = 1;

					int color = (255 << 16) | ((int) (255 * r) << 8);
					VisualizeFeatures.drawPoint(g2, f.pixel.x * scale, f.pixel.y * scale, 4.0, new Color(color), false);

					// if requested, draw a circle around tracks spawned in this frame
					if( controls.showNew && f.firstFrame == (frame-1) ) {
						g2.setStroke(strokeThin);
						g2.setColor(Color.CYAN);
						VisualizeFeatures.drawCircle(g2,f.pixel.x*scale, f.pixel.y*scale,5.0);
					}
				}
			}
		}
	}

	class PointCloudPanel extends JPanel {
		PointCloudViewer gui = VisualizeData.createPointCloudViewer();

		public PointCloudPanel() {
			super(new BorderLayout());

			gui.setDotSize(1);

			add(BorderLayout.CENTER,gui.getComponent());
		}

		public void configureViewer( double hfov ) {
			gui.setCameraHFov(hfov);
		}

		/**
		 * Updates using the latest set of features visible
		 */
		public void update()
		{
			BoofSwingUtil.checkGuiThread();
			gui.clearPoints();
			if( controls.showCloud ) {
				final long frameID = alg.getFrameID();
				synchronized (features) {
					for (int i = 0; i < features.size; i++) {
						FeatureInfo f = features.get(i);
						if (!f.inlier)
							continue;
						if (controls.maxDepth > 0 && f.depth > controls.maxDepth)
							continue;
						if(controls.maxTrackAge > 0 && frameID > f.firstFrame+controls.maxTrackAge )
							continue;
						gui.addPoint(f.world.x, f.world.y, f.world.z, f.rgb);
					}
				}
			}
			repaint();
		}
	}

	private static PathLabel createExample( String name ) {
		String path0 = UtilIO.pathExample("vo/"+name+"/left.mjpeg");
		String path1 = UtilIO.pathExample("vo/"+name+"/right.mjpeg");

		return new PathLabel(name,path0,path1);
	}

	public static void main(String[] args) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(createExample("backyard"));
		examples.add(createExample("rockville"));
		examples.add(createExample("library"));

		SwingUtilities.invokeLater(()->{
			var app = new VisualizeStereoVisualOdometryApp2<>(examples, GrayU8.class);

			// Processing time takes a bit so don't open right away
			app.openExample(examples.get(0));
			app.displayImmediate("Stereo Visual Odometry");
		});
	}
}
