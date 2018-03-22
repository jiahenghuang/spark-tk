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

import org.trustedanalytics.sparktk.frame.DataTypes.DataType
/**
 * Aggregator for counting distinct column values.
  * 用于统计不同列值的聚合器
 *
 * This aggregator assumes that the number of distinct values for a given key can fit within memory.
  * 该聚合器假定给定密钥的不同值的数量可以适应内存
 */
case class DistinctCountAggregator() extends GroupByAggregator {

  /** Type for aggregate values that corresponds to type U in Spark's aggregateByKey() */
  override type AggregateType = Set[Any]

  /** Output type of the map function that corresponds to type V in Spark's aggregateByKey() */
  override type ValueType = Any

  /** The 'empty' or 'zero' or default value for the aggregator
    * 聚合器的“空”或“零”或默认值*/
  override def zero = Set.empty[Any]

  /**
   * Outputs column value
    * 输出列值
   */
  override def mapFunction(columnValue: Any, columnDataType: DataType): ValueType = columnValue

  /**
   * Add map value to set which stores distinct column values.
    * 添加map值来设置哪些存储不同的列值
   */
  override def add(set: AggregateType, mapValue: ValueType): AggregateType = set + mapValue

  /**
   * Merge two sets.
    * 合并两个set
   */
  override def merge(set1: AggregateType, set2: AggregateType) = set1 ++ set2

  /**
   * Returns count of distinct column values
    * 返回不同列值的计数
   */
  override def getResult(set: AggregateType): Any = set.size.toLong // toLong needed to avoid casting errors when writing to Parquet
}
