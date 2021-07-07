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

import boofcv.alg.fiducial.dots.RandomDotMarkerGenerator;
import boofcv.app.dots.CreateRandomDotDocumentImage;
import boofcv.app.dots.CreateRandomDotDocumentPDF;
import boofcv.app.fiducials.CreateFiducialDocumentImage;
import boofcv.app.fiducials.CreateFiducialDocumentPDF;
import boofcv.generate.Unit;
import boofcv.gui.BoofSwingUtil;
import boofcv.io.fiducial.FiducialIO;
import boofcv.io.fiducial.RandomDotDefinition;
import georegression.struct.point.Point2D_F64;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates a printable random dot marker
 *
 * @author Peter Abeles
 */
public class CreateFiducialRandomDot extends BaseFiducialSquare {
	@Option(name = "-n", aliases = {"--DotsPerMarker"}, usage = "Maximum number of dots per marker")
	public int maxDotsPerMarker = 30;

	@Option(name = "-um", aliases = {"--UniqueMarkers"}, usage = "Number of unique markers that it should create")
	public int totalUnique = 1;

	@Option(name = "-dd", aliases = {"--Diameter"}, usage = "The diameter of each dot. In document units")
	public double dotDiameter = 0.5;

	@Option(name = "--SpaceDiameter", usage = "Specify to use space dots using a different diameter. In document units")
	public double spaceDiameter = -1;

	@Option(name = "-rs", aliases = {"--RandomSeed"}, usage = "Random seed used to create the markers")
	public long randomSeed = 0xDEAD_BEEFL;

	@Option(name = "--DumpYaml", usage = "Save marker coordinates into a YAML file")
	public boolean dumpLocations = false;

	@Option(name = "--MarkerBorder", usage = "Draws a black line at the marker's border.")
	public boolean drawLineBorder = false;

	@Option(name = "-h", aliases = {"--MarkerHeight"}, usage = "Height of each marker. In document units. If -1 then square")
	public float markerHeight = -1; // when parsed it will be set to a non-negative value

	List<List<Point2D_F64>> markers = new ArrayList<>();

	@Override
	public void run() throws IOException {
		super.run();

		if (dumpLocations)
			saveMarkersToYaml();
	}

	@Override
	protected void printPdfInfo() {
		super.printPdfInfo();
		System.out.println("   marker height : " + markerHeight + " (" + unit.abbreviation + ")");
	}

	/**
	 * Saves a description of all the markers in a YAML file so that it can be easily read later on
	 */
	private void saveMarkersToYaml() throws IOException {
		String nameYaml = FilenameUtils.getBaseName(fileName) + ".yaml";
		var writer = new OutputStreamWriter(
				new FileOutputStream(new File(new File(fileName).getParentFile(), nameYaml)), UTF_8);

		var def = new RandomDotDefinition();
		def.randomSeed = randomSeed;
		def.dotDiameter = dotDiameter;
		def.maxDotsPerMarker = maxDotsPerMarker;
		def.markerWidth = markerWidth;
		def.markerHeight = markerHeight;
		def.units = unit.getAbbreviation();
		def.markers.addAll(markers);

		FiducialIO.saveRandomDotYaml(def, writer);
		writer.close();
	}

	@Override
	protected CreateFiducialDocumentImage createRendererImage( String filename ) {
		var ret = new CreateRandomDotDocumentImage(filename);
		ret.dotDiameter = dotDiameter;
		ret.markerHeight = (int)markerHeight;
		return ret;
	}

	@Override
	protected CreateFiducialDocumentPDF createRendererPdf( String documentName, PaperSize paper, Unit units ) {
		var ret = new CreateRandomDotDocumentPDF(documentName, paper, units);
		ret.dotDiameter = dotDiameter;
		ret.drawLineBorder = drawLineBorder;
		ret.markerHeight = markerHeight;
		return ret;
	}

	@Override
	protected void callRenderPdf( CreateFiducialDocumentPDF renderer ) throws IOException {
		((CreateRandomDotDocumentPDF)renderer).render(markers, maxDotsPerMarker, randomSeed);
	}

	@Override
	protected void callRenderImage( CreateFiducialDocumentImage renderer ) {
		((CreateRandomDotDocumentImage)renderer).render(markers);
	}

	@Override
	public void finishParsing() {
		super.finishParsing();

		// assume square if height is not specified
		if (markerHeight < 0)
			markerHeight = markerWidth;

		Random rand = new Random(randomSeed);

		double spacingDiameter = this.spaceDiameter <= 0 ? dotDiameter : this.spaceDiameter;

		for (int i = 0; i < totalUnique; i++) {
			List<Point2D_F64> marker = RandomDotMarkerGenerator.createRandomMarker(rand,
					maxDotsPerMarker, markerWidth, markerHeight, spacingDiameter);
			markers.add(marker);
		}
	}

	@Override
	protected void printHelp( CmdLineParser parser ) {
		super.printHelp(parser);

		System.out.println("Creates 3 images in PNG format 500x500 pixels. Circles with a diameter of 21 pixels." +
				" Up to 32 dots per image");
		System.out.println("-w 500 -dd 21 -um 3 -n 32 -o dots.png");
		System.out.println();
		System.out.println("Creates a PDF document the fills in a grid with 4 markers, 8cm wide, 30 dots," +
				" and dumps a description to yaml");
		System.out.println("--MarkerBorder --DumpYaml -w 8 -um 4 -n 30 -o dots.pdf");
		System.out.println();
		System.out.println("Opens a GUI");
		System.out.println("--GUI");

		System.exit(-1);
	}

	public static void main( String[] args ) {
		CreateFiducialRandomDot generator = new CreateFiducialRandomDot();
		CmdLineParser parser = new CmdLineParser(generator);

		if (args.length == 0) {
			generator.printHelp(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				BoofSwingUtil.invokeNowOrLater(() -> new CreateFiducialRandomDotGui(generator));
			} else {
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
