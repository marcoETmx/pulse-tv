# Pulse TV

An Android TV app for keeping an eye on your products from the couch. The first screen is a
traffic dashboard fed by [DataFast](https://datafa.st): total visitors and sales across every
website in your account, a visitors-over-time chart, and a ranking of your products with what each
one sold and whether its visitors went up or down. Products are listed by their DataFast name, or by
domain when a website has no name.

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
| ◀ ▶ on the chart | Move a cursor through the hours, days or weeks, with exact visitors and sales |
| ▶ at the end of the chart | Enter the product ranking (first row, or the last one you focused) |
| ▲ ▼ in the ranking | Move one product at a time; the header shows which rows are on screen |
| ▲ | Back to the period selector |

The dashboard refreshes every 5 minutes while it is on screen, pauses when you leave the app,
and keeps the TV awake while it is open.

### DataFast rate limit

DataFast allows 60 API requests per minute per token. A dashboard load costs two requests per
website, one for the period and one for the previous period (the website list is reused for an
hour). A period you opened less than 5 minutes ago is shown again without calling DataFast, and
after a `429` the app waits until DataFast's reset time before retrying, keeping the last data on
screen. The previous period is asked for last, so if the limit runs out there, the ranking just
shows no arrows.

## Periods

Pulse opens on **7 days**. The header shows the date range of the data on screen and when it
was last updated.

- **Today**: midnight to now, in hourly buckets (hours that have not started are not drawn).
- **7, 15, 30 days**: today plus the previous 6, 14 or 29 days, in daily buckets.
- **This year**: January 1 to today. Buckets follow the window length: hourly on January 1,
  daily up to 62 days, weekly after that.

Dates use the TV's timezone.

Each product in the ranking shows a green ▲ or a red ▼ when its visitors went up or down against
the same stretch of the previous period: at 12:30 on **7 days**, that is the 7 days before, up to
12:30. There is no arrow when the number is the same or the previous period could not be loaded.

## Sales

Sales are the revenue and payments DataFast attributes to each website, so a product shows sales
only once its payments are connected in DataFast. They arrive in the same request as the visitors,
so they cost no extra rate-limit budget. Amounts use the currency DataFast reports; when websites
report different currencies the total reads "Mixed currencies", because they cannot be added up.

When the period has sales, the chart draws them as gold bars behind the visitors line, on a money
axis on the right that shares the visitor gridlines. The two axes are scaled independently, so
compare each measure with itself over time, not the height of a bar with the line. The chart
cursor shows both values for each hour, day or week.

## Launcher artwork

Fire TV draws sideloaded apps' icons in a square centered on a gray 16:9 tile and squeezes
non-square images, so `ic_launcher.png` is a transparent square with the "Pulse" lockup. The 16:9
`banner.png` is still declared for Android TV / Google TV launchers. Both are rendered by
`tools/RenderBranding.java` (usage in its header).

The Fire TV launcher caches an app's icon per package name and does not refresh it on update,
reinstall or reboot; that is why the package is `com.marcoelizalde.pulse.tv`. Get the artwork right
before installing under a new package name.

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

The Gradle build cache is disabled in `gradle.properties` so the token is not copied into
`~/.gradle/caches`; keep it that way. The HTTP client never follows redirects, so the token is only
ever sent to `datafa.st`.

## Credits

Space Grotesk font by the Space Grotesk Project Authors, licensed under the SIL Open Font License 1.1
(`docs/licenses/SpaceGrotesk-OFL.txt`).
