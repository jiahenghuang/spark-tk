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
package org.trustedanalytics.sparktk.testutils

import java.io.File

import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger

/**
 * Utility methods for working with directories.
  *用于处理目录的实用方法
 */
object DirectoryUtils {

  private val log: Logger = Logger.getLogger(DirectoryUtils.getClass)

  /**
   * Create a Temporary directory
    * 创建一个临时目录
   * @param prefix the prefix for the directory name, this is used to make the Temp directory more identifiable.
    *               目录名称的前缀,这用于使Temp目录更易于识别
   * @return the temporary directory
   */
  def createTempDirectory(prefix: String): File = {
    try {
      val tmpDir = convertFileToDirectory(File.createTempFile(prefix, "-tmp"))

      // Don't rely on this- it is just an extra safety net
      //不要依赖这个 - 它只是一个额外的安全
      tmpDir.deleteOnExit()

      tmpDir
    }
    catch {
      case e: Exception =>
        throw new RuntimeException("Could NOT initialize temp directory, prefix: " + prefix, e)
    }
  }

  /**
   * Convert a file into a directory
    * 将文件转换为目录
   * @param file a file that isn't a directory
    *             一个不是目录的文件
   * @return directory with same name as File
    *         文件同名的目录
   */
  private def convertFileToDirectory(file: File): File = {
    file.delete()
    if (!file.mkdirs()) {
      throw new RuntimeException("Failed to create tmpDir: " + file.getAbsolutePath)
    }
    file
  }

  def deleteTempDirectory(tmpDir: File) {
    FileUtils.deleteQuietly(tmpDir)
    if (tmpDir != null && tmpDir.exists) {
      log.error("Failed to delete tmpDir: " + tmpDir.getAbsolutePath)
    }
  }
}
