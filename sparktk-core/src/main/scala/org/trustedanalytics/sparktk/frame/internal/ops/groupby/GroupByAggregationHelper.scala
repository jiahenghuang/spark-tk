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
package org.trustedanalytics.sparktk.frame.internal.ops.groupby

import org.apache.spark.rdd.RDD
import org.trustedanalytics.sparktk.frame.internal.ops.groupby.aggregators._
import org.trustedanalytics.sparktk.frame.{ Schema, Column, FrameSchema }
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.trustedanalytics.sparktk.frame.DataTypes

/**
 * Arguments for GroupBy aggregation
  * GroupBy聚合的参数
 *
 * @param function Name of aggregation function (e.g., count, sum, variance)
 * @param columnName Name of column to aggregate
 * @param newColumnName Name of new column that stores the aggregated results
 */
case class GroupByAggregationArgs(function: String, columnName: String, newColumnName: String) {
  def this(aggregation: Map[String, String]) = {
    this(aggregation.get("function").get,
      aggregation.get("column_name").get,
      aggregation.get("new_column_name").get)
  }
}

/**
 * Aggregations for Frames (SUM, COUNT, etc)
 * Frames的聚合（SUM，COUNT等）
 * This is a wrapper to encapsulate methods that may need to be serialized to executed on Spark worker nodes.
  * 这是封装可能需要序列化以在Spark工作节点上执行的方法的封装器
 * If you don't know what this means please read about Closure Mishap
  * 如果您不知道这意味着什么,请阅读关于关闭的硬伤
 * [[http://ampcamp.berkeley.edu/wp-content/uploads/2012/06/matei-zaharia-part-1-amp-camp-2012-spark-intro.pdf]]
 * and Task Serialization
 * [[http://stackoverflow.com/questions/22592811/scala-spark-task-not-serializable-java-io-notserializableexceptionon-when]]
 */
private object GroupByAggregationHelper extends Serializable {

  /**
   * Create a Summarized Frame with Aggregations (Avg, Count, Max, Min, ...).
   * 使用聚合创建汇总Frame（Avg，Count，Max，Min，...）。
   * For example, grouping a frame by gender and age, and computing the average income.
    * 例如:按照性别和年龄分组一个框架,并计算平均收入。
   *
   * New aggregations can be added by implementing a GroupByAggregator.
   * 新的聚合可以通过实现一个GroupByAggregator来添加
   * @see GroupByAggregator
   * @param frameRdd Input frame
   * @param groupByColumns List of columns to group by
   * @param aggregationArguments List of aggregation arguments
   * @return Summarized frame with aggregations
   */
  def aggregation(frameRdd: FrameRdd,
                  groupByColumns: List[Column],
                  aggregationArguments: List[GroupByAggregationArgs]): FrameRdd = {

    val frameSchema = frameRdd.frameSchema
    val columnAggregators = createColumnAggregators(frameSchema, aggregationArguments)

    val pairedRowRDD = pairRowsByGroupByColumns(frameRdd, groupByColumns, aggregationArguments)

    val aggregationRDD = GroupByAggregateByKey(pairedRowRDD, columnAggregators).aggregateByKey()

    val newColumns = groupByColumns ++ columnAggregators.map(_.column)
    val newSchema = FrameSchema(newColumns.toVector)

    new FrameRdd(newSchema, aggregationRDD)
  }

  /**
   * Returns a list of columns and corresponding accumulators used to aggregate values
   * 返回用于聚合值的列和相应累加器的列表
   * @param aggregationArguments List of aggregation arguments (i.e., aggregation function, column, new column name)
    *                             聚合参数列表(即聚合函数,列,新列名称)
   * @param frameSchema Frame schema
   * @return  List of columns and corresponding accumulators 列和相应的累加器的列表
   */
  def createColumnAggregators(frameSchema: Schema, aggregationArguments: List[(GroupByAggregationArgs)]): List[ColumnAggregator] = {

    aggregationArguments.zipWithIndex.map {
      case (arg, i) =>
        val column = frameSchema.column(arg.columnName)

        arg.function match {
          case "COUNT" =>
            ColumnAggregator(Column(arg.newColumnName, DataTypes.int64), i, CountAggregator())
          case "COUNT_DISTINCT" =>
            ColumnAggregator(Column(arg.newColumnName, DataTypes.int64), i, DistinctCountAggregator())
          case "MIN" =>
            ColumnAggregator(Column(arg.newColumnName, column.dataType), i, MinAggregator())
          case "MAX" =>
            ColumnAggregator(Column(arg.newColumnName, column.dataType), i, MaxAggregator())
          case "SUM" if column.dataType.isNumerical =>
            if (column.dataType.isInteger)
              ColumnAggregator(Column(arg.newColumnName, DataTypes.int64), i, new SumAggregator[Long]())
            else
              ColumnAggregator(Column(arg.newColumnName, DataTypes.float64), i, new SumAggregator[Double]())
          case "AVG" if column.dataType.isNumerical =>
            ColumnAggregator(Column(arg.newColumnName, DataTypes.float64), i, MeanAggregator())
          case "VAR" if column.dataType.isNumerical =>
            ColumnAggregator(Column(arg.newColumnName, DataTypes.float64), i, VarianceAggregator())
          case "STDEV" if column.dataType.isNumerical =>
            ColumnAggregator(Column(arg.newColumnName, DataTypes.float64), i, StandardDeviationAggregator())
          case function if function.matches("""HISTOGRAM.*""") && column.dataType.isNumerical =>
            ColumnAggregator.getHistogramColumnAggregator(arg, i)
          case _ => throw new IllegalArgumentException(s"Unsupported aggregation function: ${arg.function} for data type: ${column.dataType}")
        }
    }
  }

  /**
   * Create a pair RDD using the group-by keys, and aggregation columns
   * 使用group-by键和聚合列创建一对RDD
   * The group-by key is a sequence of column values, for example, group-by gender and age. The aggregation
   * columns are the columns containing the values to be aggregated, for example, annual income.
   * 分组键是一系列列值,例如按性别和年龄分组,汇总列是包含要汇总的值的列,例如年收入
   * @param frameRdd Input frame
   * @param groupByColumns Group by columns 按列分组
   * @param aggregationArguments List of aggregation arguments 聚合参数列表
   * @return RDD of group-by keys, and aggregation column values group-by键的RDD和聚合列值
   */
  def pairRowsByGroupByColumns(frameRdd: FrameRdd,
                               groupByColumns: List[Column],
                               aggregationArguments: List[GroupByAggregationArgs]): RDD[(Seq[Any], Seq[Any])] = {
    val frameSchema = frameRdd.frameSchema
    val groupByColumnsNames = groupByColumns.map(col => col.name)

    val aggregationColumns = aggregationArguments.map(arg => frameSchema.column(columnName = arg.columnName))

    frameRdd.mapRows(row => {
      val groupByKey = if (groupByColumnsNames.nonEmpty) row.valuesAsArray(groupByColumnsNames).toSeq else Seq[Any]()
      val groupByRow = aggregationColumns.map(col => row.data(frameSchema.columnIndex(col.name)))
      (groupByKey, groupByRow.toSeq)
    })
  }

}
