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
package org.trustedanalytics.sparktk.frame.internal.rdd

import org.trustedanalytics.sparktk.frame.DataTypes

/**
 * Ordering for sorting frame RDDs by multiple columns
 * 按多列排序框架RDD排序
 * Each column is ordered in ascending or descending order.
 * 每列按升序或降序排列
 * @param ascendingPerColumn Indicates whether to sort each column in list in ascending (true)
 *                           or descending (false) order
  *                           指示是按升序(true)还是降(false）顺序对列中的每列进行排序
 */
class MultiColumnOrdering(ascendingPerColumn: List[Boolean]) extends Ordering[List[Any]] {
  override def compare(a: List[Any], b: List[Any]): Int = {
    for (i <- a.indices) {
      val columnA = a(i)
      val columnB = b(i)
      val result = DataTypes.compare(columnA, columnB)
      if (result != 0) {
        if (ascendingPerColumn(i)) {
          // ascending
          return result
        }
        else {
          // descending
          return result * -1
        }
      }
    }
    0
  }
}
