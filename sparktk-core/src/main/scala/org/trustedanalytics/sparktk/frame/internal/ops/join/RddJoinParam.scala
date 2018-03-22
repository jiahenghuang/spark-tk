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

import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd

/**
 * Join parameters for RDD
 * 加入RDD的参数
 * @param frame Frame used for join
 * @param joinColumns Join column name
 */
case class RddJoinParam(frame: FrameRdd,
                        joinColumns: Seq[String]) {
  require(frame != null, "join frame is required")
  require(joinColumns != null, "join column(s) are required")
}

