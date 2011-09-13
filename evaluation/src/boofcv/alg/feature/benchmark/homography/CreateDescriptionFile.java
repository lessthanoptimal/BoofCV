/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.benchmark.homography;

import boofcv.abst.feature.describe.ExtractFeatureDescription;
import boofcv.alg.feature.orientation.OrientationImage;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;
import pja.dev.cv.IntensityGraphDesc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import static pja.dev.cv.FactoryDescribeNewStructure.*;


/**
 * Creates a file containing all the detected feature location and scales.
 *
 * @author Peter Abeles
 */
public class CreateDescriptionFile<T extends ImageBase> {

	// algorithm that detects the features
	ExtractFeatureDescription<T> alg;
	// estimates the feature's orientation
	OrientationImage<T> orientation;
	// type of input image
	Class<T> imageType;
	// name of the description
	String descriptionName;

	public CreateDescriptionFile(ExtractFeatureDescription<T> alg,
								 OrientationImage<T> orientation ,
								 Class<T> imageType,
								 String descriptionName ) {
		this.alg = alg;
		this.orientation = orientation;
		this.imageType = imageType;
		this.descriptionName = descriptionName;
	}

	public void directory( String path , String imageSuffix , String detectionSuffix ) throws FileNotFoundException {
		File dir = new File(path);
		if( !dir.isDirectory() )
			throw new IllegalArgumentException("Path does not point to a directory!");

		File[] files = dir.listFiles();
		for( File f : files ) {
			if( !f.isFile() || !f.getName().endsWith(imageSuffix))
				continue;

			System.out.println("Describing features inside of: "+f.getName());

			String fullPath = f.getPath();
			String baseName = fullPath.substring(0,fullPath.length()-imageSuffix.length());

			File detectionFile = new File(baseName+detectionSuffix);
			if( !detectionFile.exists() )
				throw new RuntimeException("Detection file does not exist");

			BufferedImage image = UtilImageIO.loadImage(fullPath);
			process(image,detectionFile.getPath(),baseName+"_"+descriptionName+".txt");
		}
	}

	public void process( BufferedImage input , String detectionName , String outputName ) throws FileNotFoundException {
		T image = ConvertBufferedImage.convertFrom(input,null,imageType);

		alg.setImage(image);
		orientation.setImage(image);

		List<DetectionInfo> detections = LoadBenchmarkFiles.loadDetection(detectionName);

		FileOutputStream fos = new FileOutputStream(outputName);
		PrintStream out = new PrintStream(fos);

		out.printf("%d\n",alg.getDescriptionLength());
		for( DetectionInfo d : detections  ) {
			Point2D_I32 p = d.location;
			double theta=0;
			if( alg.requiresOrientation() ) {
				orientation.setScale(d.scale);
				theta = orientation.compute(p.x,p.y);
			}
			TupleDesc_F64 desc = alg.process(p.x,p.y,theta,d.scale,null);
			if( desc != null ) {
				out.printf("%d %d",p.getX(),p.getY());
				for( int i = 0; i < desc.value.length; i++ ) {
					out.printf(" %.10f",desc.value[i]);
				}
				out.println();
			}
		}
		out.close();
	}

	public static <T extends ImageBase>
	void doStuff( String directory , String imageSuffix , Class<T> imageType ) throws FileNotFoundException {
//		ExtractFeatureDescription<T> alg = FactoryExtractFeatureDescription.surf(true,imageType);
		OrientationImage<T> orientation = FactoryOrientationAlgs.nogradient(12,imageType);
//		CreateDescriptionFile<T> cdf = new CreateDescriptionFile<T>(alg,orientation,imageType,"BoofCV_SURF");
//		cdf.directory(directory,imageSuffix,"_FH.txt");

		int radius = 12;
		int numAngles = 8;
		int numJoints = 2;
		IntensityGraphDesc graph = createCircle(radius,numAngles,numJoints);
		connectSpiderWeb(numAngles,numJoints,graph);
		ExtractFeatureDescription<T> alg = wrap(graph,imageType);
		CreateDescriptionFile<T> cdf = new CreateDescriptionFile<T>(alg,orientation,imageType,"NEW");
		cdf.directory(directory,imageSuffix,"_FH.txt");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		doStuff("evaluation/data/mikolajczk/ubc/",".png",ImageFloat32.class);
	}

}
