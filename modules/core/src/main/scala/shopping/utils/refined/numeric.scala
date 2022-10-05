package shopping.utils.refined

import eu.timepit.refined.api.Validate
import eu.timepit.refined.internal.WitnessAs
import eu.timepit.refined.numeric.Positive
import scala.annotation.tailrec

object numeric {

  final case class IntegralOfSize [N] (n: N)

  object IntegralOfSize {

    private def isSizeOf (n: Long, bound: Long): Boolean = {

      @tailrec
      def go (x: Long, acc: Long): Boolean =
        if (acc > bound) false
        else if (acc == bound) x == 0
        else if (x == 0) acc == bound
        else go(x / 10, acc + 1)
      
      go(n, 0)
    }

    implicit def validateIntegralOfSize [T, N] (implicit
      wn: WitnessAs[N, T],
      int: Integral[T]
    ): Validate.Plain[T, IntegralOfSize[N]] = {
      
      val bound = int.toLong(wn.snd)
      Validate.fromPredicate (
        t => isSizeOf(int.toLong(t), bound),
        t => s"numDigits($t) == $bound",
        IntegralOfSize(wn.fst)
      )
    }
  }

}