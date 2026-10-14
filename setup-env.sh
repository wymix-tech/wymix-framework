#!/usr/bin/env bash
#
# wymix_dev 环境安装 / 编译验证脚本（Linux / macOS / Git Bash）
# Windows 原生 CMD / PowerShell 请在 WSL 或 Git Bash 下运行本脚本。
#
# 职责：环境准备（JDK / Maven，官网下载到项目内 .tools/，不改动系统）→
#       配置 Maven 镜像 → 编译全模块验证。
#
# 默认行为：
#   1. 优先使用系统已装且达标的 JDK(>=17) / Maven(>=3.6)；
#      缺失或版本不足时从官网下载到 .tools/（Adoptium / Apache 官方 dist）
#   2. 配置 Maven 镜像（~/.m2/settings.xml），已存在则不覆盖
#   3. 执行 mvn 编译验证（默认：clean install -DskipTests，会编译 main + test 但不跑测试）
#
# 用法：
#   ./setup-env.sh                 # 默认：准备环境 → 编译验证
#   ./setup-env.sh --env-only      # 只装工具链，不编译
#   ./setup-env.sh --run-tests     # 编译并执行单元测试
#   ./setup-env.sh --goal "..."    # 自定义 Maven 目标（默认 "clean install -DskipTests"）
#   ./setup-env.sh --print-env     # 打印可用于当前 shell 的 JAVA_HOME / PATH 导出语句
#   ./setup-env.sh --clean-tools   # 删除 .tools/（重新下载工具链）
#   ./setup-env.sh help
#
# 环境变量：
#   MAVEN_MIRROR_URL=...   Maven 镜像（默认阿里云 public）
#   JDK_VERSION=17         下载用 JDK 主版本
#   MAVEN_VERSION=3.9.16   下载用 Maven 版本
#   FORCE_TOOLING=1        强制使用/下载 .tools 内工具链（忽略系统已装）
#   MAVEN_OPTS="-Xmx2g"    Maven JVM 参数
#
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLS_DIR="$PROJECT_ROOT/.tools"

MAVEN_MIRROR_URL="${MAVEN_MIRROR_URL:-https://maven.aliyun.com/repository/public}"
JDK_VERSION="${JDK_VERSION:-17}"
MAVEN_VERSION="${MAVEN_VERSION:-3.9.16}"
FORCE_TOOLING="${FORCE_TOOLING:-0}"
MAVEN_OPTS="${MAVEN_OPTS:--Xmx2g}"

MIN_JAVA_MAJOR=17
MIN_MAVEN_MINOR_ON_3=6
BUILD_GOAL="clean install -DskipTests"

ACTION="setup"
for arg in "$@"; do
  case "$arg" in
    --env-only)    ACTION="env" ;;
    --run-tests)   BUILD_GOAL="clean install" ;;
    --goal)        ACTION="__need_value" ;;
    --print-env)   ACTION="print-env" ;;
    --clean-tools) ACTION="clean-tools" ;;
    -h|--help|help) ACTION="help" ;;
    *)
      if [ "$ACTION" = "__need_value" ]; then BUILD_GOAL="$arg"; ACTION="setup"
      else echo "未知参数: $arg（用 ./setup-env.sh help 查看用法）" >&2; exit 2; fi
      ;;
  esac
done
[ "$ACTION" != "__need_value" ] || { echo "--goal 缺少取值" >&2; exit 2; }

log() { echo "[setup-env] $*"; }
die() { echo "[setup-env][错误] $*" >&2; exit 1; }

# ---------- 平台识别 ----------
detect_platform() {
  local raw_os raw_arch
  raw_os="$(uname -s)"
  raw_arch="$(uname -m)"
  case "$raw_os" in
    Linux*)                            PLATFORM="linux" ;;
    Darwin*)                           PLATFORM="mac" ;;
    MINGW*|MSYS*|CYGWIN*|Windows_NT*)  PLATFORM="windows" ;;
    *) die "不支持的操作系统: $raw_os（Windows 原生请用 WSL / Git Bash）" ;;
  esac
  case "$raw_arch" in
    x86_64|amd64|AMD64)   PLATFORM_ARCH="x64" ;;
    aarch64|arm64|ARM64)  PLATFORM_ARCH="aarch64" ;;
    *) die "不支持的 CPU 架构: $raw_arch" ;;
  esac
}
detect_platform

# ---------- 下载 / 解压 ----------
download() {
  local url="$1" out="$2"
  log "下载: $url"
  mkdir -p "$(dirname "$out")"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL --retry 3 --retry-delay 2 -o "$out" "$url" || die "下载失败: $url"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$out" "$url" || die "下载失败: $url"
  else
    die "缺少 curl / wget，无法下载 $url"
  fi
}

# 解压并去掉顶层目录，内容直接落到 dest
extract_strip() {
  local file="$1" dest="$2"
  mkdir -p "$dest"
  case "$file" in
    *.zip)
      command -v unzip >/dev/null 2>&1 || die "缺少 unzip，无法解压 $file"
      local tmp="$dest.__tmp"
      rm -rf "$tmp"; mkdir -p "$tmp"
      unzip -q -o "$file" -d "$tmp"
      local inner; inner="$(ls -1 "$tmp" | head -1)"
      [ -n "$inner" ] || die "压缩包为空: $file"
      ( shopt -s dotglob 2>/dev/null || true; mv "$tmp/$inner"/* "$dest"/ 2>/dev/null || true )
      rm -rf "$tmp"
      ;;
    *.tar.gz|*.tgz) tar -xzf "$file" -C "$dest" --strip-components=1 ;;
    *) die "不支持的压缩包格式: $file" ;;
  esac
}

tooling_urls() {
  JDK_URL="https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/ga/${PLATFORM}/${PLATFORM_ARCH}/jdk/hotspot/normal/eclipse"
  case "$PLATFORM" in
    windows)
      JDK_FILE="jdk.zip"
      MAVEN_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip"
      MAVEN_URL_FALLBACK="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip"
      MAVEN_FILE="maven.zip"
      ;;
    *)
      JDK_FILE="jdk.tar.gz"
      MAVEN_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
      MAVEN_URL_FALLBACK="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
      MAVEN_FILE="maven.tar.gz"
      ;;
  esac
}

java_major_of() {
  "$1" -version 2>&1 | head -1 \
    | sed -E 's/.*version "([^"]+)".*/\1/' \
    | awk -F. '{print ($1=="1" ? $2 : $1)}'
}

# ---------- 工具链安装（官网下载到 .tools/） ----------
ensure_java() {
  # 1) .tools 自带优先
  if [ -x "$TOOLS_DIR/jdk/bin/java" ]; then
    export JAVA_HOME="$TOOLS_DIR/jdk"
  elif [ -d "$TOOLS_DIR/jdk/Contents/Home/bin" ]; then
    export JAVA_HOME="$TOOLS_DIR/jdk/Contents/Home"
  else
    JAVA_HOME=""
  fi
  if [ -n "${JAVA_HOME:-}" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"; JAVA_BIN="$JAVA_HOME/bin/java"
  else
    JAVA_BIN="java"
  fi

  if [ "$FORCE_TOOLING" != "1" ] && command -v "$JAVA_BIN" >/dev/null 2>&1; then
    local v; v="$(java_major_of "$JAVA_BIN")"
    if [ -n "$v" ] && [ "$v" -ge "$MIN_JAVA_MAJOR" ] 2>/dev/null; then
      log "JDK 就绪 ($([ "$JAVA_BIN" = "java" ] && echo 系统 || echo .tools)): $("$JAVA_BIN" -version 2>&1 | head -1)"
      export PATH="$(dirname "$JAVA_BIN"):$PATH"
      return
    fi
    log "系统 JDK 版本不足（需 >= $MIN_JAVA_MAJOR），改用 .tools"
  fi

  log "安装 JDK $JDK_VERSION（Eclipse Temurin 官方下载）-> $TOOLS_DIR/jdk"
  tooling_urls
  rm -rf "$TOOLS_DIR/jdk"
  download "$JDK_URL" "$TOOLS_DIR/$JDK_FILE"
  extract_strip "$TOOLS_DIR/$JDK_FILE" "$TOOLS_DIR/jdk"
  rm -f "$TOOLS_DIR/$JDK_FILE"
  if [ -d "$TOOLS_DIR/jdk/Contents/Home/bin" ]; then JAVA_HOME="$TOOLS_DIR/jdk/Contents/Home"; else JAVA_HOME="$TOOLS_DIR/jdk"; fi
  export JAVA_HOME; export PATH="$JAVA_HOME/bin:$PATH"; JAVA_BIN="$JAVA_HOME/bin/java"
  [ -x "$JAVA_BIN" ] || die "JDK 安装失败: $JAVA_BIN 不可执行"
  log "JDK 安装完成: $("$JAVA_BIN" -version 2>&1 | head -1)"
}

ensure_maven() {
  if [ -x "$TOOLS_DIR/maven/bin/mvn" ]; then
    MVN_BIN="$TOOLS_DIR/maven/bin/mvn"
    log "Maven 就绪 (.tools): $("$MVN_BIN" -v 2>/dev/null | head -1)"
    return
  fi
  if [ "$FORCE_TOOLING" != "1" ] && command -v mvn >/dev/null 2>&1; then
    MVN_BIN="mvn"
    log "Maven 就绪 (系统): $(mvn -v 2>/dev/null | head -1)"
    return
  fi
  log "安装 Maven $MAVEN_VERSION（Apache 官方下载）-> $TOOLS_DIR/maven"
  tooling_urls
  rm -rf "$TOOLS_DIR/maven"
  download "$MAVEN_URL" "$TOOLS_DIR/$MAVEN_FILE" 2>/dev/null \
    || download "$MAVEN_URL_FALLBACK" "$TOOLS_DIR/$MAVEN_FILE"
  extract_strip "$TOOLS_DIR/$MAVEN_FILE" "$TOOLS_DIR/maven"
  rm -f "$TOOLS_DIR/$MAVEN_FILE"
  MVN_BIN="$TOOLS_DIR/maven/bin/mvn"
  [ -x "$MVN_BIN" ] || die "Maven 安装失败: $MVN_BIN 不可执行"
  log "Maven 安装完成: $("$MVN_BIN" -v 2>/dev/null | head -1)"
}

# ---------- Maven 镜像 ----------
ensure_maven_mirror() {
  local settings="$HOME/.m2/settings.xml"
  if [ -f "$settings" ] && grep -q "$MAVEN_MIRROR_URL" "$settings"; then
    log "Maven 镜像已配置: $MAVEN_MIRROR_URL"
    return
  fi
  if [ -f "$settings" ]; then
    log "已存在 $settings 且未包含本镜像，保持原样"
    return
  fi
  log "配置 Maven 镜像: $MAVEN_MIRROR_URL -> $settings"
  mkdir -p "$HOME/.m2"
  cat > "$settings" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <name>Aliyun Maven Public Mirror</name>
      <url>$MAVEN_MIRROR_URL</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
}

# ---------- 编译验证 ----------
do_build() {
  log "编译验证: mvn -B $BUILD_GOAL"
  ( cd "$PROJECT_ROOT" && MAVEN_OPTS="$MAVEN_OPTS" "$MVN_BIN" -B -ntp $BUILD_GOAL )
  log "编译验证通过 ✅"
}

print_env() {
  ensure_java
  ensure_maven
  echo "export JAVA_HOME=\"${JAVA_HOME:-$TOOLS_DIR/jdk}\""
  echo "export PATH=\"$(dirname "${JAVA_BIN:-$TOOLS_DIR/jdk/bin/java}"):$TOOLS_DIR/maven/bin:\$PATH\""
}

case "$ACTION" in
  help)
    sed -n '2,30p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    ;;
  clean-tools)
    rm -rf "$TOOLS_DIR"
    log "已清除 $TOOLS_DIR"
    ;;
  print-env)
    print_env
    ;;
  env)
    ensure_java
    ensure_maven
    ensure_maven_mirror
    log "环境准备完成（未编译）"
    ;;
  setup)
    ensure_java
    ensure_maven
    ensure_maven_mirror
    do_build
    ;;
esac
