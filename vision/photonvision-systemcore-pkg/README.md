# PhotonVision → SystemCore IPK skeleton

Ready-to-fill templates for packaging the **PhotonVision backend** as a SystemCore add-on,
installable through the web dashboard's **"Add Package"** card. See the parent doc
[`../PHOTONVISION_SYSTEMCORE.md`](../PHOTONVISION_SYSTEMCORE.md) for the full feasibility writeup
and where these conventions come from (dissecting `advantagescope-lite-v27.0.0-alpha-1.ipk`).

> ⚠️ **Untested.** This has not been run on real SystemCore hardware. It is a structural starting
> point derived from a known-good package, not a verified PhotonVision build.

## Files

| File | Purpose |
|---|---|
| `control` | Package metadata + `X-*` fields that render the dashboard launch card. |
| `photonvision.service` | systemd unit. Relaxed sandbox for `/dev/video*` + writable config. |
| `postinst` / `prerm` / `postrm` | enable/start, stop/disable, cleanup the service. |
| `build-ipk.sh` | Assembles the `ar` archive (`control.tar.gz` + `data.tar.gz`). |
| `payload/` | **You create this** — the actual files installed onto the device. |

## What you must provide (the `payload/` tree)

The templates assume this layout; create it before building:

```
payload/
├── usr/local/bin/photonvision/
│   ├── photonvision.jar        # PhotonVision linuxarm64 backend jar
│   ├── jre/                    # bundled arm64 JRE (do not assume Java on SystemCore)
│   └── <native libs>           # OpenCV / PV JNI .so, if not inside the jar
└── usr/share/photonvision.png  # launch-card icon
```

Getting the backend jar: download a PhotonVision `*-linuxarm64.jar` release, or build from the
`2027` branch (`./gradlew installArm64Toolchain` then the shadow/backend jar task).

## Build

Run on **Linux/macOS** (needs `ar`, `tar`, `gzip` — Windows lacks `ar`):

```sh
chmod +x build-ipk.sh postinst prerm postrm
./build-ipk.sh        # -> photonvision.ipk
```

Then upload `photonvision.ipk` via the SystemCore dashboard → **Add Package**.

## Gotchas / unknowns to verify on hardware

- **Line endings:** `postinst`/`prerm`/`postrm` and `.service` **must be LF**, not CRLF (they were
  edited on Windows — run `dos2unix` or `sed -i 's/\r$//'` if the service fails to start).
- **`Architecture` value:** set to `arm64` here; the observed sample used `all` (pure Python). If the
  dashboard rejects it, try `aarch64`.
- **Camera access** under the `systemcore` service user + `ProtectSystem=strict` is the **#1 unproven
  item** — AdvantageScope Lite never touched `/dev/video*`. Adjust `SupplementaryGroups`/`DeviceAllow`
  and check `journalctl -u photonvision.service`.
- **Package signing/gating:** whether the dashboard accepts unsigned community IPKs is undocumented.
- **CPU budget:** tune `Nice` / uncomment `CPUQuota` in the unit if the robot loop sees overruns.
- **NetworkTables:** point `photonlib` at `localhost`; make sure PV's NT/web ports don't clash with
  the robot program or the system NT server.
