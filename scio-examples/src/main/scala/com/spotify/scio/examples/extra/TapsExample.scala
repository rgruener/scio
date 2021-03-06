/*
 * Copyright (c) 2016 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.scio.examples.extra

import com.spotify.scio._
import com.spotify.scio.io.Taps

object TapsExample {

  def main(cmdlineArgs: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val taps = Taps()  // entry point to acquire taps

    val r = for {
      // extract Tap[T]s from two Future[Tap[T]]s
      t1 <- taps.textFile("kinglear.txt")
      t2 <- taps.textFile("macbeth.txt")
    } yield {
      // execution logic when both taps are available
      val (sc, args) = ContextAndArgs(cmdlineArgs)
      val out = (t1.open(sc) ++ t2.open(sc))
        .flatMap(_.split("[^a-zA-Z']+").filter(_.nonEmpty))
        .countByValue()
        .map(kv => kv._1 + "\t" + kv._2)
        .materialize
      sc.close()
      out
    }

    println(r.waitForResult().value.take(10).toList)
  }

}
