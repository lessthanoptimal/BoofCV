/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;

/**
 * Quick Response (QR) Code detector.
 * TODO Comment
 *
 * @author Peter Abeles
 */
public class QrCodeDetector<T extends ImageGray<T>> {

	QrCodePositionPatternDetector<T> detectPositionPatterns;
	QrCodeDecoder<T> decoder;

	public QrCodeDetector( QrCodePositionPatternDetector<T> detectPositionPatterns ,
						   Class<T> imageType ) {
		this.detectPositionPatterns = detectPositionPatterns;
		this.decoder = new QrCodeDecoder<>(imageType);
	}

	public void process(T gray, GrayU8 binary ) {
		detectPositionPatterns.process(gray,binary);
		decoder.process(detectPositionPatterns.getPositionPatterns(),gray);
	}

	public void resetRuntimeProfiling() {
		detectPositionPatterns.resetRuntimeProfiling();
	}

	public QrCodePositionPatternDetector<T> getDetectPositionPatterns() {
		return detectPositionPatterns;
	}

	public DetectPolygonBinaryGrayRefine<T> getSquareDetector() {
		return detectPositionPatterns.getSquareDetector();
	}

	public FastQueue<QrCode> getDetections() {
		return decoder.getFound();
	}

	public ImageType<T> getInputType() {
		return detectPositionPatterns.interpolate.getImageType();
	}
}
