# IGW download diagnosis, 2026-10-16

Read-only inspection of the Release Testing instance found:

- `minecraft/logs/latest.log` line 1116: Extra Cells 2 advertises optional In-Game Wiki support and a `[Download Latest]` link.
- Lines 1129–1131: `IGW-Mod Download Thread` logs downloading, apparent success, then failure.
- The installed `ExtraCells-1.7.10-2.4.0.jar` contains `extracells.integration.igw.IGWSupportNotifier$CommandDownloadIGW`. Its `/igwmod_download` command constructs the download thread, which writes `IGW-Mod.jar`.
- The supplied `mods/IGW-Mod.jar` is 167 bytes of HTML containing a Cloudflare `301 Moved Permanently` response. It is not a ZIP/JAR and therefore is not a working installed mod.

This identifies Extra Cells 2's optional download integration, not an EFR dependency. The log does not establish which click or command triggered it. The actual IGW project is [In-Game Wiki](https://github.com/MaartenKok8/IGW-mod).

The existing `config/IGWMod.cfg` has `B:enable_missing_notification=true`; changing that to false suppresses this integration's missing-mod notice. No instance files or configs were changed during this diagnosis, and no download was attempted.
