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

import boofcv.alg.mvs.LookUpImages;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDimension;

import java.awt.image.BufferedImage;
import java.util.List;

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
		BoofMiscOps.checkTrue(paths.size()>0);
		this.paths = paths;

		if (paths.size()==0)
			return;

		BufferedImage b = UtilImageIO.loadImage(paths.get(0));
		dimension.width = b.getWidth();
		dimension.height = b.getHeight();
	}

	@Override public boolean loadShape( String name, ImageDimension shape ) {
		int index = Integer.parseInt(name);
		if (index<0 || index>=paths.size())
			return false;
		shape.setTo(dimension);
		return true;
	}

	@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
		int index = Integer.parseInt(name);
		if (index<0 || index>=paths.size())
			return false;

		UtilImageIO.loadImage(paths.get(index), true, output);

		return true;
	}
}
