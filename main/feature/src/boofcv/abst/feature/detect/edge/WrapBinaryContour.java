/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.edge;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import java.util.List;


/**
 * Simplistic algorithm for finding the contour using binary image operations.
 * THe image is thresholded based on its mean image intensity.  Edges of objects are
 * found, clustered together based on connectivity, and lists of connected pixels returned.
 *
 * @author Peter Abeles
 */
public class WrapBinaryContour<T extends ImageSingleBand> implements DetectEdgeContour<T>{

	// threshold description
	double threshold;
	boolean down;

	// thresholded image
	ImageUInt8 thresh = new ImageUInt8(1,1);
	// found edges
	ImageUInt8 edge = new ImageUInt8(1,1);
	// labeled edges
	ImageSInt32 label = new ImageSInt32(1,1);

	// reuse found points
	private FastQueue<Point2D_I32> queuePts = new FastQueue<Point2D_I32>(100,Point2D_I32.class,true);

	List<List<Point2D_I32>> contours;

	public WrapBinaryContour(double threshold, boolean down) {
		this.threshold = threshold;
		this.down = down;
	}

	@Override
	public void process(T input) {

		thresh.reshape(input.width,input.height);
		edge.reshape(input.width,input.height);
		label.reshape(input.width,input.height);

		// threshold image
		GThresholdImageOps.threshold(input,thresh,threshold,down);
		// extract the edges
		BinaryImageOps.edge4(thresh,edge);

		// label the edges
		int numFound = BinaryImageOps.labelBlobs8(edge,label);

		// extract the contours
		contours = BinaryImageOps.labelToClusters(label,numFound,queuePts);
	}

	@Override
	public List<List<Point2D_I32>> getContours() {
		return contours;
	}
}
