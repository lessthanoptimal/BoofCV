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

import boofcv.app.fiducials.CreateFiducialDocumentImage;
import boofcv.app.fiducials.CreateFiducialDocumentPDF;
import boofcv.app.fiducials.CreateSquareFiducialDocumentImage;
import boofcv.app.fiducials.CreateSquareFiducialDocumentPDF;
import boofcv.gui.BoofSwingUtil;
import org.ddogleg.struct.DogArray_I64;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Outputs an PDF document describing a binary square fiducial that encodes the specified number
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareBinary extends BaseFiducialSquareBorder {

	@Option(name = "-n", aliases = {"--Numbers"}, usage = "Specifies the numbers to encode", handler = LongArrayOptionHandler.class)
	public Long[] numbers = new Long[0];

	@Option(name = "--PatternGridWidth", usage = "Size of grid in the pattern")
	public int gridWidth = 4;

	@Override
	protected void callRenderPdf( CreateFiducialDocumentPDF renderer ) throws IOException {
		List<String> names = new ArrayList<>();
		DogArray_I64 numbers = new DogArray_I64();

		for (int i = 0; i < this.numbers.length; i++) {
			names.add(this.numbers[i].toString());
			numbers.add(this.numbers[i]);
		}

		((CreateSquareFiducialDocumentPDF)renderer).render(names, numbers, gridWidth);
	}

	@Override
	protected void callRenderImage( CreateFiducialDocumentImage renderer ) {
		List<String> names = new ArrayList<>();
		DogArray_I64 numbers = new DogArray_I64();

		for (int i = 0; i < this.numbers.length; i++) {
			names.add(this.numbers[i].toString());
			numbers.add(this.numbers[i]);
		}

		((CreateSquareFiducialDocumentImage)renderer).render(names, numbers, gridWidth);
	}

	@Override
	protected void printHelp( CmdLineParser parser ) {
		super.printHelp(parser);

		System.out.println("Creates three images in PNG format 220x220 pixels, 20 pixel white border");
		System.out.println("-n 101 -n 4932 -n 944 -w 200 -s 20 -o binary.png");
		System.out.println();
		System.out.println("Creates a PDF document the fills in a grid from these three fiducials");
		System.out.println("5cm with 2cm space between fiducials.");
		System.out.println("-n 101 -n 4932 -n 944 -u cm -w 5 -s 2 --GridFill -o binary.pdf");
		System.out.println();
		System.out.println("Opens a GUI");
		System.out.println("--GUI");

		System.exit(-1);
	}

	public static void main( String[] args ) {
		CreateFiducialSquareBinary generator = new CreateFiducialSquareBinary();
		CmdLineParser parser = new CmdLineParser(generator);

		if (args.length == 0) {
			generator.printHelp(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				BoofSwingUtil.invokeNowOrLater(CreateFiducialSquareBinaryGui::new);
			} else {
				if (generator.numbers == null) {
					System.err.println("Must specify at least one number");
					System.exit(1);
				}
				generator.finishParsing();
				generator.run();
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			generator.printHelp(parser);
			System.err.println(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
