// 구버전 PLAIN 글의 본문/이미지 블록 편집기 변환 유틸
// 본문 안에서 이미지 위치를 나타내는 마커
export const IMAGE_MARKER_PATTERN = /\[\[img:(\d+)\]\]/;

export function contentHasInlineImages(content) {
  return IMAGE_MARKER_PATTERN.test(content ?? '');
}

let blockIdCounter = 0;
export function nextBlockId() {
  blockIdCounter += 1;
  return `block-${blockIdCounter}`;
}

// 텍스트/이미지 블록 배열을 마커가 섞인 하나의 본문 문자열로 합침
export function serializeBlocksToContent(blocks) {
  let imageIndex = 0;
  const parts = [];
  for (const block of blocks) {
    if (block.type === 'text') {
      if (block.value.trim() !== '') {
        parts.push(block.value);
      }
    } else {
      parts.push(`[[img:${imageIndex}]]`);
      imageIndex += 1;
    }
  }
  return parts.join('\n\n');
}

// 본문 문자열을 블록 편집기용 텍스트/이미지 블록 배열로 변환
export function parseContentToBlocks(content, existingImages) {
  const safeContent = content ?? '';

  // 마커가 없는 옛 글은 이미지를 전부 맨 앞에 두고 본문을 뒤에 이어붙임
  if (!contentHasInlineImages(safeContent)) {
    const imageBlocks = existingImages.map((img) => ({
      id: nextBlockId(),
      type: 'image',
      kind: 'existing',
      imageId: img.imageId,
      imageUrl: img.imageUrl,
    }));
    return [...imageBlocks, { id: nextBlockId(), type: 'text', value: safeContent }];
  }

  const imagesBySequence = new Map(existingImages.map((img) => [img.sequence, img]));
  const parts = safeContent.split(IMAGE_MARKER_PATTERN);
  const blocks = [];

  parts.forEach((part, index) => {
    if (index % 2 === 1) {
      const image = imagesBySequence.get(Number(part));
      if (image) {
        blocks.push({ id: nextBlockId(), type: 'image', kind: 'existing', imageId: image.imageId, imageUrl: image.imageUrl });
      }
      return;
    }
    const trimmed = part.replace(/^\n+/, '').replace(/\n+$/, '');
    if (trimmed !== '') {
      blocks.push({ id: nextBlockId(), type: 'text', value: trimmed });
    }
  });

  if (!blocks.some((block) => block.type === 'text')) {
    blocks.push({ id: nextBlockId(), type: 'text', value: '' });
  }
  return blocks;
}
