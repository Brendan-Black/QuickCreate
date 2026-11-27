#!/bin/sh
# Launches Minecraft with QuickCreate on Windows, from WSL:  ./run.sh  (runClient) | ./run.sh runServer | ./run.sh gameTest
# Copies the sources to the prison (F:\prison\QuickCreate\src, as `winrun --source` does) and runs gradlew.bat there, which
# builds with the Windows JDK and starts the game. The Windows build, Gradle cache and run\ folder (worlds, options,
# config) are kept between runs; Gradle rebuilds only what a copied source changed. Arguments are Gradle tasks and
# options, runClient when there are none. QUICKCREATE_PRISON overrides the prison.
set -eu
root=$(cd "$(dirname "$0")" && pwd)
if [ -z "${WSL_DISTRO_NAME:-}" ] || ! command -v cmd.exe >/dev/null 2>&1; then
    echo "run.sh runs the game on Windows from WSL; elsewhere, use ./gradlew runClient" >&2
    exit 1
fi
prison="${QUICKCREATE_PRISON:-/mnt/f/prison}"
[ -d "$(dirname "$prison")" ] || { echo "The prison's drive isn't visible: $(dirname "$prison") (is F: mounted?)" >&2; exit 1; }
dest="$prison/$(basename "$root")/src"
mkdir -p "$dest"
# Permissions aren't compared because the Windows drive doesn't keep them.
rsync -a --no-perms --delete --exclude .git/ --exclude /build/ --exclude /.gradle/ --exclude /run/ --exclude /bin/ \
    --exclude /.vscode/ --exclude 'NO CLAUDE ACCESS/' "$root/" "$dest/"
if [ $# -eq 0 ]; then set -- runClient; fi
cd "$dest"
# No daemon, which would keep this shell waiting after the game closes.
exec cmd.exe /c gradlew.bat --no-daemon "$@"
