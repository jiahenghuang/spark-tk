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

import org.apache.commons.csv.{ CSVPrinter, CSVFormat }
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.trustedanalytics.sparktk.frame.internal.{ FrameState, FrameSummarization, BaseFrame }

import scala.collection.mutable.ArrayBuffer

trait ExportToCsvSummarization extends BaseFrame {
  /**
   * Write current frame to HDFS in csv format.
   * 以csv格式将当前frame写入HDFS
   * Export the frame to a file in csv format as a Hadoop file.
   * 将该frame导出为csv格式的文件作为Hadoop文件
   * @param fileName The HDFS folder path where the files will be created.
    *                 HDFS的文件夹路径里的文件将被创建
   * @param separator Delimiter character.分割字符  Defaults to use a comma (`,`).
   */
  def exportToCsv(fileName: String, separator: Char = ',') = {
    execute(ExportToCsv(fileName, separator))
  }
}

case class ExportToCsv(fileName: String, separator: Char) extends FrameSummarization[Unit] {

  override def work(state: FrameState): Unit = {
    ExportToCsv.exportToCsvFile(state.rdd, fileName, separator)
  }
}

object ExportToCsv {
  //保存Txt文件
  def exportToCsvFile(rdd: RDD[Row],
                      filename: String,
                      separator: Char) = {

    val csvFormat = CSVFormat.RFC4180.withDelimiter(separator)
    ////==00==WrappedArray(com.eggpain.zhongguodashujuwang1457, 中国大数据, person2)
    val csvRdd = rdd.map(row => {
      val stringBuilder = new java.lang.StringBuilder
      val printer = new CSVPrinter(stringBuilder, csvFormat)
      //toSeq把RDD转换成ArrayBuffer(io.toutiao.bigdatacoder,大数据,person1)
      val array = row.toSeq.map {
        case null => ""
        case arr: ArrayBuffer[_] => arr.mkString(",")
        case seq: Seq[_] => seq.mkString(",")
        case x => x.toString
      }
      //输出CSV文件
      for (i <- array) printer.print(i)
      stringBuilder.toString
    })
    csvRdd.saveAsTextFile(filename)
  }
}

