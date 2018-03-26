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
package org.trustedanalytics.sparktk.frame.internal.ops

import org.trustedanalytics.sparktk.frame.internal._
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd

trait DropColumnsTransform extends BaseFrame {
  /**
   * Drops columns from the frame
   * 从框架中删除列
   * The data from the columns is lost.
   * 列中的数据丢失了
   * @note It is not possible to delete all columns from a frame.  At least one column needs to remain. If it is
   *       necessary to delete all columns, then delete the frame.
    *      从框架中删除所有列是不可能的,至少有一列需要保留,如果需要删除所有列,则删除该框架
   *
   * @param columns names of the columns to drop
   */
  def dropColumns(columns: Seq[String]): Unit = execute(DropColumns(columns))
}

case class DropColumns(columns: Seq[String]) extends FrameTransform {

  override def work(state: FrameState): FrameState = {
    state.schema.validateColumnsExist(columns)
    require(state.schema.columnNamesExcept(columns).nonEmpty, "Cannot drop all columns, must leave at least one column")
    //case class 强制类型转换
    (state: FrameRdd).selectColumns(state.schema.dropColumns(columns).columnNames)
  }
}