package org.broadinstitute.k3

import breeze.linalg.operators.{OpSub, OpAdd}
import org.apache.spark.mllib.linalg.distributed.IndexedRow

import scala.collection.mutable
import scala.language.implicitConversions
import breeze.linalg.{Vector => BVector, DenseVector => BDenseVector, SparseVector => BSparseVector}
import org.apache.spark.mllib.linalg.{Vector => SVector, DenseVector => SDenseVector, SparseVector => SSparseVector, Vectors}

class RichVector[T](v: Vector[T]) {
  def zipExact[T2](v2: Iterable[T2]): Vector[(T, T2)] = {
    val i = v.iterator
    val i2 = v2.iterator
    new Iterator[(T, T2)] {
      def hasNext: Boolean = {
        assert(i.hasNext == i2.hasNext)
        i.hasNext
      }

      def next() = (i.next(), i2.next())
    }.toVector
  }

  def zipWith[T2, V](v2: Iterable[T2], f: (T, T2) => V): Vector[V] = {
    val i = v.iterator
    val i2 = v2.iterator
    new Iterator[V] {
      def hasNext = i.hasNext && i2.hasNext

      def next() = f(i.next(), i2.next())
    }.toVector
  }

  def zipWithExact[T2, V](v2: Iterable[T2], f: (T, T2) => V): Vector[V] = {
    val i = v.iterator
    val i2 = v2.iterator
    new Iterator[V] {
      def hasNext: Boolean = {
        assert(i.hasNext == i2.hasNext)
        i.hasNext
      }

      def next() = f(i.next(), i2.next())
    }.toVector
  }

  def zipWithAndIndex[T2, V](v2: Iterable[T2], f: (T, T2, Int) => V): Vector[V] = {
    val i = v.iterator
    val i2 = v2.iterator
    val i3 = Iterator.from(0)
    new Iterator[V] {
      def hasNext = i.hasNext && i2.hasNext

      def next() = f(i.next(), i2.next(), i3.next())
    }.toVector
  }

  def zipWith[T2, T3, V](v2: Iterable[T2], v3: Iterable[T3], f: (T, T2, T3) => V): Vector[V] = {
    val i = v.iterator
    val i2 = v2.iterator
    val i3 = v3.iterator
    new Iterator[V] {
      def hasNext = i.hasNext && i2.hasNext && i3.hasNext

      def next() = f(i.next(), i2.next(), i3.next())
    }.toVector
  }
}

class RichHomogenousTuple1[T](t: Tuple1[T]) {
  def at(i: Int) = i match {
    case 1 => t._1
  }

  def insert(i: Int, x: T): (T, T) = i match {
    case 0 => (x, t._1)
    case 1 => (t._1, x)
  }

  def remove(i: Int): Unit = {
    require(i == 0)
  }
}

class RichHomogenousTuple2[T](t: (T, T)) {
  def at(i: Int): T = i match {
    case 1 => t._1
    case 2 => t._2
  }


  def insert(i: Int, x: T): (T, T, T) = i match {
    case 0 => (x, t._1, t._2)
    case 1 => (t._1, x, t._2)
    case 2 => (t._1, t._2, x)
  }

  def remove(i: Int): Tuple1[T] = i match {
    case 1 => Tuple1(t._2)
    case 2 => Tuple1(t._1)
  }
}

class RichHomogenousTuple3[T](t: (T, T, T)) {
  def at(i: Int): T = i match {
    case 1 => t._1
    case 2 => t._2
    case 3 => t._3
  }

  def insert(i: Int, x: T): (T, T, T, T) = i match {
    case 0 => (x, t._1, t._2, t._3)
    case 1 => (t._1, x, t._2, t._3)
    case 2 => (t._1, t._2, x, t._3)
    case 3 => (t._1, t._2, t._3, x)
  }

  def remove(i: Int): (T, T) = i match {
    case 1 => (t._2, t._3)
    case 2 => (t._1, t._3)
    case 3 => (t._1, t._2)
  }
}

class RichArrayBuilderOfByte(b: mutable.ArrayBuilder[Byte]) {
  def writeULEB128(x0: Int) {
    require(x0 >= 0)

    var x = x0
    var more = true
    while (more) {
      var c = x & 0x7F
      x = x >>> 7

      if (x == 0)
        more = false
      else
        c = c | 0x80

      assert(c >= 0 && c <= 255)
      b += c.toByte
    }
  }
}

class RichIteratorOfByte(i: Iterator[Byte]) {
  def readULEB128(): Int = {
    var x: Int = 0
    var shift: Int = 0
    var b: Byte = 0
    do {
      b = i.next()
      x = x | ((b & 0x7f) << shift)
      shift += 7
    } while ((b & 0x80) != 0)

    x
  }
}

class RichArray[T](a: Array[T]) {
  def index: Map[T, Int] = a.zipWithIndex.toMap
}

class RichIndexedRow(r: IndexedRow) {

  import Utils._

  def -(that: BVector[Double]): IndexedRow = new IndexedRow(r.index, r.vector - that)

  def +(that: BVector[Double]): IndexedRow = new IndexedRow(r.index, r.vector + that)
}

class RichEnumeration[T <: Enumeration](e: T) {
  def maybeWithName(name: String): Option[T#Value] =
    e.values.find(_.toString == name)
}

object Utils {
  implicit def toRichVector[T](v: Vector[T]): RichVector[T] = new RichVector(v)

  implicit def toRichTuple2[T](t: (T, T)): RichHomogenousTuple2[T] = new RichHomogenousTuple2(t)

  implicit def toRichTuple3[T](t: (T, T, T)): RichHomogenousTuple3[T] = new RichHomogenousTuple3(t)

  implicit def toRichArrayBuilderOfByte(t: mutable.ArrayBuilder[Byte]): RichArrayBuilderOfByte =
    new RichArrayBuilderOfByte(t)

  implicit def toRichIteratorOfByte(i: Iterator[Byte]): RichIteratorOfByte =
    new RichIteratorOfByte(i)

  implicit def richArray[T](a: Array[T]): RichArray[T] = new RichArray(a)

  implicit def toRichIndexedRow(r: IndexedRow): RichIndexedRow =
    new RichIndexedRow(r)

  implicit def toBDenseVector(v: SDenseVector): BDenseVector[Double] =
    new BDenseVector(v.values)

  implicit def toBSparseVector(v: SSparseVector): BSparseVector[Double] =
    new BSparseVector(v.indices, v.values, v.size)

  implicit def toBVector(v: SVector): BVector[Double] = v match {
    case v: SSparseVector => v
    case v: SDenseVector => v
  }

  implicit def toSDenseVector(v: BDenseVector[Double]): SDenseVector =
    new SDenseVector(v.toArray)

  implicit def toSSparseVector(v: BSparseVector[Double]): SSparseVector =
    new SSparseVector(v.length, v.array.index, v.array.data)

  implicit def toSVector(v: BVector[Double]): SVector = v match {
    case v: BDenseVector[Double] => v
    case v: BSparseVector[Double] => v
  }

  implicit object subBVectorSVector
    extends OpSub.Impl2[BVector[Double], SVector, BVector[Double]] {
    def apply(a: BVector[Double], b: SVector): BVector[Double] = a - toBVector(b)
  }

  implicit object subBVectorIndexedRow
    extends OpSub.Impl2[BVector[Double], IndexedRow, IndexedRow] {
    def apply(a: BVector[Double], b: IndexedRow): IndexedRow =
      new IndexedRow(b.index, a - toBVector(b.vector))
  }

  implicit object addBVectorSVector
    extends OpAdd.Impl2[BVector[Double], SVector, BVector[Double]] {
    def apply(a: BVector[Double], b: SVector): BVector[Double] = a + toBVector(b)
  }

  implicit object addBVectorIndexedRow
    extends OpAdd.Impl2[BVector[Double], IndexedRow, IndexedRow] {
    def apply(a: BVector[Double], b: IndexedRow): IndexedRow =
      new IndexedRow(b.index, a + toBVector(b.vector))
  }

  implicit def toRichEnumeration[T <: Enumeration](e: T): RichEnumeration[T] =
    new RichEnumeration(e)

  def fail() {
    assert(false)
  }
}
