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

package boofcv.simulation;

import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I64;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Point tracker that provides perfect tracks. Perfect in that there is no miss association and perfect reprojections.
 * These are generated from a passed in set of 3D points. Intended for use in debugging and unit testing.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PointTrackerPerfectCloud<T extends ImageBase<T>> implements PointTracker<T> {

	// Points in the point cloud
	public List<Point3D_F64> cloud = new ArrayList<>();
	// coordinate transform from world (cloud) into the camera view
	public Se3_F64 world_to_view = new Se3_F64();
	// normalized image coordinates to pixels. The intrinsic camera model
	public Point2Transform2_F64 norm_to_pixel;
	// Image width and height
	public int width, height;

	// Current frame that was most recently processed
	public int frameID;

	//------------ Internal Work Space
	private final Point3D_F64 viewX = new Point3D_F64();
	private final Point2D_F64 pixel = new Point2D_F64();

	private final List<PointTrack> dropped = new ArrayList<>();
	private final List<PointTrack> spawned = new ArrayList<>();

	private final DogArray<Spawnable> spawnable = new DogArray<>(Spawnable::new);
	private final DogArray<PointTrack> activeTracks = new DogArray<>(PointTrack::new, PointTrack::reset);
	private final TIntLongMap cloudIdx_to_id = new TIntLongHashMap();
	private final TLongIntMap id_to_cloudIdx = new TLongIntHashMap();
	private final TLongObjectMap<PointTrack> id_to_track = new TLongObjectHashMap<>();
	private final TLongSet observedID = new TLongHashSet();
	private long totalTracks = 0;

	public PointTrackerPerfectCloud() {
		reset();
	}

	@Override public void process( T image ) {
		Objects.requireNonNull(norm_to_pixel, "You must set norm_to_pixel first");
		BoofMiscOps.checkTrue(width != 0 && height != 0, "You must specify width and height");
		frameID++;
		observedID.clear();
		dropped.clear();
		spawned.clear();
		spawnable.reset();

		for (int cloudIdx = 0; cloudIdx < cloud.size(); cloudIdx++) {
			Point3D_F64 X = cloud.get(cloudIdx);
			world_to_view.transform(X, viewX);
			if (viewX.z <= 0.0)
				continue;
			norm_to_pixel.compute(viewX.x/viewX.z, viewX.y/viewX.z, pixel);
			if (!BoofMiscOps.isInside(width, height, pixel.x, pixel.y))
				continue;

			if (cloudIdx_to_id.containsKey(cloudIdx)) {
				long id = cloudIdx_to_id.get(cloudIdx);
				PointTrack track = id_to_track.get(id);
				// if it has been observed twice, that's a bug
				BoofMiscOps.checkTrue(observedID.add(id));
				// Save the observed pixel coordinate
				track.pixel.setTo(pixel);
				track.lastSeenFrameID = frameID;
				continue;
			}

			// Mark this point as a potential point that can be spawned into a new track
			spawnable.grow().setTo(cloudIdx, pixel);
		}

		// Drop tracks which have not been observed.
		dropUnobserved();
//		System.out.println("active.size="+activeTracks.size+" dropped.size="+dropped.size()+" totalTracks="+totalTracks);
	}

	/**
	 * Drops tracks which were not visible in the FOV
	 */
	void dropUnobserved() {
		// Make a list first to avoid modifying a data structure while traversing through it
		DogArray_I64 dropList = new DogArray_I64();
		for (long id : id_to_track.keys()) { // lint:forbidden ignore_line
			if (!observedID.contains(id)) {
				dropList.add(id);
			}
		}
		// Now actually drop the tracks
		dropList.forEach(id -> {
			PointTrack track = Objects.requireNonNull(id_to_track.remove(id));
			// Don't worry about track being recycled since it won't be recycled until the next call to process
			// and at that point drop is reset
			dropped.add(track);
			BoofMiscOps.checkTrue(activeTracks.remove(track));
			int cloudIdx = id_to_cloudIdx.remove(id);
			cloudIdx_to_id.remove(cloudIdx);
		});
	}

	@Override public void reset() {
		frameID = -1;
		totalTracks = 0;
		activeTracks.reset();
		cloudIdx_to_id.clear();
		id_to_track.clear();
		observedID.clear();
	}

	@Override public long getFrameID() {
		return frameID;
	}

	@Override public int getTotalActive() {
		return activeTracks.size;
	}

	@Override public int getTotalInactive() {
		return 0;
	}

	@Override public void dropAllTracks() {
		activeTracks.reset();
		cloudIdx_to_id.clear();
		id_to_cloudIdx.clear();
		id_to_track.clear();
	}

	@Override public int getMaxSpawn() {
		return 0;
	}

	@Override public boolean dropTrack( PointTrack track ) {
		for (long id : id_to_track.keys()) { // lint:forbidden ignore_line
			if (id_to_track.get(id) == track) {
				id_to_track.remove(id);
				cloudIdx_to_id.remove(id_to_cloudIdx.get(id));
				id_to_cloudIdx.remove(id);
				int index = activeTracks.indexOf(track);
				BoofMiscOps.checkTrue(index != -1, "BUG! Track in map but not array");
				activeTracks.removeSwap(index);
				return true;
			}
		}
		return false;
	}

	@Override public void dropTracks( Dropper dropper ) {
		throw new RuntimeException("Implement when needed");
	}

	@Override public List<PointTrack> getAllTracks( @Nullable List<PointTrack> list ) {
		return getActiveTracks(list);
	}

	@Override public List<PointTrack> getActiveTracks( @Nullable List<PointTrack> list ) {
		if (list == null)
			list = new ArrayList<>();
		list.clear();
		list.addAll(activeTracks.toList());
		return list;
	}

	@Override public List<PointTrack> getInactiveTracks( @Nullable List<PointTrack> list ) {
		if (list == null)
			list = new ArrayList<>();
		list.clear();
		return list;
	}

	@Override public List<PointTrack> getDroppedTracks( @Nullable List<PointTrack> list ) {
		if (list == null)
			list = new ArrayList<>();
		list.clear();
		list.addAll(dropped);
		return list;
	}

	@Override public List<PointTrack> getNewTracks( @Nullable List<PointTrack> list ) {
		if (list == null)
			list = new ArrayList<>();
		list.clear();
		list.addAll(spawned);
		return list;
	}

	@Override public void spawnTracks() {
		spawnable.forEach(spawn -> {
			long id = totalTracks++;
			cloudIdx_to_id.put(spawn.cloudIdx, id);
			PointTrack track = activeTracks.grow();
			track.featureId = id;
			track.detectorSetId = 0;
			track.spawnFrameID = frameID;
			track.lastSeenFrameID = frameID;
			track.pixel.setTo(spawn.pixel);
			id_to_track.put(id, track);
			spawned.add(track);
		});
	}

	@Override public ImageType<T> getImageType() {throw new IllegalArgumentException("Not implemented");}

	public void setCamera( CameraPinhole intrinsic ) {
		norm_to_pixel = new LensDistortionPinhole(intrinsic).distort_F64(false, true);
		width = intrinsic.width;
		height = intrinsic.height;
	}

	public void setCamera( CameraPinholeBrown intrinsic ) {
		norm_to_pixel = new LensDistortionBrown(intrinsic).distort_F64(false, true);
		width = intrinsic.width;
		height = intrinsic.height;
	}

	/** Stores information on a point which can be spawned into a track if requested */
	static class Spawnable {
		public int cloudIdx;
		public Point2D_F64 pixel = new Point2D_F64();

		public void setTo( int cloudIdx, Point2D_F64 pixel ) {
			this.cloudIdx = cloudIdx;
			this.pixel.setTo(pixel);
		}
	}
}
