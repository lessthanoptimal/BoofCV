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

package boofcv.factory.fiducial;

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ConfigThresholdLocalOtsu;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import boofcv.struct.ConnectRule;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for {@link boofcv.abst.fiducial.QrCodePreciseDetector}
 *
 * @author Peter Abeles
 */
public class ConfigQrCode implements Configuration {
	/** Specifies how images are thresholded and converted into a binary format */
	public ConfigThreshold threshold;

	/** Configuration for polygon detector that's used to find position patterns */
	public ConfigPolygonDetector polygon = new ConfigPolygonDetector();

	/**
	 * Minimum and maximum assumed version of QR Codes. Can be used to reduce false positives, slightly.
	 */
	public int versionMinimum = 1;
	public int versionMaximum = 40;

	/**
	 * This forces the encoding in byte mode to use the specified encoding, unless one is specified by the ECI
	 * mode. If null an attempt to automatically determine the encoding is done, but will default to UTF-8 if
	 * there is any ambiguity. ISO 18004:2015 says it should be 8859-1 but
	 * most encoders and decoders use UTF-8 instead
	 */
	public @Nullable String forceEncoding = null;

	/**
	 * If true it will consider QR codes which have been incorrectly encoded with transposed bits. Set to false if
	 * you know your markers are standard compliant and want a modest speed boost.
	 */
	public boolean considerTransposed = true;

	{

		// 40% slower but better at detecting fiducials by a few percentage points
//		ConfigThreshold configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN,15);
//		configThreshold.scale = 1.00;

		// fast but does a bad job detecting fiducials that are up close
		ConfigThresholdLocalOtsu configThreshold = ConfigThreshold.local(ThresholdType.BLOCK_OTSU, 40);
		configThreshold.useOtsu2 = true;
		// 0.95 makes it better some times but worse overall
		configThreshold.scale = 1.0;
		// this will hurt small distant targets but allows up close to work
		configThreshold.thresholdFromLocalBlocks = true;
		configThreshold.tuning = 4;

		threshold = configThreshold;

		polygon.detector.contourRule = ConnectRule.EIGHT;
		polygon.detector.clockwise = false;
		((ConfigPolylineSplitMerge)polygon.detector.contourToPoly).maxSideError = ConfigLength.relative(0.12, 3);
		((ConfigPolylineSplitMerge)polygon.detector.contourToPoly).cornerScorePenalty = 0.4;
		((ConfigPolylineSplitMerge)polygon.detector.contourToPoly).minimumSideLength = 2;
		// 28 pixels = 7 by 7 square viewed head on. Each cell is then 1 pixel. Any slight skew results in
		// aliasing and will most likely not be read well.
		polygon.detector.minimumContour = ConfigLength.fixed(40);
		// can handle much darker images. No measurable decrease in speed
		polygon.detector.minimumEdgeIntensity = 3;
		polygon.minimumRefineEdgeIntensity = 6;
		// TODO This needs to be reduced for smaller shapes, but should be larger to better handle blur? Experiment
		polygon.detector.tangentEdgeIntensity = 1.5;
	}

	/**
	 * Default configuration for a QR Code detector which is optimized for speed
	 */
	public static ConfigQrCode fast() {
		// A global threshold is faster than any local algorithm
		// plus it will generate a simpler set of internal contours speeding up that process
		ConfigQrCode config = new ConfigQrCode();
		config.threshold = ConfigThreshold.global(ThresholdType.GLOBAL_OTSU);
		return config;
	}

	public ConfigQrCode setTo( ConfigQrCode src ) {
		this.threshold.setTo(src.threshold);
		this.polygon.setTo(src.polygon);
		this.versionMinimum = src.versionMinimum;
		this.versionMaximum = src.versionMaximum;
		this.forceEncoding = src.forceEncoding;
		this.considerTransposed = src.considerTransposed;
		return this;
	}

	@Override
	public void checkValidity() {
		// this is now manually set by the detector. previous settings don't matter
//		if( polygon.detector.clockwise )
//			throw new IllegalArgumentException("Must be counter clockwise");
//		if( polygon.detector.minimumSides != 4 || polygon.detector.maximumSides != 4)
//			throw new IllegalArgumentException("Must detect 4 sides and only 4 sides");

	}
}
