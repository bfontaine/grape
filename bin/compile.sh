#!/usr/bin/env bash
# Based on @borkdude's script/compile
# https://github.com/borkdude/clj-kondo/blob/e62eb04bc8bdb754a368ca8e7b0e76d8d568253e/script/compile
# https://github.com/babashka/pod-babashka-parcera/blob/9a40889aabd2bd20a0fdb02e8c19f0049ea072a6/script/compile
# ^ published under the Eclipse licence, just like grape.

if [ -z "$GRAALVM_HOME" ]; then
    echo "Please set GRAALVM_HOME"
    exit 1
fi

"$GRAALVM_HOME/bin/gu" install native-image || true

export JAVA_HOME=$GRAALVM_HOME
export PATH=$GRAALVM_HOME/bin:$PATH

GRAPE_VERSION=$(cat resources/GRAPE_VERSION)

lein with-profiles +native-image "do" clean, uberjar

"$GRAALVM_HOME/bin/native-image" \
    -jar "target/grape-$GRAPE_VERSION-standalone.jar" \
    -H:Name=grape \
    -H:+ReportExceptionStackTraces \
    -J-Dclojure.spec.skip-macros=true \
    -J-Dclojure.compiler.direct-linking=true \
    -H:ReflectionConfigurationFiles=reflection.json \
    -H:IncludeResources=GRAPE_VERSION \
    --initialize-at-run-time=java.lang.Math\$RandomNumberGeneratorHolder \
    --initialize-at-build-time  \
    -H:Log=registerResource: \
    --verbose \
    --no-fallback \
    --no-server \
    --report-unsupported-elements-at-runtime \
    -H:EnableURLProtocols=http,https \
    --enable-all-security-services \
    -J-Xmx4500m
