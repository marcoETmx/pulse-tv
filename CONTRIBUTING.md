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

## Adding a language

Every text on screen lives in a catalog in `traffic/dashboard/i18n`, one per language.

1. Copy `EnglishStrings.kt` to a new catalog, such as `PortugueseStrings.kt`, and translate it. The
   build fails until every text has a translation, so nothing falls back to English unnoticed.
2. In the same catalog, say how the language writes numbers and dates: its `locale` (month and weekday
   names), separators, abbreviations, percent sign, where the currency goes, and the date patterns.
3. Add the language to `AppLanguage` with its language code, so the menu offers it and System default
   picks it when the TV uses that language.
4. Add a test like `SpanishStringsTest`, and look at the dashboard and the open menu on a 1920×1080
   screen: translations often run longer than English.

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
