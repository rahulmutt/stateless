package org.hablapps.stateless
package core
package raw

import scalaz.Monad

trait IGetterAlg[P[_], I, A] extends Monad[P] {

  def get: P[(I, A)]
}
