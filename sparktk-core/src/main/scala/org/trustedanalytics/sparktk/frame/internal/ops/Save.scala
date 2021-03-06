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

import org.apache.spark.sql.DataFrame
import org.trustedanalytics.sparktk.frame.Frame
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.trustedanalytics.sparktk.frame.internal.{ FrameState, FrameSummarization, BaseFrame }
import org.trustedanalytics.sparktk.saveload.TkSaveLoad

trait SaveSummarization extends BaseFrame {
  /**
   * Save the current frame.
   * 保存当前frame
   * @param path The destination path.
   */
  def save(path: String): Unit = {
    execute(Save(path))
  }
}

case class Save(path: String) extends FrameSummarization[Unit] {

  override def work(state: FrameState): Unit = {
    val frameRdd = new FrameRdd(state.schema, state.rdd)
    val df: DataFrame = frameRdd.toDataFrame
    df.write.parquet(path)
  }
}

