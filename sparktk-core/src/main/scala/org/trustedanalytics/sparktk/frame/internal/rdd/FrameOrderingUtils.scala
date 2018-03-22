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

/**
 *  Copyright (c) 2015 Intel Corporation 
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

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column => SparkSqlColumn}

object FrameOrderingUtils extends Serializable {


  /**
   * Get sort order for Spark data frames
   *获取Spark数据框的排序顺序
   * @param columnNamesAndAscending column names to sort by, true for ascending, false for descending
    *                                要排序的列名称,为升序为true,为降序为false
   * @return Sort order for data frames 对数据框排序
   */
  def getSortOrder(columnNamesAndAscending: List[(String, Boolean)]): Seq[SparkSqlColumn] = {
    require(columnNamesAndAscending != null && columnNamesAndAscending.nonEmpty, "one or more sort columns required")
    columnNamesAndAscending.map {
      case (columnName, ascending) =>
        if (ascending) {
          asc(columnName)
        }
        else {
          desc(columnName)
        }
    }
  }
}

