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

package com.spotify.scio

import com.google.cloud.dataflow.sdk.coders.CoderRegistry
import com.google.cloud.dataflow.sdk.testing.TestPipeline
import com.spotify.scio.avro.TestRecord
import com.spotify.scio.coders.CoderTestUtils._
import com.spotify.scio.testing.TestingUtils._
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.ClassTag

class RichCoderRegistryTest extends FlatSpec with Matchers {

  import Implicits._

  val pipeline = TestPipeline.create()
  val registry = pipeline.getCoderRegistry
  registry.registerScalaCoders()

  class RoundTripMatcher[T: ClassTag](value: T) extends Matcher[CoderRegistry] {
    override def apply(left: CoderRegistry): MatchResult = {
      val coder = left.getScalaCoder[T]
      coder shouldNot be (null)
      MatchResult(
        testRoundTrip(coder, value),
        s"CoderRegistry did not round trip $value",
        s"CoderRegistry did round trip $value")
    }
  }

  private def roundTrip[T: ClassTag](value: T) = new RoundTripMatcher[T](value)

  "RichCoderRegistry" should "support Scala primitives" in {
    registry should roundTrip (10)
    registry should roundTrip (10L)
    registry should roundTrip (10F)
    registry should roundTrip (10.0)
  }

  it should "support Scala tuples" in {
    registry should roundTrip (("hello", 10))
    registry should roundTrip (("hello", 10, 10.0))
    registry should roundTrip (("hello", (10, 10.0)))
  }

  it should "support Scala case classes" in {
    registry should roundTrip (Pair("record", 10))
  }

  it should "support Avro GenericRecord" in {
    val r = newGenericRecord(1)
    registry should roundTrip (r)
    registry should roundTrip (("key", r))
    registry should roundTrip (CaseClassWithGenericRecord("record", 10, r))
  }

  it should "support Avro SpecificRecord" in {
    val r = new TestRecord(1, 1L, 1F, 1.0, true, "hello")
    registry should roundTrip (r)
    registry should roundTrip (("key", r))
    registry should roundTrip (CaseClassWithSpecificRecord("record", 10, r))
  }

}
