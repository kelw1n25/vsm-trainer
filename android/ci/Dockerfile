# Окружение сборки Android без установки SDK на машину разработчика:
#   docker build --platform linux/amd64 -t vsm-android-build android/ci
#   docker run --rm --platform linux/amd64 -v "$PWD/android:/project" -v vsm-gradle:/root/.gradle -v vsm-android-home:/root/.android vsm-android-build \
#     ./gradlew -Pkotlin.compiler.execution.strategy=in-process testDebugUnitTest assembleDebug
# vsm-android-home хранит debug.keystore: иначе каждая сборка подписана новым ключом и не ставится поверх прежней.
# in-process: в Docker Desktop по умолчанию 4 ГБ — отдельному демону Kotlin памяти не хватает.
# linux/amd64 — потому что aapt2 из Android Gradle Plugin выпускается только под x86_64.
FROM eclipse-temurin:17-jdk

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

RUN apt-get update && apt-get install -y --no-install-recommends unzip curl && rm -rf /var/lib/apt/lists/* \
 && mkdir -p $ANDROID_HOME/cmdline-tools \
 && curl -fsSL -o /tmp/tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
 && unzip -q /tmp/tools.zip -d $ANDROID_HOME/cmdline-tools && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
 && rm /tmp/tools.zip \
 && yes | sdkmanager --licenses > /dev/null \
 && sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools" > /dev/null

WORKDIR /project
