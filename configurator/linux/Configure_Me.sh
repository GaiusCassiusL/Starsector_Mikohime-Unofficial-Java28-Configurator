#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "$PWD/configurator/shared/java-versions.properties" ]]; then
    GAME_ROOT="$PWD"
else
    GAME_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
fi
SHARED_DIR="$GAME_ROOT/configurator/shared"
CORE_DIR="$GAME_ROOT/starsector-core"
MIKOHIME_DIR="$GAME_ROOT/mikohime"
LOCK_DIR="$GAME_ROOT/.configure_me.lock"
LOCK_OWNED=0

die() {
    printf 'Error: %s\n' "$*" >&2
    exit 1
}

read_property() {
    local key="$1" file="$2"
    awk -F= -v key="$key" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "$file"
}

cleanup() {
    rm -f -- "${TEMP_ARCHIVE:-}" "${TEMP_ARGS:-}" "${TEMP_INFO:-}" "${TEMP_LAUNCHER:-}"
    if [[ -n "${TEMP_STAGE:-}" && -d "$TEMP_STAGE" ]]; then
        rm -rf -- "$TEMP_STAGE"
    fi
    if [[ "$LOCK_OWNED" == 1 ]]; then
        rmdir -- "$LOCK_DIR" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

validate_installation() {
    [[ -x "$GAME_ROOT/starsector.sh" || -x "$GAME_ROOT/starsector" ]] ||
        die "Run this script from a Linux Starsector installation."
    [[ -d "$CORE_DIR" && -d "$MIKOHIME_DIR" ]] ||
        die "Required directories starsector-core and mikohime were not found."
    [[ -f "$SHARED_DIR/classpath.entries" ]] ||
        die "Shared configurator data was not found under configurator/shared."
    [[ -f "$MIKOHIME_DIR/linux/liblwjgl64.so" ]] ||
        die "The Linux Mikohime native libraries are missing."
}

install_java() {
    local requested="$1" props="$SHARED_DIR/java-versions.properties"
    local prefix version folder url expected archive stage extracted actual
    case "$requested" in
        27) prefix=Jdk27 ;;
        28) prefix=Jdk28 ;;
        *) die "--install-java accepts 27 or 28" ;;
    esac
    version="$(read_property "${prefix}Version" "$props")"
    folder="$(read_property "${prefix}Folder" "$props")"
    url="$(read_property "${prefix}LinuxUrl" "$props")"
    expected="$(read_property "${prefix}LinuxSha256" "$props")"
    [[ -n "$version" && -n "$folder" && -n "$url" && -n "$expected" ]] ||
        die "The Java $requested download metadata is incomplete."
    [[ ! -e "$GAME_ROOT/$folder" ]] || die "$folder already exists."
    command -v curl >/dev/null || die "curl is required to install Java."
    command -v sha256sum >/dev/null || die "sha256sum is required to install Java."
    archive="$(mktemp "$GAME_ROOT/.jdk-${requested}.XXXXXX.tar.gz")"
    TEMP_ARCHIVE="$archive"
    printf 'Downloading Java %s (%s)...\n' "$requested" "$version"
    curl --fail --location --proto '=https' --tlsv1.2 --output "$archive" "$url"
    actual="$(sha256sum "$archive" | awk '{print $1}')"
    [[ "$actual" == "$expected" ]] || die "Java archive checksum mismatch."
    tar -tzf "$archive" >/dev/null || die "Java archive validation failed."
    stage="$(mktemp -d "$GAME_ROOT/.jdk-${requested}.stage.XXXXXX")"
    TEMP_STAGE="$stage"
    tar -xzf "$archive" -C "$stage"
    extracted="$(find "$stage" -mindepth 1 -maxdepth 1 -type d -print -quit)"
    [[ -n "$extracted" && -x "$extracted/bin/java" ]] || die "Downloaded archive does not contain a JDK."
    "$extracted/bin/java" -version 2>&1 | grep -F "\"$requested" >/dev/null ||
        die "Downloaded archive contains the wrong Java version."
    mv -- "$extracted" "$GAME_ROOT/$folder"
    rmdir -- "$stage"
    TEMP_STAGE=
    rm -f -- "$archive"
    TEMP_ARCHIVE=
    printf 'Installed Java %s at %s\n' "$requested" "$GAME_ROOT/$folder"
}

java_major() {
    "$1" -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1
}

collect_java_candidates() {
    JAVA_PATHS=()
    JAVA_LABELS=()
    local candidate major
    for candidate in \
        "$GAME_ROOT/jdk-28+13/bin/java" \
        "$GAME_ROOT/jdk-27+22/bin/java" \
        "$GAME_ROOT/jre_linux/bin/java" \
        "$GAME_ROOT/jre/bin/java"
    do
        [[ -x "$candidate" ]] || continue
        major="$(java_major "$candidate")"
        [[ "$major" == 17 || "$major" == 27 || "$major" == 28 ]] || continue
        JAVA_PATHS+=("$candidate")
        JAVA_LABELS+=("Java $major (${candidate#"$GAME_ROOT/"})")
    done
    if command -v java >/dev/null; then
        candidate="$(command -v java)"
        major="$(java_major "$candidate")"
        if [[ "$major" == 17 || "$major" == 27 || "$major" == 28 ]]; then
            JAVA_PATHS+=("$candidate")
            JAVA_LABELS+=("Java $major (system)")
        fi
    fi
    ((${#JAVA_PATHS[@]} > 0)) || die "No supported Java 17, 27, or 28 installation was found."
}

select_java() {
    collect_java_candidates
    if [[ -n "${MIKO_JAVA:-}" ]]; then
        [[ -x "$MIKO_JAVA" ]] || die "MIKO_JAVA is not executable: $MIKO_JAVA"
        SELECTED_JAVA="$MIKO_JAVA"
        return
    fi
    if [[ "${1:-}" == "--non-interactive" ]]; then
        SELECTED_JAVA="${JAVA_PATHS[0]}"
        return
    fi
    printf '\nAvailable Java installations:\n'
    local index choice
    for index in "${!JAVA_PATHS[@]}"; do
        printf '  %d. %s\n' "$((index + 1))" "${JAVA_LABELS[$index]}"
    done
    read -r -p "Select Java [1]: " choice
    choice="${choice:-1}"
    [[ "$choice" =~ ^[0-9]+$ ]] && ((choice >= 1 && choice <= ${#JAVA_PATHS[@]})) ||
        die "Invalid Java selection."
    SELECTED_JAVA="${JAVA_PATHS[$((choice - 1))]}"
}

select_memory() {
    local detected_kib recommended choice value description
    detected_kib="$(awk '/MemTotal:/ {print $2; exit}' /proc/meminfo 2>/dev/null || true)"
    if [[ -n "$detected_kib" && "$detected_kib" -ge 30000000 ]]; then
        recommended=11264
    elif [[ -n "$detected_kib" && "$detected_kib" -ge 15000000 ]]; then
        recommended=8192
    else
        recommended=4096
    fi
    if [[ -n "${MIKO_HEAP_MIB:-}" ]]; then
        HEAP_MIB="$MIKO_HEAP_MIB"
    elif [[ "${1:-}" == "--non-interactive" ]]; then
        HEAP_MIB="$recommended"
    else
        printf '\nMemory presets:\n'
        while IFS='|' read -r choice value description; do
            [[ "$choice" =~ ^[0-9]+$ ]] || continue
            printf '  %s. %s\n' "$choice" "$description"
        done < "$SHARED_DIR/memory-presets.tsv"
        read -r -p "Select memory preset or enter MiB [$recommended]: " choice
        choice="${choice:-$recommended}"
        value="$(awk -F'|' -v choice="$choice" '$1 == choice {print $2; exit}' "$SHARED_DIR/memory-presets.tsv")"
        HEAP_MIB="${value:-$choice}"
    fi
    [[ "$HEAP_MIB" =~ ^[0-9]+$ ]] && ((HEAP_MIB >= 1024)) ||
        die "Memory must be an integer of at least 1024 MiB."
}

build_classpath() {
    local entry
    CLASSPATH_VALUE=
    while IFS= read -r entry; do
        [[ -n "$entry" && "$entry" != \#* ]] || continue
        CLASSPATH_VALUE+="${CLASSPATH_VALUE:+:}$entry"
    done < "$SHARED_DIR/classpath.entries"
    if [[ -f "$CORE_DIR/fr.jar" && "${MIKO_FAST_RENDERING:-1}" != 0 ]]; then
        CLASSPATH_VALUE="fr.jar:$CLASSPATH_VALUE"
        FAST_RENDERING=Enabled
    else
        FAST_RENDERING="Not installed"
    fi
}

select_components() {
    RESOURCE_CACHE=Disabled
    PREPATCHER_AGENT=
    if [[ "$FAST_RENDERING" == Enabled && -f "$CORE_DIR/fr-resource-cache-agent.jar" ]]; then
        if [[ "${1:-}" == "--non-interactive" ]]; then
            [[ "${MIKO_RESOURCE_CACHE:-0}" == 1 ]] && RESOURCE_CACHE=Enabled
        else
            local answer
            read -r -p "Enable FR Resource Cache? [y/N]: " answer
            [[ "$answer" =~ ^[Yy]$ ]] && RESOURCE_CACHE=Enabled
        fi
    fi

    local candidate
    candidate="$(find "$GAME_ROOT/mods" -maxdepth 2 -type f -name StarsectorPrepatcherAgent.jar -print -quit 2>/dev/null || true)"
    if [[ -n "$candidate" ]]; then
        if [[ "${1:-}" == "--non-interactive" ]]; then
            [[ "${MIKO_PREPATCHER:-0}" == 1 ]] && PREPATCHER_AGENT="$candidate"
        else
            local answer
            read -r -p "Enable StarsectorPrepatcher? [y/N]: " answer
            [[ "$answer" =~ ^[Yy]$ ]] && PREPATCHER_AGENT="$candidate"
        fi
    fi
}

write_configuration() {
    local major java_command
    major="$(java_major "$SELECTED_JAVA")"
    TEMP_ARGS="$(mktemp "$GAME_ROOT/.Miko_Simple.XXXXXX")"
    TEMP_INFO="$(mktemp "$GAME_ROOT/.Miko_Info.XXXXXX")"
    TEMP_LAUNCHER="$(mktemp "$GAME_ROOT/.Miko_Rouge.XXXXXX")"
    {
        printf '%s\n' \
            '-XX:+UnlockDiagnosticVMOptions' \
            '-XX:+UnlockExperimentalVMOptions' \
            '-XX:+ShowCodeDetailsInExceptionMessages' \
            '-XX:+TieredCompilation' \
            '-XX:+UseG1GC' \
            '-XX:+UseStringDeduplication' \
            '-XX:+AlwaysPreTouch' \
            '-XX:ReservedCodeCacheSize=256m' \
            '-Xss4m' \
            "-Xms${HEAP_MIB}m" \
            "-Xmx${HEAP_MIB}m" \
            '-Djava.library.path=../mikohime/linux' \
            '-Dlog4j1.compatibility=true' \
            '-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector' \
            '-Dlog4j2.enableThreadlocals=true' \
            '-Dlog4j2.enableDirectEncoders=true' \
            '-Dlog4j2.garbagefreeThreadContextMap=true' \
            '-Djava.util.Arrays.useLegacyMergeSort=true' \
            '-Dlog4j.configuration=../mikohime/mikohime.properties' \
            '-Djava.xml.config.file=../mikohime/miko_jxp.properties' \
            '-Dcom.fs.starfarer.launcher_bg=../mikohime/launcher_bg.jpg' \
            '--add-opens=java.base/sun.nio.ch=ALL-UNNAMED' \
            '--add-opens=java.base/java.nio=ALL-UNNAMED' \
            '--add-opens=java.base/java.util=ALL-UNNAMED' \
            '--add-opens=java.base/java.util.concurrent=ALL-UNNAMED' \
            '--add-opens=java.base/java.lang=ALL-UNNAMED' \
            '--add-opens=java.desktop/java.awt=ALL-UNNAMED' \
            '--enable-native-access=ALL-UNNAMED'
        [[ "$major" == 28 ]] && printf '%s\n' '--enable-final-field-mutation=ALL-UNNAMED'
        [[ "$RESOURCE_CACHE" == Enabled ]] &&
            printf '%s\n' '-javaagent:fr-resource-cache-agent.jar=gameRoot=.,installRoot=..,cacheDir=../fr-resource-cache,flushDelaySeconds=30,memoryCacheMiB=128,maxFileSize=1048576'
        [[ "$FAST_RENDERING" == Enabled ]] && printf '%s\n' '-javaagent:fr.agent.jar'
        if [[ -n "$PREPATCHER_AGENT" ]]; then
            printf '%s\n' "-javaagent:\"../${PREPATCHER_AGENT#"$GAME_ROOT/"}\""
        fi
        printf '%s\n' \
            "-classpath $CLASSPATH_VALUE" \
            '-Dcom.fs.starfarer.settings.paths.saves=../saves' \
            '-Dcom.fs.starfarer.settings.paths.screenshots=../screenshots' \
            '-Dcom.fs.starfarer.settings.paths.mods=../mods' \
            '-Dcom.fs.starfarer.settings.paths.logs=.' \
            'com.fs.starfarer.StarfarerLauncher'
    } > "$TEMP_ARGS"
    {
        printf 'Mikohime Linux configuration\n'
        printf 'Java: %s (%s)\n' "$major" "$SELECTED_JAVA"
        printf 'Heap: %s MiB\n' "$HEAP_MIB"
        printf 'Fast Rendering: %s\n' "$FAST_RENDERING"
        printf 'FR Resource Cache: %s\n' "$RESOURCE_CACHE"
        if [[ -n "$PREPATCHER_AGENT" ]]; then
            printf 'StarsectorPrepatcher: Enabled (%s)\n' "${PREPATCHER_AGENT#"$GAME_ROOT/"}"
        elif find "$GAME_ROOT/mods" -maxdepth 2 -type f -name StarsectorPrepatcherAgent.jar -print -quit 2>/dev/null | grep -q .; then
            printf 'StarsectorPrepatcher: Disabled\n'
        else
            printf 'StarsectorPrepatcher: not installed\n'
        fi
    } > "$TEMP_INFO"
    if [[ "$SELECTED_JAVA" == "$GAME_ROOT/"* ]]; then
        java_command="../${SELECTED_JAVA#"$GAME_ROOT/"}"
    else
        java_command="$SELECTED_JAVA"
    fi
    {
        printf '%s\n' '#!/usr/bin/env bash' 'set -euo pipefail'
        printf 'cd -- "$(dirname -- "${BASH_SOURCE[0]}")/starsector-core"\n'
        printf 'exec %q @../Miko_Simple.txt "$@"\n' "$java_command"
    } > "$TEMP_LAUNCHER"
    grep -Fx 'com.fs.starfarer.StarfarerLauncher' "$TEMP_ARGS" >/dev/null
    grep -F -- '-classpath ' "$TEMP_ARGS" >/dev/null
    chmod 755 "$TEMP_LAUNCHER"
    mv -f -- "$TEMP_ARGS" "$GAME_ROOT/Miko_Simple.txt"
    mv -f -- "$TEMP_INFO" "$GAME_ROOT/Miko_Info.txt"
    mv -f -- "$TEMP_LAUNCHER" "$GAME_ROOT/Miko_Rouge.sh"
    TEMP_ARGS= TEMP_INFO= TEMP_LAUNCHER=
}

main() {
    local mode=
    case "${1:-}" in
        --install-java)
            [[ $# -eq 2 ]] || die "Usage: $0 --install-java 27|28"
            ;;
        --non-interactive) mode=--non-interactive ;;
        '') ;;
        *) die "Usage: $0 [--non-interactive | --install-java 27|28]" ;;
    esac
    validate_installation
    mkdir -- "$LOCK_DIR" 2>/dev/null || die "Another configurator instance is running."
    LOCK_OWNED=1
    if [[ "${1:-}" == "--install-java" ]]; then
        install_java "$2"
        exit
    fi
    select_java "$mode"
    select_memory "$mode"
    build_classpath
    select_components "$mode"
    write_configuration
    printf '\nCreated Miko_Simple.txt, Miko_Info.txt, and Miko_Rouge.sh.\n'
}

main "$@"
