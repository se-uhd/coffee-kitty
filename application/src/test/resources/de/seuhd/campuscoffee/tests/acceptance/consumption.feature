Feature: User coffee consumption
  A user adds coffees to their own coffee count, one at a time, and undoes the most recent coffee within its grace
  period.

  Scenario: A user adds a coffee
    Given the user "maxmustermann" has 0 coffees
    When the user adds a coffee
    Then the request succeeds and the coffee count is 1

  Scenario: A user adds two coffees
    Given the user "maxmustermann" has 0 coffees
    When the user adds a coffee
    And the user adds a coffee
    Then the request succeeds and the coffee count is 2

  Scenario Outline: Undo a coffee within the grace period
    Given the user "maxmustermann" has 3 coffees, the last one added at 10:00:00
    When the user undoes the coffee at <time>
    Then the response status is <status>
    And the coffee count is <count>

    Examples:
      | time     | status | count |
      | 10:05:00 | 200    | 2     |
      | 10:05:01 | 409    | 3     |

  Scenario: Undo at a coffee count of 0
    Given the user "maxmustermann" has 0 coffees
    When the user undoes the coffee at 10:00:00
    Then the response status is 409
    And the coffee count is 0

  Scenario: Undo twice in a row when only the most recent coffee is within its grace period
    Given the user "maxmustermann" has 3 coffees, the last one added at 10:00:00
    When the user undoes the coffee at 10:01:00
    And the user undoes the coffee at 10:02:00
    Then the response status is 409
    And the coffee count is 2

  Scenario: Undo two recent coffees in a row
    Given the user "maxmustermann" has 3 coffees, the last one added at 10:00:00
    And the user adds a coffee at 10:01:00
    When the user undoes the coffee at 10:02:00
    And the user undoes the coffee at 10:03:00
    Then the response status is 200
    And the coffee count is 2

  Scenario: Undo twice in a row when the grace period of the coffee before has passed
    Given the user "maxmustermann" has 3 coffees, the last one added at 10:00:00
    And the user adds a coffee at 10:04:00
    When the user undoes the coffee at 10:06:00
    And the user undoes the coffee at 10:06:00
    Then the response status is 409
    And the coffee count is 3
