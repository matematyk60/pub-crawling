package com.guys.coding.hackathon.backend.infrastructure.postgres

import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import doobie.implicits._
import doobie.implicits.javasql.TimestampMeta
import doobie.{implicits => _, _}

object DoobieExampleRepository {

  def getUsers(): ConnectionIO[List[User]] =
    Statements.getUsers
      .map {
        case (id, name, last, birth) => User(id, name, last, birth.toLocalDateTime().atZone(ZoneId.systemDefault()))
      }
      .to[List]

  def insertUser(id: String, name: String, lastName: Option[String], birth: ZonedDateTime): ConnectionIO[Int] =
    Statements.insertUser(id, name, lastName, birth).run

  object Statements {

    val createTable = sql"""
        |CREATE TABLE public.users (
        |    id varchar NOT NULL PRIMARY KEY,
        |    name text NOT NULL,
        |    lastname text,
        |    birthday TIMESTAMP NOT NULL
        |);""".stripMargin.update

    def insertUser(
        id: String,
        name: String,
        lastName: Option[String],
        birthday: ZonedDateTime
    ) =
      Update[(String, String, Option[String], Timestamp)](
        "INSERT INTO public.users (id,name,lastname,birthday) VALUES (?,?, ?, ?)"
      ).toUpdate0((id, name, lastName, Timestamp.from(birthday.toInstant())))

    val getUsers =
      sql"SELECT id,name,lastname,birthday FROM users".query[(String, String, Option[String], Timestamp)]
  }

  case class User(
      id: String,
      name: String,
      lastName: Option[String],
      birthday: ZonedDateTime
  )

}
