package controllers

import models.Account
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsPath
import play.api.libs.json.Reads
import play.api.libs.json.Reads._
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Reads.functorReads
import play.api.mvc.Action
import play.api.mvc.Controller
import models.Tables._
import play.api.db.slick.DBAction
import play.api.db.slick._
import play.api.libs.json.JsError

object AccountController extends Controller {

  implicit val accountRead: Reads[(Account, String)] = (
    (JsPath \ "username").read[String] and
    (JsPath \ "firstName").read[String] and
    (JsPath \ "lastName").read[String] and
    (JsPath \ "email").read[String](email) and
    (JsPath \ "password").read[String])(accountJsonToAccountWithPassword _)

  def accountJsonToAccountWithPassword(username: String, firstName: String, lastName: String, email: String, password: String) = {
    new Tuple2(Account(username, firstName, lastName, email), password)
  }

  def createAccount = DBAction(parse.json) {
    implicit rs =>
      val account = rs.body
      account.validate[(Account, String)].fold(
        valid = {
          case (account, password) =>
            val newAccount = Accounts.createAccount(account, password)
            Ok //(Json.toJson(newTask))
        },
        invalid = {
          errors => System.out.println(errors); BadRequest(JsError.toFlatJson(errors))
        })

  }
}