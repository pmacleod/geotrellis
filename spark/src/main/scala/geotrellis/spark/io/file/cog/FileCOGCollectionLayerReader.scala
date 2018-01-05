/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.file.KeyPathGenerator
import geotrellis.util._

import spray.json.JsonFormat
import com.typesafe.config.ConfigFactory

import java.net.URI
import java.io.File

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from local FS.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class FileCOGCollectionLayerReader(
  val attributeStore: AttributeStore,
  val catalogPath: String,
  val defaultThreads: Int = ConfigFactory.load().getThreads("geotrellis.file.threads.collection.read")
)
  extends COGCollectionLayerReader[LayerId] with LazyLogging {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V]): ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]], indexFilterOnly: Boolean) = {
    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      KeyPathGenerator(catalogPath, s"${id.name}/${zoomRange.slug}", maxWidth) andThen (_ ++ s".$Extension")

    baseRead[K, V](
      id              = id,
      tileQuery       = tileQuery,
      indexFilterOnly = indexFilterOnly,
      getKeyPath      = getKeyPath,
      pathExists      = { new File(_).isFile },
      fullPath        = { path => new URI(s"file://$path") },
      defaultThreads  = defaultThreads
    )
  }
}

