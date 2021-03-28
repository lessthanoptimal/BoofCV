`recognition`  contains algorithms related to recognizing objects in side of an image. Its focus is on information
derived from pixels directly and can utilize machine learning. Geometry is not emphasized and 3D SFM based
recognition approaches do not belong here, hence no dependency on `sfm` but there is a dependency on `geo`.
Use of 3D geometry contained in `geo` is discouraged.

In the future it might make sense to create a new package for approaches which use 3D/perspective geometry combined
with pixel approaches.
