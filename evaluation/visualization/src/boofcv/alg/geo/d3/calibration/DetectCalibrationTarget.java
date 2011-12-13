/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d3.calibration;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectCalibrationTarget<T extends ImageBase> {
	Class<T> imageType;

	// number of black squares in the horizontal and vertical directions
	int gridWidth;
	int gridHeight;

	ImageUInt8 threshold = new ImageUInt8(1,1);
	ImageUInt8 binaryA = new ImageUInt8(1,1);
	ImageUInt8 binaryB = new ImageUInt8(1,1);
	ImageSInt32 blobs = new ImageSInt32(1,1);
	
	int minContourSize = 20*4;

	public DetectCalibrationTarget(Class<T> imageType , int gridWidth , int gridHeight ) {
		this.imageType = imageType;
		this.gridWidth = gridWidth;
		this.gridHeight = gridHeight;
	}

	public void process( T image ) {
		threshold.reshape(image.width,image.height);
		binaryA.reshape(image.width,image.height);
		binaryB.reshape(image.width,image.height);
		blobs.reshape(image.width,image.height);

		GThresholdImageOps.threshold(image,threshold,30,true);

		// filter out small objects
		BinaryImageOps.erode8(threshold,binaryA);
		BinaryImageOps.erode8(binaryA,binaryB);
		BinaryImageOps.dilate8(binaryB, binaryA);
		BinaryImageOps.dilate8(binaryA,binaryB);

		// find blobs
		int numBlobs = BinaryImageOps.labelBlobs8(binaryB,blobs);

		//remove blobs with holes
		numBlobs = removeBlobsHoles(binaryB,blobs,numBlobs);

		// find their contours
		List<List<Point2D_I32>> contours = BinaryImageOps.labelEdgeCluster4(blobs,numBlobs,null);

		// remove blobs which touch the image edge
		filterTouchEdge(contours,image.width,image.height);

		// remove blobs which are not like a polygon at all

		// use original binary image to find corners

		// optimize corners

		BinaryImageOps.clusterToBinary(contours,threshold);
		BufferedImage b = VisualizeBinaryData.renderBinary(threshold, null);
		BufferedImage c = VisualizeBinaryData.renderLabeled(blobs, numBlobs, null);

		ShowImages.showWindow(b,"Threshold");
		ShowImages.showWindow(c,"Blobs");
	}

	/**
	 * Remove contours which touch the image's edge
	 */
	private void filterTouchEdge(List<List<Point2D_I32>> contours , int w , int h ) {
		w--;
		h--;
		
		for( int i = 0; i < contours.size(); ) {
			boolean touched = false;
			for( Point2D_I32 p : contours.get(i)) {
				if( p.x == 0 || p.y == 0 || p.x == w || p.y == h ) {
					contours.remove(i);
					touched = true;
					break;
				}
			}
			if( !touched ) 
				i++;
		}
	}

	public static void main( String args[] ) {
		DetectCalibrationTarget<ImageUInt8> app = new DetectCalibrationTarget<ImageUInt8>(ImageUInt8.class,4,3);

		ImageUInt8 input = UtilImageIO.loadImage("data/calibration/Sony_DSC-HX5V/image01.jpg",ImageUInt8.class);
		app.process(input);

	}

	/**
	 * Remove blobs with holes and blobs with a contour that is too small
	 * @param binary
	 * @param labeled
	 * @param numLabels
	 * @return
	 */
	private int removeBlobsHoles( ImageUInt8 binary , ImageSInt32 labeled , int numLabels )
	{
		ImageUInt8 contourImg = new ImageUInt8(labeled.width,labeled.height);
		ImageSInt32 contourBlobs = new ImageSInt32(labeled.width,labeled.height);
		
		BinaryImageOps.edge8(binary,contourImg);
		int numContours = BinaryImageOps.labelBlobs8(contourImg,contourBlobs);
		List<List<Point2D_I32>> contours = BinaryImageOps.labelToClusters(contourBlobs, numContours, null);

		// see how many complete contours each blob has
		int counter[] = new int[ numLabels + 1 ];
		for( int i = 0; i < numContours; i++ ) {
			List<Point2D_I32> l = contours.get(i);
			Point2D_I32 p = l.get(0);
			int which = labeled.get(p.x,p.y);
			if( l.size() < minContourSize ) {
				// set it to a size larger than one so that it will be zeroed
				counter[which] = 20;
			} else {
				counter[which]++;
			}
		}

		// find the blobs with holes
		counter[0] = 0;
		int counts = 1;
		for( int i = 1; i < counter.length; i++ ) {
			if( counter[i] > 1 )
				counter[i] = 0;
			else
				counter[i] = counts++;
		}

		// relabel the image to remove blobs with holes inside
		BinaryImageOps.relabel(labeled,counter);

		return counts;
	}
}
