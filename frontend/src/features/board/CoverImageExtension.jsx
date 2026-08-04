// Tiptap 이미지 확장, 대표 이미지 표시 속성 추가
import ImageExtension from '@tiptap/extension-image';
import { ReactNodeViewRenderer } from '@tiptap/react';
import CoverImageView from './CoverImageView';

export const CoverImage = ImageExtension.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      isCover: {
        default: false,
        parseHTML: (element) => element.getAttribute('data-cover') === 'true',
        renderHTML: (attributes) => (attributes.isCover ? { 'data-cover': 'true' } : {}),
      },
    };
  },
  addNodeView() {
    return ReactNodeViewRenderer(CoverImageView);
  },
});
