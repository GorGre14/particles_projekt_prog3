#!/bin/bash
# Skupna priprava okolja za zagonske skripte.
# Poisce JAVA_HOME in MPJ_HOME, ce ju uporabnik nima nastavljenih.
# Skripto vkljucimo z: source ./setup_env.sh

# Java in Maven sta lahko izven PATH, kadar program zazenemo iz graficnega vmesnika.
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

if [ -z "$JAVA_HOME" ]; then
    for dir in \
        "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home" \
        "/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
    do
        if [ -d "$dir" ]; then
            export JAVA_HOME="$dir"
            export PATH="$JAVA_HOME/bin:$PATH"
            break
        fi
    done
fi

# MPJ Express je prilozen projektu (lib/mpj), zato dodatna namestitev ni potrebna.
# Ce ima uporabnik svojo namestitev, jo lahko vsili z: export MPJ_HOME=/pot/do/mpj
if [ -z "$MPJ_HOME" ]; then
    PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    for dir in "$PROJECT_DIR/lib/mpj" "$HOME/mpj" "/usr/local/mpj" "/opt/mpj"; do
        if [ -d "$dir" ]; then
            export MPJ_HOME="$dir"
            break
        fi
    done
fi

# Skripte MPJ Express morajo biti izvrsljive (zip arhiv lahko izgubi pravice).
if [ -n "$MPJ_HOME" ] && [ -d "$MPJ_HOME/bin" ]; then
    chmod +x "$MPJ_HOME"/bin/* 2>/dev/null || true
fi
