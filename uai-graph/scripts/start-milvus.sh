#!/usr/bin/env bash
# 启动 Milvus Standalone（企业级路线可选）
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "❌ 未检测到 docker 命令，请先安装 Docker Desktop："
  echo "   https://www.docker.com/products/docker-desktop/"
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "❌ Docker 已安装，但 Docker 守护进程未运行。"
  echo ""
  echo "macOS 处理步骤："
  echo "  1. 打开「应用程序」中的 Docker Desktop"
  echo "  2. 等待菜单栏鲸鱼图标显示为 Running / 绿色"
  echo "  3. 再执行: bash scripts/start-milvus.sh"
  echo ""
  echo "若暂时不用 Milvus，可直接用默认内存模式（无需 Docker）："
  echo "  mvn -q exec:java -Dexec.args=\"run --video demo-data/sample.mp4 --target demo-data/target.jpg\""
  exit 1
fi

if docker ps -a --format '{{.Names}}' | grep -q '^milvus-standalone$'; then
  if docker ps --format '{{.Names}}' | grep -q '^milvus-standalone$'; then
    echo "✅ Milvus Standalone 已在运行: 127.0.0.1:19530"
    exit 0
  fi
  echo "发现已停止的容器 milvus-standalone，正在启动..."
  docker start milvus-standalone
else
  echo "正在拉取并启动 Milvus Standalone（首次较慢）..."
  docker run -d --name milvus-standalone \
    -p 19530:19530 -p 9091:9091 \
    -v milvus-data:/var/lib/milvus \
    milvusdb/milvus:v2.4.0 \
    milvus run standalone
fi

echo ""
echo "✅ Milvus Standalone 已启动: 127.0.0.1:19530"
echo "使用 Milvus 运行 demo:"
echo "  mvn -q exec:java -Dexec.args=\"run --video demo-data/sample.mp4 --target demo-data/target.jpg --milvus true\""
