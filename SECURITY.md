# Security policy

## Reporting a vulnerability

Please report security problems privately: open the repository's **Security** tab and choose
**Report a vulnerability**. Don't open a public issue for them.

Only the latest code on `main` is supported.

## What counts

Pulse handles one secret, the DataFast account token. Reports that matter most are ways the app
could:

- send the token anywhere other than `datafa.st`;
- write the token to logs, caches or files outside the APK;
- let another app on the device read it.

The token being inside the APK is expected: it is compiled in at build time, as the README's
[Security note](README.md#security-note) explains. That is why there are no prebuilt APKs and
everyone builds their own. Keep your APK private.

## If your token leaks

Revoke it in DataFast under **Account settings → API**, create a new one with only
`analytics:read` and `websites:read`, put it in `local.properties` and reinstall the app.
