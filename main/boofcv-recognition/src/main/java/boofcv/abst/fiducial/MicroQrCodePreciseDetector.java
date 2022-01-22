/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.fiducial;

import boofcv.BoofVerbose;
import boofcv.abst.filter.binary.BinaryContourHelper;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.fiducial.microqr.MicroQrCode;
import boofcv.alg.fiducial.microqr.MicroQrCodeDecoderImage;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.fiducial.qrcode.QrCodePositionPatternDetector;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.MovingAverage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import lombok.Getter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * A QR-Code detector which is designed to find the location of corners in the finder pattern precisely.
 */
public class MicroQrCodePreciseDetector<T extends ImageGray<T>> implements MicroQrCodeDetector<T>, VerbosePrint {
	@Getter QrCodePositionPatternDetector<T> detectPositionPatterns;
	@Getter MicroQrCodeDecoderImage<T> decoder;
	InputToBinary<T> inputToBinary;
	Class<T> imageType;

	BinaryContourHelper contourHelper;

	// runtime profiling
	@Nullable PrintStream profiler = null;
	protected MovingAverage milliBinary = new MovingAverage(0.8);
	protected MovingAverage milliDecoding = new MovingAverage(0.8);

	public MicroQrCodePreciseDetector( InputToBinary<T> inputToBinary,
									   QrCodePositionPatternDetector<T> detectPositionPatterns,
									   @Nullable String forceEncoding,
									   String defaultEncoding,
									   boolean copyBinary, Class<T> imageType ) {
		this.inputToBinary = inputToBinary;
		this.detectPositionPatterns = detectPositionPatterns;
		this.decoder = new MicroQrCodeDecoderImage<>(forceEncoding, defaultEncoding, imageType);
		this.imageType = imageType;
		this.contourHelper = new BinaryContourHelper(
				detectPositionPatterns.getSquareDetector().getDetector().getContourFinder(), copyBinary);

		// Let it detect larger contours. 4x the smallest side of the image. throws in some fudge-factor
		detectPositionPatterns.setMaxContourFraction(4.0);
	}

	@Override
	public void process( T gray ) {
		long time0 = System.nanoTime();
		contourHelper.reshape(gray.width, gray.height);
		inputToBinary.process(gray, contourHelper.withoutPadding());
		long time1 = System.nanoTime();
		milliBinary.update((time1 - time0)*1e-6);

		// Find position patterns and create a graph
		detectPositionPatterns.process(gray, contourHelper.padded());
		List<PositionPatternNode> positionPatterns = detectPositionPatterns.getPositionPatterns().toList();

		time0 = System.nanoTime();
		decoder.process(positionPatterns, gray);
		time1 = System.nanoTime();
		milliDecoding.update((time1 - time0)*1e-6);

		if (profiler != null) {
			DetectPolygonFromContour<T> detectorPoly = detectPositionPatterns.getSquareDetector().getDetector();
			profiler.printf("qrcode: binary %5.2f contour %5.1f shapes %5.1f adjust_bias %5.2f PosPat %6.2f decoding %5.1f\n",
					milliBinary.getAverage(),
					detectorPoly.getMilliContour(), detectorPoly.getMilliShapes(),
					detectPositionPatterns.getSquareDetector().getMilliAdjustBias(),
					detectPositionPatterns.getProfilingMS().getAverage(), milliDecoding.getAverage());
		}
	}

	@Override
	public List<MicroQrCode> getDetections() {
		return decoder.getFound();
	}

	@Override
	public List<MicroQrCode> getFailures() {
		return decoder.getFailures();
	}

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates. The undistorted image is never explicitly created.</p>
	 *
	 * @param width Input image width. Used in sanity check only.
	 * @param height Input image height. Used in sanity check only.
	 * @param model distortion model. Null to remove a distortion model.
	 */
	public void setLensDistortion( int width, int height, @Nullable LensDistortionNarrowFOV model ) {
		detectPositionPatterns.setLensDistortion(width, height, model);
		decoder.setLensDistortion(width, height, model);
	}

	public GrayU8 getBinary() {
		return contourHelper.withoutPadding();
	}

	public void resetRuntimeProfiling() {
		milliBinary.reset();
		milliDecoding.reset();
		detectPositionPatterns.resetRuntimeProfiling();
	}

	public DetectPolygonBinaryGrayRefine<T> getSquareDetector() {
		return detectPositionPatterns.getSquareDetector();
	}

	@Override
	public Class<T> getImageType() {
		return imageType;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		BoofMiscOps.verboseChildren(out, configuration, decoder, detectPositionPatterns);

		if (configuration == null)
			return;
		if (configuration.contains(BoofVerbose.RUNTIME))
			this.profiler = BoofMiscOps.addPrefix(this, out);
	}
}
