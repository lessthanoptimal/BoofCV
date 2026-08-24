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

/*
 * Copyright (C) 2009-2015 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bytedeco.copiedstuff;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for {@link FFmpegFrameGrabber}.
 *
 * <p>Trimmed from JavaCV for BoofCV. Everything BoofCV does not use was removed: the static grabber
 * registry and {@code createDefault} factories, the {@code PropertyEditor}, the multi-grabber
 * {@code Array} synchronizer, {@code delayedGrab}, trigger mode, and every audio property. What is
 * left is the property bag that {@link FFmpegFrameGrabber} reads plus the grab lifecycle. See the
 * readme in this package.</p>
 *
 * @author Samuel Audet
 */
public abstract class FrameGrabber implements Closeable {

    public enum ImageMode {
        COLOR, GRAY
    }

    protected int videoStream = -1;
    protected String format = null;
    protected int imageWidth = 0, imageHeight = 0;
    protected ImageMode imageMode = ImageMode.COLOR;
    protected int pixelFormat = -1, videoCodec, videoBitrate = 0;
    protected double aspectRatio = 0, frameRate = 0;
    protected double gamma = 0.0;
    protected HashMap<String, String> options = new HashMap<>();
    protected HashMap<String, String> videoOptions = new HashMap<>();
    protected HashMap<String, String> metadata = new HashMap<>();
    protected HashMap<String, String> videoMetadata = new HashMap<>();
    protected int frameNumber = 0;
    protected long timestamp = 0;

    public int getVideoStream() {
        return videoStream;
    }
    public void setVideoStream(int videoStream) {
        this.videoStream = videoStream;
    }

    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }

    public int getImageWidth() {
        return imageWidth;
    }
    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }
    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public ImageMode getImageMode() {
        return imageMode;
    }
    public void setImageMode(ImageMode imageMode) {
        this.imageMode = imageMode;
    }

    public int getPixelFormat() {
        return pixelFormat;
    }
    public void setPixelFormat(int pixelFormat) {
        this.pixelFormat = pixelFormat;
    }

    public int getVideoCodec() {
        return videoCodec;
    }
    public void setVideoCodec(int videoCodec) {
        this.videoCodec = videoCodec;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }
    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }
    public void setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public double getFrameRate() {
        return frameRate;
    }
    public void setFrameRate(double frameRate) {
        this.frameRate = frameRate;
    }

    public double getGamma() {
        return gamma;
    }
    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public String getOption(String key) {
        return options.get(key);
    }
    public void setOption(String key, String value) {
        options.put(key, value);
    }

    public String getVideoOption(String key) {
        return videoOptions.get(key);
    }
    public void setVideoOption(String key, String value) {
        videoOptions.put(key, value);
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getMetadata(String key) {
        return metadata.get(key);
    }
    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }

    public String getVideoMetadata(String key) {
        return videoMetadata.get(key);
    }
    public void setVideoMetadata(String key, String value) {
        videoMetadata.put(key, value);
    }

    public int getFrameNumber() {
        return frameNumber;
    }
    public void setFrameNumber(int frameNumber) throws Exception {
        this.frameNumber = frameNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) throws Exception {
        this.timestamp = timestamp;
    }

    public int getLengthInFrames() {
        return 0;
    }
    public long getLengthInTime() {
        return 0;
    }

    public static class Exception extends IOException {
        public Exception(String message) { super(message); }
        public Exception(String message, Throwable cause) { super(message, cause); }
    }

    public abstract void start() throws Exception;
    public abstract void stop() throws Exception;
    public abstract void release() throws Exception;

    @Override public void close() throws Exception {
        stop();
        release();
    }

    /**
     * Each call to grab stores the new image in the memory address for the previously returned frame. <br/>
     * IE.<br/>
     * <code>
     * grabber.grab() == grabber.grab()
     * </code>
     * <br/>
     * This means that if you need to cache images returned from grab you should {@link Frame#clone()} the
     * returned frame as the next call to grab will overwrite your existing image's memory.
     * <br/>
     * <b>Why?</b><br/>
     * Using this method instead of allocating a new buffer every time a frame
     * is grabbed improves performance by reducing the frequency of garbage collections.
     * Almost no additional heap space is typically allocated per frame.
     *
     * @return The frame returned from the grabber
     * @throws Exception If there is a problem grabbing the frame.
     */
    public abstract Frame grab() throws Exception;

    public void restart() throws Exception {
        stop();
        start();
    }
}
