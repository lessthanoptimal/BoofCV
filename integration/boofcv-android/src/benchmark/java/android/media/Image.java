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

import android.graphics.Rect;

import java.nio.ByteBuffer;

public abstract class Image implements AutoCloseable {

	public abstract int getFormat();

	public abstract int getWidth();

	public abstract int getHeight();

	public abstract long getTimestamp();

	public void setTimestamp(long timestamp) {
		throw new RuntimeException("Stub!");
	}

	public Rect getCropRect() {
		throw new RuntimeException("Stub!");
	}

	public void setCropRect(Rect cropRect) {
		throw new RuntimeException("Stub!");
	}

	public abstract Image.Plane[] getPlanes();

	public abstract void close();

	public abstract static class Plane {

		public abstract int getRowStride();

		public abstract int getPixelStride();

		public abstract ByteBuffer getBuffer();
	}
}