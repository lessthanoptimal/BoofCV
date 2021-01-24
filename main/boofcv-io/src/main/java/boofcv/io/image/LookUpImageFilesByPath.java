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

package boofcv.io.image;

import boofcv.misc.LookUpImages;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDimension;
import lombok.Getter;
import lombok.Setter;

import static boofcv.misc.BoofMiscOps.checkEq;

/**
 * The image ID or name is assumed to the path to the image
 *
 * @author Peter Abeles
 */
public class LookUpImageFilesByPath implements LookUpImages {
	final ImageDimension dimension = new ImageDimension(-1,-1);
	@Getter @Setter LookUpImageFilesByIndex.LoadImage loader;

	public LookUpImageFilesByPath( LookUpImageFilesByIndex.LoadImage loader ) {
		this.loader = loader;
	}

	public LookUpImageFilesByPath() {
		this((path,output)-> UtilImageIO.loadImage(path, true, output));
	}

	@Override public boolean loadShape( String path, ImageDimension shape ) {
		if (dimension.height==-1) {
			var gray = new GrayU8(1, 1);
			loader.load(path, gray);
			dimension.width = gray.getWidth();
			dimension.height = gray.getHeight();
		}

		shape.setTo(dimension);
		return true;
	}

	@Override public <LT extends ImageBase<LT>> boolean loadImage( String path, LT output ) {
		loader.load(path, output);

		// Validate the assumption that all images are the same size. if this is false then loadShape() is giving
		// incorrect results
		if (dimension.width==-1) {
			dimension.setTo(output.width, output.height);
		} else {
			checkEq(dimension.width, output.width);
			checkEq(dimension.height, output.height);
		}
		return true;
	}
}
