package datastructures

/** An optional value that may ([[Some]]) or may not ([[None]]) be present.
  *
  * `Option` is a container of size one——it either holds a single value of type
  * `A` ([[Some]]) or is empty ([[None]]). This eliminates null-pointer errors
  * and forces the caller to handle both cases explicitly.
  */
enum Option[+A]:
  case Some(get: A)
  case None

  /** Transform the inner value by applying `f` when this is [[Some]].
    *
    * If this is [[None]] the function is not applied and [[None]] is returned.
    * This is the fundamental "lift" operation that lets pure functions work
    * inside the `Option` context.
    *
    * @param f
    *   the function to apply to the inner value (if present)
    * @return
    *   `Some(f(a))` if this is `Some(a)`, otherwise `None`
    */
  def map[B](f: A => B): Option[B] = this match
    case Some(a) => Some(f(a))
    case None    => None

  /** Return the inner value if this is [[Some]], otherwise return `default`.
    *
    * The `default` parameter is passed '''by-name''' (`=> B`), meaning it is
    * evaluated only when `this` is [[None]]. If `this` is [[Some]] the default
    * is never computed——a useful optimisation when the default expression is
    * expensive.
    *
    * @param default
    *   by-name default value, evaluated only when needed
    * @return
    *   the inner value or `default`
    */
  def getOrElse[B >: A](default: => B): B = this match
    case Some(get) => get
    case None      => default

  /** Apply `f`, which itself may produce an [[Option]], and flatten the result.
    *
    * Equivalent to `map(f).getOrElse(None)`. Useful for chaining operations
    * that each return `Option`——if any step returns [[None]] the chain
    * short-circuits.
    *
    * @param f
    *   the function to apply, returning an `Option[B]`
    * @return
    *   the inner `Option[B]` if this is `Some(a)`, otherwise `None`
    */
  def flatMap[B](f: A => Option[B]): Option[B] =
    map(f).getOrElse(None)

  /** Return this [[Option]] if it is [[Some]], otherwise return the alternative
    * `ob`. orElse is different from getOrElse in that it doesn't unwrap the
    * value and we can stay in [[Option]] land which can allow us to do chaining
    * and other nicer operations on errors and defaults until chain fails.
    *
    * Like `getOrElse`, the alternative is passed '''by-name''' and is only
    * evaluated when this is [[None]]. This is the `Option` analogue of the
    * logical "or" operation.
    *
    * @param ob
    *   by-name alternative `Option`, evaluated only when needed
    * @return
    *   `this` if it is `Some(a)`, otherwise `ob`
    */
  def orElse[B >: A](ob: => Option[B]): Option[B] =
    map(a => Some(a)).getOrElse(ob)
    // this match
    // case Some(_) => this   // keep the original Some
    // case None    => ob     // fall back

  /** Keep the inner value only if it satisfies the predicate `f`.
    *
    * Equivalent to `flatMap(a => if f(a) then Some(a) else None)`. If this is
    * [[None]] or the predicate returns `false`, [[None]] is returned.
    *
    * @param f
    *   the test predicate
    * @return
    *   `Some(a)` if `this` is `Some(a)` and `f(a)` is `true`, else `None`
    */
  def filter(f: A => Boolean): Option[A] =
    flatMap(a => if f(a) then Some(a) else None)
