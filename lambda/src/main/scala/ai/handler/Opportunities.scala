package ai.handler

import java.util.Base64
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import sttp.client4.*
import cats.data.EitherT
import cats.effect.IO
import ujson.Value.Value
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps
import io.circe.parser.decode
import io.circe.generic.auto._
import ai.model.Opportunity
import io.circe

object Opportunities {

  val backend = DefaultFutureBackend()

  def fetchOpportunities(): IO[List[Opportunity]] = {
    val request = basicRequest
      .get(uri"https://api.yoma.world/api/v1/opportunities")
      .header("accept", "application/json")
      .response(asStringAlways)

    val decodedOpportunities: EitherT[IO, String, String] = for {
      response        <- EitherT.liftF(IO.fromFuture(IO(request.send(backend))))
      parsed          <- EitherT.fromEither(
                           Try(ujson.read(response.body)).toEither.left
                             .map(e => e.getMessage)
                         )
      data            <- EitherT.fromEither(
                           Try(parsed("data").str).toEither.left
                             .map(e => e.getMessage)
                             .flatMap(s =>
                               Either
                                 .cond(
                                   s.nonEmpty,
                                   s,
                                   s"Failure in fetching opportunities data: ${response.body}"
                                 )
                             )
                         )
      decodedData      = decodeBase64(data)
      decompressedData = decompressGzip(decodedData)
    } yield decompressedData

    val listOfOpportunities: IO[List[Opportunity]] = decodedOpportunities.value.flatMap {
      case Right(decodeSuccess) =>
        val parsed: Either[circe.Error, List[Opportunity]] =
          decode[List[Opportunity]](decodeSuccess)
        IO.fromEither(parsed).handleErrorWith { parseError =>
          scribe.warn(s"Opportunities parsing error: $parseError")
          IO.pure(List.empty[Opportunity])
        }

      case Left(decodeError) =>
        val r = s"Opportunities decoding error: $decodeError"
        scribe.warn(r)
        IO.pure(List.empty[Opportunity])
    }

    listOfOpportunities
  }

  private def decodeBase64(encoded: String): Array[Byte] = {
    Base64.getDecoder.decode(encoded)
  }

  private def decompressGzip(compressed: Array[Byte]): String = {
    val inputStream  = new GZIPInputStream(new ByteArrayInputStream(compressed))
    val outputStream = new ByteArrayOutputStream()

    val buffer = new Array[Byte](1024)
    var length = 0
    while ({ length = inputStream.read(buffer); length } > 0) {
      outputStream.write(buffer, 0, length)
    }

    new String(outputStream.toByteArray, "UTF-8")
  }
}
