/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.core.image.border.BorderType;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Removes lens distortion from all the images in a directory
 *
 * @author Peter Abeles
 */
public class BatchRemoveLensDistortion {

	public static void printHelpAndExit(String[] args) {
		System.out.println("Expected 1 flag and 3 arguments, had "+args.length+" instead");
		System.out.println();
		System.out.println("<path to directory> <file name regex> <path to intrinsic.yaml> <output directory>");
		System.out.println("path/to/input/image\\d*.jpg path/to/intrinsic.yaml");
		System.out.println();
		System.out.println("Flags:");
		System.out.println("-rename  Rename files on output to image%0d.png");
		System.out.println("-EXPAND  Output image will be expanded until there are no dark regions");
		System.out.println("-FULL_VIEW  Output image will contain the entire undistorted image");
		System.out.println();
		System.out.println("Default is FULL_VIEW and it doesn't rename the images");
	}

	public static void main(String[] args) {
		String inputPath,regex,pathIntrinsic,outputDir;
		AdjustmentType adjustmentType = AdjustmentType.FULL_VIEW;
		boolean rename = false;

		if( args.length >= 4 ) {
			int numFlags = args.length-4;
			for (int i = 0; i < numFlags; i++) {
				if( args[i].compareToIgnoreCase("-rename") == 0 ) {
					rename = true;
				} else if( args[i].compareToIgnoreCase("-EXPAND") == 0 ) {
					adjustmentType = AdjustmentType.EXPAND;
				} else if( args[i].compareToIgnoreCase("-FULL_VIEW") == 0 ) {
					adjustmentType = AdjustmentType.FULL_VIEW;
				} else {
					System.err.println("Unknown flag "+args[i]);
				}
			}

			inputPath = args[numFlags];
			regex = args[numFlags+1];
			pathIntrinsic = args[numFlags+2];
			outputDir = args[numFlags+3];
		}else {
			printHelpAndExit(args);
			System.exit(0);
			return;
		}

		System.out.println("AdjustmentType = "+adjustmentType);
		System.out.println("rename         = "+rename);
		System.out.println("input path     = "+inputPath);
		System.out.println("name regex     = "+regex);
		System.out.println("output dir     = "+outputDir);


		File fileOutputDir = new File(outputDir);
		if( !fileOutputDir.exists() ) {
			if( !fileOutputDir.mkdirs() ) {
				throw new RuntimeException("Output directory did not exist and failed to create it");
			} else {
				System.out.println("  created output directory");
			}
		}

		CameraPinholeRadial param = CalibrationIO.load(pathIntrinsic);
		CameraPinholeRadial paramAdj = new CameraPinholeRadial();

		List<File> files = Arrays.asList(UtilIO.findMatches(new File(inputPath),regex));
		Collections.sort(files);

		System.out.println("Found a total of "+files.size()+" matching files");

		Planar<GrayF32> distoredImg = new Planar<>(GrayF32.class,param.width,param.height,3);
		Planar<GrayF32> undistoredImg = new Planar<>(GrayF32.class,param.width,param.height,3);

		ImageDistort distort = LensDistortionOps.changeCameraModel(adjustmentType, BorderType.ZERO, param,
				new CameraPinhole(param), paramAdj, (ImageType) distoredImg.getImageType());
		CalibrationIO.save(paramAdj,new File(outputDir,"intrinsicUndistorted.yaml").getAbsolutePath());

		BufferedImage out = new BufferedImage(param.width,param.height,BufferedImage.TYPE_INT_RGB);

		int numDigits = BoofMiscOps.numDigits(files.size()-1);
		String format = "%0"+numDigits+"d";
		for( int i = 0; i < files.size(); i++ ) {
			File file = files.get(i);
			System.out.println("processing " + file.getName());
			BufferedImage orig = UtilImageIO.loadImage(file.getAbsolutePath());
			if( orig == null ) {
				throw new RuntimeException("Can't load file: "+file.getAbsolutePath());
			}

			if( orig.getWidth() != param.width || orig.getHeight() != param.height ) {
				System.err.println("intrinsic parameters and image size do not match!");
				System.exit(-1);
			}

			ConvertBufferedImage.convertFromPlanar(orig, distoredImg, true, GrayF32.class);
			distort.apply(distoredImg,undistoredImg);
			ConvertBufferedImage.convertTo(undistoredImg,out,true);

			String nameOut;
			if( rename ) {
				nameOut = String.format("image"+format+".png",i);
			} else {
				nameOut = file.getName().split("\\.")[0]+"_undistorted.png";
			}

			UtilImageIO.saveImage(out,new File(outputDir,nameOut).getAbsolutePath());
		}
	}
}
