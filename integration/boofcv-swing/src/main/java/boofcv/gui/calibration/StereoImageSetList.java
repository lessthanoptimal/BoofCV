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

package boofcv.gui.calibration;

import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Implements {@link StereoImageSet} for two sets of image paths in lists.
 *
 * @author Peter Abeles
 */
public class StereoImageSetList implements StereoImageSet {
	java.util.List<String> left, right;
	int selected;

	public StereoImageSetList( List<String> left, List<String> right ) {
		BoofMiscOps.checkEq(left.size(), right.size());
		this.left = left;
		this.right = right;
	}

	@Override public void setSelected( int index ) {this.selected = index;}

	@Override public int size() {return left.size();}

	@Override public String getLeftName() {return new File(left.get(selected)).getName();}

	@Override public String getRightName() {return new File(right.get(selected)).getName();}

	@Override public BufferedImage loadLeft() {
		BufferedImage image = UtilImageIO.loadImage(left.get(selected));
		return Objects.requireNonNull(image);
	}

	@Override public BufferedImage loadRight() {
		BufferedImage image = UtilImageIO.loadImage(right.get(selected));
		return Objects.requireNonNull(image);
	}
}
