/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity;

import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point3D_F64;

import java.util.List;

/**
 * TODO COmment
 * TODO is this being used elsewhere, like in applets?
 *
 * @author Peter Abeles
 */
public class DisparityPointCloud {
	FastQueue<Point3D_F64> cloud = new FastQueue<Point3D_F64>(200,Point3D_F64.class,true);

	// distance between the two camera centers
	double baseline;

	double focalLengthX;
	double focalLengthY;
	double centerX;
	double centerY;

	// minimum disparity
	int minDisparity;
	// maximum minus minimum disparity
	int rangeDisparity;

	public void configure(double baseline,
						  double focalLengthX, double focalLengthY,
						  double centerX, double centerY,
						  int minDisparity, int maxDisparity) {
		this.baseline = baseline;
		this.focalLengthX = focalLengthX;
		this.focalLengthY = focalLengthY;
		this.centerX = centerX;
		this.centerY = centerY;
		this.minDisparity = minDisparity;

		this.rangeDisparity = maxDisparity-minDisparity;
	}

	public void process( ImageUInt8 disparity ) {

		cloud.reset();

		for( int y = 0; y < disparity.height; y++ ) {
			int index = disparity.startIndex + disparity.stride*y;

			for( int x = 0; x < disparity.width; x++ ) {
				int value = disparity.data[index++] & 0xFF;

				if( value >= rangeDisparity )
					continue;

				value += minDisparity;

				if( value == 0 )
					continue;

				Point3D_F64 p = cloud.grow();

				p.z = baseline*focalLengthX/value;
				p.x = p.z*(x - centerX)/focalLengthX;
				p.y = p.z*(y - centerY)/focalLengthY;
			}
		}
	}

	public List<Point3D_F64> getCloud() {
		return cloud.toList();
	}
}
