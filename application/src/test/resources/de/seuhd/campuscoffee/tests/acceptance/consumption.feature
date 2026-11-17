Feature: User coffee consumption
  A user adds coffees to their own coffee count, one at a time.

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
