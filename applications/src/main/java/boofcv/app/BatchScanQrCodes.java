/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import java.util.LinkedList;
import java.util.Queue;

/**
 * Scans all images in a directory for QR codes and outputs the results
 *
 * @author Peter Abeles
 */
public class BatchScanQrCodes {

	@Option(name = "-i", aliases = {"--Input"}, usage="Path to input directory or file")
	String pathInput;

	@Option(name = "-o", aliases = {"--Output"}, usage="Path to output file.")
	String pathOutput = "qrcodes.txt";

	@Option(name = "--Regex", usage="Optional regex to filter files by name")
	String regex = "";

	@Option(name = "--Recursive", usage="Should input directory be recursively searched")
	boolean recursive = false;

	@Option(name="--GUI", usage="Ignore all other command line arguments and switch to GUI mode")
	private boolean guiMode = false;

	QrCodeDetector<GrayU8> scanner = FactoryFiducial.qrcode(null,GrayU8.class);
	GrayU8 gray = new GrayU8(1,1);

	PrintStream output;

	BatchControlPanel.Listener listener;

	int total;

	void finishParsing() {

	}

	void process() throws FileNotFoundException, UnsupportedEncodingException {
		total = 0;
		output = new PrintStream(pathOutput);
		output.println("# Found QR Codes inside of images");
		output.println("# "+new File(pathInput).getPath());
		output.println("# Format:");
		output.println("# <File Name> <Total Found>");
		output.println("# message encoded with URLEncoder");

		try {
			Queue<File> files = new LinkedList<>();
			files.add(new File(pathInput));

			while (!files.isEmpty()) {
				File f = files.remove();
				if (!f.exists()) {
					System.err.println("Does not exist: " + f.getPath());
				} else {
					if (f.isFile()) {
						processFile(f);
					} else {
						File[] children = f.listFiles();
						if (children == null)
							continue;
						for (File c : children) {
							if (c.isFile()) {
								processFile(c);
							} else if (recursive) {
								files.add(c);
							}
						}
					}
				}
			}
		} finally {
			output.close();
		}
		System.out.println("\n\nDone! Images Count = "+total);
	}

	private void processFile(File f) throws UnsupportedEncodingException {
		if( regex.length() > 0 && !f.getName().matches(regex))
			return;

		BufferedImage buffered = UtilImageIO.loadImage(f.getAbsolutePath());
		if( buffered == null ) {
			System.err.println("Can't open "+f.getPath());
			return;
		}
		if( listener != null ) {
			listener.batchUpdate(f.getName());
		}

		ConvertBufferedImage.convertFrom(buffered,gray);

		scanner.process(gray);
		output.printf("%d %s\n",scanner.getDetections().size(),f.getPath());

		for (QrCode qr : scanner.getDetections()) {
			output.println(URLEncoder.encode(qr.message,"UTF-8"));
		}

		total++;
		if( total%50 == 0 ) {
			System.out.println("processed "+total);
		}
	}

	private static void printHelpExit(CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);

		System.out.println();
		System.out.println("Examples:");
		System.out.println();
		System.out.println("--Recursive -i /path/to/directory -o myresults.txt");
		System.out.println("--Recursive --Regex \"\\w*\\.jpg\" -i /path/to/directory -o myresults.txt");

		System.exit(1);
	}

	public static void main(String[] args) {
		BatchScanQrCodes generator = new BatchScanQrCodes();
		CmdLineParser parser = new CmdLineParser(generator);

		if( args.length == 0 ) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);
			if( generator.guiMode ) {
				new BatchScanQrCodesGui();
			} else {
				generator.finishParsing();
				try {
					generator.process();
				} catch( Exception e ) {
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
