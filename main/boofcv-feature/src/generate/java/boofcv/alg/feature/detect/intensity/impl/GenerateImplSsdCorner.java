/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateImplSsdCorner extends CodeGeneratorBase {
	String typeInput;
	String typeOutput;
	String dataInput;
	String dataOutput;
	String sumType;
	String cornerInten;

	public GenerateImplSsdCorner() {
		super.className = "off";
	}

	@Override
	public void generateCode() throws FileNotFoundException {
		createFile(AutoTypeImage.F32, AutoTypeImage.F32);
		createFile(AutoTypeImage.S16, AutoTypeImage.S32);
	}

	public void createFile( AutoTypeImage input, AutoTypeImage output ) throws FileNotFoundException {
		className = null;
		setOutputFile("ImplSsdCorner_" + input.getAbbreviatedType());

		typeInput = input.getSingleBandName();
		typeOutput = output.getSingleBandName();
		dataInput = input.getDataType();
		dataOutput = output.getDataType();
		sumType = input.getSumType();
		cornerInten = "CornerIntensity_" + input.getKernelType();

		printPreamble();
		printHorizontal();
		printVertical();
		printWorkSpace();

		out.println("}");
	}

	private void printPreamble() throws FileNotFoundException {
		out.print("import pabeles.concurrency.GrowArray;\n");
		out.print("import boofcv.struct.image." + typeInput + ";\n");

		if (typeInput.compareTo(typeOutput) != 0)
			out.print("import boofcv.struct.image." + typeOutput + ";\n");
		if (typeInput.compareTo("GrayF32") != 0 && typeOutput.compareTo("GrayF32") != 0) {
			out.print("import boofcv.struct.image.GrayF32;\n");
		}
		out.print("import javax.annotation.Generated;\n\n");
		out.print("//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;\n\n");

		out.print("/**\n" +
				" * <p>\n" +
				" * Implementation of {@link ImplSsdCornerBase} for {@link " + typeInput + "}.\n" +
				" * </p>\n" +
				" * \n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " extends ImplSsdCornerBox<" + typeInput + "," + typeOutput + "> {\n" +
				"\n" +
				"\tprivate GrowArray<WorkSpace> workspaces = new GrowArray<>(() -> new WorkSpace(0));\n" +
				"\tprivate " + cornerInten + " intensity;\n" +
				"\n" +
				"\tpublic " + className + "(int windowRadius, " + cornerInten + " intensity) {\n" +
				"\t\tsuper(windowRadius, "+typeInput+".class, " + typeOutput + ".class);\n" +
				"\t\tthis.intensity = intensity;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tprotected void setImageShape(int imageWidth, int imageHeight) {\n" +
				"\t\tsuper.setImageShape(imageWidth,imageHeight);\n" +
				"\t\tworkspaces = new GrowArray<>(() -> new WorkSpace(imageWidth));\n" +
				"\t}\n\n");
	}

	protected void printHorizontal() {
		out.print("\t/**\n" +
				"\t * Compute the derivative sum along the x-axis while taking advantage of duplicate\n" +
				"\t * calculations for each window.\n" +
				"\t */\n" +
				"\t@Override\n" +
				"\tprotected void horizontal() {\n" +
				"\t\t" + dataInput + "[] dataX = derivX.data;\n" +
				"\t\t" + dataInput + "[] dataY = derivY.data;\n" +
				"\n" +
				"\t\t" + dataOutput + "[] hXX = horizXX.data;\n" +
				"\t\t" + dataOutput + "[] hXY = horizXY.data;\n" +
				"\t\t" + dataOutput + "[] hYY = horizYY.data;\n" +
				"\n" +
				"\t\tfinal int imgHeight = derivX.getHeight();\n" +
				"\t\tfinal int imgWidth = derivX.getWidth();\n" +
				"\n" +
				"\t\tint windowWidth = radius*2 + 1;\n" +
				"\n" +
				"\t\tint radp1 = radius + 1;\n" +
				"\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopFor(0,imgHeight,row->{\n" +
				"\t\tfor (int row = 0; row < imgHeight; row++) {\n" +
				"\n" +
				"\t\t\tint pix = row*imgWidth;\n" +
				"\t\t\tint end = pix + windowWidth;\n" +
				"\n" +
				"\t\t\t" + sumType + " totalXX = 0;\n" +
				"\t\t\t" + sumType + " totalXY = 0;\n" +
				"\t\t\t" + sumType + " totalYY = 0;\n" +
				"\n" +
				"\t\t\tint indexX = derivX.startIndex + row*derivX.stride;\n" +
				"\t\t\tint indexY = derivY.startIndex + row*derivY.stride;\n" +
				"\n" +
				"\t\t\tfor (; pix < end; pix++) {\n" +
				"\t\t\t\t" + dataInput + " dx = dataX[indexX++];\n" +
				"\t\t\t\t" + dataInput + " dy = dataY[indexY++];\n" +
				"\n" +
				"\t\t\t\ttotalXX += dx*dx;\n" +
				"\t\t\t\ttotalXY += dx*dy;\n" +
				"\t\t\t\ttotalYY += dy*dy;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\thXX[pix - radp1] = totalXX;\n" +
				"\t\t\thXY[pix - radp1] = totalXY;\n" +
				"\t\t\thYY[pix - radp1] = totalYY;\n" +
				"\n" +
				"\t\t\tend = row*imgWidth + imgWidth;\n" +
				"\t\t\tfor (; pix < end; pix++, indexX++, indexY++) {\n" +
				"\n" +
				"\t\t\t\t" + dataInput + " dx = dataX[indexX - windowWidth];\n" +
				"\t\t\t\t" + dataInput + " dy = dataY[indexY - windowWidth];\n" +
				"\n" +
				"\t\t\t\t// saving these multiplications in an array to avoid recalculating them made\n" +
				"\t\t\t\t// the algorithm about 50% slower\n" +
				"\t\t\t\ttotalXX -= dx*dx;\n" +
				"\t\t\t\ttotalXY -= dx*dy;\n" +
				"\t\t\t\ttotalYY -= dy*dy;\n" +
				"\n" +
				"\t\t\t\tdx = dataX[indexX];\n" +
				"\t\t\t\tdy = dataY[indexY];\n" +
				"\n" +
				"\t\t\t\ttotalXX += dx*dx;\n" +
				"\t\t\t\ttotalXY += dx*dy;\n" +
				"\t\t\t\ttotalYY += dy*dy;\n" +
				"\n" +
				"\t\t\t\thXX[pix - radius] = totalXX;\n" +
				"\t\t\t\thXY[pix - radius] = totalXY;\n" +
				"\t\t\t\thYY[pix - radius] = totalYY;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_ABOVE });\n" +
				"\t}\n\n");
	}

	public void printWorkSpace() {
		out.print("\n" +
				"\tprivate static class WorkSpace {\n" +
				"\t\tpublic final "+sumType+"[] xx;\n" +
				"\t\tpublic final "+sumType+"[] yy;\n" +
				"\t\tpublic final "+sumType+"[] zz;\n" +
				"\n" +
				"\t\tpublic WorkSpace( int size ) {\n" +
				"\t\t\txx = new "+sumType+"[size];\n" +
				"\t\t\tyy = new "+sumType+"[size];\n" +
				"\t\t\tzz = new "+sumType+"[size];\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public void printVertical() {
		out.print("\t/**\n" +
				"\t * Compute the derivative sum along the y-axis while taking advantage of duplicate\n" +
				"\t * calculations for each window and avoiding cache misses. Then compute the eigen values\n" +
				"\t */\n" +
				"\t@Override\n" +
				"\tprotected void vertical( GrayF32 intensity ) {\n" +
				"\t\t" + sumType + "[] hXX = horizXX.data;\n" +
				"\t\t" + sumType + "[] hXY = horizXY.data;\n" +
				"\t\t" + sumType + "[] hYY = horizYY.data;\n" +
				"\t\tfinal float[] inten = intensity.data;\n" +
				"\n" +
				"\t\tfinal int imgHeight = horizXX.getHeight();\n" +
				"\t\tfinal int imgWidth = horizXX.getWidth();\n" +
				"\n" +
				"\t\tfinal int kernelWidth = radius*2 + 1;\n" +
				"\n" +
				"\t\tfinal int startX = radius;\n" +
				"\t\tfinal int endX = imgWidth - radius;\n" +
				"\n" +
				"\t\tfinal int backStep = kernelWidth*imgWidth;\n" +
				"\n" +
				"\t\tWorkSpace work = workspaces.grow(); //CONCURRENT_REMOVE_LINE\n" +
				"\t\t//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius,imgHeight-radius,workspaces,(work,y0,y1)->{\n" +
				"\t\tint y0 = radius, y1 = imgHeight - radius;\n" +
				"\t\tfinal " + dataOutput + "[] tempXX = work.xx;\n" +
				"\t\tfinal " + dataOutput + "[] tempXY = work.yy;\n" +
				"\t\tfinal " + dataOutput + "[] tempYY = work.zz;\n" +
				"\t\tfor (int x = startX; x < endX; x++) {\n" +
				"\t\t\t// defines the A matrix, from which the eigenvalues are computed\n" +
				"\t\t\tint srcIndex = x + (y0 - radius)*imgWidth;\n" +
				"\t\t\tint destIndex = imgWidth*y0 + x;\n" +
				"\t\t\t" + sumType + " totalXX = 0, totalXY = 0, totalYY = 0;\n" +
				"\n" +
				"\t\t\tint indexEnd = srcIndex + imgWidth*kernelWidth;\n" +
				"\t\t\tfor (; srcIndex < indexEnd; srcIndex += imgWidth) {\n" +
				"\t\t\t\ttotalXX += hXX[srcIndex];\n" +
				"\t\t\t\ttotalXY += hXY[srcIndex];\n" +
				"\t\t\t\ttotalYY += hYY[srcIndex];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\ttempXX[x] = totalXX;\n" +
				"\t\t\ttempXY[x] = totalXY;\n" +
				"\t\t\ttempYY[x] = totalYY;\n" +
				"\n" +
				"\t\t\t// compute the eigen values\n" +
				"\t\t\tinten[destIndex] = this.intensity.compute(totalXX, totalXY, totalYY);\n" +
				"\t\t\tdestIndex += imgWidth;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// change the order it is processed in to reduce cache misses\n" +
				"\t\tfor (int y = y0 + 1; y < y1; y++) {\n" +
				"\t\t\tint srcIndex = (y + radius)*imgWidth + startX;\n" +
				"\t\t\tint destIndex = y*imgWidth + startX;\n" +
				"\n" +
				"\t\t\tfor (int x = startX; x < endX; x++, srcIndex++, destIndex++) {\n" +
				"\t\t\t\t" + sumType + " totalXX = tempXX[x] - hXX[srcIndex - backStep];\n" +
				"\t\t\t\ttempXX[x] = totalXX += hXX[srcIndex];\n" +
				"\t\t\t\t" + sumType + " totalXY = tempXY[x] - hXY[srcIndex - backStep];\n" +
				"\t\t\t\ttempXY[x] = totalXY += hXY[srcIndex];\n" +
				"\t\t\t\t" + sumType + " totalYY = tempYY[x] - hYY[srcIndex - backStep];\n" +
				"\t\t\t\ttempYY[x] = totalYY += hYY[srcIndex];\n" +
				"\n" +
				"\t\t\t\tinten[destIndex] = this.intensity.compute(totalXX, totalXY, totalYY);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t//CONCURRENT_INLINE });\n" +
				"\t}\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplSsdCorner gen = new GenerateImplSsdCorner();
		gen.setModuleName("boofcv-feature");
		gen.generate();
	}
}
