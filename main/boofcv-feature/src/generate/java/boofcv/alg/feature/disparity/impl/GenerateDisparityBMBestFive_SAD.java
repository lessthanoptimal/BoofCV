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

package boofcv.alg.feature.disparity.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateDisparityBMBestFive_SAD extends CodeGeneratorBase {

	String typeInput;
	String dataAbr;
	String bitWise;
	String sumType;

	public GenerateDisparityBMBestFive_SAD() {
		super(false);
	}

	@Override
	public void generate() throws FileNotFoundException {
		createFile(AutoTypeImage.U8);
		createFile(AutoTypeImage.S16);
		createFile(AutoTypeImage.U16);
		createFile(AutoTypeImage.F32);
	}

	public void createFile( AutoTypeImage image ) throws FileNotFoundException {
		this.className = null;
		setOutputFile("ImplDisparityScoreBMBestFive_SAD_"+image.getAbbreviatedType());
		typeInput = image.getSingleBandName();
		bitWise = image.getBitWise();
		sumType = image.getSumType();

		dataAbr = image.isInteger() ? "S32" : "F32";

		printPreamble();
		printWorkspace();
		printComputeBlock();
		printComputeFirstRow();
		printComputeRemainingRows();
		printScoreFive();
		printTheRest();

		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.alg.InputSanityCheck;\n" +
				"import boofcv.struct.image.ImageType;\n" +
				"import boofcv.alg.feature.disparity.DisparityBlockMatchBestFive;\n" +
				"import boofcv.alg.feature.disparity.DisparitySelect;\n" +
				"import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.concurrency.IntRangeObjectConsumer;\n" +
				"import boofcv.struct.image."+typeInput+";\n" +
				"import boofcv.struct.image.ImageGray;\n" +
				"import org.ddogleg.struct.FastQueue;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Implementation of {@link boofcv.alg.feature.disparity.DisparityScoreWindowFive} for processing\n" +
				" * images of type {@link "+typeInput+"}.\n" +
				" * </p>\n" +
				" *\n" +
				generateDocString() +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				generatedAnnotation() +
				"public class "+className+"<DI extends ImageGray<DI>>\n" +
				"\t\textends DisparityBlockMatchBestFive<"+typeInput+",DI>\n" +
				"{\n" +
				"\t// Computes disparity from scores\n" +
				"\tDisparitySelect<"+sumType+"[], DI> disparitySelect0;\n" +
				"\n" +
				"\t// reference to input images;\n" +
				"\t"+typeInput+" left, right;\n" +
				"\tDI disparity;\n" +
				"\n" +
				"\tFastQueue workspace = new FastQueue<>(WorkSpace.class, WorkSpace::new);\n" +
				"\tComputeBlock computeBlock = new ComputeBlock();\n" +
				"\n" +
				"\tpublic "+className+"(int minDisparity, int maxDisparity,\n" +
				"\t\t\t\t\t\t\t\t\t\t\tint regionRadiusX, int regionRadiusY,\n" +
				"\t\t\t\t\t\t\t\t\t\t\tDisparitySelect<"+sumType+"[], DI> computeDisparity) {\n" +
				"\t\tsuper(minDisparity,maxDisparity,regionRadiusX,regionRadiusY);\n" +
				"\t\tthis.disparitySelect0 = computeDisparity;\n" +
				"\t\tworkspace.grow();\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic void _process("+typeInput+" left , "+typeInput+" right , DI disparity ) {\n" +
				"\t\tInputSanityCheck.checkSameShape(left,right);\n" +
				"\t\tdisparity.reshape(left.width,left.height);\n" +
				"\t\tthis.left = left;\n" +
				"\t\tthis.right = right;\n" +
				"\t\tthis.disparity = disparity;\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\tBoofConcurrency.loopBlocks(0,left.height,regionHeight,workspace,computeBlock);\n" +
				"\t\t} else {\n" +
				"\t\t\tcomputeBlock.accept((WorkSpace)workspace.get(0),0,left.height);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printWorkspace() {
		out.print("\tclass WorkSpace {\n" +
				"\t\t// stores the local scores for the width of the region\n" +
				"\t\t"+sumType+"[] elementScore;\n" +
				"\t\t// scores along horizontal axis for current block\n" +
				"\t\t"+sumType+"[][] horizontalScore;\n" +
				"\t\t// summed scores along vertical axis\n" +
				"\t\t// Save the last regionHeight scores in a rolling window\n" +
				"\t\t"+sumType+"[][] verticalScore;\n" +
				"\t\t// In the rolling verticalScore window, which one is the active one\n" +
				"\t\tint activeVerticalScore;\n" +
				"\t\t// Where the final score it stored that has been computed from five regions\n" +
				"\t\t"+sumType+"[] fiveScore;\n" +
				"\n" +
				"\t\tDisparitySelect<"+sumType+"[], DI> computeDisparity;\n" +
				"\n" +
				"\t\tpublic void checkSize() {\n" +
				"\t\t\tif( horizontalScore == null || verticalScore.length < lengthHorizontal ) {\n" +
				"\t\t\t\thorizontalScore = new "+sumType+"[regionHeight][lengthHorizontal];\n" +
				"\t\t\t\tverticalScore = new "+sumType+"[regionHeight][lengthHorizontal];\n" +
				"\t\t\t\telementScore = new "+sumType+"[ left.width ];\n" +
				"\t\t\t\tfiveScore = new "+sumType+"[ lengthHorizontal ];\n" +
				"\t\t\t}\n" +
				"\t\t\tif( computeDisparity == null ) {\n" +
				"\t\t\t\tcomputeDisparity = disparitySelect0.concurrentCopy();\n" +
				"\t\t\t}\n" +
				"\t\t\tcomputeDisparity.configure(disparity,minDisparity,maxDisparity,radiusX*2);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printComputeBlock() {
		out.print("\tprivate class ComputeBlock implements IntRangeObjectConsumer<WorkSpace> {\n" +
				"\t\t@Override\n" +
				"\t\tpublic void accept(WorkSpace workspace, int minInclusive, int maxExclusive)\n" +
				"\t\t{\n" +
				"\t\t\tworkspace.checkSize();\n" +
				"\n" +
				"\t\t\t// The image border will be skipped, so it needs to back track some\n" +
				"\t\t\tint row0 = Math.max(0,minInclusive-2*radiusY);\n" +
				"\t\t\tint row1 = Math.min(left.height,maxExclusive+2*radiusY);\n" +
				"\n" +
				"\t\t\t// initialize computation\n" +
				"\t\t\tcomputeFirstRow(row0, workspace);\n" +
				"\n" +
				"\t\t\t// efficiently compute rest of the rows using previous results to avoid repeat computations\n" +
				"\t\t\tcomputeRemainingRows(row0,row1, workspace);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printComputeFirstRow() {
		out.print("\t/**\n" +
				"\t * Initializes disparity calculation by finding the scores for the initial block of horizontal\n" +
				"\t * rows.\n" +
				"\t */\n" +
				"\tprivate void computeFirstRow( final int row0 , final WorkSpace workSpace ) {\n" +
				"\t\tfinal "+sumType+" firstRow[] = workSpace.verticalScore[0];\n" +
				"\t\tworkSpace.activeVerticalScore = 1;\n" +
				"\n" +
				"\t\t// compute horizontal scores for first row block\n" +
				"\t\tfor( int row = 0; row < regionHeight; row++ ) {\n" +
				"\n" +
				"\t\t\t"+sumType+" scores[] = workSpace.horizontalScore[row];\n" +
				"\n" +
				"\t\t\tUtilDisparityScore.computeSadDispRow(left, right, row0+row, scores,\n" +
				"\t\t\t\t\tminDisparity, maxDisparity, regionWidth, workSpace.elementScore);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// compute score for the top possible row\n" +
				"\t\tfor( int i = 0; i < lengthHorizontal; i++ ) {\n" +
				"\t\t\t"+sumType+" sum = 0;\n" +
				"\t\t\tfor( int row = 0; row < regionHeight; row++ ) {\n" +
				"\t\t\t\tsum += workSpace.horizontalScore[row][i];\n" +
				"\t\t\t}\n" +
				"\t\t\tfirstRow[i] = sum;\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printComputeRemainingRows() {
		out.print("\t/**\n" +
				"\t * Using previously computed results it efficiently finds the disparity in the remaining rows.\n" +
				"\t * When a new block is processes the last row/column is subtracted and the new row/column is\n" +
				"\t * added.\n" +
				"\t */\n" +
				"\tprivate void computeRemainingRows(final int row0 , final int row1, final WorkSpace workSpace )\n" +
				"\t{\n" +
				"\t\tfor( int row = row0+regionHeight; row < row1; row++ , workSpace.activeVerticalScore++) {\n" +
				"\t\t\tint oldRow = (row-row0)%regionHeight;\n" +
				"\t\t\t"+sumType+" previous[] = workSpace.verticalScore[ (workSpace.activeVerticalScore -1) % regionHeight ];\n" +
				"\t\t\t"+sumType+" active[] = workSpace.verticalScore[ workSpace.activeVerticalScore % regionHeight ];\n" +
				"\n" +
				"\t\t\t// subtract first row from vertical score\n" +
				"\t\t\t"+sumType+" scores[] = workSpace.horizontalScore[oldRow];\n" +
				"\t\t\tfor( int i = 0; i < lengthHorizontal; i++ ) {\n" +
				"\t\t\t\tactive[i] = previous[i] - scores[i];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tUtilDisparityScore.computeSadDispRow(left, right, row, scores,\n" +
				"\t\t\t\t\tminDisparity,maxDisparity,regionWidth,workSpace.elementScore);\n" +
				"\n" +
				"\t\t\t// add the new score\n" +
				"\t\t\tfor( int i = 0; i < lengthHorizontal; i++ ) {\n" +
				"\t\t\t\tactive[i] += scores[i];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tif( workSpace.activeVerticalScore >= regionHeight-1 ) {\n" +
				"\t\t\t\t"+sumType+" top[] = workSpace.verticalScore[ (workSpace.activeVerticalScore -2*radiusY) % regionHeight ];\n" +
				"\t\t\t\t"+sumType+" middle[] = workSpace.verticalScore[ (workSpace.activeVerticalScore -radiusY) % regionHeight ];\n" +
				"\t\t\t\t"+sumType+" bottom[] = workSpace.verticalScore[workSpace. activeVerticalScore % regionHeight ];\n" +
				"\n" +
				"\t\t\t\tcomputeScoreFive(top,middle,bottom,workSpace.fiveScore,left.width);\n" +
				"\t\t\t\tworkSpace.computeDisparity.process(row - (1 + 4*radiusY) + 2*radiusY+1, workSpace.fiveScore );\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printScoreFive() {
		out.print("\t/**\n" +
				"\t * Compute the final score by sampling the 5 regions.  Four regions are sampled around the center\n" +
				"\t * region.  Out of those four only the two with the smallest score are used.\n" +
				"\t */\n" +
				"\tprotected void computeScoreFive( "+sumType+" top[] , "+sumType+" middle[] , "+sumType+" bottom[] , "+sumType+" score[] , int width ) {\n" +
				"\n" +
				"\t\t// disparity as the outer loop to maximize common elements in inner loops, reducing redundant calculations\n" +
				"\t\tfor( int d = minDisparity; d < maxDisparity; d++ ) {\n" +
				"\n" +
				"\t\t\t// take in account the different in image border between the sub-regions and the effective region\n" +
				"\t\t\tint indexSrc = (d-minDisparity)*width + (d-minDisparity) + radiusX;\n" +
				"\t\t\tint indexDst = (d-minDisparity)*width + (d-minDisparity);\n" +
				"\t\t\tint end = indexSrc + (width-d-4*radiusX);\n" +
				"\t\t\twhile( indexSrc < end ) {\n" +
				"\t\t\t\tint s = 0;\n" +
				"\n" +
				"\t\t\t\t// sample four outer regions at the corners around the center region\n" +
				"\t\t\t\t"+sumType+" val0 = top[indexSrc-radiusX];\n" +
				"\t\t\t\t"+sumType+" val1 = top[indexSrc+radiusX];\n" +
				"\t\t\t\t"+sumType+" val2 = bottom[indexSrc-radiusX];\n" +
				"\t\t\t\t"+sumType+" val3 = bottom[indexSrc+radiusX];\n" +
				"\n" +
				"\t\t\t\t// select the two best scores from outer for regions\n" +
				"\t\t\t\tif( val1 < val0 ) {\n" +
				"\t\t\t\t\t"+sumType+" temp = val0;\n" +
				"\t\t\t\t\tval0 = val1;\n" +
				"\t\t\t\t\tval1 = temp;\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tif( val3 < val2 ) {\n" +
				"\t\t\t\t\t"+sumType+" temp = val2;\n" +
				"\t\t\t\t\tval2 = val3;\n" +
				"\t\t\t\t\tval3 = temp;\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tif( val3 < val0 ) {\n" +
				"\t\t\t\t\ts += val2;\n" +
				"\t\t\t\t\ts += val3;\n" +
				"\t\t\t\t} else if( val2 < val1 ) {\n" +
				"\t\t\t\t\ts += val2;\n" +
				"\t\t\t\t\ts += val0;\n" +
				"\t\t\t\t} else {\n" +
				"\t\t\t\t\ts += val0;\n" +
				"\t\t\t\t\ts += val1;\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tscore[indexDst++] = s + middle[indexSrc++];\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public void printTheRest() {
		out.print("\t@Override\n" +
				"\tpublic ImageType<"+typeInput+"> getInputType() {\n" +
				"\t\treturn ImageType.single("+typeInput+".class);\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic Class<DI> getDisparityType() {\n" +
				"\t\treturn disparitySelect0.getDisparityType();\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateDisparityBMBestFive_SAD gen = new GenerateDisparityBMBestFive_SAD();

		gen.generate();
	}
}