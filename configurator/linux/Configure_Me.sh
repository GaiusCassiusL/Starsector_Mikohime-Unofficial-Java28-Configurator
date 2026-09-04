#!/usr/bin/env bash
# =============================================================================
# Mikohime Java Configurator - Linux (feature parity with Configure_Me.cmd)
# =============================================================================
set -uo pipefail
# -----------------------------------------------------------------------------
# Path discovery and shared declarative data
# -----------------------------------------------------------------------------
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "$SCRIPT_DIR/mikohime/configurator/shared/java-versions.properties" ]]; then
    GAME_ROOT="$SCRIPT_DIR"
elif [[ -f "$PWD/mikohime/configurator/shared/java-versions.properties" ]]; then
    GAME_ROOT="$PWD"
elif [[ -f "$SCRIPT_DIR/../shared/java-versions.properties" ]]; then
    GAME_ROOT="$PWD"
else
    GAME_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
fi

SHARED_DIR="$GAME_ROOT/mikohime/configurator/shared"
if [[ ! -f "$SHARED_DIR/components.properties" && -f "$SCRIPT_DIR/../shared/components.properties" ]]; then
    SHARED_DIR="$(cd -- "$SCRIPT_DIR/../shared" && pwd)"
fi
MIKOHIME_DIR="$GAME_ROOT/mikohime"
MODS_DIR="$GAME_ROOT/mods"
BG_DIR="$MIKOHIME_DIR/bg"
DEFAULT_VM="$MIKOHIME_DIR/DefaultVM"
LOCK_DIR="$GAME_ROOT/.configure_me.lock"

# Native Linux Starsector installations use one flat game directory. All game
# JARs, Fast Rendering files, and generated launch files live beside
# starsector.sh.

# Unique transaction identifier for pending / backup files.
TRANSACTION_ID="$$_$(date +%s 2>/dev/null || echo 0)_${RANDOM}${RANDOM}"

# Cleanup / lock bookkeeping.
LOCK_OWNED=0
declare -a OWNED_TEMP_FILES=()
declare -a OWNED_TEMP_DIRS=()

# Selection state (reset before every configuration run).
SELECTED_JAVA=""
JAVA_VERSION=""
HEAP_MIB=""
HEAP_DESCRIPTION=""
FAST_RENDERING_STATUS="Disabled"
RESOURCE_CACHE_STATUS="Disabled"
LOW_CORE_MODE="No"
OLD_CPU_MODE="No"
LARGE_PAGES_ENABLED="No"
LOGGING_MODE="Full"
BACKGROUND_SOURCE=""
BACKGROUND_LABEL=""

die() {
    printf '%s%s%s\n' "${ColorRed:-}" "Error: $*" "${ColorReset:-}" >&2
    exit 1
}

warn() {
    printf '%s%s%s\n' "${ColorYellow:-}" "$*" "${ColorReset:-}" >&2
}

read_property() {
    # read_property KEY FILE -> value (first match) on stdout
    local key="$1" file="$2"
    [[ -f "$file" ]] || return 0
    awk -F= -v key="$key" '
        /^[[:space:]]*#/ { next }
        { line=$0; sub(/\r$/, "", line); sub(/^[[:space:]]+/, "", line) }
        index(line, key "=") == 1 { sub(/^[^=]*=/, "", line); print line; exit }
    ' "$file"
}

# Load the shared component and Java metadata into shell variables.
load_shared_metadata() {
    [[ -f "$SHARED_DIR/components.properties" ]] ||
        die "Missing shared configurator data: components.properties"
    [[ -f "$SHARED_DIR/java-versions.properties" ]] ||
        die "Missing shared configurator data: java-versions.properties"
    [[ -f "$SHARED_DIR/memory-presets.tsv" ]] ||
        die "Missing shared configurator data: memory-presets.tsv"
    [[ -f "$SHARED_DIR/classpath.entries" ]] ||
        die "Missing shared configurator data: classpath.entries"

    FastRenderingReleasesUrl="$(read_property FastRenderingLinuxReleasesUrl "$SHARED_DIR/components.properties")"
    ResourceCacheReleasesUrl="$(read_property ResourceCacheReleasesUrl "$SHARED_DIR/components.properties")"
    ResourceCacheLatestReleaseApi="$(read_property ResourceCacheLatestReleaseApi "$SHARED_DIR/components.properties")"
    ResourceCacheAssetUrlPrefix="$(read_property ResourceCacheAssetUrlPrefix "$SHARED_DIR/components.properties")"
    VramOptimizerReleasesUrl="$(read_property VramOptimizerReleasesUrl "$SHARED_DIR/components.properties")"

    # Load memory presets (index|megabytes|description) into associative arrays.
    declare -gA HEAP_VALUE=()
    declare -gA HEAP_DESC=()
    declare -ga HEAP_ORDER=()
    local idx value desc
    while IFS='|' read -r idx value desc; do
        desc="${desc%$'\r'}"
        [[ "$idx" =~ ^[0-9]+$ ]] || continue
        HEAP_VALUE["$idx"]="$value"
        HEAP_DESC["$idx"]="$desc"
        HEAP_ORDER+=("$idx")
    done < "$SHARED_DIR/memory-presets.tsv"

    # Build base classpath joined with the Linux ':' separator.
    BASE_CLASSPATH=""
    local entry
    while IFS= read -r entry || [[ -n "$entry" ]]; do
        entry="${entry%$'\r'}"
        [[ -n "$entry" && "$entry" != \#* ]] || continue
        if [[ "$entry" == ../mikohime/* ]]; then
            entry="${entry#../}"
        fi
        BASE_CLASSPATH+="${BASE_CLASSPATH:+:}$entry"
    done < "$SHARED_DIR/classpath.entries"
    [[ -n "$BASE_CLASSPATH" ]] || die "The shared classpath definition is empty."
}
# -----------------------------------------------------------------------------
# Colors (respect NO_COLOR and non-terminal output)
# -----------------------------------------------------------------------------
ColorGreen=""
ColorYellow=""
ColorRed=""
ColorReset=""
initialize_colors() {
    if [[ -n "${NO_COLOR:-}" ]]; then
        return 0
    fi
    if [[ ! -t 1 ]]; then
        return 0
    fi
    local esc
    esc=$'\033'
    ColorGreen="${esc}[92m"
    ColorYellow="${esc}[93m"
    ColorRed="${esc}[91m"
    ColorReset="${esc}[0m"
}

# -----------------------------------------------------------------------------
# Cleanup and signal handling
# -----------------------------------------------------------------------------
register_temp_file() { OWNED_TEMP_FILES+=("$1"); }
register_temp_dir() { OWNED_TEMP_DIRS+=("$1"); }

is_owned_stage_dir() {
    # A directory may be recursively removed only when it is one we created:
    # it must resolve under GAME_ROOT and match our temp naming pattern.
    local dir="$1" resolved
    [[ -n "$dir" && -d "$dir" ]] || return 1
    resolved="$(cd -- "$dir" 2>/dev/null && pwd -P)" || return 1
    case "$resolved" in
        "$GAME_ROOT"/.configure_stage_*|"$GAME_ROOT"/.jdk-*.stage.*|"$GAME_ROOT"/.resource-cache-*|"$GAME_ROOT"/.jdk-install-*) return 0 ;;
        *) return 1 ;;
    esac
}

cleanup() {
    local item
    for item in "${OWNED_TEMP_FILES[@]:-}"; do
        [[ -n "$item" ]] && rm -f -- "$item" 2>/dev/null
    done
    for item in "${OWNED_TEMP_DIRS[@]:-}"; do
        [[ -n "$item" ]] || continue
        if is_owned_stage_dir "$item"; then
            rm -rf -- "$item" 2>/dev/null
        else
            rmdir -- "$item" 2>/dev/null || true
        fi
    done
    release_lock
}

on_signal() {
    local sig="$1"
    printf '\n%sInterrupted (%s). Cleaning up; no files were partially written.%s\n' \
        "${ColorYellow:-}" "$sig" "${ColorReset:-}" >&2
    cleanup
    trap - EXIT
    exit 130
}

install_traps() {
    trap 'cleanup' EXIT
    trap 'on_signal INT' INT
    trap 'on_signal TERM' TERM
}

# -----------------------------------------------------------------------------
# Small helpers
# -----------------------------------------------------------------------------
is_positive_integer() {
    [[ "${1:-}" =~ ^[0-9]+$ && "${1:-}" != "0" ]]
}

java_major() {
    # Print the Java major version number for the java executable in $1.
    "$1" -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1
}

is_supported_major() {
    case "${1:-}" in
        17|27|28) return 0 ;;
        *) return 1 ;;
    esac
}

# -----------------------------------------------------------------------------
# Lock, write-access, and installation validation
# -----------------------------------------------------------------------------
acquire_lock() {
    if ! mkdir -- "$LOCK_DIR" 2>/dev/null; then
        printf 'Another configurator instance is already running, or a stale lock exists:\n' >&2
        printf '  %s\n' "$LOCK_DIR" >&2
        printf 'Close the other instance. If none is running, remove that directory manually.\n' >&2
        return 1
    fi
    LOCK_OWNED=1
    {
        printf 'pid=%s\n' "$$"
        printf 'user=%s\n' "${USER:-$(id -un 2>/dev/null || echo unknown)}"
        printf 'host=%s\n' "$(hostname 2>/dev/null || echo unknown)"
        printf 'started=%s\n' "$(date 2>/dev/null || echo unknown)"
    } > "$LOCK_DIR/owner.meta" 2>/dev/null || true
    return 0
}

release_lock() {
    # Only remove the lock when this process owns it and the owner metadata
    # confirms our PID, so a foreign lock is never deleted.
    [[ "$LOCK_OWNED" == 1 ]] || return 0
    if [[ -f "$LOCK_DIR/owner.meta" ]]; then
        if ! grep -qx "pid=$$" "$LOCK_DIR/owner.meta" 2>/dev/null; then
            return 0
        fi
        rm -f -- "$LOCK_DIR/owner.meta" 2>/dev/null || true
    fi
    rmdir -- "$LOCK_DIR" 2>/dev/null || true
    LOCK_OWNED=0
}

check_write_access() {
    local test_file
    test_file="$GAME_ROOT/.configure_write_test_${TRANSACTION_ID}.tmp"
    if ! : > "$test_file" 2>/dev/null; then
        printf 'This configurator cannot write to the Starsector installation:\n  %s\n' "$GAME_ROOT" >&2
        return 1
    fi
    rm -f -- "$test_file" 2>/dev/null
    test_file="$MIKOHIME_DIR/.configure_write_test_${TRANSACTION_ID}.tmp"
    if ! : > "$test_file" 2>/dev/null; then
        printf 'This configurator cannot write to the mikohime folder:\n  %s\n' "$MIKOHIME_DIR" >&2
        return 1
    fi
    rm -f -- "$test_file" 2>/dev/null
    return 0
}

validate_installation() {
    [[ -x "$GAME_ROOT/starsector.sh" || -x "$GAME_ROOT/starsector" ]] ||
        die "This is not a Linux Starsector installation (missing an executable starsector.sh or starsector)."
    [[ -f "$GAME_ROOT/fs.common_obf.jar" ]] ||
        die "This Linux installation is missing fs.common_obf.jar beside starsector.sh."
    [[ -d "$MIKOHIME_DIR" ]] || die "Required directory mikohime was not found."
    [[ -f "$DEFAULT_VM" ]] || die "The Linux Mikohime DefaultVM preset is missing: $DEFAULT_VM"

    local native
    for native in liblwjgl64.so libopenal64.so libjinput-linux64.so; do
        [[ -f "$MIKOHIME_DIR/linux/$native" ]] ||
            die "The Linux Mikohime native library is missing: mikohime/linux/$native"
    done

    local bg
    for bg in default_bg.jpg pather_bg.jpg mimikko_bg.jpg gamma_bg.jpg; do
        [[ -f "$BG_DIR/$bg" ]] ||
            die "A launcher background asset is missing: mikohime/bg/$bg"
    done

    [[ -f "$MIKOHIME_DIR/miko_jxp.properties" ]] ||
        die "Shared configuration file miko_jxp.properties is missing from mikohime."
    [[ -f "$SHARED_DIR/classpath.entries" ]] ||
        die "Shared configurator data was not found under mikohime/configurator/shared."
}
# -----------------------------------------------------------------------------
# Java discovery
# -----------------------------------------------------------------------------
# Populated by detect_java_installations:
declare -a JAVA_PATHS=()
declare -a JAVA_VERSIONS=()
declare -a JAVA_LABELS=()
MODERN_JAVA_AVAILABLE="No"

add_java_candidate() {
    # add_java_candidate JAVA_EXECUTABLE LABEL
    local exe="$1" label="$2" major resolved existing
    [[ -x "$exe" && -f "$exe" ]] || return 0
    major="$(java_major "$exe")"
    is_supported_major "$major" || return 0
    resolved="$(cd -- "$(dirname -- "$exe")" 2>/dev/null && printf '%s/%s' "$PWD" "$(basename -- "$exe")")" ||
        return 0
    for existing in "${JAVA_PATHS[@]:-}"; do
        [[ -n "$existing" ]] || continue
        if [[ "$existing" == "$resolved" ]]; then
            return 0
        fi
    done
    JAVA_PATHS+=("$resolved")
    JAVA_VERSIONS+=("$major")
    JAVA_LABELS+=("$label - Java $major")
    if [[ "$major" == 27 || "$major" == 28 ]]; then
        MODERN_JAVA_AVAILABLE="Yes"
    fi
}

detect_java_installations() {
    JAVA_PATHS=()
    JAVA_VERSIONS=()
    JAVA_LABELS=()
    MODERN_JAVA_AVAILABLE="No"

    # Bundled runtimes.
    add_java_candidate "$GAME_ROOT/jre_linux/bin/java" "Starsector bundled Java (jre_linux)"
    add_java_candidate "$GAME_ROOT/jre/bin/java" "Starsector bundled Java (jre)"

    # Any jdk-27* / jdk-28* directory (version suffixes included), sorted so the
    # highest build wins the lower menu numbers.
    local dir name
    while IFS= read -r dir; do
        [[ -n "$dir" ]] || continue
        name="$(basename -- "$dir")"
        add_java_candidate "$dir/bin/java" "Java installation ($name)"
    done < <(find "$GAME_ROOT" -maxdepth 1 -mindepth 1 -type d \
                \( -name 'jdk-27*' -o -name 'jdk-28*' \) 2>/dev/null | sort -r)

    # System Java on PATH.
    if command -v java >/dev/null 2>&1; then
        add_java_candidate "$(command -v java)" "System Java"
    fi
}

# -----------------------------------------------------------------------------
# Java download / install (pinned Java 27 / 28)
# -----------------------------------------------------------------------------
archive_path_escapes_root() {
    local path="$1" component depth=0
    [[ "$path" != /* ]] || return 0
    IFS='/' read -r -a path_components <<< "$path"
    for component in "${path_components[@]}"; do
        case "$component" in
            ''|.) ;;
            ..)
                (( depth > 0 )) || return 0
                ((depth--))
                ;;
            *) ((depth++)) ;;
        esac
    done
    return 1
}

archive_has_unsafe_entries() {
    # Resolve link targets relative to the link location. Adoptium JDKs contain
    # legitimate ../ symlinks that remain inside the archive root.
    local archive="$1" line name target
    while IFS= read -r line; do
        # tar -tvzf lines: "<perms> owner/group size date time name[ -> target]"
        name="$(printf '%s\n' "$line" | awk '{ $1=$2=$3=$4=$5=""; sub(/^ +/, ""); print }')"
        target=""
        if [[ "$name" == *" -> "* ]]; then
            target="${name#* -> }"
            name="${name%% -> *}"
        elif [[ "$name" == *" link to "* ]]; then
            target="${name#* link to }"
            name="${name%% link to *}"
        fi
        archive_path_escapes_root "$name" && return 0
        if [[ -n "$target" ]] &&
           archive_path_escapes_root "$(dirname -- "$name")/$target"; then
            return 0
        fi
    done < <(tar -tvzf "$archive" 2>/dev/null)
    return 1
}

install_java() {
    local requested="$1" prefix version folder url expected
    case "$requested" in
        27) prefix=Jdk27 ;;
        28) prefix=Jdk28 ;;
        *) die "Java install accepts 27 or 28." ;;
    esac
    version="$(read_property "${prefix}LinuxVersion" "$SHARED_DIR/java-versions.properties")"
    folder="$(read_property "${prefix}LinuxFolder" "$SHARED_DIR/java-versions.properties")"
    url="$(read_property "${prefix}LinuxUrl" "$SHARED_DIR/java-versions.properties")"
    expected="$(read_property "${prefix}LinuxSha256" "$SHARED_DIR/java-versions.properties")"
    [[ -n "$version" && -n "$folder" && -n "$url" && -n "$expected" ]] ||
        die "The Java $requested download metadata is incomplete."

    if [[ -e "$GAME_ROOT/$folder" ]]; then
        if [[ -x "$GAME_ROOT/$folder/bin/java" ]] &&
           [[ "$(java_major "$GAME_ROOT/$folder/bin/java")" == "$requested" ]]; then
            printf '%sJava %s is already installed at %s.%s\n' \
                "${ColorGreen:-}" "$requested" "$folder" "${ColorReset:-}"
            return 0
        fi
        die "The destination folder already exists but is not the expected JDK: $folder"
    fi

    command -v curl >/dev/null 2>&1 || die "curl is required to install Java."
    command -v sha256sum >/dev/null 2>&1 || die "sha256sum is required to install Java."
    command -v tar >/dev/null 2>&1 || die "tar is required to install Java."

    local archive stage extracted actual
    archive="$(mktemp "$GAME_ROOT/.jdk-${requested}.XXXXXX.tar.gz")" || die "Unable to create a temporary file."
    register_temp_file "$archive"
    printf 'Downloading Java %s (%s)...\n' "$requested" "$version"
    curl --fail --location --proto '=https' --tlsv1.2 --silent --show-error --output "$archive" "$url" ||
        die "The Java download failed."

    actual="$(sha256sum "$archive" | awk '{print $1}')"
    [[ "$actual" == "$expected" ]] || die "The downloaded Java archive checksum does not match."
    tar -tzf "$archive" >/dev/null 2>&1 || die "The Java archive failed its integrity check."
    if archive_has_unsafe_entries "$archive"; then
        die "The Java archive contains unsafe (absolute, traversing, or escaping) paths."
    fi

    stage="$(mktemp -d "$GAME_ROOT/.jdk-${requested}.stage.XXXXXX")" || die "Unable to create a staging directory."
    register_temp_dir "$stage"
    tar --no-same-owner -xzf "$archive" -C "$stage" ||
        die "The Java archive could not be extracted."
    extracted="$(find "$stage" -mindepth 1 -maxdepth 1 -type d -print -quit)"
    [[ -n "$extracted" && -x "$extracted/bin/java" ]] ||
        die "The downloaded archive does not contain a JDK."
    [[ "$(java_major "$extracted/bin/java")" == "$requested" ]] ||
        die "The downloaded archive contains the wrong Java version."

    [[ ! -e "$GAME_ROOT/$folder" ]] || die "The Java destination appeared while the download was running."
    mv -- "$extracted" "$GAME_ROOT/$folder" || die "Unable to move the JDK into place."
    printf '%sInstalled Java %s at %s.%s\n' \
        "${ColorGreen:-}" "$requested" "$GAME_ROOT/$folder" "${ColorReset:-}"
}
# -----------------------------------------------------------------------------
# Optional component detection
# -----------------------------------------------------------------------------
FAST_RENDERING_AVAILABLE="No"
FAST_RENDERING_DETECTED="No"
RESOURCE_CACHE_AVAILABLE="No"
RESOURCE_CACHE_DETECTED="No"
VRAM_OPTIMIZER_AVAILABLE="No"

detect_optional_components() {
    FAST_RENDERING_AVAILABLE="No"
    FAST_RENDERING_DETECTED="No"
    if [[ -s "$GAME_ROOT/fr.jar" && -s "$GAME_ROOT/fr.agent.jar" ]]; then
        FAST_RENDERING_DETECTED="Yes"
        FAST_RENDERING_AVAILABLE="Yes"
    fi

    RESOURCE_CACHE_AVAILABLE="No"
    RESOURCE_CACHE_DETECTED="No"
    if [[ -s "$GAME_ROOT/fr-resource-cache-agent.jar" ]]; then
        RESOURCE_CACHE_DETECTED="Yes"
        [[ "$FAST_RENDERING_AVAILABLE" == Yes ]] &&
            RESOURCE_CACHE_AVAILABLE="Yes"
    fi

    detect_vram_optimizer
}

detect_vram_optimizer() {
    VRAM_OPTIMIZER_AVAILABLE="No"
    [[ -d "$MODS_DIR" ]] || return 0
    local dir meta
    while IFS= read -r dir; do
        [[ -n "$dir" ]] || continue
        meta="$dir/mod_info.json"
        [[ -f "$meta" ]] || continue
        if grep -Eiq '"id"[[:space:]]*:[[:space:]]*"VramOptimizer"' "$meta" 2>/dev/null; then
            VRAM_OPTIMIZER_AVAILABLE="Yes"
            return 0
        fi
    done < <(find "$MODS_DIR" -maxdepth 1 -mindepth 1 -type d 2>/dev/null)
}

# -----------------------------------------------------------------------------
# System resource detection (memory, CPU, huge pages)
# -----------------------------------------------------------------------------
PHYSICAL_MEMORY_MIB=""
PHYSICAL_MEMORY_GIB=""
SAFE_HEAP_MIB=""
PHYSICAL_CORE_COUNT=""
LOGICAL_PROCESSOR_COUNT="1"
HUGE_PAGES_STATUS="Unavailable"
HUGE_PAGES_CAPABLE="No"

detect_system_resources() {
    detect_memory
    detect_cpu
    detect_huge_pages
}

detect_memory() {
    PHYSICAL_MEMORY_MIB=""
    PHYSICAL_MEMORY_GIB=""
    SAFE_HEAP_MIB=""
    local kib
    kib="$(awk '/^MemTotal:/ { print $2; exit }' /proc/meminfo 2>/dev/null || true)"
    if [[ -z "$kib" ]] && command -v sysctl >/dev/null 2>&1; then
        local bytes
        bytes="$(sysctl -n hw.physmem 2>/dev/null || true)"
        [[ "$bytes" =~ ^[0-9]+$ ]] && kib=$(( bytes / 1024 ))
    fi
    if [[ "$kib" =~ ^[0-9]+$ ]] && (( kib > 0 )); then
        PHYSICAL_MEMORY_MIB=$(( kib / 1024 ))
        PHYSICAL_MEMORY_GIB=$(( (PHYSICAL_MEMORY_MIB + 1023) / 1024 ))
        SAFE_HEAP_MIB=$(( PHYSICAL_MEMORY_MIB * 75 / 100 ))
    fi
}

detect_cpu() {
    PHYSICAL_CORE_COUNT=""
    LOGICAL_PROCESSOR_COUNT="1"
    local logical
    if command -v nproc >/dev/null 2>&1; then
        logical="$(nproc 2>/dev/null || true)"
    fi
    if [[ ! "$logical" =~ ^[0-9]+$ ]] || (( logical < 1 )); then
        logical="$(getconf _NPROCESSORS_ONLN 2>/dev/null || true)"
    fi
    [[ "$logical" =~ ^[0-9]+$ ]] && (( logical >= 1 )) && LOGICAL_PROCESSOR_COUNT="$logical"

    local cores=""
    if command -v lscpu >/dev/null 2>&1; then
        cores="$(lscpu -p=core 2>/dev/null | grep -v '^#' | sort -u | grep -c . || true)"
    fi
    if [[ ! "$cores" =~ ^[0-9]+$ ]] || (( cores < 1 )); then
        if [[ -r /proc/cpuinfo ]]; then
            cores="$(awk -F: '
                /^physical id/ { pid=$2 }
                /^core id/     { print pid ":" $2 }
            ' /proc/cpuinfo 2>/dev/null | sort -u | grep -c . || true)"
        fi
    fi
    [[ "$cores" =~ ^[0-9]+$ ]] && (( cores >= 1 )) && PHYSICAL_CORE_COUNT="$cores"
}

detect_huge_pages() {
    # On Linux, -XX:+UseLargePages with a fully pre-touched heap needs explicitly
    # configured huge pages AND permission to lock that memory (memlock ulimit or
    # CAP_IPC_LOCK). This is a distinct mechanism from the Windows
    # SeLockMemoryPrivilege, so no Windows privilege is implied here.
    HUGE_PAGES_STATUS="Unavailable"
    HUGE_PAGES_CAPABLE="No"

    local total="" memlock
    total="$(awk '/^HugePages_Total:/ { print $2; exit }' /proc/meminfo 2>/dev/null || true)"
    if [[ ! "$total" =~ ^[0-9]+$ ]]; then
        HUGE_PAGES_STATUS="Unavailable (huge pages not reported by this system)"
        return 0
    fi
    if (( total <= 0 )); then
        HUGE_PAGES_STATUS="Not configured (HugePages_Total is 0; reserve huge pages first)"
        return 0
    fi

    memlock="$(ulimit -l 2>/dev/null || echo 0)"
    local can_lock="No"
    if [[ "$memlock" == "unlimited" ]]; then
        can_lock="Yes"
    elif [[ "$memlock" =~ ^[0-9]+$ ]] && (( memlock >= 1048576 )); then
        can_lock="Yes"
    fi
    if [[ "$(id -u 2>/dev/null || echo 1000)" == "0" ]]; then
        can_lock="Yes"
    fi

    if [[ "$can_lock" == "Yes" ]]; then
        HUGE_PAGES_CAPABLE="Yes"
        HUGE_PAGES_STATUS="Available ($total pages reserved; memory locking permitted)"
    else
        HUGE_PAGES_STATUS="Configured but memory-lock limit too low (raise memlock ulimit or grant CAP_IPC_LOCK)"
    fi
}

refresh_environment() {
    detect_java_installations
    detect_optional_components
    detect_system_resources
}
# -----------------------------------------------------------------------------
# FR Resource Cache installer (GitHub latest release)
# -----------------------------------------------------------------------------
install_resource_cache() {
    local dest="$GAME_ROOT/fr-resource-cache-agent.jar"
    if [[ -e "$dest" ]]; then
        if [[ -s "$dest" ]]; then
            printf '%sFR Resource Cache is already installed.%s\n' "${ColorGreen:-}" "${ColorReset:-}"
            return 0
        fi
        die "The destination file exists but is empty or invalid: $dest"
    fi

    local tool
    for tool in curl unzip sha256sum; do
        command -v "$tool" >/dev/null 2>&1 || die "$tool is required to install FR Resource Cache."
    done
    [[ -n "$ResourceCacheLatestReleaseApi" && -n "$ResourceCacheAssetUrlPrefix" ]] ||
        die "FR Resource Cache release metadata is incomplete."

    local stage api_json asset_line url size digest zip staged actual
    stage="$(mktemp -d "$GAME_ROOT/.resource-cache-install.XXXXXX")" || die "Unable to create a staging directory."
    register_temp_dir "$stage"
    api_json="$stage/release.json"
    zip="$stage/release.zip"
    staged="$stage/fr-resource-cache-agent.jar"

    printf 'Finding the latest FR Resource Cache release...\n'
    curl --fail --location --proto '=https' --tlsv1.2 --silent --show-error \
        -H 'Accept: application/vnd.github+json' \
        -H 'User-Agent: Mikohime-Java-Configurator' \
        -H 'X-GitHub-Api-Version: 2022-11-28' \
        --output "$api_json" "$ResourceCacheLatestReleaseApi" ||
        die "Unable to query the FR Resource Cache release API."

    # Exactly one .zip asset must be present.
    local -a zip_urls=()
    while IFS= read -r url; do
        [[ -n "$url" ]] && zip_urls+=("$url")
    done < <(grep -oE '"browser_download_url"[[:space:]]*:[[:space:]]*"[^"]*\.zip"' "$api_json" \
                | sed -E 's/.*"(https[^"]*\.zip)"$/\1/')
    (( ${#zip_urls[@]} == 1 )) || die "The latest release must contain exactly one ZIP asset."
    url="${zip_urls[0]}"

    case "$url" in
        "$ResourceCacheAssetUrlPrefix"*) : ;;
        *) die "The release asset URL is not trusted: $url" ;;
    esac

    # Locate the asset object to read its size and digest.
    asset_line="$(tr -d '\n' < "$api_json" | sed 's/},[[:space:]]*{/}\n{/g' | grep -F "$url" | head -n 1)"
    size="$(printf '%s' "$asset_line" | sed -n 's/.*"size"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -n1)"
    digest="$(printf '%s' "$asset_line" | sed -n 's/.*"digest"[[:space:]]*:[[:space:]]*"\(sha256:[0-9a-fA-F]\{64\}\)".*/\1/p' | head -n1)"
    [[ "$size" =~ ^[0-9]+$ ]] && (( size > 0 && size <= 10485760 )) ||
        die "The release asset size is missing or exceeds the 10 MB cap."

    curl --fail --location --proto '=https' --tlsv1.2 --silent --show-error --output "$zip" "$url" ||
        die "Downloading the FR Resource Cache release failed."

    if [[ -n "$digest" ]]; then
        actual="sha256:$(sha256sum "$zip" | awk '{print $1}')"
        [[ "$actual" == "$digest" ]] || die "The downloaded release does not match GitHub's SHA-256 digest."
    fi

    unzip -tqq "$zip" >/dev/null 2>&1 || die "The downloaded release ZIP failed its integrity check."

    # Exactly one packaged JAR named fr-resource-cache-agent.jar.
    local -a jar_entries=()
    while IFS= read -r line; do
        [[ "$line" == *fr-resource-cache-agent.jar ]] && jar_entries+=("$line")
    done < <(unzip -Z1 "$zip" 2>/dev/null | grep -E '(^|/)fr-resource-cache-agent\.jar$')
    (( ${#jar_entries[@]} == 1 )) || die "The release does not contain exactly one agent JAR."

    unzip -p "$zip" "${jar_entries[0]}" > "$staged" 2>/dev/null || die "Unable to extract the agent JAR."
    [[ -s "$staged" ]] || die "The extracted agent JAR is empty."

    # Validate the JAR manifest Premain-Class.
    local manifest
    manifest="$(unzip -p "$staged" META-INF/MANIFEST.MF 2>/dev/null | tr -d '\r')"
    if ! printf '%s\n' "$manifest" | grep -Eq '^Premain-Class:[[:space:]]*dev\.frresourcecache\.FrResourceCacheAgent[[:space:]]*$'; then
        die "The agent JAR manifest is not a valid FR Resource Cache agent."
    fi

    [[ ! -e "$dest" ]] || die "The destination file appeared while the download was running."
    mv -- "$staged" "$dest" || die "Unable to install the FR Resource Cache agent."
    printf '%sThe latest FR Resource Cache release was installed successfully:%s\n  %s\n' \
        "${ColorGreen:-}" "${ColorReset:-}" "$dest"
}

# -----------------------------------------------------------------------------
# Missing component download pages
# -----------------------------------------------------------------------------
graphical_opener() {
    if command -v xdg-open >/dev/null 2>&1; then
        printf 'xdg-open'
    elif command -v gio >/dev/null 2>&1; then
        printf 'gio'
    fi
}

open_url() {
    local url="$1" opener
    opener="$(graphical_opener)"
    if [[ -z "$opener" ]]; then
        warn "No graphical URL opener (xdg-open or gio) is available."
        printf 'Open this page manually: %s\n' "$url"
        return 1
    fi
    if [[ "$opener" == gio ]]; then
        gio open "$url" >/dev/null 2>&1 &
    else
        xdg-open "$url" >/dev/null 2>&1 &
    fi
    printf 'Requested your browser open: %s\n' "$url"
    return 0
}
# -----------------------------------------------------------------------------
# Configuration generation (pending files, validation, transactional commit)
# -----------------------------------------------------------------------------
SIMPLE_PENDING=""
LAUNCHER_PENDING=""
LOGGING_PENDING=""
INFO_PENDING=""
BG_PENDING=""
declare -a COMMIT_TARGETS=()
declare -a COMMIT_PENDINGS=()

# Lines of the Linux DefaultVM whose Java 27 output is regenerated below and so
# are stripped (matched at the start of the line) before being re-emitted.
J27_STRIP_PREFIXES=(
    '-XX:+UseCriticalCompilerThreadPriority'
    '-XX:ThreadPriorityPolicy=1'
    '-XX:MaxGCPauseMillis='
    '#-XX:MaxGCPauseMillis='
    '-XX:+ShowCodeDetailsInExceptionMessages'
    '#-XX:+ShowCodeDetailsInExceptionMessages'
    '-XX:+ExtensiveErrorReports'
    '#-XX:+ExtensiveErrorReports'
    '-XX:+ErrorLogSecondaryErrorDetails'
    '#-XX:+ErrorLogSecondaryErrorDetails'
    '-XX:+PrintCommandLineFlags'
    '#-XX:+PrintCommandLineFlags'
    '-Xlog:async'
    '#-Xlog:async'
    '-Xlog:gc+init'
    '#-Xlog:gc+init'
    '-DAsyncLogger.WaitStrategy=busyspin'
    '#-DAsyncLogger.WaitStrategy=busyspin'
    '-Dsun.java2d.renderer.useLogger=true'
    '#-Dsun.java2d.renderer.useLogger=true'
)

# Whole lines removed for old-CPU (pre-AVX2) compatibility.
OLDCPU_STRIP_LINES=(
    '-XX:UseAVX=3'
    '-XX:AVX3Threshold=0'
    '-XX:CopyAVX3Threshold=0'
    '-XX:+UseFMA'
    '-XX:+UseBMI1Instructions'
    '-XX:+UseBMI2Instructions'
)

sappend() { printf '%s\n' "$@" >> "$SIMPLE_PENDING"; }

logging_prefix() {
    [[ "$LOGGING_MODE" == "Minimal" ]] && printf '#' || true
}

strip_prefix_lines() {
    # stdin -> stdout, dropping lines starting with any argument prefix.
    local plist; plist="$(printf '%s\n' "$@")"
    awk -v plist="$plist" '
        BEGIN { n = split(plist, arr, "\n") }
        {
            keep = 1
            for (i = 1; i <= n; i++) {
                p = arr[i]
                if (p == "") continue
                if (substr($0, 1, length(p)) == p) { keep = 0; break }
            }
            if (keep) print
        }
    '
}

strip_exact_lines() {
    # stdin -> stdout, dropping lines equal to any argument.
    local llist; llist="$(printf '%s\n' "$@")"
    awk -v llist="$llist" '
        BEGIN {
            n = split(llist, arr, "\n")
            for (i = 1; i <= n; i++) if (arr[i] != "") drop[arr[i]] = 1
        }
        { if (!($0 in drop)) print }
    '
}

append_jvm_diagnostic_logging() {
    local p; p="$(logging_prefix)"
    sappend "${p}-XX:+ShowCodeDetailsInExceptionMessages"
    sappend "${p}-XX:+ExtensiveErrorReports"
    [[ "$JAVA_VERSION" == 28 ]] && sappend "${p}-XX:+ErrorLogSecondaryErrorDetails"
    sappend "${p}-XX:+PrintCommandLineFlags"
    sappend "${p}-Xlog:async"
    sappend "${p}-Xlog:gc+init"
    return 0
}

append_jvm_logger_options() {
    local p; p="$(logging_prefix)"
    sappend "${p}-DAsyncLogger.WaitStrategy=busyspin"
    sappend "${p}-Dsun.java2d.renderer.useLogger=true"
    return 0
}

append_agents_and_classpath() {
    [[ "$RESOURCE_CACHE_STATUS" == Enabled ]] &&
        sappend "-javaagent:fr-resource-cache-agent.jar=gameRoot=.,installRoot=.,cacheDir=./fr-resource-cache,flushDelaySeconds=30,memoryCacheMiB=128,maxFileSize=1048576"
    [[ "$FAST_RENDERING_STATUS" == Enabled ]] &&
        sappend '-javaagent:fr.agent.jar'
    if [[ "$FAST_RENDERING_STATUS" == Enabled ]]; then
        sappend "-classpath fr.jar:$BASE_CLASSPATH"
    else
        sappend "-classpath $BASE_CLASSPATH"
    fi
    return 0
}

append_game_paths() {
    sappend \
        '-Dcom.fs.starfarer.settings.paths.saves=./saves' \
        '-Dcom.fs.starfarer.settings.paths.screenshots=./screenshots' \
        '-Dcom.fs.starfarer.settings.paths.mods=./mods' \
        '-Dcom.fs.starfarer.settings.paths.logs=.' \
        '-Dcom.fs.starfarer.settings.linux=true' \
        'com.fs.starfarer.StarfarerLauncher'
    return 0
}

append_modern_system_properties() {
    sappend \
        '-XX:ReservedCodeCacheSize=256m' \
        '-Djava.library.path=mikohime/linux' \
        '-XX:-BytecodeVerificationLocal' \
        '-XX:-BytecodeVerificationRemote' \
        '-Dlog4j1.compatibility=true' \
        '-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector'
    append_jvm_logger_options
    sappend \
        '-Dlog4j2.enableThreadlocals=true' \
        '-Dlog4j2.enableDirectEncoders=true' \
        '-Dlog4j2.garbagefreeThreadContextMap=true' \
        '-Djava.util.Arrays.useLegacyMergeSort=true' \
        '-Dsun.java2d.renderer.useRef=weak' \
        '-Dlog4j.configuration=mikohime/mikohime.properties' \
        '-Djava.xml.config.file=mikohime/miko_jxp.properties' \
        '-Dcom.fs.starfarer.launcher_bg=mikohime/launcher_bg.jpg' \
        '--add-opens=java.base/sun.nio.ch=ALL-UNNAMED' \
        '--add-opens=java.base/java.nio=ALL-UNNAMED' \
        '--add-opens=java.base/java.util=ALL-UNNAMED' \
        '--add-opens=java.base/java.util.concurrent=ALL-UNNAMED' \
        '--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED' \
        '--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED' \
        '--add-opens=java.base/java.lang.reflect=ALL-UNNAMED' \
        '--add-opens=java.base/java.lang.ref=ALL-UNNAMED' \
        '--add-opens=java.base/java.lang=ALL-UNNAMED' \
        '--add-opens=java.management/javax.management=ALL-UNNAMED' \
        '--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED' \
        '--add-opens=java.base/java.text=ALL-UNNAMED' \
        '--add-opens=java.desktop/java.awt.font=ALL-UNNAMED' \
        '--add-opens=java.desktop/java.awt=ALL-UNNAMED' \
        '--enable-native-access=ALL-UNNAMED'
    return 0
}
write_simple_java27() {
    [[ -f "$DEFAULT_VM" ]] || { warn "Missing required Java 27 preset: mikohime/DefaultVM"; return 1; }
    tr -d '\r' < "$DEFAULT_VM" |
        strip_prefix_lines "${J27_STRIP_PREFIXES[@]}" > "$SIMPLE_PENDING" || return 1
    sed 's#\.\./mikohime#mikohime#g' "$SIMPLE_PENDING" > "$SIMPLE_PENDING.tmp" || return 1
    mv -f -- "$SIMPLE_PENDING.tmp" "$SIMPLE_PENDING" || return 1
    sappend '-XX:+UseCriticalCompilerThreadPriority'
    sappend '-XX:ThreadPriorityPolicy=1'
    sappend '#-XX:MaxGCPauseMillis=100'
    append_jvm_diagnostic_logging
    append_jvm_logger_options
    sappend '-Xss4m'
    sappend "-Xms${HEAP_MIB}m"
    sappend "-Xmx${HEAP_MIB}m"
    if [[ "$LOW_CORE_MODE" == "Yes" ]]; then
        sappend '-XX:CICompilerCount=2'
        sappend '-XX:ConcGCThreads=1'
    fi
    [[ "$LARGE_PAGES_ENABLED" == "Yes" ]] && sappend '-XX:+UseLargePages'
    if [[ "$OLD_CPU_MODE" == "Yes" ]]; then
        strip_exact_lines "${OLDCPU_STRIP_LINES[@]}" < "$SIMPLE_PENDING" > "$SIMPLE_PENDING.tmp" || return 1
        mv -f -- "$SIMPLE_PENDING.tmp" "$SIMPLE_PENDING" || return 1
        sappend '-XX:UseAVX=0'
    else
        sappend '-XX:+UseCMoveUnconditionally'
    fi
    append_agents_and_classpath
    append_game_paths
    return 0
}

write_simple_modern() {
    : > "$SIMPLE_PENDING" || return 1
    sappend '-XX:+UnlockDiagnosticVMOptions'
    sappend '-XX:+UnlockExperimentalVMOptions'
    append_jvm_diagnostic_logging
    sappend '-XX:+TieredCompilation'
    sappend '-XX:TieredStopAtLevel=4'
    if [[ "$JAVA_VERSION" == 28 ]]; then
        sappend '-XX:-UseCompactObjectHeaders'
        sappend '-XX:-NMethodRelocation'
        sappend '-XX:+DisableExplicitGC'
    fi
    sappend '-XX:+UseG1GC'
    sappend '#-XX:MaxGCPauseMillis=100'
    sappend '-XX:+UseStringDeduplication'
    [[ "$JAVA_VERSION" == 28 ]] && sappend '-XX:+AlwaysPreTouchStacks'
    sappend '-XX:+AlwaysPreTouch'
    [[ "$OLD_CPU_MODE" == "Yes" ]] && sappend '-XX:UseAVX=0'
    if [[ "$LOW_CORE_MODE" == "Yes" ]]; then
        sappend '-XX:CICompilerCount=2'
        sappend '-XX:ConcGCThreads=1'
    fi
    [[ "$LARGE_PAGES_ENABLED" == "Yes" ]] && sappend '-XX:+UseLargePages'
    append_modern_system_properties
    [[ "$JAVA_VERSION" == 28 ]] && sappend '--enable-final-field-mutation=ALL-UNNAMED'
    sappend '-Xss4m'
    sappend "-Xms${HEAP_MIB}m"
    sappend "-Xmx${HEAP_MIB}m"
    append_agents_and_classpath
    append_game_paths
    return 0
}

write_info() {
    local vm_line
    case "$JAVA_VERSION" in
        28) vm_line="Java 28 safe G1 preset" ;;
        17) vm_line="Java 17 safe G1 preset" ;;
        *)  vm_line="Mikohime Java 27 preset" ;;
    esac
    {
        printf 'Mikohime Linux configuration\n'
        printf 'Memory allocation  : %s\n' "$HEAP_DESCRIPTION"
        printf 'Java installation  : %s (Java %s)\n' "$SELECTED_JAVA" "$JAVA_VERSION"
        printf 'VM tuning          : %s\n' "$vm_line"
        if [[ "$JAVA_VERSION" == 28 ]]; then
            printf 'Compact headers    : Disabled\n'
            printf 'NMethod relocation : Disabled\n'
        fi
        if [[ "$LOW_CORE_MODE" == "Yes" ]]; then
            printf 'CPU management     : Low-core tuning enabled\n'
        else
            printf 'CPU management     : Normal automatic tuning\n'
        fi
        if [[ -n "$PHYSICAL_MEMORY_MIB" ]]; then
            printf 'Detected memory    : Approximately %s GB (%s MiB)\n' "$PHYSICAL_MEMORY_GIB" "$PHYSICAL_MEMORY_MIB"
        else
            printf 'Detected memory    : Unable to detect\n'
        fi
        if [[ -n "$PHYSICAL_CORE_COUNT" ]]; then
            printf 'Physical CPU cores : %s\n' "$PHYSICAL_CORE_COUNT"
        else
            printf 'Physical CPU cores : Unable to detect\n'
        fi
        printf 'Logical processors : %s\n' "$LOGICAL_PROCESSOR_COUNT"
        if [[ "$OLD_CPU_MODE" == "Yes" ]]; then
            printf 'CPU instructions   : AVX disabled for older CPU compatibility\n'
        else
            printf 'CPU instructions   : Automatic\n'
        fi
        if [[ "$LARGE_PAGES_ENABLED" == "Yes" ]]; then
            printf 'Large Pages        : Enabled\n'
        else
            printf 'Large Pages        : Disabled\n'
        fi
        printf 'Huge page support  : %s\n' "$HUGE_PAGES_STATUS"
        printf 'Logging            : %s\n' "$LOGGING_MODE"
        printf 'Fast Rendering     : %s\n' "$FAST_RENDERING_STATUS"
        [[ "$FAST_RENDERING_STATUS" == Enabled ]] &&
            printf 'FR Resource Cache  : %s\n' "$RESOURCE_CACHE_STATUS"
        if [[ "$VRAM_OPTIMIZER_AVAILABLE" == Yes ]]; then
            printf 'VRAM Optimizer     : Installed\n'
        else
            printf 'VRAM Optimizer     : Recommended (not installed)\n'
        fi
        if [[ -n "$BACKGROUND_LABEL" ]]; then
            printf 'Launcher background: %s\n' "$BACKGROUND_LABEL"
        fi
    } > "$INFO_PENDING"
    [[ -s "$INFO_PENDING" ]]
}

write_logging() {
    {
        if [[ "$LOGGING_MODE" == "Full" ]]; then
            printf 'log4j.rootLogger=INFO, ConsoleAppender, file\n'
        else
            printf 'log4j.rootLogger=INFO, file\n'
            printf '\n'
            printf '# Suppress extremely verbose routine resource-loading messages\n'
            printf 'log4j.logger.com.fs.starfarer.loading=WARN\n'
            printf 'log4j.logger.com.genir.renderer.overrides.loading=WARN\n'
            printf 'log4j.logger.com.fs.starfarer.campaign.rules.Rules=WARN\n'
        fi
        printf '\n'
        printf '#log4j.throwableRenderer=com.fs.starfarer.log.CustomLogj4ExceptionLogger\n'
        printf '\n'
        printf '# Console appender\n'
        printf 'log4j.appender.ConsoleAppender=org.apache.log4j.ConsoleAppender\n'
        printf 'log4j.appender.ConsoleAppender.layout=org.apache.log4j.PatternLayout\n'
        printf 'log4j.appender.ConsoleAppender.layout.ConversionPattern=%%-4r [%%t] %%-5p %%c %%x - %%m%%n\n'
        printf '\n'
        printf '# Rolling file appender\n'
        printf 'log4j.appender.file=org.apache.log4j.RollingFileAppender\n'
        printf 'log4j.appender.file.File=${com.fs.starfarer.settings.paths.logs}/starsector.log\n'
        printf 'log4j.appender.file.layout=org.apache.log4j.PatternLayout\n'
        printf 'log4j.appender.file.layout.ConversionPattern=%%-4r [%%t] %%-5p %%c %%x - %%m%%n\n'
        printf 'log4j.appender.file.MaxFileSize=50000KB\n'
        printf 'log4j.appender.file.MaxBackupIndex=3\n'
    } > "$LOGGING_PENDING"
    [[ -s "$LOGGING_PENDING" ]]
}

write_launcher() {
    local java_command
    if [[ "$SELECTED_JAVA" == "$GAME_ROOT/"* ]]; then
        java_command="./${SELECTED_JAVA#"$GAME_ROOT/"}"
    else
        java_command="$SELECTED_JAVA"
    fi
    {
        printf '%s\n' '#!/usr/bin/env bash'
        printf '%s\n' 'set -euo pipefail'
        printf '%s\n' 'cd -- "$(dirname -- "${BASH_SOURCE[0]}")"'
        printf 'exec %q @Miko_Simple.txt "$@"\n' "$java_command"
    } > "$LAUNCHER_PENDING"
    chmod 755 "$LAUNCHER_PENDING" 2>/dev/null || true
    [[ -s "$LAUNCHER_PENDING" ]]
}

write_background_pending() {
    [[ -n "$BACKGROUND_SOURCE" ]] || return 0
    [[ -f "$BACKGROUND_SOURCE" ]] || { warn "Background source is missing: $BACKGROUND_SOURCE"; return 1; }
    cp -- "$BACKGROUND_SOURCE" "$BG_PENDING" || return 1
    [[ -s "$BG_PENDING" ]]
}
reset_pending_paths() {
    cleanup_pending_files
    SIMPLE_PENDING="$GAME_ROOT/Miko_Simple.${TRANSACTION_ID}.pending"
    LAUNCHER_PENDING="$GAME_ROOT/Miko_Rouge.${TRANSACTION_ID}.pending"
    LOGGING_PENDING="$MIKOHIME_DIR/mikohime.properties.${TRANSACTION_ID}.pending"
    INFO_PENDING="$GAME_ROOT/Miko_Info.${TRANSACTION_ID}.pending"
    BG_PENDING="$MIKOHIME_DIR/launcher_bg.jpg.${TRANSACTION_ID}.pending"
    register_temp_file "$SIMPLE_PENDING"
    register_temp_file "$SIMPLE_PENDING.tmp"
    register_temp_file "$LAUNCHER_PENDING"
    register_temp_file "$LOGGING_PENDING"
    register_temp_file "$INFO_PENDING"
    register_temp_file "$BG_PENDING"
}

cleanup_pending_files() {
    [[ -n "$SIMPLE_PENDING" ]] && rm -f -- "$SIMPLE_PENDING" "$SIMPLE_PENDING.tmp" 2>/dev/null
    [[ -n "$LAUNCHER_PENDING" ]] && rm -f -- "$LAUNCHER_PENDING" 2>/dev/null
    [[ -n "$LOGGING_PENDING" ]] && rm -f -- "$LOGGING_PENDING" 2>/dev/null
    [[ -n "$INFO_PENDING" ]] && rm -f -- "$INFO_PENDING" 2>/dev/null
    [[ -n "$BG_PENDING" ]] && rm -f -- "$BG_PENDING" 2>/dev/null
    return 0
}

validate_agent_order() {
    local cpline rc="" fr=""
    cpline="$(grep -nE '^-classpath ' "$SIMPLE_PENDING" | head -n1 | cut -d: -f1)"
    [[ -n "$cpline" ]] || return 1
    if [[ "$RESOURCE_CACHE_STATUS" == Enabled ]]; then
        rc="$(grep -nF -- '-javaagent:fr-resource-cache-agent.jar' "$SIMPLE_PENDING" | head -n1 | cut -d: -f1)"
        [[ -n "$rc" && "$rc" -lt "$cpline" ]] || return 1
    fi
    if [[ "$FAST_RENDERING_STATUS" == Enabled ]]; then
        fr="$(grep -nFx -- '-javaagent:fr.agent.jar' "$SIMPLE_PENDING" | head -n1 | cut -d: -f1)"
        [[ -n "$fr" && "$fr" -lt "$cpline" ]] || return 1
        [[ -z "$rc" || "$rc" -lt "$fr" ]] || return 1
    fi
    return 0
}

validate_pending_files() {
    local f
    for f in "$SIMPLE_PENDING" "$LAUNCHER_PENDING" "$LOGGING_PENDING" "$INFO_PENDING"; do
        [[ -s "$f" ]] || { warn "A generated file is missing or empty."; return 1; }
    done
    grep -Fxq 'com.fs.starfarer.StarfarerLauncher' "$SIMPLE_PENDING" || return 1
    grep -Eq '^-Xms[0-9]+m$' "$SIMPLE_PENDING" || return 1
    grep -Eq '^-Xmx[0-9]+m$' "$SIMPLE_PENDING" || return 1
    grep -Eq '^-classpath ' "$SIMPLE_PENDING" || return 1
    grep -Fq -- '@Miko_Simple.txt' "$LAUNCHER_PENDING" || return 1
    grep -Fxq 'log4j.appender.file.MaxBackupIndex=3' "$LOGGING_PENDING" || return 1

    # The launcher class must be the final non-empty line.
    [[ "$(awk 'NF{last=$0} END{print last}' "$SIMPLE_PENDING")" == 'com.fs.starfarer.StarfarerLauncher' ]] || return 1

    # The classpath must use the Linux ':' separator, never ';'.
    local cp
    cp="$(grep -E '^-classpath ' "$SIMPLE_PENDING" | head -n1)"
    [[ "$cp" == *';'* ]] && return 1

    # FR Resource Cache cross-file invariant.
    if [[ "$RESOURCE_CACHE_STATUS" == Enabled ]]; then
        [[ "$FAST_RENDERING_STATUS" == Enabled ]] || return 1
        [[ -s "$GAME_ROOT/fr-resource-cache-agent.jar" ]] || return 1
        grep -Fq -- '-javaagent:fr-resource-cache-agent.jar' "$SIMPLE_PENDING" || return 1
    else
        grep -Fq -- '-javaagent:fr-resource-cache-agent.jar' "$SIMPLE_PENDING" && return 1
    fi

    validate_agent_order || return 1

    if [[ -n "$BACKGROUND_SOURCE" ]]; then
        [[ -s "$BG_PENDING" ]] || return 1
    fi
    return 0
}

commit_files() {
    # Atomically install every (target, pending) pair. Existing targets are
    # backed up first; on any failure all changes are rolled back and backups
    # are restored. Backups are removed only after the whole set commits.
    local suffix=".configure_backup_${TRANSACTION_ID}"
    local -a c_backup=() c_had=()
    local i j committed=-1 target pending
    for i in "${!COMMIT_TARGETS[@]}"; do
        target="${COMMIT_TARGETS[$i]}"
        c_backup[$i]="${target}${suffix}"
        c_had[$i]="No"
        if [[ -e "$target" ]]; then
            if ! cp -p -- "$target" "${c_backup[$i]}" 2>/dev/null; then
                for j in "${!c_backup[@]}"; do rm -f -- "${c_backup[$j]}" 2>/dev/null; done
                return 1
            fi
            c_had[$i]="Yes"
        fi
    done
    for i in "${!COMMIT_TARGETS[@]}"; do
        target="${COMMIT_TARGETS[$i]}"
        pending="${COMMIT_PENDINGS[$i]}"
        if mv -f -- "$pending" "$target" 2>/dev/null; then
            committed=$i
        else
            for j in "${!COMMIT_TARGETS[@]}"; do
                if [[ "${c_had[$j]}" == "Yes" ]]; then
                    [[ -e "${c_backup[$j]}" ]] && mv -f -- "${c_backup[$j]}" "${COMMIT_TARGETS[$j]}" 2>/dev/null
                elif (( j <= committed )); then
                    rm -f -- "${COMMIT_TARGETS[$j]}" 2>/dev/null
                fi
            done
            for j in "${!c_backup[@]}"; do rm -f -- "${c_backup[$j]}" 2>/dev/null; done
            return 1
        fi
    done
    for i in "${!c_backup[@]}"; do rm -f -- "${c_backup[$i]}" 2>/dev/null; done
    return 0
}

generate_configuration() {
    reset_pending_paths
    write_info || { warn "Unable to write the configuration summary."; return 1; }
    if [[ "$JAVA_VERSION" == 27 ]]; then
        write_simple_java27 || return 1
    else
        write_simple_modern || return 1
    fi
    write_logging || return 1
    write_launcher || return 1
    if [[ -n "$BACKGROUND_SOURCE" ]]; then
        write_background_pending || return 1
    fi
    validate_pending_files || { warn "The generated configuration failed validation."; return 1; }

    COMMIT_TARGETS=(
        "$GAME_ROOT/Miko_Simple.txt"
        "$GAME_ROOT/Miko_Rouge.sh"
        "$MIKOHIME_DIR/mikohime.properties"
        "$GAME_ROOT/Miko_Info.txt"
    )
    COMMIT_PENDINGS=(
        "$SIMPLE_PENDING"
        "$LAUNCHER_PENDING"
        "$LOGGING_PENDING"
        "$INFO_PENDING"
    )
    if [[ -n "$BACKGROUND_SOURCE" ]]; then
        COMMIT_TARGETS+=("$MIKOHIME_DIR/launcher_bg.jpg")
        COMMIT_PENDINGS+=("$BG_PENDING")
    fi
    commit_files || { warn "Unable to install the new configuration; previous files were restored."; return 1; }
    return 0
}
# -----------------------------------------------------------------------------
# Selection reset and non-interactive (MIKO_*) resolvers
# -----------------------------------------------------------------------------
reset_selections() {
    SELECTED_JAVA=""
    JAVA_VERSION=""
    HEAP_MIB=""
    HEAP_DESCRIPTION=""
    FAST_RENDERING_STATUS="Disabled"
    RESOURCE_CACHE_STATUS="Disabled"
    LOW_CORE_MODE="No"
    OLD_CPU_MODE="No"
    LARGE_PAGES_ENABLED="No"
    LOGGING_MODE="Full"
    BACKGROUND_SOURCE=""
    BACKGROUND_LABEL=""
}

recommend_heap() {
    if [[ -n "$PHYSICAL_MEMORY_MIB" ]] && (( PHYSICAL_MEMORY_MIB >= 29000 )); then
        printf '11264'
    elif [[ -n "$PHYSICAL_MEMORY_MIB" ]] && (( PHYSICAL_MEMORY_MIB >= 14000 )); then
        printf '8192'
    else
        printf '4096'
    fi
}

describe_heap_value() {
    # Map a MiB value back to a preset description, else label it custom.
    local mib="$1" idx
    for idx in "${HEAP_ORDER[@]}"; do
        if [[ "${HEAP_VALUE[$idx]}" == "$mib" ]]; then
            printf '%s' "${HEAP_DESC[$idx]}"
            return 0
        fi
    done
    printf '%s MB (custom)' "$mib"
}

resolve_java_noninteractive() {
    if [[ -n "${MIKO_JAVA:-}" ]]; then
        local exe="$MIKO_JAVA" major
        [[ "$exe" == *$'\n'* ]] && die "MIKO_JAVA must not contain newlines."
        if [[ -d "$exe" && -x "$exe/bin/java" ]]; then
            exe="$exe/bin/java"
        fi
        [[ -x "$exe" && -f "$exe" ]] || die "MIKO_JAVA is not an executable Java binary: $MIKO_JAVA"
        major="$(java_major "$exe")"
        is_supported_major "$major" || die "MIKO_JAVA is not a supported Java 17, 27, or 28: $MIKO_JAVA"
        SELECTED_JAVA="$(cd -- "$(dirname -- "$exe")" && printf '%s/%s' "$PWD" "$(basename -- "$exe")")" ||
            die "Unable to resolve the Java executable path: $MIKO_JAVA"
        JAVA_VERSION="$major"
        return 0
    fi
    (( ${#JAVA_PATHS[@]} > 0 )) || die "No supported Java 17, 27, or 28 installation was found."
    local i chosen=0
    if [[ -n "${MIKO_JAVA_INDEX:-}" ]]; then
        [[ "$MIKO_JAVA_INDEX" =~ ^[0-9]+$ ]] && (( MIKO_JAVA_INDEX >= 1 && MIKO_JAVA_INDEX <= ${#JAVA_PATHS[@]} )) ||
            die "MIKO_JAVA_INDEX is out of range."
        chosen=$((MIKO_JAVA_INDEX - 1))
    else
        for i in "${!JAVA_VERSIONS[@]}"; do
            if [[ "${JAVA_VERSIONS[$i]}" == 27 || "${JAVA_VERSIONS[$i]}" == 28 ]]; then
                chosen=$i
                break
            fi
        done
    fi
    SELECTED_JAVA="${JAVA_PATHS[$chosen]}"
    JAVA_VERSION="${JAVA_VERSIONS[$chosen]}"
}

resolve_memory_noninteractive() {
    local rec value
    rec="$(recommend_heap)"
    if [[ -n "${MIKO_HEAP_MIB:-}" ]]; then
        HEAP_MIB="$MIKO_HEAP_MIB"
    elif [[ -n "${MIKO_HEAP_PRESET:-}" ]]; then
        value="${HEAP_VALUE[${MIKO_HEAP_PRESET}]:-}"
        [[ -n "$value" ]] || die "MIKO_HEAP_PRESET is not a known preset index."
        HEAP_MIB="$value"
    else
        HEAP_MIB="$rec"
    fi
    is_positive_integer "$HEAP_MIB" || die "Memory must be a positive integer of MiB."
    (( HEAP_MIB >= 512 && HEAP_MIB <= 1048576 )) ||
        die "Memory must be between 512 and 1048576 MiB."
    HEAP_DESCRIPTION="$(describe_heap_value "$HEAP_MIB")"
    if [[ -n "$SAFE_HEAP_MIB" ]] && (( HEAP_MIB > SAFE_HEAP_MIB )); then
        warn "Selected heap ${HEAP_MIB} MiB exceeds 75% of physical RAM (${SAFE_HEAP_MIB} MiB). Proceeding as requested."
    fi
}

resolve_components_noninteractive() {
    FAST_RENDERING_STATUS="Disabled"
    RESOURCE_CACHE_STATUS="Disabled"
    if [[ "$FAST_RENDERING_AVAILABLE" == Yes && "${MIKO_FAST_RENDERING:-1}" != 0 ]]; then
        FAST_RENDERING_STATUS="Enabled"
    fi
    if [[ "$FAST_RENDERING_STATUS" == Enabled && "$RESOURCE_CACHE_AVAILABLE" == Yes && "${MIKO_RESOURCE_CACHE:-0}" == 1 ]]; then
        RESOURCE_CACHE_STATUS="Enabled"
    fi

}

resolve_cpu_noninteractive() {
    local rec="No"
    (( LOGICAL_PROCESSOR_COUNT <= 4 )) && rec="Yes"
    case "${MIKO_LOW_CORE:-auto}" in
        yes|Yes|YES) LOW_CORE_MODE="Yes" ;;
        no|No|NO)    LOW_CORE_MODE="No" ;;
        *)           LOW_CORE_MODE="$rec" ;;
    esac
    OLD_CPU_MODE="No"
    if [[ "$LOW_CORE_MODE" == Yes ]]; then
        case "${MIKO_OLD_CPU:-no}" in
            yes|Yes|YES) OLD_CPU_MODE="Yes" ;;
        esac
    fi
}

resolve_system_noninteractive() {
    LARGE_PAGES_ENABLED="No"
    case "${MIKO_LARGE_PAGES:-no}" in
        yes|Yes|YES)
            if [[ "$HUGE_PAGES_CAPABLE" == Yes ]]; then
                LARGE_PAGES_ENABLED="Yes"
            else
                warn "Large Pages requested but not usable: $HUGE_PAGES_STATUS"
            fi
            ;;
    esac
}

resolve_logging_noninteractive() {
    case "${MIKO_LOGGING:-Full}" in
        Full|full)       LOGGING_MODE="Full" ;;
        Reduced|reduced) LOGGING_MODE="Reduced" ;;
        Minimal|minimal) LOGGING_MODE="Minimal" ;;
        *) die "MIKO_LOGGING must be Full, Reduced, or Minimal." ;;
    esac
}

resolve_background_noninteractive() {
    BACKGROUND_SOURCE=""
    BACKGROUND_LABEL=""
    case "${MIKO_BACKGROUND:-}" in
        ''|keep|Keep) : ;;
        default|Default)       BACKGROUND_SOURCE="$BG_DIR/default_bg.jpg"; BACKGROUND_LABEL="Default Mikohime 25+" ;;
        mikosector|pather)     BACKGROUND_SOURCE="$BG_DIR/pather_bg.jpg";  BACKGROUND_LABEL="Mikosector" ;;
        mimikko|Mimikko)       BACKGROUND_SOURCE="$BG_DIR/mimikko_bg.jpg"; BACKGROUND_LABEL="Mimikko" ;;
        gamma|Gamma)           BACKGROUND_SOURCE="$BG_DIR/gamma_bg.jpg";   BACKGROUND_LABEL="Gamma" ;;
        *) die "MIKO_BACKGROUND must be keep, default, mikosector, mimikko, or gamma." ;;
    esac
}

run_noninteractive() {
    refresh_environment
    reset_selections
    resolve_java_noninteractive
    resolve_memory_noninteractive
    resolve_components_noninteractive
    resolve_cpu_noninteractive
    resolve_system_noninteractive
    resolve_logging_noninteractive
    resolve_background_noninteractive
    generate_configuration || die "Configuration generation failed; existing files were left unchanged."
    printf '%sConfiguration completed successfully.%s\n' "${ColorGreen:-}" "${ColorReset:-}"
    printf 'Created Miko_Simple.txt, Miko_Rouge.sh, Miko_Info.txt, and mikohime/mikohime.properties.\n'
    [[ -n "$BACKGROUND_SOURCE" ]] && printf 'Updated mikohime/launcher_bg.jpg (%s).\n' "$BACKGROUND_LABEL"
    return 0
}
# -----------------------------------------------------------------------------
# Interactive user interface
# -----------------------------------------------------------------------------
REPLY_LINE=""
prompt_line() {
    local __p="$1" __v=""
    read -r -p "$__p" __v || __v=""
    REPLY_LINE="$__v"
}

print_header() {
    printf '==============================================================================\n'
    printf '                          Mikohime Java Configurator\n'
    printf '   Currently maintained by Yue - Unofficial Configure_Me.sh by Gaius Cassius\n'
    printf '==============================================================================\n'
}

print_detected_environment() {
    printf 'Detected environment:\n'
    printf '  Java installations : %s\n' "${#JAVA_PATHS[@]}"
    if [[ "$MODERN_JAVA_AVAILABLE" == Yes ]]; then
        printf '  Java 27 or 28      : %sInstalled%s\n' "${ColorGreen:-}" "${ColorReset:-}"
    else
        printf '  Java 27 or 28      : %sNot found%s (select J below to install)\n' "${ColorYellow:-}" "${ColorReset:-}"
    fi
    if [[ "$FAST_RENDERING_DETECTED" == Yes ]]; then
        if [[ "$FAST_RENDERING_AVAILABLE" == Yes ]]; then
            printf '  Fast Rendering     : %sInstalled%s\n' "${ColorGreen:-}" "${ColorReset:-}"
        fi
    else
        printf '  Fast Rendering     : %sLinux fork not found%s (%s)\n' "${ColorYellow:-}" "${ColorReset:-}" "$FastRenderingReleasesUrl"
    fi
    if [[ "$RESOURCE_CACHE_DETECTED" == Yes ]]; then
        if [[ "$RESOURCE_CACHE_AVAILABLE" == Yes ]]; then
            printf '  FR Resource Cache  : %sInstalled%s\n' "${ColorGreen:-}" "${ColorReset:-}"
        else
            printf '  FR Resource Cache  : %sDetected but requires Fast Rendering%s\n' "${ColorYellow:-}" "${ColorReset:-}"
        fi
    else
        printf '  FR Resource Cache  : %sUnavailable without Fast Rendering%s\n' "${ColorYellow:-}" "${ColorReset:-}"
    fi
    if [[ "$VRAM_OPTIMIZER_AVAILABLE" == Yes ]]; then
        printf '  VRAM Optimizer     : %sInstalled%s\n' "${ColorGreen:-}" "${ColorReset:-}"
    else
        printf '  VRAM Optimizer     : %sRecommended%s (%s)\n' "${ColorYellow:-}" "${ColorReset:-}" "$VramOptimizerReleasesUrl"
    fi
    if [[ -n "$PHYSICAL_MEMORY_MIB" ]]; then
        printf '  Physical memory    : Approximately %s GB\n' "$PHYSICAL_MEMORY_GIB"
    else
        printf '  Physical memory    : %sUnable to detect%s\n' "${ColorYellow:-}" "${ColorReset:-}"
    fi
    if [[ -n "$PHYSICAL_CORE_COUNT" ]]; then
        printf '  Physical CPU cores : %s\n' "$PHYSICAL_CORE_COUNT"
    else
        printf '  Physical CPU cores : %sUnable to detect%s\n' "${ColorYellow:-}" "${ColorReset:-}"
    fi
    printf '  Logical processors : %s\n' "$LOGICAL_PROCESSOR_COUNT"
    printf '  Huge page support  : %s\n' "$HUGE_PAGES_STATUS"
}

missing_download_available() {
    [[ "$FAST_RENDERING_AVAILABLE" == No ]] && return 0
    [[ "$VRAM_OPTIMIZER_AVAILABLE" == No ]] && return 0
    return 1
}

open_missing_download_page() {
    printf '\nOpen a download page:\n'
    local -a labels=() urls=()
    [[ "$FAST_RENDERING_AVAILABLE" == No ]] && { labels+=("Fast Rendering Linux fork"); urls+=("$FastRenderingReleasesUrl"); }
    [[ "$VRAM_OPTIMIZER_AVAILABLE" == No ]] && { labels+=("VRAM Optimizer"); urls+=("$VramOptimizerReleasesUrl"); }
    local i
    for i in "${!labels[@]}"; do
        printf '  %d. %s - %s\n' "$((i + 1))" "${labels[$i]}" "${urls[$i]}"
    done
    printf '  B. Back\n'
    prompt_line "Select a page to open: "
    case "$REPLY_LINE" in
        [Bb]|'') return 0 ;;
    esac
    [[ "$REPLY_LINE" =~ ^[0-9]+$ ]] && (( REPLY_LINE >= 1 && REPLY_LINE <= ${#urls[@]} )) || {
        warn "Invalid selection."
        return 0
    }
    open_url "${urls[$((REPLY_LINE - 1))]}" || true
    return 0
}

manual_java_download_menu() {
    local requested="$1" prefix url expected folder
    case "$requested" in
        27) prefix=Jdk27 ;;
        28) prefix=Jdk28 ;;
        *) return 1 ;;
    esac
    url="$(read_property "${prefix}LinuxUrl" "$SHARED_DIR/java-versions.properties")"
    expected="$(read_property "${prefix}LinuxSha256" "$SHARED_DIR/java-versions.properties")"
    folder="$(read_property "${prefix}LinuxFolder" "$SHARED_DIR/java-versions.properties")"
    printf '\nManual Java %s installation:\n' "$requested"
    printf '  Download : %s\n' "$url"
    printf '  SHA-256  : %s\n' "$expected"
    printf '  Extract the JDK as: %s/%s\n' "$GAME_ROOT" "$folder"
    printf '  Then return to the main menu and refresh detected components.\n'
    printf '  O. Open the download URL\n  B. Return to the main menu\n'
    prompt_line "Select an option: "
    [[ "$REPLY_LINE" =~ ^[Oo]$ ]] && open_url "$url" || true
    return 0
}

manual_resource_cache_menu() {
    printf '\nManual FR Resource Cache installation:\n'
    printf '  Download the latest release from: %s\n' "$ResourceCacheReleasesUrl"
    printf '  Extract fr-resource-cache-agent.jar beside starsector.sh:\n'
    printf '    %s/fr-resource-cache-agent.jar\n' "$GAME_ROOT"
    printf '  Then return to the main menu and refresh detected components.\n'
    printf '  O. Open the release page\n  B. Return to the main menu\n'
    prompt_line "Select an option: "
    [[ "$REPLY_LINE" =~ ^[Oo]$ ]] && open_url "$ResourceCacheReleasesUrl" || true
    return 0
}

install_java_interactive() {
    local requested="$1"
    if (
        LOCK_OWNED=0
        OWNED_TEMP_FILES=()
        OWNED_TEMP_DIRS=()
        trap cleanup EXIT
        install_java "$requested"
    ); then
        return 0
    fi
    warn "Automatic Java $requested installation failed."
    manual_java_download_menu "$requested"
    return 1
}

install_resource_cache_interactive() {
    if (
        LOCK_OWNED=0
        OWNED_TEMP_FILES=()
        OWNED_TEMP_DIRS=()
        trap cleanup EXIT
        install_resource_cache
    ); then
        return 0
    fi
    warn "Automatic FR Resource Cache installation failed."
    manual_resource_cache_menu
    return 1
}

install_java_menu() {
    printf '\nDownload and install Java (extracted beside starsector.sh; no system install)\n'
    printf '  1. Java 27 (%s)\n' "$(read_property Jdk27LinuxVersion "$SHARED_DIR/java-versions.properties")"
    printf '  2. Java 28 (%s)\n' "$(read_property Jdk28LinuxVersion "$SHARED_DIR/java-versions.properties")"
    printf '  B. Back\n'
    prompt_line "Select an option: "
    case "$REPLY_LINE" in
        1) install_java_interactive 27 || true ;;
        2) install_java_interactive 28 || true ;;
        *) return 0 ;;
    esac
    refresh_environment
    prompt_line "Press Enter to continue..."
    return 0
}

# --- Step 1: Java -----------------------------------------------------------
choose_java() {
    while true; do
        printf '\nStep 1 of 7 - Java\n'
        printf -- '--------------------------------------------------------------------------\n'
        if (( ${#JAVA_PATHS[@]} == 0 )); then
            printf 'No supported Java installations were detected automatically.\n'
        else
            local i
            for i in "${!JAVA_PATHS[@]}"; do
                printf '  %d. %s - %s\n' "$((i + 1))" "${JAVA_LABELS[$i]}" "${JAVA_PATHS[$i]}"
            done
        fi
        printf '  C. Specify a Java folder or executable manually\n'
        printf '  X. Cancel and return to the main menu\n'
        prompt_line "Select an option: "
        case "$REPLY_LINE" in
            [Xx]) return 2 ;;
            [Cc]) if choose_custom_java; then return 0; fi ;;
            *)
                if [[ "$REPLY_LINE" =~ ^[0-9]+$ ]] && (( REPLY_LINE >= 1 && REPLY_LINE <= ${#JAVA_PATHS[@]} )); then
                    SELECTED_JAVA="${JAVA_PATHS[$((REPLY_LINE - 1))]}"
                    JAVA_VERSION="${JAVA_VERSIONS[$((REPLY_LINE - 1))]}"
                    return 0
                fi
                warn "Invalid selection."
                ;;
        esac
    done
}

choose_custom_java() {
    printf 'Enter a Java folder (containing bin/java) or a java executable path. Enter X to cancel.\n'
    prompt_line "Path: "
    local input="$REPLY_LINE" exe major
    [[ "$input" == [Xx] ]] && return 1
    [[ -n "$input" ]] || { warn "No path entered."; return 1; }
    [[ "$input" == *$'\n'* ]] && { warn "Paths must not contain newlines."; return 1; }
    exe="$input"
    if [[ -d "$exe" && -x "$exe/bin/java" ]]; then
        exe="$exe/bin/java"
    fi
    [[ -x "$exe" && -f "$exe" ]] || { warn "No usable java executable at that path."; return 1; }
    major="$(java_major "$exe")"
    if ! is_supported_major "$major"; then
        warn "Java $major is not supported. Supported versions: 17, 27, 28."
        return 1
    fi
    SELECTED_JAVA="$(cd -- "$(dirname -- "$exe")" && printf '%s/%s' "$PWD" "$(basename -- "$exe")")" || {
        warn "Unable to resolve the Java executable path."
        return 1
    }
    JAVA_VERSION="$major"
    return 0
}
choose_rendering() {
    while true; do
        printf '\nStep 2 of 7 - Optional components\n'
        printf -- '--------------------------------------------------------------------------\n'
        if [[ "$FAST_RENDERING_AVAILABLE" != Yes ]]; then
            FAST_RENDERING_STATUS="Disabled"
            RESOURCE_CACHE_STATUS="Disabled"
            printf '%sFast Rendering was not found.%s\n' \
                "${ColorYellow:-}" "${ColorReset:-}"
            printf 'Download and extract fr-linux.zip beside starsector.sh:\n  %s\n' "$FastRenderingReleasesUrl"
            printf 'FR Resource Cache remains disabled until the Linux fork is installed.\n'
            printf '  C. Continue\n  D. Open Linux fork download page\n  B. Back\n  X. Cancel\n'
            prompt_line "Select an option: "
            case "$REPLY_LINE" in
                [Xx]) return 2 ;;
                [Bb]) return 1 ;;
                [Dd]) open_url "$FastRenderingReleasesUrl" || true ;;
                *) return 0 ;;
            esac
        fi
        printf 'Fast Rendering was detected. Enable it?\n'
        printf '  1. Yes\n  2. No\n  B. Back\n  X. Cancel\n'
        prompt_line "Select an option: "
        case "$REPLY_LINE" in
            [Xx]) return 2 ;;
            [Bb]) return 1 ;;
            2) FAST_RENDERING_STATUS="Disabled"; RESOURCE_CACHE_STATUS="Disabled"; return 0 ;;
            1) FAST_RENDERING_STATUS="Enabled" ;;
            *) warn "Invalid selection."; continue ;;
        esac
        # Resource cache
        if [[ "$RESOURCE_CACHE_AVAILABLE" != Yes ]]; then
            RESOURCE_CACHE_STATUS="Disabled"
            printf '%sFR Resource Cache was not found and will remain disabled.%s\n' "${ColorYellow:-}" "${ColorReset:-}"
            printf '  C. Continue\n  R. Download and install FR Resource Cache\n  D. Open download page\n'
            prompt_line "Select an option: "
            case "$REPLY_LINE" in
                [Rr])
                    if install_resource_cache_interactive; then
                        refresh_environment
                        [[ "$RESOURCE_CACHE_AVAILABLE" == Yes ]] && continue
                    else
                        return 2
                    fi
                    ;;
                [Dd]) open_url "$ResourceCacheReleasesUrl" || true ;;
            esac
            return 0
        fi
        printf 'FR Resource Cache was detected. Enable it?\n  1. Yes\n  2. No\n'
        prompt_line "Select an option: "
        case "$REPLY_LINE" in
            1) RESOURCE_CACHE_STATUS="Enabled" ;;
            *) RESOURCE_CACHE_STATUS="Disabled" ;;
        esac
        return 0
    done
}

# --- Step 3: Memory ---------------------------------------------------------
confirm_memory_selection() {
    [[ -n "$SAFE_HEAP_MIB" ]] || return 0
    (( HEAP_MIB > SAFE_HEAP_MIB )) || return 0
    printf '%sWARNING: %s MiB exceeds the recommended maximum of %s MiB (75%% of %s MiB physical RAM).%s\n' \
        "${ColorYellow:-}" "$HEAP_MIB" "$SAFE_HEAP_MIB" "$PHYSICAL_MEMORY_MIB" "${ColorReset:-}"
    printf '  1. Choose a different amount\n  2. Use this amount anyway\n'
    prompt_line "Select an option: "
    [[ "$REPLY_LINE" == 2 ]] && return 0
    return 1
}

choose_memory() {
    local rec; rec="$(recommend_heap)"
    while true; do
        printf '\nStep 3 of 7 - Memory\n'
        printf -- '--------------------------------------------------------------------------\n'
        if [[ -n "$PHYSICAL_MEMORY_MIB" ]]; then
            printf 'Detected physical memory: approximately %s GB (recommended heap %s MiB)\n' "$PHYSICAL_MEMORY_GIB" "$rec"
        else
            printf 'Physical memory could not be detected (recommended heap %s MiB)\n' "$rec"
        fi
        local idx
        for idx in 1 2 3 4 5 6; do
            printf '  %s. %s\n' "$idx" "${HEAP_DESC[$idx]}"
        done
        printf '  A. Advanced (11-26 GB)   E. Extreme (64/128 GB)   C. Custom MiB\n'
        printf '  B. Back                  X. Cancel\n'
        prompt_line "Select an option: "
        case "$REPLY_LINE" in
            [Xx]) return 2 ;;
            [Bb]) return 1 ;;
            [Aa]) choose_preset_group 11 12 13 14 && confirm_memory_selection && return 0 ;;
            [Ee]) choose_preset_group 15 16 && confirm_memory_selection && return 0 ;;
            [Cc]) if choose_custom_memory; then confirm_memory_selection && return 0; fi ;;
            1|2|3|4|5|6)
                HEAP_MIB="${HEAP_VALUE[$REPLY_LINE]}"
                HEAP_DESCRIPTION="${HEAP_DESC[$REPLY_LINE]}"
                confirm_memory_selection && return 0
                ;;
            *) warn "Invalid selection." ;;
        esac
    done
}

choose_preset_group() {
    local -a group=("$@") idx pos=1
    printf '\n'
    for idx in "${group[@]}"; do
        printf '  %d. %s\n' "$pos" "${HEAP_DESC[$idx]}"
        (( pos++ ))
    done
    printf '  B. Back\n'
    prompt_line "Select an option: "
    [[ "$REPLY_LINE" =~ ^[0-9]+$ ]] && (( REPLY_LINE >= 1 && REPLY_LINE <= ${#group[@]} )) || return 1
    idx="${group[$((REPLY_LINE - 1))]}"
    HEAP_MIB="${HEAP_VALUE[$idx]}"
    HEAP_DESCRIPTION="${HEAP_DESC[$idx]}"
    return 0
}

choose_custom_memory() {
    printf 'Enter the memory limit in MiB (512 through 1048576). Enter B to go back.\n'
    prompt_line "Memory in MiB: "
    [[ "$REPLY_LINE" == [Bb] ]] && return 1
    if ! is_positive_integer "$REPLY_LINE" || (( REPLY_LINE < 512 || REPLY_LINE > 1048576 )); then
        warn "Invalid amount. Enter a whole number from 512 through 1048576."
        return 1
    fi
    HEAP_MIB="$REPLY_LINE"
    HEAP_DESCRIPTION="$REPLY_LINE MB (custom)"
    return 0
}

# --- Step 4: CPU ------------------------------------------------------------
choose_cpu() {
    local rec="No"
    (( LOGICAL_PROCESSOR_COUNT <= 4 )) && rec="Yes"
    while true; do
        printf '\nStep 4 of 7 - CPU tuning\n'
        printf -- '--------------------------------------------------------------------------\n'
        if [[ -n "$PHYSICAL_CORE_COUNT" ]]; then
            printf 'Detected physical CPU cores: %s\n' "$PHYSICAL_CORE_COUNT"
        else
            printf 'Detected physical CPU cores: Unable to detect\n'
        fi
        printf 'Available logical processors: %s\n' "$LOGICAL_PROCESSOR_COUNT"
        printf 'Recommended low-core tuning: %s\n' "$rec"
        printf '  1. Use the recommended setting\n  2. Override the recommendation\n  B. Back\n  X. Cancel\n'
        prompt_line "Select an option: "
        case "$REPLY_LINE" in
            [Xx]) return 2 ;;
            [Bb]) return 1 ;;
            2) [[ "$rec" == Yes ]] && LOW_CORE_MODE="No" || LOW_CORE_MODE="Yes" ;;
            *) LOW_CORE_MODE="$rec" ;;
        esac
        OLD_CPU_MODE="No"
        if [[ "$LOW_CORE_MODE" != Yes ]]; then
            return 0
        fi
        printf 'Does this system use an older CPU that fails with error 0xc000001d (missing AVX)?\n'
        printf '  1. No\n  2. Yes\n  B. Back\n  X. Cancel\n'
        prompt_line "Select an option: "
        case "$REPLY_LINE" in
            [Xx]) return 2 ;;
            [Bb]) continue ;;
            2) OLD_CPU_MODE="Yes"; return 0 ;;
            *) OLD_CPU_MODE="No"; return 0 ;;
        esac
    done
}

# --- Step 5: System (huge pages / large pages) ------------------------------
choose_system() {
    while true; do
        printf '\nStep 5 of 7 - Linux memory options\n'
        printf -- '--------------------------------------------------------------------------\n'
        printf 'Huge page support: %s\n' "$HUGE_PAGES_STATUS"
        printf 'With -Xms equal to -Xmx and AlwaysPreTouch, the JVM reserves and commits the\n'
        printf 'whole heap at startup, so Large Pages needs enough locked huge-page memory.\n'
        printf '  1. Enable Large Pages\n  2. Disable Large Pages (recommended)\n  B. Back\n  X. Cancel\n'
        prompt_line "Select an option: "
        case "$REPLY_LINE" in
            [Xx]) return 2 ;;
            [Bb]) return 1 ;;
            2) LARGE_PAGES_ENABLED="No"; return 0 ;;
            1)
                if [[ "$HUGE_PAGES_CAPABLE" != Yes ]]; then
                    warn "Large Pages cannot be enabled: $HUGE_PAGES_STATUS"
                    continue
                fi
                LARGE_PAGES_ENABLED="Yes"
                return 0
                ;;
            *) warn "Invalid selection." ;;
        esac
    done
}

# --- Step 6: Logging --------------------------------------------------------
choose_logging() {
    printf '\nStep 6 of 7 - Logging\n'
    printf -- '--------------------------------------------------------------------------\n'
    printf '  1. Full console and file logging\n'
    printf '  2. Reduced routine console output; retain file logging\n'
    printf '  3. Reduced console output and disabled JVM diagnostics; retain file logging\n'
    printf '  B. Back\n  X. Cancel\n'
    prompt_line "Select an option: "
    case "$REPLY_LINE" in
        [Xx]) return 2 ;;
        [Bb]) return 1 ;;
        3) LOGGING_MODE="Minimal" ;;
        2) LOGGING_MODE="Reduced" ;;
        *) LOGGING_MODE="Full" ;;
    esac
    return 0
}

# --- Standalone background menu (atomic single-file commit) -----------------
background_menu() {
    printf '\nLauncher background\n'
    printf -- '--------------------------------------------------------------------------\n'
    printf '  1. Default Mikohime 25+\n  2. Mikosector\n  3. Mimikko\n  4. Gamma\n  B. Back\n'
    prompt_line "Select an option: "
    local src label
    case "$REPLY_LINE" in
        1) src="$BG_DIR/default_bg.jpg"; label="Default Mikohime 25+" ;;
        2) src="$BG_DIR/pather_bg.jpg"; label="Mikosector" ;;
        3) src="$BG_DIR/mimikko_bg.jpg"; label="Mimikko" ;;
        4) src="$BG_DIR/gamma_bg.jpg"; label="Gamma" ;;
        *) return 0 ;;
    esac
    [[ -f "$src" ]] || { warn "Background source is missing: $src"; return 0; }
    local pending="$MIKOHIME_DIR/launcher_bg.jpg.${TRANSACTION_ID}.pending"
    register_temp_file "$pending"
    cp -- "$src" "$pending" || { warn "Failed to stage the background."; return 0; }
    COMMIT_TARGETS=("$MIKOHIME_DIR/launcher_bg.jpg")
    COMMIT_PENDINGS=("$pending")
    if commit_files; then
        printf '%sInstalled the %s background.%s\n' "${ColorGreen:-}" "$label" "${ColorReset:-}"
    else
        warn "Failed to install the $label background."
    fi
    return 0
}
# -----------------------------------------------------------------------------
# Guided configuration flow (7 steps with back/cancel)
# -----------------------------------------------------------------------------
configure_interactive() {
    reset_selections
    local step=1 rc
    while true; do
        case "$step" in
            1)  # Java
                choose_java; rc=$?
                case "$rc" in 2) return 0 ;; 0) step=2 ;; esac
                ;;
            2)  # Optional rendering components
                choose_rendering; rc=$?
                case "$rc" in
                    2) return 0 ;;
                    1) step=1 ;;
                    0) step=3 ;;
                esac
                ;;
            3)  choose_memory; rc=$?
                case "$rc" in 2) return 0 ;; 1) step=2 ;; 0) step=4 ;; esac
                ;;
            4)  choose_cpu; rc=$?
                case "$rc" in 2) return 0 ;; 1) step=3 ;; 0) step=5 ;; esac
                ;;
            5)  choose_system; rc=$?
                case "$rc" in 2) return 0 ;; 1) step=4 ;; 0) step=6 ;; esac
                ;;
            6)  choose_logging; rc=$?
                case "$rc" in 2) return 0 ;; 1) step=5 ;; 0) step=7 ;; esac
                ;;
            7)  review_and_commit; rc=$?
                case "$rc" in
                    2) return 0 ;;          # cancel
                    1) step=6 ;;            # back to logging
                    3) step=1 ;;            # restart
                    0) return 0 ;;          # done
                esac
                ;;
        esac
    done
}

review_and_commit() {
    printf '\nStep 7 of 7 - Review\n'
    printf -- '--------------------------------------------------------------------------\n'
    printf 'Java              : %s (Java %s)\n' "$SELECTED_JAVA" "$JAVA_VERSION"
    printf 'Heap              : %s\n' "$HEAP_DESCRIPTION"
    printf 'Fast Rendering    : %s\n' "$FAST_RENDERING_STATUS"
    [[ "$FAST_RENDERING_STATUS" == Enabled ]] && printf 'FR Resource Cache : %s\n' "$RESOURCE_CACHE_STATUS"
    printf 'Low-core tuning   : %s\n' "$LOW_CORE_MODE"
    printf 'Old-CPU AVX off   : %s\n' "$OLD_CPU_MODE"
    printf 'Large Pages       : %s\n' "$LARGE_PAGES_ENABLED"
    printf 'Logging           : %s\n' "$LOGGING_MODE"
    printf -- '--------------------------------------------------------------------------\n'
    printf '  Y. Create the configuration\n  B. Back to logging\n  R. Restart\n  X. Cancel\n'
    prompt_line "Select an option: "
    case "$REPLY_LINE" in
        [Xx]) return 2 ;;
        [Bb]) return 1 ;;
        [Rr]) return 3 ;;
        [Yy])
            if generate_configuration; then
                printf '%sConfiguration completed successfully.%s\n' "${ColorGreen:-}" "${ColorReset:-}"
                printf 'Created Miko_Simple.txt, Miko_Rouge.sh, Miko_Info.txt, and mikohime/mikohime.properties.\n'
                printf 'Launch Starsector with ./Miko_Rouge.sh\n'
                refresh_environment
                return 0
            fi
            warn "Unable to build or install the configuration. Existing files were not replaced."
            return 1
            ;;
        *) return 1 ;;
    esac
}

main_menu() {
    refresh_environment
    while true; do
        printf '\n'
        print_header
        print_detected_environment
        printf '\n'
        printf '  1. Configure Java and launcher\n'
        printf '  2. Change launcher background\n'
        printf '  3. Refresh detected components\n'
        printf '  4. Exit\n'
        [[ "$MODERN_JAVA_AVAILABLE" == No ]] && printf '  J. Download and install Java\n'
        missing_download_available && printf '  D. Open a missing component download page\n'
        prompt_line "Select an option: "
        case "$REPLY_LINE" in
            1) configure_interactive ;;
            2) background_menu ;;
            3) refresh_environment ;;
            4|'') return 0 ;;
            [Jj]) [[ "$MODERN_JAVA_AVAILABLE" == No ]] && install_java_menu ;;
            [Dd]) missing_download_available && open_missing_download_page ;;
            *) warn "Invalid selection." ;;
        esac
    done
}

# -----------------------------------------------------------------------------
# Entry point
# -----------------------------------------------------------------------------
usage() {
    printf 'Usage: %s [--non-interactive | --install-java 27|28]\n' "$0"
}

main() {
    initialize_colors
    load_shared_metadata

    local mode="interactive"
    case "${1:-}" in
        --install-java)
            [[ $# -eq 2 ]] || die "$(usage)"
            mode="install-java"
            ;;
        --non-interactive) mode="non-interactive" ;;
        --help|-h) usage; exit 0 ;;
        '') mode="interactive" ;;
        *) die "$(usage)" ;;
    esac

    validate_installation
    install_traps
    acquire_lock || exit 1
    check_write_access || exit 1

    case "$mode" in
        install-java)
            refresh_environment
            install_java "$2"
            ;;
        non-interactive)
            run_noninteractive
            ;;
        interactive)
            if [[ ! -t 0 ]]; then
                die "Interactive mode requires a terminal. Use --non-interactive with MIKO_* variables instead."
            fi
            main_menu
            ;;
    esac
}

if [[ "${MIKO_CONFIGURATOR_LIBRARY_MODE:-0}" != "1" ]]; then
    main "$@"
fi
