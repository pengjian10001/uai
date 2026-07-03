#!/usr/bin/env bash
# 下载 OpenCV Zoo ONNX 模型（YuNet 人脸检测 + SFace 特征提取）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MODEL_DIR="${MODEL_DIR:-$PROJECT_DIR/models}"

mkdir -p "$MODEL_DIR"

download() {
  local url="$1"
  local file="$2"
  if [[ -f "$file" && -s "$file" ]]; then
    echo "已存在: $file"
    return
  fi
  echo "下载: $url"
  curl -L --fail --retry 3 -o "$file" "$url"
  echo "完成: $file"
}

download \
  "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx" \
  "$MODEL_DIR/face_detection_yunet_2023mar.onnx"

download \
  "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx" \
  "$MODEL_DIR/face_recognition_sface_2021dec.onnx"

echo "模型目录: $MODEL_DIR"
