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

import org.apache.spark.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.trustedanalytics.sparktk.frame.internal.RowWrapper

/**
 * Functions for joining pair RDDs using broadcast variables
  * 使用广播变量连接对RDD的函数
 */
class BroadcastJoinRddFunctions(self: RddJoinParam) extends Logging with Serializable {

  /**
   * Perform left outer-join using a broadcast variable
    * 使用广播变量执行左外连接
   *
   * @param other join parameter for second data frame
    *              加入第二个数据帧的参数
   * @return key-value RDD whose values are results of left-outer join
    *         键值RDD,其值是左外连接的结果
   */
  def leftJoinBroadcastingRightTable(other: RddJoinParam): RDD[Row] = {
    val rightBroadcastVariable = JoinBroadcastVariable(other)
    lazy val rightNullRow: Row = new GenericRow(other.frame.numColumns)
    val leftJoinColumns = self.joinColumns.toList
    self.frame.flatMapRows(left => {
      val leftKeys = left.values(leftJoinColumns.toVector)
      rightBroadcastVariable.get(leftKeys) match {
        case Some(rightRows) => for (rightRow <- rightRows) yield Row.merge(left.row, rightRow)
        case _ => List(Row.merge(left.row, rightNullRow.copy()))
      }
    })
  }

  /**
   * Right outer-join using a broadcast variable
   * 使用广播变量进行右外连接
   * @param other join parameter for second data frame
    *              加入第二个数据frame的参数
   * @return key-value RDD whose values are results of right-outer join
    *         键值RDD,其值是右外连接的结果
   */
  def rightJoinBroadcastingLeftTable(other: RddJoinParam): RDD[Row] = {
    val leftBroadcastVariable = JoinBroadcastVariable(self)
    lazy val leftNullRow: Row = new GenericRow(self.frame.numColumns)
    val rightJoinColumns = other.joinColumns.toList
    other.frame.flatMapRows(right => {
      val rightKeys = right.values(rightJoinColumns.toVector)
      leftBroadcastVariable.get(rightKeys) match {
        case Some(leftRows) => for (leftRow <- leftRows) yield Row.merge(leftRow, right.row)
        case _ => List(Row.merge(leftNullRow.copy(), right.row))
      }
    })
  }

  /**
   * Inner-join using a broadcast variable
   * 使用广播变量进行内部联接
   * @param other join parameter for second data frame
    *              加入第二个数据frame的参数
   * @return key-value RDD whose values are results of inner-outer join
    *         键值RDD,其值是内外连接的结果
   */
  def innerBroadcastJoin(other: RddJoinParam, useBroadcast: Option[String]): RDD[Row] = {
    if (useBroadcast == Some("right")) {
      val rowWrapper = new RowWrapper(other.frame.frameSchema)
      val rightBroadcastVariable = JoinBroadcastVariable(other)
      val rightColsToKeep = other.frame.frameSchema.dropColumns(other.joinColumns.toList).columnNames
      val leftJoinColumns = self.joinColumns.toList
      self.frame.flatMapRows(left => {
        val leftKeys = left.values(leftJoinColumns.toVector)
        rightBroadcastVariable.get(leftKeys) match {
          case Some(rightRows) =>
            for (rightRow <- rightRows) yield Row.merge(left.row, new GenericRow(rowWrapper(rightRow).values(rightColsToKeep).toArray))
          case _ => Set.empty[Row]
        }
      })
    }
    else if (useBroadcast == Some("left")) {
      val rowWrapper = new RowWrapper(self.frame.frameSchema)
      val leftBroadcastVariable = JoinBroadcastVariable(self)
      val rightColsToKeep = other.frame.frameSchema.dropColumns(other.joinColumns.toList).columnNames
      val rightJoinColumns = other.joinColumns.toList
      other.frame.flatMapRows(right => {
        val rightKeys = right.values(rightJoinColumns.toVector)
        leftBroadcastVariable.get(rightKeys) match {
          case Some(leftRows) =>
            for (leftRow <- leftRows) yield Row.merge(new GenericRow(rowWrapper(leftRow).values().toArray), new GenericRow(right.values(rightColsToKeep).toArray))
          case _ => Set.empty[Row]
        }
      })
    }
    else throw new IllegalArgumentException(s"Provide either left or right as broadcast type")
  }
}
