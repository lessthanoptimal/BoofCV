/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.binary.BinaryContourHelper;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeDecoderImage;
import boofcv.alg.fiducial.qrcode.QrCodePositionPatternDetector;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.misc.MovingAverage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.util.List;

/**
 * A QR-Code detector which is designed to find the location of corners in the finder pattern precisely.
 *
 * @param <T>
 */
public class QrCodePreciseDetector<T extends ImageGray<T>> implements QrCodeDetector<T>
{
	QrCodePositionPatternDetector<T> detectPositionPatterns;
	QrCodeDecoderImage<T> decoder;
	InputToBinary<T> inputToBinary;
	Class<T> imageType;

	BinaryContourHelper contourHelper;

	// runtime profiling
	boolean profiler = false;
	protected MovingAverage milliBinary = new MovingAverage(0.8);
	protected MovingAverage milliDecoding = new MovingAverage(0.8);

	public QrCodePreciseDetector(InputToBinary<T> inputToBinary,
								 QrCodePositionPatternDetector<T> detectPositionPatterns,
								 boolean copyBinary, Class<T> imageType) {
		this.inputToBinary = inputToBinary;
		this.detectPositionPatterns = detectPositionPatterns;
		this.decoder = new QrCodeDecoderImage<>(imageType);
		this.imageType = imageType;
		this.contourHelper = new BinaryContourHelper(detectPositionPatterns.getSquareDetector().getDetector().getContourFinder(),copyBinary);
	}

	@Override
	public void process(T gray) {
		long time0 = System.nanoTime();
		contourHelper.reshape(gray.width,gray.height);
		inputToBinary.process(gray,contourHelper.withoutPadding());
		long time1 = System.nanoTime();
		milliBinary.update((time1-time0)*1e-6);

		if( profiler )
			System.out.printf("qrcode: binary %5.2f ",milliBinary.getAverage());

		detectPositionPatterns.process(gray,contourHelper.padded());
		time0 = System.nanoTime();
		decoder.process(detectPositionPatterns.getPositionPatterns(),gray);
		time1 = System.nanoTime();
		milliDecoding.update((time1-time0)*1e-6);

		if( profiler )
			System.out.printf(" decoding %5.1f\n",milliDecoding.getAverage());
	}

	@Override
	public List<QrCode> getDetections() {
		return decoder.getFound();
	}

	@Override
	public List<QrCode> getFailures() {
		return decoder.getFailures();
	}

	public GrayU8 getBinary() {
		return contourHelper.withoutPadding();
	}

	public void setProfilerState( boolean active ) {
		profiler = active;
		detectPositionPatterns.setProfilerState(active);
	}

	public void resetRuntimeProfiling() {
		milliBinary.reset();
		milliDecoding.reset();
		detectPositionPatterns.resetRuntimeProfiling();
	}

	public QrCodePositionPatternDetector<T> getDetectPositionPatterns() {
		return detectPositionPatterns;
	}

	public DetectPolygonBinaryGrayRefine<T> getSquareDetector() {
		return detectPositionPatterns.getSquareDetector();
	}

	public QrCodeDecoderImage<T> getDecoder() {
		return decoder;
	}

	@Override
	public Class<T> getImageType() {
		return imageType;
	}
}
