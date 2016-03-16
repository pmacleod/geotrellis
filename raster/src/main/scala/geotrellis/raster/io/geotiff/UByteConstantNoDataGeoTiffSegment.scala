package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

class UByteConstantNoDataCellTypeGeoTiffSegment(bytes: Array[Byte]) extends UByteGeoTiffSegment(bytes) {
  def getInt(i: Int): Int = ub2i(getRaw(i))
  def getDouble(i: Int): Double = ub2d(getRaw(i))

  protected def intToUByteOut(v: Int): Byte = i2ub(v)
  protected def doubleToUByteOut(v: Double): Byte = d2ub(v)
}