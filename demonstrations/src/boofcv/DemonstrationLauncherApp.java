/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.demonstrations.binary.DemoBinaryImageLabelOpsApp;
import boofcv.demonstrations.binary.DemoBinaryImageOpsApp;
import boofcv.demonstrations.calibration.*;
import boofcv.demonstrations.color.ShowColorModelApp;
import boofcv.demonstrations.denoise.DenoiseVisualizeApp;
import boofcv.demonstrations.distort.*;
import boofcv.demonstrations.enhance.ImageEnhanceApp;
import boofcv.demonstrations.feature.associate.VisualizeAssociationAlgorithmsApp;
import boofcv.demonstrations.feature.associate.VisualizeAssociationMatchesApp;
import boofcv.demonstrations.feature.associate.VisualizeAssociationScoreApp;
import boofcv.demonstrations.feature.describe.VisualizeHogDescriptorApp;
import boofcv.demonstrations.feature.describe.VisualizeImageHogCellApp;
import boofcv.demonstrations.feature.describe.VisualizeRegionDescriptionApp;
import boofcv.demonstrations.feature.detect.edge.ShowEdgeContourApp;
import boofcv.demonstrations.feature.detect.extract.CompareFeatureExtractorApp;
import boofcv.demonstrations.feature.detect.intensity.IntensityFeaturePyramidApp;
import boofcv.demonstrations.feature.detect.interest.DetectPointScaleOriWithNoiseApp;
import boofcv.demonstrations.feature.detect.interest.DetectPointsInsidePyramidApp;
import boofcv.demonstrations.feature.detect.interest.DetectPointsWithNoiseApp;
import boofcv.demonstrations.feature.detect.line.DetectLineApp;
import boofcv.demonstrations.feature.detect.line.VisualizeHoughFoot;
import boofcv.demonstrations.feature.detect.line.VisualizeHoughPolar;
import boofcv.demonstrations.feature.detect.line.VisualizeLineRansac;
import boofcv.demonstrations.feature.disparity.VisualizeStereoDisparity;
import boofcv.demonstrations.feature.flow.DenseFlowApp;
import boofcv.demonstrations.feature.orientation.ShowFeatureOrientationApp;
import boofcv.demonstrations.fiducial.FiducialTrackerApp;
import boofcv.demonstrations.fiducial.VisualizeSquareBinaryFiducial;
import boofcv.demonstrations.fiducial.VisualizeSquareFiducial;
import boofcv.demonstrations.filter.DisplayGaussianKernelApp;
import boofcv.demonstrations.filter.DisplaySteerableGaussianApp;
import boofcv.demonstrations.filter.ShowImageBlurApp;
import boofcv.demonstrations.filter.ShowImageDerivative;
import boofcv.demonstrations.ip.DemonstrationInterpolateScaleApp;
import boofcv.demonstrations.ip.VisualizeFlipRotate;
import boofcv.demonstrations.segmentation.VisualizeImageSegmentationApp;
import boofcv.demonstrations.segmentation.VisualizeWatershedApp;
import boofcv.demonstrations.sfm.d2.VideoMosaicSequentialPointApp;
import boofcv.demonstrations.sfm.d2.VideoStabilizeSequentialPointApp;
import boofcv.demonstrations.sfm.d3.VisualizeDepthVisualOdometryApp;
import boofcv.demonstrations.sfm.d3.VisualizeMonocularPlaneVisualOdometryApp;
import boofcv.demonstrations.sfm.d3.VisualizeStereoVisualOdometryApp;
import boofcv.demonstrations.shapes.DetectBlackEllipseApp;
import boofcv.demonstrations.shapes.DetectBlackPolygonApp;
import boofcv.demonstrations.shapes.ShapeFitContourApp;
import boofcv.demonstrations.tracker.VideoTrackerObjectQuadApp;
import boofcv.demonstrations.tracker.VideoTrackerPointFeaturesApp;
import boofcv.demonstrations.tracker.VisualizeCirculantTrackerApp;
import boofcv.demonstrations.transform.fft.FourierVisualizeApp;
import boofcv.demonstrations.transform.pyramid.EdgeIntensitiesApp;
import boofcv.demonstrations.transform.pyramid.VisualizePyramidDiscreteApp;
import boofcv.demonstrations.transform.pyramid.VisualizePyramidFloatApp;
import boofcv.demonstrations.transform.pyramid.VisualizeScaleSpacePyramidApp;
import boofcv.demonstrations.transform.wavelet.WaveletVisualizeApp;
import boofcv.gui.ApplicationLauncherApp;
import boofcv.gui.image.ShowImages;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Application which lists most of the demonstration application in a GUI and allows the user to double click
 * to launch one in a new JVM.
 *
 * @author Peter Abeles
 */
public class DemonstrationLauncherApp extends ApplicationLauncherApp {
	@Override
	protected void createTree( DefaultMutableTreeNode root ) {
		createNodes(root,"Binary",
				DemoBinaryImageLabelOpsApp.class,
				DemoBinaryImageOpsApp.class);

		createNodes(root,"Calibration",
				CalibrateMonoPlanarGuiApp.class,
				CalibrateStereoPlanarGuiApp.class,
				DetectCalibrationChessboardApp.class,
				DetectCalibrationCircleAsymmetricApp.class,
				DetectCalibrationSquareGridApp.class);

		createNodes(root,"Color",
				ShowColorModelApp.class);

		createNodes(root,"Denoise",
				DenoiseVisualizeApp.class);

		createNodes(root,"Distort",
				EquirectangularCylinderApp.class,
				EquirectangularPinholeApp.class,
				EquirectangularRotatingApp.class,
				FisheyePinholeApp.class,
				RemoveLensDistortionApp.class,
				ShowLensDistortion.class);

		createNodes(root,"Enhance",
				ImageEnhanceApp.class);

		createNodes(root,"Feature",
				VisualizeAssociationAlgorithmsApp.class,
				VisualizeAssociationMatchesApp.class,
				VisualizeAssociationScoreApp.class,
//				CompareConvertedDescriptionsApp.class,
				VisualizeHogDescriptorApp.class,
				VisualizeImageHogCellApp.class,
				VisualizeRegionDescriptionApp.class,
				ShowEdgeContourApp.class,
				CompareFeatureExtractorApp.class,
				IntensityFeaturePyramidApp.class,
				DetectPointsWithNoiseApp.class,
				DetectPointScaleOriWithNoiseApp.class,
				DetectPointsInsidePyramidApp.class,
				DetectLineApp.class,
				VisualizeHoughFoot.class,
				VisualizeHoughPolar.class,
				VisualizeLineRansac.class,
				DenseFlowApp.class,
				ShowFeatureOrientationApp.class,
				EdgeIntensitiesApp.class);

		createNodes(root,"SFM 2D",
				VideoMosaicSequentialPointApp.class,
				VideoStabilizeSequentialPointApp.class);

		createNodes(root,"SFM 3D",
				VisualizeStereoDisparity.class,
				VisualizeDepthVisualOdometryApp.class,
				VisualizeMonocularPlaneVisualOdometryApp.class,
				VisualizeStereoVisualOdometryApp.class);

		createNodes(root,"Fiducial / Markers",
				FiducialTrackerApp.class,
				VisualizeSquareBinaryFiducial.class,
				VisualizeSquareFiducial.class);

		createNodes(root,"Image Processing",
				DisplayGaussianKernelApp.class,
				DisplaySteerableGaussianApp.class,
				ShowImageBlurApp.class,
				ShowImageDerivative.class,
				DemonstrationInterpolateScaleApp.class,
				VisualizeFlipRotate.class,
				VisualizeImageSegmentationApp.class,
				VisualizeWatershedApp.class);

		createNodes(root, "Shapes",
				DetectBlackPolygonApp.class,
				DetectBlackEllipseApp.class,
				ShapeFitContourApp.class);

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
		DemonstrationLauncherApp app = new DemonstrationLauncherApp();
		ShowImages.showWindow(app,"Demonstration Launcher",true);
	}




}
