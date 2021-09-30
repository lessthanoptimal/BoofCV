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

package boofcv;

import boofcv.demonstrations.binary.DemoBinaryBlobLabelOpsApp;
import boofcv.demonstrations.binary.DemoBinaryImageOpsApp;
import boofcv.demonstrations.binary.DemoImageThresholdingApp;
import boofcv.demonstrations.binary.VisualizeBinaryContourApp;
import boofcv.demonstrations.calibration.*;
import boofcv.demonstrations.color.ShowColorModelApp;
import boofcv.demonstrations.distort.*;
import boofcv.demonstrations.enhance.DenoiseVisualizeApp;
import boofcv.demonstrations.enhance.ImageEnhanceApp;
import boofcv.demonstrations.feature.associate.VisualizeAssociationMatchesApp;
import boofcv.demonstrations.feature.associate.VisualizeAssociationScoreApp;
import boofcv.demonstrations.feature.describe.VisualizeHogDescriptorApp;
import boofcv.demonstrations.feature.describe.VisualizeImageHogCellApp;
import boofcv.demonstrations.feature.describe.VisualizeRegionDescriptionApp;
import boofcv.demonstrations.feature.detect.DetectXCornersVisualizeApp;
import boofcv.demonstrations.feature.detect.edge.ShowEdgeContourApp;
import boofcv.demonstrations.feature.detect.extract.CompareFeatureExtractorApp;
import boofcv.demonstrations.feature.detect.intensity.IntensityFeaturePyramidApp;
import boofcv.demonstrations.feature.detect.intensity.IntensityPointFeatureApp;
import boofcv.demonstrations.feature.detect.interest.DemoDetectPointFeaturesApp;
import boofcv.demonstrations.feature.detect.interest.DetectPointScaleOriWithNoiseApp;
import boofcv.demonstrations.feature.detect.interest.DetectPointsInsidePyramidApp;
import boofcv.demonstrations.feature.detect.line.DetectLineApp;
import boofcv.demonstrations.feature.detect.line.VisualizeHoughBinary;
import boofcv.demonstrations.feature.detect.line.VisualizeHoughGradient;
import boofcv.demonstrations.feature.detect.line.VisualizeLineRansac;
import boofcv.demonstrations.feature.disparity.VisualizeStereoDisparity;
import boofcv.demonstrations.feature.flow.DenseFlowApp;
import boofcv.demonstrations.feature.orientation.ShowFeatureOrientationApp;
import boofcv.demonstrations.fiducial.*;
import boofcv.demonstrations.imageprocessing.*;
import boofcv.demonstrations.recognition.DemoSceneRecognitionSimilarImagesApp;
import boofcv.demonstrations.segmentation.VisualizeImageSegmentationApp;
import boofcv.demonstrations.segmentation.VisualizeWatershedApp;
import boofcv.demonstrations.sfm.d2.VideoMosaicSequentialPointApp;
import boofcv.demonstrations.sfm.d2.VideoStabilizeSequentialPointApp;
import boofcv.demonstrations.sfm.d3.VisualizeDepthVisualOdometryApp;
import boofcv.demonstrations.sfm.d3.VisualizeMonocularPlaneVisualOdometryApp;
import boofcv.demonstrations.sfm.d3.VisualizeStereoVisualOdometryApp;
import boofcv.demonstrations.sfm.multiview.DemoThreeViewStereoApp;
import boofcv.demonstrations.shapes.DetectBlackEllipseApp;
import boofcv.demonstrations.shapes.DetectBlackPolygonApp;
import boofcv.demonstrations.shapes.DetectPolylineApp;
import boofcv.demonstrations.shapes.ShapeFitContourApp;
import boofcv.demonstrations.tracker.VideoTrackerObjectQuadApp;
import boofcv.demonstrations.tracker.VideoTrackerPointFeaturesApp;
import boofcv.demonstrations.tracker.VisualizeBackgroundModelApp;
import boofcv.demonstrations.tracker.VisualizeCirculantTrackerApp;
import boofcv.demonstrations.transform.fft.FourierVisualizeApp;
import boofcv.demonstrations.transform.pyramid.VisualizePyramidDiscreteApp;
import boofcv.demonstrations.transform.pyramid.VisualizePyramidFloatApp;
import boofcv.demonstrations.transform.pyramid.VisualizeScaleSpacePyramidApp;
import boofcv.demonstrations.transform.wavelet.WaveletVisualizeApp;
import boofcv.gui.ApplicationLauncherApp;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Application which lists most of the demonstration application in a GUI and allows the user to double click
 * to launch one in a new JVM.
 *
 * @author Peter Abeles
 */
public class DemonstrationLauncherApp extends ApplicationLauncherApp {

	public DemonstrationLauncherApp() {
		super(true);
	}

	@Override
	protected void createTree( DefaultMutableTreeNode root ) {
		createNodes(root,"Calibration",
				CalibrateMonocularPlanarDemo.class,
				CalibrateStereoPlanarDemo.class,
				DetectCalibrationChessboardBinaryApp.class,
				DetectCalibrationChessboardXCornerApp.class,
				DetectCalibrationSquareGridApp.class,
				DetectCalibrationCircleHexagonalApp.class,
				DetectCalibrationCircleRegularApp.class);

		createNodes(root,"Color",
				ShowColorModelApp.class);

		createNodes(root,"Distort",
				EquirectangularCylinderApp.class,
				EquirectangularPinholeApp.class,
				EquirectangularRotatingApp.class,
				FisheyePinholeApp.class,
				DeformImageKeyPointsApp.class,
				RemoveLensDistortionApp.class,
				RenderSyntheticCameraModelApp.class);

		createNodes(root,"Enhance",
				ImageEnhanceApp.class,
				DenoiseVisualizeApp.class);

		createNodes(root,"Feature Association",
				VisualizeAssociationMatchesApp.class,
				VisualizeAssociationScoreApp.class);

		createNodes(root,"Feature",
				VisualizeHogDescriptorApp.class,
				VisualizeImageHogCellApp.class,
				VisualizeRegionDescriptionApp.class,
				ShowEdgeContourApp.class,
				CompareFeatureExtractorApp.class,
				IntensityFeaturePyramidApp.class,
				IntensityPointFeatureApp.class,
				DemoDetectPointFeaturesApp.class,
				DetectXCornersVisualizeApp.class,
//				DetectPointsWithNoiseApp.class,
				DetectPointScaleOriWithNoiseApp.class,
				DetectPointsInsidePyramidApp.class,
				ShowFeatureOrientationApp.class);
//				EdgeIntensitiesApp.class);

		createNodes(root,"Recognition",
				DemoSceneRecognitionSimilarImagesApp.class);

		createNodes(root,"SFM 2D",
				VideoMosaicSequentialPointApp.class,
				VideoStabilizeSequentialPointApp.class);

		createNodes(root,"SFM 3D",
				VisualizeStereoDisparity.class,
				VisualizeDepthVisualOdometryApp.class,
				VisualizeMonocularPlaneVisualOdometryApp.class,
				VisualizeStereoVisualOdometryApp.class,
				DemoThreeViewStereoApp.class);

		createNodes(root,"Fiducial / Markers",
				FiducialTrackerDemoApp.class,
				DetectQrCodeApp.class,
				DetectUchiyaMarkerApp.class,
				VisualizeSquareBinaryFiducial.class,
				VisualizeSquareFiducial.class);

		createNodes(root,"Image Processing",
				DemoBinaryImageOpsApp.class,
				DisplayGaussianKernelApp.class,
				DisplaySteerableGaussianApp.class,
				ShowImageBlurApp.class,
				ShowImageDerivativeApp.class,
				DemonstrationInterpolateScaleApp.class,
				VisualizeFlipRotate.class,
				DenseFlowApp.class);

		createNodes(root,"Segmentation",
				VisualizeBackgroundModelApp.class,
				DemoImageThresholdingApp.class,
				DemoBinaryBlobLabelOpsApp.class,
				VisualizeImageSegmentationApp.class,
				VisualizeWatershedApp.class);

		createNodes(root, "Shapes",
				DetectPolylineApp.class,
				DetectBlackPolygonApp.class,
				DetectBlackEllipseApp.class,
				ShapeFitContourApp.class,
				DetectLineApp.class,
				VisualizeHoughGradient.class,
				VisualizeHoughBinary.class,
				VisualizeLineRansac.class,
				VisualizeBinaryContourApp.class);

		createNodes(root, "Trackers",
				VideoTrackerObjectQuadApp.class,
				VideoTrackerPointFeaturesApp.class,
				VisualizeCirculantTrackerApp.class);

		createNodes(root, "Transforms",
				FourierVisualizeApp.class,
				VisualizePyramidDiscreteApp.class,
				VisualizePyramidFloatApp.class,
				VisualizeScaleSpacePyramidApp.class,
				WaveletVisualizeApp.class);

	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(()-> new DemonstrationLauncherApp().showWindow("Demonstration Launcher"));
	}
}
