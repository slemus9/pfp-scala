package shopping.pagination

import cats.syntax.all._
import cats.effect.{Resource, MonadCancelThrow}
import skunk.{Session, Query, Cursor}

// TODO: Define a type for Pagination
object Paginated {


  def fromQuery [F[_]: MonadCancelThrow, A, B] (
    size: Int,
    query: Query[A, B],
    args: A,
    postgres: Resource[F, Session[F]]
  ): F[LazyList[List[B]]] = {

    val cursorResource = for {
      session <- postgres
      q       <- session.prepare(query)
      cursor  <- q.cursor(args)
    } yield cursor

    cursorResource.use { cursor => 
      
      for {
        result    <- cursor.fetch(size)
        val (items, hasMore) = result
        nextItems <- 
          if (hasMore) fromQuery(size, query, args, postgres)
          else LazyList.empty.pure[F]
      } yield items #:: nextItems
    }
  }
}