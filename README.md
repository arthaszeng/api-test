# TW Blockchain Loyalty API Test
This project is auto api regression test of point service.

## Test step
- create new customer and merchant account with chrome plugin MateMask(done in dev macau)
- bind account in point api test
- run test queue in macau
- the same step in manila

## Test account information
- membershipId
- address
- rootKey
- private key

testCUS1
0x88E463f33B905354dAc5360Fbf0f32Ac2861206E
mockRootKeyCUS1
1f2363918802c46a89a9cbbb5c730ebc5a3a368e11267fd2ca603974ec9cfd19

testCUS2
0xad89AE26a8026B14F916FFEa7D9923d4d014Eb0f
mockRootKeyCUS1
b5d61ea1f19e759413cf8bb0f7d0b2d77cdc66388fd107a1747e6034e48154ac

testMER1
0x30D757348D75E4F5Ae3ADa9Fe4702a0b7ea1944D
mockRootKeyMER1
9dc09e2426eadaba114d3904a5e7509af8df7c960c8b694152b53a6636cad051

testMER2
0x0a8C8940e50bCdC2a05d4997610e0A93d9Cb5B30
mockRootKeyMER1
e46dcea19e6a277b1126d366586e3db00bbee3278d1ba547e6827c9263f98f9d

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
