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
// import ai.model.decodeZonedDateTime
import io.circe

object Opportunities {

  val backend = DefaultFutureBackend()

  def fetchData(): IO[String] = {
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

    val listOfOpportunities = decodedOpportunities.value.flatMap {
      case Right(successValue) =>
        val parsed = decode[List[Opportunity]](successValue)
        val r      = s"Success: $parsed"
        scribe.warn(r)
        IO.pure(r)
      case Left(errorValue)    =>
        val r = s"Error: $errorValue"
        scribe.warn(r)
        IO.pure(r)
    }
    listOfOpportunities
  }

  def decodeBase64(encoded: String): Array[Byte] = {
    Base64.getDecoder.decode(encoded)
  }

  def decompressGzip(compressed: Array[Byte]): String = {
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
