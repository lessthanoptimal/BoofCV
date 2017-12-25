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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;

/**
 * Reads binary values from the qr code's grid.
 *
 * @author Peter Abeles
 */
public class QrCodeBinaryGridReader<T extends ImageGray<T>> {
	QrCodeBinaryGridToPixel gridToImage = new QrCodeBinaryGridToPixel();

	InterpolatePixelS<T> interpolate;
	Point2D_F32 pixel = new Point2D_F32();

	int imageWidth,imageHeight;

	float threshold;


	public QrCodeBinaryGridReader( Class<T> imageType ) {
		// use nearest neighbor to avoid shifting the location
		interpolate = FactoryInterpolation.nearestNeighborPixelS(imageType);
		interpolate.setBorder(FactoryImageBorder.single(imageType, BorderType.EXTENDED));
	}

	public void setImage(T image ) {
		interpolate.setImage(image);
		imageWidth = image.width;
		imageHeight = image.height;
	}

	public void setMarker( QrCode qr ) {
		gridToImage.addAllFeatures(qr);
		gridToImage.removeOutsideCornerFeatures();
		gridToImage.computeTransform();
		threshold = (float)(qr.threshCorner+qr.threshDown+qr.threshRight)/3.0f;
	}

	public void gridToImage( float row , float col , Point2D_F32 image ) {
		gridToImage.gridToImage(row, col, image);
	}
	public void gridToImage( double row , double col , Point2D_F64 image ) {
		gridToImage.gridToImage((float)row, (float)col, pixel);
		image.x = pixel.x;
		image.y = pixel.y;
	}

	public float read( float row , float col ) {
		gridToImage.gridToImage(row, col, pixel);
		return interpolate.get(pixel.x,pixel.y);
	}

	/**
	 * Reads a bit from the qr code's data matrix while adjusting for location distortions using known
	 * feature locations.
	 * @param row grid row
	 * @param col grid column
	 * @return
	 */
	public int readBit( int row , int col ) {
		// todo use adjustments from near by alignment patterns

		float center = 0.5f;
		gridToImage.gridToImage(row+center, col+center, pixel);
		if( pixel.x < -0.5 || pixel.y < -0.5 || pixel.x > imageWidth || pixel.y > imageHeight )
			return -1;

//		float pixel01 = interpolate.get(pixel.x-1,pixel.y);
//		float pixel21 = interpolate.get(pixel.x+1,pixel.y);
//		float pixel10 = interpolate.get(pixel.x,pixel.y-1);
//		float pixel12 = interpolate.get(pixel.x,pixel.y+1);
		float pixel00 = interpolate.get(pixel.x,pixel.y);

//		float value = (pixel01+pixel21+pixel10+pixel12)*0.25f;
//		value = value*0.5f + pixel00*0.5f;

		if( pixel00 < threshold )
			return 1;
		else
			return 0;
	}

	public QrCodeBinaryGridToPixel getGridToImage() {
		return gridToImage;
	}
}
