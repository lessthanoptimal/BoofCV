///*
// * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
// *
// * This file is part of BoofCV (http://boofcv.org).
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package boofcv.alg.tracker.meanshift;
//
//import boofcv.alg.interpolate.InterpolatePixelS;
//import boofcv.struct.RectangleRotate_F32;
//import boofcv.struct.image.ImageUInt8;
//import boofcv.struct.image.MultiSpectral;
//
///**
// * @author Peter Abeles
// */
//public class TrackerMeanShiftHistogram {
//
//	InterpolatePixelS<ImageUInt8> interpolate;
//
//	private int numSamples;
//
//	// Number of standard away the edge of the rectangle is
//	private double NUM_SIGMAS = 3.0;
//
//	int maxPixelValue;
//	int numBins;
//
//	// cosine and sine of rotation rectangle angle
//	float c,s;
//
//	float imageX,imageY;
//
//	float weights[];
//
//	public void initialize( MultiSpectral<ImageUInt8> image , RectangleRotate_F32 region ) {
//
//		c = (float)Math.cos( region.theta );
//		s = (float)Math.sin( region.theta );
//
//		if( isInBounds(image,region) ) {
//			for( int y = 0; y < numSamples; y++ ) {
//				float regionY = (y/(numSamples-1.0f) - 0.5f)*region.height;
//
//				for( int x = 0; x < numSamples; x++ ) {
//					float regionX = (x/(numSamples-1.0f) - 0.5f)*region.width;
//
//					rectToImage(regionX,regionY,region);
//
//					for( int i = 0; i < image.getNumBands(); i++ ) {
//						interpolate.setImage(image.getBand(i));
//
//
//
//						ImageUInt8 band = image.getBand(i);
//						int value = band.data[index] & 0xFF;
//						int bin = numBins*value/maxPixelValue;
//				}
//			}
//		}
//
//	}
//
//	protected boolean isInBounds( MultiSpectral<ImageUInt8> image , RectangleRotate_F32 region ) {
//
//		interpolate.setImage(image.getBand(0));
//
//		float w2 = region.width/2.0f;
//		float h2 = region.height/2.0f;
//
//		rectToImage(-w2,-h2,region);
//		if( !interpolate.isInFastBounds(imageX, imageY))
//			return false;
//		rectToImage(-w2,h2,region);
//		if( !interpolate.isInFastBounds(imageX, imageY))
//			return false;
//		rectToImage(w2,-h2,region);
//		if( !interpolate.isInFastBounds(imageX, imageY))
//			return false;
//		rectToImage(w2,h2,region);
//		if( !interpolate.isInFastBounds(imageX, imageY))
//			return false;
//
//		return true;
//	}
//
//	protected void rectToImage( float x , float y , RectangleRotate_F32 region ) {
//		imageX = x*c - y*s + region.cx;
//		imageY = x*s + y*c + region.cy;
//	}
//}
