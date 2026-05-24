package datastrucutres

enum Option[+A]:
  case Some(get: A)
  case None

  /* Apply f if the option is not None */
  def map[B](f: A => B): Option[B] =
    ???
  /* Apply f, which may fail, to the Option if not None */
  def flatMap[B](f: A => Option[B]): Option[B] =
    ???
  /* default is a by-name parameter. lazy eval only used if needed */
  def getOrElse[B >: A](default: => B): B =
    ???
  /* ob is a by-name parameter. lazy eval only used if needed */
  def orElse[B >: A](ob: => Option[B]): Option[B] =
    ???
  def filter(f: A => Boolean): Option[A] =
    ???
