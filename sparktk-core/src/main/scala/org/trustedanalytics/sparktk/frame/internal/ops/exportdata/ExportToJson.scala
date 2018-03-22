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
package org.trustedanalytics.sparktk.frame.internal.ops.exportdata

import org.apache.commons.lang3.StringUtils
import org.json4s.JsonAST.{ JObject, JString }
import org.trustedanalytics.sparktk.frame.internal.rdd.{ FrameRdd, MiscFrameFunctions }
import org.trustedanalytics.sparktk.frame.internal.{ FrameState, FrameSummarization, BaseFrame }
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._

trait ExportToJsonSummarization extends BaseFrame {

  /**
   * *
   * Write current frame to HDFS in JSON format.
   * 以JSON格式将当前帧写入HDFS
   * @param path : The HDFS folder path where the files will be created.
    *             要创建文件的HDFS文件夹路径
   * @param count : The number of records you want. Default (0), or a non-positive value, is the whole frame.
    *              你想要的记录数量,默认值(0)或非正值是整个帧
   * @param offset : The number of rows to skip before exporting to the file. Default is zero (0).
    *               导出到文件之前要跳过的行数,默认为零
   */
  def exportToJson(path: String, count: Int = 0, offset: Int = 0) = {
    execute(ExportToJson(path, count, offset))
  }
}

case class ExportToJson(path: String, count: Int, offset: Int) extends FrameSummarization[Unit] {

  require(path != null, "Path is required")
  override def work(state: FrameState): Unit = {
    ExportToJson.exportToJsonFile(state, path, count, offset)
  }
}

object ExportToJson {

  def exportToJsonFile(frameRdd: FrameRdd,
                       path: String,
                       count: Int,
                       offset: Int) = {
    implicit val formats = DefaultFormats
    val filterRdd = if (count > 0) MiscFrameFunctions.getPagedRdd(frameRdd, offset, count, -1) else frameRdd
    val headers = frameRdd.frameSchema.columnNames
    val jsonRDD = filterRdd.map {
      row =>
        {
          val jsonAst = row.toSeq.zip(headers).map {
            case (value, header) => JObject((header, JString(value.toString)))
          }.reduce((a, b) => a ~ b)
          compact(render(jsonAst))
        }
    }
    jsonRDD.saveAsTextFile(path)
    if (jsonRDD.isEmpty()) StringUtils.EMPTY else jsonRDD.first()
  }
}