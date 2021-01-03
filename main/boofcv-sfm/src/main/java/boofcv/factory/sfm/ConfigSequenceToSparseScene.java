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

package boofcv.factory.sfm;

import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.alg.sfm.structure.ConfigProjectiveReconstruction;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.sfm.structure.ImageSequenceToSparseScene}
 *
 * @author Peter Abeles
 */
public class ConfigSequenceToSparseScene implements Configuration {
	/** If an image has more pixels than this it will be down sampled */
	public int maxImagePixels = 800*600;

	/** Bundle adjustment parameters */
	public final ConfigBundleUtils bundleAdjustment = new ConfigBundleUtils();

	/** Specifies how the initial projective reconstruction is computed */
	public final ConfigProjectiveReconstruction projective = new ConfigProjectiveReconstruction();

	/** Feature tracker */
	public final ConfigPointTracker tracker = new ConfigPointTracker();

	/** Creating pairwise graph */
	public final ConfigGeneratePairwiseImageGraph pairwise = new ConfigGeneratePairwiseImageGraph();

	{
		// Give the tracker reasonable default parameters for this application
		int radius = 5;
		var configTracker = new ConfigPointTracker();
		configTracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		configTracker.klt.pruneClose = true;
		configTracker.klt.toleranceFB = 2;
		configTracker.klt.templateRadius = radius;
		configTracker.klt.maximumTracks.setFixed(800);
		configTracker.klt.config.maxIterations = 30;
		configTracker.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.POINT;
		configTracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		configTracker.detDesc.detectPoint.shiTomasi.radius = 6;
		configTracker.detDesc.detectPoint.general.radius = 4;
//		configTracker.detDesc.detectPoint.general.threshold = 0;
		configTracker.detDesc.detectPoint.general.selector = ConfigSelectLimit.selectUniform(2.0);
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(maxImagePixels>0);
		bundleAdjustment.checkValidity();
		projective.checkValidity();
		tracker.checkValidity();
		pairwise.checkValidity();
	}

	public void setTo( ConfigSequenceToSparseScene src ) {
		this.maxImagePixels = src.maxImagePixels;
		this.bundleAdjustment.setTo(src.bundleAdjustment);
		this.projective.setTo(src.projective);
		this.tracker.setTo(src.tracker);
		this.pairwise.setTo(src.pairwise);
	}
}
