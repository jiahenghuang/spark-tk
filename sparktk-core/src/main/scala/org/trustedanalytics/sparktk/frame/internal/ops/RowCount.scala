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

import org.trustedanalytics.sparktk.frame.internal.{ FrameState, FrameSummarization, BaseFrame }

trait RowCountSummarization extends BaseFrame {
  /**
   * Counts all of the rows in the frame.
   * 统计frame中的所有行
   * @return The number of rows in the frame.
   */
  def rowCount(): Long = execute[Long](RowCount)
}

/**
 * Number of rows in the current frame
  * 当前frame中的行数
 */
case object RowCount extends FrameSummarization[Long] {
  def work(frame: FrameState): Long = frame.rdd.count()
}

