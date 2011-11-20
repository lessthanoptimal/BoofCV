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

package boofcv.benchmark.feature.homography;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.feature.orientation.OrientationImage;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;


/**
 * Creates a file describing each detected image feature.  The input directory is scanned for images
 * and when is found the specified detection file is loaded.
 *
 * @author Peter Abeles
 */
public class CreateDescriptionFile<T extends ImageBase> {

	// algorithm that detects the features
	DescribeRegionPoint<T> alg;
	// estimates the feature's orientation
	OrientationImage<T> orientation;
	// type of input image
	Class<T> imageType;
	// name of the description
	String descriptionName;

	/**
	 * Defines the set of images and detection files that are to be processed.
	 *
	 * @param alg Algorithm which creates a description for the feature.
	 * @param orientation Algorithm which estimates the orientation.
	 * @param imageType Type of input file.
	 * @param descriptionName The name of the description algorithm.  This name is appended to output files.
	 */
	public CreateDescriptionFile(DescribeRegionPoint<T> alg,
								 OrientationImage<T> orientation ,
								 Class<T> imageType,
								 String descriptionName ) {
		this.alg = alg;
		this.orientation = orientation;
		this.imageType = imageType;
		this.descriptionName = descriptionName;
	}

	/**
	 * Scans a all the files in a directory looking for matching image files.  Once a match is found it then
	 * looks up the corresponding detection file.  For each input image a description of all the detected
	 * features is saved to a file.
	 *
	 * @param pathToDirectory Location of the directory being searched.
	 * @param imageSuffix Input image type.  All images have this suffix.
	 * @param detectionSuffix Name of the detection log.
	 * @throws FileNotFoundException
	 */
	public void directory( String pathToDirectory , String imageSuffix , String detectionSuffix ) throws FileNotFoundException {
		File dir = new File(pathToDirectory);
		if( !dir.isDirectory() )
			throw new IllegalArgumentException("Path does not point to a directory!");

		System.out.println("Directory: "+pathToDirectory);
		int filesFound = 0;
		File[] files = dir.listFiles();
		for( File f : files ) {
			if( !f.isFile() || !f.getName().endsWith(imageSuffix))
				continue;

			System.out.println("Describing features inside of: "+f.getName());

			String imageName = f.getName();
			String directoryPath = f.getParent();
			imageName = imageName.substring(0,imageName.length()-imageSuffix.length());

			File detectionFile = new File(directoryPath+"/DETECTED_"+imageName+"_"+detectionSuffix);
			if( !detectionFile.exists() )
				throw new RuntimeException("Detection file does not exist");

			BufferedImage image = UtilImageIO.loadImage(f.getPath());
			process(image,detectionFile.getPath(),directoryPath+"/DESCRIBE_"+imageName+"_"+descriptionName+".txt");
			filesFound++;
		}
		System.out.println("Total files processed: "+filesFound);
	}

	/**
	 * Given the input image, load the specified detection file and save the description of each feature.
	 *
	 * @param input Image being processed.
	 * @param detectionName Path to detection file.
	 * @param outputName Path to output file.
	 * @throws FileNotFoundException
	 */
	public void process( BufferedImage input , String detectionName , String outputName ) throws FileNotFoundException {
		T image = ConvertBufferedImage.convertFrom(input,null,imageType);

		alg.setImage(image);
		orientation.setImage(image);

		List<DetectionInfo> detections = LoadBenchmarkFiles.loadDetection(detectionName);

		FileOutputStream fos = new FileOutputStream(outputName);
		PrintStream out = new PrintStream(fos);

		out.printf("%d\n",alg.getDescriptionLength());
		for( DetectionInfo d : detections  ) {
			Point2D_F64 p = d.location;
			double theta=0;
			if( alg.requiresOrientation() ) {
				orientation.setScale(d.scale);
				theta = orientation.compute(p.x,p.y);
			}
			TupleDesc_F64 desc = process(p.x, p.y, theta, d.scale);
			if( desc != null ) {
				// save the location and tuple description
				out.printf("%.3f %.3f %f",p.getX(),p.getY(),theta);
				for( int i = 0; i < desc.value.length; i++ ) {
					out.printf(" %.10f",desc.value[i]);
				}
				out.println();
			}
		}
		out.close();
	}

	protected TupleDesc_F64 process( double x , double y , double theta , double scale )
	{
		return alg.process(x,y,theta,scale,null);
	}

	public static <T extends ImageBase>
	void doStuff( String directory , String imageSuffix , Class<T> imageType ) throws FileNotFoundException {
//		DescribeRegionPoint<T> alg = FactoryDescribeRegionPoint.surf(true,imageType);
		DescribeRegionPoint<T> alg = FactoryDescribeRegionPoint.surfm(true, imageType);

//		int radius = 12;
//		int numAngles = 8;
//		int numJoints = 2;
//		IntensityGraphDesc graph = createCircle(radius,numAngles,numJoints);
//		connectSpiderWeb(numAngles,numJoints,graph);
//		DescribeRegionPoint<T> alg = wrap(graph,imageType);

//		DescribeRegionPoint<T> alg = FactoryDescribeRegionPoint.brief(16, 512, -1, 4, true, imageType);

//		DescribeRegionPoint<T> alg = FactoryDescribeRegionPoint.brief(16,512,-1,4,false,imageType);
//		DescribeRegionPoint<T> alg = DescribePointSamples.create(imageType);
//		DescribeRegionPoint<T> alg = DescribeSampleDifference.create(imageType);

		OrientationImage<T> orientation = FactoryOrientationAlgs.nogradient(alg.getCanonicalRadius(),imageType);
//		CreateDescriptionFile<T> cdf = new CreateDescriptionFile<T>(alg,orientation,imageType,"SAMPLEDIFF");
//		CreateDescriptionFile<T> cdf = new CreateDescriptionFile<T>(alg,orientation,imageType,"SAMPLE");
//		CreateDescriptionFile<T> cdf = new CreateDescriptionFile<T>(alg,orientation,imageType,"BoofCV_SURF");
		CreateDescriptionFile<T> cdf = new CreateDescriptionFile<T>(alg,orientation,imageType,"BoofCV_MSURF");
//		CreateDescriptionFile<T> cdf = new CreateDescriptionFile<T>(alg,orientation,imageType,"BRIEFO");
//		CreateDescriptionFile<T> cdf = new CreateDescriptionFile<T>(alg,orientation,imageType,"BRIEF");
//		CreateDescriptionFile<T> cdf = new CreateDescriptionFile<T>(alg,orientation,imageType,"NEW");
		cdf.directory(directory,imageSuffix,"FH.txt");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		doStuff("data/mikolajczk/bikes/",".png",ImageFloat32.class);
		doStuff("data/mikolajczk/boat/",".png",ImageFloat32.class);
		doStuff("data/mikolajczk/graf/",".png",ImageFloat32.class);
		doStuff("data/mikolajczk/leuven/",".png",ImageFloat32.class);
		doStuff("data/mikolajczk/ubc/",".png",ImageFloat32.class);
		doStuff("data/mikolajczk/trees/",".png",ImageFloat32.class);
		doStuff("data/mikolajczk/wall/",".png",ImageFloat32.class);
		doStuff("data/mikolajczk/bark/",".png",ImageFloat32.class);
	}
}
