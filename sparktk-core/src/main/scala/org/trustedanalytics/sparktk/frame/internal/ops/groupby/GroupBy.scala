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

import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.trustedanalytics.sparktk.frame.{ Column, Frame, Schema }
import org.trustedanalytics.sparktk.frame.internal.{ BaseFrame, FrameState, FrameSummarization }

trait GroupBySummarization extends BaseFrame {

  /**
   * Summarized Frame with Aggregations.
   * 汇总框架
   * Create a Summarized Frame with Aggregations (Avg, Count, Max, Min, Mean, Sum, Stdev, ...).
   * 使用聚合创建汇总帧（平均值，计数，最大值，最小值，平均值，和值，Stdev，...）
   * @param groupByColumns list of columns to group on 分组列的列表
   * @param aggregations list of lists contains aggregations to perform
   *                     Each inner list contains below three strings
    *                     列表列表包含要执行的聚合每个内部列表包含以下三个字符串
   *                     function: Name of aggregation function (e.g., count, sum, variance)
   *                     columnName: Name of column to aggregate
   *                     newColumnName: Name of new column that stores the aggregated results
    *                     函数:聚合函数的名称（例如，count，sum，variance）
                          columnName:要聚合的列的名称
                          newColumnName:存储聚合结果的新列的名称
   * @return Summarized Frame
   */
  def groupBy(groupByColumns: List[String], aggregations: List[GroupByAggregationArgs]) = {
    execute(GroupBy(groupByColumns, aggregations))
  }
}

case class GroupBy(groupByColumns: List[String], aggregations: List[GroupByAggregationArgs]) extends FrameSummarization[Frame] {

  require(groupByColumns != null, "group_by columns is required")
  require(aggregations != null, "aggregation list is required")

  override def work(state: FrameState): Frame = {
    val frame: FrameRdd = state
    val groupByColumnList: Iterable[Column] = frame.frameSchema.columns(columnNames = groupByColumns)

    // run the operation and save results
    val groupByRdd = GroupByAggregationHelper.aggregation(frame, groupByColumnList.toList, aggregations)
    new Frame(groupByRdd, groupByRdd.frameSchema)

  }
}

