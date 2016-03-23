/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.edge;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;

/**
 * Common tests for tracking hysteresis edges
 *
 * @author Peter Abeles
 */
public abstract class CommonHysteresisEdgeTrace {
	// simple test going around the image edge
	float[] inten0 = new float[]{
			5,5,5,5,
			5,0,0,5,
			5,0,0,5,
			5,0,0,5,
			5,5,5,5};

	byte[] dir0 = new byte[]{
			2,0,0,0,
			2,0,0,2,
			2,0,0,2,
			2,0,0,2,
			0,0,0,2};

	byte[] expect0 = new byte[]{
			1,1,1,1,
			1,0,0,1,
			1,0,0,1,
			1,0,0,1,
			1,1,1,1};

	// test diagonal elements
	float[] inten1 = new float[]{
			0,0,0,0,0,
			0,0,5,0,0,
			0,5,0,5,0,
			0,0,5,0,0,
			0,0,0,0,0};

	byte[] dir1 = new byte[]{
			0,0,0,0,0,
			0,0,1,0,0,
			0,-1,0,-1,0,
			0,0,1,0,0,
			0,0,0,0,0};

	byte[] expect1 = new byte[]{
			0,0,0,0,0,
			0,0,1,0,0,
			0,1,0,1,0,
			0,0,1,0,0,
			0,0,0,0,0};

	// threshold test
	float[] inten2 = new float[]{
			0,0,0,0,0,
			2,5,3,3,1,
			0,0,0,0,0};

	byte[] dir2 = new byte[]{
			0,0,0,0,0,
			0,0,0,0,0,
			0,0,0,0,0};

	// search around end point test

	float[] inten3 = new float[]{
			0,0,0,0,5,
			0,5,5,5,0,
			5,0,0,0,5};

	byte[] dir3 = new byte[]{
			0,0,0,0,0,
			0,0,0,0,0,
			0,0,0,0,0};

	byte[] expect3 = new byte[]{
			0,0,0,0,1,
			0,1,1,1,0,
			1,0,0,0,1};

	// ignore points not along the edge
	float[] inten4 = new float[]{
			0,0,0,0,0,
			5,5,5,5,5,
			0,0,5,0,0};

	byte[] dir4 = new byte[]{
			0,0,0,0,0,
			0,0,0,0,0,
			0,0,0,0,0};

	byte[] expect4 = new byte[]{
			0,0,0,0,0,
			1,1,1,1,1,
			0,0,0,0,0};

	public GrayF32 intensity(int which ) {
		GrayF32 a = new GrayF32();
		setShape(which,a);
		if( which == 0 ) {
			a.data = inten0;
		} else if( which == 1 ) {
			a.data = inten1;
		} else if( which == 2 ) {
			a.data = inten2;
		} else if( which == 3 ) {
			a.data = inten3;
		} else if( which == 4 ) {
			a.data = inten4;
		}
		return a.clone();
	}

	public GrayS8 direction(int which ) {
		GrayS8 a = new GrayS8();
		setShape(which,a);
		if( which == 0 ) {
			a.data = dir0;
		} else if( which == 1 ) {
			a.data = dir1;
		} else if( which == 2 ) {
			a.data = dir2;
		} else if( which == 3 ) {
			a.data = dir3;
		} else if( which == 4 ) {
			a.data = dir4;
		}
		return a.clone();
	}

	public GrayU8 expected(int which ) {
		GrayU8 a = new GrayU8();
		setShape(which,a);
		if( which == 0 ) {
			a.data = expect0;
		} else if( which == 1 ) {
			a.data = expect1;
//		} else if( which == 2 ) {
//			a.data = expect2;
		} else if( which == 3 ) {
			a.data = expect3;
		} else if( which == 4 ) {
			a.data = expect4;
		}
		return a.clone();
	}

	public void setShape( int which , ImageBase a ) {
		if( which == 0 ) {
			a.width = a.stride = 4;
			a.height = 5;
		} else if( which == 1 ) {
			a.width = a.stride = 5;
			a.height = 5;
		} else if( which == 2 ) {
			a.width = a.stride = 5;
			a.height = 3;
		} else if( which == 3 ) {
			a.width = a.stride = 5;
			a.height = 3;
		}
	}
}
