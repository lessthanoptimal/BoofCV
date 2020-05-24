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

package boofcv.demonstrations.sfm.multiview;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.cloud.DisparityToColorPointCloud;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.sfm.structure.ThreeViewEstimateMetricScene;
import boofcv.core.image.ConvertImage;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.d3.UtilDisparitySwing;
import boofcv.gui.dialogs.OpenImageSetDialog;
import boofcv.gui.feature.AssociatedTriplePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.image.*;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes a stereo point cloud using three uncalibrated images. Visualizes different pre-processing steps and
 * lets the user change a few parameters.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DemoThreeViewStereoApp extends DemonstrationBase {

	JPanel gui = new JPanel();
	AssociatedTriplePanel guiAssoc = new AssociatedTriplePanel();
	ImagePanel guiImage = new ImagePanel();
	ImagePanel guiDisparity = new ImagePanel();
	RectifiedPairPanel rectifiedPanel = new RectifiedPairPanel(true);
	PointCloudViewer guiPointCloud = VisualizeData.createPointCloudViewer();

	DemoThreeViewControls controls = new DemoThreeViewControls(this);

	DetectDescribePoint<GrayU8, TupleDesc> detDesc;
	AssociateDescription<TupleDesc> associate;

	AssociateThreeByPairs<TupleDesc> associateThree;
	FastQueue<AssociatedTriple> associated = new FastQueue<>(AssociatedTriple::new);

	ThreeViewEstimateMetricScene structureEstimator = new ThreeViewEstimateMetricScene();

	FastQueue<Point2D_F64>[] locations = new FastQueue[3];
	FastQueue<TupleDesc>[] features = new FastQueue[3];
	GrowQueue_I32[] featureSets = new GrowQueue_I32[3];
	ImageDimension[] dimensions = new ImageDimension[3];

	BufferedImage buff[] = new BufferedImage[3];

	// Rectify and remove lens distortion for stereo processing
	DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);
	DMatrixRMaj rectifiedR = new DMatrixRMaj(3, 3);

	// Results from bundle adjustment
	CameraPinholeBrown intrinsic01;
	CameraPinholeBrown intrinsic02;
	Se3_F64 leftToRight;

	// Saved disparity image for saving to disk
	ImageGray disparity;

	// Visualized Disparity
	BufferedImage visualDisparity = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
	BufferedImage visualRect1     = new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB);
	BufferedImage visualRect2     = new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB);

	Planar<GrayU8> rectColor1 = new Planar<>(GrayU8.class, 1,1, 3);
	Planar<GrayU8> rectColor2 = new Planar<>(GrayU8.class, 1,1, 3);
	GrayU8 rectMask = new GrayU8(1,1);

	final Object lockProcessing = new Object();
	boolean processing = false;
	boolean exceptionOccurred = false; // if true that means a fatal error occured while processing
	boolean hasAllImages = false;
	// change panels automatically while computing. Only do this the first time an image is opened
	// after that you might be tweaking a setting and don't want the view to change
	boolean automaticChangeViews = false;

	public DemoThreeViewStereoApp(List<PathLabel> examples) {
		super(true, false, examples, ImageType.single(GrayU8.class));

		// remove some unused items from the menu bar. This app is an exception
		JMenu fileMenu = menuBar.getMenu(0);
		fileMenu.remove(1);

		for (int i = 0; i < 3; i++) {
			locations[i] = new FastQueue<>(Point2D_F64::new);
			dimensions[i] = new ImageDimension();
			featureSets[i] = new GrowQueue_I32();
		}

		rectifiedPanel.setImages(visualRect1,visualRect2);
		guiDisparity.setImage(visualDisparity);

		gui.setLayout(new BorderLayout());
		updateVisibleGui();

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, gui);

		setPreferredSize(new Dimension(900,700));
	}

	@Override
	protected String selectRecentFileName(List<File> filePaths) {
		File f = filePaths.get(0);
		File path = new File(f.getParentFile().getName(),f.getName());
		return path.getPath();
	}

	@Override
	protected void openFileMenuBar() {
		String[] files = BoofSwingUtil.openImageSetChooser(window, OpenImageSetDialog.Mode.EXACTLY,3);
		if( files == null )
			return;
		BoofSwingUtil.invokeNowOrLater(()->openImageSet(false,files));
	}

	@Override
	protected void customAddToFileMenu(JMenu menuFile) {
		menuFile.addSeparator();

		JMenuItem itemSaveCalibration = new JMenuItem("Save Calibration");
		itemSaveCalibration.addActionListener(e -> saveCalibration());
		menuFile.add(itemSaveCalibration);

		JMenuItem itemSaveRectified = new JMenuItem("Save Rectified");
		itemSaveRectified.addActionListener(e -> saveRectified());
		menuFile.add(itemSaveRectified);

		JMenuItem itemSaveDisparity = new JMenuItem("Save Disparity");
		itemSaveDisparity.addActionListener(e -> saveDisparity());
		menuFile.add(itemSaveDisparity);

		JMenuItem itemSaveCloud = new JMenuItem("Save Point Cloud");
		itemSaveCloud.addActionListener(e -> savePointCloud());
		menuFile.add(itemSaveCloud);
	}

	private void saveCalibration() {
		CameraPinholeBrown intrinsic01 = this.intrinsic01;
		CameraPinholeBrown intrinsic02 = this.intrinsic02;
		Se3_F64 leftToRight = this.leftToRight;

		if( intrinsic01 == null || intrinsic02 == null || leftToRight == null ) {
			JOptionPane.showMessageDialog(this, "No calibration to save");
			return;
		}

		String home = BoofSwingUtil.getDefaultPath(this, BoofSwingUtil.KEY_PREVIOUS_DIRECTORY);
		File f = BoofSwingUtil.fileChooser(null,this,false,home,null, BoofSwingUtil.FileTypes.YAML);
		if( f == null )
			return;
		BoofSwingUtil.saveDefaultPath(this, BoofSwingUtil.KEY_PREVIOUS_DIRECTORY,f);

		f = BoofSwingUtil.ensureSuffix(f,".yaml");

		StereoParameters stereo = new StereoParameters();
		stereo.left = intrinsic01;
		stereo.right = intrinsic02;
		stereo.rightToLeft = leftToRight.invert(null);

		CalibrationIO.save(stereo,f.getAbsolutePath());
	}

	private void saveRectified() {
		if( rectColor1.width == 1 ) {
			JOptionPane.showMessageDialog(this, "Not yet rectified");
			return;
		}

		String currentPath = BoofSwingUtil.getDefaultPath(this, BoofSwingUtil.KEY_PREVIOUS_DIRECTORY);
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Rectified Images");
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setCurrentDirectory(new File(currentPath));
		if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File parent = fileChooser.getSelectedFile();
		BoofSwingUtil.saveDefaultPath(this, BoofSwingUtil.KEY_PREVIOUS_DIRECTORY,parent);

		UtilImageIO.saveImage(visualRect1,new File(parent,"rectified_left.png").getAbsolutePath());
		UtilImageIO.saveImage(visualRect2,new File(parent,"rectified_right.png").getAbsolutePath());
	}

	private void saveDisparity() {
		ImageGray disparity = this.disparity;
		if( disparity == null )
			return;
		BoofSwingUtil.saveDisparityDialog(this, BoofSwingUtil.KEY_PREVIOUS_DIRECTORY,disparity);
	}

	private void savePointCloud() {
		BoofSwingUtil.savePointCloudDialog(this, BoofSwingUtil.KEY_PREVIOUS_DIRECTORY,guiPointCloud);
	}

	void updateVisibleGui() {
		if( gui.getComponentCount() > 0 )
			gui.remove(0);

		switch( controls.view ) {
			case 0: gui.add(BorderLayout.CENTER,guiImage); break;
			case 1: gui.add(BorderLayout.CENTER,guiAssoc); break;
			case 2: gui.add(BorderLayout.CENTER,rectifiedPanel); break;
			case 3: gui.add(BorderLayout.CENTER,guiDisparity); break;
			case 4: gui.add(BorderLayout.CENTER,guiPointCloud.getComponent()); break;
			default: gui.add(BorderLayout.CENTER,guiImage); break;
		}

		gui.validate();
		gui.repaint();
		// this way if you press a key it manipulates the view the user just opened
		gui.getComponent(0).requestFocus();
	}

	@Override
	public void reprocessInput() {
		controls.scaleChanged = true;
		controls.assocChanged = true;
		controls.stereoChanged = true;
		super.reprocessInput();
	}

	/**
	 * Prevent the user from tring to process more than one image at once
	 */
	@Override
	public void openImageSet(boolean reopen, String ...files ) {
		// Make sure it recomputes everything when a new image set is opened
		if(!reopen) {
			automaticChangeViews = true;
			controls.scaleChanged = true;
			controls.assocChanged = true;
			controls.stereoChanged = true;
		}
		synchronized (lockProcessing) {
			if( processing ) {
				JOptionPane.showMessageDialog(this, "Still processing");
				return;
			}
		}
		// disable the menu until it finish processing the images
		setMenuBarEnabled(false);
		super.openImageSet(reopen,files);
	}

	void handleComputePressed() {
		if( isProcessing() ) {
			System.err.println("Not finished with previous computation");
			return;
		}

		// If the scale changes then the images need to be loaded again because they are
		// scaled upon input
		// if features changed the input doesn't need to be reloaded but the features need to be computed again
		// this could be done slightly more efficiently by skipping loading
		if( controls.scaleChanged || controls.featuresChanged ) {
			reprocessInput();
		} else {
			exceptionOccurred = false;
			boolean skipAssociate = false;
			boolean skipSparseStructure = false;
			if( !controls.assocChanged ) {
				skipAssociate = true;
				if( controls.stereoChanged ) {
					skipSparseStructure = true;
				}
			}
			controls.scaleChanged = false;
			controls.assocChanged = false;
			controls.stereoChanged = false;
//			System.out.println("computePressed associate "+skipAssociate+" sparse "+skipSparseStructure);

			boolean _assoc = skipAssociate;
			boolean _struct = skipSparseStructure;

			new Thread(()-> safeProcessImages(_assoc, _struct)).start();
		}
	}

	@Override
	protected void handleInputFailure(int source, String error) {
		JOptionPane.showMessageDialog(this, error);
		System.err.println(error);
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage bufferedIn, ImageBase input) {
		synchronized (lockProcessing) {
			hasAllImages = false;
		}

		BufferedImage buffered = scaleBuffered(bufferedIn);

		if( sourceID == 0 ) {
			exceptionOccurred = false;
			BoofSwingUtil.invokeNowOrLater(()->{
				guiImage.setImage(buffered);
				if( automaticChangeViews )
					controls.setViews(0);
			});
		} else if( exceptionOccurred ) {
			// abort if something went wrong on a prior image
			return;
		}

		try {
			// ok this is ugly.... find a way to not convert the image twice
			ConvertBufferedImage.convertFrom(buffered, input, true);
			System.out.println("Processing image " + sourceID + "  shape " + input.width + " " + input.height);
			System.out.println("  " + inputFilePath);
			dimensions[sourceID].set(input.width, input.height);
			buff[sourceID] = buffered;

			// assume the image center is the principle point
			double cx = input.width / 2;
			double cy = input.height / 2;

			// detect features
			if (controls.featuresChanged) {
				controls.featuresChanged = false;
				declareFeatureMatching();
			}
			detDesc.detect((GrayU8) input);
			locations[sourceID].resize(detDesc.getNumberOfFeatures());
			features[sourceID].resize(detDesc.getNumberOfFeatures());
			featureSets[sourceID].resize(detDesc.getNumberOfFeatures());

			// save results
			for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
				Point2D_F64 pixel = detDesc.getLocation(i);
				locations[sourceID].data[i].set(pixel.x - cx, pixel.y - cy);
				features[sourceID].data[i].setTo(detDesc.getDescription(i));
				featureSets[sourceID].data[i] = detDesc.getSet(i);
			}
			System.out.println("   found features " + features[sourceID].size);
		} catch( RuntimeException e ) {
			// Mark the problem, log the error, notify the user
			exceptionOccurred = true;
			e.printStackTrace();
			BoofSwingUtil.invokeNowOrLater(()-> {
				controls.clearText();
				controls.addText("Failed computing features!\n" + e.getMessage() + "\n");
			});
			BoofSwingUtil.warningDialog(this,e);
			return;
		}

		if( sourceID < 2 && !exceptionOccurred )
			return;

		synchronized (lockProcessing) {
			hasAllImages = true;
		}

		controls.scaleChanged = false;
		controls.assocChanged = false;
		controls.stereoChanged = false;
		safeProcessImages(false,false);
	}

	private void declareFeatureMatching() {
		detDesc = (DetectDescribePoint)controls.controlsDetDescAssoc.createDetectDescribe(GrayU8.class);
		associate = controls.controlsDetDescAssoc.createAssociate(detDesc);
		associate = FactoryAssociation.ensureUnique(associate);
		associateThree = new AssociateThreeByPairs<>(associate,detDesc.getDescriptionType());
		for (int i = 0; i < 3; i++) {
			features[i] = UtilFeature.createQueue(detDesc,100);
		}
	}

	private boolean isProcessing() {
		synchronized (lockProcessing) {
			return processing;
		}
	}

	/**
	 * Scale buffered image so that it meets the image size restrictions
	 */
	private BufferedImage scaleBuffered( BufferedImage input ) {
		int m = Math.max(input.getWidth(),input.getHeight());
		if( m <= controls.maxImageSize )
			return input;
		else {
			double scale = controls.maxImageSize/(double)m;
			int w = (int)(scale*input.getWidth()+0.5);
			int h = (int)(scale*input.getHeight()+0.5);

			// Use BoofCV to down sample since Graphics2D introduced too many aliasing artifacts
			int numBands = ConvertBufferedImage.numChannels(input);

			Planar<GrayU8> a = new Planar<>(GrayU8.class,input.getWidth(),input.getHeight(),numBands);
			Planar<GrayU8> b = new Planar<>(GrayU8.class,w,h,numBands);
			ConvertBufferedImage.convertFrom(input,a,true);
			AverageDownSampleOps.down(a,b);
			BufferedImage output = new BufferedImage(w,h,input.getType());
			ConvertBufferedImage.convertTo(b,output,true);
			return output;
		}
	}

	private void safeProcessImages( boolean skipAssociate , boolean skipSparseStructure ) {
		// bad stuff happens if processing is called twice at once
		synchronized (lockProcessing) {
			if( processing )
				throw new RuntimeException("Called processing while processing!");
			if( !hasAllImages )
				throw new RuntimeException("Called when not ready");
			processing = true;
		}

		// prevent user from opening another image at the same time
		SwingUtilities.invokeLater(()->setMenuBarEnabled(false));

		try {
			processImages(skipAssociate,skipSparseStructure);
		} catch (RuntimeException e ) {
			e.printStackTrace();
			controls.addText("Failed! "+e.getMessage());
			BoofSwingUtil.warningDialog(this,e);
		} finally {
			SwingUtilities.invokeLater(()->setMenuBarEnabled(true));

			synchronized (lockProcessing) {
				processing = false;
			}
		}
	}

	private void processImages( boolean skipAssociate , boolean skipStructure ) {
		boolean _automaticChangeViews = this.automaticChangeViews;
		this.automaticChangeViews = false;

		int width = buff[0].getWidth();
		int height = buff[0].getHeight();

		SwingUtilities.invokeLater(()->{
			controls.disableComputeButton();
			controls.clearText();
			controls.addText(width+" x "+height+"\n");
		});

		long time0 = System.currentTimeMillis();

		double cx = width/2;
		double cy = height/2;

		if( !skipAssociate ) {
			System.out.println("Associating three views");
			associateThree.initialize(detDesc.getNumberOfSets());
			associateThree.setFeaturesA(features[0], featureSets[0]);
			associateThree.setFeaturesB(features[1], featureSets[1]);
			associateThree.setFeaturesC(features[2], featureSets[2]);
			associateThree.associate();

			FastQueue<AssociatedTripleIndex> associatedIdx = associateThree.getMatches();
			associated.reset();
			for (int i = 0; i < associatedIdx.size; i++) {
				AssociatedTripleIndex p = associatedIdx.get(i);
				associated.grow().set(locations[0].get(p.a), locations[1].get(p.b), locations[2].get(p.c));
			}

			// Show work in progress and items are computed
			BoofSwingUtil.invokeNowOrLater(() -> {
				for (int i = 0; i < 3; i++) {
					controls.addText(String.format("Feats[%d] %d\n",i,features[i].size));
				}
				controls.addText("Associated "+associated.size+"\n");
				guiAssoc.setPixelOffset(cx, cy);
				guiAssoc.setImages(buff[0], buff[1], buff[2]);
				guiAssoc.setAssociation(associated.toList());
				if( _automaticChangeViews )
					controls.setViews(1);
			});
		} else {
			SwingUtilities.invokeLater(()-> controls.addText("Skipping Associate\n"));
		}

		if( !skipStructure ) {
			structureEstimator.configRansac.inlierThreshold = controls.inliers;
			structureEstimator.pruneFraction = (100-controls.prune)/100.0;
			if( controls.autoFocal ) {
				structureEstimator.manualFocalLength = -1;
			} else {
				structureEstimator.manualFocalLength = controls.focal;
			}

			//structureEstimator.setVerbose(System.out,0);
			System.out.println("Computing 3D structure. triplets " + associated.size);
			if (!structureEstimator.process(associated.toList(), width, height)) {
				SwingUtilities.invokeLater(()-> controls.addText("SBA Failed!\n"));
				return;
			}

			SwingUtilities.invokeLater(()->{
				int n = structureEstimator.ransac.getMatchSet().size();
				double score = structureEstimator.bundleAdjustment.getFitScore();
				int numObs = structureEstimator.observations.getObservationCount();
				int numPoints = structureEstimator.structure.points.size;
				controls.addText(String.format("Tri Feats %d\n",n));
				for (int i = 0; i < 3; i++) {
					BundlePinholeSimplified c = structureEstimator.structure.cameras.get(i).getModel();
					controls.addText(String.format("cam[%d] f=%.1f\n",i,c.f));
					controls.addText(String.format("   k1=%.2f k2=%.2f\n",c.k1,c.k2));
				}
				controls.addText(String.format("SBA Obs %4d Pts %d\n",numObs,numPoints));
				controls.addText(String.format("SBA fit score %.3f\n",score));
			});
		} else {
			SwingUtilities.invokeLater(() -> controls.addText("Skipping Structure\n"));
		}

		// Pick the two best views to compute stereo from
		int[] selected = selectBestPair(structureEstimator.structure);

		if ( !computeStereoCloud(selected[0],selected[1], cx, cy,skipStructure,_automaticChangeViews) )
			return;

		long time1 = System.currentTimeMillis();
		SwingUtilities.invokeLater(()->{
			controls.addText(String.format("ET %d (ms)",time1-time0));
		});
		System.out.println("Success!");
	}

	private boolean computeStereoCloud( int view0 , int view1, double cx, double cy, boolean skipRectify,
										boolean _automaticChangeViews) {
		if( !skipRectify ) {
			System.out.println("Computing rectification: views " + view0 + " " + view1);
			SceneStructureMetric structure = structureEstimator.getStructure();

			BundlePinholeSimplified cp = structure.getCameras().get(view0).getModel();
			intrinsic01 = new CameraPinholeBrown();
			intrinsic01.fsetK(cp.f, cp.f, 0, cx, cy, dimensions[view0].width, dimensions[view0].height);
			intrinsic01.fsetRadial(cp.k1, cp.k2);

			cp = structure.getCameras().get(view1).getModel();
			intrinsic02 = new CameraPinholeBrown();
			intrinsic02.fsetK(cp.f, cp.f, 0, cx, cy, dimensions[view1].width, dimensions[view1].height);
			intrinsic02.fsetRadial(cp.k1, cp.k2);

			Se3_F64 w_to_0 = structure.views.data[view0].worldToView;
			Se3_F64 w_to_1 = structure.views.data[view1].worldToView;

			leftToRight = w_to_0.invert(null).concat(w_to_1, null);

			Planar<GrayU8> color1 = new Planar<>(GrayU8.class, dimensions[view0].width, dimensions[view0].height, 3);
			Planar<GrayU8> color2 = new Planar<>(GrayU8.class, dimensions[view1].width, dimensions[view1].height, 3);
			ConvertBufferedImage.convertFrom(buff[view0], color1, true);
			ConvertBufferedImage.convertFrom(buff[view1], color2, true);

			// rectify a colored image
			rectifyImages(color1, color2, leftToRight, intrinsic01, intrinsic02,
					rectColor1, rectColor2, rectMask, rectifiedK, rectifiedR);

			SwingUtilities.invokeLater(() -> controls.addText("Rectified Image: "+rectColor1.width+" x "+rectColor1.height+"\n"));

			visualRect1 = ConvertBufferedImage.checkDeclare(
					rectColor1.width, rectColor1.height, visualRect1, visualRect1.getType());
			visualRect2 = ConvertBufferedImage.checkDeclare(
					rectColor2.width, rectColor2.height, visualRect2, visualRect2.getType());
			ConvertBufferedImage.convertTo(rectColor1, visualRect1, true);
			ConvertBufferedImage.convertTo(rectColor2, visualRect2, true);
			BoofSwingUtil.invokeNowOrLater(() -> {
				rectifiedPanel.setImages(visualRect1, visualRect2);
				if( _automaticChangeViews )
					controls.setViews(2);
			});
		} else {
			SwingUtilities.invokeLater(() -> controls.addText("Skipping Rectify\n"));
		}

		if (rectifiedK.get(0, 0) < 0) {
			SwingUtilities.invokeLater(()-> controls.addText("Rectification Failed!\n"));
			return false;
		}

		int disparityRange = controls.controlDisparity.getDisparityRange();
		System.out.println("Computing disparity. range="+disparityRange);
		disparity = computeDisparity(rectColor1,rectColor2);

		// remove annoying false points
		if( disparity instanceof GrayU8)
			RectifyImageOps.applyMask((GrayU8)disparity,rectMask,0);
		else
			RectifyImageOps.applyMask((GrayF32)disparity,rectMask,0);

		visualDisparity = ConvertBufferedImage.checkDeclare(
				disparity.width,disparity.height,visualDisparity,visualDisparity.getType());

		BoofSwingUtil.invokeNowOrLater(()-> {
			controls.addText("Associated "+associated.size+"\n");
			VisualizeImageData.disparity(disparity, visualDisparity, disparityRange, 0);
			guiDisparity.setImageRepaint(visualDisparity);
			if( _automaticChangeViews )
				controls.setViews(3);
		});

		System.out.println("Computing Point Cloud");
		showPointCloud(disparity,visualRect1,leftToRight,rectifiedK,rectifiedR,_automaticChangeViews);

		return true;
	}

	/**
	 * Select two views which are the closest to an idea stereo pair. Little rotation and little translation along
	 * z-axis
	 */
	private int[] selectBestPair( SceneStructureMetric structure ) {
		Se3_F64 w_to_0 = structure.views.data[0].worldToView;
		Se3_F64 w_to_1 = structure.views.data[1].worldToView;
		Se3_F64 w_to_2 = structure.views.data[2].worldToView;

		Se3_F64 view0_to_1 = w_to_0.invert(null).concat(w_to_1,null);
		Se3_F64 view0_to_2 = w_to_0.invert(null).concat(w_to_2,null);
		Se3_F64 view1_to_2 = w_to_1.invert(null).concat(w_to_2,null);

		Se3_F64 candidates[] = new Se3_F64[]{view0_to_1,view0_to_2,view1_to_2};

		int best = -1;
		double bestScore = Double.MAX_VALUE;
		for (int i = 0; i < candidates.length; i++) {
			double s = score(candidates[i]);
			System.out.println("stereo score["+i+"] = "+s);
			if( s < bestScore ) {
				bestScore = s;
				best = i;
			}
		}

		switch (best) {
			case 0: return new int[]{0,1};
			case 1: return new int[]{0,2};
			case 2: return new int[]{1,2};
		}
		throw new RuntimeException("BUG!");
	}

	/**
	 * Give lower scores to transforms with no rotation and translations along x or y axis.
	 */
	private double score( Se3_F64 se ) {
//		Rodrigues_F64 rod = new Rodrigues_F64();
//		ConvertRotation3D_F64.matrixToRodrigues(se.R,rod);

		double x = Math.abs(se.T.x);
		double y = Math.abs(se.T.y);
		double z = Math.abs(se.T.z)+1e-8;

		double r = Math.max(x/(y+z),y/(x+z));

//		System.out.println(se.T+"  angle="+rod.theta);

//		return (Math.abs(rod.theta)+1e-3)/r;
		return 1.0/r; // ignoring rotation seems to work better <shrug>
	}


	public <C extends ImageBase<C> >
	void rectifyImages(C distorted1,
					   C distorted2,
					   Se3_F64 leftToRight,
					   CameraPinholeBrown intrinsic1,
					   CameraPinholeBrown intrinsic2,
					   C rectified1,
					   C rectified2,
					   GrayU8 rectifiedMask,
					   DMatrixRMaj rectifiedK,
					   DMatrixRMaj rectifiedR) {
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

		// original camera calibration matrices
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(intrinsic1, (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(intrinsic2, (DMatrixRMaj)null);

		rectifyAlg.process(K1, new Se3_F64(), K2, leftToRight);

		// rectification matrix for each image
		DMatrixRMaj rect1 = rectifyAlg.getRect1();
		DMatrixRMaj rect2 = rectifyAlg.getRect2();
		rectifiedR.set(rectifyAlg.getRectifiedRotation());

		// New calibration matrix,
		rectifiedK.set(rectifyAlg.getCalibrationMatrix());

		// Maximize the view of the left image and adjust the size of the rectified image
		ImageDimension rectifiedShape = new ImageDimension();
		RectifyImageOps.fullViewLeft(intrinsic1, rect1, rect2, rectifiedK,rectifiedShape);
//		RectifyImageOps.allInsideLeft(intrinsic1, rect1, rect2, rectifiedK,rectifiedShape);

		// undistorted and rectify images
		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3,3);
		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3,3);
		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		ImageDistort<C,C> distortLeft =
				RectifyImageOps.rectifyImage(intrinsic1, rect1_F32, BorderType.EXTENDED, distorted1.getImageType());
		ImageDistort<C,C> distortRight =
				RectifyImageOps.rectifyImage(intrinsic2, rect2_F32, BorderType.EXTENDED, distorted2.getImageType());

		rectifiedMask.reshape(rectifiedShape.width,rectifiedShape.height);
		rectified1.reshape(rectifiedShape.width,rectifiedShape.height);
		rectified2.reshape(rectifiedShape.width,rectifiedShape.height);

		distortLeft.apply(distorted1, rectified1, rectifiedMask);
		distortRight.apply(distorted2, rectified2);
	}

	public ImageGray computeDisparity( Planar<GrayU8> rectColor1 , Planar<GrayU8> rectColor2 )
	{
		GrayU8 rectifiedLeft = new GrayU8(rectColor1.width,rectColor1.height);
		GrayU8 rectifiedRight = new GrayU8(rectColor2.width,rectColor2.height);
		ConvertImage.average(rectColor1,rectifiedLeft);
		ConvertImage.average(rectColor2,rectifiedRight);

		// compute disparity
		StereoDisparity disparityAlg = controls.controlDisparity.createAlgorithm();
		disparityAlg.process(rectifiedLeft, rectifiedRight);

		return (ImageGray)disparityAlg.getDisparity();
	}

	/**
	 * Show results as a point cloud
	 */
	public void showPointCloud(ImageGray disparity, BufferedImage left,
							   Se3_F64 motion, DMatrixRMaj rectifiedK , DMatrixRMaj rectifiedR,
							   boolean _automaticChangeViews )
	{
		DisparityToColorPointCloud d2c = new DisparityToColorPointCloud();
		PointCloudWriter.CloudArraysF32 cloud = new PointCloudWriter.CloudArraysF32();
		double baseline = motion.getT().norm();
		int disparityMin = controls.controlDisparity.getDisparityMin();
		int disparityRange = controls.controlDisparity.getDisparityRange();

		d2c.configure(baseline, rectifiedK, rectifiedR, new DoNothing2Transform2_F64(), disparityMin, disparityRange);
		d2c.process(disparity, UtilDisparitySwing.wrap(left),cloud);

		CameraPinhole rectifiedPinhole = PerspectiveOps.matrixToPinhole(rectifiedK,disparity.width,disparity.height,null);

		PointCloudViewer pcv = guiPointCloud;
		pcv.setCameraHFov(PerspectiveOps.computeHFov(rectifiedPinhole));
		if( _automaticChangeViews ) // snape back to home position
			pcv.setCameraToWorld(new Se3_F64());
		pcv.clearPoints();
		pcv.addCloud(cloud.cloudXyz,cloud.cloudRgb);
		pcv.setDotSize(1);
		pcv.setTranslationStep(baseline/10);

		pcv.getComponent().setPreferredSize(new Dimension(left.getWidth(), left.getHeight()));

		if( _automaticChangeViews ) {
			BoofSwingUtil.invokeNowOrLater(() -> controls.setViews(4));
		}
	}


	private static PathLabel createExample( String name ) {
		String path0 = UtilIO.pathExample("triple/"+name+"_01.jpg");
		String path1 = UtilIO.pathExample("triple/"+name+"_02.jpg");
		String path2 = UtilIO.pathExample("triple/"+name+"_03.jpg");

		return new PathLabel(name,path0,path1,path2);
	}

	public static void main(String[] args) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(createExample("rock_leaves"));
		examples.add(createExample("rockview"));
		examples.add(createExample("mono_wall"));
		examples.add(createExample("bobcats"));
		examples.add(createExample("books"));
		examples.add(createExample("chicken"));
		examples.add(createExample("minecraft_cave1"));
		examples.add(createExample("minecraft_distant"));
		examples.add(createExample("skull"));
		examples.add(createExample("triflowers"));
		examples.add(createExample("turkey"));

		SwingUtilities.invokeLater(()->{
			DemoThreeViewStereoApp app = new DemoThreeViewStereoApp(examples);

			// Processing time takes a bit so don't open right away
//			app.openExample(examples.get(0));
			app.displayImmediate("Three View Uncalibrated Structure");
		});
	}
}
