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

package boofcv.struct.flow;

/**
 * The dense optical flow of an image.  Each pixel contains a data structure that indicates optical flow at the pixel
 * and if the optical flow could be found.
 *
 * @author Peter Abeles
 */
public class ImageFlow {
	// image dimension
	public int width,height;

	// storage for flow information
	public D data[] = new D[0];

	public ImageFlow(int width, int height) {
		reshape(width,height);
	}

	/**
	 * Changes the shape to match the specified dimension. Memory will only be created/destroyed if the requested
	 * size is larger than any previously requested size
	 * @param width New image width
	 * @param height new image height
	 */
	public void reshape( int width , int height ) {
		int N = width*height;

		if( data.length < N ) {
			D tmp[] = new D[N];
			System.arraycopy(data,0,tmp,0,data.length);
			for( int i = data.length; i < N; i++ )
				tmp[i] = new D();
			data = tmp;
		}
		this.width = width;
		this.height = height;
	}

	/**
	 * Marks all pixels as having an invalid flow
	 */
	public void invalidateAll() {
		int N = width*height;
		for( int i = 0; i < N; i++ )
			data[i].valid = false;
	}

	public D get( int x , int y ) {
		if( !isInBounds(x,y))
			throw new IllegalArgumentException("Requested pixel is out of bounds: "+x+" "+y);

		return data[y*width+x];
	}

	public D unsafe_get( int x , int y ) {
		return data[y*width+x];
	}

	public final boolean isInBounds(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * Specifies the optical flow for a single pixel.  The value of (x,y) is only valid if 'valid' equals true.
	 * If 'valid' is false then the meaning of (x,y) is undefined and should not be used.
	 */
	public static class D
	{
		/**
		 * Optical flow
		 */
		public float x,y;
		/**
		 * If true then the optical flow could be computed at this pixel. If false then the flow could not
		 * be found and (x,y) have an undefined meaning and should be ignored.
		 */
		public boolean valid;

		public void set( D d ) {
			this.x = d.x;
			this.y = d.y;
			this.valid = d.valid;
		}

		public float getX() {
			return x;
		}

		public float getY() {
			return y;
		}

		public boolean isValid() {
			return valid;
		}
	}
}
