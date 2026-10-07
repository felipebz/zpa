# Contributing

Thank you for your interest in contributing to our project! We appreciate your time and effort.

## How to report a bug or issue

If you find a bug or issue, please report it by [opening an issue](https://github.com/felipebz/zpa/issues/new) on GitHub. Please include as much detail as possible, including:

- Steps to reproduce the issue
- Expected behavior
- Actual behavior
- Screenshots (if applicable)

## How to suggest a new feature

If you have an idea for a new feature, please [open an issue](https://github.com/felipebz/zpa/issues/new) on GitHub. Please include as much detail as possible, including:

- A description of the feature
- Why you think it would be useful
- Any potential drawbacks or limitations

## Pull Requests

Currently, we are not currently accepting pull requests for new features. We appreciate your interest in contributing, but we have limited resources and cannot maintain additional features at this time.

We will accept pull requests for minor bug fixes. Please include a detailed description of the bug and the steps to reproduce it.

## Running the integration tests

The integration tests (`./gradlew integrationTest`) analyze real-world PL/SQL projects that are included in the repository as Git submodules (pljson, alexandria-plsql-utils, utPLSQL, Doag Forms). You must have these sources checked out before running them:

- Clone the repository with `git clone --recurse-submodules https://github.com/felipebz/zpa.git`, or run `git submodule update --init --recursive` in an existing clone.
- Do not use the "Download ZIP" button or the source archives of a release (`codeload.github.com/.../refs/tags/...`): GitHub does not include submodules in them, so the source directories will be empty and every test will fail with `Expected issues on ... were not found`.

If you only want to build the custom rules example, you don't need the submodules: run `./gradlew build -p plsql-custom-rules`.

## Code of Conduct

Please note that we have a [code of conduct](CODE_OF_CONDUCT.md) in place to ensure that our community is welcoming and inclusive. Please read the code of conduct before contributing.

## License

By contributing to this project, you agree to license your contributions under the [LGPL-3.0 license](LICENSE).
