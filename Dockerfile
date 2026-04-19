# =====================================================================
# 文房具基幹システム Dockerfile（Cloud Run 想定／マルチステージ）
# - ビルド: maven:3.9.6 + Eclipse Temurin JDK17 で fat-jar(bunnbougu.jar)
# - 実行 : eclipse-temurin:17-jre-jammy + Asia/Tokyo + ja_JP.UTF-8
# - 起動 : PORT 環境変数で待機。デフォルト 8080
# =====================================================================

# ---- ステージ1: ビルド ----
FROM maven:3.9.6-eclipse-temurin-17 AS build

# 作業ディレクトリ
WORKDIR /workspace

# 依存解決のため pom.xml を先にコピーしてキャッシュを効かせる
COPY Backend/pom.xml ./pom.xml
RUN mvn -B -e -ntp dependency:go-offline

# ソースをコピーして fat jar をビルド（テストは Cloud Run 側ではスキップ）
COPY Backend/src ./src
RUN mvn -B -e -ntp -DskipTests clean package


# ---- ステージ2: 実行 ----
FROM eclipse-temurin:17-jre-jammy

# 日本語ロケール・タイムゾーン（README §9 の必須要件）
ENV TZ=Asia/Tokyo \
    LANG=ja_JP.UTF-8 \
    LANGUAGE=ja_JP:ja \
    LC_ALL=ja_JP.UTF-8

# tzdata + locales を導入し ja_JP.UTF-8 を生成
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        tzdata locales ca-certificates \
 && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
 && echo $TZ > /etc/timezone \
 && sed -i 's/^# *\(ja_JP.UTF-8\)/\1/' /etc/locale.gen \
 && locale-gen ja_JP.UTF-8 \
 && update-locale LANG=ja_JP.UTF-8 \
 && rm -rf /var/lib/apt/lists/*

# 非 root ユーザで動かす（Cloud Run でも OK）
RUN useradd --no-log-init --create-home --shell /usr/sbin/nologin app
WORKDIR /app

# JAR とテンプレート（HTML/CSS/JS）を配置
COPY --from=build /workspace/target/bunnbougu.jar /app/app.jar
COPY Frontend/ /app/Frontend/

# テンプレートディレクトリ・ポートを明示
ENV TEMPLATE_DIR=/app/Frontend \
    PORT=8080

# Cloud Run でのリッスンポート
EXPOSE 8080

# 非 root に切替
USER app

# JVM チューニング（コンテナメモリに合わせて自動）
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Duser.timezone=Asia/Tokyo", \
    "-Dfile.encoding=UTF-8", \
    "-jar", "/app/app.jar"]
