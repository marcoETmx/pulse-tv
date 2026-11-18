# How the dashboard works

Reference for what Pulse shows and how it loads data from DataFast. For setup and the remote controls, see the [README](../README.md).

## Refresh and retries

The dashboard refreshes every 5 minutes while it is on screen (10 or 15 if you choose so; the choice
is kept across restarts), pauses when you leave the app, and keeps the TV awake while it is open.
Choosing an interval shorter than the age of the data on screen reloads it right away.

A request that fails because of the network or a DataFast server error (5xx) is tried twice more,
after 1 and 3 seconds, before the dashboard reports it. A dashboard is shown only when every website
loaded, because a total with a website missing would be wrong; until then the last one stays on
screen.

## DataFast rate limit

DataFast allows 60 API requests per minute per token. Pulse sends at most 55 in any minute, leaving
room for network delays; requests beyond that wait their turn, so flipping through several periods
makes the last one load more slowly instead of failing.

A dashboard load costs one request per website for the period and one for the previous period. The
previous period is reused for up to 30 minutes (less on short stretches like **Today**, so it stays
within 1% of a fresh comparison), and the website list for an hour. A period you opened within the
refresh interval is shown again without calling DataFast. No interval is shorter than 5 minutes, and
refreshing by hand is ignored while a load is running, so presses never pile up requests.

The budget belongs to the token: every device running Pulse with the same token shares DataFast's 60
requests per minute, and Pulse can't see what the others spend. So if DataFast still answers `429`,
Pulse sends nothing more until DataFast's reset (the next minute, never more than 2 minutes away)
and then retries on its own, keeping the last data on screen. The previous period is asked for last,
so if the limit runs out there, the ranking just shows no arrows. (DataFast's docs give the reset in
seconds, but it arrives in milliseconds; Pulse reads both.)

## Periods

Pulse opens on **7 days**. The header shows the date range of the data on screen and when it
was last updated; on demo data, a **DEMO DATA** badge takes the place of the update time.

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

## Logos

Each product shows the logo set in DataFast, else the icon its website declares, else its initial.
Logos are kept on the TV for a week, and so is knowing that a product has none. A logo that could not
be fetched because the site or the network failed is tried again with the next refresh.
