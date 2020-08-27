# TW Blockchain Loyalty API Test
This project is auto api regression test of point service.

## Test data prepare
- create new customer and merchant account with chrome plugin MateMask(done in dev macau)
- bind account in point api test
- run test queue in macau
- the same step in manila


## Conduct of Git
This project uses git for source code version management and `git flow` as the team's workflow.
Please read the following article for more information.
- [git flow](https://datasift.github.io/gitflow/IntroducingGitFlow.html)

### Commitment Guideline
We use the same format to write git commit messages.
* format: `type(<Trello Card number>): message`
* example: `feat(42): add the request validation for points API`
* `type` should be one of the following items:
    ```
    feat: new feature
    fix: fix the bug
    docs: change of documententaion
    style: change of code style
    refactor: refactor the code with no behavior changed
    chore: changes about utilization and build tools, no business logic
    revert: revert the previous commit
    ```
