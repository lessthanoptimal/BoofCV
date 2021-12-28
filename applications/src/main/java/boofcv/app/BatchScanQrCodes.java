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

package boofcv.app;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.app.batch.BatchControlPanel;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Scans all images in a directory for QR codes and outputs the results
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class BatchScanQrCodes {

	@Option(name = "-i", aliases = {"--Input"}, usage = "Directory or glob pattern or regex pattern.\n" +
			"Glob example: 'glob:data/**/left*.jpg'\n" +
			"Regex example: 'regex:data/\\w+/left\\d+.jpg'\n" +
			"If not a pattern then it's assumed to be a path. All files with known image extensions in their name as added, e.g. jpg, png")
	String inputPattern;

	@Option(name = "-o", aliases = {"--Output"}, usage = "Path to output file.")
	String pathOutput = "qrcodes.txt";

	@Option(name = "--GUI", usage = "Ignore all other command line arguments and switch to GUI mode")
	boolean guiMode = false;

	QrCodeDetector<GrayU8> scanner = FactoryFiducial.qrcode(null, GrayU8.class);
	GrayU8 gray = new GrayU8(1, 1);

	PrintStream output;

	BatchControlPanel.Listener listener;

	int total;

	@Option(name = "--Verbose", usage = "Prints out verbose debugging information")
	boolean verbose = false;

	void finishParsing() {}

	void process() throws FileNotFoundException, UnsupportedEncodingException {
		total = 0;
		output = new PrintStream(pathOutput);
		output.println("# Found QR Codes inside of images");
		output.println("# " + new File(inputPattern).getPath());
		output.println("# Format:");
		output.println("# <File Name> <Total Found>");
		output.println("# message encoded with URLEncoder");

		List<String> inputs = UtilIO.listSmartImages(inputPattern, false);

		if (inputs.isEmpty()) {
			System.err.println("No inputs found. Bad path or pattern? " + inputPattern);
			return;
		}

		for (String path : inputs) {
			processFile(new File(path));
		}
		if (verbose)
			System.out.println("\n\nDone! Images Count = " + total);
	}

	private void processFile( File f ) throws UnsupportedEncodingException {
		BufferedImage buffered = UtilImageIO.loadImage(f.getAbsolutePath());
		if (buffered == null) {
			System.err.println("Can't open " + f.getPath());
			return;
		}
		if (listener != null) {
			listener.batchUpdate(f.getName());
		}

		ConvertBufferedImage.convertFrom(buffered, gray);

		scanner.process(gray);
		output.printf("%d %s\n", scanner.getDetections().size(), f.getPath());

		for (QrCode qr : scanner.getDetections()) {
			output.println(URLEncoder.encode(qr.message, "UTF-8"));
		}

		total++;
		if (total%50 == 0) {
			if (verbose)
				System.out.println("processed " + total);
		}
	}

	private static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);

		System.out.println();
		System.out.println("Examples:");
		System.out.println();
		System.out.println("-i /path/to/directory -o myresults.txt");
		System.out.println("   Finds all images in 'path/to' directory");
		System.out.println("-i \"regex:path/to/\\w+\\.jpg\" -o myresults.txt");
		System.out.println("   Finds all files ending with .jpg in 'path/to' directory");
		System.out.println("-i \"glob:path/to/*.jpg\" -o myresults.txt");
		System.out.println("   Finds all files ending with .jpg in 'path/to' directory");
		System.out.println("-i \"glob:path/**/*\" -o myresults.txt");
		System.out.println("   Recursively search all directories starting at 'path' for images");

		System.exit(1);
	}

	public static void main( String[] args ) {
		BatchScanQrCodes generator = new BatchScanQrCodes();
		CmdLineParser parser = new CmdLineParser(generator);

		if (args.length == 0) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				new BatchScanQrCodesGui();
			} else {
				generator.finishParsing();
				try {
					generator.process();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println();
					System.out.println("Failed! See exception above");
				}
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		}
	}
}
