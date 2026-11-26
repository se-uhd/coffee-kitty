# FR-14: Create a User

## Statement

When an admin creates a user with a login name, an email address, a first name, and a last name, Coffee Kitty MUST store
the user with a coffee count of 0.

A login name consists of ASCII letters, digits, and underscores and has 1 to 255 characters. No two users have the same
login name, and login names that differ only in case are different. An email address has the form name@domain and
contains no control character, and no two users have the same email address, ignoring case. Coffee Kitty MUST store an
email address in lowercase. A first name and a last name are not blank and contain no control character, such as a line
break. A user is an admin only if the request says so.

Coffee Kitty MUST refuse a request by a user who is not an admin with 403 Forbidden and a request without a known acting
user with 401 Unauthorized. Coffee Kitty MUST refuse a request with a value that breaks these rules with 400 Bad Request
and a request with a login name or an email address of another user with 409 Conflict. A refused request MUST NOT store
a user.

## Rationale

The people in the group join and leave all the time, especially students. Each person needs a user before Coffee Kitty
can count their coffees. Because an admin can correct coffee counts (FR-8), a client must not be able to make itself an
admin.

## Success Criteria

- The admin jane_doe creates a user with the login name new_user. Coffee Kitty returns the user with an id, and the
  coffee count of new_user is 0. The user is not an admin.
- A request by jane_doe to create a user as an admin stores an admin, who can then create a user.
- A login name of 1 character and a login name of 255 characters create a user. A request with a login name of 256
  characters, with the login name "jane doe", or with the login name "jäne" returns 400 Bad Request.
- A request with the login name of another user returns 409 Conflict. A request with the login name "Jane_Doe" creates a
  user, although the user jane_doe exists.
- A request with the email address "New.User@SE.Uni-Heidelberg.de" stores the address "new.user@se.uni-heidelberg.de". A
  request with the email address of another user in other letter case returns 409 Conflict. A request with an email
  address that contains the NUL character returns 400 Bad Request.
- A request with a blank first name or a blank last name returns 400 Bad Request.
- A request by maxmustermann, who is not an admin, returns 403 Forbidden. A request without a known acting user returns
  401 Unauthorized.
- On an installation without an admin, Coffee Kitty stores the admin that its configuration names when it starts.

## Verification Method

Test: system tests of the status codes and the stored user, unit tests of the rules for the values, and unit tests of
the admin that Coffee Kitty stores when it starts. The system tests run against PostgreSQL in Testcontainers.
Who: developer of the change. When: before the merge. Where: GitHub Actions.

## Supporting Information

- Input, from the team assistant: "Every person is a user with a login name, an email address, a first name, and a last
  name." "A login name consists only of letters, digits, and underscores: no spaces, dots, or umlauts, and at most 255
  characters. No two users have the same login name. Upper and lower case count as different." "No two users have the
  same email address, and here upper and lower case don't matter." "First and last name must not be empty." "A new user
  starts with a coffee count of 0."
- Input, from the team assistant: "Admins add users: login name, first and last name, email address, and whether the new
  user is an admin." "The very first admin is set up when the app is installed."
- FR-4, FR-6, FR-8, FR-13, FR-17, and FR-19 depend on this requirement, because they need users and admins. An admin
  records a deposit (FR-4), corrects a coffee count (FR-8), sets the price per cup (FR-17), and records an expense for
  a user (FR-19). A user records an expense (FR-6) and adds a coffee (FR-13).
- Technical limit, not a decision of a stakeholder: An email address has at most 254 characters, the longest address
  that a mail server accepts (RFC 5321, section 4.5.3.1.3, and Erratum 1690 to RFC 3696).
- Technical limit, not a decision of a stakeholder: An email address contains no control character. RFC 5321, section
  4.1.2, allows no ASCII control character in an address, and PostgreSQL cannot store the NUL character.
- Verified by: `UserSystemTests`, `UserServiceTest`, and `BootstrapAdminLoaderTest`.
- Coffee Kitty 0.1.0 had this function without a requirement and without roles. This requirement records it with a new
  ID.
- TBD (open): What is the maximum length of a first name and of a last name? Until the team assistant answers this
  question, this requirement limits a name to 255 characters, the width of the columns that store the names, so that no
  request can store a text of any length. Contact person: team assistant.
- TBD (open): How does Coffee Kitty make sure that the acting user is the person who sends the request? Coffee Kitty has
  no authentication yet. Until the group lead answers this question, this requirement accepts the login name that a
  request names as its acting user. Contact person: group lead.
- TBD (open): Can an admin change or delete a user, and what happens then to the coffee count, the corrections, and the
  balance of the user? No function of Coffee Kitty changes or deletes a user yet. Contact person: team assistant.
