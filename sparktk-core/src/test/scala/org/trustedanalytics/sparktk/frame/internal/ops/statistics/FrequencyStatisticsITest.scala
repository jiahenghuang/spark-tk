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
package org.trustedanalytics.sparktk.frame.internal.ops.statistics

import org.scalatest.Matchers
import org.apache.spark.rdd.RDD
import org.trustedanalytics.sparktk.testutils.TestingSparkContextWordSpec

/**
 * Tests the frequency statistics package through several corner cases and bad-data cases, as well as "happy path"
  * 通过几个角落案例和不良数据案例来测试频率统计包,
 * use cases with both normalized and un-normalized weights.
  * 具有标准化和非标准化权重的用例
 */
class FrequencyStatisticsITest extends TestingSparkContextWordSpec with Matchers {

  trait FrequencyStatisticsTest {
    val epsilon = 0.000000001

    val integers = (1 to 6) :+ 1 :+ 7 :+ 3

    val strings = List("a", "b", "c", "d", "e", "f", "a", "g", "c")

    val integerFrequencies = List(1, 1, 5, 1, 7, 2, 2, 3, 2).map(_.toDouble)

    val modeFrequency = 7.toDouble
    val totalFrequencies = integerFrequencies.sum

    val fractionalFrequencies: List[Double] = integerFrequencies.map(x => x / totalFrequencies)

    val modeSetInts = Set(3, 5)
    val modeSetStrings = Set("c", "e")

    val firstModeInts = Set(3)
    val firstModeStrings = Set("c")
    val maxReturnCount = 10
  }
  //频率统计
  "FrequencyStatistics" should {
    //产生模式==无，权重等于0
    "produce mode == None and weights equal to 0" in new FrequencyStatisticsTest {
      val dataList: List[Double] = List()
      val weightList: List[Double] = List()

      val dataWeightPairs = sparkContext.parallelize(dataList.zip(weightList))

      val frequencyStats = new FrequencyStatistics[Double](dataWeightPairs, maxReturnCount)

      val testModeSet = frequencyStats.modeSet
      val testModeWeight = frequencyStats.weightOfMode
      val testTotalWeight = frequencyStats.totalWeight

      testModeSet should be('empty)
      testModeWeight shouldBe 0
      testTotalWeight shouldBe 0
    }
    //具有整数频率的整数数据
    "integer data with integer frequencies" in new FrequencyStatisticsTest {
      val dataWeightPairs = sparkContext.parallelize(integers.zip(integerFrequencies))

      val frequencyStats = new FrequencyStatistics(dataWeightPairs, maxReturnCount)

      val testModeSet = frequencyStats.modeSet
      val testModeWeight = frequencyStats.weightOfMode
      val testTotalWeight = frequencyStats.totalWeight

      testModeSet shouldBe modeSetInts
      testModeWeight shouldBe modeFrequency
      testTotalWeight shouldBe totalFrequencies
    }
    //整数频率的字符串数据
    "string data with integer frequencies" in new FrequencyStatisticsTest {
      val dataWeightPairs = sparkContext.parallelize(strings.zip(integerFrequencies))

      val frequencyStats = new FrequencyStatistics(dataWeightPairs, maxReturnCount)

      val testModeSet = frequencyStats.modeSet
      val testModeWeight = frequencyStats.weightOfMode
      val testTotalWeight = frequencyStats.totalWeight

      testModeSet shouldBe modeSetStrings
      testModeWeight shouldBe modeFrequency
      testTotalWeight shouldBe totalFrequencies
    }
    //具有整数频率的整数数据在请求一个时应该获得最少的模式
    "integer data with integer frequencies should get least mode when asking for just one" in new FrequencyStatisticsTest {
      val dataWeightPairs = sparkContext.parallelize(integers.zip(integerFrequencies))

      val frequencyStats = new FrequencyStatistics(dataWeightPairs, 1)

      val testModeSet = frequencyStats.modeSet
      val testModeWeight = frequencyStats.weightOfMode
      val testTotalWeight = frequencyStats.totalWeight

      testModeSet shouldBe firstModeInts
      testModeWeight shouldBe modeFrequency
      testTotalWeight shouldBe totalFrequencies
    }
    //整数频率的字符串数据在请求一个时应该得到最少的模式
    "string data with integer frequencies should get least mode when asking for just one" in new FrequencyStatisticsTest {
      val dataWeightPairs = sparkContext.parallelize(strings.zip(integerFrequencies))

      val frequencyStats = new FrequencyStatistics(dataWeightPairs, 1)

      val testModeSet = frequencyStats.modeSet
      val testModeWeight = frequencyStats.weightOfMode
      val testTotalWeight = frequencyStats.totalWeight

      testModeSet shouldBe firstModeStrings
      testModeWeight shouldBe modeFrequency
      testTotalWeight shouldBe totalFrequencies
    }
    //具有分数权重的整数数据
    "integer data with fractional weights" in new FrequencyStatisticsTest {
      val dataWeightPairs = sparkContext.parallelize(integers.zip(fractionalFrequencies))

      val frequencyStats = new FrequencyStatistics(dataWeightPairs, maxReturnCount)

      val testModeSet = frequencyStats.modeSet
      val testModeWeight = frequencyStats.weightOfMode
      val testTotalWeight = frequencyStats.totalWeight

      testModeSet shouldBe modeSetInts
      Math.abs(testModeWeight - (modeFrequency / totalFrequencies)) should be < epsilon
      Math.abs(testTotalWeight - 1.toDouble) should be < epsilon
    }
    //具有分数权重的字符串数据
    "string data  with fractional weights" in new FrequencyStatisticsTest {
      val dataWeightPairs = sparkContext.parallelize(strings.zip(fractionalFrequencies))

      val frequencyStats = new FrequencyStatistics(dataWeightPairs, maxReturnCount)

      val testModeSet = frequencyStats.modeSet
      val testModeWeight = frequencyStats.weightOfMode
      val testTotalWeight = frequencyStats.totalWeight

      testModeSet shouldBe modeSetStrings
      Math.abs(testModeWeight - (modeFrequency / totalFrequencies)) should be < epsilon
      Math.abs(testTotalWeight - 1.toDouble) should be < epsilon
    }
    //负重物品不应影响模式或总重量
    "items with negative weights should not affect mode or total weight" in new FrequencyStatisticsTest {
      val dataWeightPairs: RDD[(String, Double)] =
        sparkContext.parallelize((strings :+ "haha").zip(fractionalFrequencies :+ -10.0))

      val frequencyStats = new FrequencyStatistics[String](dataWeightPairs, maxReturnCount)

      val testMode = frequencyStats.modeSet
      val testModeWeight = frequencyStats.weightOfMode
      val testTotalWeight = frequencyStats.totalWeight

      testMode shouldBe modeSetStrings
      Math.abs(testModeWeight - (modeFrequency / totalFrequencies)) should be < epsilon
      Math.abs(testTotalWeight - 1.toDouble) should be < epsilon
    }
  }
}
