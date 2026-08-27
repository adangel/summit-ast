/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.summit

import com.google.summit.ast.CompilationUnit
import com.google.summit.serialization.Serializer
import com.google.summit.symbols.SummitResolver
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import org.slf4j.LoggerFactory

/**
 * This is a simple command line tool to parse and translate Apex source files.
 *
 * Pass arguments that are file paths or directories to search. All files with the `.cls` and
 * `.trigger` extensions will be read.
 *
 * If the first argument is `-json`, then the [Serializer] will be used to write the AST
 * as JSON to a file.
 */
object SummitTool {
  private val logger = LoggerFactory.getLogger(SummitTool::class.java)

  // TODO: it'll be useful to have this support flags.
  @JvmStatic
  fun main(args: Array<String>) {
    // TODO: using a logger here (in this class specifically) does not seem like
    //   the right thing to do.
    logger.info("Summit AST Tool")
    logger.info("Usage: SummitTool [-json] <Apex files or search directories>")

    var numFiles = 0
    var numFailures = 0
    var serializer : Serializer? = null;
    var filesOrDirectories : List<String> = args.toList()

    if (args.firstOrNull() == "-json") {
      logger.info("Serializing parsed Apex sources to JSON")
      serializer = Serializer(true)
      filesOrDirectories = args.drop(1)
    }

    for (arg in filesOrDirectories) {
      logger.info("Searching for Apex source at: {}", arg)

      val inputPath = Paths.get(arg);

      try {
        val stream: Stream<Path> =
          Files.find(
            inputPath,
            Integer.MAX_VALUE,
            { path, _ -> SummitAST.isApexSourceFile(path) }
          )

        val paths = stream.collect(Collectors.toList())
        val allAsts = paths.mapNotNull { path ->
          numFiles++
          var compilationUnit : CompilationUnit? = null

          try {
            compilationUnit = SummitAST.parseAndTranslate(path)

            if (serializer != null) {
              val json = serializer.serialize(compilationUnit)
              val jsonFile = path.resolveSibling(path.fileName.toString() + ".json")
              Files.write(jsonFile, Collections.singleton(json), StandardCharsets.UTF_8)
              logger.info("Serialized into {}", jsonFile)
            }
          } catch (e: SummitAST.ParseException) {
            logger.warn("Couldn't parse {}", path, e)
          }

          compilationUnit
        }
        SummitResolver().resolve(allAsts)
        numFailures = numFiles - allAsts.size
      } catch (e: IOException) {
        logger.warn("Invalid path {}", arg, e)
      }
    }

    logger.info("Found {} Apex source files", numFiles)
    logger.info("Failed to build AST for {} files", numFailures)
  }
}
