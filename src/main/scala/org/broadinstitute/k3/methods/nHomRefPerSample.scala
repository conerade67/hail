package org.broadinstitute.k3.methods

import org.broadinstitute.k3.variant._

import scala.collection.Map

// FIXME need to account for all HomRef
object nHomRefPerSample {
  def apply(vds: VariantDataset): Map[Int, Int] = {
    vds
      .mapValues(g => if (g.isHomRef) 1 else 0)
      .foldBySample(0)(_ + _)
  }
}
