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
package org.apache.spark.ml.org.trustedanalytics.sparktk.deeptrees.util

// scalastyle:off

import org.apache.spark.Logging
import org.scalatest.{ FunSuite, Outcome }

/**
 * Base abstract class for all unit tests in Spark for handling common functionality.
  * Spark中所有单元测试的基本抽象类,用于处理常用功能
 */
private[spark] abstract class SparkFunSuite extends FunSuite with Logging {
  // scalastyle:on

  /**
   * Log the suite name and the test name before and after each test.
   * 在每次测试之前和之后记录套件名称和测试名称
   * Subclasses should never override this method. If they wish to run
   * custom code before and after each test, they should mix in the
   * {{org.scalatest.BeforeAndAfter}} trait instead.
    * 子类不应该重写此方法,如果他们希望在每次测试之前和之后运行自定义代码,则应该改用{{org.scalatest.BeforeAndAfter}}特性。
   */
  final protected override def withFixture(test: NoArgTest): Outcome = {
    val testName = test.text
    val suiteName = this.getClass.getName
    val shortSuiteName = suiteName.replaceAll("org.apache.spark", "o.a.s")
    try {
      logInfo(s"\n\n===== TEST OUTPUT FOR $shortSuiteName: '$testName' =====\n")
      test()
    }
    finally {
      logInfo(s"\n\n===== FINISHED $shortSuiteName: '$testName' =====\n")
    }
  }

}
