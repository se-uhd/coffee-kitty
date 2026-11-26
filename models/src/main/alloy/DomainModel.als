module DomainModel

/*
 * The domain model of Coffee Kitty (domain/src/main/kotlin/de/seuhd/campuscoffee/domain/model/). Each sig that is
 * neither a value sig (Text, Number, Bool, Instant) nor an enum stands for a class, and each of its fields for a
 * property. A field that refers to another sig stands for a property with the same name or, for a reference by id,
 * with the suffix Id, as mergedInto for mergedIntoId. A field of the value sig Text or Number stands for the values of
 * a String or Int property at the limits of its rules, and the predicate valid<Sig><Field> states which of these values
 * the property accepts.
 */

abstract sig Role {}
one sig USER, ADMIN extends Role {}

abstract sig Bool {}
one sig True, False extends Bool {}

abstract sig Text {}
one sig ShortText, LongestText, TooLongText, BlankText, TextWithNul, TextWithControl, TextWithHyphen, TextWithoutAt
    extends Text {}

abstract sig Number {}
one sig BelowMin, Min, Middle, Max, AboveMax extends Number {}

sig Instant {}

sig User {
    loginName: one Text,
    emailAddress: one Text,
    firstName: one Text,
    lastName: one Text,
    role: one Role
}

sig CoffeeConsumption {
    user: one User,
    count: one Number,
    coffeeTimes: seq Instant
}

sig CountCorrection {
    user: one User,
    admin: one User,
    countBefore: one Number,
    countAfter: one Number,
    note: one Text
}

sig CoffeeBean {
    name: one Text,
    active: one Bool,
    mergedInto: lone CoffeeBean
}

sig CoffeeRating {
    user: one User,
    bean: one CoffeeBean,
    value: one Number
}

sig CoffeePrice {
    amountCents: one Number
}

sig Payment {
    user: lone User,
    amountCents: one Number,
    note: lone Text
}

pred notBlankNoNul[t: Text] { t in ShortText + LongestText + TextWithControl + TextWithHyphen + TextWithoutAt }

pred notBlankNoControl[t: Text] { t in ShortText + LongestText + TextWithHyphen + TextWithoutAt }

pred validUserLoginName[t: Text] { t in ShortText + LongestText + TextWithoutAt }

pred validUserEmailAddress[t: Text] { t in ShortText + LongestText + TextWithHyphen }

pred validUserFirstName[t: Text] { notBlankNoControl[t] }

pred validUserLastName[t: Text] { notBlankNoControl[t] }

pred validCoffeeConsumptionCount[n: Number] { n in Number }

pred validCountCorrectionCountBefore[n: Number] { n in Number }

pred validCountCorrectionCountAfter[n: Number] { n not in BelowMin }

pred validCountCorrectionNote[t: Text] { notBlankNoNul[t] }

pred validCoffeeBeanName[t: Text] { notBlankNoNul[t] }

pred validCoffeeRatingValue[n: Number] { n in Min + Middle + Max }

pred validCoffeePriceAmountCents[n: Number] { n in Min + Middle + Max }

pred validPaymentAmountCents[n: Number] { n in Number }

pred validPaymentNote[t: Text] { notBlankNoNul[t] }

fact OneConsumptionPerUser { all u: User | one c: CoffeeConsumption | c.user = u }

assert AsManyConsumptionsAsUsers { #CoffeeConsumption = #User }

check AsManyConsumptionsAsUsers for 5

/*
 * No text property accepts the NUL character (TextPatterns.NO_NUL_CHARACTER or a stricter pattern). The assertion
 * names the predicate of each Text field.
 */
assert NoTextPropertyAcceptsNUL {
    not validUserLoginName[TextWithNul]
    not validUserEmailAddress[TextWithNul]
    not validUserFirstName[TextWithNul]
    not validUserLastName[TextWithNul]
    not validCountCorrectionNote[TextWithNul]
    not validCoffeeBeanName[TextWithNul]
    not validPaymentNote[TextWithNul]
}

check NoTextPropertyAcceptsNUL

run CorrectionOfAUser { some CountCorrection } for 3
