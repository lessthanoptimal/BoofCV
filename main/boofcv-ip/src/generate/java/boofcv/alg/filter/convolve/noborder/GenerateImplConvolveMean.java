/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve.noborder;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

/// Generates ImplConvolveMean. Functions take a range of rows, so ImplConvolveMean_MT can call them on each block
public class GenerateImplConvolveMean extends CodeGeneratorBase {

	public AutoTypeImage imageIn;
	public AutoTypeImage imageOut;

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		addFunctions(AutoTypeImage.U8, AutoTypeImage.I8);
		addFunctions(AutoTypeImage.S16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.U16, AutoTypeImage.I16);
		addFunctions(AutoTypeImage.F32, AutoTypeImage.F32);
		addFunctions(AutoTypeImage.F64, AutoTypeImage.F64);
		out.println("}");
	}

	public void addFunctions( AutoTypeImage imageIn, AutoTypeImage imageOut ) {
		this.imageIn = imageIn;
		this.imageOut = imageOut;
		printHorizontal();
		printVertical();
		printPixel();
		printCopyRow();
		if (!imageIn.isInteger())
			printDivide();
	}

	public void printPreamble() {
		autoSelectName();
		out.print("""
				import boofcv.struct.border.ImageBorder_F32;
				import boofcv.struct.border.ImageBorder_F64;
				import boofcv.struct.border.ImageBorder_S32;
				import boofcv.struct.image.*;
				import org.ddogleg.struct.DogArray;
				import org.ddogleg.struct.DogArrayPrimitive;
				import org.ddogleg.struct.DogArray_F32;
				import org.ddogleg.struct.DogArray_F64;
				import org.ddogleg.struct.DogArray_I32;
				import org.jetbrains.annotations.Nullable;

				import javax.annotation.Generated;
				import java.util.Arrays;

				/**
				 * Convolves a mean filter across the image. Each output pixel is the mean of 'length' pixels, starting 'offset'
				 * pixels before it. Near the image border the mean is of the pixels inside the image, unless an
				 * {@link boofcv.struct.border.ImageBorder} gives the pixels outside the image. Each function also has a version for
				 * a range of rows, which {@link ImplConvolveMean_MT} calls on each block.
				 *
				 * <p>The vertical pass keeps the last 'length' input rows in a ring of row buffers, so every array in the inner loop
				 * has the same index and the loop can be vectorized. S16 and U16 row buffers are double so the division can be
				 * vectorized, since x86-64 has no vector integer division. Their sums are integers below 2^53, so double arithmetic
				 * is exact in any order and (int)(sum/length) equals integer division. U8 divides with an exact multiply and
				 * shift.</p>
				""" +
				generateDocString("Peter Abeles") +
				"""
				@SuppressWarnings({"ForLoopReplaceableByForEach", "Duplicates"})
				public class CLASS_NAME {

					/// Resizes the vertical pass's buffers: length + 1 ring rows, totals and the row being written
					private static void declareRows( DogArray<? extends DogArrayPrimitive<?>> rows, int length, int width ) {
						rows.resize(length + 3);
						for (int k = 0; k < rows.size; k++) {
							rows.get(k).resize(width);
						}
					}

				""".replace("CLASS_NAME", className));
	}

	//------------------------------------------------------------------------------------------------------------
	// Strings for the current pair of image types

	String nameIn() {return imageIn.getSingleBandName();}

	String nameOut() {return imageOut.getSingleBandName();}

	String sumType() {return imageIn.getSumType();}

	String bitWise() {return imageIn.getBitWise();}

	String castOut() {return imageOut.getTypeCastFromSum();}

	boolean isU8() {return imageIn == AutoTypeImage.U8;}

	/// ImageBorder type which provides pixels outside the image
	String borderType() {
		if (imageIn.isInteger())
			return "ImageBorder_S32<" + nameIn() + ">";
		return "ImageBorder_" + imageIn.getAbbreviatedType();
	}

	/// Primitive type of the vertical pass's row buffers
	String rowType() {
		if (!imageIn.isInteger())
			return sumType();
		return isU8() ? "int" : "double";
	}

	public String rowArray() {
		return switch (rowType()) {
			case "int" -> "DogArray_I32";
			case "float" -> "DogArray_F32";
			case "double" -> "DogArray_F64";
			default -> throw new IllegalArgumentException(rowType());
		};
	}

	public String apply( String template ) {
		return template.
				replace("NAME_IN", nameIn()).
				replace("NAME_OUT", nameOut()).
				replace("SUM_TYPE", sumType()).
				replace("ROW_TYPE", rowType()).
				replace("ROW_ARRAY", rowArray()).
				replace("BORDER_TYPE", borderType()).
				replace("BIT_WISE", bitWise()).
				replace("CAST_OUT", castOut());
	}

	/// Declares the variables for an exact U8 division with a multiply and shift
	static final String U8_DIVISION = """
					// (n*multiplier) >>> 23 equals n/length while n*error < 2^23
					final int shift = 23;
					final long multiplier = ((1L << shift) + length - 1)/length;
					final long maxNumerator = 255L*length + length/2;
					final boolean exact = maxNumerator*(multiplier*length - (1L << shift)) < (1L << shift) &&
							maxNumerator*multiplier < (1L << 32);
					final int m = (int)multiplier;
			""";

	//------------------------------------------------------------------------------------------------------------
	// Horizontal

	public void printHorizontal() {
		out.print(apply("""
					/// Mean along x. See the class description for the border
					public static void horizontal( NAME_IN input, NAME_OUT output, int offset, int length,
												   @Nullable BORDER_TYPE border ) {
						horizontal(input, output, offset, length, border, 0, input.height);
					}

					/// horizontal() of the rows y0 to y1
					public static void horizontal( NAME_IN input, NAME_OUT output, int offset, int length,
												   @Nullable BORDER_TYPE border, int y0, int y1 ) {
						final int width = input.width;
						final int offsetR = length - offset - 1;
				"""));
		out.print(apply("""
						// pixels outside the image to the left and right of a row
						final SUM_TYPE[] outside = border == null ? null : new SUM_TYPE[offset + offsetR];

						for (int y = y0; y < y1; y++) {
							final int indexIn = input.startIndex + y*input.stride;
							final int indexOut = output.startIndex + y*output.stride;
							if (border != null) {
								for (int i = 0; i < offset; i++) {
									outside[i] = border.get(i - offset, y);
								}
								for (int i = 0; i < offsetR; i++) {
									outside[offset + i] = border.get(width + i, y);
								}
							}

							if (border == null && length <= width) {
								borderInside(input, output, offset, length, indexIn, indexOut);
								continue;
							}

							// Running sums along the left and then the right border. Without a border only pixels inside
							// the image are counted
							for (int side = 0; side < 2; side++) {
								final int xStart = side == 0 ? 0 : Math.max(Math.min(offset, width), width - offsetR);
								final int xEnd = side == 0 ? Math.min(offset, width) : width;
								SUM_TYPE total = 0;
								int count = 0;
								for (int x = xStart; x < xEnd; x++) {
									if (x == xStart) {
										for (int i = x - offset; i < x - offset + length; i++) {
											total += pixel(input, indexIn, i, outside, offset);
											count += i >= 0 && i < width ? 1 : 0;
										}
									} else {
										final int removed = x - offset - 1, added = x - offset + length - 1;
										total -= pixel(input, indexIn, removed, outside, offset);
										total += pixel(input, indexIn, added, outside, offset);
										count += (added < width ? 1 : 0) - (removed >= 0 ? 1 : 0);
									}
				"""));
		if (imageIn.isInteger())
			out.print(apply("""
										final int divisor = border == null ? count : length;
										output.data[indexOut + x] = CAST_OUT((total + divisor/2)/divisor);
					"""));
		else
			out.print("""
										output.data[indexOut + x] = total/(border == null ? count : length);
					""");
		out.print("""
								}
							}
						}
						horizontalInner(input, output, offset, length, y0, y1);
					}

				""");
		printBorderInside();
		out.print(apply("""
					/// horizontal() without the border
					public static void horizontalInner( NAME_IN input, NAME_OUT output, int offset, int length ) {
						horizontalInner(input, output, offset, length, 0, input.height);
					}

					/// horizontalInner() of the rows y0 to y1
					public static void horizontalInner( NAME_IN input, NAME_OUT output, int offset, int length, int y0, int y1 ) {
						final int width = input.width;
						if (length > width)
							return;
				"""));
		if (imageIn.isInteger())
			out.print("\t\tfinal int half = length/2;\n");
		else
			out.print(apply("\t\tfinal SUM_TYPE divisor = length;\n"));
		if (imageIn.isInteger())
			printHorizontalInnerInteger();
		else
			printHorizontalInnerFloat();
		out.print("\t}\n\n");
	}

	/// Border of one row without an ImageBorder, when the kernel fits inside the image: the mean of the pixels inside
	/// the image, with a running sum along each side
	private void printBorderInside() {
		String mean = imageIn.isInteger() ? "CAST_OUT((total + count/2)/count)" : "total/count";
		out.print(apply("""
					private static void borderInside( NAME_IN input, NAME_OUT output, int offset, int length, int indexIn,
													  int indexOut ) {
						final int width = input.width;
						final int offsetR = length - offset - 1;
						if (offset > 0) {
							SUM_TYPE total = 0;
							int count = length - offset;
							for (int i = 0; i < count; i++) {
								total += input.data[indexIn + i]BIT_WISE;
							}
							output.data[indexOut] = MEAN;
							for (int x = 1; x < offset; x++) {
								total += input.data[indexIn + x + offsetR]BIT_WISE;
								count++;
								output.data[indexOut + x] = MEAN;
							}
						}
						if (offsetR > 0) {
							final int x0 = width - offsetR;
							SUM_TYPE total = 0;
							int count = offsetR + offset;
							for (int i = x0 - offset; i < width; i++) {
								total += input.data[indexIn + i]BIT_WISE;
							}
							output.data[indexOut + x0] = MEAN;
							for (int x = x0 + 1; x < width; x++) {
								total -= input.data[indexIn + x - offset - 1]BIT_WISE;
								count--;
								output.data[indexOut + x] = MEAN;
							}
						}
					}

				""".replace("MEAN", mean)));
	}

	/// Inner columns of integer images. U8 divides with a multiply and shift when that's exact
	private void printHorizontalInnerInteger() {
		if (isU8())
			out.print(U8_DIVISION);
		out.print(apply("""
						for (int y = y0; y < y1; y++) {
							int indexIn = input.startIndex + input.stride*y;
							int indexOut = output.startIndex + output.stride*y + offset;

							int total = 0;
							int indexEnd = indexIn + length;
							for (; indexIn < indexEnd; indexIn++) {
								total += input.data[indexIn]BIT_WISE;
							}
							output.data[indexOut++] = CAST_OUT((total + half)/length);

							indexEnd = indexIn + width - length;
				"""));
		if (isU8())
			out.print(apply("""
								if (exact) {
									for (; indexIn < indexEnd; indexIn++) {
										total -= input.data[indexIn - length]BIT_WISE;
										total += input.data[indexIn]BIT_WISE;
										output.data[indexOut++] = CAST_OUT(((total + half)*m) >>> shift);
									}
									continue;
								}
					"""));
		out.print(apply("""
							for (; indexIn < indexEnd; indexIn++) {
								total -= input.data[indexIn - length]BIT_WISE;
								total += input.data[indexIn]BIT_WISE;
								output.data[indexOut++] = CAST_OUT((total + half)/length);
							}
						}
				"""));
	}

	/// Two rows at a time. total += added - removed puts one addition per pixel on the dependency chain, and the two
	/// rows' chains run in parallel. The sums are written to the output, then divided in a loop that can be vectorized
	private void printHorizontalInnerFloat() {
		out.print(apply("""
						final int strideIn = input.stride, strideOut = output.stride;
						final int n = width - length + 1;
						for (int y = y0; y < y1; y += 2) {
							// the second row, or the same row again if only one is left
							final boolean pair = y + 1 < y1;
							final int nextIn = pair ? strideIn : 0, nextOut = pair ? strideOut : 0;
							int indexIn = input.startIndex + strideIn*y;
							final int indexOut0 = output.startIndex + strideOut*y + offset;
							int indexOut = indexOut0;

							SUM_TYPE total0 = 0, total1 = 0;
							for (int i = indexIn; i < indexIn + length; i++) {
								total0 += input.data[i];
								total1 += input.data[i + nextIn];
							}
							output.data[indexOut] = total0;
							output.data[indexOut + nextOut] = total1;

							final int indexEnd = indexIn + width;
							for (int i = indexIn + length; i < indexEnd; i++) {
								indexOut++;
								total0 += input.data[i] - input.data[i - length];
								total1 += input.data[i + nextIn] - input.data[i + nextIn - length];
								output.data[indexOut] = total0;
								output.data[indexOut + nextOut] = total1;
							}
							divide(output.data, indexOut0, n, divisor);
							if (pair)
								divide(output.data, indexOut0 + strideOut, n, divisor);
						}
				"""));
	}

	/// Divides n elements of a row in place. Same index on both sides, so it can be vectorized
	private void printDivide() {
		out.print(apply("""
					private static void divide( SUM_TYPE[] data, int index, int n, SUM_TYPE divisor ) {
						final int end = index + n;
						for (int i = index; i < end; i++) {
							data[i] = data[i]/divisor;
						}
					}

				"""));
	}

	//------------------------------------------------------------------------------------------------------------
	// Vertical

	public void printVertical() {
		boolean floats = !imageIn.isInteger();
		out.print(apply("""
					/// Mean along y. See the class description for the border
					///
					/// @param rows Work space for the row buffers
					public static void vertical( NAME_IN input, NAME_OUT output, int offset, int length,
												 @Nullable BORDER_TYPE border, DogArray<ROW_ARRAY> rows ) {
						vertical(input, output, offset, length, border, 0, input.height, rows);
					}

				"""));
		out.print(apply(floats ? """
					/// vertical() of the output rows y0 to y1. The running sums start at y0 and every
					/// ImplConvolveBox.VERTICAL_RESTART rows, see ImplConvolveBox.restartRow()
				""" : """
					/// vertical() of the output rows y0 to y1. The running sums start at the block's first inner row
				"""));
		out.print(apply("""
					public static void vertical( NAME_IN input, NAME_OUT output, int offset, int length,
												 @Nullable BORDER_TYPE border, int y0, int y1, DogArray<ROW_ARRAY> rows ) {
						final int width = input.width, height = input.height;
						final int offsetR = length - offset - 1;

						declareRows(rows, length, width);
						final ROW_TYPE[] totals = rows.get(length + 1).data;
						final ROW_TYPE[] row = rows.get(length + 2).data;

						// Border rows. Pixels outside the image are fetched once into the ring rows
						if (border != null) {
							if (y0 < offset) {
								for (int j = -offset; j < 0; j++) {
									fetchRow(border, width, j, rows.get(j + offset).data);
								}
							}
							if (y1 > height - offsetR) {
								for (int j = height; j < height + offsetR; j++) {
									fetchRow(border, width, j, rows.get(offset + j - height).data);
								}
							}
						}
				"""));
		out.print(apply("""
						for (int y = y0; y < y1; y++) {
							if (y >= offset && y < height - offsetR) {
								y = height - offsetR - 1;
								continue;
							}
							Arrays.fill(totals, 0, width, 0);
							int count = 0;
							for (int k = 0; k < length; k++) {
								final int j = y - offset + k;
								final ROW_TYPE[] values;
								if (j >= 0 && j < height) {
									copyRow(input, width, j, row);
									values = row;
									count++;
								} else if (border != null) {
									values = rows.get(j < 0 ? j + offset : offset + j - height).data;
								} else {
									continue;
								}
				"""));
		String mean = floats ? "totals[i]/divisor" : "CAST_OUT((int)(totals[i] + divisor/2)/divisor)";
		out.print(apply("""
								for (int i = 0; i < width; i++) {
									totals[i] += values[i];
								}
							}
							final int divisor = border == null ? count : length;
							final int indexOut = output.startIndex + y*output.stride;
							for (int i = 0; i < width; i++) {
								output.data[indexOut + i] = MEAN;
							}
						}
				""".replace("MEAN", mean)));
		out.print("""

						verticalInner(input, output, offset, length, y0, y1, rows);
					}

				""");
		out.print(apply("""
					/// vertical() without the border
					public static void verticalInner( NAME_IN input, NAME_OUT output, int offset, int length,
													  DogArray<ROW_ARRAY> rows ) {
						verticalInner(input, output, offset, length, 0, input.height, rows);
					}

					/// verticalInner() of the output rows y0 to y1
					public static void verticalInner( NAME_IN input, NAME_OUT output, int offset, int length, int y0, int y1,
													  DogArray<ROW_ARRAY> rows ) {
						final int offsetR = length - offset - 1;
						final int inner0 = Math.max(y0, offset), inner1 = Math.min(y1, input.height - offsetR);
						if (inner0 >= inner1)
							return;
						declareRows(rows, length, input.width);
				"""));
		if (floats)
			out.print("""
							final int restart = ImplConvolveBox.VERTICAL_RESTART;
							for (int start = inner0; start < inner1; ) {
								// next multiple of 'restart' rows after the first inner row
								int end = offset + ((start - offset)/restart + 1)*restart;
								end = Math.min(end, inner1);
								verticalBlock(input, output, offset, length, start, end, rows);
								start = end;
							}
						}

					""");
		else
			out.print("""
							verticalBlock(input, output, offset, length, inner0, inner1, rows);
						}

					""");
		printVerticalBlock();
		printFetchRow();
	}

	private void printVerticalBlock() {
		out.print(apply("""
					/// Inner output rows y0 to y1 with running sums that start at y0
					private static void verticalBlock( NAME_IN input, NAME_OUT output, int offset, int length, int y0, int y1,
													   DogArray<ROW_ARRAY> rows ) {
						final int n = input.width;
						final ROW_TYPE[] totals = rows.get(length + 1).data;
						final ROW_TYPE[] quotients = rows.get(length + 2).data;
				INNER_DIVISION

						// Input row k is in buffer (k - first)%ringSize
						final int first = y0 - offset;
						final int ringSize = length + 1;
						for (int k = 0; k < length; k++) {
							copyRow(input, n, first + k, rows.get(k).data);
						}
						System.arraycopy(rows.get(0).data, 0, totals, 0, n);
						for (int k = 1; k < length; k++) {
							final ROW_TYPE[] row = rows.get(k).data;
							for (int i = 0; i < n; i++) {
								totals[i] += row[i];
							}
						}

						for (int y = y0; y < y1; y++) {
							if (y > y0) {
								final ROW_TYPE[] removed = rows.get((y - offset - 1 - first)%ringSize).data;
								final ROW_TYPE[] added = rows.get((y - offset + length - 1 - first)%ringSize).data;
								copyRow(input, n, y - offset + length - 1, added);
								for (int i = 0; i < n; i++) {
				""").replace("INNER_DIVISION\n", innerDivision()));
		if (imageIn.isInteger())
			out.print("""
										totals[i] = totals[i] - removed[i] + added[i];
					""");
		else
			out.print(apply("""
										ROW_TYPE total = totals[i] - removed[i];
										totals[i] = total + added[i];
					"""));
		out.print("""
								}
							}
							final int indexOut = output.startIndex + y*output.stride;
				""");
		if (isU8()) {
			out.print("""
								if (exact) {
									for (int i = 0; i < n; i++) {
										quotients[i] = ((totals[i] + half)*m) >>> shift;
									}
								} else {
									for (int i = 0; i < n; i++) {
										quotients[i] = (totals[i] + half)/length;
									}
								}
								for (int i = 0; i < n; i++) {
									output.data[indexOut + i] = (byte)quotients[i];
								}
							}
						}

					""");
		} else if (imageIn.isInteger()) {
			out.print("""
								for (int i = 0; i < n; i++) {
									quotients[i] = (totals[i] + half)/divisor;
								}
								for (int i = 0; i < n; i++) {
									output.data[indexOut + i] = (short)(int)quotients[i];
								}
							}
						}

					""");
		} else {
			out.print("""
								for (int i = 0; i < n; i++) {
									quotients[i] = totals[i]/divisor;
								}
								System.arraycopy(quotients, 0, output.data, indexOut, n);
							}
						}

					""");
		}
	}

	/// Declarations of the division in verticalBlock()
	String innerDivision() {
		if (isU8())
			return "\t\tfinal int half = length/2;\n" + U8_DIVISION;
		if (imageIn.isInteger())
			return "\t\tfinal int half = length/2;\n\t\tfinal double divisor = length;\n";
		return "\t\tfinal " + rowType() + " divisor = length;\n";
	}

	Set<String> printedFetchRow = new HashSet<>();

	private void printFetchRow() {
		// integer images share ImageBorder_S32, so one for each border and row type
		String border = imageIn.isInteger() ? "ImageBorder_S32<?>" : borderType();
		if (!printedFetchRow.add(border + rowType()))
			return;
		out.print(apply("""
					/// Pixels of row j, which is outside the image, from the border
					private static void fetchRow( BORDER, int width, int j, ROW_TYPE[] row ) {
						for (int i = 0; i < width; i++) {
							row[i] = border.get(i, j);
						}
					}

				""".replace("BORDER", border + " border")));
	}

	private void printPixel() {
		out.print(apply("""
					/// Pixel i of the row starting at indexIn. Outside the image it's from 'outside', or 0 if that's null
					private static SUM_TYPE pixel( NAME_IN input, int indexIn, int i, @Nullable SUM_TYPE[] outside, int offset ) {
						final int width = input.width;
						if (i >= 0 && i < width)
							return input.data[indexIn + i]BIT_WISE;
						if (outside == null)
							return 0;
						return outside[i < 0 ? i + offset : offset + i - width];
					}

				"""));
	}

	/// Copies the first n pixels of row y into 'row'. One for each input type
	private void printCopyRow() {
		out.print(apply("""
					private static void copyRow( NAME_IN input, int n, int y, ROW_TYPE[] row ) {
						final int indexIn = input.startIndex + y*input.stride;
				"""));
		if (imageIn.getDataType().equals(rowType()))
			out.print("\t\tSystem.arraycopy(input.data, indexIn, row, 0, n);\n");
		else
			out.print(apply("""
							for (int i = 0; i < n; i++) {
								row[i] = input.data[indexIn + i]BIT_WISE;
							}
					"""));
		out.print("\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var generator = new GenerateImplConvolveMean();
		generator.setModuleName("boofcv-ip");
		generator.generateCode();
	}
}
