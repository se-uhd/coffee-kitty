Feature: Correction of a coffee count
  An admin corrects a wrong coffee count of a user at any time and adds a note with the reason for the correction. The
  list of the corrections of a user contains the count before and after each correction, the note, and the admin.

  Scenario: An admin corrects a coffee count with a note
    Given the user "maxmustermann" has 3 coffees
    When "jane_doe" corrects the coffee count of "maxmustermann" to 2 with the note "Double tap on 16.11."
    Then the correction returns the status 200
    And the coffee count of "maxmustermann" is 2
    And the corrections of "maxmustermann" are:
      | countBefore | countAfter | note                 | adminLoginName |
      | 3           | 2          | Double tap on 16.11. | jane_doe       |

  Scenario Outline: A refused correction changes neither the coffee count nor the corrections
    Given the user "maxmustermann" has 3 coffees
    When "<actingUser>" corrects the coffee count of "maxmustermann" to <count> <note>
    Then the correction returns the status <status>
    And the coffee count of "maxmustermann" is 3
    And "maxmustermann" has no corrections

    Examples:
      | actingUser    | count | note                                 | status |
      | maxmustermann | 2     | with the note "Double tap on 16.11." | 403    |
      | jane_doe      | 1     | without a note                       | 400    |
      | jane_doe      | 1     | with the note "   "                  | 400    |
      | jane_doe      | -1    | with the note "Double tap on 16.11." | 400    |

  Scenario: Undo after a correction that removed the most recent coffee
    Given the user "maxmustermann" has 3 coffees, the last one added at 10:00:00
    And "jane_doe" corrected the coffee count of "maxmustermann" to 2 with the note "Double tap on 16.11." at 10:02:00
    When the user undoes the coffee at 10:03:00
    Then the response status is 409
    And the coffee count is 2
