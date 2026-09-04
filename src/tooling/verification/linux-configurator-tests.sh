#!/usr/bin/env bash
# =============================================================================
# Integration tests for configurator/linux/Configure_Me.sh
#
# Builds isolated fake Starsector roots under ignored build output, provides
# fake Java 17 / 27-ea / 28-ea launchers, and exercises non-interactive
# generation and the transactional/validation machinery. All fake roots are
# removed on exit.
# =============================================================================
set -uo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/../../.." && pwd)"
CONFIG="$REPO_ROOT/configurator/linux/Configure_Me.sh"
DEFAULT_VM_SRC="$REPO_ROOT/distribution/linux/configuration/DefaultVM"
SHARED_SRC="$REPO_ROOT/configurator/shared"
WORK="$REPO_ROOT/src/build/linux-config-tests"

PASS=0
FAIL=0
CURRENT=""

cleanup_work() { [[ -n "${WORK:-}" && -d "$WORK" ]] && rm -rf -- "$WORK"; }
trap cleanup_work EXIT

section() { CURRENT="$1"; printf '\n=== %s ===\n' "$1"; }
ok() { PASS=$((PASS + 1)); printf '  [PASS] %s\n' "$1"; }
bad() { FAIL=$((FAIL + 1)); printf '  [FAIL] %s\n' "$1"; }

assert_have()    { if grep -Fq -- "$2" "$1"; then ok "$3"; else bad "$3 (missing: $2)"; fi; }
assert_havex()   { if grep -Fxq -- "$2" "$1"; then ok "$3"; else bad "$3 (missing exact line: $2)"; fi; }
assert_nothave() { if grep -Fq -- "$2" "$1"; then bad "$3 (unexpected: $2)"; else ok "$3"; fi; }
assert_file()    { if [[ -f "$1" ]]; then ok "$2"; else bad "$2 (no file: $1)"; fi; }
assert_exec()    { if [[ -x "$1" ]]; then ok "$2"; else bad "$2 (not executable: $1)"; fi; }
assert_true()    { if eval "$1"; then ok "$2"; else bad "$2"; fi; }

line_no() { grep -nF -- "$2" "$1" | head -n1 | cut -d: -f1; }

assert_order() {
    # assert_order FILE EARLIER LATER MSG
    local a b
    a="$(line_no "$1" "$2")"
    b="$(line_no "$1" "$3")"
    if [[ -n "$a" && -n "$b" ]] && (( a < b )); then ok "$4"; else bad "$4 (order $2@$a vs $3@$b)"; fi
}

make_java() {
    # make_java PATH VERSIONSTRING
    mkdir -p -- "$(dirname -- "$1")"
    {
        printf '#!/usr/bin/env bash\n'
        printf 'echo '\''openjdk version "%s"'\'' >&2\n' "$2"
        printf 'echo '\''OpenJDK Runtime Environment'\'' >&2\n'
        printf 'exit 0\n'
    } > "$1"
    chmod +x "$1"
}

make_root() {
    # make_root DIR  -> a minimal valid Linux Starsector installation
    local d="$1" b n
    rm -rf -- "$d"
    mkdir -p -- "$d/starsector-core" "$d/mods" "$d/mikohime/bg" "$d/mikohime/linux" "$d/mikohime/configurator" "$d/fakejava"
    cp -r -- "$SHARED_SRC" "$d/mikohime/configurator/shared"
    cp -- "$CONFIG" "$d/Configure_Me.sh"
    chmod +x "$d/Configure_Me.sh"
    cp -- "$DEFAULT_VM_SRC" "$d/mikohime/DefaultVM"
    printf 'placeholder\n' > "$d/mikohime/miko_jxp.properties"
    printf 'placeholder\n' > "$d/mikohime/mikohime.properties"
    for b in default pather mimikko gamma; do printf 'JPEG-%s\n' "$b" > "$d/mikohime/bg/${b}_bg.jpg"; done
    printf 'JPEG-current\n' > "$d/mikohime/launcher_bg.jpg"
    for n in liblwjgl64.so libopenal64.so libjinput-linux64.so; do printf 'SO\n' > "$d/mikohime/linux/$n"; done
    printf '#!/usr/bin/env bash\necho starsector\n' > "$d/starsector.sh"
    chmod +x "$d/starsector.sh"
    make_java "$d/fakejava/java17" '17.0.11'
    make_java "$d/fakejava/java27" '27-ea'
    make_java "$d/fakejava/java28" '28-ea'
}

make_flat_root() {
    local d="$1"
    make_root "$d"
    rmdir -- "$d/starsector-core"
    printf 'JAR\n' > "$d/fs.common_obf.jar"
}

add_fast_rendering() { printf 'FR\n' > "$1/starsector-core/fr.jar"; printf 'FRA\n' > "$1/starsector-core/fr.agent.jar"; }
add_resource_cache() { printf 'RC\n' > "$1/starsector-core/fr-resource-cache-agent.jar"; }

add_prepatcher() {
    # add_prepatcher DIR FOLDER VERSION_JSON_FRAGMENT
    local d="$1" folder="$2" frag="$3"
    mkdir -p -- "$d/mods/$folder/agent"
    printf 'PPA\n' > "$d/mods/$folder/agent/StarsectorPrepatcherAgent.jar"
    printf '{ "id":"prepatcher", "name":"Prepatcher", %s }\n' "$frag" > "$d/mods/$folder/mod_info.json"
}

run_gen() {
    # run_gen DIR ENV... ; returns configurator exit code, output in $RUN_OUT
    local d="$1"; shift
    RUN_OUT="$(cd -- "$d" && env -i HOME="$HOME" PATH="$PATH" NO_COLOR=1 "$@" bash "$d/Configure_Me.sh" --non-interactive 2>&1)"
    return $?
}

# -----------------------------------------------------------------------------
mkdir -p -- "$WORK"

# --- Version matrix: 17, 27, 28 with FR + Resource Cache + Prepatcher --------
for ver in 17 27 28; do
    section "Java $ver generation (FR + Resource Cache + Prepatcher)"
    root="$WORK/ver$ver"
    make_root "$root"
    add_fast_rendering "$root"
    add_resource_cache "$root"
    add_prepatcher "$root" "StarsectorPrepatcher" '"version":"0.18.4"'
    if run_gen "$root" \
        MIKO_JAVA="$root/fakejava/java$ver" \
        MIKO_HEAP_MIB=8192 \
        MIKO_FAST_RENDERING=1 \
        MIKO_RESOURCE_CACHE=1 \
        MIKO_PREPATCHER=1 \
        MIKO_LOGGING=Full; then
        ok "generation exit 0"
    else
        bad "generation exit 0 (output: $RUN_OUT)"
    fi
    simple="$root/Miko_Simple.txt"
    launcher="$root/Miko_Rouge.sh"
    assert_file "$simple" "Miko_Simple.txt created"
    assert_file "$root/Miko_Info.txt" "Miko_Info.txt created"
    assert_file "$root/mikohime/mikohime.properties" "mikohime.properties created"
    assert_exec "$launcher" "Miko_Rouge.sh is executable"
    assert_havex "$simple" 'com.fs.starfarer.StarfarerLauncher' "launcher class present"
    assert_true "[[ \"\$(awk 'NF{l=\$0} END{print l}' '$simple')\" == 'com.fs.starfarer.StarfarerLauncher' ]]" "launcher class is final line"
    assert_havex "$simple" '-Xms8192m' "heap min set"
    assert_havex "$simple" '-Xmx8192m' "heap max set"
    assert_have "$simple" '-classpath fr.jar:' "classpath prefixed with fr.jar and FR enabled"
    cpline="$(grep -E '^-classpath ' "$simple" | head -n1)"
    assert_true "[[ '$cpline' == *:* && '$cpline' != *';'* ]]" "classpath uses ':' not ';'"
    assert_havex "$simple" '-javaagent:fr.agent.jar' "fast rendering agent present"
    assert_have "$simple" '-javaagent:fr-resource-cache-agent.jar' "resource cache agent present"
    assert_have "$simple" 'StarsectorPrepatcherAgent.jar' "prepatcher agent present"
    assert_order "$simple" '-javaagent:fr-resource-cache-agent.jar' '-javaagent:fr.agent.jar' "resource cache before fast rendering"
    assert_order "$simple" '-javaagent:fr.agent.jar' 'StarsectorPrepatcherAgent.jar' "fast rendering before prepatcher"
    assert_order "$simple" 'StarsectorPrepatcherAgent.jar' '-classpath ' "prepatcher before classpath"
    assert_have "$simple" '-Djava.library.path=../mikohime/linux' "linux native path"
    assert_have "$simple" '-Dcom.fs.starfarer.settings.paths.saves=../saves' "linux saves path"
    assert_have "$launcher" '@../Miko_Simple.txt' "launcher references argfile"
    assert_have "$launcher" '../fakejava/java'"$ver" "launcher references relative java"
    case "$ver" in
        17)
            assert_nothave "$simple" '-XX:-UseCompactObjectHeaders' "no compact-header toggle on 17"
            assert_nothave "$simple" '--enable-final-field-mutation=ALL-UNNAMED' "no final-field-mutation on 17"
            assert_nothave "$simple" '-XX:+ErrorLogSecondaryErrorDetails' "no secondary error details on 17"
            assert_havex "$simple" '-XX:+UseG1GC' "G1 GC on 17"
            assert_havex "$simple" '-XX:TieredStopAtLevel=4' "tiered stop level on 17"
            ;;
        27)
            assert_havex "$simple" '-XX:+UseCriticalCompilerThreadPriority' "critical compiler priority on 27"
            assert_havex "$simple" '#-XX:MaxGCPauseMillis=100' "commented pause target on 27"
            assert_nothave "$simple" '-XX:MaxGCPauseMillis=20' "original pause target stripped on 27"
            assert_havex "$simple" '-XX:+UseCMoveUnconditionally' "AVX2 hint on 27"
            assert_nothave "$simple" '-XX:+ErrorLogSecondaryErrorDetails' "secondary error details dropped on 27"
            assert_havex "$simple" '-XX:+UseShenandoahGC' "DefaultVM body retained on 27"
            ;;
        28)
            assert_havex "$simple" '-XX:-UseCompactObjectHeaders' "compact headers disabled on 28"
            assert_havex "$simple" '-XX:-NMethodRelocation' "nmethod relocation disabled on 28"
            assert_havex "$simple" '-XX:+DisableExplicitGC' "explicit GC disabled on 28"
            assert_havex "$simple" '-XX:+AlwaysPreTouchStacks' "pretouch stacks on 28"
            assert_havex "$simple" '--enable-final-field-mutation=ALL-UNNAMED' "final field mutation on 28"
            assert_havex "$simple" '-XX:+ErrorLogSecondaryErrorDetails' "secondary error details on 28"
            assert_have "$simple" '-Djava.library.path=../mikohime/linux' "linux native path on 28"
            ;;
    esac
done

# --- Logging modes ----------------------------------------------------------
section "Logging modes"
root="$WORK/logging"
make_root "$root"; add_fast_rendering "$root"
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_LOGGING=Full
props="$root/mikohime/mikohime.properties"; simple="$root/Miko_Simple.txt"
assert_have "$props" 'log4j.rootLogger=INFO, ConsoleAppender, file' "Full retains console appender"
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_LOGGING=Reduced
assert_havex "$props" 'log4j.rootLogger=INFO, file' "Reduced drops console appender"
assert_have "$props" 'log4j.logger.com.fs.starfarer.loading=WARN' "Reduced suppresses verbose loggers"
assert_havex "$simple" '-XX:+PrintCommandLineFlags' "Reduced keeps JVM diagnostics uncommented"
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_LOGGING=Minimal
assert_have "$props" 'log4j.rootLogger=INFO, file' "Minimal drops console appender"
assert_have "$simple" '#-XX:+PrintCommandLineFlags' "Minimal comments JVM diagnostics"
if grep -Eq '^-XX:\+PrintCommandLineFlags$' "$simple"; then bad "Minimal must not have uncommented PrintCommandLineFlags"; else ok "Minimal has no uncommented PrintCommandLineFlags"; fi

# --- Fast Rendering presence semantics --------------------------------------
section "Fast Rendering presence semantics"
root="$WORK/fronly"
make_root "$root"
printf 'FR\n' > "$root/starsector-core/fr.jar"   # fr.jar only, no fr.agent.jar
add_resource_cache "$root"
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_FAST_RENDERING=1 MIKO_RESOURCE_CACHE=1
simple="$root/Miko_Simple.txt"
assert_nothave "$simple" '-javaagent:fr.agent.jar' "fr.jar alone does not enable Fast Rendering"
assert_nothave "$simple" '-javaagent:fr-resource-cache-agent.jar' "resource cache stays off when FR off"
assert_nothave "$simple" '-classpath fr.jar:' "classpath has no fr.jar when FR off"
assert_have "$simple" '-classpath ' "classpath present when FR off"

root="$WORK/frboth_off"
make_root "$root"; add_fast_rendering "$root"
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_FAST_RENDERING=0
assert_nothave "$root/Miko_Simple.txt" '-javaagent:fr.agent.jar' "MIKO_FAST_RENDERING=0 disables FR"

# --- Prepatcher compatible / incompatible -----------------------------------
section "Prepatcher metadata selection"
root="$WORK/prepatcher"
make_root "$root"
add_prepatcher "$root" "StarsectorPrepatcher-old" '"version":"0.1.0"'
add_prepatcher "$root" "StarsectorPrepatcher-new" '"version":{"major":0,"minor":18,"patch":4}'
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_PREPATCHER=1
simple="$root/Miko_Simple.txt"; info="$root/Miko_Info.txt"
assert_have "$simple" '../mods/StarsectorPrepatcher-new/agent/StarsectorPrepatcherAgent.jar' "compatible (object-form) prepatcher selected"
assert_nothave "$simple" 'StarsectorPrepatcher-old' "incompatible prepatcher not selected"
assert_have "$info" 'incompatible or unreadable installation' "info reports incompatible count"
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_PREPATCHER=0
assert_nothave "$root/Miko_Simple.txt" 'StarsectorPrepatcherAgent.jar' "MIKO_PREPATCHER=0 disables prepatcher"

root="$WORK/prepatcher-unsafe-name"
make_root "$root"
add_prepatcher "$root" 'StarsectorPrepatcher"invalid' '"version":"0.19.0"'
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_PREPATCHER=1
assert_nothave "$root/Miko_Simple.txt" 'StarsectorPrepatcherAgent.jar' "argfile-unsafe prepatcher name rejected"
assert_have "$root/Miko_Info.txt" 'incompatible or unreadable installation' "unsafe prepatcher reported incompatible"

# --- Memory validation ------------------------------------------------------
section "Memory validation"
root="$WORK/mem"
make_root "$root"
if run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=100; then bad "heap 100 MiB should be rejected"; else ok "heap below 512 MiB rejected"; fi
if run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=1048577; then bad "heap above cap should be rejected"; else ok "heap above 1048576 MiB rejected"; fi
if run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=512; then ok "heap 512 MiB accepted"; else bad "heap 512 MiB should be accepted ($RUN_OUT)"; fi

# --- Lock ownership ---------------------------------------------------------
section "Lock ownership"
root="$WORK/lock"
make_root "$root"
mkdir -p -- "$root/.configure_me.lock"
printf 'pid=999999\n' > "$root/.configure_me.lock/owner.meta"
if run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096; then bad "should refuse to run behind an existing lock"; else ok "existing lock blocks a second instance"; fi
assert_true "[[ -d '$root/.configure_me.lock' ]]" "foreign lock left intact"
rm -rf -- "$root/.configure_me.lock"
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096
assert_true "[[ ! -e '$root/.configure_me.lock' ]]" "own lock released after success"

# --- Rollback on induced commit failure -------------------------------------
section "Rollback on induced commit failure"
root="$WORK/rollback"
make_root "$root"
printf 'SENTINEL-SIMPLE\n' > "$root/Miko_Simple.txt"
printf 'SENTINEL-ROUGE\n' > "$root/Miko_Rouge.sh"
printf 'SENTINEL-PROPS\n' > "$root/mikohime/mikohime.properties"
mkdir -p -- "$root/Miko_Info.txt"; printf 'x\n' > "$root/Miko_Info.txt/blocker"   # directory blocks commit
if run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096; then bad "commit should fail when a target cannot be replaced"; else ok "commit failure surfaced"; fi
assert_true "[[ \"\$(cat '$root/Miko_Simple.txt')\" == 'SENTINEL-SIMPLE' ]]" "Miko_Simple.txt rolled back"
assert_true "[[ \"\$(cat '$root/Miko_Rouge.sh')\" == 'SENTINEL-ROUGE' ]]" "Miko_Rouge.sh rolled back"
assert_true "[[ \"\$(cat '$root/mikohime/mikohime.properties')\" == 'SENTINEL-PROPS' ]]" "mikohime.properties rolled back"
if find "$root" -name '*.pending' -o -name '*.configure_backup_*' 2>/dev/null | grep -q .; then
    bad "no pending/backup residue after rollback"
else
    ok "no pending/backup residue after rollback"
fi

# --- Paths with spaces ------------------------------------------------------
section "Paths containing spaces"
root="$WORK/with space/game root"
make_root "$root"
add_fast_rendering "$root"
add_prepatcher "$root" "StarsectorPrepatcher Test" '"version":"0.19.0"'
if run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_FAST_RENDERING=1 MIKO_PREPATCHER=1; then
    ok "generation succeeds under a path with spaces"
else
    bad "generation under spaced path ($RUN_OUT)"
fi
assert_have "$root/Miko_Simple.txt" 'StarsectorPrepatcher Test/agent/StarsectorPrepatcherAgent.jar' "spaced prepatcher folder handled"
assert_have "$root/Miko_Simple.txt" '"-javaagent:../mods/StarsectorPrepatcher Test/agent/StarsectorPrepatcherAgent.jar"' "spaced agent token is quoted"
assert_exec "$root/Miko_Rouge.sh" "launcher executable under spaced path"

# --- Background staging as part of the transaction --------------------------
section "Background selection in transaction"
root="$WORK/background"
make_root "$root"
run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 MIKO_BACKGROUND=gamma
assert_true "[[ \"\$(cat '$root/mikohime/launcher_bg.jpg')\" == 'JPEG-gamma' ]]" "selected background committed"
assert_have "$root/Miko_Info.txt" 'Launcher background: Gamma' "info records background selection"

# --- Native Linux flat layout and CRLF input handling ------------------------
section "Flat Linux layout"
root="$WORK/flat"
make_flat_root "$root"
if run_gen "$root" MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096; then
    ok "flat-layout generation succeeds"
else
    bad "flat-layout generation succeeds ($RUN_OUT)"
fi
simple="$root/Miko_Simple.txt"
assert_have "$simple" '-classpath mikohime/janino-3.0.12.jar:mikohime/commons-compiler-3.0.12.jar' "flat classpath is one argument"
assert_have "$simple" '-Djava.library.path=mikohime/linux' "flat native path"
assert_have "$root/Miko_Rouge.sh" '@Miko_Simple.txt' "flat launcher references root argfile"
if grep -q $'\r' "$simple"; then
    bad "generated argument file uses LF endings"
else
    ok "generated argument file uses LF endings"
fi

section "Launcher path resolution"
root="$WORK/relative-java"
make_root "$root"
run_gen "$root" MIKO_JAVA=fakejava/java28 MIKO_HEAP_MIB=4096
assert_have "$root/Miko_Rouge.sh" '../fakejava/java28' "relative Java path resolved for nested runtime"

root="$WORK/script-location"
make_flat_root "$root"
RUN_OUT="$(cd -- "$WORK" && env -i HOME="$HOME" PATH="$PATH" NO_COLOR=1 \
    MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 \
    bash "$root/Configure_Me.sh" --non-interactive 2>&1)"
if [[ $? -eq 0 ]]; then
    ok "packaged configurator finds its root outside the current directory"
else
    bad "packaged configurator finds its root outside the current directory ($RUN_OUT)"
fi

root="$WORK/source-script"
make_flat_root "$root"
RUN_OUT="$(cd -- "$root" && env -i HOME="$HOME" PATH="$PATH" NO_COLOR=1 \
    MIKO_JAVA="$root/fakejava/java28" MIKO_HEAP_MIB=4096 \
    bash "$CONFIG" --non-interactive 2>&1)"
if [[ $? -eq 0 ]]; then
    ok "repository configurator uses the current game root"
else
    bad "repository configurator uses the current game root ($RUN_OUT)"
fi

# --- Archive path safety -----------------------------------------------------
section "Archive path safety"
MIKO_CONFIGURATOR_LIBRARY_MODE=1 source "$CONFIG"
if archive_path_escapes_root 'jdk-28+13/legal/java.base/../../ASSEMBLY_EXCEPTION'; then
    bad "safe parent-relative JDK symlink accepted"
else
    ok "safe parent-relative JDK symlink accepted"
fi
if archive_path_escapes_root 'jdk-28+13/lib/../../../outside'; then
    ok "escaping JDK symlink rejected"
else
    bad "escaping JDK symlink rejected"
fi
if archive_path_escapes_root '/tmp/outside'; then
    ok "absolute archive path rejected"
else
    bad "absolute archive path rejected"
fi

# --- Summary ----------------------------------------------------------------
printf '\n============================================================\n'
printf 'Linux configurator tests: %d passed, %d failed\n' "$PASS" "$FAIL"
printf '============================================================\n'
[[ "$FAIL" -eq 0 ]]
