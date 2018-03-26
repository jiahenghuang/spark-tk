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

import org.trustedanalytics.sparktk.frame.Frame
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.trustedanalytics.sparktk.frame.internal.{ FrameState, FrameSummarization, BaseFrame }
//笛卡尔连接
trait JoinCrossSummarization extends BaseFrame {

  /**
   * JoinCross returns a frame with the Cartesian product of the rows from the specified frames.  Each row from the
   * current frame is combined with each row from the right frame.
    * JoinCross从指定的frame中返回一个包含行笛卡尔积的帧,当前frame中的每一行都与右侧帧中的每一行相结合。
   *
   * @param right   The right frame in the cross join operation 交叉连接操作中的右边框
   */
  def joinCross(right: Frame): Frame = {
    execute(JoinCross(right))
  }
}

case class JoinCross(right: Frame) extends FrameSummarization[Frame] {
  require(right != null, "right frame is required")

  override def work(state: FrameState): Frame = {
    val leftFrame: FrameRdd = state
    val rightFrame: FrameRdd = new FrameRdd(right.schema, right.rdd)

    JoinRddFunctions.crossJoin(leftFrame, rightFrame)
  }
}
