Feature: User coffee consumption
  A user adds coffees to their own coffee count, one at a time.

  Scenario: A user adds a coffee
    Given the user "maxmustermann"
    When the user adds a coffee
    Then the request succeeds and the coffee count is 1

  Scenario: A user adds two coffees
    Given the user "maxmustermann"
    When the user adds a coffee
    And the user adds a coffee
    Then the request succeeds and the coffee count is 2
