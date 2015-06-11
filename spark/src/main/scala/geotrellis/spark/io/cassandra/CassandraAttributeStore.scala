package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import spray.json._
import DefaultJsonProtocol._

import com.datastax.driver.core.DataType.text
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{set, eq => eqs}
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.{ResultSet, Session}

import org.apache.spark.Logging
import scala.collection.JavaConversions._

object CassandraAttributeStore {
  def apply(attributeTable: String)(implicit session: CassandraSession): CassandraAttributeStore =
    new CassandraAttributeStore(attributeTable)
}

class CassandraAttributeStore(val attributeTable: String)(implicit session: CassandraSession) extends AttributeStore with Logging {
  type ReadableWritable[T] = RootJsonFormat[T]

  // Create the attribute table if it doesn't exist
  {
    val schema = SchemaBuilder.createTable(session.keySpace, attributeTable).ifNotExists()
      .addPartitionKey("layerId", text)
      .addClusteringColumn("name", text)
      .addColumn("value", text)

    session.execute(schema)
  }

  private def fetch(layerId: Option[LayerId], attributeName: String): ResultSet = {
    val query = 
      layerId match {
        case Some(id) => 
          QueryBuilder.select.column("value")
            .from(session.keySpace, attributeTable)
            .where (eqs("layerId", id.toString))
            .and   (eqs("name", attributeName))
        case None =>
          QueryBuilder.select.column("value")
            .from(session.keySpace, attributeTable)
            .where(eqs("name", attributeName))
      }

    session.execute(query)
  }

  def read[T: RootJsonFormat](layerId: LayerId, attributeName: String): T = {
    val query = 
      QueryBuilder.select.column("value")
        .from(session.keySpace, attributeTable)
        .where (eqs("layerId", layerId.toString))
        .and   (eqs("name", attributeName))

    val values = session.execute(query)

    val size = values.getAvailableWithoutFetching
    if(size == 0) {
      throw new Attribute4LayerNotFoundError(attributeName, layerId)
    } else if (size > 1) {
      throw new MultipleAttributesError(attributeName, layerId)
    } else {
      val (_, result) = values.one.getString("value").parseJson.convertTo[(LayerId, T)]
      result
    }
  }

  def readAll[T: RootJsonFormat](attributeName: String): Map[LayerId,T] = {
    val query = 
      QueryBuilder.select.column("value")
        .from(session.keySpace, attributeTable)
        .where(eqs("name", attributeName))

    session.execute(query)
      .all
      .map { _.getString("value").parseJson.convertTo[(LayerId, T)] }
      .toMap
  }

  def write[T: RootJsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    val update = 
      QueryBuilder.update(session.keySpace, attributeTable)
        .`with`(set("value", (layerId, value).toJson.compactPrint))
        .where (eqs("layerId", layerId.toString))
        .and   (eqs("name", attributeName))

    val results = session.execute(update)
  }
}