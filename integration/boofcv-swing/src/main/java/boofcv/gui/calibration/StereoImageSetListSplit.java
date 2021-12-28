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
import org.apache.commons.io.FilenameUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Implements {@link StereoImageSet} for a single list of images which are split in half
 *
 * @author Peter Abeles
 */
public class StereoImageSetListSplit implements StereoImageSet {
	List<String> paths;
	int splitX;
	int selected;

	public StereoImageSetListSplit( List<String> paths, int splitX ) {
		this.paths = paths;
		this.splitX = splitX;
	}

	@Override public void setSelected( int index ) {this.selected = index;}

	@Override public int size() {return paths.size();}

	@Override public String getLeftName() {return FilenameUtils.getBaseName(new File(paths.get(selected)).getName());}

	@Override public String getRightName() {return getLeftName() + "R";}

	@Override public BufferedImage loadLeft() {
		BufferedImage image = UtilImageIO.loadImage(paths.get(selected));
		Objects.requireNonNull(image);
		BufferedImage half = new BufferedImage(splitX, image.getHeight(), image.getType());
		half.createGraphics().drawImage(image, 0, 0, null);
		return half;
	}

	@Override public BufferedImage loadRight() {
		BufferedImage image = Objects.requireNonNull(UtilImageIO.loadImage(paths.get(selected)));
		int width = image.getWidth() - splitX;
		int height = image.getHeight();
		BufferedImage half = new BufferedImage(width, image.getHeight(), image.getType());
		half.createGraphics().drawImage(image, 0, 0, width, height, splitX, 0, image.getWidth(), height, null);
		return half;
	}
}
