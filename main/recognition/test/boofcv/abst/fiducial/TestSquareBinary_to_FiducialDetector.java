/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.fiducial;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class TestSquareBinary_to_FiducialDetector extends GenericFiducialDetectorChecks {


	String directory = UtilIO.getPathToBase()+"data/applet/fiducial/binary/";

	public TestSquareBinary_to_FiducialDetector() {
		types.add( ImageType.single(ImageUInt8.class));
		types.add( ImageType.single(ImageFloat32.class));
	}

	@Override
	public ImageBase loadImage(ImageType imageType) {

		BufferedImage out = UtilImageIO.loadImage(directory+"image0000.jpg");
		return ConvertBufferedImage.convertFrom(out,true,imageType);
	}

	@Override
	public IntrinsicParameters loadIntrinsic() {
		return UtilIO.loadXML(directory+"intrinsic.xml");
	}

	@Override
	public FiducialDetector createDetector(ImageType imageType) {
		return FactoryFiducial.squareBinaryRobust(new ConfigFiducialBinary(0.1),6,imageType.getImageClass());
	}
}