/*
 * Copyright 2016 Spotify AB.
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

package com.spotify.scio.extra

import com.spotify.scio.extra.Iterators._
import org.scalacheck.Prop.{BooleanOperators, all, forAll}
import org.scalacheck._

object IteratorsSpec extends Properties("Iterators") {

  val maxInterval = 10L

  val timeSeries = Gen.listOf[Long](Gen.choose(0L, maxInterval)).map {
    case head :: tail => tail.scanLeft(head)(_ + _)
    case Nil => Nil
  }

  def isWithinBounds(xs: Seq[Long], size: Long, offset: Long): Boolean = {
    xs.forall(x => x >= lowerBound(x, size, offset) && x < upperBound(x, size, offset))
  }

  def windowSize(xs: Seq[Long]): Long = xs.last - xs.head

  implicit class PairsIterable[T](self: Iterable[T]) {
    def pairs: Iterator[(T, T)] = self.sliding(2).filter(_.size == 2).map(s => (s.head, s.last))
  }

  val fixedParams = for {
    size <- Gen.choose(1L, maxInterval)
    offset <- Gen.choose(0L, size - 1L)
  } yield (size, offset)

  property("fixed") = forAll(timeSeries, fixedParams) { case (ts, (size, offset)) =>
    val r = ts.iterator.timeSeries(identity).fixed(size, offset).toList
    all(
      "flatten"  |: r.flatten == ts,
      "nonEmpty" |: r.forall(_.nonEmpty),
      "size"     |: r.forall(windowSize(_) < size),
      "bounds"   |: r.forall(isWithinBounds(_, size, offset))
    )
  }

  property("session") = forAll(timeSeries, Gen.choose(1L, maxInterval)) { (ts, gap) =>
    val r = ts.iterator.timeSeries(identity).session(gap).toList
    all(
      "flatten"     |: r.flatten == ts,
      "nonEmpty"    |: r.forall(_.nonEmpty),
      "gap inside"  |: r.forall(_.pairs.forall(p => p._2 - p._1 < gap)),
      "gap between" |: r.pairs.forall(s => s._2.head - s._1.last >= gap)
    )
  }

  val slidingParams = for {
    size <- Gen.choose(1L, maxInterval)
    offset <- Gen.choose(0L, size - 1L)
    period <- Gen.choose(offset + 1L, maxInterval)
  } yield (size, period, offset)

  property("sliding") = Prop.forAll(timeSeries, slidingParams) { case (ts, (size, period, offset)) =>
    val r = ts.iterator.timeSeries(identity).sliding(size, period, offset).toList
    all(
      "nonEmpty" |: r.forall(_.nonEmpty),
      "size"     |: r.forall(windowSize(_)< size),
      "bounds"   |: r.forall(isWithinBounds(_, size, offset)),
      "lower"    |: r.map(w => lowerBound(w.head, period, offset)).pairs.forall(p => p._2 - p._1 >= period),
      "upper"    |: r.map(w => upperBound(w.head, period, offset)).pairs.forall(p => p._2 - p._1 >= period)
    )
  }

}
