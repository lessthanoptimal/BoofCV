/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.io.image;

import boofcv.misc.BoofMiscOps;
import boofcv.misc.LookUpImages;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDimension;

import java.util.List;

import static boofcv.misc.BoofMiscOps.checkEq;

/**
 * Implementation of {@link LookUpImages} that converts the name into an integer. The integer represents the index
 * of the image in the list of paths provided. It's assumed that all images have the same shape and the first image
 * is loaded to get the shape.
 *
 * @author Peter Abeles
 */
public class LookUpImageFilesByIndex implements LookUpImages {
	List<String> paths;
	ImageDimension dimension = new ImageDimension();

	public LookUpImageFilesByIndex( List<String> paths ) {
		BoofMiscOps.checkTrue(paths.size() > 0);
		this.paths = paths;

		if (paths.size() == 0)
			return;

		// load one image and assume all the rest are the same dimension
		var gray = new GrayU8(1, 1);
		loadImage(paths.get(0), true, gray);
		dimension.width = gray.getWidth();
		dimension.height = gray.getHeight();
	}

	@Override public boolean loadShape( String name, ImageDimension shape ) {
		int index = Integer.parseInt(name);
		if (index < 0 || index >= paths.size())
			return false;
		shape.setTo(dimension);
		return true;
	}

	@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
		int index = Integer.parseInt(name);
		if (index < 0 || index >= paths.size())
			return false;

		loadImage(paths.get(index), true, output);

		// Validate the assumption that all images are the same size. if this is false then loadShape() is giving
		// incorrect results
		checkEq(dimension.width, output.width);
		checkEq(dimension.height, output.height);

		return true;
	}

	/** Wrap for unit testing */
	protected <T extends ImageBase<T>> T loadImage( String imagePath, boolean orderRgb, T output ) {
		return UtilImageIO.loadImage(imagePath, orderRgb, output);
	}
}
