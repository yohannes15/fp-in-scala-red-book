package datastructures

import scala.util.control.NonFatal

enum Try[+T]:
  case Failure(exception: Throwable)
  case Success(value: T)

object Try:
  /** Constructs a `Try` using the by-name parameter as a result value.
    *
    * The evaluation of `r` is attempted once.
    *
    * Any non-fatal exception is caught and results in a `Failure` that holds
    * the exception.
    *
    * @tparam T
    *   the type of the value to be computed
    * @param r
    *   the result value to compute
    * @return
    *   the result of evaluating the value, as a `Success` or `Failure`
    */
  def apply[T](r: => T): Try[T] =
    try
      val r1 = r
      Success(r1)
    catch
      case NonFatal(e) => Failure(e)
