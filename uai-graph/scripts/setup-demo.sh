#!/usr/bin/env bash
# 生成演示素材：用 ffmpeg 从公开人脸图合成短视频，并截取目标人物照
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEMO_DIR="$PROJECT_DIR/demo-data"
MODEL_DIR="$PROJECT_DIR/models"

mkdir -p "$DEMO_DIR"

if ! command -v curl >/dev/null 2>&1; then
  echo "请先安装 curl"
  exit 1
fi

PERSON_A="https://raw.githubusercontent.com/opencv/opencv/4.x/samples/data/lena.jpg"
PERSON_B="https://raw.githubusercontent.com/opencv/opencv/4.x/samples/data/baboon.jpg"

curl -L --fail --retry 3 -o "$DEMO_DIR/person_a.jpg" "$PERSON_A"
curl -L --fail --retry 3 -o "$DEMO_DIR/person_b.jpg" "$PERSON_B"
cp "$DEMO_DIR/person_a.jpg" "$DEMO_DIR/target.jpg"

if command -v ffmpeg >/dev/null 2>&1; then
  # 合成 12 秒演示视频：0-4s person_a, 4-8s person_b, 8-12s person_a
  ffmpeg -y -loop 1 -t 4 -i "$DEMO_DIR/person_a.jpg" \
    -loop 1 -t 4 -i "$DEMO_DIR/person_b.jpg" \
    -loop 1 -t 4 -i "$DEMO_DIR/person_a.jpg" \
    -filter_complex "[0:v]scale=640:480,setsar=1[v0];[1:v]scale=640:480,setsar=1[v1];[2:v]scale=640:480,setsar=1[v2];[v0][v1][v2]concat=n=3:v=1:a=0[v]" \
    -map "[v]" -r 25 -c:v libx264 -pix_fmt yuv420p "$DEMO_DIR/sample.mp4"
else
  echo "未检测到 ffmpeg，改用 JavaCV 生成演示视频..."
  mvn -q exec:java -Dexec.mainClass=com.uni.uai.graph.facesearch.DemoDataGenerator \
    -Dexec.args="$DEMO_DIR"
fi

bash "$SCRIPT_DIR/download-models.sh"

echo ""
echo "演示素材已生成:"
echo "  视频: $DEMO_DIR/sample.mp4"
echo "  目标人物图: $DEMO_DIR/target.jpg"
echo ""
echo "运行演示:"
echo "  cd $PROJECT_DIR"
echo "  mvn -q exec:java -Dexec.args=\"run --video demo-data/sample.mp4 --target demo-data/target.jpg\""
