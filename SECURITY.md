# Security

Do not open a public issue containing an API key, a signing key, a keystore password, or private chat content.

This repository never contains signing keys. The release workflow builds an unsigned APK. Sign it on your own machine with `scripts/sign-local.sh` and a keystore that stays outside the repository.

If you find a security problem, contact the maintainer privately instead of filing a public issue.
