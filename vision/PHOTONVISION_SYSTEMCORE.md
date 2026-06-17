# Running PhotonVision *on* the SystemCore — Feasibility & Packaging Notes

> Investigation date: **2026-06-07** · Target: **WPILib 2027 (alpha‑6)** on **SystemCore**
> Status: **feasible but unsupported** — no official PhotonVision SystemCore package exists yet.
> This document captures the research and a concrete IPK packaging blueprint so the work can
> be picked up when real SystemCore hardware is available.

## TL;DR

Running the **full PhotonVision image‑processing backend directly on the SystemCore** (not just the
`photonlib` client on the robot) is **technically feasible and aligned with how the platform is
designed** — but **nobody has built the package yet**, so you'd be the early adopter.

- The SystemCore is a **Raspberry Pi Compute Module 5** (quad‑core Cortex‑A76, 4 GB RAM,
  VideoCore VII GPU, real‑time Linux) — the *same SoC class PhotonVision already supports on the
  Pi 5*. Hardware is not the blocker.
- SystemCore is **not a locked appliance**: its web dashboard installs add‑on apps as **IPK
  packages** ("Add Package" card), and apps run **on the device as systemd services** alongside
  robot code. Precedents: **Elastic**, **AdvantageScope Lite**, **CTR CANivore support**.
- The only real blockers left are **hardware‑validation items** (camera `/dev/video*` access under
  the service sandbox; whether the dashboard accepts unsigned community packages), **not**
  architectural ones.

## Background: how PhotonVision is normally architected

PhotonVision is two separate pieces:

1. **The backend** — a standalone server (camera capture → AprilTag / object pipeline → pose
   results). Distributed as a **prebuilt OS image** or Debian install script. Runs as its own OS
   service, *traditionally on a separate coprocessor*.
2. **`photonlib`** — a thin client vendordep linked into robot code that reads results over
   NetworkTables.

"Run actual PhotonVision image processing on the SystemCore" = put **piece #1 on the SystemCore**
instead of on a separate Pi/Orange Pi/Limelight, with `photonlib` pointed at `localhost`.

## SystemCore hardware (relevant facts)

| Spec | Value | Source |
|---|---|---|
| SoC | Raspberry Pi **Compute Module 5** | WPILib SystemCore intro |
| CPU | quad‑core ARM **Cortex‑A76** | WPILib SystemCore intro |
| RAM | **4 GB** | FIRST community announcement |
| GPU | VideoCore VII | FIRST community announcement |
| OS | real‑time Linux (systemd; ships `python3`) | inferred from dissected IPK below |
| I/O | USB, Ethernet, CAN, I2C, reconfigurable PWM/DIO/AIO (RP2350) | spec sheet |
| Camera | **USB confirmed**; CSI/MIPI breakout unconfirmed | spec sheet / WPILib docs |

PhotonVision's published minimums are *Cortex‑A53, 2 GB RAM, Ubuntu 24.04/Debian* — the SystemCore
**exceeds** them and shares the Pi 5's BCM2712 / Cortex‑A76, which PhotonVision already targets.

## Feasibility: blocker‑by‑blocker

| Concern | Status | Notes |
|---|---|---|
| Can you run your own software on it? | ✅ Resolved | IPK "Add Package" system; apps run as systemd services. AdvantageScope Lite runs on‑device. |
| Hardware capable? | ✅ Yes | CM5 = supported PV SoC class; 4 GB RAM. |
| Camera input | ✅ USB / ⚠️ CSI | USB confirmed; CSI unconfirmed (not required). |
| Official PV build for SystemCore | ❌ Missing | Only the `photonlib` *client* has 2027 dev builds. No backend IPK yet. |
| Packaging format | ⚠️ Constraint | SystemCore uses **IPK**, not Debian `apt`. PV's apt install script won't work; must repackage. |
| CPU contention w/ control loop | ⚠️ Validate | Vision is CPU‑bound; cap it (Nice/CPUQuota) so it can't starve the robot loop. |
| Camera access under service sandbox | ❓ Unknown | Service runs as `systemcore` with `ProtectSystem=strict`; needs `/dev/video*` grant. **#1 thing to test.** |
| `Add Package` gating/signing | ❓ Unknown | Dashboard package source/signing model undocumented (WPILib page is a placeholder). |
| Competition legality | ❓ Unknown | 2027 game manual not published. |

## Anatomy of a real SystemCore IPK

Dissected `advantagescope-lite-v27.0.0-alpha-1.ipk` (from the AdvantageScope GitHub release). An
`.ipk` is an **`ar` archive** with two gzipped tarballs (note: **no `debian-binary` member**):

```
<pkg>.ipk  (ar archive)
├── control.tar.gz   → control + postinst / prerm / postrm
└── data.tar.gz      → files laid onto the filesystem (./etc, ./usr, ...)
```

### The `control` file — drives the dashboard launch card

The custom `X-*` fields are how the web dashboard renders the app's launch card:

```
Package: advantagescope-lite
Version: 27.0.0-alpha-1
Architecture: all
Maintainer: Littleton Robotics
X-Display-Name: AdvantageScope Lite      # card title
X-Port: 5808                             # port the card links to
X-Has-UI: true                           # render a clickable web card
X-Auto-Start: true
X-Icon-Path: /usr/share/advantagescope-lite.png
```

### Maintainer scripts — just wire up systemd

```sh
# postinst:  systemctl daemon-reload; systemctl enable --now <pkg>.service
# prerm:     systemctl stop <pkg>.service; systemctl disable <pkg>.service
# postrm:    systemctl daemon-reload; systemctl reset-failed <pkg>.service
```

### Payload + systemd unit

Files land at `/etc/systemd/system/<pkg>.service`, `/usr/local/bin/<pkg>/...`,
`/usr/share/<icon>.png`. The observed unit:

```ini
[Service]
Type=simple
User=systemcore
Group=systemcore
WorkingDirectory=/usr/local/bin/advantagescope-lite
ExecStart=/usr/bin/env python3 lite_server.py   # OS provides python3
Restart=always
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ReadWritePaths=/var/log
[Install]
WantedBy=multi-user.target
```

**What this proves about the SystemCore image:** it runs **systemd**, has a `systemcore` service
user, ships **python3**, and the dashboard discovers apps purely from the `X-*` control fields.
That is the entire contract a PhotonVision package must satisfy.

## Blueprint: a PhotonVision SystemCore IPK

AdvantageScope Lite is pure Python (`Architecture: all`). PhotonVision is a Java app with native
code, so it's heavier. Deltas:

| Aspect | AS Lite (observed) | PhotonVision (required) |
|---|---|---|
| Architecture | `all` | `arm64`/`aarch64` (has native libs) — **confirm the value the dashboard expects** |
| Runtime | system `python3` | **bundle a JRE** (don't assume Java on SystemCore) |
| Native deps | none | bundle **OpenCV + PV JNI `.so`** libs |
| Payload | `lite_server.py` | `photonvision-*-linuxarm64.jar` + JRE + libs |
| ExecStart | `python3 lite_server.py` | `…/jre/bin/java -jar photonvision.jar` |
| Port (`X-Port`) | 5808 | **5800** (PV web UI) |
| Camera | n/a | needs `/dev/video*` → `SupplementaryGroups=video`, `DeviceAllow`, relaxed sandbox |
| Writable config | `/var/log` | `ReadWritePaths=/opt/photonvision` (PV writes config/calibration/logs) |
| NetworkTables | system NT | PV NT client → `localhost`; avoid port clash |
| CPU budget | light | `Nice`/`CPUQuota` so vision can't starve the robot loop |

Ready‑to‑fill templates live in [`photonvision-systemcore-pkg/`](photonvision-systemcore-pkg/),
with a `build-ipk.sh` that assembles the `ar` archive in the exact layout observed above.

## Bring‑up checklist (needs real hardware)

1. Get shell on the SystemCore; confirm `cat /etc/os-release`, `uname -m`, that `systemd` + `python3`
   are present, and that you can install a unit.
2. Plug a USB camera; confirm it enumerates: `v4l2-ctl --list-devices` / `ls /dev/video*`.
3. **Critical test:** can the `systemcore` service user read `/dev/video*` with the sandboxed unit?
   (Add `SupplementaryGroups=video`, `DeviceAllow=char-video4linux rw`; relax `ProtectSystem` as needed.)
4. Build the PV backend for `linuxarm64` (PV release jar or `./gradlew … installArm64Toolchain`),
   bundle a JRE + OpenCV, assemble the IPK via `build-ipk.sh`.
5. Install via the dashboard "Add Package" card — verify whether unsigned community IPKs are accepted.
6. Confirm PV UI on `http://<systemcore>:5800`; point `photonlib` (2027 dev build) at `localhost`.
7. Stress‑test: run a vision pipeline + robot code together; watch for **control‑loop overruns**.
   Tune `Nice`/`CPUQuota` if needed.

## Robot-side client (`photonlib`) status — on hold

Regardless of *where* the PV backend runs, the robot program reads results via the **`photonlib`**
vendordep. As of this investigation, **no `photonlib` build is compatible with WPILib
`2027.0.0-alpha-6`**:

- WPILib 2027 moved the namespace from `edu.wpi.first.*` to **`org.wpilib.*`** (this project already
  uses it, e.g. `org.wpilib.command2` in `RobotContainer.java`). Released photonlib (2025/2026) is
  compiled against the old `edu.wpi.first.*` artifacts and won't link.
- A vendordep pins a specific WPILib year; released ones resolve 2025/2026, not the alpha‑6 artifacts.
- Only a build from PhotonVision's **`2027` branch** (CI artifacts, behind a GitHub login) could
  work, and it must match the alpha closely (cf. WPILib issue *"Check for compatibility with
  SystemCore image version"*).
- You **cannot** sidestep photonlib by reading NetworkTables directly — PV publishes serialized
  packets that require photonlib to deserialize.

**Decision (2026-06-07):** hold off on writing `VisionSubsystem` / adding the vendordep until an
official alpha‑6‑compatible `photonlib` is released. The robot-side code is identical whether the
backend ends up on-device or on a separate coprocessor, so nothing is lost by waiting.

## Alternative (supported today)

If on‑device proves blocked by hardware/policy, the supported path is unchanged: **PV backend on a
separate coprocessor** (Pi 5 / Orange Pi 5 / Limelight) + **`photonlib` on the SystemCore**. The
robot‑side code is *identical* — only the camera host/IP differs — so wiring up a `VisionSubsystem`
now is not wasted either way.

## Sources

- [WPILib — SystemCore Introduction](https://docs.wpilib.org/en/2027/docs/software/systemcore-info/systemcore-introduction.html)
- [WPILib — SystemCore docs index](https://docs.wpilib.org/en/latest/docs/software/systemcore-info/index.html)
- [PhotonVision — Selecting Hardware](https://docs.photonvision.org/en/latest/docs/hardware/selecting-hardware.html)
- [PhotonVision — Debian coprocessor install](https://docs.photonvision.org/en/latest/docs/advanced-installation/sw_install/other-coprocessors.html)
- [AdvantageScope Lite](https://docs.advantagescope.org/more-features/advantagescope-lite/) (dissected IPK: `advantagescope-lite-v27.0.0-alpha-1.ipk`)
- [wpilibsuite/SystemcoreTesting](https://github.com/wpilibsuite/SystemcoreTesting)
- [YetiRobotics — Current Solution for PhotonVision 2027](https://wiki.yetirobotics.org/books/robot-software/page/current-solution-for-photonvision-2027)
- [Chief Delphi — SystemCore and PhotonVision](https://www.chiefdelphi.com/t/systemcore-and-photonvision/515525)
- [Chief Delphi — Systemcore, PLEASE support Linux!](https://www.chiefdelphi.com/t/systemcore-please-support-linux/504602)
- [REV Hardware Client 2 (desktop app, not on‑device)](https://docs.revrobotics.com/rev-hardware-client-2)
