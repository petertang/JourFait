package models

import org.joda.time.DateTime

case class Account(
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  verified: Boolean,
  expiryDate: Option[DateTime])

