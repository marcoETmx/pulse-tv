# Pulse TV

An Android TV app for keeping an eye on your products from the couch. The first screen is a
traffic dashboard fed by [DataFast](https://datafa.st): total visitors across every website in
your account, a visitors-over-time chart, and a ranking of your products.

Built for the Fire TV Stick (Fire OS 7, Android 9 / API 28) with Kotlin and Compose for TV.

## Setup

### 1. Create a read-only DataFast token

Pulse reads every website in your account, so it needs a DataFast **account token** (`dft_`),
not a single-website key (`df_`). Give it only these permissions:

- `analytics:read`
- `websites:read`

Create it in DataFast under **Account settings → API**, or with the DataFast CLI:

```sh
datafast tokens create --name "Pulse TV" --permissions "analytics:read,websites:read"
```

### 2. Add it to `local.properties`

`local.properties` is git-ignored. Add one line:

```properties
DATAFAST_TOKEN=dft_your_token_here
```

Without a token the app runs on clearly labelled **demo data**, which is handy for trying the design.

### 3. Build and install on the Fire TV

Enable **ADB debugging** on the Fire TV (Settings → My Fire TV → Developer options), then:

```sh
export JAVA_HOME="$HOME/.local/share/android-studio/jbr"   # Android Studio's bundled JDK
adb connect <fire-tv-ip>:5555
./gradlew installRelease
```

The release build is minified with R8 (about 1.6 MB, cold start around 2.6 s on a Fire TV Stick Lite)
and signed with the local debug key, because it is meant for sideloading only. Open **Pulse** from
*Your Apps & Channels*.

## Using it with the remote

| Remote | Action |
|---|---|
| ◀ ▶ on the top row | Move between Today, 7 days, 15 days, 30 days and This year |
| OK | Load the focused period |
| ▼ | Focus the chart |
| ◀ ▶ on the chart | Move a cursor through the hours, days or weeks, with exact values |
| ▲ | Back to the period selector |

The dashboard refreshes every 5 minutes while it is on screen, pauses when you leave the app,
and keeps the TV awake while it is open.

## Periods

- **Today**: midnight to now, in hourly buckets.
- **7, 15, 30 days**: today plus the previous 6, 14 or 29 days, in daily buckets.
- **This year**: January 1 to today, in weekly buckets (daily during the first two months).

Dates use the TV's timezone.

## Architecture

The code is organized by capability (`traffic`), with ports and adapters inside it:

```
traffic/
  domain/      Periods, report windows, LoadTrafficDashboard, AnalyticsSource port
  datafast/    DataFast REST adapter (account token, websiteId per request) + HTTP transport
  demo/        Deterministic demo source used when no token is set
  dashboard/   View model, display formats, and Compose for TV screen and components
ui/theme/      Colors, Space Grotesk typography, theme
```

`MainActivity` is the composition root: it picks the DataFast or demo source and builds the view model.

## Tests

```sh
./gradlew testDebugUnitTest
```

## Security note

The token is compiled into the APK, so anyone with the APK file can extract it. Keep the APK private,
and use a token limited to `analytics:read,websites:read`. Revoke it in DataFast if the APK leaks.

## Credits

Space Grotesk font by the Space Grotesk Project Authors, licensed under the SIL Open Font License 1.1
(`docs/licenses/SpaceGrotesk-OFL.txt`).
