/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.sfm.structure.ThreeViewEstimateMetricScene;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.d3.DisparityToColorPointCloud;
import boofcv.gui.dialogs.OpenImageSetDialog;
import boofcv.gui.feature.AssociatedTriplePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.image.*;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes a stereo point cloud using three uncalibrated images. Visualizes different pre-processing steps and
 * lets the user change a few parameters.
 *
 * @author Peter Abeles
 */
public class DemoThreeViewStereoApp extends DemonstrationBase {

	JPanel gui = new JPanel();
	AssociatedTriplePanel guiAssoc = new AssociatedTriplePanel();
	ImagePanel guiImage = new ImagePanel();
	ImagePanel guiDisparity = new ImagePanel();
	RectifiedPairPanel rectifiedPanel = new RectifiedPairPanel(true);
	PointCloudViewer guiPointCloud = VisualizeData.createPointCloudViewer();

	DemoThreeViewControls controls = new DemoThreeViewControls(this);

	DetectDescribePoint<GrayU8, BrightFeature> detDesc;
	ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class,true);
	AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 0.1, true);

	AssociateThreeByPairs<BrightFeature> associateThree = new AssociateThreeByPairs<>(associate,BrightFeature.class);
	FastQueue<AssociatedTriple> associated = new FastQueue<>(AssociatedTriple.class,true);

	ThreeViewEstimateMetricScene structureEstimator = new ThreeViewEstimateMetricScene();

	FastQueue<Point2D_F64> locations[] = new FastQueue[3];
	FastQueue<BrightFeature> features[] = new FastQueue[3];
	ImageDimension dimensions[] = new ImageDimension[3];

	BufferedImage buff[] = new BufferedImage[3];

	// Rectify and remove lens distortion for stereo processing
	DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);
	DMatrixRMaj rectifiedR = new DMatrixRMaj(3, 3);

	// Results from bundle adjustment
	CameraPinholeBrown intrinsic01;
	CameraPinholeBrown intrinsic02;
	Se3_F64 leftToRight;

	// Visualized Disparity
	BufferedImage visualDisparity= new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
	BufferedImage visualRect1= new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB),
			visualRect2= new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB);

	final Object lockProcessing = new Object();
	boolean processing = false;
	boolean hasAllImages = false;

	public DemoThreeViewStereoApp(List<PathLabel> examples) {
		super(true, false, examples, ImageType.single(GrayU8.class));

		// remove some unused items from the menu bar. This app is an exception
		JMenu fileMenu = menuBar.getMenu(0);
		fileMenu.remove(1);
		fileMenu.remove(1);

		detDesc = FactoryDetectDescribe.surfStable( new ConfigFastHessian(
				0, 4, 1000, 1, 9, 4, 2), null,null, GrayU8.class);

		for (int i = 0; i < 3; i++) {
			locations[i] = new FastQueue<>(Point2D_F64.class,true);
			features[i] = UtilFeature.createQueue(detDesc,100);
			dimensions[i] = new ImageDimension();
		}

		rectifiedPanel.setImages(visualRect1,visualRect2);
		guiDisparity.setImage(visualDisparity);

		gui.setLayout(new BorderLayout());
		updateVisibleGui();

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, gui);

		setPreferredSize(new Dimension(800,600));
	}

	@Override
	protected void openFileMenuBar() {
		String[] files = BoofSwingUtil.openImageSetChooser(window, OpenImageSetDialog.Mode.EXACTLY,3);
		if( files == null )
			return;
		BoofSwingUtil.invokeNowOrLater(()->openImageSet(files));
	}

	void updateVisibleGui() {
		if( gui.getComponentCount() > 0 )
			gui.remove(0);

		switch( controls.view ) {
			case 0: {
				gui.add(BorderLayout.CENTER,guiImage);
			} break;

			case 1:
				gui.add(BorderLayout.CENTER,guiAssoc);
				break;

			case 2:
				gui.add(BorderLayout.CENTER,rectifiedPanel);
				break;

			case 3:
				gui.add(BorderLayout.CENTER,guiDisparity);
				break;

			case 4: {
				gui.add(BorderLayout.CENTER,guiPointCloud.getComponent());
			} break;


			default:
				gui.add(BorderLayout.CENTER,guiImage);
				break;
		}

		gui.validate();
		gui.repaint();
		// this way if you press a key it manipulates the view the user just opened
		gui.getComponent(0).requestFocus();
	}

	/**
	 * Prevent the user from tring to process more than one image at once
	 */
	@Override
	public void openImageSet(String ...files ) {
		synchronized (lockProcessing) {
			if( processing ) {
				JOptionPane.showMessageDialog(this, "Still processing");
				return;
			}
		}
		// disable the menu until it finish processing the images
		setMenuBarEnabled(false);
		super.openImageSet(files);
	}

	void handleComputePressed() {
		if( isProcessing() ) {
			System.err.println("Not finished with previous computation");
			return;
		}

		// If the scale changes then the images need to be loaded again because they are
		// scaled upon input
		if( controls.scaleChanged ) {
			reprocessInput();
		} else {
			boolean skipAssociate = true;
			boolean skipStructure = true;

			if (controls.assocChanged) {
				skipAssociate = false;
				skipStructure = false;
			}
			if (controls.stereoChanged) {
				skipStructure = false;
			}

			boolean _assoc = skipAssociate;
			boolean _struct = skipStructure;

			new Thread(()-> safeProcessImages(_assoc, _struct)).start();
		}
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage bufferedIn, ImageBase input) {
		synchronized (lockProcessing) {
			hasAllImages = false;
		}

		BufferedImage buffered = scaleBuffered(bufferedIn);

		if( sourceID == 0 ) {
			BoofSwingUtil.invokeNowOrLater(()->{
				guiImage.setImage(buffered);
				controls.setViews(0);
			});
		}

		// ok this is ugly.... find a way to not convert the image twice
		ConvertBufferedImage.convertFrom(buffered,input,true);
		System.out.println("Processing image "+sourceID+"  shape "+input.width+" "+input.height);
		System.out.println("  "+inputFilePath);
		dimensions[sourceID].set(input.width,input.height);
		buff[sourceID] = buffered;

		// assume the image center is the principle point
		double cx = input.width/2;
		double cy = input.height/2;

		// detect features
		detDesc.detect((GrayU8)input);
		locations[sourceID].reset();
		features[sourceID].reset();

		// save results
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
			locations[sourceID].grow().set(pixel.x-cx,pixel.y-cy);
			features[sourceID].grow().setTo(detDesc.getDescription(i));
		}
		System.out.println("   found features "+features[sourceID].size);

		if( sourceID < 2 )
			return;

		synchronized (lockProcessing) {
			hasAllImages = true;
		}

		safeProcessImages(false,false);
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
			BufferedImage output = new BufferedImage(w,h,input.getType());
			Planar<GrayU8> a = new Planar<>(GrayU8.class,input.getWidth(),input.getHeight(),3);
			Planar<GrayU8> b = new Planar<>(GrayU8.class,w,h,3);
			ConvertBufferedImage.convertFrom(input,a,true);
			AverageDownSampleOps.down(a,b);
			ConvertBufferedImage.convertTo(b,output,true);
			return output;
		}
	}

	private void safeProcessImages( boolean skipAssociate , boolean skipStructure ) {
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
			processImages(skipAssociate,skipStructure);
		} catch (RuntimeException e ) {

		} finally {
			SwingUtilities.invokeLater(()->setMenuBarEnabled(true));

			synchronized (lockProcessing) {
				processing = false;
			}
		}
	}

	private void processImages( boolean skipAssociate , boolean skipStructure ) {
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
			associateThree.setFeaturesA(features[0]);
			associateThree.setFeaturesB(features[1]);
			associateThree.setFeaturesC(features[2]);
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
				controls.setViews(1);
			});
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
				int numPoints = structureEstimator.structure.points.length;
				controls.addText(String.format("Tri Feats %d\n",n));
				for (int i = 0; i < 3; i++) {
					BundlePinholeSimplified c = structureEstimator.structure.cameras[i].getModel();
					controls.addText(String.format("cam[%d] f=%.1f\n",i,c.f));
					controls.addText(String.format("   k1=%.2f k2=%.2f\n",c.k1,c.k2));
				}
				controls.addText(String.format("SBA Obs %4d Pts %d\n",numObs,numPoints));
				controls.addText(String.format("SBA fit score %.3f\n",score));
			});
		}

		System.out.println("Computing rectification");
		SceneStructureMetric structure = structureEstimator.getStructure();

		BundlePinholeSimplified cp = structure.getCameras()[0].getModel();
		intrinsic01 = new CameraPinholeBrown();
		intrinsic01.fsetK(cp.f, cp.f, 0, cx, cy, dimensions[0].width, dimensions[0].height);
		intrinsic01.fsetRadial(cp.k1, cp.k2);

		cp = structure.getCameras()[1].getModel();
		intrinsic02 = new CameraPinholeBrown();
		intrinsic02.fsetK(cp.f, cp.f, 0, cx, cy, dimensions[1].width, dimensions[1].height);
		intrinsic02.fsetRadial(cp.k1, cp.k2);

		leftToRight = structure.views[1].worldToView;

		Planar<GrayU8> color1 = new Planar<>(GrayU8.class, dimensions[0].width, dimensions[0].height, 3);
		Planar<GrayU8> color2 = new Planar<>(GrayU8.class, dimensions[1].width, dimensions[1].height, 3);
		ConvertBufferedImage.convertFrom(buff[0], color1, true);
		ConvertBufferedImage.convertFrom(buff[1], color2, true);

		// rectify a colored image
		Planar<GrayU8> rectColor1 = new Planar<>(GrayU8.class, color1.width, color1.height, 3);
		Planar<GrayU8> rectColor2 = new Planar<>(GrayU8.class, color2.width, color2.height, 3);
		GrayU8 rectMask = new GrayU8(color1.width, color1.height);

		rectifyImages(color1, color2, leftToRight, intrinsic01, intrinsic02,
				rectColor1, rectColor2,rectMask, rectifiedK, rectifiedR);

		visualRect1 = ConvertBufferedImage.checkDeclare(
				rectColor1.width, rectColor1.height, visualRect1, visualRect1.getType());
		visualRect2 = ConvertBufferedImage.checkDeclare(
				rectColor2.width, rectColor2.height, visualRect2, visualRect2.getType());
		ConvertBufferedImage.convertTo(rectColor1, visualRect1, true);
		ConvertBufferedImage.convertTo(rectColor2, visualRect2, true);
		BoofSwingUtil.invokeNowOrLater(() -> {
			rectifiedPanel.setImages(visualRect1, visualRect2);
			controls.setViews(2);
		});

		if (rectifiedK.get(0, 0) < 0) {
			SwingUtilities.invokeLater(()-> controls.addText("Rectification Failed!\n"));
			return;
		}

		System.out.println("Computing disparity. min="+controls.minDisparity+" max="+controls.maxDisparity);
		GrayF32 disparity = computeDisparity(rectColor1,rectColor2);

		// remove annoying false points
		RectifyImageOps.applyMask(disparity,rectMask,0);

		visualDisparity = ConvertBufferedImage.checkDeclare(
				disparity.width,disparity.height,visualDisparity,visualDisparity.getType());

		BoofSwingUtil.invokeNowOrLater(()-> {
			VisualizeImageData.disparity(disparity, visualDisparity,
					controls.minDisparity, controls.maxDisparity, 0);
			guiDisparity.setImageRepaint(visualDisparity);
			controls.setViews(3);
		});

		System.out.println("Computing Point Cloud");
		showPointCloud(disparity,visualRect1,leftToRight,rectifiedK,rectifiedR);

		long time1 = System.currentTimeMillis();
		SwingUtilities.invokeLater(()->{
			controls.addText(String.format("ET %d (ms)",time1-time0));
		});

		System.out.println("Success!");
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

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.fullViewLeft(intrinsic1, rect1, rect2, rectifiedK);

		// undistorted and rectify images
		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3,3);
		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3,3);
		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		ImageDistort<C,C> distortLeft =
				RectifyImageOps.rectifyImage(intrinsic1, rect1_F32, BorderType.EXTENDED, distorted1.getImageType());
		ImageDistort<C,C> distortRight =
				RectifyImageOps.rectifyImage(intrinsic2, rect2_F32, BorderType.EXTENDED, distorted2.getImageType());

		rectifiedMask.reshape(rectified1.width,rectified2.height);
		distortLeft.apply(distorted1, rectified1, rectifiedMask);
		distortRight.apply(distorted2, rectified2);
	}

	public GrayF32 computeDisparity( Planar<GrayU8> rectColor1 , Planar<GrayU8> rectColor2 )
	{

		GrayU8 rectifiedLeft = new GrayU8(rectColor1.width,rectColor1.height);
		GrayU8 rectifiedRight = new GrayU8(rectColor2.width,rectColor2.height);
		ConvertImage.average(rectColor1,rectifiedLeft);
		ConvertImage.average(rectColor2,rectifiedRight);

		// compute disparity
		StereoDisparity<GrayS16, GrayF32> disparityAlg =
				FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
						controls.minDisparity, controls.maxDisparity, 6, 6, 30, 3, 0.05, GrayS16.class);

		// Apply the Laplacian across the image to add extra resistance to changes in lighting or camera gain
		GrayS16 derivLeft = new GrayS16(rectColor1.width,rectColor1.height);
		GrayS16 derivRight = new GrayS16(rectColor2.width,rectColor2.height);
		LaplacianEdge.process(rectifiedLeft, derivLeft,null);
		LaplacianEdge.process(rectifiedRight,derivRight,null);

		// process and return the results
		disparityAlg.process(derivLeft, derivRight);
		return disparityAlg.getDisparity();
	}

	/**
	 * Show results as a point cloud
	 */
	public void showPointCloud(GrayF32 disparity, BufferedImage left,
							   Se3_F64 motion, DMatrixRMaj rectifiedK , DMatrixRMaj rectifiedR)
	{
		DisparityToColorPointCloud d2c = new DisparityToColorPointCloud();
		double baseline = motion.getT().norm();
		d2c.configure(baseline, rectifiedK, rectifiedR, new DoNothing2Transform2_F64(), controls.minDisparity, controls.maxDisparity);
		d2c.process(disparity,left);

		CameraPinhole rectifiedPinhole = PerspectiveOps.matrixToPinhole(rectifiedK,disparity.width,disparity.height,null);

		PointCloudViewer pcv = guiPointCloud;
		pcv.setCameraHFov(PerspectiveOps.computeHFov(rectifiedPinhole));
		pcv.setCameraToWorld(new Se3_F64());
		pcv.setTranslationStep(baseline/3);
		pcv.clearPoints();
		pcv.addCloud(d2c.getCloud(),d2c.getCloudColor());
		pcv.setDotSize(1);
		pcv.setTranslationStep(baseline/10);

		pcv.getComponent().setPreferredSize(new Dimension(left.getWidth(), left.getHeight()));

		BoofSwingUtil.invokeNowOrLater(()->{
			controls.setViews(4);
		});
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
