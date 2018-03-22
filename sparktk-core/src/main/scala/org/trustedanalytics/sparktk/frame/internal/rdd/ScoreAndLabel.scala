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
 * Frequency of scores and corresponding labels in model predictions
 * 在模型预测中得分和相应标签的频率
 * @param score Score or prediction by model
  *             按模型评分或预测
 * @param label Ground-truth label 真相标签
 * @param frequency Frequency of predictions that match score and label
  *                  与分数和标签相匹配的预测频率
 * @tparam T Type of score and label 分数和标签的类型
 */
case class ScoreAndLabel[T](score: T, label: T, frequency: Long = 1)