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

package boofcv.app;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Removes lens distortion from all the images in a directory
 *
 * @author Peter Abeles
 */
public class BatchRemoveLensDistortion {
	public static void main(String[] args) {

		String directory = "/home/pja/projects/boofcv/evaluation/fiducial/";

		IntrinsicParameters param = UtilIO.loadXML(directory + "intrinsic.xml");
		IntrinsicParameters paramAdj = new IntrinsicParameters();
		List<String> fileNames = BoofMiscOps.directoryList(directory, "image");

		MultiSpectral<ImageFloat32> distoredImg = new MultiSpectral<ImageFloat32>(ImageFloat32.class,param.width,param.height,3);
		MultiSpectral<ImageFloat32> undistoredImg = new MultiSpectral<ImageFloat32>(ImageFloat32.class,param.width,param.height,3);

		ImageDistort distort = LensDistortionOps.removeDistortion(true,null,param,paramAdj,
				(ImageType)distoredImg.getImageType());
		UtilIO.saveXML(paramAdj,directory+"intrinsicUndistorted.xml");

		BufferedImage out = new BufferedImage(param.width,param.height,BufferedImage.TYPE_INT_RGB);

		for( String name : fileNames ) {
			System.out.println("processing " + name);
			BufferedImage orig = UtilImageIO.loadImage(name);

			ConvertBufferedImage.convertFromMulti(orig, distoredImg, true, ImageFloat32.class);
			distort.apply(distoredImg,undistoredImg);
			ConvertBufferedImage.convertTo(undistoredImg,out,true);

			String nameOut = name.split("\\.")[0]+"_undistorted.png";
			UtilImageIO.saveImage(out,nameOut);
		}
	}
}
