/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Loads a set of images, resizes them to a smaller size using an intelligent algorithm then saves them.
 *
 * @author Peter Abeles
 */
public class BatchDownsizeImage {

	@Option(name = "-i", aliases = {"--Input"},
			usage="Path to input directory")
	String pathInput;
	@Option(name = "-o", aliases = {"--Output"}, usage="Path to output directory")
	String pathOutput;
	@Option(name = "-r", aliases = {"--Regex"}, usage="Regex. Example: .*\\.jpg")
	String regex;
	@Option(name = "--Rename", usage="Rename files")
	boolean rename;
	@Option(name = "--Recursive", usage="Should input directory be recursively searched")
	boolean recursive;
	@Option(name="--GUI", usage="Ignore all other command line arguments and switch to GUI mode")
	private boolean guiMode = false;
	@Option(name = "-w", aliases = {"--Width"}, usage="Sets output width. If zero then aspect is matched with height")
	int width=0;
	@Option(name = "-h", aliases = {"--Height"}, usage="Sets output width. If zero then aspect is matched with height")
	int height=0;

	Listener listener;
	boolean cancel;

	public static void printHelpExit(CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);

		System.out.println();
		System.out.println("Examples:");
		System.out.println();
		System.exit(1);
	}

	public void process() {
		cancel = false;
		if( width == 0 && height == 0 ) {
			throw new RuntimeException("Need to specify at least a width or height");
		}

		System.out.println("width          = "+ width);
		System.out.println("height         = "+ height);
		System.out.println("rename         = "+ rename);
		System.out.println("input path     = "+ pathInput);
		System.out.println("name regex     = "+ regex);
		System.out.println("output dir     = "+ pathOutput);

		List<File> files = Arrays.asList(UtilIO.findMatches(new File(pathInput),regex));
		Collections.sort(files);

		// Create the output directory if it doesn't exist
		if( !new File(pathOutput).exists() ) {
			new File(pathOutput).mkdirs();
		}

		Planar<GrayU8> planar = new Planar<>(GrayU8.class,1,1,1);
		Planar<GrayU8> small = new Planar<>(GrayU8.class,1,1,1);
		int numDigits = BoofMiscOps.numDigits(files.size()-1);
		String format = "%0"+numDigits+"d";
		for( int i = 0; i < files.size(); i++ ) {
			File file = files.get(i);
			System.out.print("processing " + file.getName());
			BufferedImage orig = UtilImageIO.loadImage(file.getAbsolutePath());
			if( orig == null ) {
				throw new RuntimeException("Can't load file: "+file.getAbsolutePath());
			}

			int smallWidth,smallHeight;
			if( width == 0 ) {
				smallWidth = orig.getWidth()*height/orig.getHeight();
			} else {
				smallWidth = width;
			}
			if( height == 0 ) {
				smallHeight = orig.getHeight()*width/orig.getWidth();
			} else {
				smallHeight = height;
			}
			System.out.println("   "+smallWidth+" x "+smallHeight);

			if( smallWidth > orig.getWidth() || smallHeight > orig.getHeight() ) {
				System.out.println("Skipping "+file.getName()+" because it is too small");
			}

			if( listener != null )
				listener.loadedImage(orig,file.getName());

			String nameOut;
			if( rename ) {
				nameOut = String.format("image"+format+".png",i);
			} else {
				nameOut = file.getName().split("\\.")[0]+"_small.png";
			}

			planar.reshape(orig.getWidth(),orig.getHeight());
			ConvertBufferedImage.convertFrom(orig, planar, true);

			small.reshape(smallWidth,smallHeight,planar.getNumBands());

			AverageDownSampleOps.down(planar,small);

			BufferedImage output = ConvertBufferedImage.convertTo(small,null,true);

			UtilImageIO.saveImage(output,new File(pathOutput,nameOut).getAbsolutePath());

			if( cancel ) {
				break;
			}
		}
		if( listener != null )
			listener.finishedConverting();
	}

	public interface Listener {
		void loadedImage( BufferedImage image , String name );

		void finishedConverting();
	}

	public static void main(String[] args) {
		BatchDownsizeImage generator = new BatchDownsizeImage();
		CmdLineParser parser = new CmdLineParser(generator);

		if( args.length == 0 ) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);
			if( generator.guiMode ) {
				new BatchDownsizeImageGui();
			} else {
				generator.process();
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		}
	}
}
