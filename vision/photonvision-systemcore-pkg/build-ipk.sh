#!/usr/bin/env bash
# Assemble a SystemCore .ipk for PhotonVision, matching the exact layout observed in
# advantagescope-lite-v27.0.0-alpha-1.ipk:
#   <pkg>.ipk = ar archive containing  control.tar.gz  +  data.tar.gz   (no debian-binary)
#
# Run on a Linux/macOS host (needs: ar, tar, gzip). Produces ./photonvision.ipk
#
# Before running, populate ./payload (see README.md):
#   payload/usr/local/bin/photonvision/photonvision.jar     <- linuxarm64 backend jar
#   payload/usr/local/bin/photonvision/jre/                 <- bundled arm64 JRE
#   payload/usr/local/bin/photonvision/<native libs>        <- OpenCV / JNI .so as needed
#   payload/usr/share/photonvision.png                      <- launch-card icon
set -euo pipefail
cd "$(dirname "$0")"

OUT="photonvision.ipk"
BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT

# --- data.tar.gz : filesystem payload + the systemd unit ---------------------
DATA="$BUILD/data"
mkdir -p "$DATA/etc/systemd/system"
cp photonvision.service "$DATA/etc/systemd/system/photonvision.service"
# Merge the staged app payload (jar, jre, libs, icon).
if [ -d payload ]; then cp -a payload/. "$DATA/"; fi
# Match observed tar style: paths prefixed with ./
( cd "$DATA" && tar --owner=0 --group=0 -czf "$BUILD/data.tar.gz" ./ )

# --- control.tar.gz : metadata + maintainer scripts -------------------------
CTRL="$BUILD/control"
mkdir -p "$CTRL"
cp control "$CTRL/control"
for s in postinst prerm postrm; do
    install -m 0755 "$s" "$CTRL/$s"   # scripts MUST be executable + LF line endings
done
( cd "$CTRL" && tar --owner=0 --group=0 -czf "$BUILD/control.tar.gz" ./control ./postinst ./prerm ./postrm )

# --- assemble the ar archive (control first, then data) ---------------------
rm -f "$OUT"
ar rc "$OUT" "$BUILD/control.tar.gz" "$BUILD/data.tar.gz"

echo "Built $OUT"
ar t "$OUT"
