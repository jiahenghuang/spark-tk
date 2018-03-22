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
package org.trustedanalytics.sparktk.frame.internal.ops.join

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql._

import scala.collection.mutable.{ HashMap, MultiMap, Set }

/**
 * Broadcast variable for joins
 * 广播变量的连接
 * The broadcast variable is represented as a sequence of multi-maps. Multi-maps allow us to
 * support duplicate keys during a join. The key in the multi-map is the join key, and the value is row.
  * 广播变量表示为多Map序列,多Map使我们能够在连接过程中支持重复键,多map中的键是连接键,值是行
 * Representing the broadcast variable as a sequence of multi-maps allows us to support broadcast variables
 * larger than 2GB (current limit in Spark 1.2).
  * 将广播变量表示为多地图序列允许我们支持大于2GB的广播变量(Spark 1.2中的电流限制）
 *
 * @param joinParam Join parameter for data frame
 */
case class JoinBroadcastVariable(joinParam: RddJoinParam) {
  require(joinParam != null, "Join parameter should not be null")

  // Represented as a sequence of multi-maps to support broadcast variables larger than 2GB
  //表示为支持大于2GB的广播变量的多map序列
  // Using multi-maps instead of hash maps so that we can support duplicate keys.
  val broadcastMultiMap: Broadcast[Map[Any, List[Row]]] = createBroadcastMultiMaps(joinParam)

  /**
   * Get matching set of rows by key from broadcast join variable
   * 通过广播连接变量中的键获取匹配的一组行
   * @param key Join key
   * @return Matching set of rows if found. Multiple rows might match if there are duplicate keys.
   */
  def get(key: Any): Option[List[Row]] = {
    broadcastMultiMap.value.get(key)
  }

  // Create the broadcast variable for the join
  //为连接创建广播变量
  private def createBroadcastMultiMaps(joinParam: RddJoinParam): Broadcast[Map[Any, List[Row]]] = {
    //Grouping by key to ensure that duplicate keys are not split across different broadcast variables
    //按键分组以确保重复键不会跨不同的广播变量
    val broadcastList = joinParam.frame.groupByRows(row => row.values(joinParam.joinColumns)).collect()
    val broadcastMap: Map[Any, List[Row]] = broadcastList.map { case (key, rows) => (key, rows.toList) }.toMap
    joinParam.frame.sparkContext.broadcast(broadcastMap)
  }

}
