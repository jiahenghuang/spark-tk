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
package org.trustedanalytics.sparktk.frame.internal

import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.scalatest.Matchers
import org.trustedanalytics.sparktk.frame.{ Column, DataTypes, FrameSchema }
import org.trustedanalytics.sparktk.testutils._

class FrameRddTest extends TestingSparkContextWordSpec with Matchers {

  "FrameRdd" should {

    /**
     * Method that accepts FrameState as a parameter (for testing implicit conversion).
      * 接受FrameState作为参数的方法(用于测试隐式转换)
     * @return Returns schema column column and rdd row count.
      *         返回模式列列和rdd行数
     */
    def frameStateColumnCount(frameState: FrameState): (Int, Long) = {
      (frameState.schema.columns.length, frameState.rdd.count())
    }

    /**
     * Method that accepts FrameRdd as a parameter (for testing implicit conversion)
      * 接受FrameRdd作为参数的方法(用于测试隐式转换)
     * @return Returns schema column column and rdd row count.
      *         返回模式列列和rdd行数
     */
    def frameRddColumnCount(frameRdd: FrameRdd): (Int, Long) = {
      (frameRdd.frameSchema.columns.length, frameRdd.count())
    }
    //隐式地在FrameState和FrameRdd之间进行转换
    "implicitly convert between FrameState and FrameRdd" in {
      val schema = FrameSchema(Vector(Column("num", DataTypes.int32), Column("name", DataTypes.string)))
      val rows = FrameRdd.toRowRDD(schema, sparkContext.parallelize((1 to 100).map(i => Array(i.toLong, i.toString))).repartition(3))

      val frameRdd = new FrameRdd(schema, rows)
      val frameState = FrameState(rows, schema)

      // Call both methods with FrameState
      //用FrameState调用两个方法
      assert(frameStateColumnCount(frameState) == (2, 100))
      assert(frameRddColumnCount(frameState) == (2, 100))

      // Call both methods with FrameRdd
      //用FrameRdd调用这两个方法
      assert(frameRddColumnCount(frameRdd) == (2, 100))
      assert(frameStateColumnCount(frameRdd) == (2, 100))
    }
  }
}