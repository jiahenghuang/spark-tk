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

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row

import scala.reflect.ClassTag

//implicit conversion for PairRDD
import org.apache.spark.SparkContext._

/**
 *
 * This is a wrapper to encapsulate methods that may need to be serialized to executed on Spark worker nodes.
  * 这是封装可能需要序列化以在Spark工作节点上执行的方法的封装器
 * If you don't know what this means please read about Closure Mishap
 * [[http://ampcamp.berkeley.edu/wp-content/uploads/2012/06/matei-zaharia-part-1-amp-camp-2012-spark-intro.pdf]]
 * and Task Serialization
 * [[http://stackoverflow.com/questions/22592811/scala-spark-task-not-serializable-java-io-notserializableexceptionon-when]]
 */
object MiscFrameFunctions extends Serializable {

  /**
   * take an input RDD and return another RDD which contains the subset of the original contents
    * 接受输入RDD并返回包含原始内容子集的另一个RDD
   * @param rdd input RDD
   * @param offset rows to be skipped before including rows in the new RDD
    *              在包含新RDD中的行之前要跳过的行
   * @param count total rows to be included in the new RDD
    *              总行将包含在新的RDD中
   * @param limit limit on number of rows to be included in the new RDD
    *              限制要包含在新RDD中的行数
   */
  def getPagedRdd[T: ClassTag](rdd: RDD[T], offset: Long, count: Long, limit: Int): RDD[T] = {

    val sumsAndCounts = MiscFrameFunctions.getPerPartitionCountAndAccumulatedSum(rdd)
    val capped = limit match {
      case -1 => count
      case _ => Math.min(count, limit)
    }
    //Start getting rows. We use the sums and counts to figure out which
    //partitions we need to read from and which to just ignore
    //开始获得行,我们使用总和和计数来确定我们需要读取哪些分区以及哪些分区忽略
    /**
      * mapPartitionsWithIndex与mapPartitions基本相同,只是在处理函数的参数是一个二元元组,
      * 元组的第一个元素是当前处理的分区的index,元组的第二个元素是当前处理的分区元素组成的Iterator
      */
    val pagedRdd: RDD[T] = rdd.mapPartitionsWithIndex((i, rows) => {
      val (ct: Long, sum: Long) = sumsAndCounts(i)
      val thisPartStart = sum - ct
      if (sum < offset || thisPartStart >= offset + capped) {
        //println("skipping partition " + i)
        Iterator.empty
      }
      else {
        val start = Math.max(offset - thisPartStart, 0)
        val numToTake = Math.min((capped + offset) - thisPartStart, ct) - start
        //println(s"partition $i: starting at $start and taking $numToTake")
        rows.slice(start.toInt, start.toInt + numToTake.toInt)
      }
    })

    pagedRdd
  }

  /**
   * take input RDD and return the subset of the original content
    * 输入RDD并返回原始内容的子集
   * @param rdd input RDD
   * @param offset  rows to be skipped before including rows in the result
   * @param count total rows to be included in the result
   * @param limit limit on number of rows to be included in the result
   */
  def getRows[T: ClassTag](rdd: RDD[T], offset: Long, count: Int, limit: Int): Seq[T] = {
    val pagedRdd = getPagedRdd(rdd, offset, count, limit)
    val rows: Seq[T] = pagedRdd.collect()
    rows
  }

  /**
   * Return the count and accumulated sum of rows in each partition
    * 返回每个分区中的行数和累计总和
   */
  def getPerPartitionCountAndAccumulatedSum[T](rdd: RDD[T]): Map[Int, (Long, Long)] = {
    //Count the rows in each partition, then order the counts by partition number
    //计算每个分区中的行数,然后按分区数量排序
    val counts = rdd.mapPartitionsWithIndex(
      (i: Int, rows: Iterator[T]) => Iterator.single((i, rows.size.toLong)))
      .collect()
      .sortBy(_._1)

    //Create cumulative sums of row counts by partition, e.g. 1 -> 200, 2-> 400, 3-> 412
    //if there were 412 rows divided into two 200 row partitions and one 12 row partition
    val sums = counts.scanLeft((0L, 0L)) {
      (t1, t2) => (t2._1, t1._2 + t2._2)
    }
      .drop(1) //first one is (0,0), drop that
      .toMap

    //Put the per-partition counts and cumulative counts together
    //将每个分区计数和累计计数放在一起
    val sumsAndCounts = counts.map {
      case (part, count) => (part, (count, sums(part)))
    }.toMap
    sumsAndCounts
  }

  /**
   * Remove duplicate rows identified by the key
    * 删除由Key标识的重复行
   * @param pairRdd rdd which has (key, value) structure in each row
    *                rdd，每行有(key,value)结构
   */
  def removeDuplicatesByKey(pairRdd: RDD[(List[Any], Row)]): RDD[Row] = {
    pairRdd.reduceByKey((x, y) => x).map(x => x._2)
  }

}
