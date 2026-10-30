# Contributing to Pulse TV

Thanks for helping. Issues and pull requests are welcome.

## Run it without a DataFast account

Leave `DATAFAST_TOKEN` out of `local.properties` and the app runs on demo data, so you can work on
the dashboard without an account. Any Android TV or Fire TV device works, and so does an Android
emulator with a 1920×1080 screen and a D-pad.

## Before you open a pull request

- Run the unit tests with `./gradlew testDebugUnitTest`. CI runs them and builds the release APK on
  every pull request.
- Add or update tests for the behavior you change; `app/src/test` has examples for the domain, the
  DataFast adapter and the dashboard.
- If you change what a remote button does, update the remote table in the README.
- Keep each pull request to one change.

## Where things live

The README's [Architecture](README.md#architecture) section describes the packages. A new analytics
provider implements the `AnalyticsSource` interface in `traffic/domain`, and `MainActivity` picks
which source to use.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/), for example
`feat(dashboard): ...`, `fix(datafast): ...` or `docs: ...`. Explain why in the body when the
change doesn't make it obvious.

## Keep tokens out

Your token belongs in `local.properties`, which is git-ignored. Never paste a token in an issue,
a pull request or a commit.

## License

By contributing, you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
