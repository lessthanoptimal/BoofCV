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

package android.media;

import android.graphics.ImageFormat;

import java.nio.ByteBuffer;
import java.util.Random;

public class MockImage_420_888 extends Image {

	int width,height;
	int pixelStrideUV;
	int periodUV;

	byte gray[];
	byte bandUV[];

	Plane[] planes = new Plane[3];

	public MockImage_420_888(Random rand , int width , int height , int pixelStrideUV , int periodUV, int extra ) {
		this();
		this.pixelStrideUV = pixelStrideUV;
		this.periodUV = periodUV;
		this.width = width;
		this.height = height;

		gray = new byte[(width+extra)*height];
		int rowStrideUV = pixelStrideUV*(width/periodUV + (width%periodUV))+extra;

		bandUV = new byte[2*rowStrideUV*(height/periodUV)];

		planes[0] = new DummyPlane(1,width+extra,0,gray.length,gray);
		planes[1] = new DummyPlane(pixelStrideUV,rowStrideUV,0,bandUV.length,bandUV);
		planes[2] = new DummyPlane(pixelStrideUV,rowStrideUV,1,bandUV.length-1,bandUV);

		rand.nextBytes(gray);
		rand.nextBytes(bandUV);
	}

	protected MockImage_420_888(){}

	@Override
	public int getFormat() {
		return ImageFormat.YUV_420_888;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public long getTimestamp() {
		return 0;
	}

	@Override
	public Plane[] getPlanes() {
		return planes;
	}

	@Override
	public void close() {

	}

	public class DummyPlane extends Plane {

		int pixelStride,rowStride;
		int offset,length;
		byte[] data;

		public DummyPlane(int pixelStride, int rowStride, int offset, int length, byte[] data) {
			this.pixelStride = pixelStride;
			this.rowStride = rowStride;
			this.offset = offset;
			this.length = length;
			this.data = data;
		}

		@Override
		public int getRowStride() {
			return rowStride;
		}

		@Override
		public int getPixelStride() {
			return pixelStride;
		}

		@Override
		public ByteBuffer getBuffer() {
			return ByteBuffer.wrap(data,offset,length);
		}
	}
}
