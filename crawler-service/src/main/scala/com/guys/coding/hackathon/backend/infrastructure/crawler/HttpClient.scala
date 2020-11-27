package com.guys.coding.hackathon.backend.infrastructure.crawler

import org.http4s.client._
import org.http4s._
import org.http4s.Uri
import org.http4s.util.CaseInsensitiveString
import org.http4s.Request
import cats.Applicative
import cats.syntax.functor.toFunctorOps

object HttpClient {

  private val uaValue = "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:83.0) Gecko/20100101 Firefox/83.0"

  def get[F[_]: Applicative](url: String)(implicit client: Client[F], ed: EntityDecoder[F, String]): F[Option[String]] = {
    Uri.fromString(url) match {
      case Left(_) =>
        Applicative[F].pure(None)

      case Right(url) =>
        val request = Request[F](
          uri = url,
          headers = Headers.of(Header.Raw(CaseInsensitiveString("User-Agent"), uaValue))
        )

        client.expect[String](request).map(Some(_))
    }
  }

}
