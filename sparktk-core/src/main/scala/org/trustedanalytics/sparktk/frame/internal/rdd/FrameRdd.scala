/**
 *  Copyright (c) 2016 Intel Corporation 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.trustedanalytics.sparktk.frame.internal.rdd

import breeze.linalg.DenseVector
import org.apache.spark.mllib.linalg.distributed.IndexedRow
import org.apache.spark.mllib.linalg.{Vector, Vectors, DenseVector => MllibDenseVector}
import org.apache.spark.mllib.stat.{MultivariateStatisticalSummary, Statistics}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.apache.spark.sql.types.{DataTypes => _, _}
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.{Partition, TaskContext}
import org.trustedanalytics.sparktk.frame.DataTypes._
import org.trustedanalytics.sparktk.frame._
import org.trustedanalytics.sparktk.frame.internal.{FrameState, RowWrapper}

import scala.collection.immutable.{Vector => ScalaVector}
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * A Frame RDD is a SchemaRDD with our version of the associated schema.
 * 一个Frame RDD是SchemaRDD与我们的版本相关的模式
 * This is our preferred format for loading frames as RDDs.
 * 这是我们用于将frames加载为RDD的首选格式
 * @param frameSchema  the schema describing the columns of this frame
  *                     描述这个框架的列的模式
 */
class FrameRdd(val frameSchema: Schema, val prev: RDD[Row])
    extends RDD[Row](prev) {

  // Represents self in FrameRdd and its sub-classes
  //在FrameRdd及其子类中表示self
  type Self = this.type

  /**
   * A Frame RDD is a SchemaRDD with our version of the associated schema
   * 框架RDD是SchemaRDD与我们的相关模式的版本
   * @param schema  the schema describing the columns of this frame
    *                描述这个框架的列的模式
   * @param dataframe an existing schemaRDD that this FrameRdd will represent
    *                  此FrameRdd将表示的现有schemaRDD
   */
  def this(schema: Schema, dataframe: DataFrame) = this(schema, dataframe.rdd)

  /* Number of columns in frame
  * 框架中的列数*/
  val numColumns = frameSchema.columns.size

  /** This wrapper provides richer API for working with Rows
    * 这个包装提供了更丰富的API来处理行*/
  val rowWrapper = new RowWrapper(frameSchema)

  /**
   * Spark schema representation of Frame Schema to be used with Spark SQL APIs
    * 用于Spark SQL API的框架模式的Spark模式表示
   */
  lazy val sparkSchema = FrameRdd.schemaToStructType(frameSchema)

  /**
   * Convert a FrameRdd to a Spark Dataframe.
    * 将FrameRdd转换为Spark数据框
   *
   * Note that datetime columns will be represented as longs in the DataFrame.
    * 请注意,datetime列将在DataFrame中表示为long
   *
   * @return Dataframe representing the FrameRdd
   */
  def toDataFrame: DataFrame = {
    new SQLContext(this.sparkContext).createDataFrame(this, sparkSchema)
  }

  def toDataFrameUsingHiveContext = new org.apache.spark.sql.hive.HiveContext(this.sparkContext).createDataFrame(this, sparkSchema)

  override def compute(split: Partition, context: TaskContext): Iterator[Row] =
    firstParent[Row].iterator(split, context)

  /**
   * overrides the default behavior so new partitions get created in the sorted order to maintain the data order for the
   * user
    * 将覆盖默认行为,以便按照排序顺序创建新分区以维护用户的数据顺序
   *
   * @return an array of partitions
   */
  override def getPartitions: Array[org.apache.spark.Partition] = {
    firstParent[Row].partitions
  }

  /**
   * Compute MLLib's MultivariateStatisticalSummary from FrameRdd
   * 从FrameRdd计算MLLib的多元统计摘要
   * @param columnNames Names of the frame's column(s) whose column statistics are to be computed
    *                    要计算列统计信息的框架列的名称
   * @return MLLib's MultivariateStatisticalSummary
   */
  def columnStatistics(columnNames: Seq[String]): MultivariateStatisticalSummary = {
    val vectorRdd = toDenseVectorRdd(columnNames)
    Statistics.colStats(vectorRdd)
  }

  /**
   * Convert FrameRdd to RDD[Vector] by mean centering the specified columns
   * 将FrameRdd转换为RDD [Vector]，方法是将指定的列居中
   * @param featureColumnNames Names of the frame's column(s) to be used
    *                           要使用的框架列的名称
   * @return RDD of (org.apache.spark.mllib)Vector
   */
  def toMeanCenteredDenseVectorRdd(featureColumnNames: Seq[String]): RDD[Vector] = {
    val vectorRdd = toDenseVectorRdd(featureColumnNames)
    val columnMeans: Vector = columnStatistics(featureColumnNames).mean
    vectorRdd.map(i => {
      Vectors.dense((new DenseVector(i.toArray) - new DenseVector(columnMeans.toArray)).toArray)
    })
  }

  /**
   * Convert FrameRdd into RDD[Vector] format required by MLLib
   * 将FrameRdd转换为MLLib所需的RDD [向量]格式
   * @param featureColumnNames Names of the frame's column(s) to be used
   * @return RDD of (org.apache.spark.mllib)Vector
   */
  def toDenseVectorRdd(featureColumnNames: Seq[String]): RDD[Vector] = {
    this.mapRows(row => {
      val array = row.valuesAsArray(featureColumnNames, flattenInputs = true)
      val b = array.map(i => DataTypes.toDouble(i))
      Vectors.dense(b)
    })
  }

  def toDenseVectorRdd(columns: Seq[String], weights: Option[Seq[Double]]): RDD[Vector] = {
    weights match {
      case Some(w) => toDenseVectorRddWithWeights(columns, w)
      case None => toDenseVectorRdd(columns)
    }
  }

  /**
   * Convert FrameRdd to RDD[Vector]
   * 将FrameRdd转换为RDD [向量]
   * @param featureColumnNames Names of the frame's column(s) to be used
    *                           要使用的框架列的名称
   * @param columnWeights The weights of the columns 列的权重
   * @return RDD of (org.apache.spark.mllib)Vector
   */
  def toDenseVectorRddWithWeights(featureColumnNames: Seq[String], columnWeights: Seq[Double]): RDD[Vector] = {
    require(columnWeights.length == featureColumnNames.length, "Length of columnWeights and featureColumnNames needs to be the same")
    this.mapRows(row => {
      val array = row.valuesAsArray(featureColumnNames).map(row => DataTypes.toDouble(row))
      val columnWeightsArray = columnWeights.toArray
      val doubles = array.zip(columnWeightsArray).map { case (x, y) => x * y }
      Vectors.dense(doubles)
    }
    )
  }

  /**
   * Convert FrameRdd to RDD[(Long, Long, Double)]
    * 将FrameRdd转换为RDD [（Long，Long，Double）]
   *
   * @param sourceColumnName Name of the frame's column storing the source id of the edge
    *                         存储边的源ID的帧的列的名称
   * @param destinationColumnName Name of the frame's column storing the destination id of the edge
    *                              存储边的目标ID的帧的列的名称
   * @param edgeSimilarityColumnName Name of the frame's column storing the similarity between the source and destination
   * @return RDD[(Long, Long, Double)]
   */
  def toSourceDestinationSimilarityRDD(sourceColumnName: String, destinationColumnName: String, edgeSimilarityColumnName: String): RDD[(Long, Long, Double)] = {
    this.mapRows(row => {
      val source: Long = row.longValue(sourceColumnName)
      val destination: Long = row.longValue(destinationColumnName)
      val similarity: Double = row.doubleValue(edgeSimilarityColumnName)
      (source, destination, similarity)
    })
  }

  def toVectorRDD(featureColumnNames: scala.collection.immutable.Vector[String]) = {
    this mapRows (row => {
      val features = row.values(featureColumnNames).map(value => DataTypes.toDouble(value))
      Vectors.dense(features.toArray)
    })
  }

  /**
   * Spark map with a rowWrapper
    * 用rowWrapper的Spark map
   */
  def mapRows[U: ClassTag](mapFunction: (RowWrapper) => U): RDD[U] = {
    this.map(sqlRow => {
      mapFunction(rowWrapper(sqlRow))
    })
  }

  /**
   * Spark flatMap with a rowWrapper
   */
  def flatMapRows[U: ClassTag](mapFunction: (RowWrapper) => TraversableOnce[U]): RDD[U] = {
    this.flatMap(sqlRow => {
      mapFunction(rowWrapper(sqlRow))
    })
  }

  /**
   * Spark groupBy with a rowWrapper
    * Spark 分组行包装器
   */
  def groupByRows[K: ClassTag](function: (RowWrapper) => K): RDD[(K, scala.Iterable[Row])] = {
    this.groupBy(row => {
      function(rowWrapper(row))
    })
  }

  def selectColumn(columnName: String): FrameRdd = {
    selectColumns(List(columnName))
  }

  /**
   * Spark keyBy with a rowWrapper
   */
  def keyByRows[K: ClassTag](function: (RowWrapper) => K): RDD[(K, Row)] = {
    this.keyBy(row => {
      function(rowWrapper(row))
    })
  }

  /**
   * Create a new FrameRdd that is only a subset of the columns of this FrameRdd
   * 创建一个新的FrameRdd,它只是这个FrameRdd列的一个子集
   * @param columnNames names to include 包含的名称
   * @return the FrameRdd with only some columns 只有一些列的FrameRdd
   */
  def selectColumns(columnNames: Seq[String]): FrameRdd = {
    if (columnNames.isEmpty) {
      throw new IllegalArgumentException("list of column names can't be empty")
    }
    new FrameRdd(frameSchema.copySubset(columnNames), mapRows(row => row.valuesAsRow(columnNames)))
  }

  /**
   * Select a subset of columns while renaming them
   * 重命名时选择一个列的子集
   * @param columnNamesWithRename map of old names to new names
   * @return the new FrameRdd
   */
  def selectColumnsWithRename(columnNamesWithRename: Map[String, String]): FrameRdd = {
    if (columnNamesWithRename.isEmpty) {
      throw new IllegalArgumentException("map of column names can't be empty")
    }
    val preservedOrderColumnNames = frameSchema.columnNames.filter(name => columnNamesWithRename.contains(name))
    new FrameRdd(frameSchema.copySubsetWithRename(columnNamesWithRename), mapRows(row => row.valuesAsRow(preservedOrderColumnNames)))
  }

  /*
  Please see documentation. Zip works if 2 SchemaRDDs have the same number of partitions and same number of elements
  in  each partition
  请参阅文档,如果2个SchemaRDD具有相同数量的分区和相同数量的元素,则Zip将起作用在每个分区
  */
  def zipFrameRdd(frameRdd: FrameRdd): FrameRdd = {
    new FrameRdd(frameSchema.addColumns(frameRdd.frameSchema.columns), this.zip(frameRdd).map { case (a, b) => Row.merge(a, b) })
  }

  /**
   * Drop columns - create a new FrameRdd with the columns specified removed
    * 删除列 - 创建一个新的FrameRdd与删除指定的列
   */
  def dropColumns(columnNames: List[String]): FrameRdd = {
    convertToNewSchema(frameSchema.dropColumns(columnNames))
  }

  /**
   * Drop all columns with the 'ignore' data type.
   *删除“忽略”数据类型的所有列
   * The ignore data type is a slight hack for ignoring some columns on import.
   */
  def dropIgnoreColumns(): FrameRdd = {
    convertToNewSchema(frameSchema.dropIgnoreColumns())
  }

  /**
   * Remove duplicate rows by specifying the unique columns
    * 通过指定唯一的列来删除重复的行
   */
  def dropDuplicatesByColumn(columnNames: scala.collection.immutable.Vector[String]): Self = {
    val numColumns = frameSchema.columns.size
    val columnIndices = frameSchema.columnIndices(columnNames)
    val otherColumnNames = frameSchema.columnNamesExcept(columnNames)
    val otherColumnIndices = frameSchema.columnIndices(otherColumnNames)

    val duplicatesRemovedRdd: RDD[Row] = this.mapRows(row => otherColumnNames match {
      case Nil => (row.values(columnNames), Nil)
      case _ => (row.values(columnNames), row.values(otherColumnNames.toVector))
    }).reduceByKey((x, y) => x).map {
      case (keyRow, valueRow) =>
        valueRow match {
          case Nil => new GenericRow(keyRow.toArray)
          case _ => {
            //merge and re-order entries to match schema
            //合并和重新排序条目以匹配模式
            val rowArray = new Array[Any](numColumns)
            for (i <- keyRow.indices) rowArray(columnIndices(i)) = keyRow(i)
            for (i <- valueRow.indices) rowArray(otherColumnIndices(i)) = valueRow(i)
            new GenericRow(rowArray)
          }
        }
    }
    update(duplicatesRemovedRdd)
  }

  /**
   * Update rows in the frame
   * 更新框架中的行
   * @param newRows New rows
   * @return New frame with updated rows
   */
  def update(newRows: RDD[Row]): Self = {
    new FrameRdd(this.frameSchema, newRows).asInstanceOf[Self]
  }

  /**
   * Union two Frame's, merging schemas if needed
    * 联合两个框架,如果需要合并模式
   */
  def union(other: FrameRdd): FrameRdd = {
    val unionedSchema = frameSchema.union(other.frameSchema)
    val part1 = convertToNewSchema(unionedSchema).prev
    val part2 = other.convertToNewSchema(unionedSchema).prev
    val unionedRdd = part1.union(part2)
    new FrameRdd(unionedSchema, unionedRdd)
  }

  def renameColumn(oldName: String, newName: String): FrameRdd = {
    new FrameRdd(frameSchema.renameColumn(oldName, newName), this)
  }

  /**
   * Add/drop columns to make this frame match the supplied schema
   * 添加/删除列以使该框架与提供的模式匹配
   * @param updatedSchema the new schema to take effect
   * @return the new RDD
   */
  def convertToNewSchema(updatedSchema: Schema): FrameRdd = {
    if (frameSchema == updatedSchema) {
      // no changes needed
      this
    }
    else {
      // map to new schema
      new FrameRdd(updatedSchema, mapRows(row => row.valuesForSchema(updatedSchema)))
    }
  }

  /**
   * Sort by one or more columns
   * 按一列或多列排序
   * @param columnNamesAndAscending column names to sort by, true for ascending, false for descending
    *                                要排序的列名称,为升序为true,为降序为false
   * @return the sorted Frame
   */
  def sortByColumns(columnNamesAndAscending: List[(String, Boolean)]): FrameRdd = {
    require(columnNamesAndAscending != null && columnNamesAndAscending.nonEmpty, "one or more sort columns required")
    this.frameSchema.validateColumnsExist(columnNamesAndAscending.map(_._1))
    val sortOrder = FrameOrderingUtils.getSortOrder(columnNamesAndAscending)

    val sortedFrame = this.toDataFrame.orderBy(sortOrder: _*)
    new FrameRdd(frameSchema, sortedFrame.rdd)
  }

  /**
   * Create a new RDD where unique ids are assigned to a specified value.
    * 创建一个新的RDD,其中唯一的ID分配给一个指定的值
   * The ids will be long values that start from a specified value.
    * D将是从指定值开始的长整型值
   * The ids are inserted into a specified column. if it does not exist the column will be created.
   * 标识符被插入到指定的列中,如果不存在,则将创建该列
    *
   * (used for _vid and _eid in graphs but can be used elsewhere)
   *
   * @param columnName column to insert ids into (adding if needed)
    *                   列中插入ID(如果需要添加)
   * @param startId  the first id to add (defaults to 0), incrementing from there
    *                 要添加的第一个ID(默认为0),从那里增加
   */
  def assignUniqueIds(columnName: String, startId: Long = 0): FrameRdd = {
    val sumsAndCounts: Map[Int, (Long, Long)] = RddUtils.getPerPartitionCountAndAccumulatedSum(this)

    val newRows: RDD[Row] = this.mapPartitionsWithIndex((i, rows) => {
      val sum: Long = if (i == 0) 0L
      else sumsAndCounts(i - 1)._2
      val partitionStart = sum + startId
      rows.zipWithIndex.map {
        case (row: Row, index: Int) =>
          val id: Long = partitionStart + index
          rowWrapper(row).addOrSetValue(columnName, id)
      }
    })

    val newSchema: Schema = if (!frameSchema.hasColumn(columnName)) {
      frameSchema.addColumn(columnName, DataTypes.int64)
    }
    else
      frameSchema

    new FrameRdd(newSchema, newRows)
  }

  /**
   * Convert Vertex or Edge Frames to plain data frames
    * 将顶点或边框转换为纯数据框
   */
  def toPlainFrame: FrameRdd = {
    new FrameRdd(frameSchema.toFrameSchema, this)
  }

  /**
   * Save this RDD to disk or other store
    * 将此RDD保存到磁盘或其他存储
   *
   * @param absolutePath location to store 本地存储
   * @param storageFormat "file/parquet", "file/sequence", etc.
   */
  def save(absolutePath: String, storageFormat: String = "file/parquet"): Unit = {
    storageFormat match {
      case "file/sequence" => this.saveAsObjectFile(absolutePath)
      case "file/parquet" => this.toDataFrame.write.parquet(absolutePath)
      case format => throw new IllegalArgumentException(s"Unrecognized storage format: $format")
    }
  }

  /**
   * Convert FrameRdd into RDD of scores, labels, and associated frequency
    * 将FrameRdd转换为分数,标签和相关频率的RDD
   *
   * @param labelColumn Column of class labels 类标签的列
   * @param predictionColumn Column of predictions (or scores)
    *                         预测列(或分数)
   * @param frequencyColumn Column with frequency of observations
    *                        具有观察频率的列
   * @tparam T type of score and label 得分和标签的类型
   * @return RDD with score, label, and frequency
    *         RDD与分数,标签和频率
   */
  def toScoreAndLabelRdd[T](labelColumn: String,
                            predictionColumn: String,
                            frequencyColumn: Option[String] = None): RDD[ScoreAndLabel[T]] = {
    this.mapRows(row => {
      val label = row.value(labelColumn).asInstanceOf[T]
      val score = row.value(predictionColumn).asInstanceOf[T]
      val frequency = frequencyColumn match {
        case Some(column) => DataTypes.toLong(row.value(column))
        case _ => 1
      }
      ScoreAndLabel[T](score, label, frequency)
    })
  }

  /**
   * Convert FrameRdd into RDD of scores, labels, and associated frequency
    *将FrameRdd转换为分数,标签和相关频率的RDD
   *
   * @param scoreAndLabelFunc Function that extracts score and label from row
    *                          从行中提取分数和标签的函数
   * @tparam T type of score and label 得分和标签的类型
   * @return RDD with score, label, and frequency RDD与分数,标签和频率
   */
  def toScoreAndLabelRdd[T](scoreAndLabelFunc: (RowWrapper) => ScoreAndLabel[T]): RDD[ScoreAndLabel[T]] = {
    this.mapRows(row => {
      scoreAndLabelFunc(row)
    })
  }

  /**
   * Add column to frame 将列添加到框架
   *
   * @param column Column to add 要添加的列
   * @param addColumnFunc Function that extracts column value to add from row
    *                      提取列值从行添加的函数
   * @tparam T type of added column 添加列的类型
   * @return Frame with added column 添加列的框架
   */
  def addColumn[T](column: Column, addColumnFunc: (RowWrapper) => T): FrameRdd = {
    val rows = this.mapRows(row => {
      val columnValue = addColumnFunc(row)
      row.addValue(columnValue)
    })
    new FrameRdd(frameSchema.addColumn(column), rows)
  }



  /**
   * Convert FrameRdd into RDD[LabeledPoint] format required by MLLib
    * 将FrameRdd转换为MLLib所需的RDD [LabeledPoint]格式
   */

  import org.apache.spark.mllib.regression.LabeledPoint

  def toLabeledPointRDD(labelColumnName: String, featureColumnNames: List[String]): RDD[LabeledPoint] = {
    this.mapRows(row => {
      val features = row.values(featureColumnNames).map(value => DataTypes.toDouble(value))
      new LabeledPoint(DataTypes.toDouble(row.value(labelColumnName)), new MllibDenseVector(features.toArray))
    })
  }

}

/**
 * Static Methods for FrameRdd mostly deals with
  * FrameRdd的静态方法主要处理
 */
object FrameRdd {

  def toFrameRdd(schema: Schema, rowRDD: RDD[Array[Any]]) = {
    new FrameRdd(schema, FrameRdd.toRowRDD(schema, rowRDD))
  }

  /**
   * Implicit conversion of FrameState to FrameRdd.
    * 将FrameState隐式转换为FrameRdd
   */
  implicit def fromFrameState(frameState: FrameState) =
    new FrameRdd(frameState.schema, frameState.rdd)

  /**
   * Implicit conversion of FrameRdd to FrameState.
    * 将FrameRdd隐式转换为FrameState
   */
  implicit def toFrameState(frameRdd: FrameRdd) =
    FrameState(frameRdd, frameRdd.frameSchema)

  /**
   * converts a data frame to frame rdd
    * 将数据frame转换为 rdd
   *
   * @param rdd a data frame
   * @return a frame rdd
   */
  def toFrameRdd(rdd: DataFrame): FrameRdd = {
    val fields: Seq[StructField] = rdd.schema.fields
    val list = new ListBuffer[Column]
    for (field <- fields) {
      list += new Column(field.name, sparkDataTypeToSchemaDataType(field.dataType))
    }
    val schema = new FrameSchema(list.toVector)
    //val convertedRdd: RDD[Row]=null
    val convertedRdd: RDD[Row] = rdd.rdd.map(row => {
      val rowArray = new Array[Any](row.length)
         row.toSeq.zipWithIndex.foreach {
            case (o, i) =>
              if (o == null) {
                rowArray(i) = null
              }
              else if (fields(i).dataType.getClass == TimestampType.getClass || fields(i).dataType.getClass == DateType.getClass) {
                rowArray(i) = row.getTimestamp(i).getTime
              }
              else if (fields(i).dataType.getClass == ShortType.getClass) {
                rowArray(i) = row.getShort(i).toInt
              }
              else if (fields(i).dataType.getClass == BooleanType.getClass) {
                rowArray(i) = row.getBoolean(i).compareTo(false)
              }
              else if (fields(i).dataType.getClass == ByteType.getClass) {
                rowArray(i) = row.getByte(i).toInt
              }
              else if (fields(i).dataType.getClass == classOf[DecimalType]) { // DecimalType.getClass return value (DecimalType$) differs from expected DecimalType
                rowArray(i) = row.getAs[java.math.BigDecimal](i).doubleValue()
              }
              else {
                val colType = schema.columns(i).dataType
                rowArray(i) = o.asInstanceOf[colType.ScalaType]
              }
          }
      new GenericRow(rowArray)
    }
    )
    new FrameRdd(schema, convertedRdd)
  }

  /**
   * Converts row object from an RDD[Array[Any]] to an RDD[Product] so that it can be used to create a SchemaRDD
   * 将行对象从RDD [Array [Any]]转换为RDD [Product],以便可以使用它来创建SchemaRDD
   * @return RDD[org.apache.spark.sql.Row] with values equal to row object
   */
  def toRowRDD(schema: Schema, rows: RDD[Array[Any]]): RDD[org.apache.spark.sql.Row] = {
    val rowRDD: RDD[org.apache.spark.sql.Row] = rows.map(row => {
      val rowArray = new Array[Any](row.length)
      row.zipWithIndex.map {
        case (o, i) =>
          o match {
            case null => null
            case _ =>
              val colType = schema.column(i).dataType
              rowArray(i) = colType.parse(o).get
          }
      }
      new GenericRow(rowArray)
    })
    rowRDD
  }

  /**
   * Converts row object to RDD[IndexedRow] needed to create an IndexedRowMatrix
   * 将行对象转换为创建IndexedRowMatrix所需的RDD [IndexedRow]
   * @param indexedRows Rows of the frame as RDD[Row] 作为RDD的行的行[Row]
   * @param frameSchema Schema of the frame 框架的架构
   * @param featureColumnNames List of the frame's column(s) to be used 要使用的框架列的列表
   * @return RDD[IndexedRow]
   */
  def toIndexedRowRdd(indexedRows: RDD[(Long, org.apache.spark.sql.Row)], frameSchema: Schema, featureColumnNames: List[String]): RDD[IndexedRow] = {
    val rowWrapper = new RowWrapper(frameSchema)
    indexedRows.map {
      case (index, row) =>
        val array = rowWrapper(row).valuesAsArray(featureColumnNames, flattenInputs = true)
        val b = array.map(i => DataTypes.toDouble(i))
        IndexedRow(index, Vectors.dense(b))
    }
  }

  /**
   * Converts row object to RDD[IndexedRow] needed to create an IndexedRowMatrix
   * 将行对象转换为创建IndexedRowMatrix所需的RDD [IndexedRow]
   * @param indexedRows Rows of the frame as RDD[Row]作为RDD的行的行[Row]
   * @param frameSchema Schema of the frame 框架的架构
   * @param featureColumnNames List of the frame's column(s) to be used
    *                           要使用的框架列的列表
   * @param meanVector Vector storing the means of the columns
    *                   向量存储列的平均值
    * @return RDD[IndexedRow]
   */
  def toMeanCenteredIndexedRowRdd(indexedRows: RDD[(Long, org.apache.spark.sql.Row)], frameSchema: Schema, featureColumnNames: List[String], meanVector: Vector): RDD[IndexedRow] = {
    val rowWrapper = new RowWrapper(frameSchema)
    indexedRows.map {
      case (index, row) =>
        val array = rowWrapper(row).valuesAsArray(featureColumnNames, flattenInputs = true)
        val b = array.map(i => DataTypes.toDouble(i))
        val meanCenteredVector = Vectors.dense((new DenseVector(b) - new DenseVector(meanVector.toArray)).toArray)
        IndexedRow(index, meanCenteredVector)
    }
  }

  /**
   * Convert FrameRdd into RDD[LabeledPoint] format required by MLLib
    * 将FrameRdd转换为MLLib所需的RDD [LabeledPoint]格式
   */

  import org.apache.spark.mllib.regression.LabeledPoint


  def toLabeledPointRDD(rdd: FrameRdd, labelColumnName: String, featureColumnNames: Seq[String]): RDD[LabeledPoint] = {
    val featureColumnsAsVector = featureColumnNames.toVector
    rdd.mapRows(row => {
      val features = row.values(featureColumnsAsVector).map(value => DataTypes.toDouble(value))
      new LabeledPoint(DataTypes.toDouble(row.value(labelColumnName)), new MllibDenseVector(features.toArray))
    })
  }

  /**
   * Defines a VectorType "StructType" for SchemaRDDs
    * 为SchemaRDD定义一个VectorType“StructType”
   */
  val VectorType = ArrayType(DoubleType, containsNull = false)
  //import org.apache.spark.mllib.org.trustedanalytics.sparktk.MllibAliases.MatrixUDT
  import org.apache.spark.mllib.linalg.MatrixUDT
  //val MatrixType = new MatrixUDT

  /**
   * Converts the schema object to a StructType for use in creating a SchemaRDD
    * 将schema对象转换为用于创建SchemaRDD的StructType
   *
   * @return StructType with StructFields corresponding to the columns of the schema object
    *         StructType与StructFields对应的模式对象的列
   */
  def schemaToStructType(schema: Schema): StructType = {
    val fields: Seq[StructField] = schema.columns.map {
      column =>
        //\\s匹配任意的空白符
        StructField(column.name.replaceAll("\\s", ""), schemaDataTypeToSqlDataType(column.dataType), nullable = true)
    }
    StructType(fields)
  }
  //模式数据类型转换为Sql数据类型
  def schemaDataTypeToSqlDataType(dataType: DataTypes.DataType): org.apache.spark.sql.types.DataType = {
    dataType match {
      case x if x.equals(DataTypes.int32) => IntegerType
      case x if x.equals(DataTypes.int64) => LongType
      case x if x.equals(DataTypes.float32) => FloatType
      case x if x.equals(DataTypes.float64) => DoubleType
      case x if x.equals(DataTypes.string) => StringType
      case x if x.equals(DataTypes.datetime) => LongType
      case x if x.isVector => VectorType
      case x if x.equals(DataTypes.ignore) => StringType
      //case x if x.equalsDataType(DataTypes.matrix) => MatrixType
    }
  }

  /**
   * Converts the spark DataTypes to our schema Datatypes
    * 将spark数据类型转换为我们的模式数据类型
   *
   * @return our schema DataType
   */
  def sparkDataTypeToSchemaDataType(dataType: org.apache.spark.sql.types.DataType): DataTypes.DataType = {
    val intType = IntegerType.getClass
    val longType = LongType.getClass
    val floatType = FloatType.getClass
    val doubleType = DoubleType.getClass
    val stringType = StringType.getClass
    val dateType = DateType.getClass
    val timeStampType = TimestampType.getClass
    val byteType = ByteType.getClass
    val booleanType = BooleanType.getClass
    val decimalType = classOf[DecimalType] // DecimalType.getClass return value (DecimalType$) differs from expected DecimalType
    val shortType = ShortType.getClass

   // val matrixType = classOf[MatrixUDT]

    //TODO: val vectorType = ArrayType(DoubleType, containsNull = false).getClass

    val a = dataType.getClass
    a match {
      case `intType` => int32
      case `longType` => int64
      case `floatType` => float32
      case `doubleType` => float64
      case `decimalType` => float64
      case `shortType` => int32
      case `stringType` => DataTypes.string
      //case `dateType` => DataTypes.string
      case `byteType` => int32
      //TODO: case `vectorType` => DataTypes.vector(5)
      //case `booleanType` => int32
      case `timeStampType` => DataTypes.datetime
     // case `matrixType` => matrix
      case _ => throw new IllegalArgumentException(s"unsupported column data type $a")
    }
  }

  /**
   * Converts a spark dataType (as string)to our schema Datatype
   * 将spark数据类型(作为字符串)转换为我们的模式数据类型
   * @param sparkDataType spark data type
   * @return a DataType
   */
  def sparkDataTypeToSchemaDataType(sparkDataType: String): DataTypes.DataType = {
    if ("intType".equalsIgnoreCase(sparkDataType)) { int32 }
    else if ("longType".equalsIgnoreCase(sparkDataType)) { int64 }
    else if ("floatType".equalsIgnoreCase(sparkDataType)) { float32 }
    else if ("doubleType".equalsIgnoreCase(sparkDataType)) { float64 }
    else if ("decimalType".equalsIgnoreCase(sparkDataType)) { float64 }
    else if ("shortType".equalsIgnoreCase(sparkDataType)) { int32 }
    else if ("stringType".equalsIgnoreCase(sparkDataType)) { DataTypes.string }
    else if ("dateType".equalsIgnoreCase(sparkDataType)) { DataTypes.string }
    else if ("byteType".equalsIgnoreCase(sparkDataType)) { int32 }
    else if ("booleanType".equalsIgnoreCase(sparkDataType)) { int32 }
    else if ("timeStampType".equalsIgnoreCase(sparkDataType)) { DataTypes.datetime }
    else throw new IllegalArgumentException(s"unsupported type $sparkDataType")
  }

  /**
   * Converts the schema object to a StructType for use in creating a SchemaRDD
   * 将模式对象转换为用于创建SchemaRDD的StructType
   * @return StructType with StructFields corresponding to the columns of the schema object
    *         StructType与StructFields对应的模式对象的列
   */
  def schemaToAvroType(schema: Schema): Seq[(String, String)] = {
    val fields = schema.columns.map {
      column =>
        (column.name.replaceAll("\\s", ""), column.dataType match {
          case x if x.equals(DataTypes.int32) => "int"
          case x if x.equals(DataTypes.int64) => "long"
          case x if x.equals(DataTypes.float32) => "double"
          case x if x.equals(DataTypes.float64) => "double"
          case x if x.equals(DataTypes.string) => "string"
          case x if x.equals(DataTypes.datetime) => "long"
          case x => throw new IllegalArgumentException(s"unsupported export type ${x.toString}")
        })
    }
    fields
  }
}
