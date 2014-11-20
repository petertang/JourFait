package models

case class Account(
  username: String,
  firstName: String,
  lastName: String,
  email: String)

trait Password {
  def password: String
}

