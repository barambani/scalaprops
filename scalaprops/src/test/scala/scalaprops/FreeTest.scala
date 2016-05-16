package scalaprops

import scalaz._
import scalaz.std.anyVal._

object FreeTest extends Scalaprops {

  implicit def freeEqual[F[_]: Functor, A](implicit
    E: Eq1[F],
    A: Equal[A]
  ): Equal[Free[F, A]] =
    Equal.equal((aa, bb) =>
      (aa.resume, bb.resume) match {
        case (-\/(a), -\/(b)) =>
          E.eq1[Free[F, A]].equal(a, b)
        case (\/-(a), \/-(b)) =>
          A.equal(a, b)
        case _ =>
          false
      }
    )

  implicit def freeGen[F[_]: Applicative, A](implicit
    F: Gen[F[A]],
    A: Gen[A]
  ): Gen[Free[F, A]] =
    Gen.oneOf(
      A.map(Free.point(_)),
      A.map(Free.return_(_)),
      F.map(Free.liftF(_))
    )


  val bindRecIList =
    scalazlaws.bindRec.laws[({type l[a] = Free[IList, a]})#l].andThenParam(Param.maxSize(1))

  val testMaybe = {
    type F[A] = Free[Maybe, A]

    Properties.list(
      scalazlaws.monad.all[F],
      scalazlaws.zip.all[F],
      scalazlaws.bindRec.all[F],
      scalazlaws.traverse.all[F]
    )
  }

  val testIList = {
    type F[A] = Free[IList, A]

    Properties.list(
      scalazlaws.monad.all[F],
      scalazlaws.zip.all[F],
      scalazlaws.traverse.all[F]
    )
  }

  val testNel = {
    type F[A] = Free[NonEmptyList, A]

    Properties.list(
      scalazlaws.monad.all[F],
      scalazlaws.zip.all[F],
      scalazlaws.traverse1.all[F]
    )
  }

  val testDisjunction = {
    type S[a] = Int \/ a
    type F[A] = Free[S, A]
    implicit val m = Free.freeMonad[S]
    Properties.list(
      scalazlaws.monad.all[F],
      scalazlaws.bindRec.all[F],
      scalazlaws.traverse.all[F]
    )
  }

  val traverseSource = {
    import scalaz.std.tuple._
    import Free.Source
    implicit def sourceTraverse[A0]: Traverse[({type l[a] = Source[a, A0]})#l] = {
      type F[A] = Free.Source[A, A0]
      new Traverse[F] {
        def traverseImpl[G[_] : Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]] = {
          fa.resume match {
            case -\/((head, tailSource)) => {
              Applicative[G].apply2(f(head), traverseImpl[G, A, B](tailSource)(f)) { (mappedHead: B, mappedTail: F[B]) =>
                Free.produce(mappedHead).flatMap { _: Unit =>
                  mappedTail
                }
              }
            }
            case \/-(a) =>
              Applicative[G].point(Free.point[({type l[a] = (B, a)})#l, A0](a))
          }
        }
      }
    }

    // https://github.com/scalaz/scalaz/pull/1159

    //scalazlaws.traverse.all[({type x[a] = Free[({type y[b] = (a, b)})#y, Int]})#x]
    scalazlaws.traverse.all[({type x[a] = Source[a, Int]})#x].andThenParam(Param.minSuccessful(10000))
  }
}
