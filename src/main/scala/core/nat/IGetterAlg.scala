package org.hablapps.stateless
package core
package nat

import scalaz.{ Monad, MonadReader, ~> }
import scalaz.Leibniz.===
import scalaz.Id.Id
import scalaz.syntax.functor._

import shapeless._, ops.hlist._

trait IGetterAlg[P[_], I <: HList, A] extends raw.IGetterAlg[P, I, A]
    with IOpticAlg[P, I, A, MonadReader, Id] {

  def get: P[(I, A)] = hom(ev.ask.strengthL)

  /* composing algebras */

  def composeFold[J <: HList, K <: HList, B](
      fl: IFoldAlg[Q, J, B])(implicit
      ev0: Prepend.Aux[I, J, K]): IFoldAlg.Aux[P, fl.Q, K, B] =
    asFold.composeFold(fl)

  def composeGetter[J <: HList, K <: HList, B](
      gt: IGetterAlg[Q, J, B])(implicit
      ev0: Prepend.Aux[I, J, K]): IGetterAlg.Aux[P, gt.Q, K, B] =
    IGetterAlg(new (λ[x => K => gt.Q[x]] ~> P) {
      def apply[X](iqx: K => gt.Q[X]): P[X] =
        hom[X](i => gt.hom[X](j => iqx(i ++ j)))
    })(this, gt.ev)

  def composeTraversal[J <: HList, K <: HList, B](
      tr: ITraversalAlg[Q, J, B])(implicit
      ev0: Prepend.Aux[I, J, K]): IFoldAlg.Aux[P, tr.Q, K, B] =
    composeFold(tr.asFold)

  def composeOptional[J <: HList, K <: HList, B](
      op: IOptionalAlg[Q, J, B])(implicit
      ev0: Prepend.Aux[I, J, K]): IFoldAlg.Aux[P, op.Q, K, B] =
    composeFold(op.asFold)

  def composeLens[J <: HList, K <: HList, B](
      ln: ILensAlg[Q, J, B])(implicit
      ev0: Prepend.Aux[I, J, K]): IGetterAlg.Aux[P, ln.Q, K, B] =
    composeGetter(ln.asGetter)

  /* transforming algebras */

  def asFold: IFoldAlg.Aux[P, Q, I, A] =
    IFoldAlg(λ[λ[x => I => Q[x]] ~> λ[x => P[List[x]]]] { qx =>
      map(hom(qx))(List(_))
    })(this, ev)

  def asPlain(implicit ev0: I === HNil): GetterAlg.Aux[P, Q, A] =
    GetterAlg[P, Q, A](new (Q ~> P) {
      def apply[X](qx: Q[X]): P[X] = hom[X](_ => qx)
    })(this, ev)
}

object IGetterAlg {

  type Aux[P[_], Q2[_], I <: HList, A] = IGetterAlg[P, I, A] { type Q[x] = Q2[x] }

  def apply[P[_], Q2[_], I <: HList, A](
      hom2: λ[x => I => Q2[x]] ~> P)(implicit
      ev0: Monad[P],
      ev1: MonadReader[Q2, A]): Aux[P, Q2, I, A] = new IGetterAlg[P, I, A] {
    type Q[x] = Q2[x]
    def point[X](x: => X) = ev0.point(x)
    def bind[X, Y](fx: P[X])(f: X => P[Y]): P[Y] = ev0.bind(fx)(f)
    implicit val ev = ev1
    val hom = hom2
  }

  implicit def toIndexed[P[_], A](gt: GetterAlg[P, A]): Aux[P, gt.Q, HNil, A] =
    gt.asIndexed
}
