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
package org.trustedanalytics.sparktk.frame.internal.ops.groupby.aggregators

import org.trustedanalytics.sparktk.frame.DataTypes
import org.trustedanalytics.sparktk.frame.DataTypes.DataType
/**
 * Aggregator for computing the minimum column values using Spark's aggregateByKey()
 * 使用Spark的aggregateByKey（）计算最小列值的聚合器
 * Supports any data type that is comparable.
 * 支持任何可比的数据类型
 * @see org.apache.spark.rdd.PairRDDFunctions#aggregateByKey
 */
case class MinAggregator() extends GroupByAggregator {

  /** Type for aggregate values that corresponds to type U in Spark's aggregateByKey()
    * 在Spark的aggregateByKey（）中键入对应于类型U的聚合值 */
  override type AggregateType = Any

  /** Output type of the map function that corresponds to type V in Spark's aggregateByKey()
    * Spark的aggregateByKey（）中与类型V对应的map函数的输出类型*/
  override type ValueType = Any

  /** The 'empty' or 'zero' or default value for the aggregator
    * 聚合器的“空”或“零”或默认值*/
  override def zero: Any = null

  /**
   * Outputs column value
    * 输出列值
   */
  override def mapFunction(columnValue: Any, columnDataType: DataType): ValueType = columnValue

  /**
   * Returns the minimum of the two input parameters.
    * 返回两个输入参数的最小值
   */
  override def add(min: AggregateType, mapValue: ValueType): AggregateType = getMinimum(min, mapValue)

  /**
   * Returns the minimum of the two input parameters.
    * 返回两个输入参数的最小值
   */
  override def merge(min1: AggregateType, min2: AggregateType) = getMinimum(min1, min2)

  /**
   * Returns the minimum value for data types that are comparable
    * 返回可比数据类型的最小值
   */
  private def getMinimum(left: Any, right: Any): Any = {
    if ((left != null && DataTypes.compare(left, right) <= 0) || right == null) // Ignoring nulls
      left
    else right
  }
}
