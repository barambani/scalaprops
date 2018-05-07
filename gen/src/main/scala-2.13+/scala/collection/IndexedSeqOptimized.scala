package scala.collection

import scala.collection.mutable.Builder

// workaround scalaz (osgi plugin?) issue
trait IndexedSeqOptimized[+A, +Repr] extends Any {
  protected[this] def newBuilder: Builder[A, Repr]
}
