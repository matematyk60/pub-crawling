import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.client.blaze._
import org.http4s.client._
import org.http4s.client.middleware.FollowRedirect
import org.http4s.util.CaseInsensitiveString

import scala.concurrent.ExecutionContext.global
import cats.effect.Blocker
import java.util.concurrent._

implicit val cs: ContextShift[IO] = IO.contextShift(global)
implicit val timer: Timer[IO]     = IO.timer(global)

val blockingPool           = Executors.newFixedThreadPool(5)
val blocker                = Blocker.liftExecutorService(blockingPool)
val httpClient: Client[IO] = FollowRedirect(maxRedirects = 5)(JavaNetClientBuilder[IO](blocker).create)

val uaValue = "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:83.0) Gecko/20100101 Firefox/83.0"
val query   = "test%20!g"
val url     = s"http://duckduckgo.com/html/?q=$query"

val request = Request[IO](
  uri = Uri.unsafeFromString(url),
  headers = Headers.of(Header.Raw(CaseInsensitiveString("User-Agent"), uaValue))
)

val result = httpClient.expect[String](request).attempt.unsafeRunSync()

val content =
  result match {
    case Left(value)  => value.toString
    case Right(value) => value.toString
  }

content.contains("speedtest")
// content.contains("speedtest")
