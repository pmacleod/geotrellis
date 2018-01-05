package geotrellis.spark.io

import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector.Extent

import org.apache.spark.util.AccumulatorV2
import java.util

package object cog extends Implicits with TiffMethodsImplicits {
  type MetadataAccumulator[M] = AccumulatorV2[(Int, M), util.Map[Int, M]]

  val GTKey     = "GT_KEY"
  val Extension = "tiff"

  implicit class withExtentMethods(extent: Extent) {
    def bufferByLayout(layout: LayoutDefinition): Extent =
      Extent(
        extent.xmin + layout.cellwidth / 2,
        extent.ymin + layout.cellheight / 2,
        extent.xmax - layout.cellwidth / 2,
        extent.ymax - layout.cellheight / 2
      )
  }
}