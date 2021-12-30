/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.calib;

import boofcv.struct.image.ImageDimension;
import lombok.Getter;
import lombok.Setter;

/**
 * Information about a view when elevating it from projective to metric.
 *
 * @author Peter Abeles
 */
public class ElevateViewInfo {
	/**
	 * Shape of the image.
	 */
	@Getter public final ImageDimension shape = new ImageDimension();
	/**
	 * The camera ID. Used to indicate if the same intrinsic parameters can be assumed for multiple views. The view
	 * ID will always be 0 &le; id &lt; len(views).
	 */
	@Getter @Setter public int cameraID;

	public ElevateViewInfo() {}

	public ElevateViewInfo( int width, int height, int id ) {
		shape.setTo(width, height);
		this.cameraID = id;
	}

	public void setTo( ElevateViewInfo src ) {
		this.shape.setTo(src.shape);
		this.cameraID = src.cameraID;
	}

	public void setTo( int width, int height, int id ) {
		shape.setTo(width, height);
		this.cameraID = id;
	}

	public void reset() {
		shape.setTo(-1, -1);
		cameraID = -1;
	}
}
